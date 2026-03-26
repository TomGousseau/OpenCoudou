package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.LanguageProvider

/**
 * Language provider for OpenComputers.
 * Generates localization files for blocks, items, and other translatable content.
 */
class ModLanguageProvider(
    output: PackOutput,
    locale: String = "en_us"
) : LanguageProvider(output, OpenComputers.MOD_ID, locale) {
    
    override fun addTranslations() {
        // === Tab ===
        add("itemGroup.${OpenComputers.MOD_ID}", "OpenComputers")
        add("itemGroup.${OpenComputers.MOD_ID}.main", "OpenComputers")
        
        // === Blocks ===
        addBlock(ModBlocks.CASE_TIER1, "Computer Case (Tier 1)")
        addBlock(ModBlocks.CASE_TIER2, "Computer Case (Tier 2)")
        addBlock(ModBlocks.CASE_TIER3, "Computer Case (Tier 3)")
        addBlock(ModBlocks.CASE_CREATIVE, "Creative Computer Case")
        
        addBlock(ModBlocks.SCREEN_TIER1, "Screen (Tier 1)")
        addBlock(ModBlocks.SCREEN_TIER2, "Screen (Tier 2)")
        addBlock(ModBlocks.SCREEN_TIER3, "Screen (Tier 3)")
        
        addBlock(ModBlocks.KEYBOARD, "Keyboard")
        
        addBlock(ModBlocks.CAPACITOR, "Capacitor")
        addBlock(ModBlocks.CHARGER, "Charger")
        addBlock(ModBlocks.POWER_CONVERTER, "Power Converter")
        addBlock(ModBlocks.POWER_DISTRIBUTOR, "Power Distributor")
        
        addBlock(ModBlocks.RELAY, "Relay")
        addBlock(ModBlocks.SWITCH, "Switch")
        addBlock(ModBlocks.ACCESS_POINT, "Access Point")
        addBlock(ModBlocks.NET_SPLITTER, "Net Splitter")
        
        addBlock(ModBlocks.DISK_DRIVE, "Disk Drive")
        addBlock(ModBlocks.RAID, "RAID")
        
        addBlock(ModBlocks.ADAPTER, "Adapter")
        addBlock(ModBlocks.ASSEMBLER, "Electronics Assembler")
        addBlock(ModBlocks.DISASSEMBLER, "Disassembler")
        addBlock(ModBlocks.GEOLYZER, "Geolyzer")
        addBlock(ModBlocks.HOLOGRAM_TIER1, "Hologram Projector (Tier 1)")
        addBlock(ModBlocks.HOLOGRAM_TIER2, "Hologram Projector (Tier 2)")
        addBlock(ModBlocks.MOTION_SENSOR, "Motion Sensor")
        addBlock(ModBlocks.PRINTER, "3D Printer")
        addBlock(ModBlocks.RACK, "Server Rack")
        addBlock(ModBlocks.REDSTONE_IO, "Redstone I/O")
        addBlock(ModBlocks.TRANSPOSER, "Transposer")
        addBlock(ModBlocks.WAYPOINT, "Waypoint")
        
        addBlock(ModBlocks.ROBOT, "Robot")
        addBlock(ModBlocks.MICROCONTROLLER, "Microcontroller")
        
        addBlock(ModBlocks.CHAMELIUM_BLOCK, "Chamelium Block")
        addBlock(ModBlocks.CABLE, "Cable")
        
        // === Materials ===
        addItem(ModItems.RAW_CIRCUIT_BOARD, "Raw Circuit Board")
        addItem(ModItems.CIRCUIT_BOARD, "Circuit Board")
        addItem(ModItems.PRINTED_CIRCUIT_BOARD, "Printed Circuit Board")
        addItem(ModItems.TRANSISTOR, "Transistor")
        addItem(ModItems.MICROCHIP_TIER1, "Microchip (Tier 1)")
        addItem(ModItems.MICROCHIP_TIER2, "Microchip (Tier 2)")
        addItem(ModItems.MICROCHIP_TIER3, "Microchip (Tier 3)")
        addItem(ModItems.ALU, "Arithmetic Logic Unit")
        addItem(ModItems.CONTROL_UNIT, "Control Unit")
        addItem(ModItems.DISK_PLATTER, "Disk Platter")
        addItem(ModItems.INTERWEB, "Interweb")
        addItem(ModItems.CHAMELIUM, "Chamelium")
        addItem(ModItems.INK_CARTRIDGE, "Ink Cartridge")
        addItem(ModItems.INK_CARTRIDGE_COLOR, "Color Ink Cartridge")
        addItem(ModItems.ACID, "Acid")
        addItem(ModItems.CUTTING_WIRE, "Cutting Wire")
        addItem(ModItems.CAPACITOR, "Capacitor")
        addItem(ModItems.BUTTON_GROUP, "Button Group")
        addItem(ModItems.ARROW_KEYS, "Arrow Keys")
        addItem(ModItems.NUMPAD, "Numeric Keypad")
        addItem(ModItems.CARD_BASE, "Card Base")
        addItem(ModItems.CABLE, "Cable")
        
        // === CPUs ===
        addItem(ModItems.CPU_TIER1, "Central Processing Unit (Tier 1)")
        addItem(ModItems.CPU_TIER2, "Central Processing Unit (Tier 2)")
        addItem(ModItems.CPU_TIER3, "Central Processing Unit (Tier 3)")
        
        // === Memory ===
        addItem(ModItems.RAM_TIER1, "Memory (Tier 1)")
        addItem(ModItems.RAM_TIER15, "Memory (Tier 1.5)")
        addItem(ModItems.RAM_TIER2, "Memory (Tier 2)")
        addItem(ModItems.RAM_TIER25, "Memory (Tier 2.5)")
        addItem(ModItems.RAM_TIER3, "Memory (Tier 3)")
        addItem(ModItems.RAM_TIER35, "Memory (Tier 3.5)")
        
        // === Storage ===
        addItem(ModItems.EEPROM, "EEPROM")
        addItem(ModItems.FLOPPY_DISK, "Floppy Disk")
        addItem(ModItems.HDD_TIER1, "Hard Disk Drive (Tier 1)")
        addItem(ModItems.HDD_TIER2, "Hard Disk Drive (Tier 2)")
        addItem(ModItems.HDD_TIER3, "Hard Disk Drive (Tier 3)")
        
        // === Cards ===
        addItem(ModItems.GRAPHICS_CARD_TIER1, "Graphics Card (Tier 1)")
        addItem(ModItems.GRAPHICS_CARD_TIER2, "Graphics Card (Tier 2)")
        addItem(ModItems.GRAPHICS_CARD_TIER3, "Graphics Card (Tier 3)")
        addItem(ModItems.NETWORK_CARD, "Network Card")
        addItem(ModItems.WIRELESS_CARD_TIER1, "Wireless Network Card (Tier 1)")
        addItem(ModItems.WIRELESS_CARD_TIER2, "Wireless Network Card (Tier 2)")
        addItem(ModItems.INTERNET_CARD, "Internet Card")
        addItem(ModItems.LINKED_CARD, "Linked Card")
        addItem(ModItems.REDSTONE_CARD_TIER1, "Redstone Card (Tier 1)")
        addItem(ModItems.REDSTONE_CARD_TIER2, "Redstone Card (Tier 2)")
        addItem(ModItems.DATA_CARD_TIER1, "Data Card (Tier 1)")
        addItem(ModItems.DATA_CARD_TIER2, "Data Card (Tier 2)")
        addItem(ModItems.DATA_CARD_TIER3, "Data Card (Tier 3)")
        addItem(ModItems.WORLD_SENSOR_CARD, "World Sensor Card")
        
        // === Upgrades ===
        addItem(ModItems.ANGEL_UPGRADE, "Angel Upgrade")
        addItem(ModItems.BATTERY_UPGRADE_TIER1, "Battery Upgrade (Tier 1)")
        addItem(ModItems.BATTERY_UPGRADE_TIER2, "Battery Upgrade (Tier 2)")
        addItem(ModItems.BATTERY_UPGRADE_TIER3, "Battery Upgrade (Tier 3)")
        addItem(ModItems.CHUNKLOADER_UPGRADE, "Chunkloader Upgrade")
        addItem(ModItems.CRAFTING_UPGRADE, "Crafting Upgrade")
        addItem(ModItems.DATABASE_UPGRADE_TIER1, "Database Upgrade (Tier 1)")
        addItem(ModItems.DATABASE_UPGRADE_TIER2, "Database Upgrade (Tier 2)")
        addItem(ModItems.DATABASE_UPGRADE_TIER3, "Database Upgrade (Tier 3)")
        addItem(ModItems.EXPERIENCE_UPGRADE, "Experience Upgrade")
        addItem(ModItems.GENERATOR_UPGRADE, "Generator Upgrade")
        addItem(ModItems.HOVER_UPGRADE_TIER1, "Hover Upgrade (Tier 1)")
        addItem(ModItems.HOVER_UPGRADE_TIER2, "Hover Upgrade (Tier 2)")
        addItem(ModItems.INVENTORY_UPGRADE, "Inventory Upgrade")
        addItem(ModItems.INVENTORY_CONTROLLER_UPGRADE, "Inventory Controller Upgrade")
        addItem(ModItems.NAVIGATION_UPGRADE, "Navigation Upgrade")
        addItem(ModItems.PISTON_UPGRADE, "Piston Upgrade")
        addItem(ModItems.SIGN_UPGRADE, "Sign I/O Upgrade")
        addItem(ModItems.SOLAR_GENERATOR_UPGRADE, "Solar Generator Upgrade")
        addItem(ModItems.TANK_UPGRADE, "Tank Upgrade")
        addItem(ModItems.TANK_CONTROLLER_UPGRADE, "Tank Controller Upgrade")
        addItem(ModItems.TRACTOR_BEAM_UPGRADE, "Tractor Beam Upgrade")
        addItem(ModItems.TRADING_UPGRADE, "Trading Upgrade")
        addItem(ModItems.LEASH_UPGRADE, "Leash Upgrade")
        
        // === Tools ===
        addItem(ModItems.ANALYZER, "Analyzer")
        addItem(ModItems.TERMINAL, "Terminal")
        addItem(ModItems.REMOTE_TERMINAL, "Remote Terminal")
        addItem(ModItems.MANUAL, "OpenComputers Manual")
        addItem(ModItems.NANOMACHINES, "Nanomachines")
        addItem(ModItems.WRENCH, "Wrench")
        addItem(ModItems.HOVER_BOOTS, "Hover Boots")
        
        // === APUs ===
        addItem(ModItems.APU_TIER1, "Accelerated Processing Unit (Tier 1)")
        addItem(ModItems.APU_TIER2, "Accelerated Processing Unit (Tier 2)")
        
        // === Component Bus ===
        addItem(ModItems.COMPONENT_BUS_TIER1, "Component Bus (Tier 1)")
        addItem(ModItems.COMPONENT_BUS_TIER2, "Component Bus (Tier 2)")
        addItem(ModItems.COMPONENT_BUS_TIER3, "Component Bus (Tier 3)")
        
        // === Servers ===
        addItem(ModItems.SERVER_TIER1, "Server (Tier 1)")
        addItem(ModItems.SERVER_TIER2, "Server (Tier 2)")
        addItem(ModItems.SERVER_TIER3, "Server (Tier 3)")
        addItem(ModItems.SERVER_CREATIVE, "Creative Server")
        addItem(ModItems.TERMINAL_SERVER, "Terminal Server")
        
        // === Tablet Cases ===
        addItem(ModItems.TABLET_CASE_TIER1, "Tablet Case (Tier 1)")
        addItem(ModItems.TABLET_CASE_TIER2, "Tablet Case (Tier 2)")
        addItem(ModItems.TABLET_CASE_CREATIVE, "Creative Tablet Case")
        
        // === Microcontroller Cases ===
        addItem(ModItems.MICROCONTROLLER_CASE_TIER1, "Microcontroller Case (Tier 1)")
        addItem(ModItems.MICROCONTROLLER_CASE_TIER2, "Microcontroller Case (Tier 2)")
        addItem(ModItems.MICROCONTROLLER_CASE_CREATIVE, "Creative Microcontroller Case")
        
        // === Drone Cases ===
        addItem(ModItems.DRONE_CASE_TIER1, "Drone Case (Tier 1)")
        addItem(ModItems.DRONE_CASE_TIER2, "Drone Case (Tier 2)")
        addItem(ModItems.DRONE_CASE_CREATIVE, "Creative Drone Case")
        
        // === Tooltips ===
        add("tooltip.${OpenComputers.MOD_ID}.tier", "Tier: %s")
        add("tooltip.${OpenComputers.MOD_ID}.energy", "Energy: %s / %s")
        add("tooltip.${OpenComputers.MOD_ID}.energy_rate", "Energy Rate: %s/t")
        add("tooltip.${OpenComputers.MOD_ID}.components", "Components: %s / %s")
        add("tooltip.${OpenComputers.MOD_ID}.slots", "Slots: %s")
        add("tooltip.${OpenComputers.MOD_ID}.capacity", "Capacity: %s")
        add("tooltip.${OpenComputers.MOD_ID}.address", "Address: %s")
        add("tooltip.${OpenComputers.MOD_ID}.hold_shift", "Hold Shift for details")
        add("tooltip.${OpenComputers.MOD_ID}.not_analyzed", "Use Analyzer to see details")
        
        // Component tooltips
        add("tooltip.${OpenComputers.MOD_ID}.cpu.architecture", "Architecture: Lua %s")
        add("tooltip.${OpenComputers.MOD_ID}.cpu.components", "Max Components: %s")
        add("tooltip.${OpenComputers.MOD_ID}.memory.amount", "Memory: %s bytes")
        add("tooltip.${OpenComputers.MOD_ID}.hdd.capacity", "Capacity: %s bytes")
        add("tooltip.${OpenComputers.MOD_ID}.eeprom.bios", "Contains: %s")
        add("tooltip.${OpenComputers.MOD_ID}.eeprom.empty", "Empty EEPROM")
        add("tooltip.${OpenComputers.MOD_ID}.floppy.label", "Label: %s")
        add("tooltip.${OpenComputers.MOD_ID}.floppy.unlabeled", "Unlabeled")
        add("tooltip.${OpenComputers.MOD_ID}.gpu.resolution", "Max Resolution: %s x %s")
        add("tooltip.${OpenComputers.MOD_ID}.gpu.depth", "Color Depth: %s")
        add("tooltip.${OpenComputers.MOD_ID}.screen.resolution", "Resolution: %s x %s")
        add("tooltip.${OpenComputers.MOD_ID}.wireless.range", "Range: %s blocks")
        add("tooltip.${OpenComputers.MOD_ID}.internet.enabled", "Internet access enabled")
        add("tooltip.${OpenComputers.MOD_ID}.upgrade.slot", "Upgrade (Tier %s)")
        add("tooltip.${OpenComputers.MOD_ID}.card.slot", "Card (Tier %s)")
        add("tooltip.${OpenComputers.MOD_ID}.container.slot", "Container (Tier %s)")
        
        // === Messages ===
        add("message.${OpenComputers.MOD_ID}.analyzer.address", "Address: %s")
        add("message.${OpenComputers.MOD_ID}.analyzer.component_name", "Component: %s")
        add("message.${OpenComputers.MOD_ID}.analyzer.energy", "Energy: %s / %s")
        add("message.${OpenComputers.MOD_ID}.analyzer.connected", "Connected nodes: %s")
        add("message.${OpenComputers.MOD_ID}.analyzer.usage", "Right-click to analyze")
        add("message.${OpenComputers.MOD_ID}.analyzer.clipboard", "Address copied to clipboard")
        
        add("message.${OpenComputers.MOD_ID}.not_enough_energy", "Not enough energy")
        add("message.${OpenComputers.MOD_ID}.machine_running", "Machine is already running")
        add("message.${OpenComputers.MOD_ID}.machine_stopped", "Machine is stopped")
        add("message.${OpenComputers.MOD_ID}.no_bootable_medium", "No bootable medium found")
        add("message.${OpenComputers.MOD_ID}.no_cpu", "No CPU installed")
        add("message.${OpenComputers.MOD_ID}.no_memory", "No memory installed")
        add("message.${OpenComputers.MOD_ID}.too_many_components", "Too many components connected")
        
        add("message.${OpenComputers.MOD_ID}.assembler.assemble", "Assemble")
        add("message.${OpenComputers.MOD_ID}.assembler.start", "Start assembly")
        add("message.${OpenComputers.MOD_ID}.assembler.progress", "Progress: %s%%")
        add("message.${OpenComputers.MOD_ID}.assembler.missing", "Missing components:")
        add("message.${OpenComputers.MOD_ID}.assembler.warning", "Warnings:")
        
        add("message.${OpenComputers.MOD_ID}.robot.inventory", "Robot Inventory")
        add("message.${OpenComputers.MOD_ID}.robot.components", "Robot Components")
        add("message.${OpenComputers.MOD_ID}.robot.selected_slot", "Selected slot: %s")
        
        add("message.${OpenComputers.MOD_ID}.nanomachines.activated", "Nanomachines activated")
        add("message.${OpenComputers.MOD_ID}.nanomachines.deactivated", "Nanomachines deactivated")
        add("message.${OpenComputers.MOD_ID}.nanomachines.reconfigured", "Nanomachines reconfigured")
        
        // === GUI ===
        add("gui.${OpenComputers.MOD_ID}.case", "Computer")
        add("gui.${OpenComputers.MOD_ID}.screen", "Screen")
        add("gui.${OpenComputers.MOD_ID}.disk_drive", "Disk Drive")
        add("gui.${OpenComputers.MOD_ID}.raid", "RAID")
        add("gui.${OpenComputers.MOD_ID}.assembler", "Assembler") 
        add("gui.${OpenComputers.MOD_ID}.disassembler", "Disassembler")
        add("gui.${OpenComputers.MOD_ID}.printer", "3D Printer")
        add("gui.${OpenComputers.MOD_ID}.rack", "Server Rack")
        add("gui.${OpenComputers.MOD_ID}.robot", "Robot")
        add("gui.${OpenComputers.MOD_ID}.drone", "Drone")
        add("gui.${OpenComputers.MOD_ID}.tablet", "Tablet")
        add("gui.${OpenComputers.MOD_ID}.server", "Server")
        add("gui.${OpenComputers.MOD_ID}.manual", "Manual")
        add("gui.${OpenComputers.MOD_ID}.terminal", "Terminal")
        
        add("gui.${OpenComputers.MOD_ID}.power", "Power")
        add("gui.${OpenComputers.MOD_ID}.power_on", "Power On")
        add("gui.${OpenComputers.MOD_ID}.power_off", "Power Off")
        add("gui.${OpenComputers.MOD_ID}.run", "Run")
        add("gui.${OpenComputers.MOD_ID}.stop", "Stop")
        add("gui.${OpenComputers.MOD_ID}.save", "Save")
        add("gui.${OpenComputers.MOD_ID}.load", "Load")
        
        // === Config ===
        add("config.${OpenComputers.MOD_ID}.title", "OpenComputers Configuration")
        add("config.${OpenComputers.MOD_ID}.category.general", "General")
        add("config.${OpenComputers.MOD_ID}.category.power", "Power")
        add("config.${OpenComputers.MOD_ID}.category.robot", "Robots")
        add("config.${OpenComputers.MOD_ID}.category.client", "Client")
        add("config.${OpenComputers.MOD_ID}.category.internet", "Internet")
        
        // === Commands ===
        add("command.${OpenComputers.MOD_ID}.usage", "OpenComputers commands")
        add("command.${OpenComputers.MOD_ID}.list.usage", "Lists all computers")
        add("command.${OpenComputers.MOD_ID}.debug.usage", "Debug computer at position")
        
        // === Death Messages ===
        add("death.attack.${OpenComputers.MOD_ID}.nanomachines", "%s was consumed by nanomachines")
        add("death.attack.${OpenComputers.MOD_ID}.robot", "%s was crushed by a robot")
        
        // === Subtitles ===
        add("subtitles.${OpenComputers.MOD_ID}.computer_running", "Computer running")
        add("subtitles.${OpenComputers.MOD_ID}.floppy_insert", "Floppy disk inserted")
        add("subtitles.${OpenComputers.MOD_ID}.floppy_eject", "Floppy disk ejected")
        add("subtitles.${OpenComputers.MOD_ID}.hdd_activity", "Hard drive activity")
        add("subtitles.${OpenComputers.MOD_ID}.robot_move", "Robot moving")
        add("subtitles.${OpenComputers.MOD_ID}.robot_swing", "Robot swinging")
        add("subtitles.${OpenComputers.MOD_ID}.robot_use", "Robot using item")
        
        // === Advancements ===
        add("advancement.${OpenComputers.MOD_ID}.root.title", "OpenComputers")
        add("advancement.${OpenComputers.MOD_ID}.root.description", "Build your first computer")
        add("advancement.${OpenComputers.MOD_ID}.first_boot.title", "Hello, World!")
        add("advancement.${OpenComputers.MOD_ID}.first_boot.description", "Successfully boot a computer")
        add("advancement.${OpenComputers.MOD_ID}.build_robot.title", "Rise of the Machines")
        add("advancement.${OpenComputers.MOD_ID}.build_robot.description", "Assemble your first robot")
        add("advancement.${OpenComputers.MOD_ID}.drone.title", "Eye in the Sky")
        add("advancement.${OpenComputers.MOD_ID}.drone.description", "Build a drone")
        add("advancement.${OpenComputers.MOD_ID}.internet.title", "Information Superhighway")
        add("advancement.${OpenComputers.MOD_ID}.internet.description", "Make an HTTP request")
        add("advancement.${OpenComputers.MOD_ID}.hacker.title", "1337 H4X0R")
        add("advancement.${OpenComputers.MOD_ID}.hacker.description", "Hack into another computer")
        
        // === JEI ===
        add("jei.${OpenComputers.MOD_ID}.assembling", "Assembling")
        add("jei.${OpenComputers.MOD_ID}.disassembling", "Disassembling")
        add("jei.${OpenComputers.MOD_ID}.printing", "3D Printing")
    }
}
