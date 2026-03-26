package li.cil.oc.common.item.components

import li.cil.oc.common.Tier
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

/**
 * Hard drive items with tiered storage
 */
class HardDiskDrive(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    
    val capacity: Int = when (tier) {
        Tier.ONE -> 1024 * 1024 // 1 MB
        Tier.TWO -> 2 * 1024 * 1024 // 2 MB
        Tier.THREE -> 4 * 1024 * 1024 // 4 MB
        else -> 8 * 1024 * 1024 // 8 MB
    }
    
    val speed: Double = when (tier) {
        Tier.ONE -> 1.0
        Tier.TWO -> 1.5
        Tier.THREE -> 2.0
        else -> 4.0
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        val capacityMB = capacity / (1024 * 1024)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.hdd.capacity", capacityMB)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Floppy disk item
 */
class FloppyDisk(properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    
    val capacity: Int = 512 * 1024 // 512 KB
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.floppy.capacity", capacity / 1024)
            .withStyle(ChatFormatting.GRAY))
        
        // Show label if set
        val tag = stack.tag
        if (tag != null && tag.contains("oc:label")) {
            val label = tag.getString("oc:label")
            tooltip.add(Component.translatable("tooltip.opencomputers.floppy.label", label)
                .withStyle(ChatFormatting.AQUA))
        }
    }
}

/**
 * EEPROM item for boot firmware
 */
class EEPROM(properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    
    companion object {
        const val CODE_SIZE = 4096
        const val DATA_SIZE = 256
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        val tag = stack.tag
        if (tag != null) {
            if (tag.contains("oc:label")) {
                val label = tag.getString("oc:label")
                tooltip.add(Component.translatable("tooltip.opencomputers.eeprom.label", label)
                    .withStyle(ChatFormatting.AQUA))
            }
            
            if (tag.contains("oc:code")) {
                val codeSize = tag.getByteArray("oc:code").size
                tooltip.add(Component.translatable("tooltip.opencomputers.eeprom.code_used", codeSize, CODE_SIZE)
                    .withStyle(ChatFormatting.GRAY))
            }
        }
    }
}

/**
 * CPU items
 */
class CPU(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val componentSlots: Int = when (tier) {
        Tier.ONE -> 2
        Tier.TWO -> 3
        Tier.THREE -> 4
        else -> 99
    }
    
    val architectures: Set<String> = when (tier) {
        Tier.ONE -> setOf("Lua 5.2")
        Tier.TWO -> setOf("Lua 5.2", "Lua 5.3")
        Tier.THREE -> setOf("Lua 5.2", "Lua 5.3", "Lua 5.4")
        else -> setOf("Lua 5.2", "Lua 5.3", "Lua 5.4")
    }
    
    val callBudget: Int = when (tier) {
        Tier.ONE -> 0 // Direct calls only
        Tier.TWO -> 32
        Tier.THREE -> 64
        else -> 256
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.cpu.components", componentSlots)
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.translatable("tooltip.opencomputers.cpu.architectures", 
            architectures.joinToString(", ")).withStyle(ChatFormatting.GRAY))
    }
}

/**
 * RAM items
 */
class RAM(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val size: Int = when (tier) {
        Tier.ONE -> 192 * 1024 // 192 KB
        Tier.TWO -> 256 * 1024 // 256 KB
        Tier.THREE -> 384 * 1024 // 384 KB
        Tier.THREE_HALF -> 512 * 1024 // 512 KB
        Tier.FOUR -> 768 * 1024 // 768 KB
        Tier.FIVE -> 1024 * 1024 // 1 MB
        else -> 2048 * 1024 // 2 MB
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        val sizeKB = size / 1024
        val displaySize = if (sizeKB >= 1024) "${sizeKB / 1024} MB" else "$sizeKB KB"
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.ram.size", displaySize)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Component bus item for servers
 */
class ComponentBus(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val componentCount: Int = when (tier) {
        Tier.ONE -> 6
        Tier.TWO -> 9
        Tier.THREE -> 12
        else -> 99
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.component_bus.slots", componentCount)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * APU (combined CPU + GPU) item
 */
class APU(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val cpuTier: Tier = tier
    val gpuTier: Tier = when (tier) {
        Tier.ONE -> Tier.ONE
        Tier.TWO -> Tier.TWO
        else -> tier
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.apu.info")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Terminal server item
 */
class TerminalServer(properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    
    companion object {
        const val MAX_TERMINALS = 4
        const val RANGE = 16
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.terminal_server.max", MAX_TERMINALS)
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.translatable("tooltip.opencomputers.terminal_server.range", RANGE)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Server items for server racks
 */
class Server(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    
    val componentSlots: Int = when (tier) {
        Tier.ONE -> 2
        Tier.TWO -> 3
        Tier.THREE -> 4
        else -> 9
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.server.components", componentSlots)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Drone case item (for building drones)
 */
class DroneCase(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val componentSlots: Int = when (tier) {
        Tier.ONE -> 2
        Tier.TWO -> 3
        Tier.THREE -> 4
        else -> 9
    }
    
    val upgradeSlots: Int = when (tier) {
        Tier.ONE -> 1
        Tier.TWO -> 2
        Tier.THREE -> 3
        else -> 9
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.drone_case.components", componentSlots)
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.translatable("tooltip.opencomputers.drone_case.upgrades", upgradeSlots)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Microcontroller case item (for building microcontrollers)
 */
class MicrocontrollerCase(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val componentSlots: Int = when (tier) {
        Tier.ONE -> 1
        Tier.TWO -> 2
        else -> 2
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.microcontroller_case.components", componentSlots)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Tablet case item (for building tablets)
 */
class TabletCase(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    val componentSlots: Int = when (tier) {
        Tier.ONE -> 2
        Tier.TWO -> 3
        else -> 3
    }
    
    val upgradeSlots: Int = when (tier) {
        Tier.ONE -> 1
        Tier.TWO -> 2
        else -> 3
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.tablet_case.components", componentSlots)
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.translatable("tooltip.opencomputers.tablet_case.upgrades", upgradeSlots)
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Nanomachines item
 */
class Nanomachines(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    
    companion object {
        const val MAX_INPUTS = 18
        const val BASE_ENERGY_COST = 0.5
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        tooltip.add(Component.translatable("tooltip.opencomputers.nanomachines")
            .withStyle(ChatFormatting.GRAY))
    }
}
