package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.block.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for all OpenComputers blocks.
 */
object ModBlocks {
    private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(OpenComputers.MOD_ID)
    
    // ========================================
    // Computer Cases
    // ========================================
    
    val CASE_TIER1: DeferredBlock<CaseBlock> = BLOCKS.registerBlock("case1", ::CaseBlock) {
        baseProperties().strength(2.0f)
    }
    
    val CASE_TIER2: DeferredBlock<CaseBlock> = BLOCKS.registerBlock("case2", ::CaseBlock) {
        baseProperties().strength(2.5f)
    }
    
    val CASE_TIER3: DeferredBlock<CaseBlock> = BLOCKS.registerBlock("case3", ::CaseBlock) {
        baseProperties().strength(3.0f)
    }
    
    val CASE_CREATIVE: DeferredBlock<CaseBlock> = BLOCKS.registerBlock("case_creative", ::CaseBlock) {
        baseProperties().strength(-1.0f, 3600000.0f)
    }
    
    // ========================================
    // Screens
    // ========================================
    
    val SCREEN_TIER1: DeferredBlock<ScreenBlock> = BLOCKS.registerBlock("screen1", ::ScreenBlock) {
        baseProperties().strength(2.0f).noOcclusion()
    }
    
    val SCREEN_TIER2: DeferredBlock<ScreenBlock> = BLOCKS.registerBlock("screen2", ::ScreenBlock) {
        baseProperties().strength(2.5f).noOcclusion()
    }
    
    val SCREEN_TIER3: DeferredBlock<ScreenBlock> = BLOCKS.registerBlock("screen3", ::ScreenBlock) {
        baseProperties().strength(3.0f).noOcclusion()
    }
    
    // ========================================
    // Input/Output
    // ========================================
    
    val KEYBOARD: DeferredBlock<KeyboardBlock> = BLOCKS.registerBlock("keyboard", ::KeyboardBlock) {
        baseProperties().strength(1.5f).noOcclusion()
    }
    
    val REDSTONE_IO: DeferredBlock<RedstoneIOBlock> = BLOCKS.registerBlock("redstone", ::RedstoneIOBlock) {
        baseProperties().strength(2.0f)
    }
    
    // ========================================
    // Networking
    // ========================================
    
    val CABLE: DeferredBlock<CableBlock> = BLOCKS.registerBlock("cable", ::CableBlock) {
        baseProperties().strength(0.5f).noOcclusion()
    }
    
    val RELAY: DeferredBlock<RelayBlock> = BLOCKS.registerBlock("relay", ::RelayBlock) {
        baseProperties().strength(2.0f)
    }
    
    val ACCESS_POINT: DeferredBlock<AccessPointBlock> = BLOCKS.registerBlock("accesspoint", ::AccessPointBlock) {
        baseProperties().strength(2.0f)
    }
    
    // ========================================
    // Power
    // ========================================
    
    val CAPACITOR: DeferredBlock<CapacitorBlock> = BLOCKS.registerBlock("capacitor", ::CapacitorBlock) {
        baseProperties().strength(2.0f)
    }
    
    val POWER_CONVERTER: DeferredBlock<PowerConverterBlock> = BLOCKS.registerBlock("powerconverter", ::PowerConverterBlock) {
        baseProperties().strength(2.0f)
    }
    
    val POWER_DISTRIBUTOR: DeferredBlock<PowerDistributorBlock> = BLOCKS.registerBlock("powerdistributor", ::PowerDistributorBlock) {
        baseProperties().strength(2.0f)
    }
    
    val CHARGER: DeferredBlock<ChargerBlock> = BLOCKS.registerBlock("charger", ::ChargerBlock) {
        baseProperties().strength(2.0f)
    }
    
    // ========================================
    // Adapters & Integration
    // ========================================
    
    val ADAPTER: DeferredBlock<AdapterBlock> = BLOCKS.registerBlock("adapter", ::AdapterBlock) {
        baseProperties().strength(2.0f)
    }
    
    val TRANSPOSER: DeferredBlock<TransposerBlock> = BLOCKS.registerBlock("transposer", ::TransposerBlock) {
        baseProperties().strength(2.0f)
    }
    
    // ========================================
    // Storage
    // ========================================
    
    val DISK_DRIVE: DeferredBlock<DiskDriveBlock> = BLOCKS.registerBlock("diskdrive", ::DiskDriveBlock) {
        baseProperties().strength(2.0f)
    }
    
    val RAID: DeferredBlock<RaidBlock> = BLOCKS.registerBlock("raid", ::RaidBlock) {
        baseProperties().strength(2.5f)
    }
    
    // ========================================
    // Sensors & Analysis
    // ========================================
    
    val GEOLYZER: DeferredBlock<GeolyzerBlock> = BLOCKS.registerBlock("geolyzer", ::GeolyzerBlock) {
        baseProperties().strength(2.0f)
    }
    
    val MOTION_SENSOR: DeferredBlock<MotionSensorBlock> = BLOCKS.registerBlock("motionsensor", ::MotionSensorBlock) {
        baseProperties().strength(2.0f)
    }
    
    val WAYPOINT: DeferredBlock<WaypointBlock> = BLOCKS.registerBlock("waypoint", ::WaypointBlock) {
        baseProperties().strength(2.0f).lightLevel { 4 }
    }
    
    // ========================================
    // Advanced
    // ========================================
    
    val RACK: DeferredBlock<RackBlock> = BLOCKS.registerBlock("rack", ::RackBlock) {
        baseProperties().strength(3.0f)
    }
    
    val ASSEMBLER: DeferredBlock<AssemblerBlock> = BLOCKS.registerBlock("assembler", ::AssemblerBlock) {
        baseProperties().strength(3.0f)
    }
    
    val DISASSEMBLER: DeferredBlock<DisassemblerBlock> = BLOCKS.registerBlock("disassembler", ::DisassemblerBlock) {
        baseProperties().strength(3.0f)
    }
    
    val PRINTER: DeferredBlock<PrinterBlock> = BLOCKS.registerBlock("printer", ::PrinterBlock) {
        baseProperties().strength(2.5f)
    }
    
    // ========================================
    // Holographics
    // ========================================
    
    val HOLOGRAM_TIER1: DeferredBlock<HologramBlock> = BLOCKS.registerBlock("hologram1", ::HologramBlock) {
        baseProperties().strength(2.0f).noOcclusion().lightLevel { 7 }
    }
    
    val HOLOGRAM_TIER2: DeferredBlock<HologramBlock> = BLOCKS.registerBlock("hologram2", ::HologramBlock) {
        baseProperties().strength(2.5f).noOcclusion().lightLevel { 10 }
    }
    
    // ========================================
    // Microcontroller
    // ========================================
    
    val MICROCONTROLLER: DeferredBlock<MicrocontrollerBlock> = BLOCKS.registerBlock("microcontroller", ::MicrocontrollerBlock) {
        baseProperties().strength(2.0f)
    }
    
    // ========================================
    // Materials
    // ========================================
    
    val CHAMELIUM_BLOCK: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("chameliumblock") {
        baseProperties().strength(1.5f)
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    private fun baseProperties(): BlockBehaviour.Properties {
        return BlockBehaviour.Properties.of()
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
    }
    
    /**
     * Gets the tier of a case block.
     */
    fun getCaseTier(block: Block): Int = when (block) {
        CASE_TIER1.get() -> 0
        CASE_TIER2.get() -> 1
        CASE_TIER3.get() -> 2
        CASE_CREATIVE.get() -> 3
        else -> 0
    }
    
    /**
     * Gets the tier of a screen block.
     */
    fun getScreenTier(block: Block): Int = when (block) {
        SCREEN_TIER1.get() -> 0
        SCREEN_TIER2.get() -> 1
        SCREEN_TIER3.get() -> 2
        else -> 0
    }
    
    /**
     * Gets the tier of a hologram block.
     */
    fun getHologramTier(block: Block): Int = when (block) {
        HOLOGRAM_TIER1.get() -> 0
        HOLOGRAM_TIER2.get() -> 1
        else -> 0
    }
    
    /**
     * Registers all blocks with the event bus.
     */
    fun register(bus: IEventBus) {
        BLOCKS.register(bus)
        OpenComputers.LOGGER.debug("Registered blocks")
    }
}
