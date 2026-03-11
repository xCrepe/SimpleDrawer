package net.crepe.interaction.drawer

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.page.DrawerUpgradePage


class DrawerUpgradeInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerUpgradeInteraction::class.java, ::DrawerUpgradeInteraction, SimpleBlockInteraction.CODEC)
            .build()
    }
    
    val LOGGER = HytaleLogger.forEnclosingClass()
    
    override fun interactWithBlock(
        world: World,
        cmdBuffer: CommandBuffer<EntityStore?>,
        type: InteractionType,
        ctx: InteractionContext,
        item: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        try {
            Class.forName("au.ellie.hyui.builders.PageBuilder")
        } catch (e: ClassNotFoundException) {
            world.sendMessage(Message.raw("[SimpleDrawer] HyUI not found! Please install HyUI to use the drawer upgrade feature."))
            LOGGER.atWarning().log("PageBuilder class not found! HyUI library may be missing.")
            return
        }
        
        val ref = ctx.entity
        val player = cmdBuffer.getComponent(ref, Player.getComponentType()) ?: return
        val playerRef = cmdBuffer.getComponent(ref, PlayerRef.getComponentType())!!
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return

        DrawerUpgradePage().forPlayer(player).forBlock(blockRef).open(playerRef, cmdBuffer.store)
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