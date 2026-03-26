package li.cil.oc.common.item

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/**
 * Material items used in crafting OC components.
 */

/**
 * Raw materials - basic crafting components.
 */
object MaterialItems {
    
    /**
     * Base item class for simple materials.
     */
    open class MaterialItem(properties: Properties) : Item(properties)
    
    /**
     * Cutting Wire - used in cutting processes.
     */
    class CuttingWireItem(properties: Properties) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.cutting_wire.desc"))
        }
    }
    
    /**
     * Acid - used in circuit production.
     */
    class AcidItem(properties: Properties) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.acid.desc"))
        }
    }
    
    /**
     * Raw Circuit Board - unprocessed PCB.
     */
    class RawCircuitBoardItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Circuit Board - processed, ready for components.
     */
    class CircuitBoardItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Printed Circuit Board (PCB) - complete board with traces.
     */
    class PrintedCircuitBoardItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Card base - foundation for expansion cards.
     */
    class CardBaseItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Transistor - basic electronic component.
     */
    class TransistorItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Microchip - tiered processing unit.
     */
    class MicrochipItem(properties: Properties, val tier: Int) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.literal("Tier $tier")
                .withStyle { it.withColor(getTierColor()) })
        }
        
        private fun getTierColor(): Int = when (tier) {
            1 -> 0xB4B4B4
            2 -> 0xFFFF55
            3 -> 0x55FFFF
            else -> 0xFFFFFF
        }
    }
    
    /**
     * ALU - Arithmetic Logic Unit.
     */
    class ALUItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Control Unit - CPU component.
     */
    class ControlUnitItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Disk Platter - storage medium base.
     */
    class DiskPlatterItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Interweb - used in Internet Card crafting.
     */
    class InterwebItem(properties: Properties) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.interweb.desc")
                .withStyle { it.withColor(0x888888) })
        }
    }
    
    /**
     * Button Group - for keyboard crafting.
     */
    class ButtonGroupItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Arrow Keys - keyboard component.
     */
    class ArrowKeysItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Numpad - keyboard component.
     */
    class NumpadItem(properties: Properties) : MaterialItem(properties)
    
    /**
     * Chamelium - 3D printer material.
     */
    class ChameliumItem(properties: Properties) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.chamelium.desc"))
        }
    }
    
    /**
     * Ink Cartridge - 3D printer consumable.
     */
    class InkCartridgeItem(properties: Properties) : MaterialItem(properties) {
        companion object {
            val INK_COLORS = listOf("Black", "Cyan", "Magenta", "Yellow", "Color")
        }
        
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.ink.desc"))
        }
    }
    
    /**
     * Drone Case - for drone assembly.
     */
    class DroneCaseItem(properties: Properties, val tier: Int) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.literal("Tier $tier")
                .withStyle { it.withColor(getTierColor()) })
        }
        
        private fun getTierColor(): Int = when (tier) {
            1 -> 0xB4B4B4
            2 -> 0xFFFF55
            3 -> 0x55FFFF
            4 -> 0xFF55FF
            else -> 0xFFFFFF
        }
    }
    
    /**
     * Microcontroller Case - for microcontroller assembly.
     */
    class MicrocontrollerCaseItem(properties: Properties, val tier: Int) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.literal("Tier $tier")
                .withStyle { it.withColor(getTierColor()) })
        }
        
        private fun getTierColor(): Int = when (tier) {
            1 -> 0xB4B4B4
            2 -> 0xFFFF55
            else -> 0xFFFFFF
        }
    }
    
    /**
     * Tablet Case - for tablet assembly.
     */
    class TabletCaseItem(properties: Properties, val tier: Int) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.literal("Tier $tier")
                .withStyle { it.withColor(getTierColor()) })
        }
        
        private fun getTierColor(): Int = when (tier) {
            1 -> 0xB4B4B4
            2 -> 0xFFFF55
            3 -> 0x55FFFF
            4 -> 0xFF55FF
            else -> 0xFFFFFF
        }
    }
    
    /**
     * Nanomachines - medical/augmentation item.
     */
    class NanomachinesItem(properties: Properties) : MaterialItem(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.nanomachines.desc"))
        }
    }
}

/**
 * Tool items.
 */
object ToolItems {
    
    /**
     * Analyzer - right-click OC blocks to see component info.
     */
    class AnalyzerItem(properties: Properties) : Item(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.analyzer.desc"))
        }
    }
    
    /**
     * Scrench (OC Wrench) - rotate/configure blocks.
     */
    class WrenchItem(properties: Properties) : Item(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.wrench.rotate"))
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.wrench.configure"))
        }
    }
    
    /**
     * Manual - in-game documentation.
     */
    class ManualItem(properties: Properties) : Item(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.manual.desc"))
        }
    }
    
    /**
     * Hover Boots - player flight item.
     */
    class HoverBootsItem(properties: Properties) : Item(properties) {
        override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltipComponents: MutableList<Component>,
            tooltipFlag: TooltipFlag
        ) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.hover_boots.desc"))
        }
    }
}
