package li.cil.oc.common.item

import li.cil.oc.common.init.ModDataComponents
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/**
 * Tablet item - portable computer.
 * 
 * Tablets are assembled with components and provide a mobile computing experience.
 */
class TabletItem(properties: Properties) : Item(properties) {
    
    companion object {
        const val MAX_ENERGY = 10000.0
        const val TIER_1_SLOTS = 16
        const val TIER_2_SLOTS = 24
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        val energy = stack.get(ModDataComponents.ENERGY.get()) ?: 0.0
        val maxEnergy = stack.get(ModDataComponents.MAX_ENERGY.get()) ?: MAX_ENERGY
        
        tooltipComponents.add(Component.literal("Energy: ${energy.toInt()} / ${maxEnergy.toInt()}")
            .withStyle { it.withColor(0x55FF55) })
        
        val isRunning = stack.get(ModDataComponents.RUNNING.get()) ?: false
        if (isRunning) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.tablet.running")
                .withStyle { it.withColor(0x55FF55) })
        }
        
        if (Screen.hasShiftDown()) {
            // Show installed components
            tooltipComponents.add(Component.literal(""))
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.tablet.components"))
        }
    }
    
    fun getEnergy(stack: ItemStack): Double = stack.get(ModDataComponents.ENERGY.get()) ?: 0.0
    
    fun setEnergy(stack: ItemStack, energy: Double) {
        val maxEnergy = stack.get(ModDataComponents.MAX_ENERGY.get()) ?: MAX_ENERGY
        stack.set(ModDataComponents.ENERGY.get(), energy.coerceIn(0.0, maxEnergy))
    }
    
    fun isRunning(stack: ItemStack): Boolean = stack.get(ModDataComponents.RUNNING.get()) ?: false
    
    fun setRunning(stack: ItemStack, running: Boolean) {
        stack.set(ModDataComponents.RUNNING.get(), running)
    }
}

/**
 * Server item - rack-mounted computer.
 */
class ServerItem(properties: Properties, val tier: Int) : Item(properties) {
    
    companion object {
        val TIER_COMPONENTS = mapOf(
            1 to 8,
            2 to 12,
            3 to 16,
            4 to 24 // Creative
        )
    }
    
    fun getMaxComponents(): Int = TIER_COMPONENTS[tier] ?: 8
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.literal("Tier $tier")
            .withStyle { it.withColor(getTierColor()) })
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.server.components",
            getMaxComponents()))
        
        if (Screen.hasShiftDown()) {
            // Show installed components
        }
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
 * Terminal Server item - provides remote terminals.
 */
class TerminalServerItem(properties: Properties) : Item(properties) {
    
    companion object {
        const val MAX_TERMINALS = 4
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.terminal_server.max",
            MAX_TERMINALS))
    }
}

/**
 * Remote Terminal item - connects to terminal servers.
 */
class RemoteTerminalItem(properties: Properties) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        val boundTo = stack.get(ModDataComponents.BOUND_ADDRESS.get())
        if (!boundTo.isNullOrEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.terminal.bound")
                .withStyle { it.withColor(0x55FF55) })
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.terminal.unbound")
                .withStyle { it.withColor(0xFF5555) })
        }
    }
    
    fun bind(stack: ItemStack, address: String) {
        stack.set(ModDataComponents.BOUND_ADDRESS.get(), address)
    }
    
    fun getBoundAddress(stack: ItemStack): String? {
        return stack.get(ModDataComponents.BOUND_ADDRESS.get())
    }
}

/**
 * APU (Accelerated Processing Unit) item - combined CPU+GPU.
 */
class APUItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        // Combines CPU and GPU specs
        val TIER_COMPONENTS = mapOf(
            1 to 6,
            2 to 8
        )
        
        val TIER_RESOLUTION = mapOf(
            1 to Pair(80, 25),
            2 to Pair(160, 50)
        )
    }
    
    fun getMaxComponents(): Int = TIER_COMPONENTS[tier] ?: 6
    fun getMaxResolution(): Pair<Int, Int> = TIER_RESOLUTION[tier] ?: Pair(80, 25)
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.apu.combined"))
        
        val (w, h) = getMaxResolution()
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.apu.resolution", "$w x $h"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.apu.components", getMaxComponents()))
    }
}

/**
 * Component Bus item - allows more components in rack servers.
 */
class ComponentBusItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        val TIER_COMPONENTS = mapOf(
            1 to 4,
            2 to 8,
            3 to 12
        )
    }
    
    fun getAdditionalComponents(): Int = TIER_COMPONENTS[tier] ?: 4
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.component_bus.slots",
            getAdditionalComponents()))
    }
}

/**
 * Linked Card item - quantum-entangled network card pair.
 */
class LinkedCardItem(properties: Properties) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.linked.desc"))
        
        val channel = stack.get(ModDataComponents.CHANNEL.get())
        if (!channel.isNullOrEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.linked.paired")
                .withStyle { it.withColor(0x55FF55) })
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.linked.unpaired")
                .withStyle { it.withColor(0xFF5555) })
        }
    }
}

/**
 * Debug Card item - creative-only debugging tool.
 */
class DebugCardItem(properties: Properties) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.debug.creative")
            .withStyle { it.withColor(0xFF55FF) })
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.debug.desc"))
    }
}

/**
 * Data Card item - provides hashing, encryption, etc.
 */
class DataCardItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        when (tier) {
            1 -> {
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.crc32"))
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.deflate"))
            }
            2 -> {
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.md5"))
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.sha256"))
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.aes"))
            }
            3 -> {
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.ecdh"))
                tooltipComponents.add(Component.translatable("tooltip.opencomputers.data.ecdsa"))
            }
        }
    }
}

/**
 * World Sensor Card item - reads various world data.
 */
class WorldSensorCardItem(properties: Properties) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.world_sensor.desc"))
    }
}
