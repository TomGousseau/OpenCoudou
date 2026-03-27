package li.cil.oc.common.block

import li.cil.oc.common.blockentity.WebDisplayBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
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
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Web Display block - displays web pages in the world.
 * Similar to WebDisplays mod functionality but integrated with SkibidiOS2.
 * 
 * Features:
 * - Right-click to open URL configuration GUI
 * - Laser Pointer for click interactions
 * - Remote Keyboard for typing
 * - Multi-block screen support
 */
class WebDisplayBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val WIDTH: IntegerProperty = IntegerProperty.create("width", 1, 8)
        val HEIGHT: IntegerProperty = IntegerProperty.create("height", 1, 8)
        
        // Screen shape (slightly recessed)
        private val NORTH_SHAPE: VoxelShape = box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0)
        private val SOUTH_SHAPE: VoxelShape = box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0)
        private val EAST_SHAPE: VoxelShape = box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0)
        private val WEST_SHAPE: VoxelShape = box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    }
    
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WIDTH, 1)
                .setValue(HEIGHT, 1)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, WIDTH, HEIGHT)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.NORTH -> NORTH_SHAPE
            Direction.SOUTH -> SOUTH_SHAPE
            Direction.EAST -> EAST_SHAPE
            Direction.WEST -> WEST_SHAPE
            else -> NORTH_SHAPE
        }
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return WebDisplayBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.WEB_DISPLAY.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? WebDisplayBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    /**
     * Right-click to open URL configuration screen.
     */
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? WebDisplayBlockEntity
            blockEntity?.openConfigScreen(player as ServerPlayer)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    /**
     * Handle interaction from laser pointer.
     */
    fun handleLaserClick(
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
        isRightClick: Boolean
    ) {
        val blockEntity = level.getBlockEntity(pos) as? WebDisplayBlockEntity ?: return
        
        // Calculate hit position on screen (0.0 to 1.0)
        val localHit = hitResult.location.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val facing = level.getBlockState(pos).getValue(FACING)
        
        val (screenX, screenY) = when (facing) {
            Direction.NORTH -> Pair(1.0 - localHit.x, 1.0 - localHit.y)
            Direction.SOUTH -> Pair(localHit.x, 1.0 - localHit.y)
            Direction.EAST -> Pair(1.0 - localHit.z, 1.0 - localHit.y)
            Direction.WEST -> Pair(localHit.z, 1.0 - localHit.y)
            else -> Pair(0.5, 0.5)
        }
        
        blockEntity.handleClick(screenX.toFloat(), screenY.toFloat(), if (isRightClick) 1 else 0)
    }
    
    /**
     * Handle scroll from laser pointer.
     */
    fun handleLaserScroll(level: Level, pos: BlockPos, delta: Int) {
        val blockEntity = level.getBlockEntity(pos) as? WebDisplayBlockEntity ?: return
        blockEntity.handleScroll(delta)
    }
    
    /**
     * Handle keyboard input.
     */
    fun handleKeyInput(level: Level, pos: BlockPos, keyCode: Int, char: Char, pressed: Boolean) {
        val blockEntity = level.getBlockEntity(pos) as? WebDisplayBlockEntity ?: return
        if (pressed) {
            blockEntity.handleKeyPress(keyCode, char)
        } else {
            blockEntity.handleKeyRelease(keyCode)
        }
    }
    
    override fun getLightEmission(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        val blockEntity = level.getBlockEntity(pos) as? WebDisplayBlockEntity
        return if (blockEntity?.isOn == true) 7 else 0
    }
}
