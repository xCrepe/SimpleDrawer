package net.crepe.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.math.matrix.Matrix4d
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.DebugShape
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes
import com.hypixel.hytale.protocol.packets.player.DisplayDebug
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.drawer.DrawerLinkedComponent

class DrawerLinkSystem {
    companion object {
        val log = { log: String -> com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().atInfo().log(log) }
        
        fun addWireframe(player: PlayerRef, pos: Vector3d, scale: Double, color: Vector3f) {
            val scale2 = scale + 0.01
            val matrix = Matrix4d().identity().translate(pos).scale(scale2, scale2, scale2)
            val packet = DisplayDebug(
                DebugShape.Cube,
                matrix.asFloatData(),
                com.hypixel.hytale.protocol.Vector3f(color.x, color.y, color.z),
                Float.MAX_VALUE,
                false,
                null
            )

            player.packetHandler.write(packet)
        }
        
        fun renderWireframes(player: PlayerRef, pos: Vector3i, world: World) {
            val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
            val linksComponent = blockRef?.store?.getComponent(blockRef, ControllerLinksComponent.getComponentType()) ?: return

            val hologramPos = pos.toVector3d().add(0.5, 0.5, 0.5)
            addWireframe(
                player,
                hologramPos,
                1.0,
                Vector3f(1f, 1f, 1f)
            )
            addWireframe(
                player,
                hologramPos,
                1.0 + 2.0 * linksComponent.radius,
                Vector3f(1f, 1f, 1f)
            )

            for (drawerPos in linksComponent.drawers) {
                addWireframe(
                    player,
                    drawerPos.toVector3d().add(0.5, 0.5, 0.5),
                    1.0,
                    Vector3f(0f, 1f, 0f)
                )
            }
        }
        
        fun removeWireframes(player: PlayerRef) {
            val packet = ClearDebugShapes()
            player.packetHandler.write(packet)
        }
    }
    
    class ControllerBreakUnlink : EntityEventSystem<EntityStore?, BreakBlockEvent>(BreakBlockEvent::class.java) {
        override fun handle(
            idx: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: BreakBlockEvent
        ) {
            val player = chunk.getComponent(idx, PlayerRef.getComponentType())!!
            val pos = event.targetBlock
            val world = store.externalData.world
            val blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
            val linksComponent = blockRef?.store?.getComponent(blockRef, ControllerLinksComponent.getComponentType()) ?: return
            
            for (drawerPos in linksComponent.drawers) {
                val drawerRef = BlockModule.getBlockEntity(world, drawerPos.x, drawerPos.y, drawerPos.z) ?: continue
                world.chunkStore.store.removeComponent(drawerRef, DrawerLinkedComponent.getComponentType())
            }
            
            event.itemInHand?.let {
                val selectedBlock = it.metadata?.get("SelectedBlock")?.asDocument() ?: return@let
                val storedPos = Vector3i.CODEC.decode(selectedBlock)
                if (storedPos == pos) removeWireframes(player)
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }

    }
    
    class DrawerBreakUnlink : EntityEventSystem<EntityStore?, BreakBlockEvent>(BreakBlockEvent::class.java) {
        override fun handle(
            idx: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: BreakBlockEvent
        ) {
            val player = chunk.getComponent(idx, PlayerRef.getComponentType())!!
            val pos = event.targetBlock
            val world = store.externalData.world
            val drawerRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z)
            val linkedComponent = drawerRef?.store?.getComponent(drawerRef, DrawerLinkedComponent.getComponentType()) ?: return
            val controllerPos = linkedComponent.controller
            val controllerRef = BlockModule.getBlockEntity(world, controllerPos.x, controllerPos.y, controllerPos.z)
            val linksComponent = controllerRef?.store?.getComponent(controllerRef, ControllerLinksComponent.getComponentType()) ?: return

            val index = linksComponent.drawers.indexOf(pos)
            if (index != -1) {
                linksComponent.drawers.removeAt(index)
                linksComponent.containers.removeAt(index)

                val item = event.itemInHand?.let {
                    val selectedBlock = it.metadata?.get("SelectedBlock")?.asDocument() ?: return@let
                    val storedPos = Vector3i.CODEC.decode(selectedBlock)
                    if (storedPos == controllerPos) {
                        removeWireframes(player)
                        renderWireframes(player, controllerPos, world)
                    }
                }
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
    
    class LinkingToolDrop : EntityEventSystem<EntityStore?, DropItemEvent.Drop>(DropItemEvent.Drop::class.java) {
        override fun handle(
            idx: Int,
            chunk: ArchetypeChunk<EntityStore?>,
            store: Store<EntityStore?>,
            cmdBuffer: CommandBuffer<EntityStore?>,
            event: DropItemEvent.Drop
        ) {
            val player = chunk.getComponent(idx, PlayerRef.getComponentType())!!
            
            val selectedBlock = event.itemStack.metadata?.get("SelectedBlock")?.asDocument() ?: return
            removeWireframes(player)
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Query.and(Player.getComponentType())
        }
    }
}