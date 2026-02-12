package net.crepe.interaction.drawer

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
import net.crepe.component.drawer.DrawerDisplayComponent
import net.crepe.component.drawer.DrawerSlotHitComponent
import net.crepe.component.drawer.DrawerUpgradableComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent.Companion.getComponentType
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
        
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }
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
            ctx.state.state = InteractionState.Skip
            return
        }
        val containerComponent = blockRef.store.getComponent(blockRef, getComponentType()) ?: return
        val slotHitComponent = blockRef.store.getComponent(blockRef, DrawerSlotHitComponent.getComponentType())!!
        
        if (player == null) {
            ctx.state.state = InteractionState.Skip
            return
        }
        
        slotHitComponent.hitIndex ?: return
        when (action) {
            DrawerAction.Insert -> if (item != null) {
                DrawerSystem.insertItem(
                    blockRef,
                    slotHitComponent.hitIndex!!,
                    item,
                    ctx.heldItemSlot.toShort(),
                    ctx.heldItemContainer,
                    pos
                )
            } else if (!ItemStack.isEmpty(containerComponent.slots[slotHitComponent.hitIndex!!].storedItem)) {
                ctx.state.state = InteractionState.Failed
            }
            DrawerAction.Extract -> {
                ctx.state.state = DrawerSystem.extractItem(
                    blockRef,
                    slotHitComponent.hitIndex!!,
                    cmdBuffer,
                    player,
                    movementState,
                    ctx,
                    pos
                )
            }
        }
        world.execute {
            DrawerSystem.updateDisplay(blockRef, slotHitComponent.hitIndex!!, pos)
        }
    }

    override fun simulateInteractWithBlock(
        p0: InteractionType,
        p1: InteractionContext,
        p2: ItemStack?,
        p3: World,
        p4: Vector3i
    ) {
    }
}