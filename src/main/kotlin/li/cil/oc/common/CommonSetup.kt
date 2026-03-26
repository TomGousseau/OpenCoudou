package li.cil.oc.common

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.*
import li.cil.oc.common.config.Config
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.ModLoadingContext
import org.apache.logging.log4j.LogManager

/**
 * Common mod setup for OpenComputers.
 * Handles registration and initialization shared between client and server.
 */
@Mod(OpenComputers.MOD_ID)
class CommonSetup(modBus: IEventBus) {
    
    companion object {
        private val LOGGER = LogManager.getLogger("OpenComputers")
        
        fun id(path: String): ResourceLocation = 
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
    }
    
    init {
        LOGGER.info("OpenComputers initializing...")
        
        // Register config
        Config.register(ModLoadingContext.get())
        
        // Register all deferred registers to mod bus
        ModBlocks.BLOCKS.register(modBus)
        ModItems.ITEMS.register(modBus)
        ModBlockEntities.BLOCK_ENTITIES.register(modBus)
        ModMenus.MENUS.register(modBus)
        ModCreativeTabs.CREATIVE_TABS.register(modBus)
        ModDataComponents.DATA_COMPONENTS.register(modBus)
        ModSounds.SOUNDS.register(modBus)
        ModEntities.ENTITY_TYPES.register(modBus)
        
        // Register setup event
        modBus.addListener(::onCommonSetup)
        
        LOGGER.info("OpenComputers registration complete")
    }
    
    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            LOGGER.info("OpenComputers common setup...")
            
            // Initialize network handling
            // NetworkHandler.register()
            
            // Initialize component drivers
            // DriverRegistry.init()
            
            LOGGER.info("OpenComputers common setup complete")
        }
    }
}
