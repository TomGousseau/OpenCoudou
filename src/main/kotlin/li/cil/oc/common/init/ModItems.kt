package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.item.*
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for all OpenComputers items.
 */
object ModItems {
    private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(OpenComputers.MOD_ID)
    
    // ========================================
    // Block Items
    // ========================================
    
    val CASE_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER1)
    val CASE_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER2)
    val CASE_TIER3_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER3)
    val CASE_CREATIVE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_CREATIVE)
    
    val SCREEN_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER1)
    val SCREEN_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER2)
    val SCREEN_TIER3_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER3)
    
    val KEYBOARD_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.KEYBOARD)
    val REDSTONE_IO_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_IO)
    
    val CABLE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CABLE)
    val RELAY_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RELAY)
    val ACCESS_POINT_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ACCESS_POINT)
    
    val CAPACITOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CAPACITOR)
    val POWER_CONVERTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_CONVERTER)
    val POWER_DISTRIBUTOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_DISTRIBUTOR)
    val CHARGER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CHARGER)
    
    val ADAPTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ADAPTER)
    val TRANSPOSER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.TRANSPOSER)
    val DISK_DRIVE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.DISK_DRIVE)
    val RAID_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RAID)
    
    val GEOLYZER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.GEOLYZER)
    val MOTION_SENSOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.MOTION_SENSOR)
    val WAYPOINT_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.WAYPOINT)
    
    val RACK_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RACK)
    val ASSEMBLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ASSEMBLER)
    val DISASSEMBLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.DISASSEMBLER)
    val PRINTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.PRINTER)
    
    val HOLOGRAM_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.HOLOGRAM_TIER1)
    val HOLOGRAM_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.HOLOGRAM_TIER2)
    
    val MICROCONTROLLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.MICROCONTROLLER)
    val CHAMELIUM_BLOCK_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CHAMELIUM_BLOCK)
    
    // ========================================
    // CPUs
    // ========================================
    
    val CPU_TIER1: DeferredItem<CPUItem> = ITEMS.registerItem("cpu1") { props ->
        CPUItem(0, props)
    }
    
    val CPU_TIER2: DeferredItem<CPUItem> = ITEMS.registerItem("cpu2") { props ->
        CPUItem(1, props)
    }
    
    val CPU_TIER3: DeferredItem<CPUItem> = ITEMS.registerItem("cpu3") { props ->
        CPUItem(2, props)
    }
    
    // ========================================
    // Memory
    // ========================================
    
    val MEMORY_TIER1: DeferredItem<MemoryItem> = ITEMS.registerItem("ram1") { props ->
        MemoryItem(0, props)
    }
    
    val MEMORY_TIER1_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram1_5") { props ->
        MemoryItem(1, props)
    }
    
    val MEMORY_TIER2: DeferredItem<MemoryItem> = ITEMS.registerItem("ram2") { props ->
        MemoryItem(2, props)
    }
    
    val MEMORY_TIER2_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram2_5") { props ->
        MemoryItem(3, props)
    }
    
    val MEMORY_TIER3: DeferredItem<MemoryItem> = ITEMS.registerItem("ram3") { props ->
        MemoryItem(4, props)
    }
    
    val MEMORY_TIER3_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram3_5") { props ->
        MemoryItem(5, props)
    }
    
    // ========================================
    // Graphics Cards
    // ========================================
    
    val GPU_TIER1: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard1") { props ->
        GPUItem(0, props)
    }
    
    val GPU_TIER2: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard2") { props ->
        GPUItem(1, props)
    }
    
    val GPU_TIER3: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard3") { props ->
        GPUItem(2, props)
    }
    
    // ========================================
    // Storage
    // ========================================
    
    val HDD_TIER1: DeferredItem<HDDItem> = ITEMS.registerItem("hdd1") { props ->
        HDDItem(0, props)
    }
    
    val HDD_TIER2: DeferredItem<HDDItem> = ITEMS.registerItem("hdd2") { props ->
        HDDItem(1, props)
    }
    
    val HDD_TIER3: DeferredItem<HDDItem> = ITEMS.registerItem("hdd3") { props ->
        HDDItem(2, props)
    }
    
    val FLOPPY: DeferredItem<FloppyItem> = ITEMS.registerItem("floppy", ::FloppyItem)
    
    val EEPROM: DeferredItem<EEPROMItem> = ITEMS.registerItem("eeprom", ::EEPROMItem)
    
    // ========================================
    // Network Cards
    // ========================================
    
    val NETWORK_CARD: DeferredItem<NetworkCardItem> = ITEMS.registerItem("lancard", ::NetworkCardItem)
    
    val WIRELESS_CARD_TIER1: DeferredItem<WirelessCardItem> = ITEMS.registerItem("wlancard1") { props ->
        WirelessCardItem(0, props)
    }
    
    val WIRELESS_CARD_TIER2: DeferredItem<WirelessCardItem> = ITEMS.registerItem("wlancard2") { props ->
        WirelessCardItem(1, props)
    }
    
    val INTERNET_CARD: DeferredItem<InternetCardItem> = ITEMS.registerItem("internetcard", ::InternetCardItem)
    
    val LINKED_CARD: DeferredItem<LinkedCardItem> = ITEMS.registerItem("linkedcard", ::LinkedCardItem)
    
    // ========================================
    // Other Cards
    // ========================================
    
    val REDSTONE_CARD_TIER1: DeferredItem<RedstoneCardItem> = ITEMS.registerItem("redstonecard1") { props ->
        RedstoneCardItem(0, props)
    }
    
    val REDSTONE_CARD_TIER2: DeferredItem<RedstoneCardItem> = ITEMS.registerItem("redstonecard2") { props ->
        RedstoneCardItem(1, props)
    }
    
    val DATA_CARD_TIER1: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard1") { props ->
        DataCardItem(0, props)
    }
    
    val DATA_CARD_TIER2: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard2") { props ->
        DataCardItem(1, props)
    }
    
    val DATA_CARD_TIER3: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard3") { props ->
        DataCardItem(2, props)
    }
    
    val WORLD_SENSOR_CARD: DeferredItem<WorldSensorCardItem> = ITEMS.registerItem("worldsensorcard", ::WorldSensorCardItem)
    
    // ========================================
    // Upgrades
    // ========================================
    
    val UPGRADE_ANGEL: DeferredItem<UpgradeItem> = ITEMS.registerItem("angelupgrade") { props ->
        UpgradeItem("angel", props)
    }
    
    val UPGRADE_BATTERY_TIER1: DeferredItem<UpgradeItem> = ITEMS.registerItem("batteryupgrade1") { props ->
        UpgradeItem("battery", 0, props)
    }
    
    val UPGRADE_BATTERY_TIER2: DeferredItem<UpgradeItem> = ITEMS.registerItem("batteryupgrade2") { props ->
        UpgradeItem("battery", 1, props)
    }
    
    val UPGRADE_BATTERY_TIER3: DeferredItem<UpgradeItem> = ITEMS.registerItem("batteryupgrade3") { props ->
        UpgradeItem("battery", 2, props)
    }
    
    val UPGRADE_CHUNKLOADER: DeferredItem<UpgradeItem> = ITEMS.registerItem("chunkloaderupgrade") { props ->
        UpgradeItem("chunkloader", props)
    }
    
    val UPGRADE_CRAFTING: DeferredItem<UpgradeItem> = ITEMS.registerItem("craftingupgrade") { props ->
        UpgradeItem("crafting", props)
    }
    
    val UPGRADE_DATABASE_TIER1: DeferredItem<UpgradeItem> = ITEMS.registerItem("databaseupgrade1") { props ->
        UpgradeItem("database", 0, props)
    }
    
    val UPGRADE_DATABASE_TIER2: DeferredItem<UpgradeItem> = ITEMS.registerItem("databaseupgrade2") { props ->
        UpgradeItem("database", 1, props)
    }
    
    val UPGRADE_DATABASE_TIER3: DeferredItem<UpgradeItem> = ITEMS.registerItem("databaseupgrade3") { props ->
        UpgradeItem("database", 2, props)
    }
    
    val UPGRADE_EXPERIENCE: DeferredItem<UpgradeItem> = ITEMS.registerItem("experienceupgrade") { props ->
        UpgradeItem("experience", props)
    }
    
    val UPGRADE_GENERATOR: DeferredItem<UpgradeItem> = ITEMS.registerItem("generatorupgrade") { props ->
        UpgradeItem("generator", props)
    }
    
    val UPGRADE_HOVER_TIER1: DeferredItem<UpgradeItem> = ITEMS.registerItem("hoverupgrade1") { props ->
        UpgradeItem("hover", 0, props)
    }
    
    val UPGRADE_HOVER_TIER2: DeferredItem<UpgradeItem> = ITEMS.registerItem("hoverupgrade2") { props ->
        UpgradeItem("hover", 1, props)
    }
    
    val UPGRADE_INVENTORY: DeferredItem<UpgradeItem> = ITEMS.registerItem("inventoryupgrade") { props ->
        UpgradeItem("inventory", props)
    }
    
    val UPGRADE_INVENTORY_CONTROLLER: DeferredItem<UpgradeItem> = ITEMS.registerItem("inventorycontrollerupgrade") { props ->
        UpgradeItem("inventory_controller", props)
    }
    
    val UPGRADE_LEASH: DeferredItem<UpgradeItem> = ITEMS.registerItem("leashupgrade") { props ->
        UpgradeItem("leash", props)
    }
    
    val UPGRADE_MFU: DeferredItem<UpgradeItem> = ITEMS.registerItem("mfu") { props ->
        UpgradeItem("mfu", props)
    }
    
    val UPGRADE_NAVIGATION: DeferredItem<UpgradeItem> = ITEMS.registerItem("navigationupgrade") { props ->
        UpgradeItem("navigation", props)
    }
    
    val UPGRADE_PISTON: DeferredItem<UpgradeItem> = ITEMS.registerItem("pistonupgrade") { props ->
        UpgradeItem("piston", props)
    }
    
    val UPGRADE_SIGN: DeferredItem<UpgradeItem> = ITEMS.registerItem("signupgrade") { props ->
        UpgradeItem("sign", props)
    }
    
    val UPGRADE_SOLAR_GENERATOR: DeferredItem<UpgradeItem> = ITEMS.registerItem("solargeneratorupgrade") { props ->
        UpgradeItem("solar_generator", props)
    }
    
    val UPGRADE_TANK: DeferredItem<UpgradeItem> = ITEMS.registerItem("tankupgrade") { props ->
        UpgradeItem("tank", props)
    }
    
    val UPGRADE_TANK_CONTROLLER: DeferredItem<UpgradeItem> = ITEMS.registerItem("tankcontrollerupgrade") { props ->
        UpgradeItem("tank_controller", props)
    }
    
    val UPGRADE_TRACTOR_BEAM: DeferredItem<UpgradeItem> = ITEMS.registerItem("tractorupgrade") { props ->
        UpgradeItem("tractor_beam", props)
    }
    
    val UPGRADE_TRADING: DeferredItem<UpgradeItem> = ITEMS.registerItem("tradingupgrade") { props ->
        UpgradeItem("trading", props)
    }
    
    // ========================================
    // Containers
    // ========================================
    
    val CARD_CONTAINER_TIER1: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer1") { props ->
        ContainerItem("card", 0, props)
    }
    
    val CARD_CONTAINER_TIER2: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer2") { props ->
        ContainerItem("card", 1, props)
    }
    
    val CARD_CONTAINER_TIER3: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer3") { props ->
        ContainerItem("card", 2, props)
    }
    
    val UPGRADE_CONTAINER_TIER1: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer1") { props ->
        ContainerItem("upgrade", 0, props)
    }
    
    val UPGRADE_CONTAINER_TIER2: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer2") { props ->
        ContainerItem("upgrade", 1, props)
    }
    
    val UPGRADE_CONTAINER_TIER3: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer3") { props ->
        ContainerItem("upgrade", 2, props)
    }
    
    // ========================================
    // Special Items
    // ========================================
    
    val TABLET: DeferredItem<TabletItem> = ITEMS.registerItem("tablet", ::TabletItem)
    
    val DRONE_CASE_TIER1: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase1") { props ->
        DroneCaseItem(0, props)
    }
    
    val DRONE_CASE_TIER2: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase2") { props ->
        DroneCaseItem(1, props)
    }
    
    val DRONE_CASE_CREATIVE: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase_creative") { props ->
        DroneCaseItem(2, props)
    }
    
    val HOVER_BOOTS: DeferredItem<HoverBootsItem> = ITEMS.registerItem("hoverboots", ::HoverBootsItem)
    
    val NANOMACHINES: DeferredItem<NanomachinesItem> = ITEMS.registerItem("nanomachines", ::NanomachinesItem)
    
    val TERMINAL: DeferredItem<TerminalItem> = ITEMS.registerItem("terminal", ::TerminalItem)
    
    val ANALYZER: DeferredItem<AnalyzerItem> = ITEMS.registerItem("analyzer", ::AnalyzerItem)
    
    val MANUAL: DeferredItem<ManualItem> = ITEMS.registerItem("manual", ::ManualItem)
    
    // ========================================
    // Materials
    // ========================================
    
    val CHAMELIUM: DeferredItem<Item> = ITEMS.registerSimpleItem("chamelium")
    
    val CIRCUIT_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("circuitboard")
    val CIRCUIT_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("printedcircuitboard")
    val CIRCUIT_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("integratedcircuit")
    val CIRCUIT_TIER4: DeferredItem<Item> = ITEMS.registerSimpleItem("processor")
    
    val TRANSISTOR: DeferredItem<Item> = ITEMS.registerSimpleItem("transistor")
    val MICROCHIP_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("chip1")
    val MICROCHIP_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("chip2")
    val MICROCHIP_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("chip3")
    
    val ALU: DeferredItem<Item> = ITEMS.registerSimpleItem("alu")
    val CONTROL_UNIT: DeferredItem<Item> = ITEMS.registerSimpleItem("controlunit")
    
    val DISK_PLATTER: DeferredItem<Item> = ITEMS.registerSimpleItem("diskplatter")
    val CARD_BASE: DeferredItem<Item> = ITEMS.registerSimpleItem("card")
    
    val CUTTING_WIRE: DeferredItem<Item> = ITEMS.registerSimpleItem("cuttingwire")
    val ACID: DeferredItem<Item> = ITEMS.registerSimpleItem("acid")
    val RAW_CIRCUIT_BOARD: DeferredItem<Item> = ITEMS.registerSimpleItem("rawcircuitboard")
    
    val INK_CARTRIDGE: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridge")
    val INK_CARTRIDGE_EMPTY: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridgeempty")
    
    /** Item representing a completed 3D-printed block/object from the Printer. */
    val PRINTED_BLOCK: DeferredItem<Item> = ITEMS.registerSimpleItem("printedblock")
    
    val INTERWEB: DeferredItem<Item> = ITEMS.registerSimpleItem("interweb")
    val COMPONENT_BUS_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus1")
    val COMPONENT_BUS_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus2")
    val COMPONENT_BUS_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus3")
    
    // ========================================
    // Tools
    // ========================================
    
    val WRENCH: DeferredItem<WrenchItem> = ITEMS.registerItem("wrench", ::WrenchItem)
    
    val DEBUG_CARD: DeferredItem<DebugCardItem> = ITEMS.registerItem("debugcard", ::DebugCardItem)
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        ITEMS.register(bus)
        OpenComputers.LOGGER.debug("Registered items")
    }
}
