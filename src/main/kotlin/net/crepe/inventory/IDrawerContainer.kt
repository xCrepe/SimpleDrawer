package net.crepe.inventory

import com.hypixel.hytale.server.core.inventory.ItemStack
import java.util.function.Supplier

interface IDrawerContainer {
    val slotCount: Short
    fun <V : Any?> writeAction(action: Supplier<V?>): V?
    fun getSlot(slot: Short): ItemStack?
    fun getSlotStackCapacity(slot: Short): Int
    fun testCantAddToSlot(slot: Short, itemStack: ItemStack, slotItemStack: ItemStack?): Boolean
    fun setSlot(slot: Short, itemStack: ItemStack?): ItemStack?
    fun removeSlot(slot: Short): ItemStack?
}