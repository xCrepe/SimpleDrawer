package net.crepe.component.common

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec


data class DataItem(var itemId: String? = null, var quantity: Int = 0) {
    companion object {
        val CODEC = BuilderCodec.builder(DataItem::class.java, ::DataItem)
            .append(KeyedCodec("ItemId", Codec.STRING),
                { o, v -> o.itemId = v },
                { it.itemId }).add()
            .append(KeyedCodec("Quantity", Codec.INTEGER),
                { o, v -> o.quantity = v },
                { it.quantity }).add()
            .build()
        
        fun isEmpty(item: DataItem?): Boolean {
            return item == null || item.isEmpty()
        }
    }
    
    fun isEmpty(): Boolean {
        return itemId == null || quantity <= 0
    }
}