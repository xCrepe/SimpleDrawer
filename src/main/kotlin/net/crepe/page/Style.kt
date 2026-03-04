package net.crepe.page

import com.hypixel.hytale.server.core.ui.PatchStyle
import com.hypixel.hytale.server.core.ui.Value

class Style {
    companion object {
        val Slot = Value.of(PatchStyle().setTexturePath(Value.of("Pages/Inventory/Slot.png")))
    }
}