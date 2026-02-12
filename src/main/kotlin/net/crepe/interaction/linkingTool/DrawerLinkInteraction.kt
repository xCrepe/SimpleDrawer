package net.crepe.interaction.linkingTool

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
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
import net.crepe.component.drawer.DrawerLinkedComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.system.DrawerLinkSystem
import net.crepe.utils.isWithinBox

class DrawerLinkInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerLinkInteraction::class.java, ::DrawerLinkInteraction, SimpleBlockInteraction.CODEC)
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
        val player = cmdBuffer.getComponent(ctx.entity, PlayerRef.getComponentType()) ?: return
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
        val linkedComponent = blockRef?.store?.getComponent(blockRef, DrawerLinkedComponent.getComponentType())

        if (ItemStack.isEmpty(item)) return
        
        val data = item!!.metadata?.get("SelectedBlock")?.asDocument() ?: return
        val containerComponent = blockRef?.store?.getComponent(blockRef, DrawerSlotsContainerComponent.getComponentType()) ?: return
        
        val controllerPos = Vector3i.CODEC.decode(data)!!
        val controllerRef = BlockModule.getBlockEntity(world, controllerPos.x, controllerPos.y, controllerPos.z) ?: return
        val linksComponent = blockRef.store.getComponent(controllerRef, ControllerLinksComponent.getComponentType()) ?: return
        
        if (linksComponent.drawers.contains(pos)) {
            val index = linksComponent.drawers.indexOf(pos)
            linksComponent.drawers.removeAt(index)
            linksComponent.containers.removeAt(index)
            blockRef.store.removeComponent(blockRef, DrawerLinkedComponent.getComponentType())
            DrawerLinkSystem.removeWireframes(player)
            DrawerLinkSystem.renderWireframes(player, controllerPos, world)
        } else {
            if (!pos.toVector3d().isWithinBox(controllerPos.toVector3d(), linksComponent.radius.toDouble()) || linkedComponent != null) return
            
            linksComponent.drawers.add(pos)
            linksComponent.containers.add(containerComponent)
            blockRef.store.addComponent(blockRef, DrawerLinkedComponent.getComponentType(), DrawerLinkedComponent(controllerPos))
            DrawerLinkSystem.addWireframe(player, pos.toVector3d().add(0.5, 0.5, 0.5), 1.0, Vector3f(0f, 1f ,0f))
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