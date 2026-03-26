package li.cil.oc.common.item

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/**
 * Base upgrade item class.
 */
abstract class UpgradeItem(
    properties: Properties,
    val tier: Int = 1
) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        if (tier > 1) {
            tooltipComponents.add(Component.literal("Tier $tier").withStyle {
                it.withColor(getTierColor())
            })
        }
        
        if (Screen.hasShiftDown()) {
            addDetailedTooltip(stack, context, tooltipComponents)
        }
    }
    
    protected open fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {}
    
    private fun getTierColor(): Int = when (tier) {
        1 -> 0xB4B4B4
        2 -> 0xFFFF55
        3 -> 0x55FFFF
        else -> 0xFFFFFF
    }
}

/**
 * Battery upgrade - increases robot/drone power storage.
 */
class BatteryUpgradeItem(properties: Properties, tier: Int) : UpgradeItem(properties, tier) {
    
    companion object {
        val TIER_CAPACITY = mapOf(
            1 to 10000.0,
            2 to 15000.0,
            3 to 20000.0
        )
    }
    
    fun getCapacity(): Double = TIER_CAPACITY[tier] ?: 10000.0
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.battery.capacity",
            getCapacity().toInt()))
    }
}

/**
 * Chunkloader upgrade - keeps chunks loaded around robots/drones.
 */
class ChunkloaderUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.chunkloader.desc"))
    }
}

/**
 * Crafting upgrade - enables robots to craft.
 */
class CraftingUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.crafting.desc"))
    }
}

/**
 * Database upgrade - stores analyzed block/entity data.
 */
class DatabaseUpgradeItem(properties: Properties, tier: Int) : UpgradeItem(properties, tier) {
    
    companion object {
        val TIER_ENTRIES = mapOf(
            1 to 9,
            2 to 25,
            3 to 81
        )
    }
    
    fun getMaxEntries(): Int = TIER_ENTRIES[tier] ?: 9
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.database.entries",
            getMaxEntries()))
    }
}

/**
 * Experience upgrade - robots gain experience from killing mobs.
 */
class ExperienceUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.experience.desc"))
    }
}

/**
 * Generator upgrade - robots generate power from fuel.
 */
class GeneratorUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val ENERGY_PER_FUEL = 15.0 // Energy per fuel item burn tick
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.generator.desc"))
    }
}

/**
 * Hover upgrade - allows robots to fly (limited height by tier).
 */
class HoverUpgradeItem(properties: Properties, tier: Int) : UpgradeItem(properties, tier) {
    
    companion object {
        val TIER_HEIGHT = mapOf(
            1 to 8,   // 8 blocks above ground
            2 to 256  // Unlimited
        )
    }
    
    fun getMaxHeight(): Int = TIER_HEIGHT[tier] ?: 8
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        val height = getMaxHeight()
        if (height >= 256) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.hover.unlimited"))
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.hover.limited", height))
        }
    }
}

/**
 * Inventory upgrade - increases robot inventory.
 */
class InventoryUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val SLOTS_PER_UPGRADE = 16
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.inventory.slots",
            SLOTS_PER_UPGRADE))
    }
}

/**
 * Inventory Controller upgrade - advanced inventory manipulation.
 */
class InventoryControllerUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.inventory_controller.desc"))
    }
}

/**
 * Leash upgrade - robots can lead animals.
 */
class LeashUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.leash.desc"))
    }
}

/**
 * Navigation upgrade - GPS-like positioning.
 */
class NavigationUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val RANGE = 120 // Blocks from map center
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.navigation.desc"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.navigation.range", RANGE))
    }
}

/**
 * Piston upgrade - robots can push blocks.
 */
class PistonUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.piston.desc"))
    }
}

/**
 * Sign I/O upgrade - robots can read/write signs.
 */
class SignIOUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.sign_io.desc"))
    }
}

/**
 * Solar Generator upgrade - generates power from sunlight.
 */
class SolarGeneratorUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val POWER_PER_TICK = 1.0
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.solar.desc"))
    }
}

/**
 * Tank upgrade - robots can store fluids.
 */
class TankUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val CAPACITY = 16000 // mB per upgrade
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.tank.capacity",
            CAPACITY))
    }
}

/**
 * Tank Controller upgrade - advanced fluid handling.
 */
class TankControllerUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.tank_controller.desc"))
    }
}

/**
 * Trading upgrade - enables robot trading with villagers.
 */
class TradingUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.trading.desc"))
    }
}

/**
 * Tractor Beam upgrade - picks up items remotely.
 */
class TractorBeamUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    companion object {
        const val RANGE = 3.0
    }
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.tractor_beam.desc"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.tractor_beam.range", RANGE))
    }
}

/**
 * Angel upgrade - robots can place blocks in mid-air.
 */
class AngelUpgradeItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.angel.desc"))
    }
}

/**
 * MFU (Multi-Function Upgrade) - adapter for external components.
 */
class MFUItem(properties: Properties) : UpgradeItem(properties) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.mfu.desc"))
    }
}
