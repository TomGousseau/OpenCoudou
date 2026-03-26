package li.cil.oc.common.item.crafting

import li.cil.oc.common.Tier
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

/**
 * Transistor - basic electronic component
 */
class Transistor(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.transistor")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Microchip - tiered electronic component
 */
class Microchip(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
    }
}

/**
 * Arithmetic Logic Unit (ALU)
 */
class ALU(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.alu")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Control Unit (CU)
 */
class ControlUnit(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.control_unit")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Card Base - used for crafting cards
 */
class CardBase(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.card_base")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Printed Circuit Board (PCB)
 */
class PrintedCircuitBoard(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.pcb")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Raw Circuit Board - needs processing
 */
class RawCircuitBoard(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.raw_circuit_board")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Cutting Wire - for circuit board processing
 */
class CuttingWire(properties: Properties = Properties()) : Item(
    properties.stacksTo(1).durability(32)
) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.cutting_wire")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Disk Platter - for crafting hard drives
 */
class DiskPlatter(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.disk_platter")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Interweb - for internet cards
 */
class Interweb(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.interweb")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Chamelium - for 3D printing
 */
class Chamelium(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.chamelium")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Ink Cartridge - for 3D printing colors
 */
class InkCartridge(properties: Properties = Properties()) : Item(properties.stacksTo(1)) {
    companion object {
        const val MAX_INK = 100000
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        
        val tag = stack.tag
        val ink = tag?.getInt("ink") ?: MAX_INK
        val percentage = (ink * 100) / MAX_INK
        
        val color = when {
            percentage > 50 -> ChatFormatting.GREEN
            percentage > 20 -> ChatFormatting.YELLOW
            else -> ChatFormatting.RED
        }
        
        tooltip.add(Component.translatable("tooltip.opencomputers.ink_cartridge.level", percentage)
            .withStyle(color))
    }
}

/**
 * Empty Ink Cartridge
 */
class InkCartridgeEmpty(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.ink_cartridge_empty")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Acid - for circuit board processing
 */
class Acid(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.acid")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Numpad - for secure keyboard input
 */
class Numpad(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.numpad")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Button Group - for keyboard crafting
 */
class ButtonGroup(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.button_group")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Arrow Keys - for keyboard crafting
 */
class ArrowKeys(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.arrow_keys")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Wire - for cable and network component crafting
 */
class Wire(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.wire")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Drone motor - for drone crafting
 */
class DroneMotor(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.drone_motor")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Hover boots component
 */
class HoverBoots(properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    companion object {
        const val HOVER_HEIGHT = 1.5
        const val ENERGY_COST = 0.1
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.hover_boots")
            .withStyle(ChatFormatting.GRAY))
    }
}

/**
 * Abstract Base - for crafting tiered bases
 */
class AbstractBase(val tier: Tier, properties: Properties = Properties()) : Item(properties.stacksTo(64)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip.add(Component.translatable("tooltip.opencomputers.tier", tier.displayName)
            .withStyle(tier.color))
        tooltip.add(Component.translatable("tooltip.opencomputers.abstract_base")
            .withStyle(ChatFormatting.GRAY))
    }
}
