package net.crepe.system

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.component.system.RefSystem
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
import com.hypixel.hytale.server.core.universe.world.meta.BlockState
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.common.DataItem
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.controller.ControllerUpgradesComponent
import net.crepe.inventory.ControllerAggregateContainer
import net.crepe.utils.BlockUtils
import net.crepe.utils.UpgradeUtils
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString

class ControllerSystem {
    class ControllerRefSystem : RefSystem<ChunkStore?>() {
        override fun onEntityAdded(
            ref: Ref<ChunkStore?>,
            reason: AddReason,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>,
        ) {
            val state = BlockState.getBlockState(ref, cmdBuffer)
            if (state is ItemContainerState) {
                if (state.itemContainer !is ControllerAggregateContainer) {
                    state.setItemContainer(ControllerAggregateContainer(store.externalData.world.name))
                }
            }
        }

        override fun onEntityRemove(
            p0: Ref<ChunkStore?>,
            p1: RemoveReason,
            p2: Store<ChunkStore?>,
            p3: CommandBuffer<ChunkStore?>,
        ) {
        }

        override fun getQuery(): Query<ChunkStore?>? {
            return Query.and(ControllerLinksComponent.getComponentType())
        }
    }
    
    class ControllerBreakEvent : EntityEventSystem<EntityStore?, BreakBlockEvent>(BreakBlockEvent::class.java) {
        override fun handle(
            index: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: BreakBlockEvent,
        ) {
            val player = chunk.getComponent(index, Player.getComponentType())!!
            val pos = event.targetBlock
            val world = store.externalData.world
            val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return
            val upgradesComponent = blockRef.store.getComponent(blockRef, ControllerUpgradesComponent.getComponentType()) ?: return

            if (player.gameMode != GameMode.Creative) {
                val metadata = BsonDocument()
                val componentData = BsonDocument()
                if (upgradesComponent.upgrades.isNotEmpty() && upgradesComponent.upgrades.any { !DataItem.isEmpty(it) }) {
                    val upgradesData = BsonDocument()
                    upgradesComponent.upgrades.forEachIndexed { index, item ->
                        if (!DataItem.isEmpty(item)) {
                            val itemData = BsonDocument()
                            itemData["Id"] = BsonString(item!!.itemId)
                            itemData["Quantity"] = BsonInt32(item.quantity)
                            upgradesData["$index"] = itemData
                        }
                    }
                    componentData["Upgrades"] = upgradesData
                }
                if (!componentData.isEmpty()) {
                    metadata["SimpleDrawer"] = componentData
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
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }

    }
    
    class ControllerPlaceEvent : EntityEventSystem<EntityStore?, PlaceBlockEvent>(PlaceBlockEvent::class.java) {
        override fun handle(
            index: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: PlaceBlockEvent,
        ) {
            val heldItem = event.itemInHand!!

            BlockType.getAssetMap().getAsset(heldItem.itemId)?.blockEntity?.getComponent(ControllerLinksComponent.getComponentType()) ?: return

            cmdBuffer.run {
                val pos = event.targetBlock
                val world = store.externalData.world
                val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return@run
                val linksComponent =
                    blockRef.store.getComponent(blockRef, ControllerLinksComponent.getComponentType())!!
                val upgradesComponent =
                    blockRef.store.getComponent(blockRef, ControllerUpgradesComponent.getComponentType()) ?: return@run

                if (heldItem.metadata?.getOrDefault("SimpleDrawer", null) == null) return@run

                val data = heldItem.metadata!!["SimpleDrawer"]!!.asDocument()
                if (data.containsKey("Upgrades")) {
                    val upgrades = data["Upgrades"]!!.asDocument()
                    for (slot in upgradesComponent.upgrades.indices) {
                        val slotData = upgrades["$slot"]?.asDocument() ?: continue

                        upgradesComponent.upgrades[slot] = DataItem(
                            slotData["Id"]!!.asString().value,
                            slotData["Quantity"]!!.asInt32().value
                        )

                        val item = ItemStack(slotData["Id"]!!.asString().value)
                        UpgradeUtils.getUpgradeBoost("Range", item.item.data.rawTags.keys)?.let {
                            linksComponent.radius += it
                        }
                    }
                }
                BlockUtils.saveBlock(blockRef)
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
}