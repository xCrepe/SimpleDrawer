package net.crepe.utils

import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.inventory.transaction.ActionType
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
import net.crepe.component.common.DataItem
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.controller.ControllerUpgradesComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.component.drawer.DrawerUpgradesComponent

class UpgradeUtils {
    companion object {
        val log = { log: String -> com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        fun getUpgradeBoost(upgrade: String, tags: Set<String>): Int? {
            return tags.find { Regex(".*$upgrade=\\d+") in it }?.substringAfter("=")?.toInt()
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
            getUpgradeBoost("Limit", item.item.data.rawTags.keys)?.let { limit ->
                if (container.upgrades.count { it?.itemId == item.itemId } >= limit) return failedTransaction
            }
            val boost = getUpgradeBoost("Range", item.item.data.rawTags.keys) ?: return failedTransaction
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
            val boost = getUpgradeBoost("Range", itemStack.item.data.rawTags.keys)
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

        fun addUpgrade(item: ItemStack?, upgradesContainer: DrawerUpgradesComponent, target: DrawerSlotsContainerComponent): ItemStackSlotTransaction {
            val failedTransaction = ItemStackSlotTransaction(false, ActionType.ADD, -1, null, null, null, false, false, true, false, item, item)
            
            if (ItemStack.isEmpty(item) || !item!!.item.data.rawTags.keys.any { it in upgradesContainer.upgradeTypes }) return failedTransaction
            
            log("${getUpgradeBoost("Limit", item.item.data.rawTags.keys)}")
            getUpgradeBoost("Limit", item.item.data.rawTags.keys)?.let { limit ->
                if (upgradesContainer.upgrades.count { it?.itemId == item.itemId } >= limit) return failedTransaction
            }
            
            for (slot in upgradesContainer.upgrades.indices) {
                if (DataItem.isEmpty(upgradesContainer.upgrades[slot])) {
                    upgradesContainer.upgrades[slot] = DataItem(item.itemId, 1)
                    when (item.item.data.rawTags.keys.find { it in upgradesContainer.upgradeTypes && "Limit" !in it }) {
                        "SimpleDrawer_Upgrade_Stack" -> {
                            val boost = getUpgradeBoost("Stack", item.item.data.rawTags.keys) ?: return failedTransaction
                            upgradesContainer.multiplier *= boost
                            target.setCapacity(upgradesContainer.multiplier)
                        }
                        "SimpleDrawer_Upgrade_Void" -> upgradesContainer.void = true
                    }
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
        
        fun removeUpgradeFromSlot(slot: Int, upgradesContainer: DrawerUpgradesComponent, target: DrawerSlotsContainerComponent): ItemStackSlotTransaction {
            val item = upgradesContainer.upgrades[slot]
            
            if (DataItem.isEmpty(item)) return ItemStackSlotTransaction(false, ActionType.REMOVE, slot.toShort(), null, null, null, false, false, false, false, null, null)

            val itemStack = ItemStack(item!!.itemId!!, item.quantity)

            upgradesContainer.upgrades[slot] = DataItem()
            when (itemStack.item.data.rawTags.keys.find { it in upgradesContainer.upgradeTypes && "Limit" !in it }) {
                "SimpleDrawer_Upgrade_Stack" -> {
                    val boost = getUpgradeBoost("Stack", itemStack.item.data.rawTags.keys) ?: 1
                    upgradesContainer.multiplier /= boost
                    target.setCapacity(upgradesContainer.multiplier)
                }
                "SimpleDrawer_Upgrade_Void" -> {
                    upgradesContainer.void = false
                }
            }
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

        fun moveUpgradeTo(slot: Int, upgradesContainer: DrawerUpgradesComponent, target: DrawerSlotsContainerComponent, containerTo: ItemContainer): Pair<ItemStackTransaction?, ItemStackSlotTransaction?> {
            val item = upgradesContainer.upgrades[slot]
            if (DataItem.isEmpty(item)) return Pair(null, null)

            val itemStack = ItemStack(item!!.itemId!!, item.quantity)
            
            when (itemStack.item.data.rawTags.keys.find { it in upgradesContainer.upgradeTypes }) {
                "SimpleDrawer_Upgrade_Stack" -> {
                    val boost = getUpgradeBoost("Stack", itemStack.item.data.rawTags.keys) ?: 1
                    if (target.slots.any { slot ->
                        slot.storedQuantity > slot.capacity / boost
                    }) {
                        return Pair(null, null)
                    }
                }
            }

            val transaction = containerTo.addItemStack(itemStack)
            if (!transaction.succeeded()) return Pair(transaction, null)

            val transaction2 = removeUpgradeFromSlot(slot, upgradesContainer, target)
            return Pair(transaction, transaction2)
        }
    }
}