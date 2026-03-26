package li.cil.oc

import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModMenus
import li.cil.oc.common.init.ModCreativeTabs
import li.cil.oc.common.init.ModDataComponents
import li.cil.oc.common.init.ModSoundEvents
import li.cil.oc.common.network.NetworkHandler
import li.cil.oc.server.machine.MachineRegistry
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * OpenComputers Rewrite - A modern reimagining of the classic OpenComputers mod.
 * 
 * This mod adds programmable computers and robots to Minecraft that can be programmed
 * in Lua. Features include:
 * 
 * - Modular computers with tiered components (CPU, Memory, GPU, etc.)
 * - Multi-block screens for visual output
 * - Robots that can interact with the world
 * - Drones for flying automation
 * - Network cables for component communication
 * - Integration with other mods via the Driver API
 * 
 * @author Original: Sangar, Vexatos, payonel, magik6k
 * @author Rewrite: Community
 * @version 3.0.0
 */
@Mod(OpenComputers.MOD_ID)
class OpenComputers(
    modBus: IEventBus,
    container: ModContainer
) {
    companion object {
        const val MOD_ID = "opencomputers"
        const val MOD_NAME = "OpenComputers"
        
        @JvmStatic
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)
        
        @JvmStatic
        lateinit var instance: OpenComputers
            private set
    }
    
    init {
        instance = this
        LOGGER.info("Initializing $MOD_NAME")
        
        // Register deferred registers
        ModBlocks.register(modBus)
        ModItems.register(modBus)
        ModBlockEntities.register(modBus)
        ModMenus.register(modBus)
        ModCreativeTabs.register(modBus)
        ModDataComponents.register(modBus)
        ModSoundEvents.register(modBus)
        
        // Register event handlers
        modBus.addListener(this::commonSetup)
        modBus.addListener(this::clientSetup)
        
        // Register game event handlers
        NeoForge.EVENT_BUS.register(this)
        
        LOGGER.info("$MOD_NAME initialization complete")
    }
    
    private fun commonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("Common setup starting")
        
        event.enqueueWork {
            // Initialize network handler
            NetworkHandler.register()
            
            // Register default architectures
            MachineRegistry.registerDefaults()
            
            LOGGER.info("Common setup complete")
        }
    }
    
    private fun clientSetup(event: FMLClientSetupEvent) {
        LOGGER.info("Client setup starting")
        
        event.enqueueWork {
            // Register client-side renderers
            // This will be handled by the client module
            
            LOGGER.info("Client setup complete")
        }
    }
}
