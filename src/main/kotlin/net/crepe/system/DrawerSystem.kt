package net.crepe.system

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
import com.hypixel.hytale.server.core.universe.world.SoundUtil
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.DrawerBoundDisplayComponent
import net.crepe.component.DrawerDisplayComponent
import net.crepe.component.DrawerLockComponent
import net.crepe.component.DrawerSlotHitComponent
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.component.DrawerSlotsContainerComponent
import net.crepe.component.DrawerSlotsContainerComponent.Companion.getComponentType
import net.crepe.components.DrawerContainerComponent
import net.crepe.utils.DisplayUtils
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32

class DrawerSystem {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        fun quickStack(
            ref: Ref<ChunkStore?>,
            pos: Vector3i,
            containerComponent: DrawerSlotsContainerComponent,
            displaysComponent: DrawerDisplayComponent,
            upgradableComponent: DrawerUpgradableComponent?,
            slotIndex: Int,
            player: Player,
        ) {
            val inventory = player.inventory
            val drawerSlot = containerComponent.slots.getOrNull(slotIndex) ?: return
            
            inventory.hotbar.forEach { slot, item ->
                if (ItemStack.isEmpty(item)) return@forEach
                if (drawerSlot.storedQuantity == drawerSlot.capacity) return@forEach

                if (item.isStackableWith(drawerSlot.storedItem)) {
                    insertItem(
                        ref,
                        containerComponent,
                        displaysComponent,
                        upgradableComponent,
                        slotIndex,
                        item,
                        slot,
                        inventory.hotbar,
                        pos
                    )
                }
            }
            inventory.storage.forEach { slot, item ->
                if (ItemStack.isEmpty(item)) return@forEach
                if (drawerSlot.storedQuantity == drawerSlot.capacity) return@forEach

                if (item.isStackableWith(drawerSlot.storedItem)) {
                    insertItem(
                        ref,
                        containerComponent,
                        displaysComponent,
                        upgradableComponent,
                        slotIndex,
                        item,
                        slot,
                        inventory.storage,
                        pos
                    )
                }
            }
        }
        
        fun insertItem(
            ref: Ref<ChunkStore?>,
            containerComponent: DrawerSlotsContainerComponent,
            displaysComponent: DrawerDisplayComponent,
            upgradableComponent: DrawerUpgradableComponent?,
            slotIndex: Int,
            item: ItemStack,
            itemSlot: Short,
            itemContainer: ItemContainer?,
            pos: Vector3i
        ): InteractionState {
            val slot = containerComponent.slots.getOrNull(slotIndex) ?: return InteractionState.Failed
            val slotDisplays = displaysComponent.slotsDisplays.getOrNull(slotIndex) ?: return InteractionState.Failed
            val prevQt = slot.storedQuantity
            
            if (slot.storedQuantity == slot.capacity && slot.capacity > 0) return InteractionState.Failed
            
            val world = ref.store.externalData.world
            val rot = Vector3f()
            rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat()

            if (ItemStack.isEmpty(slot.storedItem)) {
                slot.storedItem = item.withQuantity(1)!!
                slot.capacity = (item.item.maxStack *
                        (36 * (upgradableComponent?.tiers?.getOrNull(upgradableComponent.tier)?.multiplier ?: 1) /
                            containerComponent.slots.size)).toLong()
                slot.storedQuantity = item.quantity.toLong()
                itemContainer?.setItemStackForSlot(itemSlot, ItemStack.EMPTY)

                world.execute {
                    val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.displayOffset)
                    slotDisplays.displayEntity = DisplayUtils.spawnDisplayEntity(
                        world.entityStore.store,
                        displayPos,
                        rot,
                        slot.displaysTransform.displayScale,
                        slot.storedItem
                    )
                }
            } else {
                if (item.isStackableWith(slot.storedItem)) {
                    val insertCount = minOf(slot.capacity - slot.storedQuantity, item.quantity.toLong()).toInt()
                    slot.storedQuantity += insertCount
                    val remaining = item.quantity - insertCount
                    if (remaining <= 0) {
                        itemContainer?.setItemStackForSlot(itemSlot, ItemStack.EMPTY)
                    } else {
                        itemContainer?.setItemStackForSlot(itemSlot, item.withQuantity(remaining))
                    }
                }
            }
            
            val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.numberOffset)
            world.execute {
                DisplayUtils.renderNumber(
                    prevQt,
                    slot.storedQuantity,
                    slotDisplays,
                    numberPos,
                    rot,
                    slot.displaysTransform.numberScale,
                    world,
                    world.entityStore.store
                )
            }

            ref.store.replaceComponent(ref, getComponentType(), containerComponent)

            return InteractionState.Finished
        }
        
        fun extractItem(
            ref: Ref<ChunkStore?>,
            component: DrawerSlotsContainerComponent,
            displaysComponent: DrawerDisplayComponent,
            slotIndex: Int,
            cmdBuffer: CommandBuffer<EntityStore?>,
            player: Player,
            state: MovementStatesComponent?,
            ctx: InteractionContext,
            pos: Vector3i
        ): InteractionState {
            val slot = component.slots.getOrNull(slotIndex) ?: return InteractionState.Failed
            val slotDisplays = displaysComponent.slotsDisplays.getOrNull(slotIndex) ?: return InteractionState.Failed
            val isLocked = ref.store.getComponent(ref, DrawerLockComponent.getComponentType()) != null
            
            if (ItemStack.isEmpty(slot.storedItem) || slot.storedQuantity <= 0) {
                return InteractionState.Failed
            }

            val prevQt = slot.storedQuantity
            var extractQt = if (state?.movementStates?.crouching == true) slot.storedItem.item.maxStack.toLong() else 1L
            extractQt = minOf(extractQt, slot.storedQuantity)
            slot.storedQuantity -= extractQt
            var item = slot.storedItem.withQuantity(extractQt.toInt())!!

            if (slot.storedQuantity <= 0 && !isLocked) {
                slot.storedItem = ItemStack.EMPTY
                slot.storedQuantity = 0
                slot.capacity = 0

                val world = ref.store.externalData.world
                world.execute {
                    DisplayUtils.clearDisplaySlot(slotDisplays, cmdBuffer.store)
                }
            } else {
                val world = ref.store.externalData.world
                val rot = Vector3f()
                rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat()
                val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.numberOffset)
                world.execute {
                    DisplayUtils.renderNumber(
                        prevQt,
                        slot.storedQuantity,
                        slotDisplays,
                        numberPos,
                        rot,
                        slot.displaysTransform.numberScale,
                        world,
                        world.entityStore.store
                    )
                }
            }

            ref.store.replaceComponent(ref, getComponentType(), component)
            
            val soundId = SoundEvent.getAssetMap().getIndex("SFX_Player_Pickup_Item")

            if (ctx.heldItem == null) {
                ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item)
                SoundUtil.playSoundEvent3d(player.reference, soundId, pos.toVector3d(), cmdBuffer)
                return InteractionState.Finished
            }
            
            if (ctx.heldItem!!.isStackableWith(item)) {
                var qt = ctx.heldItem!!.quantity + extractQt
                val addQt = minOf(qt, item.item.maxStack.toLong())
                qt -= addQt
                ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item.withQuantity(addQt.toInt()))

                if (qt <= 0) {
                    SoundUtil.playSoundEvent3d(player.reference, soundId, pos.toVector3d(), cmdBuffer)
                    return InteractionState.Finished
                } else item = item.withQuantity(qt.toInt())!!
            }

            val transactionHotbar = player.inventory.hotbar.addItemStack(item)
            val remainder = transactionHotbar.remainder
            if (!ItemStack.isEmpty(remainder)) {
                cmdBuffer.run {
                    SimpleItemContainer.addOrDropItemStack(
                        it,
                        player.reference!!,
                        player.inventory.storage,
                        remainder!!
                    )
                }
            }

            SoundUtil.playSoundEvent3d(player.reference, soundId, pos.toVector3d(), cmdBuffer)
            return InteractionState.Finished
        }
    }
    
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
            val containerComponent = blockRef.store.getComponent(blockRef, getComponentType())
                ?: return

            if (player.gameMode != GameMode.Creative) {
                val upgradeComponent = blockRef.store.getComponent(blockRef, DrawerUpgradableComponent.getComponentType())
                val isLocked = blockRef.store.getComponent(blockRef, DrawerLockComponent.getComponentType()) != null
                val metadata = BsonDocument()
                containerComponent.slots.forEachIndexed { idx, slot ->
                    if (!ItemStack.isEmpty(slot.storedItem)) {
                        val slotBson = BsonDocument()
                        slotBson.put("StoredItem", ItemStack.CODEC.encode(slot.storedItem))
                        slotBson.put("StoredQuantity", BsonInt32(slot.storedQuantity.toInt()))
                        slotBson.put("Capacity", BsonInt32(slot.capacity.toInt()))
                        metadata.put("Slot-$idx", slotBson)
                    }
                }
                if (upgradeComponent != null) {
                    if (upgradeComponent.tier > 0) {
                        metadata.put("Tier", BsonInt32(upgradeComponent.tier))
                    }
                }
                if (isLocked) {
                    metadata.put("Locked", BsonBoolean(true))
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

            val displayComponent = blockRef.store.getComponent(blockRef, DrawerDisplayComponent.getComponentType())!!
            cmdBuffer.run { store ->
                if (displayComponent.iconDisplays["Lock"] != null) {
                    DisplayUtils.removeDisplayEntity(store, displayComponent.iconDisplays["Lock"]!!)
                }
                for (slot in displayComponent.slotsDisplays) {
                    DisplayUtils.clearDisplaySlot(slot, store)
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

            BlockType.getAssetMap().getAsset(heldItem.itemId)?.blockEntity?.getComponent(getComponentType()) ?: return
            
            cmdBuffer.run {
                val pos = event.targetBlock
                val blockEntity = BlockModule.getBlockEntity(it.externalData.world, pos.x, pos.y, pos.z)!!
                val containerComponent = blockEntity.store.getComponent(blockEntity, getComponentType())!!
                val displayComponent = blockEntity.store.ensureAndGetComponent(blockEntity, DrawerDisplayComponent.getComponentType())
                val upgradeComponent = blockEntity.store.getComponent(blockEntity, DrawerUpgradableComponent.getComponentType())
                blockEntity.store.ensureComponent(blockEntity, DrawerSlotHitComponent.getComponentType())
                
                displayComponent.slotsDisplays = Array(containerComponent.slots.size) {
                    DrawerDisplayComponent.SlotDisplays()
                }
                
                if (heldItem.metadata == null) return@run
                
                val rot = Vector3f()
                rot.yaw = event.rotation.yaw.radians.toFloat()
                
                if (heldItem.metadata!!["Locked"] != null) {
                    blockEntity.store.addComponent(blockEntity, DrawerLockComponent.getComponentType(), DrawerLockComponent.instance)

                    val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, Vector3d(0.0, 0.4453125, -0.0621))
                    displayComponent.iconDisplays["Lock"] = DisplayUtils.spawnIcon(cmdBuffer.store, displayPos, rot, 0.25f, "Lock")
                }
                
                for (i in containerComponent.slots.indices) {
                    val slot = containerComponent.slots[i]
                    val slotData = heldItem.metadata!!["Slot-$i"]?.asDocument() ?: continue
                    
                    slot.storedQuantity = slotData["StoredQuantity"]?.asInt32()?.value?.toLong()!!
                    slot.capacity = slotData["Capacity"]?.asInt32()?.value?.toLong()!!
                    val storedItemData = slotData["StoredItem"]?.asDocument()
                    slot.storedItem = ItemStack.CODEC.decode(storedItemData)!!

                    val slotDisplays = displayComponent.slotsDisplays[i]
                    val displaysTransform = slot.displaysTransform
                    val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, displaysTransform.displayOffset)
                    slotDisplays.displayEntity = DisplayUtils.spawnDisplayEntity(store, displayPos, rot, displaysTransform.displayScale, slot.storedItem)
                    val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, displaysTransform.numberOffset)
                    DisplayUtils.renderNumber(null, slot.storedQuantity, slotDisplays, numberPos, rot, displaysTransform.numberScale, store.externalData.world, store)
                }
                
                // Legacy container component backward compatibility
                if (heldItem.metadata!!.get("StoredItem") != null) {
                    val data = heldItem.metadata!!
                    val slot = containerComponent.slots[0]

                    val storedItemData = data["StoredItem"]?.asDocument()
                    if (storedItemData != null) {
                        slot.storedItem = ItemStack.CODEC.decode(storedItemData) ?: ItemStack.EMPTY
                        slot.storedQuantity = data["StoredQuantity"]?.asInt32()?.value?.toLong() ?: 0L
                        slot.capacity = data["Capacity"]?.asInt32()?.value?.toLong() ?: 0L

                        val slotDisplays = displayComponent.slotsDisplays[0]
                        val displaysTransform = slot.displaysTransform
                        val rot = Vector3f()
                        rot.yaw = event.rotation.yaw.radians.toFloat()
                        val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, displaysTransform.displayOffset)
                        slotDisplays.displayEntity = DisplayUtils.spawnDisplayEntity(store, displayPos, rot, displaysTransform.displayScale, slot.storedItem)
                        val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, displaysTransform.numberOffset)
                        DisplayUtils.renderNumber(null, slot.storedQuantity, slotDisplays, numberPos, rot, displaysTransform.numberScale, store.externalData.world, store)
                    }
                }
                //
                
                if (upgradeComponent != null) {
                    val data = heldItem.metadata
                    if (data != null) {
                        upgradeComponent.tier = data["Tier"]?.asInt32()?.value ?: 0
                    }
                }
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
    
    class DrawerMigrateSystem : EntityTickingSystem<ChunkStore?>() {
        override fun tick(
            dt: Float,
            index: Int,
            chunk: ArchetypeChunk<ChunkStore?>,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>
        ) {
            val ref = chunk.getReferenceTo(index)
            val newContainer = cmdBuffer.ensureAndGetComponent(ref, getComponentType())
            val legacyContainer = cmdBuffer.getComponent(ref, DrawerContainerComponent.getComponentType())
            newContainer.slots = Array(1) {
                DrawerSlotsContainerComponent.DrawerSlot()
            }
            val slot = newContainer.slots[0]
            slot.storedItem = legacyContainer?.storedItem ?: ItemStack.EMPTY
            slot.storedQuantity = (legacyContainer?.storedQuantity ?: 0).toLong()
            slot.capacity = (legacyContainer?.capacity ?: 0).toLong()
            val displayTransform = slot.displaysTransform
            displayTransform.displayOffset = Vector3d(0.0, -0.125, 0.0)
            displayTransform.numberOffset = Vector3d(0.0, -0.35, 0.0)
            cmdBuffer.removeComponent(ref, DrawerContainerComponent.getComponentType())
            
            val newDisplays = cmdBuffer.ensureAndGetComponent(ref, DrawerDisplayComponent.getComponentType())
            val legacyDisplays = cmdBuffer.getComponent(ref, DrawerBoundDisplayComponent.getComponentType())
            newDisplays.slotsDisplays = Array(1) {
                DrawerDisplayComponent.SlotDisplays()
            }
            val slotDisplays = newDisplays.slotsDisplays[0]
            slotDisplays.displayEntity = legacyDisplays?.displayEntity
            slotDisplays.numberDisplays = legacyDisplays?.numberDisplays?.toMutableList() ?: mutableListOf(null)
            cmdBuffer.removeComponent(ref, DrawerBoundDisplayComponent.getComponentType())
            
            cmdBuffer.ensureComponent(ref, DrawerSlotHitComponent.getComponentType())
        }

        override fun getQuery(): Query<ChunkStore?>? {
            return Query.and(DrawerContainerComponent.getComponentType())
        }
    }
}