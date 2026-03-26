package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.*
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for all OpenComputers container/menu types.
 */
object ModMenus {
    private val MENUS: DeferredRegister<MenuType<*>> = 
        DeferredRegister.create(Registries.MENU, OpenComputers.MOD_ID)
    
    // ========================================
    // Computer Containers
    // ========================================
    
    val CASE: DeferredHolder<MenuType<*>, MenuType<CaseContainer>> =
        MENUS.register("case") {
            IMenuTypeExtension.create(::CaseContainer)
        }
    
    val RACK: DeferredHolder<MenuType<*>, MenuType<RackContainer>> =
        MENUS.register("rack") {
            IMenuTypeExtension.create(::RackContainer)
        }
    
    // ========================================
    // Storage Containers
    // ========================================
    
    val DISK_DRIVE: DeferredHolder<MenuType<*>, MenuType<DiskDriveContainer>> =
        MENUS.register("diskdrive") {
            IMenuTypeExtension.create(::DiskDriveContainer)
        }
    
    val RAID: DeferredHolder<MenuType<*>, MenuType<RaidContainer>> =
        MENUS.register("raid") {
            IMenuTypeExtension.create(::RaidContainer)
        }
    
    // ========================================
    // Crafting Containers
    // ========================================
    
    val ASSEMBLER: DeferredHolder<MenuType<*>, MenuType<AssemblerContainer>> =
        MENUS.register("assembler") {
            IMenuTypeExtension.create(::AssemblerContainer)
        }
    
    val DISASSEMBLER: DeferredHolder<MenuType<*>, MenuType<DisassemblerContainer>> =
        MENUS.register("disassembler") {
            IMenuTypeExtension.create(::DisassemblerContainer)
        }
    
    val PRINTER: DeferredHolder<MenuType<*>, MenuType<PrinterContainer>> =
        MENUS.register("printer") {
            IMenuTypeExtension.create(::PrinterContainer)
        }
    
    // ========================================
    // Utility Containers
    // ========================================
    
    val CHARGER: DeferredHolder<MenuType<*>, MenuType<ChargerContainer>> =
        MENUS.register("charger") {
            IMenuTypeExtension.create(::ChargerContainer)
        }
    
    val ADAPTER: DeferredHolder<MenuType<*>, MenuType<AdapterContainer>> =
        MENUS.register("adapter") {
            IMenuTypeExtension.create(::AdapterContainer)
        }
    
    val RELAY: DeferredHolder<MenuType<*>, MenuType<RelayContainer>> =
        MENUS.register("relay") {
            IMenuTypeExtension.create(::RelayContainer)
        }
    
    // ========================================
    // Portable Device Containers
    // ========================================
    
    val TABLET: DeferredHolder<MenuType<*>, MenuType<TabletContainer>> =
        MENUS.register("tablet") {
            IMenuTypeExtension.create(::TabletContainer)
        }
    
    val DRONE: DeferredHolder<MenuType<*>, MenuType<DroneContainer>> =
        MENUS.register("drone") {
            IMenuTypeExtension.create(::DroneContainer)
        }
    
    // ========================================
    // Item Containers
    // ========================================
    
    val DATABASE: DeferredHolder<MenuType<*>, MenuType<DatabaseContainer>> =
        MENUS.register("database") {
            IMenuTypeExtension.create(::DatabaseContainer)
        }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        MENUS.register(bus)
        OpenComputers.LOGGER.debug("Registered menus")
    }
}
