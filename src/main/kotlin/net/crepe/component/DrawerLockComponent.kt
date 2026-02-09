package net.crepe.component

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerLockComponent : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerLockComponent::class.java) { instance }.build()
        val instance = DrawerLockComponent()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerLockComponent> {
            return SimpleDrawerPlugin.instance.drawerLockComponent
        }
    }
    
    private constructor()
    
    override fun clone(): Component<ChunkStore?>? {
        return instance
    }
}