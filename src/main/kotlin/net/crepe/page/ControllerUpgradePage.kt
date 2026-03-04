package net.crepe.page

import au.ellie.hyui.builders.ItemGridBuilder
import au.ellie.hyui.builders.LabelBuilder
import au.ellie.hyui.events.SlotClickingEventData
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.controller.ControllerUpgradesComponent
import net.crepe.utils.BlockUtils
import net.crepe.utils.UpgradeUtils

class ControllerUpgradePage() : PlayerInventoryPage("Pages/Inventory/ControllerUpgrade.html", CustomPageLifetime.CanDismissOrCloseThroughInteraction) {
    fun load(store: Store<EntityStore?>, playerRef: PlayerRef, linksComponent: ControllerLinksComponent, upgradesComponent: ControllerUpgradesComponent): ControllerUpgradePage {
        val template = inventoryTemplate(playerRef, store)
        val upgrades = MutableList<UIItem>(upgradesComponent.upgrades.size) { UIItem(null, 0) }
        upgradesComponent.upgrades.forEachIndexed { slot, item ->
            if (item != null) {
                upgrades[slot] = UIItem(item.itemId, item.quantity)
            }
        }
        template
            .setVariable("currentRange", linksComponent.radius)
            .setVariable("upgradeSlotsWidth", upgrades.size * 73)
            .setVariable("upgradeSlots", upgrades.size)
            .setVariable("upgrades", upgrades)
        page.loadHtml(path, template)
        return this
    }
    
    fun initEvents(ref: Ref<ChunkStore?>, linksComponent: ControllerLinksComponent, upgradesComponent: ControllerUpgradesComponent, player: Player): ControllerUpgradePage {
        val storage = player.inventory.storage
        val hotbar = player.inventory.hotbar
        
        val controllerUpgrades = page.getById("controller-upgrades", ItemGridBuilder::class.java)
        val playerStorage = page.getById("player-inventory-storage", ItemGridBuilder::class.java)
        val playerHotbar = page.getById("player-inventory-hotbar", ItemGridBuilder::class.java)
        val description = page.getById("upgrade-description", LabelBuilder::class.java)
        
        controllerUpgrades?.ifPresent { grid ->
            page.addEventListener("controller-upgrades", CustomUIEventBindingType.SlotClicking) { data, ctx ->
                val slot = (data as SlotClickingEventData).slotIndex
                var transactions = UpgradeUtils.moveUpgradeTo(slot, upgradesComponent, linksComponent, storage)
                
                if (transactions.first == null) return@addEventListener
                
                if (transactions.first!!.succeeded()) {
                    BlockUtils.saveBlock(ref)
                    updateSlot(grid, slot, transactions.second?.slotAfter)
                    playerStorage?.ifPresent { grid2 ->
                        for (transaction in transactions.first!!.slotTransactions) {
                            if (transaction.succeeded()) {
                                updateSlot(grid2, transaction.slot.toInt(), transaction.slotAfter)
                            }
                        }
                    }
                    description?.ifPresent { label ->
                        label.withText("Range: ${linksComponent.radius}")
                    }
                    ctx.updatePage(false)
                } else {
                    transactions = UpgradeUtils.moveUpgradeTo(slot, upgradesComponent, linksComponent, hotbar)
                    if (transactions.first == null) return@addEventListener
                    
                    if (transactions.first!!.succeeded()) {
                        BlockUtils.saveBlock(ref)
                        updateSlot(grid, slot, transactions.second?.slotAfter)
                        playerHotbar?.ifPresent { grid2 ->
                            for (transaction in transactions.first!!.slotTransactions) {
                                if (transaction.succeeded()) {
                                    updateSlot(grid2, transaction.slot.toInt(), transaction.slotAfter)
                                }
                            }
                        }
                        description?.ifPresent { label ->
                            label.withText("Range: ${linksComponent.radius}")
                        }
                        ctx.updatePage(false)
                    }
                }
            }
        }
        playerStorage?.ifPresent { grid ->
            page.addEventListener("player-inventory-storage", CustomUIEventBindingType.SlotClicking) { data, ctx ->
                val slot = (data as SlotClickingEventData).slotIndex.toShort()
                
                val item = storage.getItemStack(slot)
                val transaction = UpgradeUtils.addUpgrade(item, upgradesComponent, linksComponent)
                if (transaction.succeeded()) {
                    BlockUtils.saveBlock(ref)
                    storage.setItemStackForSlot(slot, transaction.remainder)
                    
                    updateSlot(grid, slot.toInt(), transaction.remainder)
                    controllerUpgrades?.ifPresent { grid2 ->
                        updateSlot(grid2, transaction.slot.toInt(), transaction.slotAfter)
                    }
                    description?.ifPresent { label ->
                        label.withText("Range: ${linksComponent.radius}")
                    }
                    
                    ctx.updatePage(false)
                }
            }
        }
        playerHotbar?.ifPresent { grid ->
            page.addEventListener("player-inventory-hotbar", CustomUIEventBindingType.SlotClicking) { data, ctx ->
                val slot = (data as SlotClickingEventData).slotIndex.toShort()

                val item = hotbar.getItemStack(slot)
                val transaction = UpgradeUtils.addUpgrade(item, upgradesComponent, linksComponent)
                if (transaction.succeeded()) {
                    BlockUtils.saveBlock(ref)
                    hotbar.setItemStackForSlot(slot, transaction.remainder)

                    updateSlot(grid, slot.toInt(), transaction.remainder)
                    controllerUpgrades?.ifPresent { grid2 ->
                        updateSlot(grid2, transaction.slot.toInt(), transaction.slotAfter)
                    }
                    description?.ifPresent { label ->
                        label.withText("Range: ${linksComponent.radius}")
                    }

                    ctx.updatePage(false)
                }
            }
        }
        return this
    }
    
    override fun initStyle(): ControllerUpgradePage {
        super.initStyle()
        page.getById("controller-upgrades", ItemGridBuilder::class.java)?.ifPresent { grid ->
            for (slot in grid.slots) {
                slot.setBackground(Style.Slot)
            }
        }
        return this
    }
}