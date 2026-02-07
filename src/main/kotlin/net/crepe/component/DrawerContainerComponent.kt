package net.crepe.components

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerContainerComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerContainerComponent::class.java, ::DrawerContainerComponent)
            .append(KeyedCodec("StoredItem", ItemStack.CODEC),
                { o, v -> o.storedItem = v },
                { it.storedItem }).add()
            .append(KeyedCodec("StoredQuantity", Codec.INTEGER),
                { o, v -> o.storedQuantity = v },
                { it.storedQuantity }).add()
            .append(KeyedCodec("Capacity", Codec.INTEGER),
                { o, v -> o.capacity = v },
                { it.capacity }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerContainerComponent> {
            return SimpleDrawerPlugin.instance.drawerContainerComponent
        }
    }

    
    val logger = HytaleLogger.forEnclosingClass()
    var storedItem: ItemStack = ItemStack.EMPTY
    var storedQuantity: Int = 0
    var capacity: Int = 0
    
    constructor(storedItem: ItemStack, storedQuantity: Int, capacity: Int) : this() {
        this.storedItem = storedItem
        this.storedQuantity = storedQuantity
        this.capacity = capacity
    }
    
    override fun clone(): Component<ChunkStore?> {
        return DrawerContainerComponent(storedItem, storedQuantity, capacity)
    }
    
    override fun toString(): String {
        return "DrawerContainerComponent(storedItem=$storedItem, storedQuantity=$storedQuantity, capacity=$capacity)"
    }
}