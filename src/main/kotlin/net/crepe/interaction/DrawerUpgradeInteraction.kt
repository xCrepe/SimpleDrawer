package net.crepe.interaction

import au.ellie.hyui.builders.ContainerBuilder
import au.ellie.hyui.builders.CustomButtonBuilder
import au.ellie.hyui.builders.ItemGridBuilder
import au.ellie.hyui.builders.LabelBuilder
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.ui.ItemGridSlot
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.components.DrawerContainerComponent
import kotlin.math.min


class DrawerUpgradeInteraction : SimpleBlockInteraction() {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerUpgradeInteraction::class.java, ::DrawerUpgradeInteraction, SimpleBlockInteraction.CODEC)
            .build()
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
        val ref = ctx.entity
        val player = cmdBuffer.getComponent(ref, Player.getComponentType())!!
        val playerRef = cmdBuffer.getComponent(ref, PlayerRef.getComponentType())
        val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)!!
        val component = world.chunkStore.store.getComponent(blockRef, DrawerUpgradableComponent.getComponentType()) ?: return
        val containerComponent = world.chunkStore.store.getComponent(blockRef, DrawerContainerComponent.getComponentType())!!
        val tiers = component.tiers
        
        val template = TemplateProcessor()
            .setVariable("tierUpgrade", "${component.tier}${if (component.tier < tiers.size - 1) " -> ${component.tier + 1}" else " (MAX)"}")
            .setVariable("capacityUpgrade",
                "${36 * (tiers.getOrNull(component.tier)?.multiplier ?: 1)} stacks${if (component.tier < tiers.size - 1)
                    " -> ${36 * (tiers.getOrNull(component.tier + 1)?.multiplier ?: 1)} stacks"
                else ""}")
            .setVariable("rowSlots", min(6, tiers.getOrNull(component.tier + 1)?.itemsRequired?.size ?: 1))
            .setVariable("requiredItems", tiers.getOrNull(component.tier + 1)?.itemsRequired ?: arrayOf<DrawerUpgradableComponent.Tier.Item>())
            .setVariable("isDisabled", (component.tier >= tiers.size - 1) ||
                    ((tiers.getOrNull(component.tier + 1)?.itemsRequired ?: arrayOf()).any {
                        !playerHasItem(player, it.itemId!!, it.quantity)
                    } && player.gameMode != GameMode.Creative))
        val page = PageBuilder.pageForPlayer(playerRef)
            .loadHtml("Pages/DrawerUpgrade.html", template)
            .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
        
        page.getById("upgrade-button", CustomButtonBuilder::class.java).ifPresent { button ->
            button.addEventListener(CustomUIEventBindingType.Activating) { data, ctx ->
                if (component.tier >= tiers.size - 1) {
                    return@addEventListener
                }
                
                var itemsRequired = tiers.getOrNull(component.tier + 1)?.itemsRequired ?: arrayOf()
                if (itemsRequired.any { !playerHasItem(player, it.itemId!!, it.quantity) } && player.gameMode != GameMode.Creative) {
                    return@addEventListener
                } else {
                    if (player.gameMode != GameMode.Creative) {
                        itemsRequired.forEach {
                            val itemStack = ItemStack(it.itemId!!, it.quantity)
                            if (player.inventory.storage.canRemoveItemStack(itemStack)) {
                                player.inventory.storage.removeItemStack(itemStack)
                            } else if (player.inventory.hotbar.canRemoveItemStack(itemStack)) {
                                player.inventory.hotbar.removeItemStack(itemStack)
                            }
                        }
                    }
                }

                containerComponent.capacity = containerComponent.capacity *
                        (tiers.getOrNull(component.tier + 1)?.multiplier ?: 1) /
                        (tiers.getOrNull(component.tier)?.multiplier ?: 1)
                component.tier += 1
                itemsRequired = tiers.getOrNull(component.tier + 1)?.itemsRequired ?: arrayOf()
                if (component.tier >= component.tiers.size - 1 || itemsRequired.any {
                        !playerHasItem(player, it.itemId!!, it.quantity)
                    } && player.gameMode != GameMode.Creative) {
                    button.withDisabled(true)
                }

                ctx.getById("upgrade-ui", ContainerBuilder::class.java).ifPresent {
                    it.withTitleText("Tier ${component.tier}${if (component.tier < tiers.size - 1) " -> ${component.tier + 1}" else " (MAX)"}")
                }
                ctx.getById("upgrade-description", LabelBuilder::class.java).ifPresent { 
                    it.withText("Capacity: ${36 * (tiers.getOrNull(component.tier)?.multiplier ?: 1)} stacks${if (component.tier < tiers.size - 1)
                        " -> ${36 * (tiers.getOrNull(component.tier + 1)?.multiplier ?: 1)} stacks"
                    else ""}")
                }
                ctx.getById("required-items", ItemGridBuilder::class.java).ifPresent { grid ->
                    grid.withSlotsPerRow(min(6, itemsRequired.size))
                    grid.withSlots(itemsRequired.map { item ->
                        ItemGridSlot(ItemStack(item.itemId!!, item.quantity))
                    })
                }
                ctx.updatePage(true)
            }
        }
        
        page.open(cmdBuffer.store)
    }
    
    private fun playerHasItem(player: Player, itemId: String, quantity: Int): Boolean {
        val inventory = player.inventory
        val storage = inventory.storage
        val hotbar = inventory.hotbar
        
        return (storage.canRemoveItemStack(ItemStack(itemId, quantity)) ||
                hotbar.canRemoveItemStack(ItemStack(itemId, quantity)))
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