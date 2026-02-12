package net.crepe.component.drawer

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerSlotHitComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerSlotHitComponent::class.java, ::DrawerSlotHitComponent)
            .append(KeyedCodec("HitIndex", Codec.INTEGER),
                { o, v -> o.hitIndex = v },
                { it.hitIndex }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerSlotHitComponent> {
            return SimpleDrawerPlugin.instance.drawerSlotHitComponent
        }
    }
    
    var hitIndex: Int? = null
    
    constructor(hitIndex: Int?) : this() {
        this.hitIndex = hitIndex
    }
    
    override fun clone(): Component<ChunkStore?>? {
        return DrawerSlotHitComponent(hitIndex)
    }

    override fun toString(): String {
        return "DrawerSlotHitComponent(hitIndex=$hitIndex)"
    }
}