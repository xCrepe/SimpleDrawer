package net.crepe.component.controller

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.codec.validation.validator.ArraySizeRangeValidator
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin
import net.crepe.component.common.DataItem

class ControllerUpgradesComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerUpgradesComponent::class.java, ::ControllerUpgradesComponent)
            .append(KeyedCodec("Upgrades", ArrayCodec(DataItem.CODEC) { size -> Array(size) { null } }),
                { o, v -> o.upgrades = v.toMutableList() },
                { o -> o.upgrades.toTypedArray() }).addValidator(ArraySizeRangeValidator(0, 9)).add()
            .build()
        
        fun getComponentType() : ComponentType<ChunkStore?, ControllerUpgradesComponent> {
            return SimpleDrawerPlugin.instance.controllerUpgradesComponent
        }
    }
    
    var upgrades = mutableListOf<DataItem?>()
    
    constructor(upgrades: MutableList<DataItem?>) : this() {
        this.upgrades = upgrades.toMutableList()
    }
    
    override fun clone(): Component<ChunkStore?> {
        return ControllerUpgradesComponent(upgrades)
    }
}