package net.crepe.provider

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.i18n.I18nModule
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import org.bson.BsonDocument
import org.herolias.tooltips.api.TooltipData
import org.herolias.tooltips.api.TooltipPriority
import org.herolias.tooltips.api.TooltipProvider

class DrawerTooltipProvider : TooltipProvider {
    override fun getProviderId(): String {
        return "simple-drawer:drawer-tooltips"
    }

    override fun getPriority(): Int {
        return TooltipPriority.DEFAULT
    }
    
    val upgradeColor = mapOf(
        "Iron" to "#FFFFFF",
        "Thorium" to "#5AFF43",
        "Cobalt" to "#28A9FF",
        "Adamantite" to "#D5443B",
    )

    override fun getTooltipData(itemId: String, json: String?): TooltipData? {
        json ?: return null
        if (BlockType.getAssetMap().getAsset(itemId)?.blockEntity?.getComponent(DrawerSlotsContainerComponent.getComponentType()) == null)
            return null
        
        val metadata = BsonDocument.parse(json)
        val lines = mutableListOf<String>()
        var hashInput = "drawer-data:"
        metadata["Locked"]?.asBoolean()?.value?.let {
            lines += "<color is=\"#FFFFFF\">Locked</color>"
            hashInput = "${hashInput}locked;"
        }
        metadata["Slots"]?.asDocument()?.let { slots ->
            lines += "<color is=\"#31FF6D\">Content:</color>"
            slots.forEach { (slot, data) ->
                val slotData = data.asDocument()
                ItemStack.CODEC.decode(slotData["StoredItem"]?.asDocument())?.let { item ->
                    val quantity = slotData["StoredQuantity"]?.asInt32()?.value?.toLong()!!
                    val name = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, item.item.translationKey)
                    lines += "- $name <color is=\"#31FF6D\">x$quantity</color>"
                    hashInput = "$hashInput${item.itemId}x$quantity;"
                }
            }
        }
        metadata["Upgrades"]?.asDocument()?.let { upgrades ->
            lines += "<color is=\"#31FF6D\">Upgrades:</color>"
            for (i in 0..3) {
                upgrades["$i"]?.asDocument()?.let { slot ->
                    val id = slot["Id"]?.asString()?.value ?: return@let
                    val name = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, ItemStack(id).item.translationKey)
                    lines += "- <color is=\"${upgradeColor[upgradeColor.keys.find { id.contains(it) }] ?: "#FFFFFF"}\">$name</color>"
                    hashInput = "$hashInput${id};"
                }
            }
        }
        
        val tooltip = TooltipData.builder().hashInput(hashInput)
        lines.forEach { line ->
            tooltip.addLine(line)
        }
        return tooltip.build()
    }
}