package li.cil.oc.common.block

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

/**
 * Geolyzer - scans surrounding terrain.
 */
class GeolyzerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.GeolyzerBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.GEOLYZER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.GeolyzerBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Motion Sensor - detects entity movement.
 */
class MotionSensorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
    }
    
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.MotionSensorBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.MOTION_SENSOR.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.MotionSensorBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Waypoint - navigation marker.
 */
class WaypointBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
    }
    
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.WaypointBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.WaypointBlockEntity
            blockEntity?.cycleLabel()
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}

/**
 * Hologram - projects 3D holograms.
 */
class HologramBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val RUNNING: BooleanProperty = BooleanProperty.create("running")
    }
    
    init {
        registerDefaultState(stateDefinition.any().setValue(RUNNING, false))
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(RUNNING)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.HologramBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.HOLOGRAM.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.HologramBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    fun getTier(state: BlockState): Int = ModBlocks.getHologramTier(state.block)
}

/**
 * Microcontroller - simple programmable controller.
 */
class MicrocontrollerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val RUNNING: BooleanProperty = BooleanProperty.create("running")
    }
    
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(RUNNING, false)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, RUNNING)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.MicrocontrollerBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
            // Toggle power or open config
            if (player.isCrouching) {
                blockEntity?.toggle()
            } else {
                blockEntity?.openGui(player)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.MICROCONTROLLER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.MicrocontrollerBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
            blockEntity?.onNeighborChanged()
        }
    }
    
    override fun isSignalSource(state: BlockState): Boolean = true
    
    override fun getSignal(
        state: BlockState,
        level: net.minecraft.world.level.BlockGetter,
        pos: BlockPos,
        direction: Direction
    ): Int {
        val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
        return blockEntity?.getRedstoneOutput(direction.opposite) ?: 0
    }
    
    override fun getDirectSignal(
        state: BlockState,
        level: net.minecraft.world.level.BlockGetter,
        pos: BlockPos,
        direction: Direction
    ): Int {
        return getSignal(state, level, pos, direction)
    }
}
