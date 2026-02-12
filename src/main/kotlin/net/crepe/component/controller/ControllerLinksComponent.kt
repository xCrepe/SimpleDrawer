package net.crepe.component.controller

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin
import net.crepe.component.drawer.DrawerSlotsContainerComponent

class ControllerLinksComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerLinksComponent::class.java, ::ControllerLinksComponent)
            .append(
                KeyedCodec("Radius", Codec.INTEGER),
                { o, v -> o.radius = v },
                { o -> o.radius }).add()
            .append(
                KeyedCodec("Drawers", ArrayCodec(Vector3i.CODEC) { size -> Array(size) { Vector3i() } }),
                { o, v -> o.drawers = v.toMutableList() },
                { o -> o.drawers.toTypedArray() }).add()
            .append(
                KeyedCodec(
                    "Containers",
                    ArrayCodec(DrawerSlotsContainerComponent.Companion.CODEC) { size -> Array(size) { DrawerSlotsContainerComponent() } }),
                { o, v -> o.containers = v.toMutableList() },
                { o -> o.containers.toTypedArray() }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, ControllerLinksComponent> {
            return SimpleDrawerPlugin.Companion.instance.controllerLinksComponent
        }
    }
    
    var radius = 8
    var drawers = mutableListOf<Vector3i>()
    var containers = mutableListOf<DrawerSlotsContainerComponent>()
    
    constructor(radius: Int, drawers: MutableList<Vector3i>, containers: MutableList<DrawerSlotsContainerComponent>) : this() {
        this.radius = radius
        this.drawers = drawers.toMutableList()
        this.containers = containers.toMutableList()
    }
    
    override fun clone(): Component<ChunkStore?> {
        return ControllerLinksComponent(radius, drawers, containers)
    }

    override fun toString(): String {
        return "ControllerLinksComponent(radius=$radius, linkedDrawers=$drawers, linkedContainers=$containers)"
    }
}