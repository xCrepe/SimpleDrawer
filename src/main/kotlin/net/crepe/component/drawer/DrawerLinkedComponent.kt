package net.crepe.component.drawer

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerLinkedComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerLinkedComponent::class.java, ::DrawerLinkedComponent)
            .append(KeyedCodec("Controller", Vector3i.CODEC),
                { o, v -> o.controller = v },
                { it.controller }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerLinkedComponent> {
            return SimpleDrawerPlugin.instance.drawerLinkedComponent
        }
    }
    
    var controller = Vector3i()
    
    constructor(linkedController: Vector3i): this() {
        this.controller = linkedController.clone()
    }
    
    override fun clone(): Component<ChunkStore?> {
        return DrawerLinkedComponent(controller)
    }
}