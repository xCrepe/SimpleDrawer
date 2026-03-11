package net.crepe.inventory

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import java.util.function.Supplier

interface IDrawerContainer {
    val slotCount: Short
    fun getRef(slot: Short): Ref<ChunkStore?>
    fun <V> writeAction(action: Supplier<V?>): V?
    fun getSlot(slot: Short): ItemStack?
    fun getSlotItem(slot: Short): ItemStack?
    fun getSlotQuantity(slot: Short): Int
    fun getSlotStackCapacity(slot: Short): Int
    fun testCantAddToSlot(slot: Short, itemStack: ItemStack, slotItemStack: ItemStack?): Boolean
    fun setSlot(slot: Short, itemStack: ItemStack?): ItemStack?
    fun removeSlot(slot: Short): ItemStack?
}