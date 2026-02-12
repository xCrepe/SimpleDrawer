package net.crepe.interaction

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.drawer.DrawerDisplayComponent
import net.crepe.component.drawer.DrawerLockComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.system.DrawerSystem
import net.crepe.utils.DisplayUtils

class DrawerLockInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerLockInteraction::class.java, ::DrawerLockInteraction,
            SimpleBlockInteraction.CODEC).build()
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
        val ref = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z) ?: return
        val store = ref.store
        val containerComponent = store.getComponent(ref, DrawerSlotsContainerComponent.getComponentType()) ?: return
        val displayComponent = store.getComponent(ref, DrawerDisplayComponent.getComponentType())!!
        val rot = Vector3f()
        rot.yaw = RotationTuple.get(world.getBlockRotationIndex(pos.x, pos.y, pos.z)).yaw().radians.toFloat()
        
        world.execute {
            if (store.getComponent(ref, DrawerLockComponent.getComponentType()) == null) {
                store.addComponent(ref, DrawerLockComponent.getComponentType(), DrawerLockComponent.instance)

                val displayPos = DisplayUtils.calcDisplayPosition(pos, rot, Vector3d(0.0, 0.4453125, -0.0621))
                displayComponent.iconDisplays["Lock"] = DisplayUtils.spawnIcon(cmdBuffer.store, displayPos, rot, 0.25f, "Lock")
            } else {
                store.removeComponent(ref, DrawerLockComponent.getComponentType())

                if (displayComponent.iconDisplays["Lock"] != null) {
                    DisplayUtils.removeDisplayEntity(cmdBuffer.store, displayComponent.iconDisplays["Lock"]!!)
                }
                containerComponent.slots.forEachIndexed { idx, slot ->
                    if (slot.storedQuantity <= 0 && !ItemStack.isEmpty(slot.storedItem)) {
                        slot.storedItem = ItemStack.EMPTY
                        slot.capacity = 0

                        val slotDisplays = displayComponent.slotsDisplays[idx]
                        DrawerSystem.clearDisplaySlot(slotDisplays, cmdBuffer.store)
                    }
                }
            }
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