package net.crepe

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketFilter
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.controller.ControllerLinksComponent
import net.crepe.component.drawer.DrawerDisplayComponent
import net.crepe.component.drawer.DrawerBoundDisplayComponent
import net.crepe.component.drawer.DrawerLinkedComponent
import net.crepe.component.drawer.DrawerLockComponent
import net.crepe.component.drawer.DrawerSlotHitComponent
import net.crepe.component.drawer.DrawerUpgradableComponent
import net.crepe.component.drawer.DrawerSlotsContainerComponent
import net.crepe.components.DrawerContainerComponent
import net.crepe.interaction.controller.ControllerInsertInteraction
import net.crepe.interaction.DebugInteraction
import net.crepe.interaction.linkingTool.ControllerSelectInteraction
import net.crepe.interaction.drawer.DrawerIOInteraction
import net.crepe.interaction.drawer.DrawerInteraction
import net.crepe.interaction.DrawerLockInteraction
import net.crepe.interaction.controller.ControllerQuickStack
import net.crepe.interaction.drawer.DrawerQuickStackInteraction
import net.crepe.interaction.drawer.DrawerUpgradeInteraction
import net.crepe.interaction.linkingTool.DrawerLinkInteraction
import net.crepe.inventory.ControllerState
import net.crepe.inventory.DrawerContainerState
import net.crepe.packet.HeldItemChangeHandler
import net.crepe.provider.DrawerTooltipProvider
import net.crepe.system.ControllerSystem
import net.crepe.system.DrawerLinkSystem
import net.crepe.system.DrawerSystem
import net.crepe.utils.DisplayUtils
import org.herolias.tooltips.api.DynamicTooltipsApiProvider

class SimpleDrawerPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    companion object {
        lateinit var instance: SimpleDrawerPlugin
            private set
    }
    
    lateinit var drawerContainerComponent: ComponentType<ChunkStore?, DrawerContainerComponent>
        private set
    lateinit var drawerSlotsContainerComponent: ComponentType<ChunkStore?, DrawerSlotsContainerComponent>
        private set
    lateinit var drawerBoundDisplayComponent: ComponentType<ChunkStore?, DrawerBoundDisplayComponent>
        private set
    lateinit var drawerDisplayComponent: ComponentType<ChunkStore?, DrawerDisplayComponent>
        private set
    lateinit var drawerUpgradableComponent: ComponentType<ChunkStore?, DrawerUpgradableComponent>
        private set
    lateinit var drawerSlotHitComponent: ComponentType<ChunkStore?, DrawerSlotHitComponent>
        private set
    lateinit var drawerLockComponent: ComponentType<ChunkStore?, DrawerLockComponent>
        private set
    lateinit var drawerLinkedComponent: ComponentType<ChunkStore?, DrawerLinkedComponent>
        private set
    
    lateinit var controllerLinksComponent: ComponentType<ChunkStore?, ControllerLinksComponent>
        private set
    
    private lateinit var linkerSelectHandler: PacketFilter
    
    protected override fun setup() {
        instance = this
        
        val chunkStoreRegistry = this.chunkStoreRegistry
        val entityStoreRegistry = this.entityStoreRegistry
        
        // Deprecated
        drawerContainerComponent = chunkStoreRegistry.registerComponent(DrawerContainerComponent::class.java, "SimpleDrawer_DrawerContainer", DrawerContainerComponent.CODEC)
        drawerBoundDisplayComponent = chunkStoreRegistry.registerComponent(DrawerBoundDisplayComponent::class.java, "SimpleDrawer_DrawerBoundDisplay",
            DrawerBoundDisplayComponent.CODEC)
        chunkStoreRegistry.registerSystem(DrawerSystem.DrawerMigrateSystem())

        
        drawerSlotsContainerComponent = chunkStoreRegistry.registerComponent(DrawerSlotsContainerComponent::class.java, "SimpleDrawer_DrawerSlotsContainer", DrawerSlotsContainerComponent.CODEC)
        drawerDisplayComponent = chunkStoreRegistry.registerComponent(DrawerDisplayComponent::class.java, "SimpleDrawer_DrawerDisplay",
            DrawerDisplayComponent.CODEC)
        drawerUpgradableComponent = chunkStoreRegistry.registerComponent(DrawerUpgradableComponent::class.java, "SimpleDrawer_DrawerUpgradable",
            DrawerUpgradableComponent.CODEC)
        drawerSlotHitComponent = chunkStoreRegistry.registerComponent(DrawerSlotHitComponent::class.java, "SimpleDrawer_DrawerSlotHit",
            DrawerSlotHitComponent.CODEC)
        drawerLockComponent = chunkStoreRegistry.registerComponent(DrawerLockComponent::class.java, "SimpleDrawer_DrawerLock", DrawerLockComponent.CODEC)
        drawerLinkedComponent = chunkStoreRegistry.registerComponent(DrawerLinkedComponent::class.java, "SimpleDrawer_DrawerLinked",
            DrawerLinkedComponent.CODEC)
        
        controllerLinksComponent = chunkStoreRegistry.registerComponent(ControllerLinksComponent::class.java, "SimpleDrawer_ControllerLinks",
            ControllerLinksComponent.CODEC)
        
        
        Interaction.CODEC.register("SimpleDrawer_DrawerInteract", DrawerInteraction::class.java, DrawerInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerIO", DrawerIOInteraction::class.java, DrawerIOInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerQuickStack", DrawerQuickStackInteraction::class.java,
            DrawerQuickStackInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerUpgrade", DrawerUpgradeInteraction::class.java,
            DrawerUpgradeInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerLock", DrawerLockInteraction::class.java,
            DrawerLockInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_ControllerSelect", ControllerSelectInteraction::class.java,
            ControllerSelectInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerLink", DrawerLinkInteraction::class.java,
            DrawerLinkInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_ControllerInsert", ControllerInsertInteraction::class.java,
            ControllerInsertInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_ControllerQuickStack", ControllerQuickStack::class.java,
            ControllerQuickStack.CODEC)
        Interaction.CODEC.register("SimpleDrawer_Debug", DebugInteraction::class.java, DebugInteraction.CODEC)

        
        entityStoreRegistry.registerSystem(DrawerSystem.DrawerBreakEvent())
        entityStoreRegistry.registerSystem(DrawerSystem.DrawerPlaceEvent())
        entityStoreRegistry.registerSystem(DrawerLinkSystem.ControllerBreakUnlink())
        entityStoreRegistry.registerSystem(DrawerLinkSystem.DrawerBreakUnlink())
        entityStoreRegistry.registerSystem(DrawerLinkSystem.LinkingToolDrop())
        
        chunkStoreRegistry.registerSystem(DrawerSystem.DrawerWrapContainer())
        chunkStoreRegistry.registerSystem(ControllerSystem.ControllerPlace())

        
        this.blockStateRegistry.registerBlockState(DrawerContainerState::class.java, "SimpleDrawer_Container", DrawerContainerState.CODEC)
        this.blockStateRegistry.registerBlockState(ControllerState::class.java, "SimpleDrawer_Controller",
            ControllerState.CODEC)
        
        
        linkerSelectHandler = PacketAdapters.registerInbound(HeldItemChangeHandler())

        
        DisplayUtils.initDisplayEntityTemplate()
        this.eventRegistry.register(LoadedAssetsEvent::class.java, ModelAsset::class.java) { event ->
            if (event.loadedAssets.any { it.key.toString().startsWith("SimpleDrawer") }) {
                DisplayUtils.initIcons()
                DisplayUtils.initNumberDisplayHolders()
            }
        }
        
        
        this.eventRegistry.registerGlobal(PlayerReadyEvent::class.java) { event ->
            event.player.sendMessage(Message.raw("[SimpleDrawer] v1.4.0 - Integration with workbenches and storage mods (things might break as I haven't done much testing)"))
        }

        
        try {
            Class.forName("org.herolias.tooltips.api.DynamicTooltipsApiProvider")
            val api = DynamicTooltipsApiProvider.get()
            api?.registerProvider(DrawerTooltipProvider())
        } catch (e: ClassNotFoundException) {
            this.logger.atWarning().log("DynamicTooltipsLib not found.")
        }
    }
    
    protected override fun shutdown() {
        if (linkerSelectHandler != null) {
            PacketAdapters.deregisterInbound(linkerSelectHandler)
        }
    }
}