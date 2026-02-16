package net.crepe.inventory

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.meta.BlockState
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import net.crepe.component.controller.ControllerLinksComponent

class ControllerState : ItemContainerState() {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerState::class.java, ::ControllerState, ItemContainerState.CODEC as BuilderCodec)
            .build()

        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }

    override fun getItemContainer(): ItemContainer? {
        itemContainer = null
        chunk?.world?.let { world ->
            val container = ControllerAggregateContainer(world.name)
            val position = Vector3i(
                ChunkUtil.worldCoordFromLocalCoord(chunk!!.x, position.x),
                position.y,
                ChunkUtil.worldCoordFromLocalCoord(chunk!!.z, position.z)
            )
            BlockModule.getBlockEntity(world, position.x, position.y, position.z)?.let { ref ->
                ref.store.getComponent(ref, ControllerLinksComponent.getComponentType())?.let { component ->
                    component.drawers.forEach { pos ->
                        val drawerRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return@forEach
                        val state = BlockState.getBlockState(drawerRef, ref.store) ?: return@forEach
                        
                        if (state !is DrawerContainerState) return@forEach
                        
                        if (state.internal_getItemContainer() is DrawerContainerWrapper) {
                            container.addDrawer(state.internal_getItemContainer() as DrawerContainerWrapper)
                        }
                    }
                }
            }
            itemContainer = container
        }
        
        return itemContainer
    }
}