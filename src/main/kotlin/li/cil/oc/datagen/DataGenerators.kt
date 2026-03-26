package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.DataGenerator
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.recipes.*
import net.minecraft.data.tags.BlockTagsProvider
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider as NeoBlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.data.event.GatherDataEvent
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

/**
 * Data generation entry point.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object DataGenerators {
    
    @SubscribeEvent
    @JvmStatic
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val lookupProvider = event.lookupProvider
        val existingFileHelper = event.existingFileHelper
        
        // Block and item tags
        val blockTags = OCBlockTagsProvider(output, lookupProvider, existingFileHelper)
        generator.addProvider(event.includeServer(), blockTags)
        generator.addProvider(event.includeServer(), OCItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper))
        
        // Recipes
        generator.addProvider(event.includeServer(), OCRecipeProvider(output, lookupProvider))
        
        // Loot tables
        generator.addProvider(event.includeServer(), OCLootTableProvider(output, lookupProvider))
    }
}

/**
 * Custom tags for OpenComputers.
 */
object OCTags {
    object Blocks {
        val COMPUTER_BLOCKS = create("computer_blocks")
        val NETWORK_CABLES = create("network_cables")
        
        private fun create(name: String): TagKey<Block> {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name))
        }
    }
    
    object Items {
        val COMPONENTS = create("components")
        val CPUS = create("cpus")
        val MEMORY = create("memory")
        val STORAGE = create("storage")
        val CARDS = create("cards")
        val UPGRADES = create("upgrades")
        val CRAFTING_MATERIALS = create("crafting_materials")
        
        private fun create(name: String): TagKey<Item> {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name))
        }
    }
}

/**
 * Block tags provider.
 */
class OCBlockTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper?
) : NeoBlockTagsProvider(output, lookupProvider, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // Computer blocks (mineable with pickaxe)
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocks.CASE_TIER1.get())
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.CASE_CREATIVE.get())
            .add(ModBlocks.SCREEN_TIER1.get())
            .add(ModBlocks.SCREEN_TIER2.get())
            .add(ModBlocks.SCREEN_TIER3.get())
            .add(ModBlocks.KEYBOARD.get())
            .add(ModBlocks.CAPACITOR.get())
            .add(ModBlocks.POWER_CONVERTER.get())
            .add(ModBlocks.POWER_DISTRIBUTOR.get())
            .add(ModBlocks.CHARGER.get())
            .add(ModBlocks.DISK_DRIVE.get())
            .add(ModBlocks.RAID.get())
            .add(ModBlocks.RACK.get())
            .add(ModBlocks.RELAY.get())
            .add(ModBlocks.ACCESS_POINT.get())
            .add(ModBlocks.ADAPTER.get())
            .add(ModBlocks.TRANSPOSER.get())
            .add(ModBlocks.ASSEMBLER.get())
            .add(ModBlocks.DISASSEMBLER.get())
            .add(ModBlocks.GEOLYZER.get())
            .add(ModBlocks.MOTION_SENSOR.get())
            .add(ModBlocks.WAYPOINT.get())
            .add(ModBlocks.HOLOGRAM_TIER1.get())
            .add(ModBlocks.HOLOGRAM_TIER2.get())
            .add(ModBlocks.PRINTER.get())
            .add(ModBlocks.MICROCONTROLLER.get())
            .add(ModBlocks.REDSTONE_IO.get())
        
        // Need iron tool
        tag(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.RACK.get())
        
        // Cable blocks
        tag(OCTags.Blocks.NETWORK_CABLES)
            .add(ModBlocks.CABLE.get())
        
        // Computer blocks grouping
        tag(OCTags.Blocks.COMPUTER_BLOCKS)
            .add(ModBlocks.CASE_TIER1.get())
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.CASE_CREATIVE.get())
    }
}

/**
 * Item tags provider.
 */
class OCItemTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    blockTags: CompletableFuture<TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper?
) : ItemTagsProvider(output, lookupProvider, blockTags, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // CPUs
        tag(OCTags.Items.CPUS)
            .add(ModItems.CPU_TIER1.get())
            .add(ModItems.CPU_TIER2.get())
            .add(ModItems.CPU_TIER3.get())
        
        // Memory
        tag(OCTags.Items.MEMORY)
            .add(ModItems.MEMORY_TIER1.get())
            .add(ModItems.MEMORY_TIER15.get())
            .add(ModItems.MEMORY_TIER2.get())
            .add(ModItems.MEMORY_TIER25.get())
            .add(ModItems.MEMORY_TIER3.get())
            .add(ModItems.MEMORY_TIER35.get())
        
        // Storage
        tag(OCTags.Items.STORAGE)
            .add(ModItems.HDD_TIER1.get())
            .add(ModItems.HDD_TIER2.get())
            .add(ModItems.HDD_TIER3.get())
            .add(ModItems.FLOPPY_DISK.get())
            .add(ModItems.EEPROM.get())
        
        // Cards
        tag(OCTags.Items.CARDS)
            .add(ModItems.GPU_TIER1.get())
            .add(ModItems.GPU_TIER2.get())
            .add(ModItems.GPU_TIER3.get())
            .add(ModItems.NETWORK_CARD.get())
            .add(ModItems.INTERNET_CARD.get())
            .add(ModItems.REDSTONE_CARD_TIER1.get())
            .add(ModItems.REDSTONE_CARD_TIER2.get())
            .add(ModItems.LINKED_CARD.get())
            .add(ModItems.DATA_CARD_TIER1.get())
            .add(ModItems.DATA_CARD_TIER2.get())
            .add(ModItems.DATA_CARD_TIER3.get())
            .add(ModItems.WORLD_SENSOR_CARD.get())
        
        // Upgrades
        tag(OCTags.Items.UPGRADES)
            .add(ModItems.BATTERY_UPGRADE_TIER1.get())
            .add(ModItems.BATTERY_UPGRADE_TIER2.get())
            .add(ModItems.BATTERY_UPGRADE_TIER3.get())
            .add(ModItems.CHUNKLOADER_UPGRADE.get())
            .add(ModItems.CRAFTING_UPGRADE.get())
            .add(ModItems.DATABASE_UPGRADE_TIER1.get())
            .add(ModItems.DATABASE_UPGRADE_TIER2.get())
            .add(ModItems.DATABASE_UPGRADE_TIER3.get())
            .add(ModItems.EXPERIENCE_UPGRADE.get())
            .add(ModItems.GENERATOR_UPGRADE.get())
            .add(ModItems.HOVER_UPGRADE_TIER1.get())
            .add(ModItems.HOVER_UPGRADE_TIER2.get())
            .add(ModItems.INVENTORY_UPGRADE.get())
            .add(ModItems.INVENTORY_CONTROLLER_UPGRADE.get())
            .add(ModItems.LEASH_UPGRADE.get())
            .add(ModItems.NAVIGATION_UPGRADE.get())
            .add(ModItems.PISTON_UPGRADE.get())
            .add(ModItems.SIGN_IO_UPGRADE.get())
            .add(ModItems.SOLAR_GENERATOR_UPGRADE.get())
            .add(ModItems.TANK_UPGRADE.get())
            .add(ModItems.TANK_CONTROLLER_UPGRADE.get())
            .add(ModItems.TRADING_UPGRADE.get())
            .add(ModItems.TRACTOR_BEAM_UPGRADE.get())
            .add(ModItems.ANGEL_UPGRADE.get())
            .add(ModItems.MFU.get())
        
        // All components
        tag(OCTags.Items.COMPONENTS)
            .addTag(OCTags.Items.CPUS)
            .addTag(OCTags.Items.MEMORY)
            .addTag(OCTags.Items.STORAGE)
            .addTag(OCTags.Items.CARDS)
            .addTag(OCTags.Items.UPGRADES)
        
        // Crafting materials
        tag(OCTags.Items.CRAFTING_MATERIALS)
            .add(ModItems.CUTTING_WIRE.get())
            .add(ModItems.ACID.get())
            .add(ModItems.RAW_CIRCUIT_BOARD.get())
            .add(ModItems.CIRCUIT_BOARD.get())
            .add(ModItems.PRINTED_CIRCUIT_BOARD.get())
            .add(ModItems.CARD_BASE.get())
            .add(ModItems.TRANSISTOR.get())
            .add(ModItems.MICROCHIP_TIER1.get())
            .add(ModItems.MICROCHIP_TIER2.get())
            .add(ModItems.MICROCHIP_TIER3.get())
            .add(ModItems.ALU.get())
            .add(ModItems.CONTROL_UNIT.get())
            .add(ModItems.DISK_PLATTER.get())
            .add(ModItems.INTERWEB.get())
            .add(ModItems.BUTTON_GROUP.get())
            .add(ModItems.ARROW_KEYS.get())
            .add(ModItems.NUMPAD.get())
            .add(ModItems.CHAMELIUM.get())
            .add(ModItems.INK_CARTRIDGE.get())
    }
}

/**
 * Recipe provider.
 */
class OCRecipeProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>
) : RecipeProvider(output, lookupProvider) {
    
    override fun buildRecipes(output: RecipeOutput) {
        // ========================================
        // Crafting Materials
        // ========================================
        
        // Cutting Wire
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CUTTING_WIRE.get(), 8)
            .pattern("sss")
            .define('s', Items.STICK)
            .unlockedBy("has_stick", has(Items.STICK))
            .save(output)
        
        // Acid
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ACID.get())
            .requires(Items.WATER_BUCKET)
            .requires(Items.SLIME_BALL)
            .requires(Items.FERMENTED_SPIDER_EYE)
            .unlockedBy("has_slime", has(Items.SLIME_BALL))
            .save(output)
        
        // Raw Circuit Board
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAW_CIRCUIT_BOARD.get(), 8)
            .pattern("ccc")
            .pattern("ggg")
            .pattern("ccc")
            .define('c', Items.CLAY_BALL)
            .define('g', Tags.Items.DYES_GREEN)
            .unlockedBy("has_clay", has(Items.CLAY_BALL))
            .save(output)
        
        // Circuit Board (smelting raw circuit board)
        SimpleCookingRecipeBuilder.smelting(
            Ingredient.of(ModItems.RAW_CIRCUIT_BOARD.get()),
            RecipeCategory.MISC,
            ModItems.CIRCUIT_BOARD.get(),
            0.1f,
            200
        )
            .unlockedBy("has_raw_circuit_board", has(ModItems.RAW_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Printed Circuit Board
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PRINTED_CIRCUIT_BOARD.get())
            .requires(ModItems.CIRCUIT_BOARD.get())
            .requires(ModItems.ACID.get())
            .requires(Tags.Items.NUGGETS_GOLD)
            .unlockedBy("has_circuit_board", has(ModItems.CIRCUIT_BOARD.get()))
            .save(output)
        
        // Card Base
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CARD_BASE.get())
            .pattern("ii ")
            .pattern("ip ")
            .pattern("ig ")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('g', Tags.Items.NUGGETS_GOLD)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Transistor
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRANSISTOR.get(), 8)
            .pattern("iri")
            .pattern("grg")
            .pattern("iri")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', Tags.Items.NUGGETS_GOLD)
            .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
            .save(output)
        
        // Microchip Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER1.get(), 8)
            .pattern(" r ")
            .pattern("tpt")
            .pattern(" r ")
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('t', ModItems.TRANSISTOR.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_transistor", has(ModItems.TRANSISTOR.get()))
            .save(output)
        
        // Microchip Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER2.get(), 4)
            .pattern(" g ")
            .pattern("cmg")
            .pattern(" g ")
            .define('g', Tags.Items.NUGGETS_GOLD)
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('m', Items.GLOWSTONE_DUST)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Microchip Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER3.get(), 2)
            .pattern(" d ")
            .pattern("ncn")
            .pattern(" d ")
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('n', Items.NETHER_STAR)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // ALU
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ALU.get())
            .pattern("trt")
            .pattern("cpc")
            .pattern("trt")
            .define('t', ModItems.TRANSISTOR.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Control Unit
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTROL_UNIT.get())
            .pattern("gtg")
            .pattern("crc")
            .pattern("gtg")
            .define('g', Tags.Items.NUGGETS_GOLD)
            .define('t', ModItems.TRANSISTOR.get())
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .unlockedBy("has_chip", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Disk Platter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DISK_PLATTER.get(), 4)
            .pattern(" i ")
            .pattern("i i")
            .pattern(" i ")
            .define('i', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
            .save(output)
        
        // ========================================
        // CPUs
        // ========================================
        
        // CPU Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER1.get())
            .pattern("cac")
            .pattern("rpr")
            .pattern("cuc")
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('a', ModItems.ALU.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('u', ModItems.CONTROL_UNIT.get())
            .unlockedBy("has_alu", has(ModItems.ALU.get()))
            .save(output)
        
        // CPU Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER2.get())
            .pattern("cac")
            .pattern("rpr")
            .pattern("cuc")
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('a', ModItems.ALU.get())
            .define('r', Tags.Items.INGOTS_GOLD)
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('u', ModItems.CONTROL_UNIT.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // CPU Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER3.get())
            .pattern("cac")
            .pattern("dpe")
            .pattern("cuc")
            .define('c', ModItems.MICROCHIP_TIER3.get())
            .define('a', ModItems.ALU.get())
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('e', Tags.Items.GEMS_EMERALD)
            .define('u', ModItems.CONTROL_UNIT.get())
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // ========================================
        // Memory
        // ========================================
        
        // Memory Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEMORY_TIER1.get())
            .pattern("ccc")
            .pattern(" p ")
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Memory Tier 1.5
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEMORY_TIER15.get())
            .pattern("cc")
            .pattern("pp")
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.MICROCHIP_TIER2.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Memory Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEMORY_TIER2.get())
            .pattern("ccc")
            .pattern(" p ")
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // ========================================
        // Storage
        // ========================================
        
        // HDD Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HDD_TIER1.get())
            .pattern("cpc")
            .pattern("ddd")
            .pattern("ipi")
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('d', ModItems.DISK_PLATTER.get())
            .define('i', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_platter", has(ModItems.DISK_PLATTER.get()))
            .save(output)
        
        // HDD Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HDD_TIER2.get())
            .pattern("cpc")
            .pattern("ddd")
            .pattern("gpg")
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('d', ModItems.DISK_PLATTER.get())
            .define('g', Tags.Items.INGOTS_GOLD)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Floppy Disk
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLOPPY_DISK.get())
            .pattern("iii")
            .pattern("ipi")
            .pattern("idi")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('p', Items.PAPER)
            .define('d', ModItems.DISK_PLATTER.get())
            .unlockedBy("has_platter", has(ModItems.DISK_PLATTER.get()))
            .save(output)
        
        // EEPROM
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EEPROM.get())
            .pattern("gtg")
            .pattern("cpc")
            .pattern("gtg")
            .define('g', Tags.Items.NUGGETS_GOLD)
            .define('t', ModItems.TRANSISTOR.get())
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // ========================================
        // Graphics Cards
        // ========================================
        
        // GPU Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GPU_TIER1.get())
            .pattern("car")
            .pattern("cpr")
            .pattern("car")
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('a', ModItems.ALU.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('p', ModItems.CARD_BASE.get())
            .unlockedBy("has_card_base", has(ModItems.CARD_BASE.get()))
            .save(output)
        
        // GPU Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GPU_TIER2.get())
            .pattern("cag")
            .pattern("cpg")
            .pattern("cag")
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('a', ModItems.ALU.get())
            .define('g', Tags.Items.NUGGETS_GOLD)
            .define('p', ModItems.CARD_BASE.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // GPU Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GPU_TIER3.get())
            .pattern("cad")
            .pattern("cpd")
            .pattern("cad")
            .define('c', ModItems.MICROCHIP_TIER3.get())
            .define('a', ModItems.ALU.get())
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('p', ModItems.CARD_BASE.get())
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // ========================================
        // Cases
        // ========================================
        
        // Case Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER1.get())
            .pattern("ici")
            .pattern("cpc")
            .pattern("iri")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Case Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER2.get())
            .pattern("gcg")
            .pattern("cpc")
            .pattern("grg")
            .define('g', Tags.Items.INGOTS_GOLD)
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Case Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER3.get())
            .pattern("dcd")
            .pattern("cpe")
            .pattern("dcd")
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('c', ModItems.MICROCHIP_TIER3.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('e', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // ========================================
        // Screens
        // ========================================
        
        // Screen Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCREEN_TIER1.get())
            .pattern("igi")
            .pattern("tpt")
            .pattern("igi")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('g', Tags.Items.GLASS_BLOCKS)
            .define('t', ModItems.TRANSISTOR.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_transistor", has(ModItems.TRANSISTOR.get()))
            .save(output)
        
        // Screen Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCREEN_TIER2.get())
            .pattern("grg")
            .pattern("tpt")
            .pattern("gcg")
            .define('g', Tags.Items.GLASS_BLOCKS)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('t', ModItems.TRANSISTOR.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('c', ModItems.MICROCHIP_TIER2.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // ========================================  
        // Keyboard
        // ========================================
        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.KEYBOARD.get())
            .pattern("bna")
            .pattern("ppp")
            .define('b', ModItems.BUTTON_GROUP.get())
            .define('n', ModItems.NUMPAD.get())
            .define('a', ModItems.ARROW_KEYS.get())
            .define('p', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_buttons", has(ModItems.BUTTON_GROUP.get()))
            .save(output)
        
        // ========================================
        // Tools
        // ========================================
        
        // Analyzer
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.ANALYZER.get())
            .pattern(" rg")
            .pattern("rtr")
            .pattern("tr ")
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', Tags.Items.GLASS_BLOCKS)
            .define('t', ModItems.TRANSISTOR.get())
            .unlockedBy("has_transistor", has(ModItems.TRANSISTOR.get()))
            .save(output)
        
        // Wrench
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
            .pattern("i i")
            .pattern(" i ")
            .pattern(" i ")
            .define('i', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
            .save(output)
        
        // Manual
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANUAL.get())
            .pattern("b")
            .pattern("c")
            .define('b', Items.BOOK)
            .define('c', ModItems.MICROCHIP_TIER1.get())
            .unlockedBy("has_chip", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // ========================================
        // Cables
        // ========================================
        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CABLE.get(), 8)
            .pattern(" r ")
            .pattern("rir")
            .pattern(" r ")
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('i', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
            .save(output)
    }
}

/**
 * Loot table provider.
 */
class OCLootTableProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>
) : LootTableProvider(output, setOf(), listOf(
    SubProviderEntry(::OCBlockLoot, LootContextParamSets.BLOCK)
), lookupProvider)

/**
 * Block loot sub-provider.
 */
class OCBlockLoot(
    lookupProvider: HolderLookup.Provider
) : BlockLootSubProvider(setOf(), FeatureFlags.REGISTRY.allFlags(), lookupProvider) {
    
    override fun generate() {
        // All OC blocks drop themselves
        dropSelf(ModBlocks.CASE_TIER1.get())
        dropSelf(ModBlocks.CASE_TIER2.get())
        dropSelf(ModBlocks.CASE_TIER3.get())
        dropSelf(ModBlocks.CASE_CREATIVE.get())
        dropSelf(ModBlocks.SCREEN_TIER1.get())
        dropSelf(ModBlocks.SCREEN_TIER2.get())
        dropSelf(ModBlocks.SCREEN_TIER3.get())
        dropSelf(ModBlocks.KEYBOARD.get())
        dropSelf(ModBlocks.CABLE.get())
        dropSelf(ModBlocks.CAPACITOR.get())
        dropSelf(ModBlocks.POWER_CONVERTER.get())
        dropSelf(ModBlocks.POWER_DISTRIBUTOR.get())
        dropSelf(ModBlocks.CHARGER.get())
        dropSelf(ModBlocks.DISK_DRIVE.get())
        dropSelf(ModBlocks.RAID.get())
        dropSelf(ModBlocks.RACK.get())
        dropSelf(ModBlocks.RELAY.get())
        dropSelf(ModBlocks.ACCESS_POINT.get())
        dropSelf(ModBlocks.ADAPTER.get())
        dropSelf(ModBlocks.TRANSPOSER.get())
        dropSelf(ModBlocks.ASSEMBLER.get())
        dropSelf(ModBlocks.DISASSEMBLER.get())
        dropSelf(ModBlocks.GEOLYZER.get())
        dropSelf(ModBlocks.MOTION_SENSOR.get())
        dropSelf(ModBlocks.WAYPOINT.get())
        dropSelf(ModBlocks.HOLOGRAM_TIER1.get())
        dropSelf(ModBlocks.HOLOGRAM_TIER2.get())
        dropSelf(ModBlocks.PRINTER.get())
        dropSelf(ModBlocks.MICROCONTROLLER.get())
        dropSelf(ModBlocks.REDSTONE_IO.get())
    }
    
    override fun getKnownBlocks(): Iterable<Block> {
        return listOf(
            ModBlocks.CASE_TIER1.get(),
            ModBlocks.CASE_TIER2.get(),
            ModBlocks.CASE_TIER3.get(),
            ModBlocks.CASE_CREATIVE.get(),
            ModBlocks.SCREEN_TIER1.get(),
            ModBlocks.SCREEN_TIER2.get(),
            ModBlocks.SCREEN_TIER3.get(),
            ModBlocks.KEYBOARD.get(),
            ModBlocks.CABLE.get(),
            ModBlocks.CAPACITOR.get(),
            ModBlocks.POWER_CONVERTER.get(),
            ModBlocks.POWER_DISTRIBUTOR.get(),
            ModBlocks.CHARGER.get(),
            ModBlocks.DISK_DRIVE.get(),
            ModBlocks.RAID.get(),
            ModBlocks.RACK.get(),
            ModBlocks.RELAY.get(),
            ModBlocks.ACCESS_POINT.get(),
            ModBlocks.ADAPTER.get(),
            ModBlocks.TRANSPOSER.get(),
            ModBlocks.ASSEMBLER.get(),
            ModBlocks.DISASSEMBLER.get(),
            ModBlocks.GEOLYZER.get(),
            ModBlocks.MOTION_SENSOR.get(),
            ModBlocks.WAYPOINT.get(),
            ModBlocks.HOLOGRAM_TIER1.get(),
            ModBlocks.HOLOGRAM_TIER2.get(),
            ModBlocks.PRINTER.get(),
            ModBlocks.MICROCONTROLLER.get(),
            ModBlocks.REDSTONE_IO.get()
        )
    }
}
