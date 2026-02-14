package net.crepe.component.drawer

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
            .append(KeyedCodec("Slots", ArrayCodec(DrawerSlot.CODEC) { size -> Array(size) { DrawerSlot() } }),
                { o, v -> o.slots = v },
                { it.slots }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerSlotsContainerComponent> {
            return SimpleDrawerPlugin.instance.drawerSlotsContainerComponent
        }
    }

    val LOGGER = HytaleLogger.forEnclosingClass()
    
    var slots = arrayOf<DrawerSlot>()
    val size: Int
        get() = slots.size
    
    constructor(slots: Array<DrawerSlot>) : this() {
        this.slots = slots.map { it.copy() }.toTypedArray()
    }

    fun setCapacityForSlot(slotIndex: Int, upgradableComponent: DrawerUpgradableComponent?) {
        if (slotIndex !in slots.indices) return
        
        val slot = slots[slotIndex]
        if (ItemStack.isEmpty(slot.storedItem)) {
            slot.capacity = 0
            return
        }
        slot.capacity = (slot.storedItem.item.maxStack.toLong() *
                (36 * (upgradableComponent?.tiers?.getOrNull(upgradableComponent.tier)?.multiplier ?: 1) / size)
                    .toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    
    fun getSlotStackCapacity(upgradableComponent: DrawerUpgradableComponent?): Int {
        return (36 * (upgradableComponent?.tiers?.getOrNull(upgradableComponent.tier)?.multiplier ?: 1) / size)
            .coerceAtMost(Int.MAX_VALUE)
    }
    
    override fun clone(): Component<ChunkStore?> {
        return DrawerSlotsContainerComponent(slots)
    }
    
    override fun toString(): String {
        return "DrawerSlotsContainerComponent(Slots=${slots.contentToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass == other?.javaClass) return false
        
        other as DrawerSlotsContainerComponent

        return slots.contentEquals(other.slots)
    }

    override fun hashCode(): Int {
        return slots.contentHashCode()
    }
    
    class DrawerSlot() {
        companion object {
            val CODEC = BuilderCodec.builder(DrawerSlot::class.java, ::DrawerSlot)
                .append(KeyedCodec("StoredItem", ItemStack.CODEC),
                    { o, v -> o.storedItem = v },
                    { it.storedItem }).add()
                .append(KeyedCodec("StoredQuantity", Codec.INTEGER),
                    { o, v -> o.storedQuantity = v },
                    { it.storedQuantity }).add()
                .append(KeyedCodec("Capacity", Codec.INTEGER),
                    { o, v -> o.capacity = v },
                    { it.capacity }).add()
                .append(KeyedCodec("DisplaysTransform", DisplaysTransform.CODEC),
                    { o, v -> o.displaysTransform = v },
                    { it.displaysTransform }).add()
                .build()
        }
        
        var storedItem: ItemStack = ItemStack.EMPTY
        var storedQuantity: Int = 0
        var capacity: Int = 0
        var displaysTransform = DisplaysTransform()
        
        constructor(storedItem: ItemStack, storedQuantity: Int, capacity: Int, displaysTransform: DisplaysTransform) : this() {
            this.storedItem = storedItem
            this.storedQuantity = storedQuantity
            this.capacity = capacity
            this.displaysTransform = displaysTransform
        }
        
        fun isEmpty(): Boolean {
            return storedQuantity <= 0
        }

        override fun toString(): String {
            return "DrawerSlot(storedItem=$storedItem, storedQuantity=$storedQuantity, capacity=$capacity)"
        }
        
        fun copy(): DrawerSlot {
            return DrawerSlot(
                if (ItemStack.isEmpty(storedItem)) ItemStack.EMPTY else storedItem.withQuantity(1)!!,
                storedQuantity,
                capacity,
                displaysTransform
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass == other?.javaClass) return false
            
            other as DrawerSlot

            if (storedItem != other.storedItem) return false
            if (storedQuantity != other.storedQuantity) return false
            if (capacity != other.capacity) return false
            if (displaysTransform != other.displaysTransform) return false

            return true
        }

        override fun hashCode(): Int {
            var result = storedQuantity
            result = 31 * result + capacity
            result = 31 * result + storedItem.hashCode()
            result = 31 * result + displaysTransform.hashCode()
            return result
        }
        
        data class DisplaysTransform(
            var displayOffset: Vector3d = Vector3d(),
            var displayScale: Float = 1f,
            var numberOffset: Vector3d = Vector3d(),
            var numberScale: Float = 1f
        ) {
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
        }
    }
}