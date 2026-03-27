package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.*
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/**
 * Registration for all OpenComputers block entities.
 */
object ModBlockEntities {
    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.MOD_ID)
    
    // ========================================
    // Computer Cases
    // ========================================
    
    val CASE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CaseBlockEntity>> = 
        BLOCK_ENTITIES.register("case") {
            BlockEntityType.Builder.of(
                ::CaseBlockEntity,
                ModBlocks.CASE_TIER1.get(),
                ModBlocks.CASE_TIER2.get(),
                ModBlocks.CASE_TIER3.get(),
                ModBlocks.CASE_CREATIVE.get()
            ).build(null)
        }
    
    // ========================================
    // Screens
    // ========================================
    
    val SCREEN: DeferredHolder<BlockEntityType<*>, BlockEntityType<ScreenBlockEntity>> = 
        BLOCK_ENTITIES.register("screen") {
            BlockEntityType.Builder.of(
                ::ScreenBlockEntity,
                ModBlocks.SCREEN_TIER1.get(),
                ModBlocks.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get()
            ).build(null)
        }
    
    // ========================================
    // Input/Output
    // ========================================
    
    val KEYBOARD: DeferredHolder<BlockEntityType<*>, BlockEntityType<KeyboardBlockEntity>> = 
        BLOCK_ENTITIES.register("keyboard") {
            BlockEntityType.Builder.of(
                ::KeyboardBlockEntity,
                ModBlocks.KEYBOARD.get()
            ).build(null)
        }
    
    val REDSTONE_IO: DeferredHolder<BlockEntityType<*>, BlockEntityType<RedstoneIOBlockEntity>> = 
        BLOCK_ENTITIES.register("redstone") {
            BlockEntityType.Builder.of(
                ::RedstoneIOBlockEntity,
                ModBlocks.REDSTONE_IO.get()
            ).build(null)
        }
    
    // ========================================
    // Networking
    // ========================================
    
    val CABLE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CableBlockEntity>> = 
        BLOCK_ENTITIES.register("cable") {
            BlockEntityType.Builder.of(
                ::CableBlockEntity,
                ModBlocks.CABLE.get()
            ).build(null)
        }
    
    val RELAY: DeferredHolder<BlockEntityType<*>, BlockEntityType<RelayBlockEntity>> = 
        BLOCK_ENTITIES.register("relay") {
            BlockEntityType.Builder.of(
                ::RelayBlockEntity,
                ModBlocks.RELAY.get()
            ).build(null)
        }
    
    val ACCESS_POINT: DeferredHolder<BlockEntityType<*>, BlockEntityType<AccessPointBlockEntity>> = 
        BLOCK_ENTITIES.register("accesspoint") {
            BlockEntityType.Builder.of(
                ::AccessPointBlockEntity,
                ModBlocks.ACCESS_POINT.get()
            ).build(null)
        }
    
    // ========================================
    // Power
    // ========================================
    
    val CAPACITOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<CapacitorBlockEntity>> = 
        BLOCK_ENTITIES.register("capacitor") {
            BlockEntityType.Builder.of(
                ::CapacitorBlockEntity,
                ModBlocks.CAPACITOR.get()
            ).build(null)
        }
    
    val POWER_CONVERTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<PowerConverterBlockEntity>> = 
        BLOCK_ENTITIES.register("powerconverter") {
            BlockEntityType.Builder.of(
                ::PowerConverterBlockEntity,
                ModBlocks.POWER_CONVERTER.get()
            ).build(null)
        }
    
    val POWER_DISTRIBUTOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<PowerDistributorBlockEntity>> = 
        BLOCK_ENTITIES.register("powerdistributor") {
            BlockEntityType.Builder.of(
                ::PowerDistributorBlockEntity,
                ModBlocks.POWER_DISTRIBUTOR.get()
            ).build(null)
        }
    
    val CHARGER: DeferredHolder<BlockEntityType<*>, BlockEntityType<ChargerBlockEntity>> = 
        BLOCK_ENTITIES.register("charger") {
            BlockEntityType.Builder.of(
                ::ChargerBlockEntity,
                ModBlocks.CHARGER.get()
            ).build(null)
        }
    
    // ========================================
    // Adapters & Integration
    // ========================================
    
    val ADAPTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<AdapterBlockEntity>> = 
        BLOCK_ENTITIES.register("adapter") {
            BlockEntityType.Builder.of(
                ::AdapterBlockEntity,
                ModBlocks.ADAPTER.get()
            ).build(null)
        }
    
    val TRANSPOSER: DeferredHolder<BlockEntityType<*>, BlockEntityType<TransposerBlockEntity>> = 
        BLOCK_ENTITIES.register("transposer") {
            BlockEntityType.Builder.of(
                ::TransposerBlockEntity,
                ModBlocks.TRANSPOSER.get()
            ).build(null)
        }
    
    // ========================================
    // Storage
    // ========================================
    
    val DISK_DRIVE: DeferredHolder<BlockEntityType<*>, BlockEntityType<DiskDriveBlockEntity>> = 
        BLOCK_ENTITIES.register("diskdrive") {
            BlockEntityType.Builder.of(
                ::DiskDriveBlockEntity,
                ModBlocks.DISK_DRIVE.get()
            ).build(null)
        }
    
    val RAID: DeferredHolder<BlockEntityType<*>, BlockEntityType<RaidBlockEntity>> = 
        BLOCK_ENTITIES.register("raid") {
            BlockEntityType.Builder.of(
                ::RaidBlockEntity,
                ModBlocks.RAID.get()
            ).build(null)
        }
    
    // ========================================
    // Sensors & Analysis
    // ========================================
    
    val GEOLYZER: DeferredHolder<BlockEntityType<*>, BlockEntityType<GeolyzerBlockEntity>> = 
        BLOCK_ENTITIES.register("geolyzer") {
            BlockEntityType.Builder.of(
                ::GeolyzerBlockEntity,
                ModBlocks.GEOLYZER.get()
            ).build(null)
        }
    
    val MOTION_SENSOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<MotionSensorBlockEntity>> = 
        BLOCK_ENTITIES.register("motionsensor") {
            BlockEntityType.Builder.of(
                ::MotionSensorBlockEntity,
                ModBlocks.MOTION_SENSOR.get()
            ).build(null)
        }
    
    val WAYPOINT: DeferredHolder<BlockEntityType<*>, BlockEntityType<WaypointBlockEntity>> = 
        BLOCK_ENTITIES.register("waypoint") {
            BlockEntityType.Builder.of(
                ::WaypointBlockEntity,
                ModBlocks.WAYPOINT.get()
            ).build(null)
        }
    
    // ========================================
    // Advanced
    // ========================================
    
    val RACK: DeferredHolder<BlockEntityType<*>, BlockEntityType<RackBlockEntity>> = 
        BLOCK_ENTITIES.register("rack") {
            BlockEntityType.Builder.of(
                ::RackBlockEntity,
                ModBlocks.RACK.get()
            ).build(null)
        }
    
    val ASSEMBLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<AssemblerBlockEntity>> = 
        BLOCK_ENTITIES.register("assembler") {
            BlockEntityType.Builder.of(
                ::AssemblerBlockEntity,
                ModBlocks.ASSEMBLER.get()
            ).build(null)
        }
    
    val DISASSEMBLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<DisassemblerBlockEntity>> = 
        BLOCK_ENTITIES.register("disassembler") {
            BlockEntityType.Builder.of(
                ::DisassemblerBlockEntity,
                ModBlocks.DISASSEMBLER.get()
            ).build(null)
        }
    
    val PRINTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<PrinterBlockEntity>> = 
        BLOCK_ENTITIES.register("printer") {
            BlockEntityType.Builder.of(
                ::PrinterBlockEntity,
                ModBlocks.PRINTER.get()
            ).build(null)
        }
    
    // ========================================
    // Holographics
    // ========================================
    
    val HOLOGRAM: DeferredHolder<BlockEntityType<*>, BlockEntityType<HologramBlockEntity>> = 
        BLOCK_ENTITIES.register("hologram") {
            BlockEntityType.Builder.of(
                ::HologramBlockEntity,
                ModBlocks.HOLOGRAM_TIER1.get(),
                ModBlocks.HOLOGRAM_TIER2.get()
            ).build(null)
        }
    
    // ========================================
    // Microcontroller
    // ========================================
    
    val MICROCONTROLLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<MicrocontrollerBlockEntity>> = 
        BLOCK_ENTITIES.register("microcontroller") {
            BlockEntityType.Builder.of(
                ::MicrocontrollerBlockEntity,
                ModBlocks.MICROCONTROLLER.get()
            ).build(null)
        }
    
    val SWITCH: DeferredHolder<BlockEntityType<*>, BlockEntityType<SwitchBlockEntity>> =
        BLOCK_ENTITIES.register("switch") {
            BlockEntityType.Builder.of(
                ::SwitchBlockEntity,
                ModBlocks.SWITCH.get()
            ).build(null)
        }
    
    val NET_SPLITTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<NetSplitterBlockEntity>> =
        BLOCK_ENTITIES.register("net_splitter") {
            BlockEntityType.Builder.of(
                ::NetSplitterBlockEntity,
                ModBlocks.NET_SPLITTER.get()
            ).build(null)
        }
    
    val ROBOT: DeferredHolder<BlockEntityType<*>, BlockEntityType<RobotBlockEntity>> =
        BLOCK_ENTITIES.register("robot") {
            BlockEntityType.Builder.of(
                ::RobotBlockEntity,
                ModBlocks.ROBOT.get()
            ).build(null)
        }
    
    // ========================================
    // Web Display
    // ========================================
    
    val WEB_DISPLAY: DeferredHolder<BlockEntityType<*>, BlockEntityType<WebDisplayBlockEntity>> =
        BLOCK_ENTITIES.register("web_display") {
            BlockEntityType.Builder.of(
                ::WebDisplayBlockEntity,
                ModBlocks.WEB_DISPLAY.get()
            ).build(null)
        }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        BLOCK_ENTITIES.register(bus)
        OpenComputers.LOGGER.debug("Registered block entities")
    }
}
