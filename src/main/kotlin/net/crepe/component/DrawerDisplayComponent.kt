package net.crepe.component

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.codec.codecs.map.MapCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin
import java.util.UUID

class DrawerDisplayComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerDisplayComponent::class.java, ::DrawerDisplayComponent)
            .append(KeyedCodec("SlotsDisplays", ArrayCodec(SlotDisplays.CODEC) { size -> Array(size) { SlotDisplays() } }),
                { o, v -> o.slotsDisplays = v },
                { it.slotsDisplays }).add()
            .append(KeyedCodec("IconDisplays", MapCodec(Codec.UUID_BINARY, ::HashMap)),
                { o, v -> o.iconDisplays = HashMap(v) },
                { it.iconDisplays }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerDisplayComponent> {
            return SimpleDrawerPlugin.instance.drawerDisplayComponent
        }
    }
    
    var slotsDisplays = arrayOf<SlotDisplays>()
    var iconDisplays = HashMap<String, UUID?>()
    
    constructor(slotsDisplays: Array<SlotDisplays>, iconDisplays: HashMap<String, UUID?>) : this() {
        this.slotsDisplays = slotsDisplays.clone()
        this.iconDisplays = HashMap(iconDisplays)
    }
    
    override fun clone(): Component<ChunkStore?>? {
        return DrawerDisplayComponent(slotsDisplays, iconDisplays)
    }

    override fun toString(): String {
        return "DrawerDisplayComponent(slotDisplays=${slotsDisplays.contentToString()})"
    }
    
    class SlotDisplays() {
        companion object {
            val CODEC = BuilderCodec.builder(SlotDisplays::class.java, ::SlotDisplays)
                .append(KeyedCodec("DisplayEntity", Codec.UUID_BINARY),
                    { o, v -> o.displayEntity = v },
                    { it.displayEntity }).add()
                .append(KeyedCodec("NumberDisplays", ArrayCodec(Codec.UUID_BINARY) { size -> Array<UUID?>(size) { null } }),
                    { o, v -> o.numberDisplays = v.toMutableList() },
                    { it.numberDisplays.toTypedArray() }).add()
                .build()
        }
        
        var displayEntity: UUID? = null
        var numberDisplays = mutableListOf<UUID?>(null)
        
        constructor(displayEntity: UUID?, numberDisplays: MutableList<UUID?>) : this() {
            this.displayEntity = displayEntity
            this.numberDisplays = numberDisplays
        }

        override fun toString(): String {
            return "SlotDisplays(displayEntity=$displayEntity, numberDisplays=$numberDisplays)"
        }
    }
}