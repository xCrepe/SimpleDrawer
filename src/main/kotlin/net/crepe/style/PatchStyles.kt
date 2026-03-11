package net.crepe.style

import com.hypixel.hytale.server.core.ui.PatchStyle
import com.hypixel.hytale.server.core.ui.Value

class PatchStyles {
    companion object {
        val Slot = Value.of(PatchStyle(Value.of("Pages/Inventory/Slot.png")))
        val SlotUpgradeStack = Value.of(PatchStyle(Value.of("Pages/Inventory/SlotUpgradeStack.png")))
        val SlotUpgradeRange = Value.of(PatchStyle(Value.of("Pages/Inventory/SlotUpgradeRange.png")))
    }
}