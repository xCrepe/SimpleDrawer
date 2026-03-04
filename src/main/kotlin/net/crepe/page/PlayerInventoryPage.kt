package net.crepe.page

import au.ellie.hyui.builders.HyUIPage
import au.ellie.hyui.builders.ItemGridBuilder
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.ui.ItemGridSlot
import com.hypixel.hytale.server.core.ui.PatchStyle
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

abstract class PlayerInventoryPage {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }
    
    protected val path: String
    val page: PageBuilder
    
    constructor(path: String) {
        this.path = path
        page = PageBuilder.detachedPage().withLifetime(CustomPageLifetime.CanDismiss)
    }
    
    constructor(path: String, lifetime: CustomPageLifetime) {
        this.path = path
        page = PageBuilder.detachedPage().withLifetime(lifetime)
    }
    
    protected fun updateSlot(grid: ItemGridBuilder, slot: Int, item: ItemStack?) {
        val newSlot = if (ItemStack.isEmpty(item)) ItemGridSlot().setBackground(Style.Slot)
        else ItemGridSlot(item).setBackground(Style.Slot)
        newSlot.isActivatable = true
        grid.updateSlot(newSlot, slot)
    }
    
    protected fun inventoryTemplate(playerRef: PlayerRef, store: Store<EntityStore?>): TemplateProcessor {
        playerRef.reference ?: return TemplateProcessor()
        val player = store.getComponent(playerRef.reference!!, Player.getComponentType())
        val storage = MutableList<UIItem>(36) { UIItem(null, 0) }
        player?.inventory?.storage?.forEach { slot, item ->
            if (!ItemStack.isEmpty(item)) {
                storage[slot.toInt()] = UIItem(item.itemId, item.quantity)
            }
        }
        val hotbar = MutableList(9) { UIItem(null, 0) }
        player?.inventory?.hotbar?.forEach { slot, item ->
            hotbar[slot.toInt()] = UIItem(item.itemId, item.quantity)
        }
        
        return TemplateProcessor()
            .setVariable("inventoryStorage", storage)
            .setVariable("inventoryHotbar", hotbar)
    }
    
    open fun initStyle() : PlayerInventoryPage {
        page.getById("player-inventory-storage", ItemGridBuilder::class.java)?.ifPresent { grid ->
            for (slot in grid.slots) {
                slot.setBackground(Value.of(PatchStyle().setTexturePath(Value.of("Pages/Inventory/Slot.png"))))
            }
        }
        page.getById("player-inventory-hotbar", ItemGridBuilder::class.java)?.ifPresent { grid ->
            for (slot in grid.slots) {
                slot.setBackground(Value.of(PatchStyle().setTexturePath(Value.of("Pages/Inventory/Slot.png"))))
            }
        }
        return this
    }
    
    fun open(playerRef: PlayerRef, store: Store<EntityStore?>): HyUIPage {
        return page.open(playerRef, store)
    }
    
    data class UIItem(val id: String?, val quantity: Int)
}