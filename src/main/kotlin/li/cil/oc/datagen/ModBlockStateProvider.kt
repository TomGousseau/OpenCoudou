package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import net.minecraft.core.Direction
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ConfiguredModel
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.data.ExistingFileHelper

/**
 * Block state and model provider for OpenComputers.
 * Generates block states and block models for all blocks.
 */
class ModBlockStateProvider(
    output: PackOutput,
    helper: ExistingFileHelper
) : BlockStateProvider(output, OpenComputers.MOD_ID, helper) {
    
    override fun registerStatesAndModels() {
        // Cases - simple cubes with different textures per tier
        simpleBlockWithItem(ModBlocks.CASE_TIER1.get(), "case_tier1")
        simpleBlockWithItem(ModBlocks.CASE_TIER2.get(), "case_tier2")
        simpleBlockWithItem(ModBlocks.CASE_TIER3.get(), "case_tier3")
        simpleBlockWithItem(ModBlocks.CASE_CREATIVE.get(), "case_creative")
        
        // Screens - horizontal facing blocks
        horizontalFacingBlock(ModBlocks.SCREEN_TIER1.get(), "screen_tier1")
        horizontalFacingBlock(ModBlocks.SCREEN_TIER2.get(), "screen_tier2")
        horizontalFacingBlock(ModBlocks.SCREEN_TIER3.get(), "screen_tier3")
        
        // Keyboard - small block
        horizontalFacingBlock(ModBlocks.KEYBOARD.get(), "keyboard")
        
        // Power blocks
        simpleBlockWithItem(ModBlocks.CAPACITOR.get(), "capacitor")
        simpleBlockWithItem(ModBlocks.CHARGER.get(), "charger") 
        simpleBlockWithItem(ModBlocks.POWER_CONVERTER.get(), "power_converter")
        simpleBlockWithItem(ModBlocks.POWER_DISTRIBUTOR.get(), "power_distributor")
        
        // Network blocks
        simpleBlockWithItem(ModBlocks.RELAY.get(), "relay")
        simpleBlockWithItem(ModBlocks.SWITCH.get(), "switch")
        simpleBlockWithItem(ModBlocks.ACCESS_POINT.get(), "access_point")
        simpleBlockWithItem(ModBlocks.NET_SPLITTER.get(), "net_splitter")
        
        // Storage
        horizontalFacingBlock(ModBlocks.DISK_DRIVE.get(), "disk_drive")
        horizontalFacingBlock(ModBlocks.RAID.get(), "raid")
        
        // Utility blocks
        horizontalFacingBlock(ModBlocks.ADAPTER.get(), "adapter")
        horizontalFacingBlock(ModBlocks.ASSEMBLER.get(), "assembler")
        horizontalFacingBlock(ModBlocks.DISASSEMBLER.get(), "disassembler")
        simpleBlockWithItem(ModBlocks.GEOLYZER.get(), "geolyzer")
        simpleBlockWithItem(ModBlocks.HOLOGRAM_TIER1.get(), "hologram_tier1")
        simpleBlockWithItem(ModBlocks.HOLOGRAM_TIER2.get(), "hologram_tier2")
        simpleBlockWithItem(ModBlocks.MOTION_SENSOR.get(), "motion_sensor")
        horizontalFacingBlock(ModBlocks.PRINTER.get(), "printer")
        horizontalFacingBlock(ModBlocks.RACK.get(), "rack")
        simpleBlockWithItem(ModBlocks.REDSTONE_IO.get(), "redstone_io")
        simpleBlockWithItem(ModBlocks.TRANSPOSER.get(), "transposer")
        simpleBlockWithItem(ModBlocks.WAYPOINT.get(), "waypoint")
        
        // Robot & Microcontroller
        simpleBlockWithItem(ModBlocks.ROBOT.get(), "robot")
        simpleBlockWithItem(ModBlocks.MICROCONTROLLER.get(), "microcontroller")
        
        // Materials
        simpleBlockWithItem(ModBlocks.CHAMELIUM_BLOCK.get(), "chamelium_block")
        
        // Cable - special multi-connection model
        cableBlock(ModBlocks.CABLE.get())
    }
    
    private fun simpleBlockWithItem(block: Block, name: String) {
        val model = models().cubeAll(name, modLoc("block/$name"))
        simpleBlock(block, model)
        simpleBlockItem(block, model)
    }
    
    private fun horizontalFacingBlock(block: Block, name: String) {
        val model = models().cubeAll(name, modLoc("block/$name"))
        horizontalBlock(block, model)
        simpleBlockItem(block, model)
    }
    
    private fun cableBlock(block: Block) {
        // Cable uses a multi-part model with connection parts
        val coreModel = models()
            .withExistingParent("cable_core", mcLoc("block/cube_all"))
            .texture("all", modLoc("block/cable"))
        
        // For now, just use a simple cube model
        // A full implementation would use multi-part with connection booleans
        simpleBlock(block, coreModel)
        simpleBlockItem(block, coreModel)
    }
    
    private fun modLoc(path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
}
