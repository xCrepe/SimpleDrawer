package net.crepe.component

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin
import java.util.UUID

class DrawerBoundDisplayComponent(var displayEntity: UUID?) : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerBoundDisplayComponent::class.java, ::DrawerBoundDisplayComponent)
            .append(KeyedCodec("DisplayEntity", Codec.UUID_BINARY),
                { o, v -> o.displayEntity = v },
                { it.displayEntity }).add()
            .append(KeyedCodec("NumberDisplays", ArrayCodec(Codec.UUID_BINARY) { size -> Array<UUID?>(size) { null } }),
                { o, v -> o.numberDisplays = v.toMutableList() },
                { it.numberDisplays.toTypedArray() }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerBoundDisplayComponent> {
            return SimpleDrawerPlugin.instance.drawerBoundDisplay
        }
    }
    
    var numberDisplays = mutableListOf<UUID?>(null)
    
    constructor(uuid: UUID?, numberDisplays: MutableList<UUID?>) : this(uuid) {
        this.numberDisplays = numberDisplays
    }
    
    constructor() : this(null) {}
    
    override fun clone(): Component<ChunkStore?>? {
        return DrawerBoundDisplayComponent(displayEntity, numberDisplays)
    }

    override fun toString(): String {
        return "DrawerBoundDisplayComponent(displayEntity=$displayEntity, numberDisplays=$numberDisplays)"
    }
}