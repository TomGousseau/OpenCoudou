package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * Loot table provider for OpenComputers blocks.
 * Generates block drop loot tables.
 */
class ModLootTableProvider(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>
) : LootTableProvider(
    output,
    emptySet(),
    listOf(
        SubProviderEntry(::ModBlockLootSubProvider, LootContextParamSets.BLOCK)
    ),
    registries
)

/**
 * Block loot sub-provider that generates drop tables.
 */
class ModBlockLootSubProvider(
    provider: HolderLookup.Provider
) : BlockLootSubProvider(emptySet(), FeatureFlags.REGISTRY.allFlags(), provider) {
    
    override fun generate() {
        // Simple drops (block drops itself when broken)
        dropSelf(ModBlocks.CASE_TIER1.get())
        dropSelf(ModBlocks.CASE_TIER2.get())
        dropSelf(ModBlocks.CASE_TIER3.get())
        dropSelf(ModBlocks.CASE_CREATIVE.get())
        
        dropSelf(ModBlocks.SCREEN_TIER1.get())
        dropSelf(ModBlocks.SCREEN_TIER2.get())
        dropSelf(ModBlocks.SCREEN_TIER3.get())
        
        dropSelf(ModBlocks.KEYBOARD.get())
        
        dropSelf(ModBlocks.CAPACITOR.get())
        dropSelf(ModBlocks.CHARGER.get())
        dropSelf(ModBlocks.POWER_CONVERTER.get())
        dropSelf(ModBlocks.POWER_DISTRIBUTOR.get())
        
        dropSelf(ModBlocks.RELAY.get())
        dropSelf(ModBlocks.SWITCH.get())
        dropSelf(ModBlocks.ACCESS_POINT.get())
        dropSelf(ModBlocks.NET_SPLITTER.get())
        
        dropSelf(ModBlocks.DISK_DRIVE.get())
        dropSelf(ModBlocks.RAID.get())
        
        dropSelf(ModBlocks.ADAPTER.get())
        dropSelf(ModBlocks.ASSEMBLER.get())
        dropSelf(ModBlocks.DISASSEMBLER.get())
        dropSelf(ModBlocks.GEOLYZER.get())
        dropSelf(ModBlocks.HOLOGRAM_TIER1.get())
        dropSelf(ModBlocks.HOLOGRAM_TIER2.get())
        dropSelf(ModBlocks.MOTION_SENSOR.get())
        dropSelf(ModBlocks.PRINTER.get())
        dropSelf(ModBlocks.RACK.get())
        dropSelf(ModBlocks.REDSTONE_IO.get())
        dropSelf(ModBlocks.TRANSPOSER.get())
        dropSelf(ModBlocks.WAYPOINT.get())
        
        dropSelf(ModBlocks.MICROCONTROLLER.get())
        dropSelf(ModBlocks.ROBOT.get())
        
        dropSelf(ModBlocks.CHAMELIUM_BLOCK.get())
        
        // Cable has special handling - drop cable item
        dropSelf(ModBlocks.CABLE.get())
    }
    
    override fun getKnownBlocks(): Iterable<Block> {
        return BuiltInRegistries.BLOCK.stream()
            .filter { block ->
                val key = BuiltInRegistries.BLOCK.getKey(block)
                key.namespace == OpenComputers.MOD_ID
            }
            .toList()
    }
}
