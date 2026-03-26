package li.cil.oc.common.block

import li.cil.oc.common.blockentity.CableBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Cable block that can connect to other cables and OC components.
 * Uses a dynamic shape based on which sides are connected.
 */
class CableBlock(properties: Properties) : Block(properties), EntityBlock, SimpleWaterloggedBlock {
    
    companion object {
        // Connection properties for each direction
        val NORTH: BooleanProperty = BooleanProperty.create("north")
        val SOUTH: BooleanProperty = BooleanProperty.create("south")
        val EAST: BooleanProperty = BooleanProperty.create("east")
        val WEST: BooleanProperty = BooleanProperty.create("west")
        val UP: BooleanProperty = BooleanProperty.create("up")
        val DOWN: BooleanProperty = BooleanProperty.create("down")
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        
        // Connection property by direction
        fun getPropertyForDirection(direction: Direction): BooleanProperty {
            return when (direction) {
                Direction.NORTH -> NORTH
                Direction.SOUTH -> SOUTH
                Direction.EAST -> EAST
                Direction.WEST -> WEST
                Direction.UP -> UP
                Direction.DOWN -> DOWN
            }
        }
        
        // Core shape (center of cable)
        private val CORE_SHAPE: VoxelShape = box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0)
        
        // Connection shapes for each direction
        private val CONNECTION_SHAPES: Map<Direction, VoxelShape> = mapOf(
            Direction.NORTH to box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0),
            Direction.SOUTH to box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0),
            Direction.EAST to box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0),
            Direction.WEST to box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0),
            Direction.UP to box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0),
            Direction.DOWN to box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0)
        )
        
        // Cache for combined shapes
        private val SHAPE_CACHE = mutableMapOf<Int, VoxelShape>()
        
        /**
         * Get the combined shape for a given connection state
         */
        fun getShapeForConnections(
            north: Boolean,
            south: Boolean,
            east: Boolean,
            west: Boolean,
            up: Boolean,
            down: Boolean
        ): VoxelShape {
            val key = (if (north) 1 else 0) or
                      (if (south) 2 else 0) or
                      (if (east) 4 else 0) or
                      (if (west) 8 else 0) or
                      (if (up) 16 else 0) or
                      (if (down) 32 else 0)
            
            return SHAPE_CACHE.getOrPut(key) {
                var shape = CORE_SHAPE
                if (north) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.NORTH]!!, BooleanOp.OR)
                if (south) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.SOUTH]!!, BooleanOp.OR)
                if (east) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.EAST]!!, BooleanOp.OR)
                if (west) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.WEST]!!, BooleanOp.OR)
                if (up) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.UP]!!, BooleanOp.OR)
                if (down) shape = Shapes.join(shape, CONNECTION_SHAPES[Direction.DOWN]!!, BooleanOp.OR)
                shape.optimize()
            }
        }
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
                .setValue(WATERLOGGED, false)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val level = context.level
        val pos = context.clickedPos
        val fluidState = level.getFluidState(pos)
        
        return defaultBlockState()
            .setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
            .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
            .setValue(EAST, canConnectTo(level, pos, Direction.EAST))
            .setValue(WEST, canConnectTo(level, pos, Direction.WEST))
            .setValue(UP, canConnectTo(level, pos, Direction.UP))
            .setValue(DOWN, canConnectTo(level, pos, Direction.DOWN))
            .setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
    }
    
    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        
        val property = getPropertyForDirection(direction)
        return state.setValue(property, canConnectTo(level, pos, direction))
    }
    
    /**
     * Check if the cable can connect to a block in the given direction
     */
    private fun canConnectTo(level: LevelAccessor, pos: BlockPos, direction: Direction): Boolean {
        val neighborPos = pos.relative(direction)
        val neighborState = level.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        
        // Always connect to other cables
        if (neighborBlock is CableBlock) {
            return true
        }
        
        // Connect to OC blocks (any block that implements our network interface)
        val blockEntity = level.getBlockEntity(neighborPos)
        if (blockEntity != null) {
            // Check if it's an OC block entity
            return isOCNetworkable(blockEntity)
        }
        
        return false
    }
    
    /**
     * Check if a block entity can connect to the OC network
     */
    private fun isOCNetworkable(blockEntity: BlockEntity): Boolean {
        // Check if the block entity has an OC network node
        // This would be any of our computer, screen, etc. block entities
        return blockEntity.javaClass.name.startsWith("li.cil.oc")
    }
    
    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return getShapeForConnections(
            state.getValue(NORTH),
            state.getValue(SOUTH),
            state.getValue(EAST),
            state.getValue(WEST),
            state.getValue(UP),
            state.getValue(DOWN)
        )
    }
    
    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return getShape(state, level, pos, context)
    }
    
    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) {
            Fluids.WATER.getSource(false)
        } else {
            super.getFluidState(state)
        }
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CableBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.CABLE.get()) {
            BlockEntityTicker { tickLevel, tickPos, tickState, blockEntity ->
                (blockEntity as? CableBlockEntity)?.tick()
            }
        } else null
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide && state.block != oldState.block) {
            // Notify the block entity to update network connections
            (level.getBlockEntity(pos) as? CableBlockEntity)?.onPlace()
        }
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide && state.block != newState.block) {
            // Notify the block entity to remove network connections
            (level.getBlockEntity(pos) as? CableBlockEntity)?.onRemove()
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
    
    /**
     * Get the number of connections this cable has
     */
    fun getConnectionCount(state: BlockState): Int {
        var count = 0
        if (state.getValue(NORTH)) count++
        if (state.getValue(SOUTH)) count++
        if (state.getValue(EAST)) count++
        if (state.getValue(WEST)) count++
        if (state.getValue(UP)) count++
        if (state.getValue(DOWN)) count++
        return count
    }
}

/**
 * A colored cable variant that only connects to cables of the same color
 */
class ColoredCableBlock(
    properties: Properties,
    val color: CableColor
) : CableBlock(properties) {
    
    companion object {
        val COLOR: net.minecraft.world.level.block.state.properties.EnumProperty<CableColor> =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("color", CableColor::class.java)
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(COLOR)
    }
}

/**
 * Cable colors for colored cables
 */
enum class CableColor : net.minecraft.util.StringRepresentable {
    WHITE,
    ORANGE,
    MAGENTA,
    LIGHT_BLUE,
    YELLOW,
    LIME,
    PINK,
    GRAY,
    LIGHT_GRAY,
    CYAN,
    PURPLE,
    BLUE,
    BROWN,
    GREEN,
    RED,
    BLACK;
    
    override fun getSerializedName(): String = name.lowercase()
    
    companion object {
        fun fromDye(dyeColor: net.minecraft.world.item.DyeColor): CableColor {
            return entries[dyeColor.ordinal]
        }
    }
}
