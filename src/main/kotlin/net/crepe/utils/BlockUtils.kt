package net.crepe.utils

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore

class BlockUtils {
    companion object {
        fun getPos(ref: Ref<ChunkStore?>?): Vector3i? {
            ref ?: return null
            
            val store = ref.store
            val state = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType()) ?: return null
            val chunkRef = state.chunkRef

            if (!chunkRef.isValid) return null

            val blockChunk = store.getComponent(chunkRef, BlockChunk.getComponentType())!!
            val x = ChunkUtil.worldCoordFromLocalCoord(blockChunk.x, ChunkUtil.xFromBlockInColumn(state.index))
            val y = ChunkUtil.yFromBlockInColumn(state.index)
            val z = ChunkUtil.worldCoordFromLocalCoord(blockChunk.z, ChunkUtil.zFromBlockInColumn(state.index))
            return Vector3i(x, y, z)
        }
    }
}