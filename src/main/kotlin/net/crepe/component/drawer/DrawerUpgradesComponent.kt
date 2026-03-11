package net.crepe.component.drawer

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin
import net.crepe.component.common.DataItem

class DrawerUpgradesComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerUpgradesComponent::class.java, ::DrawerUpgradesComponent)
            .append(KeyedCodec("Multiplier", Codec.INTEGER),
                { o, v -> o.multiplier = v },
                { it.multiplier }).add()
            .append(KeyedCodec("Void", Codec.BOOLEAN),
                { o, v -> o.void = v },
                { it.void }).add()
            .append(KeyedCodec("Upgrades", ArrayCodec(DataItem.CODEC) { size -> Array(size) { null } }),
                { o, v -> o.upgrades = v.toMutableList() },
                { o -> o.upgrades.toTypedArray() }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerUpgradesComponent> {
            return SimpleDrawerPlugin.instance.drawerUpgradesComponent
        }
    }
    
    val upgradeTypes = listOf("Stack", "Void").map { "SimpleDrawer_Upgrade_$it" }

    var multiplier = 1
    var void = false
    var upgrades = mutableListOf<DataItem?>()

    constructor(multiplier: Int, void: Boolean, upgrades: MutableList<DataItem?>) : this() {
        this.multiplier = multiplier
        this.void = void
        this.upgrades = upgrades.toMutableList()
    }

    override fun clone(): Component<ChunkStore?> {
        return DrawerUpgradesComponent(multiplier, void, upgrades)
    }
}