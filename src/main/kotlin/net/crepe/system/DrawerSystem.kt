package net.crepe.system

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.DrawerBoundDisplayComponent
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.components.DrawerContainerComponent
import net.crepe.utils.DisplayUtils
import org.bson.BsonDocument
import org.bson.BsonInt32

class DrawerSystem {
    class DrawerBreakEvent : EntityEventSystem<EntityStore?, BreakBlockEvent>(BreakBlockEvent::class.java) {
        override fun handle(
            index: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: BreakBlockEvent
        ) {
            val player = chunk.getComponent(index, Player.getComponentType())!!
            val pos = event.targetBlock
            val world = store.externalData.world
            val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return
            
            if (player.gameMode != GameMode.Creative) {
                val drawerComponent = blockRef.store.getComponent(blockRef, DrawerContainerComponent.getComponentType())
                    ?: return
                val upgradeComponent = blockRef.store.getComponent(blockRef, DrawerUpgradableComponent.getComponentType())
                val metadata = BsonDocument()
                if (drawerComponent.storedItem != ItemStack.EMPTY) {
                    metadata.put("StoredItem", ItemStack.CODEC.encode(drawerComponent.storedItem))
                    metadata.put("StoredQuantity", BsonInt32(drawerComponent.storedQuantity))
                    metadata.put("Capacity", BsonInt32(drawerComponent.capacity))
                }
                if (upgradeComponent != null) {
                    if (upgradeComponent.tier > 0) {
                        metadata.put("Tier", BsonInt32(upgradeComponent.tier))
                    }
                }
                val drop = ItemComponent.generateItemDrop(
                    store,
                    ItemStack(event.blockType.id, 1, if (metadata.isEmpty()) null else metadata),
                    Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
                    Vector3f.ZERO,
                    0f, 0f, 0f
                ) ?: return

                cmdBuffer.addEntity(drop, AddReason.SPAWN)
            }

            val boundDisplay = blockRef.store.getComponent(blockRef, DrawerBoundDisplayComponent.getComponentType()) ?: return
            cmdBuffer.run {
                if (boundDisplay.displayEntity != null)
                    DisplayUtils.removeDisplayEntity(it, boundDisplay.displayEntity!!)
                if (boundDisplay.numberDisplays.isNotEmpty()) {
                    for (uuid in boundDisplay.numberDisplays) {
                        if (uuid != null) {
                            DisplayUtils.removeDisplayEntity(it, uuid)
                        }
                    }
                }
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
    
    class DrawerPlaceEvent : EntityEventSystem<EntityStore?, PlaceBlockEvent>(PlaceBlockEvent::class.java) {
        override fun handle(
            index: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: PlaceBlockEvent
        ) {
            val heldItem = event.itemInHand!!

            BlockType.getAssetMap().getAsset(heldItem.itemId)?.blockEntity?.getComponent(DrawerContainerComponent.getComponentType()) ?: return
            
            cmdBuffer.run {
                val pos = event.targetBlock
                val blockEntity = BlockModule.getBlockEntity(it.externalData.world, pos.x, pos.y, pos.z)
                val drawerContainer = blockEntity?.store?.getComponent(blockEntity, DrawerContainerComponent.getComponentType())
                val boundDisplay = blockEntity?.store?.ensureAndGetComponent(blockEntity, DrawerBoundDisplayComponent.getComponentType())
                val upgradeComponent = blockEntity?.store?.getComponent(blockEntity, DrawerUpgradableComponent.getComponentType())
                
                if (drawerContainer != null) {
                     if (retrieveContainerItem(drawerContainer, heldItem)) {
                         val rot = Vector3f(0f, event.rotation.yaw.radians.toFloat() + Math.PI.toFloat(), 0f)
                         val uuid = DisplayUtils.spawnDisplayEntity(store, pos, rot, drawerContainer.storedItem)
                         boundDisplay!!.displayEntity = uuid

                         val forwardVector = Vector3d(0.0, 0.0, -0.438).rotateY(rot.yaw)
                         val numberPos = pos.toVector3d().add(Vector3d(0.5, 0.15, 0.5).add(forwardVector))
                         DisplayUtils.renderNumber(0, drawerContainer.storedQuantity, boundDisplay, numberPos, rot, store.externalData.world, store)
                     }
                }
                if (upgradeComponent != null) {
                    val data = heldItem.metadata
                    if (data != null) {
                        upgradeComponent.tier = data["Tier"]?.asInt32()?.value ?: 0
                    }
                }
            }
        }
        
        private fun retrieveContainerItem(drawerContainer: DrawerContainerComponent, item: ItemStack): Boolean {
            val data = item.metadata ?: return false

            drawerContainer.storedQuantity = data["StoredQuantity"]?.asInt32()?.value ?: 0
            drawerContainer.capacity = data["Capacity"]?.asInt32()?.value ?: 0
            val storedItemData = data["StoredItem"]?.asDocument() ?: return false
            drawerContainer.storedItem = ItemStack.CODEC.decode(storedItemData) ?: ItemStack.EMPTY
            
            return true
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
}