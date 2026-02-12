package net.crepe.provider

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.inventory.ItemStack
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
    
    val tierColor = mapOf(
        1 to "#5AFF43",
        2 to "#28A9FF",
        3 to "#873BFF",
        4 to "#FF9F2F",
    )

    override fun getTooltipData(itemId: String, json: String?): TooltipData? {
        json ?: return null
        if (BlockType.getAssetMap().getAsset(itemId)?.blockEntity?.getComponent(DrawerSlotsContainerComponent.getComponentType()) == null)
            return null
        
        val metadata = BsonDocument.parse(json)
        val lines = mutableListOf<String>()
        var hashInput = "drawer-data:"
        metadata["Slots"]?.asDocument().let { slots ->
            lines.add("<color is=\"#31FF6D\">Content:</color>")
            slots?.forEach { slot, data ->
                val slotData = data.asDocument()
                ItemStack.CODEC.decode(slotData["StoredItem"]?.asDocument())?.let { item ->
                    val quantity = slotData["StoredQuantity"]?.asInt32()?.value?.toLong()!!
                    lines += "${item.itemId} <color is=\"#31FF6D\">x$quantity</color>"
                    hashInput = "$hashInput${item.itemId}x$quantity;"
                }
            }
        }
        metadata["Tier"]?.asInt32()?.value?.let { tier ->
            lines.add(0, "<color is=\"#31FF6D\">Tier:</color> <color is=\"${tierColor.getOrDefault(tier, "#EFBF04")}\">$tier</color>")
            hashInput = "${hashInput}tier-$tier"
        }
        
        val tooltip = TooltipData.builder().hashInput(hashInput)
        lines.forEach { line ->
            tooltip.addLine(line)
        }
        return tooltip.build()
    }
}