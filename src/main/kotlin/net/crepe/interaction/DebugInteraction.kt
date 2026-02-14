package net.crepe.interaction

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.meta.BlockState
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.drawer.DrawerSlotsContainerComponent

class DebugInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DebugInteraction::class.java, ::DebugInteraction, SimpleBlockInteraction.CODEC)
            .build()
    }
    
    val logger = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    
    override fun interactWithBlock(
        world: World,
        cmdBuffer: CommandBuffer<EntityStore?>,
        type: InteractionType,
        ctx: InteractionContext,
        item: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        val ref = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return
        val state = BlockState.getBlockState(ref, ref.store)
        
//        val component = ref?.store?.getComponent(ref, DrawerSlotsContainerComponent.getComponentType())
//        val component2 = ref?.store?.getComponent(ref, DrawerDisplayComponent.getComponentType())
//        component?.slots[0]?.capacity = 123456789
//        component?.slots[0]?.storedQuantity = 123456789
//        val slot = component?.slots[0]!!
//        val rot = Vector3f()
//        rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat()
//        val numberPos = DisplayUtils.calcDisplayPosition(pos, rot, slot.displaysTransform.numberOffset)
//        world.execute {
//            DisplayUtils.renderNumber(
//                1,
//                123456789,
//                component2?.slotsDisplays[0]!!,
//                numberPos,
//                rot,
//                0.75f,
//                world,
//                cmdBuffer.store
//            )
//        }
        if (state is ItemContainerState) {
            logger("${state.itemContainer}")
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