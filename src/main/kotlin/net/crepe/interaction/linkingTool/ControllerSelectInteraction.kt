package net.crepe.interaction.linkingTool

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.system.DrawerLinkSystem
import org.bson.BsonDocument

class ControllerSelectInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerSelectInteraction::class.java, ::ControllerSelectInteraction,
            SimpleBlockInteraction.CODEC).build()
        
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }

    protected override fun tick0(
        firstRun: Boolean,
        time: Float,
        type: InteractionType,
        ctx: InteractionContext,
        cooldownHandler: CooldownHandler
    ) {
        super.tick0(firstRun, time, type, ctx, cooldownHandler)
        if (firstRun && ctx.state.state == InteractionState.Failed) {
            val item = ctx.heldItem

            if (ItemStack.isEmpty(item)) return

            val player = ctx.commandBuffer?.getComponent(ctx.entity, PlayerRef.getComponentType()) ?: return
            val data = item!!.metadata
            
            if (data?.get("SelectedBlock") != null) {
                DrawerLinkSystem.removeWireframes(player)

                val newItem = ItemStack(item.itemId, item.quantity, null)
                ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), newItem)
            }
        }
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
        if (ItemStack.isEmpty(item)) return
       
        val player = cmdBuffer.getComponent(ctx.entity, PlayerRef.getComponentType()) ?: return
        val data = item!!.metadata ?: BsonDocument()
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
        val store = blockRef?.store
        val linksComponent = store?.getComponent(blockRef, ControllerLinksComponent.getComponentType())
        if (linksComponent != null) {
            if (data["SelectedBlock"] != null) {
                DrawerLinkSystem.removeWireframes(player)
            }
            
            data["SelectedBlock"] = Vector3i.CODEC.encode(pos)
            DrawerLinkSystem.renderWireframes(player, pos, world)
            val newItem = ItemStack(item.itemId, item.quantity, data)
            ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), newItem)
        } else {
            if (data["SelectedBlock"] != null) {
                DrawerLinkSystem.removeWireframes(player)
            }
            val newItem = ItemStack(item.itemId, item.quantity, null)
            ctx.heldItemContainer?.setItemStackForSlot(ctx.heldItemSlot.toShort(), newItem)
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