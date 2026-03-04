package net.crepe.system

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.ResourceType
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.spatial.SpatialResource
import com.hypixel.hytale.component.spatial.SpatialSystem
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.inventory.ControllerState
import net.crepe.inventory.DrawerContainerState

class ContainerStateSystem {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
        var needsRebuild = false
    }
    
    class ContainerStateRefSystem : RefSystem<ChunkStore?>() {
        override fun onEntityAdded(
            ref: Ref<ChunkStore?>,
            reason: AddReason,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>,
        ) {
            needsRebuild = true
        }

        override fun onEntityRemove(
            ref: Ref<ChunkStore?>,
            reason: RemoveReason,
            store: Store<ChunkStore?>,
            cmdBuffer: CommandBuffer<ChunkStore?>,
        ) {
            needsRebuild = true
        }

        override fun getQuery(): Query<ChunkStore?>? {
            return Query.or(
                requireNotNull(BlockStateModule.get().getComponentType(DrawerContainerState::class.java)) {
                    "DrawerContainerState must be registered before DrawerContainerSpatialSystem is instantiated"
                },
                requireNotNull(BlockStateModule.get().getComponentType(ControllerState::class.java)) {
                    "ControllerState must be registered before DrawerContainerSpatialSystem is instantiated"
                }
            )
        }

    }
    
    class ContainerStateSpatialSystem : SpatialSystem<ChunkStore?> {
        constructor(resourceType: ResourceType<ChunkStore?, SpatialResource<Ref<ChunkStore?>, ChunkStore?>>) : super(resourceType) {
            log("$resourceType")
        }
        override fun tick(dt: Float, systemIndex: Int, store: Store<ChunkStore?>) {
            if (needsRebuild) {
                needsRebuild = false
                super.tick(dt, systemIndex, store)
            }
        }

        override fun getPosition(
            chunk: ArchetypeChunk<ChunkStore?>,
            index: Int,
        ): Vector3d? {
            val blockInfo = chunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType())
            val chunkRef = blockInfo?.chunkRef
            if (chunkRef?.isValid != true) return null

            val blockChunk = chunkRef.store.getComponent(chunkRef, BlockChunk.getComponentType())!!
            val worldX = (blockChunk.x shl 5) or ChunkUtil.xFromBlockInColumn(blockInfo.index)
            val worldY = ChunkUtil.yFromBlockInColumn(blockInfo.index)
            val worldZ = (blockChunk.z shl 5) or ChunkUtil.zFromBlockInColumn(blockInfo.index)
            return Vector3d(worldX.toDouble(), worldY.toDouble(), worldZ.toDouble())
        }

        override fun getQuery(): Query<ChunkStore?>? {
            return Query.or(
                requireNotNull(BlockStateModule.get().getComponentType(DrawerContainerState::class.java)) {
                    "DrawerContainerState must be registered before DrawerContainerSpatialSystem is instantiated"
                },
                requireNotNull(BlockStateModule.get().getComponentType(ControllerState::class.java)) {
                    "ControllerState must be registered before DrawerContainerSpatialSystem is instantiated"
                }
            )
        }
    }
}