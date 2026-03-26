package li.cil.oc.server.machine

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.*

/**
 * Assembly system for creating complex devices like robots, drones, tablets.
 * Handles the assembly process, validation, and output generation.
 */
object AssemblyManager {
    
    /**
     * Validate an assembly configuration.
     * Returns a list of warnings/errors, or empty list if valid.
     */
    fun validate(inputs: AssemblyInputs): AssemblyResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check for required components based on template type
        when (inputs.template) {
            is RobotTemplate -> validateRobot(inputs, errors, warnings)
            is DroneTemplate -> validateDrone(inputs, errors, warnings)
            is TabletTemplate -> validateTablet(inputs, errors, warnings)
            is MicrocontrollerTemplate -> validateMicrocontroller(inputs, errors, warnings)
            is ServerTemplate -> validateServer(inputs, errors, warnings)
            null -> errors.add("No assembly template selected")
        }
        
        return AssemblyResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            requiredEnergy = calculateEnergyRequired(inputs),
            requiredTime = calculateTimeRequired(inputs)
        )
    }
    
    private fun validateRobot(inputs: AssemblyInputs, errors: MutableList<String>, warnings: MutableList<String>) {
        val template = inputs.template as RobotTemplate
        
        // Must have a CPU
        if (inputs.components.none { isCpu(it) }) {
            errors.add("Missing CPU")
        }
        
        // Must have memory
        if (inputs.components.none { isMemory(it) }) {
            errors.add("Missing memory")
        }
        
        // Check tier compatibility
        val cpuTier = inputs.components.filter { isCpu(it) }.maxOfOrNull { getTier(it) } ?: 0
        val caseTier = template.tier
        
        if (cpuTier > caseTier) {
            errors.add("CPU tier too high for case")
        }
        
        // Check slot usage
        val usedCardSlots = inputs.components.count { isCard(it) }
        val usedUpgradeSlots = inputs.components.count { isUpgrade(it) }
        val usedContainerSlots = inputs.components.count { isContainer(it) }
        
        if (usedCardSlots > template.cardSlots) {
            errors.add("Too many cards (max ${template.cardSlots})")
        }
        if (usedUpgradeSlots > template.upgradeSlots) {
            errors.add("Too many upgrades (max ${template.upgradeSlots})")
        }
        if (usedContainerSlots > template.containerSlots) {
            errors.add("Too many containers (max ${template.containerSlots})")
        }
        
        // Warnings
        if (inputs.components.none { isEeprom(it) }) {
            warnings.add("No EEPROM - will not boot")
        }
        
        if (inputs.components.none { isStorage(it) }) {
            warnings.add("No storage - limited functionality")
        }
    }
    
    private fun validateDrone(inputs: AssemblyInputs, errors: MutableList<String>, warnings: MutableList<String>) {
        val template = inputs.template as DroneTemplate
        
        // Drones require fewer components but still need basics
        if (inputs.components.none { isMemory(it) }) {
            errors.add("Missing memory")
        }
        
        if (inputs.components.none { isEeprom(it) }) {
            errors.add("Drones require an EEPROM")
        }
        
        val usedUpgradeSlots = inputs.components.count { isUpgrade(it) }
        if (usedUpgradeSlots > template.upgradeSlots) {
            errors.add("Too many upgrades (max ${template.upgradeSlots})")
        }
        
        // Check for flight-related upgrades
        if (inputs.components.none { isHoverUpgrade(it) }) {
            warnings.add("No hover upgrade - limited flight capability")
        }
    }
    
    private fun validateTablet(inputs: AssemblyInputs, errors: MutableList<String>, warnings: MutableList<String>) {
        val template = inputs.template as TabletTemplate
        
        if (inputs.components.none { isCpu(it) }) {
            errors.add("Missing CPU")
        }
        
        if (inputs.components.none { isMemory(it) }) {
            errors.add("Missing memory")
        }
        
        // Tablets must have a screen component built-in
        val cardCount = inputs.components.count { isCard(it) }
        val upgradeCount = inputs.components.count { isUpgrade(it) }
        
        if (cardCount > template.cardSlots) {
            errors.add("Too many cards (max ${template.cardSlots})")
        }
        
        if (upgradeCount > template.upgradeSlots) {
            errors.add("Too many upgrades (max ${template.upgradeSlots})")
        }
        
        if (inputs.components.none { isEeprom(it) }) {
            warnings.add("No EEPROM - will not boot")
        }
    }
    
    private fun validateMicrocontroller(inputs: AssemblyInputs, errors: MutableList<String>, warnings: MutableList<String>) {
        val template = inputs.template as MicrocontrollerTemplate
        
        // Microcontrollers are simpler
        if (inputs.components.none { isCpu(it) }) {
            errors.add("Missing CPU")
        }
        
        if (inputs.components.none { isMemory(it) }) {
            errors.add("Missing memory")
        }
        
        if (inputs.components.none { isEeprom(it) }) {
            errors.add("Microcontrollers require an EEPROM")
        }
        
        // Only limited cards/upgrades
        val cardCount = inputs.components.count { isCard(it) }
        if (cardCount > template.cardSlots) {
            errors.add("Too many cards (max ${template.cardSlots})")
        }
    }
    
    private fun validateServer(inputs: AssemblyInputs, errors: MutableList<String>, warnings: MutableList<String>) {
        val template = inputs.template as ServerTemplate
        
        if (inputs.components.none { isCpu(it) }) {
            errors.add("Missing CPU")
        }
        
        if (inputs.components.none { isMemory(it) }) {
            errors.add("Missing memory")
        }
        
        val componentBusCount = inputs.components.count { isComponentBus(it) }
        if (componentBusCount > template.componentBusSlots) {
            errors.add("Too many component buses (max ${template.componentBusSlots})")
        }
        
        if (inputs.components.none { isEeprom(it) }) {
            warnings.add("No EEPROM - will not boot")
        }
    }
    
    // === Component Type Checks ===
    
    private fun isCpu(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.CPU_TIER1.get() ||
               item == ModItems.CPU_TIER2.get() ||
               item == ModItems.CPU_TIER3.get() ||
               item == ModItems.APU_TIER1.get() ||
               item == ModItems.APU_TIER2.get()
    }
    
    private fun isMemory(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.RAM_TIER1.get() ||
               item == ModItems.RAM_TIER15.get() ||
               item == ModItems.RAM_TIER2.get() ||
               item == ModItems.RAM_TIER25.get() ||
               item == ModItems.RAM_TIER3.get() ||
               item == ModItems.RAM_TIER35.get()
    }
    
    private fun isEeprom(stack: ItemStack): Boolean {
        return stack.item == ModItems.EEPROM.get()
    }
    
    private fun isStorage(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.HDD_TIER1.get() ||
               item == ModItems.HDD_TIER2.get() ||
               item == ModItems.HDD_TIER3.get() ||
               item == ModItems.FLOPPY_DISK.get()
    }
    
    private fun isCard(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.GRAPHICS_CARD_TIER1.get() ||
               item == ModItems.GRAPHICS_CARD_TIER2.get() ||
               item == ModItems.GRAPHICS_CARD_TIER3.get() ||
               item == ModItems.NETWORK_CARD.get() ||
               item == ModItems.WIRELESS_CARD_TIER1.get() ||
               item == ModItems.WIRELESS_CARD_TIER2.get() ||
               item == ModItems.INTERNET_CARD.get() ||
               item == ModItems.REDSTONE_CARD_TIER1.get() ||
               item == ModItems.REDSTONE_CARD_TIER2.get() ||
               item == ModItems.DATA_CARD_TIER1.get() ||
               item == ModItems.DATA_CARD_TIER2.get() ||
               item == ModItems.DATA_CARD_TIER3.get() ||
               item == ModItems.WORLD_SENSOR_CARD.get() ||
               item == ModItems.LINKED_CARD.get()
    }
    
    private fun isUpgrade(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.ANGEL_UPGRADE.get() ||
               item == ModItems.BATTERY_UPGRADE_TIER1.get() ||
               item == ModItems.BATTERY_UPGRADE_TIER2.get() ||
               item == ModItems.BATTERY_UPGRADE_TIER3.get() ||
               item == ModItems.CRAFTING_UPGRADE.get() ||
               item == ModItems.CHUNKLOADER_UPGRADE.get() ||
               item == ModItems.DATABASE_UPGRADE_TIER1.get() ||
               item == ModItems.DATABASE_UPGRADE_TIER2.get() ||
               item == ModItems.DATABASE_UPGRADE_TIER3.get() ||
               item == ModItems.EXPERIENCE_UPGRADE.get() ||
               item == ModItems.GENERATOR_UPGRADE.get() ||
               item == ModItems.HOVER_UPGRADE_TIER1.get() ||
               item == ModItems.HOVER_UPGRADE_TIER2.get() ||
               item == ModItems.INVENTORY_UPGRADE.get() ||
               item == ModItems.INVENTORY_CONTROLLER_UPGRADE.get() ||
               item == ModItems.NAVIGATION_UPGRADE.get() ||
               item == ModItems.PISTON_UPGRADE.get() ||
               item == ModItems.SIGN_UPGRADE.get() ||
               item == ModItems.SOLAR_GENERATOR_UPGRADE.get() ||
               item == ModItems.TANK_UPGRADE.get() ||
               item == ModItems.TANK_CONTROLLER_UPGRADE.get() ||
               item == ModItems.TRACTOR_BEAM_UPGRADE.get() ||
               item == ModItems.TRADING_UPGRADE.get() ||
               item == ModItems.LEASH_UPGRADE.get()
    }
    
    private fun isContainer(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.HDD_TIER1.get() ||
               item == ModItems.HDD_TIER2.get() ||
               item == ModItems.HDD_TIER3.get() ||
               item == ModItems.FLOPPY_DISK.get()
    }
    
    private fun isHoverUpgrade(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.HOVER_UPGRADE_TIER1.get() ||
               item == ModItems.HOVER_UPGRADE_TIER2.get()
    }
    
    private fun isComponentBus(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.COMPONENT_BUS_TIER1.get() ||
               item == ModItems.COMPONENT_BUS_TIER2.get() ||
               item == ModItems.COMPONENT_BUS_TIER3.get()
    }
    
    private fun getTier(stack: ItemStack): Int {
        val item = stack.item
        return when (item) {
            ModItems.CPU_TIER1.get(), ModItems.RAM_TIER1.get(), ModItems.RAM_TIER15.get() -> 1
            ModItems.CPU_TIER2.get(), ModItems.RAM_TIER2.get(), ModItems.RAM_TIER25.get(), ModItems.APU_TIER1.get() -> 2
            ModItems.CPU_TIER3.get(), ModItems.RAM_TIER3.get(), ModItems.RAM_TIER35.get(), ModItems.APU_TIER2.get() -> 3
            else -> 1
        }
    }
    
    // === Energy & Time Calculations ===
    
    private fun calculateEnergyRequired(inputs: AssemblyInputs): Int {
        var energy = 10000 // Base cost
        
        // Add cost per component
        for (component in inputs.components) {
            energy += getComponentEnergyCost(component)
        }
        
        return energy
    }
    
    private fun getComponentEnergyCost(stack: ItemStack): Int {
        val tier = getTier(stack)
        return when {
            isCpu(stack) -> 1000 * tier
            isMemory(stack) -> 500 * tier
            isCard(stack) -> 750 * tier
            isUpgrade(stack) -> 500 * tier
            isStorage(stack) -> 400 * tier
            else -> 100
        }
    }
    
    private fun calculateTimeRequired(inputs: AssemblyInputs): Int {
        // Time in ticks (20 ticks = 1 second)
        var time = 200 // Base 10 seconds
        
        // Add time per component
        for (component in inputs.components) {
            time += 20 * getTier(component) // 1 second per tier level
        }
        
        return time
    }
    
    // === Assembly Execution ===
    
    /**
     * Perform the assembly and return the result item.
     */
    fun assemble(inputs: AssemblyInputs, level: Level): ItemStack? {
        val validation = validate(inputs)
        if (!validation.isValid) {
            return null
        }
        
        return when (inputs.template) {
            is RobotTemplate -> assembleRobot(inputs, inputs.template)
            is DroneTemplate -> assembleDrone(inputs, inputs.template)
            is TabletTemplate -> assembleTablet(inputs, inputs.template)
            is MicrocontrollerTemplate -> assembleMicrocontroller(inputs, inputs.template)
            is ServerTemplate -> assembleServer(inputs, inputs.template)
            else -> null
        }
    }
    
    private fun assembleRobot(inputs: AssemblyInputs, template: RobotTemplate): ItemStack {
        // Create robot block item with all component data
        val stack = ItemStack(li.cil.oc.common.init.ModBlocks.ROBOT.get())
        
        val tag = stack.getOrCreateTag()
        saveComponents(tag, inputs.components)
        tag.putInt("tier", template.tier)
        tag.putString("name", inputs.name ?: "Robot")
        
        return stack
    }
    
    private fun assembleDrone(inputs: AssemblyInputs, template: DroneTemplate): ItemStack {
        val baseTier = template.tier
        val item = when (baseTier) {
            1 -> ModItems.DRONE_CASE_TIER1.get()
            2 -> ModItems.DRONE_CASE_TIER2.get()
            else -> ModItems.DRONE_CASE_CREATIVE.get()
        }
        
        val stack = ItemStack(item)
        val tag = stack.getOrCreateTag()
        saveComponents(tag, inputs.components)
        tag.putInt("tier", baseTier)
        tag.putString("name", inputs.name ?: "Drone")
        tag.putBoolean("assembled", true)
        
        return stack
    }
    
    private fun assembleTablet(inputs: AssemblyInputs, template: TabletTemplate): ItemStack {
        val baseTier = template.tier
        val item = when (baseTier) {
            1 -> ModItems.TABLET_CASE_TIER1.get()
            2 -> ModItems.TABLET_CASE_TIER2.get()
            else -> ModItems.TABLET_CASE_CREATIVE.get()
        }
        
        val stack = ItemStack(item)
        val tag = stack.getOrCreateTag()
        saveComponents(tag, inputs.components)
        tag.putInt("tier", baseTier)
        tag.putString("name", inputs.name ?: "Tablet")
        tag.putBoolean("assembled", true)
        
        return stack
    }
    
    private fun assembleMicrocontroller(inputs: AssemblyInputs, template: MicrocontrollerTemplate): ItemStack {
        val stack = ItemStack(li.cil.oc.common.init.ModBlocks.MICROCONTROLLER.get())
        
        val tag = stack.getOrCreateTag()
        saveComponents(tag, inputs.components)
        tag.putInt("tier", template.tier)
        tag.putString("name", inputs.name ?: "Microcontroller")
        
        return stack
    }
    
    private fun assembleServer(inputs: AssemblyInputs, template: ServerTemplate): ItemStack {
        val baseTier = template.tier
        val item = when (baseTier) {
            1 -> ModItems.SERVER_TIER1.get()
            2 -> ModItems.SERVER_TIER2.get()
            3 -> ModItems.SERVER_TIER3.get()
            else -> ModItems.SERVER_CREATIVE.get()
        }
        
        val stack = ItemStack(item)
        val tag = stack.getOrCreateTag()
        saveComponents(tag, inputs.components)
        tag.putInt("tier", baseTier)
        tag.putString("name", inputs.name ?: "Server")
        
        return stack
    }
    
    private fun saveComponents(tag: CompoundTag, components: List<ItemStack>) {
        val list = ListTag()
        for ((index, component) in components.withIndex()) {
            if (!component.isEmpty) {
                val componentTag = CompoundTag()
                componentTag.putInt("slot", index)
                // component.save(componentTag)
                list.add(componentTag)
            }
        }
        tag.put("components", list)
    }
}

/**
 * Input configuration for assembly.
 */
data class AssemblyInputs(
    val template: AssemblyTemplate?,
    val components: List<ItemStack>,
    val name: String? = null
)

/**
 * Result of assembly validation.
 */
data class AssemblyResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val requiredEnergy: Int,
    val requiredTime: Int
)

/**
 * Base class for assembly templates.
 */
sealed class AssemblyTemplate {
    abstract val tier: Int
    abstract val cardSlots: Int
    abstract val upgradeSlots: Int
}

/**
 * Robot assembly template.
 */
data class RobotTemplate(
    override val tier: Int,
    override val cardSlots: Int = tier,
    override val upgradeSlots: Int = tier * 3,
    val containerSlots: Int = tier
) : AssemblyTemplate()

/**
 * Drone assembly template.
 */
data class DroneTemplate(
    override val tier: Int,
    override val cardSlots: Int = 0,
    override val upgradeSlots: Int = tier + 1
) : AssemblyTemplate()

/**
 * Tablet assembly template.
 */
data class TabletTemplate(
    override val tier: Int,
    override val cardSlots: Int = tier,
    override val upgradeSlots: Int = tier * 2
) : AssemblyTemplate()

/**
 * Microcontroller assembly template.
 */
data class MicrocontrollerTemplate(
    override val tier: Int,
    override val cardSlots: Int = tier,
    override val upgradeSlots: Int = 1
) : AssemblyTemplate()

/**
 * Server assembly template.
 */
data class ServerTemplate(
    override val tier: Int,
    override val cardSlots: Int = 3,
    override val upgradeSlots: Int = tier * 2,
    val componentBusSlots: Int = tier
) : AssemblyTemplate()

/**
 * Block entity for the assembler.
 */
class AssemblerState {
    var template: AssemblyTemplate? = null
    var progress = 0
    var maxProgress = 0
    var isAssembling = false
    var storedEnergy = 0
    val inputSlots = ItemStackHandler(24)
    val outputSlot = ItemStackHandler(1)
    
    fun startAssembly(inputs: AssemblyInputs): Boolean {
        val result = AssemblyManager.validate(inputs)
        if (!result.isValid) {
            return false
        }
        
        if (storedEnergy < result.requiredEnergy) {
            return false
        }
        
        template = inputs.template
        maxProgress = result.requiredTime
        progress = 0
        isAssembling = true
        storedEnergy -= result.requiredEnergy
        
        return true
    }
    
    fun tick(level: Level) {
        if (!isAssembling) return
        
        progress++
        
        if (progress >= maxProgress) {
            completeAssembly(level)
        }
    }
    
    private fun completeAssembly(level: Level) {
        val components = mutableListOf<ItemStack>()
        for (i in 0 until inputSlots.slots) {
            val stack = inputSlots.getStackInSlot(i)
            if (!stack.isEmpty) {
                components.add(stack.copy())
            }
        }
        
        val inputs = AssemblyInputs(template, components)
        val result = AssemblyManager.assemble(inputs, level)
        
        if (result != null) {
            outputSlot.setStackInSlot(0, result)
            
            // Clear input slots
            for (i in 0 until inputSlots.slots) {
                inputSlots.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
        
        isAssembling = false
        progress = 0
        maxProgress = 0
        template = null
    }
    
    fun save(tag: CompoundTag) {
        tag.putInt("progress", progress)
        tag.putInt("maxProgress", maxProgress)
        tag.putBoolean("isAssembling", isAssembling)
        tag.putInt("storedEnergy", storedEnergy)
        tag.put("inputs", inputSlots.serializeNBT(null))
        tag.put("output", outputSlot.serializeNBT(null))
    }
    
    fun load(tag: CompoundTag) {
        progress = tag.getInt("progress")
        maxProgress = tag.getInt("maxProgress")
        isAssembling = tag.getBoolean("isAssembling")
        storedEnergy = tag.getInt("storedEnergy")
        if (tag.contains("inputs")) {
            inputSlots.deserializeNBT(null, tag.getCompound("inputs"))
        }
        if (tag.contains("output")) {
            outputSlot.deserializeNBT(null, tag.getCompound("output"))
        }
    }
}

/**
 * Disassembler state for breaking down assembled items.
 */
class DisassemblerState {
    var progress = 0
    var maxProgress = 0
    var isDisassembling = false
    val inputSlot = ItemStackHandler(1)
    val outputSlots = ItemStackHandler(16)
    
    fun startDisassembly(): Boolean {
        val stack = inputSlot.getStackInSlot(0)
        if (stack.isEmpty) {
            return false
        }
        
        // Check if item can be disassembled
        val tag = stack.tag ?: return false
        if (!tag.contains("components")) {
            return false
        }
        
        maxProgress = 200 // 10 seconds
        progress = 0
        isDisassembling = true
        
        return true
    }
    
    fun tick() {
        if (!isDisassembling) return
        
        progress++
        
        if (progress >= maxProgress) {
            completeDisassembly()
        }
    }
    
    private fun completeDisassembly() {
        val stack = inputSlot.getStackInSlot(0)
        val tag = stack.tag
        
        if (tag != null && tag.contains("components")) {
            val componentsList = tag.getList("components", 10)
            var outputIndex = 0
            
            for (i in 0 until componentsList.size) {
                if (outputIndex >= outputSlots.slots) break
                
                val componentTag = componentsList.getCompound(i)
                // val componentStack = ItemStack.of(componentTag)
                // outputSlots.setStackInSlot(outputIndex++, componentStack)
            }
        }
        
        inputSlot.setStackInSlot(0, ItemStack.EMPTY)
        isDisassembling = false
        progress = 0
        maxProgress = 0
    }
    
    fun save(tag: CompoundTag) {
        tag.putInt("progress", progress)
        tag.putInt("maxProgress", maxProgress)
        tag.putBoolean("isDisassembling", isDisassembling)
        tag.put("input", inputSlot.serializeNBT(null))
        tag.put("outputs", outputSlots.serializeNBT(null))
    }
    
    fun load(tag: CompoundTag) {
        progress = tag.getInt("progress")
        maxProgress = tag.getInt("maxProgress")
        isDisassembling = tag.getBoolean("isDisassembling")
        if (tag.contains("input")) {
            inputSlot.deserializeNBT(null, tag.getCompound("input"))
        }
        if (tag.contains("outputs")) {
            outputSlots.deserializeNBT(null, tag.getCompound("outputs"))
        }
    }
}
