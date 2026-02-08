package net.crepe.interaction

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.DrawerDisplayComponent
import net.crepe.component.DrawerSlotHitComponent
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.component.DrawerSlotsContainerComponent.Companion.getComponentType
import net.crepe.system.DrawerSystem

class DrawerIOInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerIOInteraction::class.java, ::DrawerIOInteraction, SimpleBlockInteraction.CODEC)
            .append(KeyedCodec("Action", EnumCodec(DrawerAction::class.java)),
                { o, v -> o.action = v },
                { o -> o.action }).add()
            .build()
        
        enum class DrawerAction {
            Insert,
            Extract
        }
    }
    
    val logger = HytaleLogger.forEnclosingClass()
    private var action: DrawerAction = DrawerAction.Extract
    
    override fun interactWithBlock(
        world: World,
        cmdBuffer: CommandBuffer<EntityStore?>,
        type: InteractionType,
        ctx: InteractionContext,
        item: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        val ref = ctx.entity
        val player = cmdBuffer.getComponent(ref, Player.getComponentType())
        val movementState = cmdBuffer.getComponent(ref, MovementStatesComponent.getComponentType())
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
        if (blockRef == null) {
            ctx.state.state = InteractionState.Failed
            return
        }
        val containerComponent = blockRef.store.getComponent(blockRef, getComponentType()) ?: return
        val slotHitComponent = blockRef.store.getComponent(blockRef, DrawerSlotHitComponent.getComponentType())!!
        val slotsDisplaysComponent = blockRef.store.getComponent(blockRef, DrawerDisplayComponent.getComponentType())!!
        val upgradableComponent = blockRef.store.getComponent(blockRef, DrawerUpgradableComponent.getComponentType())
        
        if (player == null) {
            ctx.state.state = InteractionState.Failed
            return
        }
        
        slotHitComponent.hitIndex ?: return
        when (action) {
            DrawerAction.Insert -> if (item != null) DrawerSystem.insertItem(
                blockRef,
                containerComponent,
                slotsDisplaysComponent,
                upgradableComponent,
                slotHitComponent.hitIndex!!,
                ctx,
                item,
                pos
            )
            DrawerAction.Extract -> {
                ctx.state.state = DrawerSystem.extractItem(
                    blockRef,
                    containerComponent,
                    slotsDisplaysComponent,
                    slotHitComponent.hitIndex!!,
                    cmdBuffer,
                    player,
                    movementState,
                    ctx,
                    pos
                )
            }
        }
    }

//    @Deprecated("Legacy drawer insert code for backward compatibility")
//    fun insertItem(
//        blockRef: Ref<ChunkStore?>,
//        component: DrawerContainerLegacyComponent,
//        upgradeComponent: DrawerUpgradableComponent?,
//        ctx: InteractionContext,
//        item: ItemStack,
//        pos: Vector3i,
//    ) {
//        val prevQt = component.storedQuantity
//        
//        if (component.storedItem.itemId == "Empty") {
//            component.storedItem = item.withQuantity(1)!!
//            component.capacity = item.item.maxStack * 36 * (upgradeComponent?.tiers?.getOrNull(upgradeComponent.tier)?.multiplier ?: 1)
//            component.storedQuantity = item.quantity
//            ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), ItemStack.EMPTY)
//            
//            val world = blockRef.store.externalData.world
//            val rot = Vector3f()
//            rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat() + Math.PI.toFloat()
//            world.execute {
//                val uuid = DisplayUtils.spawnDisplayEntity(world.entityStore.store, pos, rot, component.storedItem)
//                blockRef.store.getComponent(blockRef, DrawerBoundDisplayComponent.getComponentType())?.displayEntity = uuid
//            }
//        } else {
//            if (item.isStackableWith(component.storedItem)) {
//                val insertCount = minOf(component.capacity - component.storedQuantity, item.quantity)
//                component.storedQuantity += insertCount
//                val remaining = item.quantity - insertCount
//                if (remaining <= 0) {
//                    ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), ItemStack.EMPTY)
//                } else {
//                    ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item.withQuantity(remaining))
//                }
//            }
//        }
//
//        blockRef.store.getComponent(blockRef, DrawerBoundDisplayComponent.getComponentType())?.let {
//            val world = blockRef.store.externalData.world
//            val rot = Vector3f()
//            rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat() + Math.PI.toFloat()
//            val forwardVector = Vector3d(0.0, 0.0, -0.438).rotateY(rot.yaw)
//            val numberPos = pos.toVector3d().add(Vector3d(0.5, 0.15, 0.5).add(forwardVector))
//            world.execute {
//                DisplayUtils.renderNumber(
//                    prevQt,
//                    component.storedQuantity,
//                    it,
//                    numberPos,
//                    rot,
//                    world,
//                    world.entityStore.store
//                )
//            }
//        }
//        
//        blockRef.store.replaceComponent(blockRef, DrawerContainerLegacyComponent.getComponentType() ,component)
//    }
//
//    @Deprecated("Legacy drawer extract code for backward compatibility")
//    fun extractItem(
//        blockRef: Ref<ChunkStore?>,
//        component: DrawerContainerLegacyComponent,
//        cmdBuffer: CommandBuffer<EntityStore?>,
//        player: Player,
//        state: MovementStatesComponent?,
//        ctx: InteractionContext,
//        pos: Vector3i
//    ) {
//        if (component.storedItem == ItemStack.EMPTY || component.storedQuantity <= 0) {
//            return
//        }
//        val prevQt = component.storedQuantity
//        var extractQuantity = if (state?.movementStates?.crouching == true) component.storedItem.item.maxStack else 1
//        extractQuantity = minOf(extractQuantity, component.storedQuantity)
//        component.storedQuantity -= extractQuantity
//        var item = component.storedItem.withQuantity(extractQuantity)!!
//
//        if (component.storedQuantity <= 0) {
//            component.storedItem = ItemStack.EMPTY
//            component.storedQuantity = 0
//            component.capacity = 0
//            
//            val boundDisplay = blockRef.store.getComponent(blockRef, DrawerBoundDisplayComponent.getComponentType())
//            if (boundDisplay != null) {
//                val world = blockRef.store.externalData.world
//                world.execute {
//                    if (boundDisplay.displayEntity != null)
//                        DisplayUtils.removeDisplayEntity(cmdBuffer.store, boundDisplay.displayEntity!!)
//                    if (boundDisplay.numberDisplays.isNotEmpty()) {
//                        for (uuid in boundDisplay.numberDisplays) {
//                            if (uuid != null) {
//                                DisplayUtils.removeDisplayEntity(cmdBuffer.store, uuid)
//                            }
//                        }
//                        boundDisplay.numberDisplays.clear()
//                        boundDisplay.numberDisplays.add(null)
//                    }
//                }
//            }
//        } else {
//            blockRef.store.getComponent(blockRef, DrawerBoundDisplayComponent.getComponentType())?.let {
//                val world = blockRef.store.externalData.world
//                val rot = Vector3f()
//                rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat() + Math.PI.toFloat()
//                val forwardVector = Vector3d(0.0, 0.0, -0.438).rotateY(rot.yaw)
//                val numberPos = pos.toVector3d().add(Vector3d(0.5, 0.15, 0.5).add(forwardVector))
//                world.execute {
//                    DisplayUtils.renderNumber(
//                        prevQt,
//                        component.storedQuantity,
//                        it,
//                        numberPos,
//                        rot,
//                        world,
//                        world.entityStore.store
//                    )
//                }
//            }
//        }
//
//        blockRef.store.replaceComponent(blockRef, DrawerContainerLegacyComponent.getComponentType(), component)
//
//        if (ctx.heldItem == null) {
//            ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item)
//            return
//        }
//
//        if (ctx.heldItem!!.isStackableWith(item)) {
//            var qt = ctx.heldItem!!.quantity + extractQuantity
//            val addQt = minOf(qt, item.item.maxStack)
//            qt -= addQt
//            ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), item.withQuantity(addQt))
//
//            if (qt <= 0) return
//            else item = item.withQuantity(qt)!!
//        }
//
//        val transactionHotbar = player.inventory.hotbar.addItemStack(item)
//        val remainder = transactionHotbar.remainder
//        if (!ItemStack.isEmpty(remainder)) {
//            cmdBuffer.run {
//                SimpleItemContainer.addOrDropItemStack(
//                    it,
//                    player.reference!!,
//                    player.inventory.storage,
//                    remainder!!
//                )
//            }
//        }
//    }

    override fun simulateInteractWithBlock(
        p0: InteractionType,
        p1: InteractionContext,
        p2: ItemStack?,
        p3: World,
        p4: Vector3i
    ) {
    }
}