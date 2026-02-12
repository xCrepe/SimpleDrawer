package net.crepe.interaction.controller

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.system.DrawerSystem

class ControllerInsertInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerInsertInteraction::class.java, ::ControllerInsertInteraction, SimpleBlockInteraction.CODEC)
            .build()
        
        val log = { log: String -> com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atInfo().log(log) }
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
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
        val component = blockRef?.store?.getComponent(blockRef, ControllerLinksComponent.Companion.getComponentType()) ?: return
        
        var remainingItem = item
        if (ItemStack.isEmpty(remainingItem)) {
            ctx.state.state = InteractionState.Failed
            return
        }
        component.containers.forEachIndexed { idx, container ->
            if (ItemStack.isEmpty(remainingItem)) return@forEachIndexed
            val drawerPos = component.drawers[idx]
            val drawerRef = BlockModule.getBlockEntity(world, drawerPos.x, drawerPos.y, drawerPos.z) ?: return@forEachIndexed
            
            container.slots.forEachIndexed { idx2, slot ->
                if (ItemStack.isEmpty(remainingItem)) return@forEachIndexed
                if (!remainingItem!!.isStackableWith(slot.storedItem)) return@forEachIndexed

                remainingItem = DrawerSystem.insertItem(
                    drawerRef,
                    idx2,
                    remainingItem!!,
                    ctx.heldItemSlot.toShort(),
                    ctx.heldItemContainer,
                    drawerPos
                )
                
                world.execute {
                    DrawerSystem.updateDisplay(drawerRef, idx2, drawerPos)
                }
            }
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