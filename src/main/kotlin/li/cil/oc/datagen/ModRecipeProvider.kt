package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.CompletableFuture

/**
 * Recipe provider for OpenComputers.
 * Generates all crafting recipes for blocks and items.
 */
class ModRecipeProvider(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>
) : RecipeProvider(output, registries) {
    
    override fun buildRecipes(output: RecipeOutput) {
        // === Materials ===
        buildMaterialRecipes(output)
        
        // === Components ===
        buildComponentRecipes(output)
        
        // === Cards ===
        buildCardRecipes(output)
        
        // === Upgrades ===
        buildUpgradeRecipes(output)
        
        // === Computer Cases ===
        buildCaseRecipes(output)
        
        // === Screens ===
        buildScreenRecipes(output)
        
        // === Network Equipment ===
        buildNetworkRecipes(output)
        
        // === Power ===
        buildPowerRecipes(output)
        
        // === Storage ===
        buildStorageRecipes(output)
        
        // === Misc ===
        buildMiscRecipes(output)
        
        // === Robot ===
        buildRobotRecipes(output)
        
        // === Tablet ===
        buildTabletRecipes(output)
        
        // === Microcontroller ===
        buildMicrocontrollerRecipes(output)
        
        // === Server ===
        buildServerRecipes(output)
        
        // === Drone ===
        buildDroneRecipes(output)
    }
    
    private fun buildMaterialRecipes(output: RecipeOutput) {
        // Raw materials
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAW_CIRCUIT_BOARD.get(), 8)
            .pattern("GCG")
            .pattern("CDC")
            .pattern("GCG")
            .define('G', Items.GOLD_NUGGET)
            .define('C', Items.CLAY_BALL)
            .define('D', Items.GREEN_DYE)
            .unlockedBy("has_gold", has(Items.GOLD_NUGGET))
            .save(output)
        
        // Circuit boards (smelting)
        SimpleCookingRecipeBuilder.smelting(
            Ingredient.of(ModItems.RAW_CIRCUIT_BOARD.get()),
            RecipeCategory.MISC,
            ModItems.CIRCUIT_BOARD.get(),
            0.35f,
            200
        ).unlockedBy("has_raw_board", has(ModItems.RAW_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Printed circuit boards
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PRINTED_CIRCUIT_BOARD.get(), 1)
            .pattern(" G ")
            .pattern("GCG")
            .pattern(" G ")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.CIRCUIT_BOARD.get())
            .unlockedBy("has_board", has(ModItems.CIRCUIT_BOARD.get()))
            .save(output)
        
        // Transistor
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRANSISTOR.get(), 8)
            .pattern("IRI")
            .pattern("GNG")
            .pattern(" R ")
            .define('I', Items.IRON_NUGGET)
            .define('R', Items.REDSTONE)
            .define('G', Items.GOLD_NUGGET)
            .define('N', Items.QUARTZ)
            .unlockedBy("has_redstone", has(Items.REDSTONE))
            .save(output)
        
        // Microchip Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER1.get(), 8)
            .pattern("IRI")
            .pattern("TCT")
            .pattern("IRI")
            .define('I', Items.IRON_NUGGET)
            .define('R', Items.REDSTONE)
            .define('T', ModItems.TRANSISTOR.get())
            .define('C', ModItems.CIRCUIT_BOARD.get())
            .unlockedBy("has_transistor", has(ModItems.TRANSISTOR.get()))
            .save(output)
        
        // Microchip Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER2.get(), 4)
            .pattern("IGI")
            .pattern("TCT")
            .pattern("IGI")
            .define('I', Items.IRON_NUGGET)
            .define('G', Items.GOLD_NUGGET)
            .define('T', ModItems.TRANSISTOR.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Microchip Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP_TIER3.get(), 2)
            .pattern("IGI")
            .pattern("TCT")
            .pattern("IDI")
            .define('I', Items.IRON_NUGGET)
            .define('G', Items.GOLD_INGOT)
            .define('D', Items.DIAMOND)
            .define('T', ModItems.TRANSISTOR.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Arithmetic Logic Unit
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ALU.get(), 1)
            .pattern("TCT")
            .pattern("CPC")
            .pattern("TCT")
            .define('T', ModItems.TRANSISTOR.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Control Unit
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTROL_UNIT.get(), 1)
            .pattern("TRT")
            .pattern("CAC")
            .pattern("TPT")
            .define('T', ModItems.TRANSISTOR.get())
            .define('R', Items.REDSTONE)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('A', ModItems.ALU.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_alu", has(ModItems.ALU.get()))
            .save(output)
        
        // Disk platter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DISK_PLATTER.get(), 4)
            .pattern(" I ")
            .pattern("I I")
            .pattern(" I ")
            .define('I', Items.IRON_INGOT)
            .unlockedBy("has_iron", has(Items.IRON_INGOT))
            .save(output)
        
        // Button group
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BUTTON_GROUP.get(), 1)
            .pattern("BBB")
            .pattern("BPB")
            .pattern("BBB")
            .define('B', Items.STONE_BUTTON)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Arrow keys
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARROW_KEYS.get(), 1)
            .pattern(" B ")
            .pattern("BPB")
            .pattern(" B ")
            .define('B', Items.STONE_BUTTON)
            .define('P', ModItems.BUTTON_GROUP.get())
            .unlockedBy("has_buttons", has(ModItems.BUTTON_GROUP.get()))
            .save(output)
        
        // Number pad
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NUMPAD.get(), 1)
            .pattern("BBB")
            .pattern("BPB")
            .pattern("BBB")
            .define('B', Items.STONE_BUTTON)
            .define('P', ModItems.BUTTON_GROUP.get())
            .unlockedBy("has_buttons", has(ModItems.BUTTON_GROUP.get()))
            .save(output)
        
        // Card base
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CARD_BASE.get(), 1)
            .pattern(" IG")
            .pattern("IPC")
            .pattern(" IG")
            .define('I', Items.IRON_NUGGET)
            .define('G', Items.GOLD_NUGGET)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Interweb
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INTERWEB.get(), 1)
            .pattern("SES")
            .pattern("SES")
            .pattern("SES")
            .define('S', Items.STRING)
            .define('E', Items.ENDER_PEARL)
            .unlockedBy("has_pearl", has(Items.ENDER_PEARL))
            .save(output)
        
        // Wire
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CUTTING_WIRE.get(), 8)
            .pattern("I I")
            .pattern(" I ")
            .pattern("I I")
            .define('I', Items.IRON_NUGGET)
            .unlockedBy("has_iron", has(Items.IRON_NUGGET))
            .save(output)
        
        // Acid
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ACID.get(), 4)
            .pattern(" S ")
            .pattern("SWS")
            .pattern(" S ")
            .define('S', Items.SUGAR)
            .define('W', Items.WATER_BUCKET)
            .unlockedBy("has_sugar", has(Items.SUGAR))
            .save(output)
        
        // Chamelium
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHAMELIUM.get(), 8)
            .pattern("RGR")
            .pattern("GCG")
            .pattern("RGR")
            .define('R', Items.REDSTONE)
            .define('G', Items.GRAVEL)
            .define('C', Items.COAL)
            .unlockedBy("has_redstone", has(Items.REDSTONE))
            .save(output)
        
        // Ink cartridge  
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INK_CARTRIDGE.get(), 1)
            .pattern("III")
            .pattern("DDD")
            .pattern("III")
            .define('I', Items.IRON_NUGGET)
            .define('D', Items.INK_SAC)
            .unlockedBy("has_ink", has(Items.INK_SAC))
            .save(output)
        
        // Color cartridge
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INK_CARTRIDGE_COLOR.get(), 1)
            .pattern("RGB")
            .pattern("III")
            .pattern("NNN")
            .define('R', Items.RED_DYE)
            .define('G', Items.GREEN_DYE)
            .define('B', Items.BLUE_DYE)
            .define('I', Items.IRON_NUGGET)
            .define('N', Items.IRON_NUGGET)
            .unlockedBy("has_dyes", has(Items.RED_DYE))
            .save(output)
    }
    
    private fun buildComponentRecipes(output: RecipeOutput) {
        // CPU Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER1.get(), 1)
            .pattern("ICI")
            .pattern("CAC")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('A', ModItems.CONTROL_UNIT.get())
            .unlockedBy("has_cu", has(ModItems.CONTROL_UNIT.get()))
            .save(output)
        
        // CPU Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER2.get(), 1)
            .pattern("ICI")
            .pattern("CPC")
            .pattern("ICI")
            .define('I', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.CPU_TIER1.get())
            .unlockedBy("has_cpu1", has(ModItems.CPU_TIER1.get()))
            .save(output)
        
        // CPU Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CPU_TIER3.get(), 1)
            .pattern("IDI")
            .pattern("CPC")
            .pattern("IDI")
            .define('D', Items.DIAMOND)
            .define('I', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.CPU_TIER2.get())
            .unlockedBy("has_cpu2", has(ModItems.CPU_TIER2.get()))
            .save(output)
        
        // RAM Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER1.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // RAM Tier 1.5
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER15.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("CCC")
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // RAM Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER2.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // RAM Tier 2.5
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER25.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("CCC")
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // RAM Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER3.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // RAM Tier 3.5
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RAM_TIER35.get(), 1)
            .pattern("CCC")
            .pattern("PRP")
            .pattern("CCC")
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE)
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // EEPROM
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EEPROM.get(), 1)
            .pattern("TGT")
            .pattern("CPC")
            .pattern("TGT")
            .define('T', ModItems.TRANSISTOR.get())
            .define('G', Items.GOLD_NUGGET)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', Items.PAPER)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
    }
    
    private fun buildCardRecipes(output: RecipeOutput) {
        // Graphics Card Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRAPHICS_CARD_TIER1.get(), 1)
            .pattern("CRA")
            .pattern("BPB")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('R', Items.REDSTONE)
            .define('A', ModItems.ALU.get())
            .define('B', ModItems.CARD_BASE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_card_base", has(ModItems.CARD_BASE.get()))
            .save(output)
        
        // Graphics Card Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRAPHICS_CARD_TIER2.get(), 1)
            .pattern("CAG")
            .pattern("BPR")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('A', ModItems.ALU.get())
            .define('G', ModItems.GRAPHICS_CARD_TIER1.get())
            .define('B', ModItems.CARD_BASE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('R', Items.REDSTONE_BLOCK)
            .unlockedBy("has_gpu1", has(ModItems.GRAPHICS_CARD_TIER1.get()))
            .save(output)
        
        // Graphics Card Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRAPHICS_CARD_TIER3.get(), 1)
            .pattern("CAG")
            .pattern("BPD")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('A', ModItems.ALU.get())
            .define('G', ModItems.GRAPHICS_CARD_TIER2.get())
            .define('B', ModItems.CARD_BASE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('D', Items.DIAMOND)
            .unlockedBy("has_gpu2", has(ModItems.GRAPHICS_CARD_TIER2.get()))
            .save(output)
        
        // Network Card
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETWORK_CARD.get(), 1)
            .pattern("  C")
            .pattern("BPW")
            .pattern("   ")
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('B', ModItems.CARD_BASE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('W', ModItems.CABLE.get())
            .unlockedBy("has_card_base", has(ModItems.CARD_BASE.get()))
            .save(output)
        
        // Wireless Network Card Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WIRELESS_CARD_TIER1.get(), 1)
            .pattern("  A")
            .pattern("CPE")
            .pattern("   ")
            .define('A', Items.IRON_BARS)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('E', Items.ENDER_PEARL)
            .unlockedBy("has_pearl", has(Items.ENDER_PEARL))
            .save(output)
        
        // Wireless Network Card Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WIRELESS_CARD_TIER2.get(), 1)
            .pattern("  A")
            .pattern("CPE")
            .pattern("  W")
            .define('A', Items.IRON_BARS)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('E', Items.ENDER_EYE)
            .define('W', ModItems.WIRELESS_CARD_TIER1.get())
            .unlockedBy("has_wireless1", has(ModItems.WIRELESS_CARD_TIER1.get()))
            .save(output)
        
        // Internet Card
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INTERNET_CARD.get(), 1)
            .pattern(" WA")
            .pattern("CPE")
            .pattern("   ")
            .define('A', Items.IRON_BARS)
            .define('W', ModItems.INTERWEB.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('E', Blocks.OBSIDIAN)
            .unlockedBy("has_interweb", has(ModItems.INTERWEB.get()))
            .save(output)
        
        // Redstone Card Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REDSTONE_CARD_TIER1.get(), 1)
            .pattern("RCR")
            .pattern("BPT")
            .pattern("   ")
            .define('R', Items.REDSTONE)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('B', ModItems.CARD_BASE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('T', Items.REDSTONE_TORCH)
            .unlockedBy("has_card_base", has(ModItems.CARD_BASE.get()))
            .save(output)
        
        // Redstone Card Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REDSTONE_CARD_TIER2.get(), 1)
            .pattern("RCR")
            .pattern("BPT")
            .pattern("RCR")
            .define('R', Items.REDSTONE_BLOCK)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('B', ModItems.REDSTONE_CARD_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('T', Items.COMPARATOR)
            .unlockedBy("has_redstone1", has(ModItems.REDSTONE_CARD_TIER1.get()))
            .save(output)
        
        // Data Card Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_CARD_TIER1.get(), 1)
            .pattern("  A")
            .pattern("CPB")
            .pattern("   ")
            .define('A', ModItems.ALU.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('B', ModItems.CARD_BASE.get())
            .unlockedBy("has_card_base", has(ModItems.CARD_BASE.get()))
            .save(output)
        
        // Data Card Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_CARD_TIER2.get(), 1)
            .pattern("  A")
            .pattern("CPD")
            .pattern("   ")
            .define('A', ModItems.ALU.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.DATA_CARD_TIER1.get())
            .define('D', Items.DIAMOND)
            .unlockedBy("has_data1", has(ModItems.DATA_CARD_TIER1.get()))
            .save(output)
        
        // Data Card Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_CARD_TIER3.get(), 1)
            .pattern(" EA")
            .pattern("CPD")
            .pattern("   ")
            .define('E', Items.EMERALD)
            .define('A', ModItems.ALU.get())
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.DATA_CARD_TIER2.get())
            .define('D', Items.DIAMOND)
            .unlockedBy("has_data2", has(ModItems.DATA_CARD_TIER2.get()))
            .save(output)
    }
    
    private fun buildUpgradeRecipes(output: RecipeOutput) {
        // Angel Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ANGEL_UPGRADE.get(), 1)
            .pattern("IEI")
            .pattern("CPC")
            .pattern("IFI")
            .define('I', Items.IRON_INGOT)
            .define('E', Items.ENDER_PEARL)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('F', Items.FEATHER)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Battery Upgrade Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_UPGRADE_TIER1.get(), 1)
            .pattern("IBI")
            .pattern("CPC")
            .pattern("IBI")
            .define('I', Items.IRON_INGOT)
            .define('B', ModItems.CAPACITOR.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_capacitor", has(ModItems.CAPACITOR.get()))
            .save(output)
        
        // Battery Upgrade Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_UPGRADE_TIER2.get(), 1)
            .pattern("IBI")
            .pattern("CPC")
            .pattern("IBI")
            .define('I', Items.GOLD_INGOT)
            .define('B', ModItems.CAPACITOR.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.BATTERY_UPGRADE_TIER1.get())
            .unlockedBy("has_battery1", has(ModItems.BATTERY_UPGRADE_TIER1.get()))
            .save(output)
        
        // Battery Upgrade Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_UPGRADE_TIER3.get(), 1)
            .pattern("IDI")
            .pattern("CPC")
            .pattern("IDI")
            .define('I', Items.GOLD_INGOT)
            .define('D', Items.DIAMOND)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.BATTERY_UPGRADE_TIER2.get())
            .unlockedBy("has_battery2", has(ModItems.BATTERY_UPGRADE_TIER2.get()))
            .save(output)
        
        // Chunkloader Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHUNKLOADER_UPGRADE.get(), 1)
            .pattern("GEG")
            .pattern("CUC")
            .pattern("GOG")
            .define('G', Items.GOLD_INGOT)
            .define('E', Items.ENDER_EYE)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('U', ModItems.CONTROL_UNIT.get())
            .define('O', Blocks.OBSIDIAN)
            .unlockedBy("has_cu", has(ModItems.CONTROL_UNIT.get()))
            .save(output)
        
        // Crafting Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRAFTING_UPGRADE.get(), 1)
            .pattern("ICI")
            .pattern("WPW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('W', Blocks.CRAFTING_TABLE)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Database Upgrade Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATABASE_UPGRADE_TIER1.get(), 1)
            .pattern("ICI")
            .pattern("APA")
            .pattern("IDI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('A', ModItems.ALU.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('D', ModItems.HDD_TIER1.get())
            .unlockedBy("has_hdd1", has(ModItems.HDD_TIER1.get()))
            .save(output)
        
        // Experience Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EXPERIENCE_UPGRADE.get(), 1)
            .pattern("GEG")
            .pattern("CPC")
            .pattern("GEG")
            .define('G', Items.GOLD_INGOT)
            .define('E', Items.EMERALD)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // Generator Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GENERATOR_UPGRADE.get(), 1)
            .pattern("IPI")
            .pattern("CFC")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('P', Items.PISTON)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('F', Blocks.FURNACE)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Hover Upgrade Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HOVER_UPGRADE_TIER1.get(), 1)
            .pattern("IFI")
            .pattern("CPC")
            .pattern("ILI")
            .define('I', Items.IRON_INGOT)
            .define('F', Items.FEATHER)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('L', Items.LEATHER)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Hover Upgrade Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HOVER_UPGRADE_TIER2.get(), 1)
            .pattern("IEI")
            .pattern("CPC")
            .pattern("IHI")
            .define('I', Items.GOLD_INGOT)
            .define('E', Items.ENDER_PEARL)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('H', ModItems.HOVER_UPGRADE_TIER1.get())
            .unlockedBy("has_hover1", has(ModItems.HOVER_UPGRADE_TIER1.get()))
            .save(output)
        
        // Inventory Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INVENTORY_UPGRADE.get(), 1)
            .pattern("WCW")
            .pattern("IPI")
            .pattern("WCW")
            .define('W', ItemTags.PLANKS)
            .define('C', Items.CHEST)
            .define('I', Items.IRON_INGOT)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Inventory Controller Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), 1)
            .pattern("GCG")
            .pattern("APA")
            .pattern("GIG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('A', ModItems.ALU.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('I', ModItems.INVENTORY_UPGRADE.get())
            .unlockedBy("has_inv", has(ModItems.INVENTORY_UPGRADE.get()))
            .save(output)
        
        // Navigation Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NAVIGATION_UPGRADE.get(), 1)
            .pattern("GCG")
            .pattern("MPM")
            .pattern("GCG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('M', Items.MAP)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // Piston Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PISTON_UPGRADE.get(), 1)
            .pattern("IPI")
            .pattern("CPC")
            .pattern("ISI")
            .define('I', Items.IRON_INGOT)
            .define('P', Items.STICKY_PISTON)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('S', Items.SLIME_BALL)
            .unlockedBy("has_chip1", has(ModItems.MICROCHIP_TIER1.get()))
            .save(output)
        
        // Sign I/O Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SIGN_UPGRADE.get(), 1)
            .pattern("ISI")
            .pattern("CPD")
            .pattern("ISI")
            .define('I', Items.IRON_INGOT)
            .define('S', Items.OAK_SIGN)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('D', Items.INK_SAC)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Solar Generator Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SOLAR_GENERATOR_UPGRADE.get(), 1)
            .pattern("GGG")
            .pattern("CPC")
            .pattern("IRI")
            .define('G', Items.GLASS)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Tank Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TANK_UPGRADE.get(), 1)
            .pattern("IGI")
            .pattern("CPC")
            .pattern("IGI")
            .define('I', Items.IRON_INGOT)
            .define('G', Items.GLASS)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Tank Controller Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TANK_CONTROLLER_UPGRADE.get(), 1)
            .pattern("GCG")
            .pattern("APA")
            .pattern("GTG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('A', ModItems.ALU.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('T', ModItems.TANK_UPGRADE.get())
            .unlockedBy("has_tank", has(ModItems.TANK_UPGRADE.get()))
            .save(output)
        
        // Tractor Beam Upgrade  
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRACTOR_BEAM_UPGRADE.get(), 1)
            .pattern("IGI")
            .pattern("CPC")
            .pattern("ISI")
            .define('I', Items.IRON_INGOT)
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', Items.STICKY_PISTON)
            .define('S', Items.SLIME_BLOCK)
            .unlockedBy("has_chip3", has(ModItems.MICROCHIP_TIER3.get()))
            .save(output)
        
        // Trading Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRADING_UPGRADE.get(), 1)
            .pattern("GEG")
            .pattern("CPC")
            .pattern("GCG")
            .define('G', Items.GOLD_INGOT)
            .define('E', Items.EMERALD)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Leash Upgrade
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LEASH_UPGRADE.get(), 1)
            .pattern("SLS")
            .pattern("CPC")
            .pattern("SSS")
            .define('S', Items.STRING)
            .define('L', Items.LEAD)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_lead", has(Items.LEAD))
            .save(output)
    }
    
    private fun buildCaseRecipes(output: RecipeOutput) {
        // Case Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER1.get(), 1)
            .pattern("ICI")
            .pattern("P P")
            .pattern("IFI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('F', Blocks.CHEST)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Case Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER2.get(), 1)
            .pattern("GCG")
            .pattern("P P")
            .pattern("GFG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('F', ModBlocks.CASE_TIER1.get())
            .unlockedBy("has_case1", has(ModBlocks.CASE_TIER1.get()))
            .save(output)
        
        // Case Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CASE_TIER3.get(), 1)
            .pattern("DCD")
            .pattern("P P")
            .pattern("DFD")
            .define('D', Items.DIAMOND)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('F', ModBlocks.CASE_TIER2.get())
            .unlockedBy("has_case2", has(ModBlocks.CASE_TIER2.get()))
            .save(output)
    }
    
    private fun buildScreenRecipes(output: RecipeOutput) {
        // Screen Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCREEN_TIER1.get(), 1)
            .pattern("IGI")
            .pattern("RTR")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('G', Items.GLASS_PANE)
            .define('R', Items.REDSTONE)
            .define('T', ModItems.TRANSISTOR.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .unlockedBy("has_transistor", has(ModItems.TRANSISTOR.get()))
            .save(output)
        
        // Screen Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCREEN_TIER2.get(), 1)
            .pattern("GGG")
            .pattern("RSR")
            .pattern("GCG")
            .define('G', Items.GOLD_INGOT)
            .define('R', Items.REDSTONE)
            .define('S', ModBlocks.SCREEN_TIER1.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .unlockedBy("has_screen1", has(ModBlocks.SCREEN_TIER1.get()))
            .save(output)
        
        // Screen Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCREEN_TIER3.get(), 1)
            .pattern("DOD")
            .pattern("RSR")
            .pattern("DCD")
            .define('D', Items.DIAMOND)
            .define('O', Blocks.OBSIDIAN)
            .define('R', Items.REDSTONE)
            .define('S', ModBlocks.SCREEN_TIER2.get())
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .unlockedBy("has_screen2", has(ModBlocks.SCREEN_TIER2.get()))
            .save(output)
        
        // Keyboard
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.KEYBOARD.get(), 1)
            .pattern("BNA")
            .pattern("CPC")
            .pattern("   ")
            .define('B', ModItems.BUTTON_GROUP.get())
            .define('N', ModItems.NUMPAD.get())
            .define('A', ModItems.ARROW_KEYS.get())
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_buttons", has(ModItems.BUTTON_GROUP.get()))
            .save(output)
    }
    
    private fun buildNetworkRecipes(output: RecipeOutput) {
        // Cable
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CABLE.get(), 4)
            .pattern("IRI")
            .pattern("RRR")
            .pattern("IRI")
            .define('I', Items.IRON_NUGGET)
            .define('R', Items.REDSTONE)
            .unlockedBy("has_redstone", has(Items.REDSTONE))
            .save(output)
        
        // Relay
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RELAY.get(), 1)
            .pattern("ICI")
            .pattern("WPW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('W', ModItems.CABLE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_cable", has(ModItems.CABLE.get()))
            .save(output)
        
        // Switch
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SWITCH.get(), 1)
            .pattern("ICI")
            .pattern("WRW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('W', ModItems.CABLE.get())
            .define('R', ModBlocks.RELAY.get())
            .unlockedBy("has_relay", has(ModBlocks.RELAY.get()))
            .save(output)
        
        // Access Point
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ACCESS_POINT.get(), 1)
            .pattern("IAI")
            .pattern("WSW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('A', Items.IRON_BARS)
            .define('W', ModItems.WIRELESS_CARD_TIER1.get())
            .define('S', ModBlocks.SWITCH.get())
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .unlockedBy("has_switch", has(ModBlocks.SWITCH.get()))
            .save(output)
        
        // Power Distributor
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.POWER_DISTRIBUTOR.get(), 1)
            .pattern("ICI")
            .pattern("WPW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.CAPACITOR.get())
            .define('W', ModItems.CABLE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_capacitor", has(ModItems.CAPACITOR.get()))
            .save(output)
        
        // Net Splitter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_SPLITTER.get(), 1)
            .pattern("ICI")
            .pattern("WPW")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('W', ModItems.CABLE.get())
            .define('P', Items.PISTON)
            .unlockedBy("has_cable", has(ModItems.CABLE.get()))
            .save(output)
    }
    
    private fun buildPowerRecipes(output: RecipeOutput) {
        // Capacitor  
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPACITOR.get(), 1)
            .pattern("IGI")
            .pattern("PRP")
            .pattern("IGI")
            .define('I', Items.IRON_INGOT)
            .define('G', Items.GOLD_NUGGET)
            .define('P', Items.PAPER)
            .define('R', Items.REDSTONE)
            .unlockedBy("has_redstone", has(Items.REDSTONE))
            .save(output)
        
        // Capacitor Block
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CAPACITOR.get(), 1)
            .pattern("ICI")
            .pattern("CPC")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.CAPACITOR.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_capacitor", has(ModItems.CAPACITOR.get()))
            .save(output)
        
        // Power Converter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.POWER_CONVERTER.get(), 1)
            .pattern("ICI")
            .pattern("GPG")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.CAPACITOR.get())
            .define('G', Items.GOLD_INGOT)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_capacitor", has(ModItems.CAPACITOR.get()))
            .save(output)
        
        // Charger
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CHARGER.get(), 1)
            .pattern("ICI")
            .pattern("CPC")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.CAPACITOR.get())
            .define('P', ModBlocks.CAPACITOR.get())
            .unlockedBy("has_cap_block", has(ModBlocks.CAPACITOR.get()))
            .save(output)
    }
    
    private fun buildStorageRecipes(output: RecipeOutput) {
        // Floppy Disk
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLOPPY_DISK.get(), 1)
            .pattern("IPI")
            .pattern("PDP")
            .pattern("IPI")
            .define('I', Items.IRON_NUGGET)
            .define('P', Items.PAPER)
            .define('D', ModItems.DISK_PLATTER.get())
            .unlockedBy("has_platter", has(ModItems.DISK_PLATTER.get()))
            .save(output)
        
        // Disk Drive
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DISK_DRIVE.get(), 1)
            .pattern("ICI")
            .pattern("PDP")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('D', Items.STICKY_PISTON)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Hard Disk Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HDD_TIER1.get(), 1)
            .pattern("ICI")
            .pattern("DPD")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('D', ModItems.DISK_PLATTER.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_platter", has(ModItems.DISK_PLATTER.get()))
            .save(output)
        
        // Hard Disk Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HDD_TIER2.get(), 1)
            .pattern("GCG")
            .pattern("DPD")
            .pattern("GHG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('D', ModItems.DISK_PLATTER.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('H', ModItems.HDD_TIER1.get())
            .unlockedBy("has_hdd1", has(ModItems.HDD_TIER1.get()))
            .save(output)
        
        // Hard Disk Tier 3
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HDD_TIER3.get(), 1)
            .pattern("DCD")
            .pattern("PPP")
            .pattern("DHD")
            .define('D', Items.DIAMOND)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.DISK_PLATTER.get())
            .define('H', ModItems.HDD_TIER2.get())
            .unlockedBy("has_hdd2", has(ModItems.HDD_TIER2.get()))
            .save(output)
        
        // RAID
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RAID.get(), 1)
            .pattern("ICI")
            .pattern("DPD")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('D', ModBlocks.DISK_DRIVE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_drive", has(ModBlocks.DISK_DRIVE.get()))
            .save(output)
    }
    
    private fun buildMiscRecipes(output: RecipeOutput) {
        // Adapter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ADAPTER.get(), 1)
            .pattern("ICI")
            .pattern("WPW")
            .pattern("IBI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('W', ModItems.CABLE.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('B', ModBlocks.CASE_TIER1.get())
            .unlockedBy("has_case1", has(ModBlocks.CASE_TIER1.get()))
            .save(output)
        
        // Assembler
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ASSEMBLER.get(), 1)
            .pattern("ICN")
            .pattern("WPW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('N', Items.PISTON)
            .define('W', ModItems.CABLE.get())
            .define('P', Blocks.CRAFTING_TABLE)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Disassembler
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DISASSEMBLER.get(), 1)
            .pattern("ICI")
            .pattern("APW")
            .pattern("ICI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('A', ModBlocks.ASSEMBLER.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('W', ModItems.CUTTING_WIRE.get())
            .unlockedBy("has_assembler", has(ModBlocks.ASSEMBLER.get()))
            .save(output)
        
        // Geolyzer
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.GEOLYZER.get(), 1)
            .pattern("GCG")
            .pattern("EDE")
            .pattern("GPG")
            .define('G', Items.GOLD_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('E', Items.ENDER_PEARL)
            .define('D', Items.DAYLIGHT_DETECTOR)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Hologram Projector Tier 1
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HOLOGRAM_TIER1.get(), 1)
            .pattern("GCG")
            .pattern("PDP")
            .pattern("OBO")
            .define('G', Items.GLASS)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('D', Items.DIAMOND)
            .define('O', Blocks.OBSIDIAN)
            .define('B', Blocks.GLOWSTONE)
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Hologram Projector Tier 2
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HOLOGRAM_TIER2.get(), 1)
            .pattern("GCG")
            .pattern("PHP")
            .pattern("OEO")
            .define('G', Items.GLASS)
            .define('C', ModItems.MICROCHIP_TIER3.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('H', ModBlocks.HOLOGRAM_TIER1.get())
            .define('O', Blocks.OBSIDIAN)
            .define('E', Items.EMERALD)
            .unlockedBy("has_holo1", has(ModBlocks.HOLOGRAM_TIER1.get()))
            .save(output)
        
        // Motion Sensor
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MOTION_SENSOR.get(), 1)
            .pattern("ICI")
            .pattern("EDE")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('E', Items.ENDER_PEARL)
            .define('D', Items.DAYLIGHT_DETECTOR)
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_chip2", has(ModItems.MICROCHIP_TIER2.get()))
            .save(output)
        
        // Printer
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PRINTER.get(), 1)
            .pattern("ICI")
            .pattern("PDP")
            .pattern("IHI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('P', Items.PISTON)
            .define('D', ModItems.INK_CARTRIDGE.get())
            .define('H', Blocks.HOPPER)
            .unlockedBy("has_ink", has(ModItems.INK_CARTRIDGE.get()))
            .save(output)
        
        // Redstone I/O
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.REDSTONE_IO.get(), 1)
            .pattern("ICI")
            .pattern("RBR")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('R', Items.REDSTONE_BLOCK)
            .define('B', ModItems.REDSTONE_CARD_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_redstone_card", has(ModItems.REDSTONE_CARD_TIER1.get()))
            .save(output)
        
        // Transposer
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TRANSPOSER.get(), 1)
            .pattern("ICI")
            .pattern("PHP")
            .pattern("IBI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .define('H', Blocks.HOPPER)
            .define('B', Items.BUCKET)
            .unlockedBy("has_pcb", has(ModItems.PRINTED_CIRCUIT_BOARD.get()))
            .save(output)
        
        // Waypoint
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WAYPOINT.get(), 1)
            .pattern("ICI")
            .pattern("TPT")
            .pattern("ITI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER1.get())
            .define('T', Items.REDSTONE_TORCH)
            .define('P', ModItems.INTERWEB.get())
            .unlockedBy("has_interweb", has(ModItems.INTERWEB.get()))
            .save(output)
    }
    
    private fun buildRobotRecipes(output: RecipeOutput) {
        // Note: Robot is assembled in the Assembler, not crafted directly
    }
    
    private fun buildTabletRecipes(output: RecipeOutput) {
        // Note: Tablet is assembled in the Assembler
    }
    
    private fun buildMicrocontrollerRecipes(output: RecipeOutput) {
        // Note: Microcontroller is assembled in the Assembler
    }
    
    private fun buildServerRecipes(output: RecipeOutput) {
        // Server Rack
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RACK.get(), 1)
            .pattern("ICI")
            .pattern("WRW")
            .pattern("IPI")
            .define('I', Items.IRON_INGOT)
            .define('C', ModItems.MICROCHIP_TIER2.get())
            .define('W', ModItems.CABLE.get())
            .define('R', ModBlocks.RELAY.get())
            .define('P', ModItems.PRINTED_CIRCUIT_BOARD.get())
            .unlockedBy("has_relay", has(ModBlocks.RELAY.get()))
            .save(output)
    }
    
    private fun buildDroneRecipes(output: RecipeOutput) {
        // Note: Drone is assembled in the Assembler
    }
    
    companion object {
        private fun id(name: String): ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name)
    }
}
