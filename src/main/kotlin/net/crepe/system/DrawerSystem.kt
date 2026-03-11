package net.crepe.system

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.component.system.RefSystem
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
import com.hypixel.hytale.server.core.universe.world.meta.BlockState
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.common.DataItem
import net.crepe.component.drawer.DrawerDisplayComponent
import net.crepe.component.drawer.DrawerLockComponent
import net.crepe.component.drawer.DrawerSlotHitComponent
import net.crepe.component.drawer.DrawerUpgradableComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent.Companion.getComponentType
import net.crepe.component.drawer.DrawerUpgradesComponent
import net.crepe.utils.BlockUtils
import net.crepe.utils.DisplayUtils
import net.crepe.inventory.DrawerContainerWrapper
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString

class DrawerSystem {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        fun quickStack(
            ref: Ref<ChunkStore?>,
            pos: Vector3i,
            slotIndex: Int,
            player: Player,
        ) {
            val containerComponent = ref.store.getComponent(ref, getComponentType()) ?: return
            
            val inventory = player.inventory
            val drawerSlot = containerComponent.slots.getOrNull(slotIndex) ?: return
            
            inventory.hotbar.forEach { slot, item ->
                if (ItemStack.isEmpty(item)) return@forEach

                if (item.isStackableWith(drawerSlot.storedItem)) {
                    insertItem(
                        ref,
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

                if (item.isStackableWith(drawerSlot.storedItem)) {
                    insertItem(
                        ref,
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
            slotIndex: Int,
            item: ItemStack,
            itemSlot: Short,
            itemContainer: ItemContainer?,
            pos: Vector3i
        ): ItemStack {
            val containerComponent = ref.store.getComponent(ref, getComponentType()) ?: return item
            val upgradesComponent = ref.store.getComponent(ref, DrawerUpgradesComponent.getComponentType())
            
            val slot = containerComponent.slots.getOrNull(slotIndex) ?: return item
            if (slot.storedQuantity == slot.capacity && slot.capacity > 0) {
                if (upgradesComponent?.void == true) {
                    itemContainer?.setItemStackForSlot(itemSlot, ItemStack.EMPTY)
                    return ItemStack.EMPTY
                }
                return item
            }
            
            var remainingItem = item

            if (ItemStack.isEmpty(slot.storedItem)) {
                slot.storedItem = item.withQuantity(1)!!
                containerComponent.setCapacityForSlot(slotIndex, upgradesComponent?.multiplier)
                
                val insertCount = minOf(item.quantity, slot.capacity)
                if (insertCount > 0) {
                    slot.storedQuantity += insertCount
                    
                    val remaining = item.quantity - insertCount
                    if (remaining > 0) {
                        remainingItem = item.withQuantity(remaining)!!
                        itemContainer?.setItemStackForSlot(itemSlot, remainingItem)
                    } else {
                        itemContainer?.setItemStackForSlot(itemSlot, ItemStack.EMPTY)
                        remainingItem = ItemStack.EMPTY
                    }
                } else {
                    slot.storedItem = ItemStack.EMPTY
                }
            } else {
                if (item.isStackableWith(slot.storedItem)) {
                    val insertCount = minOf(slot.capacity - slot.storedQuantity, item.quantity)
                    slot.storedQuantity += insertCount
                    val remaining = item.quantity - insertCount
                    if (remaining <= 0 || (upgradesComponent?.void == true && slot.storedQuantity == slot.capacity)) {
                        itemContainer?.setItemStackForSlot(itemSlot, ItemStack.EMPTY)
                        remainingItem = ItemStack.EMPTY
                    } else {
                        remainingItem = item.withQuantity(remaining)!!
                        itemContainer?.setItemStackForSlot(itemSlot, remainingItem)
                    }
                }
            }

            BlockUtils.saveBlock(ref)

            return remainingItem
        }
        
        fun extractItem(
            ref: Ref<ChunkStore?>,
            slotIndex: Int,
            cmdBuffer: CommandBuffer<EntityStore?>,
            player: Player,
            state: MovementStatesComponent?,
            ctx: InteractionContext,
            pos: Vector3i
        ): InteractionState {
            val component = ref.store.getComponent(ref, getComponentType()) ?: return InteractionState.Failed
            
            val slot = component.slots.getOrNull(slotIndex) ?: return InteractionState.Failed
            val isLocked = ref.store.getComponent(ref, DrawerLockComponent.getComponentType()) != null
            
            if (ItemStack.isEmpty(slot.storedItem) || slot.storedQuantity <= 0) {
                return InteractionState.Failed
            }

            var extractQt = if (state?.movementStates?.crouching == true) slot.storedItem.item.maxStack else 1
            extractQt = minOf(extractQt, slot.storedQuantity)
            slot.storedQuantity -= extractQt
            var item = slot.storedItem.withQuantity(extractQt)!!

            if (slot.storedQuantity <= 0 && !isLocked) {
                slot.storedItem = ItemStack.EMPTY
                slot.storedQuantity = 0
                slot.capacity = 0
            }

            BlockUtils.saveBlock(ref)
            
            val soundId = SoundEvent.getAssetMap().getIndex("SFX_Player_Pickup_Item")

            if (ctx.heldItem == null) {
                ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item)
                SoundUtil.playSoundEvent3d(player.reference, soundId, pos.toVector3d(), cmdBuffer)
                return InteractionState.Finished
            }
            
            if (ctx.heldItem!!.isStackableWith(item)) {
                var qt = ctx.heldItem!!.quantity + extractQt
                val addQt = minOf(qt, item.item.maxStack)
                qt -= addQt
                ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item.withQuantity(addQt))

                if (qt <= 0) {
                    SoundUtil.playSoundEvent3d(player.reference, soundId, pos.toVector3d(), cmdBuffer)
                    return InteractionState.Finished
                } else item = item.withQuantity(qt)!!
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

        fun updateDisplay(
            ref: Ref<ChunkStore?>,
            slotIndex: Int,
            pos: Vector3i
        ) {
            val store = ref.store
            val containerComponent = store.getComponent(ref, getComponentType()) ?: return
            val displaysComponent = store.getComponent(ref, DrawerDisplayComponent.getComponentType())!!
            val slot = containerComponent.slots.getOrNull(slotIndex) ?: return
            val slotDisplays = displaysComponent.slotsDisplays.getOrNull(slotIndex) ?: return
            val isLocked = store.getComponent(ref, DrawerLockComponent.getComponentType()) != null

            val world = store.externalData.world
            val entityStore = world.entityStore.store
            val rot = Vector3f()
            rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat()
            
            if (!ItemStack.isEmpty(slot.storedItem) && slotDisplays.displayEntity == null) {
                val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.displayOffset)
                slotDisplays.displayEntity = DisplayUtils.spawnDisplayEntity(
                    entityStore,
                    displayPos,
                    rot,
                    slot.displaysTransform.displayScale,
                    slot.storedItem
                )
            } else if (ItemStack.isEmpty(slot.storedItem) && slotDisplays.displayEntity != null && !isLocked) {
                DisplayUtils.removeDisplayEntity(entityStore, slotDisplays.displayEntity!!)
                slotDisplays.displayEntity = null
            }

            if (slot.storedQuantity > 0 || isLocked) {
                val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.numberOffset)
                DisplayUtils.renderNumber(
                    slotDisplays.currentNumber,
                    slot.storedQuantity,
                    slotDisplays,
                    numberPos,
                    rot,
                    slot.displaysTransform.numberScale,
                    world,
                    entityStore
                )
            } else {
                if (slotDisplays.numberDisplays.isNotEmpty()) {
                    for (uuid in slotDisplays.numberDisplays) {
                        if (uuid != null) {
                            DisplayUtils.removeDisplayEntity(entityStore, uuid)
                        }
                    }
                    slotDisplays.numberDisplays.clear()
                    slotDisplays.numberDisplays.add(null)
                }
            }
            slotDisplays.currentNumber = slot.storedQuantity
        }

        fun clearDisplaySlot(slot: DrawerDisplayComponent.SlotDisplays, store: Store<EntityStore?>) {
            if (slot.displayEntity != null) {
                DisplayUtils.removeDisplayEntity(store, slot.displayEntity!!)
                slot.displayEntity = null
            }
            if (slot.numberDisplays.isNotEmpty()) {
                for (uuid in slot.numberDisplays) {
                    if (uuid != null) {
                        DisplayUtils.removeDisplayEntity(store, uuid)
                    }
                }
                slot.numberDisplays.clear()
                slot.numberDisplays.add(null)
            }
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
                val upgradesComponent = blockRef.store.getComponent(blockRef, DrawerUpgradesComponent.getComponentType())
                val isLocked = blockRef.store.getComponent(blockRef, DrawerLockComponent.getComponentType()) != null
                val metadata = BsonDocument()
                val slotsData = BsonDocument()
                containerComponent.slots.forEachIndexed { idx, slot ->
                    if (!ItemStack.isEmpty(slot.storedItem)) {
                        val slotData = BsonDocument()
                        slotData["StoredItem"] = ItemStack.CODEC.encode(slot.storedItem)
                        slotData["StoredQuantity"] = BsonInt32(slot.storedQuantity)
                        slotData["Capacity"] = BsonInt32(slot.capacity)
                        slotsData["$idx"] = slotData
                    }
                }
                if (slotsData.isNotEmpty()) {
                    metadata["Slots"] = slotsData
                }
                if (upgradesComponent != null && upgradesComponent.upgrades.isNotEmpty() && upgradesComponent.upgrades.any { !DataItem.isEmpty(it) }) {
                    val upgradesData = BsonDocument()
                    upgradesData["Multiplier"] = BsonInt32(upgradesComponent.multiplier)
                    upgradesData["Void"] = BsonBoolean(upgradesComponent.void)
                    upgradesComponent.upgrades.forEachIndexed { index, item ->
                        if (DataItem.isEmpty(item)) return@forEachIndexed

                        val itemData = BsonDocument()
                        itemData["Id"] = BsonString(item!!.itemId)
                        itemData["Quantity"] = BsonInt32(item.quantity)
                        upgradesData["$index"] = itemData
                    }
                    metadata["Upgrades"] = upgradesData
                }
                if (isLocked) {
                    metadata["Locked"] = BsonBoolean(true)
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
                    clearDisplaySlot(slot, store)
                }
            }
        }

        override fun getQuery(): Query<EntityStore?> {
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
                val ref = BlockModule.getBlockEntity(it.externalData.world, pos.x, pos.y, pos.z)!!
                val containerComponent = ref.store.getComponent(ref, getComponentType())!!
                val displayComponent = ref.store.ensureAndGetComponent(ref, DrawerDisplayComponent.getComponentType())
                val upgradesComponent = ref.store.getComponent(ref, DrawerUpgradesComponent.getComponentType())
                ref.store.ensureComponent(ref, DrawerSlotHitComponent.getComponentType())
                
                displayComponent.slotsDisplays = Array(containerComponent.slots.size) {
                    DrawerDisplayComponent.SlotDisplays()
                }
                
                if (heldItem.metadata == null) return@run
                
                val rot = Vector3f()
                rot.yaw = event.rotation.yaw.radians.toFloat()
                
                if (heldItem.metadata!!["Locked"] != null) {
                    ref.store.addComponent(ref, DrawerLockComponent.getComponentType(), DrawerLockComponent.instance)

                    val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, Vector3d(0.0, 0.4453125, -0.0621))
                    displayComponent.iconDisplays["Lock"] = DisplayUtils.spawnIcon(cmdBuffer.store, displayPos, rot, 0.25f, "Lock")
                }
                
                heldItem.metadata!!["Slots"]?.asDocument()?.let { slotsData ->
                    for (i in containerComponent.slots.indices) {
                        val slot = containerComponent.slots[i]
                        val slotData = slotsData["$i"]?.asDocument() ?: continue

                        slot.storedQuantity = slotData["StoredQuantity"]?.asInt32()?.value!!
                        slot.capacity = slotData["Capacity"]?.asInt32()?.value!!
                        val storedItemData = slotData["StoredItem"]?.asDocument()
                        slot.storedItem = ItemStack.CODEC.decode(storedItemData)!!

                        updateDisplay(ref, i, pos)
                    }
                }
                
                heldItem.metadata!!["Upgrades"]?.asDocument()?.let { upgradesData ->
                    upgradesComponent ?: return@run
                    upgradesComponent.multiplier = upgradesData["Multiplier"]?.asInt32()?.value ?: 1
                    upgradesComponent.void = upgradesData["Void"]?.asBoolean()?.value ?: false
                    for (slot in upgradesComponent.upgrades.indices) {
                        val slotData = upgradesData["$slot"]?.asDocument() ?: continue
                        upgradesComponent.upgrades[slot] = DataItem(
                            slotData["Id"]?.asString()?.value,
                            slotData["Quantity"]?.asInt32()?.value ?: 0
                        )
                    }
                }
                
                // Legacy upgrade component backward compatibility
                if (heldItem.metadata!!["Tier"] != null) {
                    val tier = heldItem.metadata!!["Tier"]!!.asInt32()?.value ?: 0
                    if (tier <= 0) return@run
                    
                    when (tier) {
                        1 -> {
                            upgradesComponent?.multiplier = 4
                            upgradesComponent?.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                        }
                        2 -> {
                            upgradesComponent?.multiplier = 16
                            upgradesComponent?.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                        }
                        3 -> {
                            upgradesComponent?.multiplier = 64
                            upgradesComponent?.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[2] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                        }
                        4 -> {
                            upgradesComponent?.multiplier = 256
                            upgradesComponent?.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[2] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                            upgradesComponent?.upgrades[3] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                        }
                    }
                }
                //

                BlockUtils.saveBlock(ref)
            }
        }

        override fun getQuery(): Query<EntityStore?> {
            return Query.and(Player.getComponentType())
        }
    }
    
    class DrawerWrapContainer : RefSystem<ChunkStore?>() {
        override fun onEntityAdded(
            ref: Ref<ChunkStore?>,
            reason: AddReason,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>
        ) {
            val state = BlockState.getBlockState(ref, cmdBuffer)
            if (state is ItemContainerState) {
                if (state.itemContainer !is DrawerContainerWrapper) {
                    val pos = BlockUtils.getPos(ref)!!
                    state.setItemContainer(DrawerContainerWrapper(store.externalData.world.name, pos))
                }
            }
        }

        override fun onEntityRemove(
            p0: Ref<ChunkStore?>,
            p1: RemoveReason,
            p2: Store<ChunkStore?>,
            p3: CommandBuffer<ChunkStore?>
        ) {
        }

        override fun getQuery(): Query<ChunkStore?> {
            return Query.and(DrawerSlotsContainerComponent.getComponentType())
        }

    }
    
    class DrawerUpgradeMigrateSystem : EntityTickingSystem<ChunkStore?>() {
        override fun tick(
            dt: Float,
            index: Int,
            chunk: ArchetypeChunk<ChunkStore?>,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>
        ) {
            val ref = chunk.getReferenceTo(index)
            val newUpgrades = cmdBuffer.ensureAndGetComponent(ref, DrawerUpgradesComponent.getComponentType())
            val legacyUpgrades = cmdBuffer.getComponent(ref, DrawerUpgradableComponent.getComponentType())
            
            newUpgrades.upgrades = MutableList(4) { null }
            
            when (legacyUpgrades?.tier) {
                1 -> {
                    newUpgrades.multiplier = 4
                    newUpgrades.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                }
                2 -> {
                    newUpgrades.multiplier = 16
                    newUpgrades.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                }
                3 -> {
                    newUpgrades.multiplier = 64
                    newUpgrades.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[2] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                }
                4 -> {
                    newUpgrades.multiplier = 256
                    newUpgrades.upgrades[0] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[1] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[2] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                    newUpgrades.upgrades[3] = DataItem("SimpleDrawer_Upgrade_Stack_Thorium", 1)
                }
            }
            cmdBuffer.removeComponent(ref, DrawerUpgradableComponent.getComponentType())

            BlockUtils.saveBlock(ref)
        }

        override fun getQuery(): Query<ChunkStore?> {
            return Query.and(DrawerUpgradableComponent.getComponentType())
        }
    }
}