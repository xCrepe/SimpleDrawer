package net.crepe.inventory

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.drawer.DrawerLockComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.component.drawer.DrawerUpgradesComponent
import net.crepe.system.DrawerSystem
import java.util.function.Supplier

class DrawerContainerWrapper() : SimpleItemContainer(), IDrawerContainer {
    companion object {
        val CODEC = BuilderCodec.builder(DrawerContainerWrapper::class.java, ::DrawerContainerWrapper,
            SimpleItemContainer.CODEC)
            .append(KeyedCodec("Position", Vector3i.CODEC),
                { o, v -> o.pos = v },
                { it.pos }).add()
            .append(KeyedCodec("WorldName", Codec.STRING),
                { o, v -> o.worldName = v },
                { it.worldName }).add()
            .append(KeyedCodec("Component", DrawerSlotsContainerComponent.CODEC),
                { o, v -> o.component = v },
                { it.component }).add()
            .append(KeyedCodec("UpgradableComponent", DrawerUpgradesComponent.CODEC),
                { o, v -> o.upgradesComponent = v },
                { it.upgradesComponent }).add()
            .build()
        
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
    }

    private lateinit var pos: Vector3i
    private lateinit var worldName: String
    private lateinit var component: DrawerSlotsContainerComponent
    private var ref: Ref<ChunkStore?>? = null
    private var upgradesComponent: DrawerUpgradesComponent? = null
    private var world: World? = null
    var accessedTick: Long = -1
    
    constructor(worldName: String, pos: Vector3i) : this() {
        this.worldName = worldName
        this.pos = pos.clone()
        ensureRef()
        this.component = ref!!.store.getComponent(ref!!, DrawerSlotsContainerComponent.getComponentType()) ?: DrawerSlotsContainerComponent()
        this.upgradesComponent = ref!!.store.getComponent(ref!!, DrawerUpgradesComponent.getComponentType())
    }
    
    constructor(other: DrawerContainerWrapper) : this(other.worldName, other.pos) {}

    override fun toString(): String {
        return "DrawerContainerWrapper(WorldName=$worldName, Position=$pos, Component=$component, UpgradesComponent=$upgradesComponent)"
    }

    override fun <V> writeAction(action: Supplier<V?>): V? {
        return super.writeAction(action)
    }
    
    private fun ensureWorld() {
        if (world != null) return
        world = Universe.get().getWorld(worldName) ?: throw IllegalArgumentException("World $worldName not found in universe")
    }

    private fun ensureRef() {
        if (ref != null) return
        ensureWorld()
        ref = BlockModule.getBlockEntity(world!!, pos.x, pos.y, pos.z)
            ?: throw IllegalArgumentException("No block entity found at position $pos in world ${world!!.name}")
    }
    
    override fun internal_getSlot(slot: Short): ItemStack? {
        return if (slot in 0..<component.size)
            component.slots[slot.toInt()].storedItem.withQuantity(component.slots[slot.toInt()].storedQuantity)
        else null
    }
    
    override fun getSlot(slot: Short): ItemStack? {
        return internal_getSlot(slot)
    }

    override fun getSlotItem(slot: Short): ItemStack? {
        return if (slot in 0..<component.size)
            component.slots[slot.toInt()].storedItem
        else null
    }

    override fun getSlotQuantity(slot: Short): Int {
        return if (slot in 0..<component.size)
            component.slots[slot.toInt()].storedQuantity
        else 0
    }

    override fun internal_setSlot(slot: Short, itemStack: ItemStack?): ItemStack? {
        if (slot !in 0..<component.size) {
            return null
        }

        ensureRef()
        val prev = internal_getSlot(slot)
        val drawerSlot = component.slots[slot.toInt()]
        if (ItemStack.isEmpty(itemStack)) {
            if (ref!!.store.getComponent(ref!!, DrawerLockComponent.getComponentType()) == null) {
                drawerSlot.storedItem = ItemStack.EMPTY
                drawerSlot.capacity = 0
            }
            drawerSlot.storedQuantity = 0
        } else {
            drawerSlot.storedItem = itemStack?.withQuantity(1)!!
            drawerSlot.storedQuantity = itemStack.quantity
            component.setCapacityForSlot(slot.toInt(), upgradesComponent?.multiplier)
        }
        
        world!!.execute {
            DrawerSystem.updateDisplay(ref!!, slot.toInt(), pos)
        }
        
        return prev
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

    override fun getRef(slot: Short): Ref<ChunkStore?> {
        ensureRef()
        return ref!!
    }

    override fun getCapacity(): Short {
        return component.size.toShort()
    }
    
    override fun getSlotStackCapacity(slot: Short): Int {
        return component.getSlotStackCapacity(upgradesComponent?.multiplier)
    }

    override fun internal_clear(): ClearTransaction {
        val itemStacks = mutableListOf<ItemStack>()
        for (i in 0..<component.size) {
            val slot = component.slots[i]
            itemStacks.add(
                if (ItemStack.isEmpty(slot.storedItem)) ItemStack.EMPTY 
                else slot.storedItem.withQuantity(slot.storedQuantity)!!
            )
            slot.storedItem = ItemStack.EMPTY
            slot.storedQuantity = 0
            slot.capacity = 0
        }
        
        return ClearTransaction(true, 0, itemStacks.toTypedArray())
    }
    
    override fun testCantAddToSlot(slot: Short, itemStack: ItemStack, slotItemStack: ItemStack?): Boolean {
        return cantAddToSlot(slot, itemStack, slotItemStack)
    }

    override fun addItemStack(
        itemStack: ItemStack,
        allOrNothing: Boolean,
        fullStacks: Boolean,
        filter: Boolean,
    ): ItemStackTransaction {
        val transaction = InternalDrawerContainerUtilItemStack.addItemStack(this, itemStack, allOrNothing, fullStacks, filter)
        this.sendUpdate(transaction)
        return transaction
    }

    override fun dropAllItemStacks(): List<ItemStack?> {
        return listOf()
    }

    override fun clone(): SimpleItemContainer {
        return DrawerContainerWrapper(this)
    }

    override fun isEmpty(): Boolean {
        this.lock.readLock().lock()
        
        var res = false
        try {
            if (component.slots.any { !it.isEmpty() }) {
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

        o as DrawerContainerWrapper

        if (pos != o.pos) return false
        if (worldName != o.worldName) return false
        if (component != o.component) return false
        if (upgradesComponent != o.upgradesComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + worldName.hashCode()
        result = 31 * result + component.hashCode()
        result = 31 * result + (upgradesComponent?.hashCode() ?: 0)
        return result
    }
}