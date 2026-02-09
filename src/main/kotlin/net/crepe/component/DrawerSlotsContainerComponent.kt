package net.crepe.component

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerSlotsContainerComponent() : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerSlotsContainerComponent::class.java, ::DrawerSlotsContainerComponent)
            .append(KeyedCodec("Slots", ArrayCodec(DrawerSlot.CODEC) { size -> Array(size) { null } }),
                { o, v -> o.slots = v },
                { it.slots }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerSlotsContainerComponent> {
            return SimpleDrawerPlugin.instance.drawerSlotsContainerComponent
        }
    }

    val LOGGER = HytaleLogger.forEnclosingClass()
    
    var slots = arrayOf<DrawerSlot>()
    
    constructor(slots: Array<DrawerSlot>) : this() {
        this.slots = slots.map { it.clone() }.toTypedArray()
    }
    
    override fun clone(): Component<ChunkStore?> {
        return DrawerSlotsContainerComponent(slots)
    }
    
    override fun toString(): String {
        return "DrawerSlotsContainerComponent(Slots=${slots.contentToString()})"
    }
    
    class DrawerSlot() : Cloneable {
        companion object {
            val CODEC = BuilderCodec.builder(DrawerSlot::class.java, ::DrawerSlot)
                .append(KeyedCodec("StoredItem", ItemStack.CODEC),
                    { o, v -> o.storedItem = v },
                    { it.storedItem }).add()
                .append(KeyedCodec("StoredQuantity", Codec.LONG),
                    { o, v -> o.storedQuantity = v },
                    { it.storedQuantity }).add()
                .append(KeyedCodec("Capacity", Codec.LONG),
                    { o, v -> o.capacity = v },
                    { it.capacity }).add()
                .append(KeyedCodec("DisplaysTransform", DisplaysTransform.CODEC),
                    { o, v -> o.displaysTransform = v },
                    { it.displaysTransform }).add()
                .build()
        }
        
        var storedItem: ItemStack = ItemStack.EMPTY
        var storedQuantity: Long = 0
        var capacity: Long = 0
        var displaysTransform = DisplaysTransform()
        
        constructor(storedItem: ItemStack, storedQuantity: Long, capacity: Long, displaysTransform: DisplaysTransform) : this() {
            this.storedItem = storedItem
            this.storedQuantity = storedQuantity
            this.capacity = capacity
            this.displaysTransform = displaysTransform
        }

        override fun toString(): String {
            return "DrawerSlot(storedItem=$storedItem, storedQuantity=$storedQuantity, capacity=$capacity)"
        }
        
        public override fun clone(): DrawerSlot {
            return DrawerSlot(storedItem, storedQuantity, capacity, displaysTransform)
        }
        
        class DisplaysTransform() {
            companion object {
                val CODEC = BuilderCodec.builder(DisplaysTransform::class.java, ::DisplaysTransform)
                    .append(KeyedCodec("DisplayOffset", Vector3d.CODEC),
                        { o, v -> o.displayOffset = v },
                        { it.displayOffset }).add()
                    .append(KeyedCodec("DisplayScale", Codec.FLOAT),
                        { o, v -> o.displayScale = v },
                        { it.displayScale }).add()
                    .append(KeyedCodec("NumberOffset", Vector3d.CODEC),
                        { o, v -> o.numberOffset = v },
                        { it.numberOffset }).add()
                    .append(KeyedCodec("NumberScale", Codec.FLOAT),
                        { o, v -> o.numberScale = v },
                        { it.numberScale }).add()
                    .build()
            }

            var displayOffset = Vector3d()
            var displayScale = 1f
            var numberOffset = Vector3d()
            var numberScale = 1f
        }
    }
}