package net.crepe.inventory

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.inventory.transaction.ActionType
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
import kotlin.math.min

class InternalDrawerContainerUtilItemStack {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        internal fun addItemStack(
            container: IDrawerContainer,
            itemStack: ItemStack,
            allOrNothing: Boolean,
            fullStacks: Boolean,
            filter: Boolean
        ): ItemStackTransaction {
            val item = itemStack.item
            val itemMaxStack = item.maxStack
            return container.writeAction {
                if (allOrNothing) {
                    var testQuantityRemaining = itemStack.quantity
                    if (!fullStacks) {
                        testQuantityRemaining = testAddToExistingItemStacks(
                            container,
                            itemStack,
                            itemMaxStack,
                            testQuantityRemaining,
                            filter
                        )
                    }
                    
                    testQuantityRemaining = testAddToEmptySlots(
                        container,
                        itemStack,
                        itemMaxStack,
                        testQuantityRemaining,
                        filter
                    )
                    if (testQuantityRemaining > 0) {
                        return@writeAction ItemStackTransaction(
                            false,
                            ActionType.ADD,
                            itemStack,
                            itemStack,
                            allOrNothing,
                            filter,
                            listOf()
                        )
                    }
                }
                log("${container.slotCount}")
                val list = mutableListOf<ItemStackSlotTransaction>()
                var remaining: ItemStack? = itemStack
                if (!fullStacks) {
                    for (i in 0..<container.slotCount) {
                        if (ItemStack.isEmpty(remaining)) break
                        
                        val transaction = internal_addToExistingSlot(
                            container,
                            i.toShort(),
                            remaining!!,
                            itemMaxStack,
                            filter
                        )
                        list.add(transaction)
                        remaining = transaction.remainder
                    }
                }
                for (i in 0..<container.slotCount) {
                    if (ItemStack.isEmpty(remaining)) break
                    
                    val transaction = internal_addToEmptySlot(
                        container,
                        i.toShort(),
                        remaining!!,
                        itemMaxStack,
                        filter
                    )
                    list.add(transaction)
                    remaining = transaction.remainder
                }
                    
                return@writeAction ItemStackTransaction(
                    true,
                    ActionType.ADD,
                    itemStack,
                    remaining,
                    allOrNothing,
                    filter,
                    list
                )
            } as ItemStackTransaction
        }
        
        internal fun internal_addToEmptySlot(
            container: IDrawerContainer,
            slot: Short,
            itemStack: ItemStack,
            itemMaxStack: Int,
            filter: Boolean
        ): ItemStackSlotTransaction {
            val slotItemStack = container.getSlot(slot)
            if (!ItemStack.isEmpty(slotItemStack)) {
                return ItemStackSlotTransaction(
                    false,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotItemStack,
                    null,
                    false,
                    false,
                    filter,
                    false,
                    itemStack,
                    itemStack
                )
            } else if (filter && container.testCantAddToSlot(slot, itemStack, slotItemStack)) {
                return ItemStackSlotTransaction(
                    false,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotItemStack,
                    null,
                    false,
                    false,
                    true,
                    false,
                    itemStack,
                    itemStack
                )
            } else {
                var quantityRemaining = itemStack.quantity
                val quantityAdjustment = min(container.getSlotStackCapacity(slot) * itemMaxStack, quantityRemaining)
                quantityRemaining -= quantityAdjustment
                val slotNew = itemStack.withQuantity(quantityAdjustment)
                container.setSlot(slot, slotNew)
                val remainder = if (quantityRemaining != itemStack.quantity)
                    itemStack.withQuantity(quantityRemaining) else itemStack
                return ItemStackSlotTransaction(
                    true,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotNew,
                    null,
                    false,
                    false,
                    filter,
                    false,
                    itemStack,
                    remainder
                )
            }
        }
        
        internal fun internal_addToExistingSlot(
            container: IDrawerContainer,
            slot: Short,
            itemStack: ItemStack,
            itemMaxStack: Int,
            filter: Boolean
        ): ItemStackSlotTransaction {
            val slotItemStack = container.getSlot(slot)
            if (ItemStack.isEmpty(slotItemStack)) {
                return ItemStackSlotTransaction(
                    false,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotItemStack,
                    null,
                    false,
                    false,
                    filter,
                    true,
                    itemStack,
                    itemStack
                )
            } else if (slotItemStack?.isStackableWith(itemStack) != true) {
                return ItemStackSlotTransaction(
                    false,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotItemStack,
                    null,
                    false,
                    false,
                    filter,
                    true,
                    itemStack,
                    itemStack
                )
            } else if (filter && container.testCantAddToSlot(slot, itemStack, slotItemStack)) {
                return ItemStackSlotTransaction(
                    false,
                    ActionType.ADD,
                    slot,
                    slotItemStack,
                    slotItemStack,
                    null,
                    false,
                    false,
                    filter,
                    true,
                    itemStack,
                    itemStack
                )
            } else {
                var quantityRemaining = itemStack.quantity
                val quantity = slotItemStack.quantity
                val quantityAdjustment = min(container.getSlotStackCapacity(slot) * itemMaxStack - quantity, quantityRemaining)
                val newQuantity = quantity + quantityAdjustment
                quantityRemaining -= quantityAdjustment
                if (quantityAdjustment <= 0) {
                    return ItemStackSlotTransaction(
                        false,
                        ActionType.ADD,
                        slot,
                        slotItemStack,
                        slotItemStack,
                        null,
                        false,
                        false,
                        filter,
                        true,
                        itemStack,
                        itemStack
                    )
                } else {
                    val slotNew = slotItemStack.withQuantity(newQuantity)
                    if (newQuantity > 0) {
                        container.setSlot(slot, slotNew)
                    } else {
                        container.removeSlot(slot)
                    }
                    
                    val remainder = if (quantityRemaining != itemStack.quantity)
                        itemStack.withQuantity(quantityRemaining) else itemStack
                    return ItemStackSlotTransaction(
                        true,
                        ActionType.ADD,
                        slot,
                        slotItemStack,
                        slotNew,
                        null,
                        false,
                        false,
                        filter,
                        true,
                        itemStack,
                        remainder
                    )
                }
            }
        }
        
        internal fun testAddToEmptySlots(
            container: IDrawerContainer,
            itemStack: ItemStack,
            itemMaxStack: Int,
            testQuantityRemaining: Int,
            filter: Boolean
        ): Int {
            var remaining = testQuantityRemaining
            for(i in 0..<container.slotCount) {
                if (remaining <= 0) break

                val slotItemStack = container.getSlot(i.toShort())
                if (ItemStack.isEmpty(slotItemStack)
                    && (!filter || !container.testCantAddToSlot(i.toShort(), itemStack, slotItemStack))) {
                    val quantityAdjustment = min(itemMaxStack, remaining)
                    remaining -= quantityAdjustment
                }
            }

            return remaining
        }
        
        internal fun testAddToExistingItemStacks(
            container: IDrawerContainer,
            itemStack: ItemStack,
            itemMaxStack: Int,
            testQuantityRemaining: Int,
            filter: Boolean
        ): Int {
            var remaining = testQuantityRemaining
            for (i in 0..<container.slotCount) {
                if (remaining <= 0) break

                remaining = testAddToExistingSlot(container, i.toShort(), itemStack, itemMaxStack, testQuantityRemaining, filter)
            }
            
            return remaining
        }
        
        internal fun testAddToExistingSlot(
            container: IDrawerContainer,
            slot: Short,
            itemStack: ItemStack,
            itemMaxStack: Int,
            testQuantityRemaining: Int,
            filter: Boolean
        ): Int {
            val slotItemStack = container.getSlot(slot)
            if (ItemStack.isEmpty(slotItemStack)) {
                return testQuantityRemaining
            } else if (slotItemStack?.isStackableWith(itemStack) != true) {
                return testQuantityRemaining
            } else if (filter && container.testCantAddToSlot(slot, itemStack, slotItemStack)) {
                return testQuantityRemaining
            } else {
                val quantity = slotItemStack.quantity
                val quantityAdjustment = min(container.getSlotStackCapacity(slot) * itemMaxStack - quantity, testQuantityRemaining)
                return testQuantityRemaining - quantityAdjustment
            }
        }
    }
}