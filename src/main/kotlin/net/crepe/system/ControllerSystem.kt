package net.crepe.system

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.universe.world.meta.BlockState
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.inventory.ControllerAggregateContainer
import net.crepe.utils.BlockUtils

class ControllerSystem {
    class ControllerPlace : RefSystem<ChunkStore?>() {
        override fun onEntityAdded(
            ref: Ref<ChunkStore?>,
            reason: AddReason,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>,
        ) {
            val state = BlockState.getBlockState(ref, cmdBuffer)
            if (state is ItemContainerState) {
                if (state.itemContainer !is ControllerAggregateContainer) {
                    state.setItemContainer(ControllerAggregateContainer(store.externalData.world.name))
                }
            }
        }

        override fun onEntityRemove(
            p0: Ref<ChunkStore?>,
            p1: RemoveReason,
            p2: Store<ChunkStore?>,
            p3: CommandBuffer<ChunkStore?>,
        ) {
        }

        override fun getQuery(): Query<ChunkStore?>? {
            return Query.and(ControllerLinksComponent.getComponentType())
        }

    }
}