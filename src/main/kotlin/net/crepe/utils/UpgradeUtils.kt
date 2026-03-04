package net.crepe.utils

import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.inventory.transaction.ActionType
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
import net.crepe.component.common.DataItem
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.controller.ControllerUpgradesComponent
import kotlin.text.toIntOrNull

class UpgradeUtils {
    companion object {
        val log = { log: String -> com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        fun getUpgradeBoost(tags: Set<String>): Int? {
            return tags.find { it.toIntOrNull() != null }?.toInt()
        }
        
        fun addUpgrade(item: ItemStack?, container: ControllerUpgradesComponent, target: ControllerLinksComponent): ItemStackSlotTransaction {
            val failedTransaction = ItemStackSlotTransaction(
                false,
                ActionType.ADD,
                -1,
                null,
                null,
                null,
                false,
                false,
                true,
                false,
                item,
                item
            )
            if (ItemStack.isEmpty(item) || "SimpleDrawer_Upgrade_Range" !in item!!.item.data.rawTags.keys) return failedTransaction
            val boost = getUpgradeBoost(item.item.data.rawTags.keys) ?: return failedTransaction
            for (slot in container.upgrades.indices) {
                if (DataItem.isEmpty(container.upgrades[slot])) {
                    container.upgrades[slot] = DataItem(item.itemId, 1)
                    target.radius += boost
                    return ItemStackSlotTransaction(
                        true,
                        ActionType.ADD,
                        slot.toShort(),
                        null,
                        item.withQuantity(1),
                        null,
                        false,
                        false,
                        true,
                        false,
                        item,
                        item.withQuantity(item.quantity - 1)
                    )
                }
            }
            return failedTransaction
        }
        
        fun removeUpgradeFromSlot(slot: Int, container: ControllerUpgradesComponent, target: ControllerLinksComponent): ItemStackSlotTransaction {
            val item = container.upgrades[slot]
            if (DataItem.isEmpty(item)) return ItemStackSlotTransaction(
                false,
                ActionType.REMOVE,
                slot.toShort(),
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                null,
                null
            )
            
            val itemStack = ItemStack(item!!.itemId!!, item.quantity)
            val boost = getUpgradeBoost(itemStack.item.data.rawTags.keys)
            container.upgrades[slot] = DataItem()
            target.radius -= boost ?: 0
            return ItemStackSlotTransaction(
                true,
                ActionType.REMOVE,
                slot.toShort(),
                itemStack,
                ItemStack.EMPTY,
                itemStack,
                false,
                false,
                false,
                false,
                itemStack,
                ItemStack.EMPTY
            )
        }
        
        fun moveUpgradeTo(slot: Int, container: ControllerUpgradesComponent, target: ControllerLinksComponent, containerTo: ItemContainer): Pair<ItemStackTransaction?, ItemStackSlotTransaction?> {
            val item = container.upgrades[slot]
            if (DataItem.isEmpty(item)) return Pair(null, null)
            
            val itemStack = ItemStack(item!!.itemId!!, item.quantity)
            val transaction = containerTo.addItemStack(itemStack)
            
            if (!transaction.succeeded()) return Pair(transaction, null)
            
            val transaction2 = removeUpgradeFromSlot(slot, container, target)
            return Pair(transaction, transaction2)
        }
    }
}