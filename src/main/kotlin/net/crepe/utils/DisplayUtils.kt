package net.crepe.utils

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent
import com.hypixel.hytale.server.core.modules.entity.component.Intangible
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.prefab.PrefabCopyableComponent
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.drawer.DrawerDisplayComponent
import java.util.UUID
import kotlin.math.floor

class DisplayUtils {
    companion object {
        val charModelAssets = mapOf(
            "0" to "SimpleDrawer_Char0",
            "1" to "SimpleDrawer_Char1",
            "2" to "SimpleDrawer_Char2",
            "3" to "SimpleDrawer_Char3",
            "4" to "SimpleDrawer_Char4",
            "5" to "SimpleDrawer_Char5",
            "6" to "SimpleDrawer_Char6",
            "7" to "SimpleDrawer_Char7",
            "8" to "SimpleDrawer_Char8",
            "9" to "SimpleDrawer_Char9",
            "." to "SimpleDrawer_CharDot",
            "k" to "SimpleDrawer_Chark",
            "M" to "SimpleDrawer_CharM",
            "B" to "SimpleDrawer_CharB",
            "T" to "SimpleDrawer_CharT",
        )
        
        val log = { log: String -> HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        val displayEntityHolder = EntityStore.REGISTRY.newHolder()
        val numberDisplayHolders = HashMap<String, Holder<EntityStore?>>()
        val iconDisplayHolders = HashMap<String, Holder<EntityStore?>>()
        private const val NUMBER_SCALE = 1f
        private const val ICON_SCALE = 0.5f
        
        fun calcDisplayPosition(blockPos: Vector3i, blockRot: Vector3f, offset: Vector3d): Vector3d {
            val pos = Vector3d.add(blockPos.toVector3d(), Vector3d(0.5, 0.5, 0.5))
            val offsetVector = Vector3d(0.0, 0.0, -0.438).add(offset).rotateY((blockRot.yaw + Math.PI).toFloat())
            return pos.add(offsetVector)
        }

        private fun initId(holder: Holder<EntityStore?>, store: Store<EntityStore?>): UUID {
            holder.addComponent(NetworkId.getComponentType(), NetworkId(store.externalData.takeNextNetworkId()))
            val uuid = UUID.randomUUID()
            holder.putComponent(UUIDComponent.getComponentType(), UUIDComponent(uuid))
            return uuid
        }
        
        fun initIcons() {
            val holder = EntityStore.REGISTRY.newHolder()
            holder.ensureComponent(Intangible.getComponentType())
            holder.ensureComponent(PropComponent.getComponentType())
            holder.ensureComponent(PrefabCopyableComponent.getComponentType())
            
            val addIconHolder = { icon: String ->
                val iconHolder = holder.clone()
                val modelAsset = ModelAsset.getAssetMap().getAsset("SimpleDrawer_Icon_$icon")!!
                val model = Model.createStaticScaledModel(modelAsset, ICON_SCALE)
                iconHolder.addComponent(ModelComponent.getComponentType(),
                    ModelComponent(model))
                iconHolder.addComponent(PersistentModel.getComponentType(),
                    PersistentModel(Model.ModelReference("SimpleDrawer_Icon_$icon", ICON_SCALE, null, true)))
                iconDisplayHolders.putIfAbsent(icon, iconHolder)
            }
            
            addIconHolder("Lock")
        }
        
        fun spawnIcon(store: Store<EntityStore?>, pos: Vector3d, rot: Vector3f, scale: Float, icon: String): UUID {
            val rot2 = rot.clone()
            rot2.yaw += Math.PI.toFloat()
            val holder = iconDisplayHolders[icon]!!.clone()
            val uuid = initId(holder, store)
            holder.addComponent(TransformComponent.getComponentType(),
                TransformComponent(pos, rot2))
            holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(scale))
            store.addEntity(holder, AddReason.SPAWN)
            return uuid
        }
        
        fun initNumberDisplayHolders() {
            val holder = EntityStore.REGISTRY.newHolder()
            holder.ensureComponent(Intangible.getComponentType())
            holder.ensureComponent(PropComponent.getComponentType())
            holder.ensureComponent(PrefabCopyableComponent.getComponentType())
            
            val addNumberHolder = { char: String ->
                val numberHolder = holder.clone()
                val modelAsset = ModelAsset.getAssetMap().getAsset(charModelAssets[char])!!
                val model = Model.createStaticScaledModel(modelAsset, NUMBER_SCALE)
                numberHolder.addComponent(ModelComponent.getComponentType(),
                    ModelComponent(model))
                numberHolder.addComponent(PersistentModel.getComponentType(),
                    PersistentModel(Model.ModelReference(charModelAssets[char], NUMBER_SCALE, null, true)))
                numberDisplayHolders.putIfAbsent(char, numberHolder)
            }
            
            for (i in 0..9) {
                addNumberHolder("$i")
            }
            addNumberHolder(".")
            addNumberHolder("k")
            addNumberHolder("M")
            addNumberHolder("B")
            addNumberHolder("T")
        }
        
        fun renderNumber(
            old: Long?,
            new: Long,
            slotDisplays: DrawerDisplayComponent.SlotDisplays,
            pos: Vector3d,
            rot: Vector3f,
            scale: Float,
            world: World,
            store: Store<EntityStore?>,
        ) {
            val fOld = if (old != null) formatNumber(old) else "-"
            val fNew = formatNumber(new)
            val rot2 = rot.clone()
            rot2.yaw += Math.PI.toFloat()

            if (fOld == fNew) return

            when {
                fOld.length < fNew.length -> {
                    for (i in 0 ..< fNew.length - fOld.length) {
                        val offset = calcOffset(pos, rot2, scale, i, fNew.length)
                        slotDisplays.numberDisplays.add(i, spawnNumber(store, offset, rot2, scale, fNew[i].toString()))
                    }
                    val diff = fNew.length - fOld.length
                    for (i in diff ..< fNew.length) {
                        val offset = calcOffset(pos, rot2, scale, i, fNew.length)
                        if (fOld[i - diff] != fNew[i] || slotDisplays.numberDisplays[i] == null) {
                            updateNumber(i, world, store, slotDisplays, offset, rot2, scale, fNew[i].toString())
                        } else {
                            updateOffset(i, world, slotDisplays, offset)
                        }
                    }
                }
                fOld.length > fNew.length -> {
                    for (i in 0 ..< fOld.length - fNew.length) {
                        slotDisplays.numberDisplays[0]?.let {
                            val ref = world.getEntityRef(it) ?: return@let
                            store.removeEntity(ref, RemoveReason.REMOVE)
                        }
                        slotDisplays.numberDisplays.removeAt(0)
                    }
                    val diff = fOld.length - fNew.length
                    for (i in fNew.indices) {
                        val offset = calcOffset(pos, rot2, scale, i, fNew.length)
                        if (fOld[i + diff] != fNew[i] || slotDisplays.numberDisplays[i] == null) {
                            updateNumber(i, world, store, slotDisplays, offset, rot2, scale, fNew[i].toString())
                        } else {
                            updateOffset(i, world, slotDisplays, offset)
                        }
                    }
                }
                else -> {
                    for (i in fNew.indices) {
                        val offset = calcOffset(pos, rot2, scale, i, fNew.length)
                        if (fOld[i] != fNew[i]) {
                            updateNumber(i, world, store, slotDisplays, offset, rot2, scale, fNew[i].toString())
                        }
                    }
                }
            }
        }
        
        private fun calcOffset(pos: Vector3d, rot: Vector3f, scale: Float, index: Int, size: Int): Vector3d {
            val offset = 0.075 * NUMBER_SCALE * (index - (size - 1) / 2.0) * scale
            return Vector3d.add(pos, Vector3d(-offset, 0.0, 0.0).rotateY(rot.yaw))
        }

        private fun updateNumber(index: Int, world: World, store: Store<EntityStore?>, slotDisplays: DrawerDisplayComponent.SlotDisplays, pos: Vector3d, rot: Vector3f, scale: Float, char: String) {
            slotDisplays.numberDisplays[index]?.let {
                val ref = world.getEntityRef(it) ?: return@let
                store.removeEntity(ref, RemoveReason.REMOVE)
            }
            slotDisplays.numberDisplays[index] = spawnNumber(store, pos, rot, scale, char)
        }

        private fun updateOffset(index: Int, world: World, slotDisplays: DrawerDisplayComponent.SlotDisplays, offset: Vector3d) {
            slotDisplays.numberDisplays[index]?.let {
                val ref = world.getEntityRef(it) ?: return@let
                val transform = ref.store.getComponent(ref, TransformComponent.getComponentType())!!
                transform.position = offset
            }
        }
        
        private fun spawnNumber(store: Store<EntityStore?>, pos: Vector3d, rot: Vector3f, scale: Float, char: String): UUID {
            val holder = numberDisplayHolders[char]!!.clone()
            val uuid = initId(holder, store)
            holder.addComponent(TransformComponent.getComponentType(),
                TransformComponent(pos, rot))
            holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(scale))
            store.addEntity(holder, AddReason.SPAWN)
            return uuid
        }
        
        private fun formatNumber(v: Long): String {
            return when {
                v >= 1_000_000_000_000 -> String.format("%.1fT", floor(v / 100_000_000_000.0) / 10.0)
                v >= 1_000_000_000 -> String.format("%.1fB", floor(v / 100_000_000.0) / 10.0)
                v >= 1_000_000 -> String.format("%.1fM", floor(v / 100_000.0) / 10.0)
                v >= 1_000 -> String.format("%.1fk", floor(v / 100.0) / 10.0)
                else -> v.toString()
            }
        }

        fun initDisplayEntityTemplate() {
            displayEntityHolder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE)
            displayEntityHolder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE)
            displayEntityHolder.ensureComponent(Intangible.getComponentType())
            displayEntityHolder.ensureComponent(PropComponent.getComponentType())
            displayEntityHolder.ensureComponent(PrefabCopyableComponent.getComponentType())
        }
        
        val itemOffsetPos = mapOf(
            "Ladder" to Vector3d(0.0, 0.0, -0.15),
            "Fence" to Vector3d(0.0, 0.0, -0.05),
            "Vertical" to Vector3d(0.0, 0.0, -0.15),
            "Shelf" to Vector3d(0.0, -0.2, -0.15),
            "Teleporter" to Vector3d(0.0, 0.0, -0.05),
            "Armor" to Vector3d(0.0, 0.1, -0.05),
            "Tree_Sap" to Vector3d(0.0, 0.1, 0.0),
            "Banner" to Vector3d(0.0, 0.25, 0.0),
            "Utility_Helipack" to Vector3d(0.0, 0.1, 0.0),
            "Utility_Leather_Medium_Backpack" to Vector3d(0.0, 0.15, 0.1),
            "Glider" to Vector3d(0.0, 0.2, 0.0),
        )
        val itemOffsetRot = mapOf(
            "Stairs" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Roof" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Vertical" to Vector3f(0f, 0f, 0f),
            "Tool" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Weapon" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Shield" to Vector3f(0f, (-90/180.0 * Math.PI).toFloat(), 0f),
            "Ingredient" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Utility_Helipack" to Vector3f(0f, (Math.PI).toFloat(), 0f),
            "Utility_Leather_Medium_Backpack" to Vector3f(0f, (Math.PI).toFloat(), 0f),
            "Utility_Leather_Quiver" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
            "Glider" to Vector3f(0f, (90/180.0 * Math.PI).toFloat(), 0f),
        )

        fun spawnDisplayEntity(store: Store<EntityStore?>, pos: Vector3d, rot: Vector3f, scale: Float, item: ItemStack): UUID {
            val holder = displayEntityHolder.clone()
            val uuid = initId(holder, store)

            var offsetPos = Vector3d(0.0, 0.0, 0.0)
            for ((k, v) in itemOffsetPos) {
                if (item.itemId.contains(k)) {
                    offsetPos = v.clone()
                    break
                }
            }
            var offsetRot = Vector3f(0f, 0f, 0f)
            for ((k, v) in itemOffsetRot) {
                if (item.itemId.contains(k)) {
                    offsetRot = v.clone()
                }
            }
            val rot2 = rot.clone()
            rot2.yaw += Math.PI.toFloat()
            holder.addComponent(
                TransformComponent.getComponentType(),
                TransformComponent(
                    Vector3d.add(pos, offsetPos.scale(scale.toDouble()).rotateY(rot2.yaw)),
                    Vector3f.add(rot2, offsetRot)
                )
            )

            val displayItem = ItemStack(item.itemId, 1)
            displayItem.overrideDroppedItemAnimation = true
            holder.addComponent(ItemComponent.getComponentType(), ItemComponent(displayItem))

            val scale = (displayItem.item.iconProperties?.scale ?: 0.5f) * scale
            holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(scale))

            store.addEntity(holder, AddReason.SPAWN)
            return uuid
        }

        fun removeDisplayEntity(store: Store<EntityStore?>, uuid: UUID): Boolean {
            val displayRef = store.externalData.world.getEntityRef(uuid) ?: return false
            store.removeEntity(displayRef, RemoveReason.REMOVE)
            return true
        }
    }
}