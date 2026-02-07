package net.crepe

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import net.crepe.component.DrawerBoundDisplayComponent
import net.crepe.component.DrawerUpgradableComponent
import net.crepe.components.DrawerContainerComponent
import net.crepe.interaction.DrawerIOInteraction
import net.crepe.interaction.DrawerInteraction
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
    lateinit var drawerBoundDisplay: ComponentType<ChunkStore?, DrawerBoundDisplayComponent>
        private set
    lateinit var drawerUpgradableComponent: ComponentType<ChunkStore?, DrawerUpgradableComponent>
        private set
    
    protected override fun setup() {
        instance = this
        drawerContainerComponent = this.chunkStoreRegistry.registerComponent(DrawerContainerComponent::class.java, "SimpleDrawer_DrawerContainer", DrawerContainerComponent.CODEC)
        drawerBoundDisplay = this.chunkStoreRegistry.registerComponent(DrawerBoundDisplayComponent::class.java, "SimpleDrawer_DrawerBoundDisplay",
            DrawerBoundDisplayComponent.CODEC)
        drawerUpgradableComponent = this.chunkStoreRegistry.registerComponent(DrawerUpgradableComponent::class.java, "SimpleDrawer_DrawerUpgradable",
            DrawerUpgradableComponent.CODEC)
        
        Interaction.CODEC.register("SimpleDrawer_DrawerInteract", DrawerInteraction::class.java, DrawerInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerIO", DrawerIOInteraction::class.java, DrawerIOInteraction.CODEC)
        Interaction.CODEC.register("SimpleDrawer_DrawerUpgrade", DrawerUpgradeInteraction::class.java,
            DrawerUpgradeInteraction.CODEC)
        this.entityStoreRegistry.registerSystem(DrawerSystem.DrawerBreakEvent())
        this.entityStoreRegistry.registerSystem(DrawerSystem.DrawerPlaceEvent())
        
        this.eventRegistry.register(LoadedAssetsEvent::class.java, ModelAsset::class.java) { event ->
            if (event.loadedAssets.any { it.key.toString().startsWith("SimpleDrawer") }) {
                DisplayUtils.initNumberDisplayHolders()
                DisplayUtils.initDisplayEntityTemplate()
            }
        }
    }
}