package net.crepe

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.DrawerDisplayComponent
import net.crepe.component.DrawerBoundDisplayComponent
import net.crepe.component.DrawerLockComponent
import net.crepe.component.DrawerSlotHitComponent
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.component.DrawerSlotsContainerComponent
import net.crepe.components.DrawerContainerComponent
import net.crepe.interaction.DebugInteraction
import net.crepe.interaction.DrawerIOInteraction
import net.crepe.interaction.DrawerInteraction
import net.crepe.interaction.DrawerLockInteraction
import net.crepe.interaction.DrawerQuickStackInteraction
import net.crepe.interaction.DrawerUpgradeInteraction
import net.crepe.system.DrawerSystem
import net.crepe.utils.DisplayUtils

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
        
        Interaction.CODEC.register("SimpleDrawer_DrawerInteract", DrawerInteraction::class.java, DrawerInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerIO", DrawerIOInteraction::class.java, DrawerIOInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerQuickStack", DrawerQuickStackInteraction::class.java,
            DrawerQuickStackInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerUpgrade", DrawerUpgradeInteraction::class.java,
            DrawerUpgradeInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerLock", DrawerLockInteraction::class.java,
            DrawerLockInteraction.CODEC)
//        Interaction.CODEC.register("SimpleDrawer_Debug", DebugInteraction::class.java, DebugInteraction.CODEC)

        entityStoreRegistry.registerSystem(DrawerSystem.DrawerBreakEvent())
        entityStoreRegistry.registerSystem(DrawerSystem.DrawerPlaceEvent())
        
        this.eventRegistry.register(LoadedAssetsEvent::class.java, ModelAsset::class.java) { event ->
            if (event.loadedAssets.any { it.key.toString().startsWith("SimpleDrawer") }) {
                DisplayUtils.initIcons()
                DisplayUtils.initNumberDisplayHolders()
                DisplayUtils.initDisplayEntityTemplate()
            }
        }
    }
}