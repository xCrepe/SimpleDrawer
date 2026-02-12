package net.crepe.interaction.drawer

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.drawer.DrawerSlotHitComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent.Companion.getComponentType
import net.crepe.system.DrawerSystem
import net.crepe.system.DrawerSystem.Companion.updateDisplay

class DrawerQuickStackInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerQuickStackInteraction::class.java, ::DrawerQuickStackInteraction, SimpleBlockInteraction.CODEC)
            .build()
    }
    
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
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
        if (blockRef == null) {
            ctx.state.state = InteractionState.Failed
            return
        }
        val containerComponent = blockRef.store.getComponent(blockRef, getComponentType()) ?: return
        val slotHitComponent = blockRef.store.getComponent(blockRef, DrawerSlotHitComponent.getComponentType())!!
        
        if (player == null) {
            ctx.state.state = InteractionState.Failed
            return
        }

        DrawerSystem.quickStack(
            blockRef,
            pos,
            slotHitComponent.hitIndex!!,
            player,
        )

        world.execute {
            updateDisplay(blockRef, slotHitComponent.hitIndex!!, pos)
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