package net.crepe.inventory

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.drawer.DrawerLinkedComponent

class DrawerContainerState : ItemContainerState() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerContainerState::class.java, ::DrawerContainerState, ItemContainerState.CODEC as BuilderCodec)
            .build()
        
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }

    override fun getItemContainer(): ItemContainer? {
        (itemContainer as? DrawerContainerWrapper)?.accessedTick = chunk?.world?.tick ?: -1
        
        return itemContainer
    }
    
    internal fun internal_getItemContainer(): ItemContainer? {
        return itemContainer
    }
}