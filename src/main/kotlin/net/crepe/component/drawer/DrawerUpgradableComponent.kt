package net.crepe.component.drawer

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.codec.validation.validator.ArraySizeRangeValidator
import com.hypixel.hytale.codec.validation.validator.RangeValidator
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.SimpleDrawerPlugin

class DrawerUpgradableComponent : Component<ChunkStore?> {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerUpgradableComponent::class.java, ::DrawerUpgradableComponent)
            .append(KeyedCodec("Tier", Codec.INTEGER),
                { o, v -> o.tier = v },
                { it.tier }).add()
            .append(KeyedCodec("Tiers", ArrayCodec(Tier.CODEC) { size -> Array(size) { Tier() } }),
                { o, v -> o.tiers = v },
                { it.tiers }).add()
            .build()
        
        fun getComponentType(): ComponentType<ChunkStore?, DrawerUpgradableComponent> {
            return SimpleDrawerPlugin.instance.drawerUpgradableComponent
        }
    }
    
    var tier: Int = 0
    var tiers = arrayOf<Tier>()
    
    constructor(tier: Int, tiers: Array<Tier>) {
        this.tier = tier
        this.tiers = tiers
    }
    
    constructor() : this(0, arrayOf()) {}
    
    override fun clone(): Component<ChunkStore?> {
        return DrawerUpgradableComponent(tier, tiers)
    }

    override fun toString(): String {
        return "DrawerUpgradableComponent(tier=$tier, tiers=${tiers.contentToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DrawerUpgradableComponent

        if (tier != other.tier) return false
        if (!tiers.contentEquals(other.tiers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tier
        result = 31 * result + tiers.contentHashCode()
        return result
    }
    
    class Tier {
        companion object {
            val CODEC = BuilderCodec.builder(Tier::class.java, ::Tier)
                .append(KeyedCodec("Multiplier", Codec.INTEGER),
                    { o, v -> o.multiplier = v },
                    { it.multiplier }).addValidator(RangeValidator(1, 596523, true)).add()
                .append(KeyedCodec("ItemsRequired", ArrayCodec(Item.CODEC) { size -> Array(size) { Item() } }),
                    { o, v -> o.itemsRequired = v },
                    { it.itemsRequired }).addValidator(ArraySizeRangeValidator(0, 45)).add()
                .build()
        }
        
        var multiplier: Int = 1
        var itemsRequired = arrayOf<Item>()
        
        constructor(multiplier: Int, itemsRequired: Array<Item>) {
            this.multiplier = multiplier
            this.itemsRequired = itemsRequired
        }
        
        constructor() : this(1, arrayOf()) {}
        
        override fun toString(): String {
            return "Tier(multiplier=$multiplier, itemsRequired=${itemsRequired.contentToString()})"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Tier

            if (multiplier != other.multiplier) return false
            if (!itemsRequired.contentEquals(other.itemsRequired)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = multiplier
            result = 31 * result + itemsRequired.contentHashCode()
            return result
        }
        
        data class Item(var itemId: String? = null, var quantity: Int = 0) {
            companion object {
                val CODEC = BuilderCodec.builder(Item::class.java, ::Item)
                    .append(KeyedCodec("ItemId", Codec.STRING),
                        { o, v -> o.itemId = v },
                        { it.itemId }).add()
                    .append(KeyedCodec("Quantity", Codec.INTEGER),
                        { o, v -> o.quantity = v },
                        { it.quantity }).add()
                    .build()
            }
        }
    }
}