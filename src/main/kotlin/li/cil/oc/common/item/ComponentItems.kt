package li.cil.oc.common.item

import li.cil.oc.common.init.ModDataComponents
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import java.util.UUID

/**
 * Base class for tiered items in OpenComputers.
 * 
 * Most OC items come in multiple tiers, with higher tiers
 * having better capabilities but higher costs.
 */
abstract class TieredItem(
    properties: Properties,
    val tier: Int
) : Item(properties) {
    
    companion object {
        val TIER_NAMES = arrayOf("Tier 1", "Tier 2", "Tier 3", "Creative")
    }
    
    fun getTierName(): String = TIER_NAMES.getOrElse(tier - 1) { "Unknown" }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.literal(getTierName()).withStyle {
            it.withColor(getTierColor())
        })
        
        if (Screen.hasShiftDown()) {
            addDetailedTooltip(stack, context, tooltipComponents)
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.hold_shift")
                .withStyle { it.withColor(0x888888) })
        }
    }
    
    protected open fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        // Override in subclasses to add item-specific tooltips
    }
    
    private fun getTierColor(): Int = when (tier) {
        1 -> 0xB4B4B4 // Gray
        2 -> 0xFFFF55 // Yellow
        3 -> 0x55FFFF // Cyan
        4 -> 0xFF55FF // Magenta (Creative)
        else -> 0xFFFFFF
    }
}

/**
 * CPU item - determines computer speed and component count.
 */
class CPUItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        // Operations per tick by tier
        val TIER_SPEED = mapOf(
            1 to 1.0,    // 100%
            2 to 1.5,    // 150%
            3 to 2.0     // 200%
        )
        
        // Max components by tier
        val TIER_COMPONENTS = mapOf(
            1 to 8,
            2 to 12,
            3 to 16
        )
    }
    
    fun getSpeed(): Double = TIER_SPEED[tier] ?: 1.0
    fun getMaxComponents(): Int = TIER_COMPONENTS[tier] ?: 8
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.cpu.speed", 
            "${(getSpeed() * 100).toInt()}%"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.cpu.components",
            getMaxComponents()))
    }
}

/**
 * Memory (RAM) item - determines available memory.
 */
class MemoryItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        // Memory sizes in KB
        val TIER_SIZE = mapOf(
            1 to 192,    // 192 KB
            2 to 256,    // 256 KB
            3 to 384,    // 384 KB
            4 to 512,    // 512 KB
            5 to 768,    // 768 KB
            6 to 1024    // 1 MB
        )
    }
    
    // Map item tier to memory tier (some memory items are Tier 1.5, etc.)
    fun getMemorySize(): Int = TIER_SIZE[tier] ?: 192
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        val size = getMemorySize()
        val sizeStr = if (size >= 1024) "${size / 1024} MB" else "$size KB"
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.memory.size", sizeStr))
    }
}

/**
 * Hard Disk Drive item - provides persistent storage.
 */
class HDDItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        // Storage sizes in KB
        val TIER_SIZE = mapOf(
            1 to 1024,   // 1 MB
            2 to 2048,   // 2 MB
            3 to 4096    // 4 MB
        )
    }
    
    fun getCapacity(): Int = TIER_SIZE[tier] ?: 1024
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        val size = getCapacity()
        val sizeStr = if (size >= 1024) "${size / 1024} MB" else "$size KB"
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.hdd.capacity", sizeStr))
        
        // Show stored data label if present
        stack.get(ModDataComponents.LABEL.get())?.let { label ->
            if (label.isNotBlank()) {
                tooltipComponents.add(Component.literal("Label: $label")
                    .withStyle { it.withColor(0xAAAAAA) })
            }
        }
    }
}

/**
 * Floppy Disk item - removable storage.
 */
class FloppyDiskItem(properties: Properties) : Item(properties) {
    
    companion object {
        const val CAPACITY = 512 // 512 KB
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.floppy.capacity", "512 KB"))
        
        stack.get(ModDataComponents.LABEL.get())?.let { label ->
            if (label.isNotBlank()) {
                tooltipComponents.add(Component.literal("Label: $label")
                    .withStyle { it.withColor(0xAAAAAA) })
            }
        }
    }
    
    fun setLabel(stack: ItemStack, label: String) {
        stack.set(ModDataComponents.LABEL.get(), label.take(32))
    }
    
    fun getLabel(stack: ItemStack): String {
        return stack.get(ModDataComponents.LABEL.get()) ?: ""
    }
}

/**
 * EEPROM item - contains boot code or BIOS.
 */
class EEPROMItem(properties: Properties) : Item(properties) {
    
    companion object {
        const val CODE_SIZE = 4096  // 4 KB for code
        const val DATA_SIZE = 256   // 256 bytes for data
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        stack.get(ModDataComponents.LABEL.get())?.let { label ->
            if (label.isNotBlank()) {
                tooltipComponents.add(Component.literal(label)
                    .withStyle { it.withColor(0xFFFFFF) })
            }
        }
        
        val codeSize = stack.get(ModDataComponents.EEPROM_CODE.get())?.length ?: 0
        tooltipComponents.add(Component.literal("$codeSize / $CODE_SIZE bytes")
            .withStyle { it.withColor(0x888888) })
    }
    
    fun getCode(stack: ItemStack): String {
        return stack.get(ModDataComponents.EEPROM_CODE.get()) ?: ""
    }
    
    fun setCode(stack: ItemStack, code: String) {
        stack.set(ModDataComponents.EEPROM_CODE.get(), code.take(CODE_SIZE))
    }
    
    fun getData(stack: ItemStack): ByteArray {
        return stack.get(ModDataComponents.EEPROM_DATA.get()) ?: ByteArray(0)
    }
    
    fun setData(stack: ItemStack, data: ByteArray) {
        stack.set(ModDataComponents.EEPROM_DATA.get(), data.take(DATA_SIZE).toByteArray())
    }
}

/**
 * Graphics Card item - GPU for screen output.
 */
class GPUItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    companion object {
        // Max resolution by tier
        val TIER_RESOLUTION = mapOf(
            1 to Pair(50, 16),
            2 to Pair(80, 25),
            3 to Pair(160, 50)
        )
        
        // Color depth by tier
        val TIER_COLORS = mapOf(
            1 to 1,      // Monochrome
            2 to 16,     // 16 colors
            3 to 256     // 256 colors
        )
        
        // Operations per tick
        val TIER_SPEED = mapOf(
            1 to 1,
            2 to 4,
            3 to 8
        )
    }
    
    fun getMaxResolution(): Pair<Int, Int> = TIER_RESOLUTION[tier] ?: Pair(50, 16)
    fun getColorDepth(): Int = TIER_COLORS[tier] ?: 1
    fun getSpeed(): Int = TIER_SPEED[tier] ?: 1
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        val (w, h) = getMaxResolution()
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.gpu.resolution", "$w x $h"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.gpu.colors", getColorDepth()))
    }
}

/**
 * Network Card item - enables network communication.
 */
class NetworkCardItem(properties: Properties, tier: Int = 1) : TieredItem(properties, tier) {
    
    val isWireless: Boolean = tier == 2
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        if (isWireless) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.network.wireless"))
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.network.wired"))
        }
    }
}

/**
 * Internet Card item - enables HTTP requests and TCP connections.
 */
class InternetCardItem(properties: Properties) : Item(properties) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.internet.http"))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.internet.tcp"))
    }
}

/**
 * Redstone Card item - enhanced redstone control from software.
 */
class RedstoneCardItem(properties: Properties, tier: Int) : TieredItem(properties, tier) {
    
    val supportsBundled: Boolean = tier == 2
    
    override fun addDetailedTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>
    ) {
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.redstone.basic"))
        if (supportsBundled) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.redstone.bundled"))
        }
    }
}
