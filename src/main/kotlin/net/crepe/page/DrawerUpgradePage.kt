package net.crepe.page

import au.ellie.hyui.builders.Alignment
import au.ellie.hyui.builders.ContainerBuilder
import au.ellie.hyui.builders.GroupBuilder
import au.ellie.hyui.builders.HyUIAnchor
import au.ellie.hyui.builders.HyUIStyle
import au.ellie.hyui.builders.ItemGridBuilder
import au.ellie.hyui.builders.LabelBuilder
import au.ellie.hyui.events.SlotClickingEventData
import au.ellie.hyui.events.UIContext
import au.ellie.hyui.types.LayoutMode
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.common.DataItem
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.component.drawer.DrawerUpgradesComponent
import net.crepe.utils.BlockUtils
import net.crepe.utils.UpgradeUtils

class DrawerUpgradePage : PlayerInventoryPage<DrawerUpgradePage>() {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }
    
    lateinit var ref: Ref<ChunkStore?>
    lateinit var upgradesComponent: DrawerUpgradesComponent
    lateinit var containerComponent: DrawerSlotsContainerComponent
    
    private val capacityLabel = LabelBuilder()
        .withId("capacity-label")
        .withText("Capacity: 36 stacks")
        .withAnchor(
            HyUIAnchor()
                .setBottom(15)
        )
        .withStyle(
            HyUIStyle()
                .setAlignment(Alignment.Center)
        )
    private val upgradeContainerGrid = ItemGridBuilder()
        .withInventorySectionId(0)
        .withAreItemsDraggable(false)
        .withSlotsPerRow(4)
        .withStyle(
            HyUIStyle()
                .set("SlotSpacing", 2)
                .set("SlotSize", 74)
        )
        .withSlots((0..3).asSequence().map { emptySlot }.toList())
        .addEventListenerWithContext(CustomUIEventBindingType.SlotClicking, SlotClickingEventData::class.java) { data, ctx ->
            onUpgradeSlotClick(data, ctx)
        }

    override val custom: ContainerBuilder = ContainerBuilder()
        .withTitleText("DRAWER")
        .withAnchor(
            HyUIAnchor()
                .setWidth(715)
                .setHeight(185)
                .setBottom(7)
        )
        .addContentChild(
            GroupBuilder()
                .withLayoutMode(LayoutMode.MiddleCenter)
                .addChild(capacityLabel)
                .addChild(upgradeContainerGrid)
        )
    
    fun forBlock(ref: Ref<ChunkStore?>) : DrawerUpgradePage {
        this.ref = ref
        containerComponent = ref.store.getComponent(ref, DrawerSlotsContainerComponent.getComponentType())
            ?: throw IllegalStateException("DrawerSlotsContainerComponent not found for interacted block")
        upgradesComponent = ref.store.getComponent(ref, DrawerUpgradesComponent.getComponentType())
            ?: throw IllegalStateException("DrawerUpgradesComponent not found for interacted block")
        
        upgradesComponent.upgrades.forEachIndexed { index, slot ->
            if (DataItem.isEmpty(slot)) return@forEachIndexed
            upgradeContainerGrid.updateSlot(emptySlot.setItemStack(ItemStack(slot!!.itemId!!)), index)
        }
        capacityLabel.withText("Capacity: ${containerComponent.getSlotStackCapacity(upgradesComponent.multiplier)} stacks")
        
        return self
    }

    override fun onInventorySlotClick(data: SlotClickingEventData, ctx: UIContext, grid: ItemGridBuilder, container: ItemContainer) {
        player ?: {
            throw IllegalStateException("Player must be set before handling upgrade slot clicks")
        }

        val slot = data.slotIndex.toShort()

        val item = container.getItemStack(slot)
        val transaction = UpgradeUtils.addUpgrade(item, upgradesComponent, containerComponent)
        if (transaction.succeeded()) {
            BlockUtils.saveBlock(ref)
            container.setItemStackForSlot(slot, transaction.remainder)

            updateSlot(grid, slot.toInt(), transaction.remainder)
            updateSlot(upgradeContainerGrid, transaction.slot.toInt(), transaction.slotAfter)
            capacityLabel.withText("Capacity: ${containerComponent.getSlotStackCapacity(upgradesComponent.multiplier)} stacks")

            ctx.updatePage(false)
        }
    }
    
    private fun onUpgradeSlotClick(data: SlotClickingEventData, ctx: UIContext) {
        player ?: {
            throw IllegalStateException("Player must be set before handling upgrade slot clicks")
        }
        
        val slot = data.slotIndex
        var transactions = UpgradeUtils.moveUpgradeTo(slot, upgradesComponent, containerComponent, player!!.inventory.storage)
        
        transactions.first ?: return
        
        if (transactions.first!!.succeeded()) {
            BlockUtils.saveBlock(ref)
            updateSlot(upgradeContainerGrid, slot, transactions.second?.slotAfter)
            transactions.first!!.slotTransactions.filter { it.succeeded() }.forEach { transaction ->
                updateSlot(playerStorageGrid, transaction.slot.toInt(), transaction.slotAfter)
            }
            capacityLabel.withText("Capacity: ${containerComponent.getSlotStackCapacity(upgradesComponent.multiplier)} stacks")
        } else {
            transactions = UpgradeUtils.moveUpgradeTo(slot, upgradesComponent, containerComponent, player!!.inventory.hotbar)

            if (transactions.first == null || !transactions.first!!.succeeded()) return
            
            BlockUtils.saveBlock(ref)
            updateSlot(upgradeContainerGrid, slot, transactions.second?.slotAfter)
            transactions.first!!.slotTransactions.filter { it.succeeded() }.forEach { transaction ->
                updateSlot(playerHotbarGrid, transaction.slot.toInt(), transaction.slotAfter)
            }
            capacityLabel.withText("Capacity: ${containerComponent.getSlotStackCapacity(upgradesComponent.multiplier)} stacks")
        }
        
        ctx.updatePage(false)
    }
}