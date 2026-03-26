package li.cil.oc.common.block

import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Cable block - connects components in a network.
 * 
 * Cables automatically connect to adjacent OC blocks and can be dyed
 * different colors to create separate networks.
 */
class CableBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val NORTH: BooleanProperty = BooleanProperty.create("north")
        val SOUTH: BooleanProperty = BooleanProperty.create("south")
        val EAST: BooleanProperty = BooleanProperty.create("east")
        val WEST: BooleanProperty = BooleanProperty.create("west")
        val UP: BooleanProperty = BooleanProperty.create("up")
        val DOWN: BooleanProperty = BooleanProperty.create("down")
        
        private val CORE_SHAPE = box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0)
        private val NORTH_SHAPE = box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0)
        private val SOUTH_SHAPE = box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0)
        private val EAST_SHAPE = box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0)
        private val WEST_SHAPE = box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0)
        private val UP_SHAPE = box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0)
        private val DOWN_SHAPE = box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0)
    }
    
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return getConnectionState(context.level, context.clickedPos)
    }
    
    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        return getConnectionState(level, pos)
    }
    
    private fun getConnectionState(level: LevelAccessor, pos: BlockPos): BlockState {
        return defaultBlockState()
            .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
            .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
            .setValue(EAST, canConnect(level, pos, Direction.EAST))
            .setValue(WEST, canConnect(level, pos, Direction.WEST))
            .setValue(UP, canConnect(level, pos, Direction.UP))
            .setValue(DOWN, canConnect(level, pos, Direction.DOWN))
    }
    
    private fun canConnect(level: LevelAccessor, pos: BlockPos, direction: Direction): Boolean {
        val neighborPos = pos.relative(direction)
        val neighborState = level.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        
        // Connect to other OC blocks
        return neighborBlock is CableBlock ||
               neighborBlock is CaseBlock ||
               neighborBlock is ScreenBlock ||
               neighborBlock is KeyboardBlock ||
               neighborBlock is AdapterBlock ||
               neighborBlock is RelayBlock ||
               neighborBlock is CapacitorBlock ||
               neighborBlock is PowerConverterBlock ||
               neighborBlock is PowerDistributorBlock ||
               neighborBlock is ChargerBlock ||
               neighborBlock is DiskDriveBlock ||
               neighborBlock is RackBlock ||
               neighborBlock is RedstoneIOBlock ||
               neighborBlock is AccessPointBlock ||
               neighborBlock is GeolyzerBlock ||
               neighborBlock is MotionSensorBlock ||
               neighborBlock is HologramBlock ||
               neighborBlock is MicrocontrollerBlock ||
               neighborBlock is TransposerBlock
    }
    
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        var shape = CORE_SHAPE
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE)
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE)
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE)
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE)
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE)
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE)
        return shape
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.CableBlockEntity(pos, state)
    }
}

/**
 * Relay block - bridges networks together.
 */
class RelayBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.RelayBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RelayBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.RELAY.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.RelayBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Access Point - wireless network bridge.
 */
class AccessPointBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.AccessPointBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.AccessPointBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.ACCESS_POINT.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.AccessPointBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Adapter block - exposes adjacent blocks as components.
 */
class AdapterBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.AdapterBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.AdapterBlockEntity
            blockEntity?.updateAdaptedBlocks()
        }
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.ADAPTER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.AdapterBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Transposer - moves items and fluids between inventories.
 */
class TransposerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.TransposerBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.TRANSPOSER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.TransposerBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}
