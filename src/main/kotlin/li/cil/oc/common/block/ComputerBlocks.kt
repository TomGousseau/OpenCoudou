package li.cil.oc.common.block

import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
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
 * Computer case block - the main computer housing.
 * 
 * Cases come in different tiers with varying component capacities:
 * - Tier 1: 8 components
 * - Tier 2: 12 components
 * - Tier 3: 16 components
 * - Creative: Unlimited
 */
class CaseBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(RUNNING, false)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    // ========================================
    // Block Entity
    // ========================================
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CaseBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.CASE.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? CaseBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    // ========================================
    // Interaction
    // ========================================
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? CaseBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        // Handle wrench rotation, etc.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }
    
    // ========================================
    // Utility
    // ========================================
    
    fun getTier(state: BlockState): Int {
        return ModBlocks.getCaseTier(state.block)
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? CaseBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * Screen block - displays text output from computers.
 */
class ScreenBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.ScreenBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.SCREEN.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.ScreenBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.ScreenBlockEntity
            blockEntity?.handleTouch(player, hitResult)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    fun getTier(state: BlockState): Int = ModBlocks.getScreenTier(state.block)
}

/**
 * Keyboard block - receives player input.
 */
class KeyboardBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.KeyboardBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.KeyboardBlockEntity
            blockEntity?.startTyping(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}

/**
 * Redstone I/O block - provides advanced redstone control.
 */
class RedstoneIOBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.RedstoneIOBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun isSignalSource(state: BlockState): Boolean = true
    
    override fun getSignal(state: BlockState, level: net.minecraft.world.level.BlockGetter, pos: BlockPos, direction: Direction): Int {
        val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RedstoneIOBlockEntity
        return blockEntity?.getOutput(direction.opposite) ?: 0
    }
    
    override fun getDirectSignal(state: BlockState, level: net.minecraft.world.level.BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RedstoneIOBlockEntity
            blockEntity?.checkInputChanged()
        }
    }
}
