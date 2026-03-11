package net.crepe.page

import au.ellie.hyui.builders.ContainerBuilder
import au.ellie.hyui.builders.GroupBuilder
import au.ellie.hyui.builders.HyUIAnchor
import au.ellie.hyui.builders.HyUIPage
import au.ellie.hyui.builders.HyUIStyle
import au.ellie.hyui.builders.ItemGridBuilder
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.builders.PageOverlayBuilder
import au.ellie.hyui.builders.UIElementBuilder
import au.ellie.hyui.events.SlotClickingEventData
import au.ellie.hyui.events.UIContext
import au.ellie.hyui.types.LayoutMode
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.ui.ItemGridSlot
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.style.PatchStyles
import kotlin.jvm.optionals.getOrNull

open class PlayerInventoryPage<T: PlayerInventoryPage<T>> {
    companion object {
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }
    
    @Suppress("UNCHECKED_CAST")
    protected val self
        get() = this as T

    protected val emptySlot: ItemGridSlot
        get() = ItemGridSlot().setBackground(PatchStyles.Slot).apply { isActivatable = true }
    
    protected val playerStorageGrid: ItemGridBuilder = ItemGridBuilder()
        .withId("inventory-storage")
        .withInventorySectionId(-2)
        .withAreItemsDraggable(false)
        .withSlotsPerRow(9)
        .withStyle(HyUIStyle()
            .set("SlotSpacing", 2)
            .set("SlotSize", 74)
        )
        .withSlots((0..35).asSequence().map { emptySlot }.toList())
    
    protected val playerHotbarGrid: ItemGridBuilder = ItemGridBuilder()
        .withId("inventory-hotbar")
        .withAnchor(HyUIAnchor()
            .setTop(6)
        )
        .withInventorySectionId(-1)
        .withAreItemsDraggable(false)
        .withSlotsPerRow(9)
        .withStyle(HyUIStyle()
            .set("SlotSpacing", 2)
            .set("SlotSize", 74)
        )
        .withSlots((0..8).asSequence().map { emptySlot }.toList())
    
    protected open val custom: UIElementBuilder<*>? = null
    
    protected val page: PageBuilder by lazy {
        PageBuilder()
            .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
            .addElement(
                PageOverlayBuilder()
                    .withId("root")
                    .withLayoutMode(LayoutMode.Bottom)
                    .apply { custom?.let { addChild(it) } }
                    .addChild(
                        ContainerBuilder()
                            .withId("inventory")
                            .withTitleText("INVENTORY")
                            .withAnchor(
                                HyUIAnchor()
                                    .setWidth(715)
                                    .setHeight(455)
                                    .setBottom(20)
                            )
                            .addContentChild(
                                GroupBuilder()
                                    .withLayoutMode(LayoutMode.Middle)
                                    .addChild(playerStorageGrid)
                                    .addChild(playerHotbarGrid)
                            )
                    )
            )
    }
    
    protected var player: Player? = null
    
    fun forPlayer(player: Player): T {
        this.player = player
        
        page.getById("inventory-storage", ItemGridBuilder::class.java).getOrNull()?.let { grid ->
            player.inventory.storage.forEach { slot, item ->
                grid.updateSlot(
                    if (ItemStack.isEmpty(item))
                        emptySlot
                    else
                        emptySlot.setItemStack(item.withMetadata(null)),
                    slot.toInt()
                )
            }
            
            page.addEventListener("inventory-storage", CustomUIEventBindingType.SlotClicking) { data, ctx ->
                onInventorySlotClick(data as SlotClickingEventData, ctx, grid, player.inventory.storage)
            }
        }
        page.getById("inventory-hotbar", ItemGridBuilder::class.java).getOrNull()?.let { grid ->
            player.inventory.hotbar.forEach { slot, item ->
                grid.updateSlot(
                    if (ItemStack.isEmpty(item))
                        emptySlot
                    else
                        emptySlot.setItemStack(item.withMetadata(null)),
                    slot.toInt()
                )
            }

            page.addEventListener("inventory-hotbar", CustomUIEventBindingType.SlotClicking) { data, ctx ->
                onInventorySlotClick(data as SlotClickingEventData, ctx, grid, player.inventory.hotbar)
            }
        }
        
        return self
    }

    protected open fun onInventorySlotClick(data: SlotClickingEventData, ctx: UIContext, grid: ItemGridBuilder, container: ItemContainer) {}
    
    protected fun updateSlot(grid: ItemGridBuilder, slot: Int, itemStack: ItemStack?) {
        grid.updateSlot(
            if (ItemStack.isEmpty(itemStack))
                emptySlot
            else
                emptySlot.setItemStack(itemStack!!),
            slot
        )
    }
    
    fun open(player: PlayerRef, store: Store<EntityStore?>): HyUIPage {
        return page.open(player, store)
    }
}