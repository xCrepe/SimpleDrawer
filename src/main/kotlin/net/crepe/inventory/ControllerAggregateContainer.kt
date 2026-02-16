package net.crepe.inventory

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import java.util.function.Supplier

class ControllerAggregateContainer() : SimpleItemContainer(), IDrawerContainer {
    companion object {
        val CODEC = BuilderCodec.builder(ControllerAggregateContainer::class.java, ::ControllerAggregateContainer,
            SimpleItemContainer.CODEC)
            .append(KeyedCodec("WorldName", Codec.STRING),
                { o, v -> o.worldName = v },
                { it.worldName }).add()
            .append(KeyedCodec("Drawers", ArrayCodec(DrawerContainerWrapper.CODEC) { size -> Array<DrawerContainerWrapper>(size) { DrawerContainerWrapper() } }),
                { o, v -> o.drawers = v.toMutableList() },
                { it.drawers.toTypedArray() }).add()
            .build()
    }
    
    private var worldName: String = ""
    private var drawers = mutableListOf<DrawerContainerWrapper>()
    
    private var world: World? = null
    val size
        get() = drawers.size
    
    constructor(worldName: String) : this() {
        this.worldName = worldName
        ensureWorld()
    }
    
    constructor(other: ControllerAggregateContainer) : this(other.worldName) {
        drawers = other.drawers.toMutableList()
    }
    
    private fun ensureWorld() {
        if (world != null) return
        world = Universe.get().getWorld(worldName) ?: throw IllegalArgumentException("World $worldName not found in universe")
    }
    
    internal fun addDrawer( container: DrawerContainerWrapper?) {
        container ?: return
        drawers.add(container)
    }
    
    private fun getValidDrawers(): List<DrawerContainerWrapper> {
        ensureWorld()
        return drawers.filter { it.accessedTick != world?.tick }
    }
    
    internal fun internal_getDrawerAndSlot(slot: Short): Pair<DrawerContainerWrapper?, Short?> {
        if (slot < 0) return Pair(null, null)

        var slot = slot
        for (drawer in getValidDrawers()) {
            if (slot < drawer.capacity) {
                return Pair(drawer, slot)
            } else {
                slot = (slot - drawer.capacity).toShort()
            }
        }
        return Pair(null, null)
    }
    
    override fun <V : Any?> writeAction(action: Supplier<V?>): V? {
        return super.writeAction(action)
    }

    override fun internal_getSlot(slot: Short): ItemStack? {
        val (drawer, slot) = internal_getDrawerAndSlot(slot)
        return drawer?.getSlot(slot!!)
    }
    
    override fun getSlot(slot: Short): ItemStack? {
        return internal_getSlot(slot)
    }

    override fun internal_setSlot(slot: Short, itemStack: ItemStack?): ItemStack? {
        val (drawer, slot) = internal_getDrawerAndSlot(slot)
        return drawer?.setSlot(slot!!, itemStack)
    }
    
    override fun setSlot(slot: Short, itemStack: ItemStack?): ItemStack? {
        return internal_setSlot(slot, itemStack)
    }

    override fun internal_removeSlot(slot: Short): ItemStack? {
        return internal_setSlot(slot, ItemStack.EMPTY)
    }
    
    override fun removeSlot(slot: Short): ItemStack? {
        return internal_removeSlot(slot)
    }
    
    override val slotCount: Short
        get() = getCapacity()
    
    override fun getCapacity(): Short {
        return getValidDrawers().sumOf { it.capacity.toInt() }.toShort()
    }
    
    override fun getSlotStackCapacity(slot: Short): Int {
        val (drawer, slot) = internal_getDrawerAndSlot(slot)
        return drawer?.getSlotStackCapacity(slot!!) ?: 0
    }

    override fun internal_clear(): ClearTransaction {
        val itemStacks = mutableListOf<ItemStack>()
        for (drawer in getValidDrawers()) {
            val transaction = drawer.clear()
            itemStacks.addAll(transaction.items)
        }
        return ClearTransaction(true, 0, itemStacks.toTypedArray())
    }
    
    override fun testCantAddToSlot(slot: Short, itemStack: ItemStack, slotItemStack: ItemStack?): Boolean {
        val (drawer, slot) = internal_getDrawerAndSlot(slot)
        return drawer?.testCantAddToSlot(slot!!, itemStack, slotItemStack) ?: false
    }

    override fun addItemStack(
        itemStack: ItemStack,
        allOrNothing: Boolean,
        fullStacks: Boolean,
        filter: Boolean
    ): ItemStackTransaction {
        val transaction = InternalDrawerContainerUtilItemStack.addItemStack(this, itemStack, allOrNothing, fullStacks, filter)
        this.sendUpdate(transaction)
        return transaction
    }

    override fun dropAllItemStacks(): List<ItemStack?> {
        return listOf()
    }

    override fun clone(): SimpleItemContainer {
        return ControllerAggregateContainer(this)
    }

    override fun isEmpty(): Boolean {
        this.lock.readLock().lock()

        var res = false
        try {
            if (getValidDrawers().any { !it.isEmpty() }) {
                return false
            }

            res = true
        } finally {
            this.lock.readLock().unlock()
        }

        return res
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (javaClass != o?.javaClass) return false

        o as ControllerAggregateContainer

        if (worldName != o.worldName) return false
        if (drawers != o.drawers) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * worldName.hashCode() + drawers.hashCode()
    }
}