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
    val WEB_DISPLAY_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.WEB_DISPLAY)
    
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
    // Web Display Items
    // ========================================
    
    val LASER_POINTER: DeferredItem<LaserPointerItem> = ITEMS.registerItem("laser_pointer", ::LaserPointerItem)
    
    val REMOTE_KEYBOARD: DeferredItem<RemoteKeyboardItem> = ITEMS.registerItem("remote_keyboard", ::RemoteKeyboardItem)
    
    // ========================================
    // APUs (Accelerated Processing Units)
    // ========================================

    val APU_TIER1: DeferredItem<APUItem> = ITEMS.registerItem("apu1") { props ->
        APUItem(0, props)
    }

    val APU_TIER2: DeferredItem<APUItem> = ITEMS.registerItem("apu2") { props ->
        APUItem(1, props)
    }

    // ========================================
    // Keyboard Components
    // ========================================

    val ARROW_KEYS: DeferredItem<MaterialItems.ArrowKeysItem> = ITEMS.registerItem("arrowkeys") { props ->
        MaterialItems.ArrowKeysItem(props)
    }
    val BUTTON_GROUP: DeferredItem<MaterialItems.ButtonGroupItem> = ITEMS.registerItem("buttongroup") { props ->
        MaterialItems.ButtonGroupItem(props)
    }
    val NUMPAD: DeferredItem<MaterialItems.NumpadItem> = ITEMS.registerItem("numpad") { props ->
        MaterialItems.NumpadItem(props)
    }

    // ========================================
    // Servers
    // ========================================

    val SERVER_TIER1: DeferredItem<ServerItem> = ITEMS.registerItem("server1") { props ->
        ServerItem(props, 1)
    }
    val SERVER_TIER2: DeferredItem<ServerItem> = ITEMS.registerItem("server2") { props ->
        ServerItem(props, 2)
    }
    val SERVER_TIER3: DeferredItem<ServerItem> = ITEMS.registerItem("server3") { props ->
        ServerItem(props, 3)
    }
    val SERVER_CREATIVE: DeferredItem<ServerItem> = ITEMS.registerItem("server_creative") { props ->
        ServerItem(props, 4)
    }

    // ========================================
    // Remote Terminal / Terminal Server
    // ========================================

    val REMOTE_TERMINAL: DeferredItem<RemoteTerminalItem> = ITEMS.registerItem("remoteterminal", ::RemoteTerminalItem)
    val TERMINAL_SERVER: DeferredItem<TerminalServerItem> = ITEMS.registerItem("terminalserver", ::TerminalServerItem)

    // ========================================
    // Microcontroller Cases
    // ========================================

    val MICROCONTROLLER_CASE_TIER1: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase1") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 1)
    }
    val MICROCONTROLLER_CASE_TIER2: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase2") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 2)
    }
    val MICROCONTROLLER_CASE_CREATIVE: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase_creative") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 3)
    }

    // ========================================
    // Tablet Cases
    // ========================================

    val TABLET_CASE_TIER1: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase1") { props ->
        MaterialItems.TabletCaseItem(props, 1)
    }
    val TABLET_CASE_TIER2: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase2") { props ->
        MaterialItems.TabletCaseItem(props, 2)
    }
    val TABLET_CASE_CREATIVE: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase_creative") { props ->
        MaterialItems.TabletCaseItem(props, 3)
    }

    // ========================================
    // Higher Tier Memory (Tier 4/5/6)
    // ========================================
    
    val MEMORY_TIER4: DeferredItem<MemoryItem> = ITEMS.registerItem("ram4") { props ->
        MemoryItem(props, 4)
    }
    
    val MEMORY_TIER5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram5") { props ->
        MemoryItem(props, 5)
    }
    
    val MEMORY_TIER6: DeferredItem<MemoryItem> = ITEMS.registerItem("ram6") { props ->
        MemoryItem(props, 6)
    }

    // ========================================
    // Creative APU
    // ========================================
    
    val APU_CREATIVE: DeferredItem<APUItem> = ITEMS.registerItem("apu_creative") { props ->
        APUItem(props, 3)
    }

    // ========================================
    // Loot Disks & Storage
    // ========================================
    
    val LUA_BIOS: DeferredItem<EEPROMItem> = ITEMS.registerItem("luabios", ::EEPROMItem)
    
    val OPENOS: DeferredItem<FloppyItem> = ITEMS.registerItem("openos", ::FloppyItem)
    
    val DISK: DeferredItem<HDDItem> = ITEMS.registerItem("disk") { props ->
        HDDItem(props, 1) // Managed disk
    }

    // ========================================
    // Assembled Entities
    // ========================================
    
    val DRONE: DeferredItem<Item> = ITEMS.registerSimpleItem("drone") // TODO: DroneItem class

    // ========================================
    // Additional Upgrades
    // ========================================
    
    val UPGRADE_STICKY_PISTON: DeferredItem<UpgradeItem> = ITEMS.registerItem("stickypistonupgrade") { props ->
        UpgradeItem(props, "sticky_piston")
    }

    // ========================================
    // Creative/Debug Items
    // ========================================
    
    val ABSTRACT_BUS_CARD: DeferredItem<Item> = ITEMS.registerSimpleItem("abstractbuscard")
    
    val DEBUGGER: DeferredItem<Item> = ITEMS.registerSimpleItem("debugger")
    
    val TEXTURE_PICKER: DeferredItem<Item> = ITEMS.registerSimpleItem("texturepicker")
    
    val DISK_DRIVE_MOUNTABLE: DeferredItem<Item> = ITEMS.registerSimpleItem("diskdrivemountable")

    // ========================================
    // Additional Materials
    // ========================================
    
    val DIAMOND_CHIP: DeferredItem<Item> = ITEMS.registerSimpleItem("chipdiamond")

    val INK_CARTRIDGE_COLOR: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridge_color")
    
    // ========================================
    // Easter Eggs
    // ========================================
    
    val PRESENT: DeferredItem<Item> = ITEMS.registerSimpleItem("present")

    val SIGN_IO_UPGRADE: DeferredItem<SignIOUpgradeItem> = ITEMS.registerItem("signio_upgrade", ::SignIOUpgradeItem)

    // ========================================
    // Aliases for datagen compatibility
    // ========================================

    // Block item aliases
    val CABLE get() = CABLE_ITEM
    val CAPACITOR get() = CAPACITOR_ITEM

    // Item name aliases
    val FLOPPY_DISK get() = FLOPPY
    val GRAPHICS_CARD_TIER1 get() = GPU_TIER1
    val GRAPHICS_CARD_TIER2 get() = GPU_TIER2
    val GRAPHICS_CARD_TIER3 get() = GPU_TIER3
    val MEMORY_TIER15 get() = MEMORY_TIER1_5
    val MEMORY_TIER25 get() = MEMORY_TIER2_5
    val MEMORY_TIER35 get() = MEMORY_TIER3_5
    val RAM_TIER1 get() = MEMORY_TIER1
    val RAM_TIER15 get() = MEMORY_TIER1_5
    val RAM_TIER2 get() = MEMORY_TIER2
    val RAM_TIER25 get() = MEMORY_TIER2_5
    val RAM_TIER3 get() = MEMORY_TIER3
    val RAM_TIER35 get() = MEMORY_TIER3_5
    val RAM_TIER4 get() = MEMORY_TIER4
    val RAM_TIER5 get() = MEMORY_TIER5
    val RAM_TIER6 get() = MEMORY_TIER6

    // Upgrade aliases
    val ANGEL_UPGRADE get() = UPGRADE_ANGEL
    val BATTERY_UPGRADE_TIER1 get() = UPGRADE_BATTERY_TIER1
    val BATTERY_UPGRADE_TIER2 get() = UPGRADE_BATTERY_TIER2
    val BATTERY_UPGRADE_TIER3 get() = UPGRADE_BATTERY_TIER3
    val CHUNKLOADER_UPGRADE get() = UPGRADE_CHUNKLOADER
    val CRAFTING_UPGRADE get() = UPGRADE_CRAFTING
    val DATABASE_UPGRADE_TIER1 get() = UPGRADE_DATABASE_TIER1
    val DATABASE_UPGRADE_TIER2 get() = UPGRADE_DATABASE_TIER2
    val DATABASE_UPGRADE_TIER3 get() = UPGRADE_DATABASE_TIER3
    val EXPERIENCE_UPGRADE get() = UPGRADE_EXPERIENCE
    val GENERATOR_UPGRADE get() = UPGRADE_GENERATOR
    val HOVER_UPGRADE_TIER1 get() = UPGRADE_HOVER_TIER1
    val HOVER_UPGRADE_TIER2 get() = UPGRADE_HOVER_TIER2
    val INVENTORY_UPGRADE get() = UPGRADE_INVENTORY
    val INVENTORY_CONTROLLER_UPGRADE get() = UPGRADE_INVENTORY_CONTROLLER
    val LEASH_UPGRADE get() = UPGRADE_LEASH
    val MFU get() = UPGRADE_MFU
    val NAVIGATION_UPGRADE get() = UPGRADE_NAVIGATION
    val PISTON_UPGRADE get() = UPGRADE_PISTON
    val SIGN_UPGRADE get() = UPGRADE_SIGN
    val SOLAR_GENERATOR_UPGRADE get() = UPGRADE_SOLAR_GENERATOR
    val TANK_UPGRADE get() = UPGRADE_TANK
    val TANK_CONTROLLER_UPGRADE get() = UPGRADE_TANK_CONTROLLER
    val TRACTOR_BEAM_UPGRADE get() = UPGRADE_TRACTOR_BEAM
    val TRADING_UPGRADE get() = UPGRADE_TRADING
    val STICKY_PISTON_UPGRADE get() = UPGRADE_STICKY_PISTON

    // Material aliases
    val CIRCUIT_BOARD get() = CIRCUIT_TIER1
    val PRINTED_CIRCUIT_BOARD get() = CIRCUIT_TIER2
    val CHIP_DIAMOND get() = DIAMOND_CHIP

    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        ITEMS.register(bus)
        OpenComputers.LOGGER.debug("Registered items")
    }
}
