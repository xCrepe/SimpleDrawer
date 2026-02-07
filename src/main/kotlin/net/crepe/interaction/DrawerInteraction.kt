package net.crepe.interaction

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionState
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.TargetUtil
import net.crepe.utils.RaycastUtils

class DrawerInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerInteraction::class.java, ::DrawerInteraction, SimpleBlockInteraction.CODEC)
            .append(KeyedCodec("Inverse", Codec.BOOLEAN),
                { o, v -> o.inverse = v },
                { o -> o.inverse }).add()
            .build()
    }
    
    private var inverse = false
    
    override fun interactWithBlock(
        world: World,
        cmdBuffer: CommandBuffer<EntityStore?>,
        type: InteractionType,
        ctx: InteractionContext,
        item: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        val ref = ctx.entity
        
        val look = TargetUtil.getLook(ref, ref.store)
        val rotation = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z))
        val boundingBox = BlockBoundingBoxes.getAssetMap().getAsset(world.getBlockType(pos)?.hitboxType)!!.get(rotation.yaw, rotation.pitch, rotation.roll)
        val unionBox = boundingBox.boundingBox
        val boxes = boundingBox.detailBoxes

        if (!RaycastUtils.isHitTargetBox(unionBox, boxes, boxes.last(), pos, look)) {
            ctx.state.state = if (inverse) InteractionState.Finished else InteractionState.Failed
            return
        }
        ctx.state.state = if (inverse) InteractionState.Failed else InteractionState.Finished
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