package net.crepe.packet

import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.Packet
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains
import com.hypixel.hytale.protocol.packets.inventory.MoveItemStack
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher
import com.hypixel.hytale.server.core.universe.PlayerRef
import net.crepe.system.DrawerLinkSystem

class HeldItemChangeHandler : PlayerPacketWatcher {
    companion object {
        val log = { log: String -> com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atInfo().log(log) }
        const val LINK_INTERACTION = "SimpleDrawer_DrawerLink"
    }
    
    override fun accept(playerRef: PlayerRef?, packet: Packet?) {
        when (packet) {
            is SyncInteractionChains -> swapHandler(playerRef, packet)
            is MoveItemStack -> moveItemHandler(playerRef, packet)
        }
    }
    
    private fun swapHandler(playerRef: PlayerRef?, packet: SyncInteractionChains) {
        for (chain in packet.updates) {
            if (chain.interactionType == InteractionType.SwapFrom && chain.data != null && chain.initial) {
                val fromSlot = chain.activeHotbarSlot
                val toSlot = chain.data!!.targetSlot
                
                if (fromSlot < 0 || toSlot < 0) return

                playerRef?.reference?.let { ref ->
                    val world = ref.store.externalData.world
                    world.execute {
                        val player = ref.store.getComponent(ref, Player.getComponentType()) ?: return@execute
                        val hotbar = player.inventory.combinedHotbarFirst
                        val toItem = hotbar.getItemStack(toSlot.toShort())
                        
                        DrawerLinkSystem.removeWireframes(playerRef)
                        if (toItem?.item?.interactions?.containsValue(LINK_INTERACTION) == true && toItem.metadata?.get("SelectedBlock") != null) {
                            val pos = Vector3i.CODEC.decode(toItem.metadata!!.get("SelectedBlock")!!.asDocument())!!
                            DrawerLinkSystem.renderWireframes(playerRef, pos, world)
                        }
                    }
                }
            }
        }
    }
    
    private fun moveItemHandler(playerRef: PlayerRef?, packet: MoveItemStack) {
        playerRef?.reference?.let { ref ->
            val world = ref.store.externalData.world
            val fromSlot = packet.fromSlotId.toShort()
            val toSlot = packet.toSlotId.toShort()
            val fromSection = packet.fromSectionId
            val toSection = packet.toSectionId
            
            if (fromSlot == toSlot && fromSection == toSection) return
            
            world.execute {
                val player = ref.store.getComponent(ref, Player.getComponentType()) ?: return@execute
                val inventory = player.inventory
                val fromItem = inventory.getSectionById(fromSection)?.getItemStack(fromSlot)
                val toItem = inventory.getSectionById(toSection)?.getItemStack(toSlot)
                
                if (fromSlot.toByte() == inventory.activeHotbarSlot || toSlot.toByte() == inventory.activeHotbarSlot) {
                    if (isLinkingTool(inventory.itemInHand)) {
                        DrawerLinkSystem.removeWireframes(playerRef)
                    }
                    
                    if (!isActiveHotbarSlot(fromSlot, fromSection, inventory.activeHotbarSlot.toShort())
                        && isLinkingTool(fromItem)) {
                        val pos = Vector3i.CODEC.decode(fromItem?.metadata!!.get("SelectedBlock")!!.asDocument())!!
                        DrawerLinkSystem.renderWireframes(playerRef, pos, world)
                    } else if (!isActiveHotbarSlot(toSlot, toSection, inventory.activeHotbarSlot.toShort())
                        && isLinkingTool(toItem)) {
                        val pos = Vector3i.CODEC.decode(toItem?.metadata!!.get("SelectedBlock")!!.asDocument())!!
                        DrawerLinkSystem.renderWireframes(playerRef, pos, world)
                    }
                }
            }
        }
    }
    
    private fun isLinkingTool(item: ItemStack?): Boolean {
        return item?.item?.interactions?.containsValue(LINK_INTERACTION) == true && item.metadata?.get("SelectedBlock") != null
    }
    
    private fun isActiveHotbarSlot(slot: Short, section: Int, activeHotbarSlot: Short): Boolean {
        return section == -1 && slot == activeHotbarSlot
    }
}