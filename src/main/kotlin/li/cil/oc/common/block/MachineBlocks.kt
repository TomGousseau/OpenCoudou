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
 * Disk Drive - provides access to floppy disks.
 */
class DiskDriveBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val HAS_DISK: BooleanProperty = BooleanProperty.create("has_disk")
    }
    
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_DISK, false)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, HAS_DISK)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.DiskDriveBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DiskDriveBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DiskDriveBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * RAID - combines multiple hard drives.
 */
class RaidBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return li.cil.oc.common.blockentity.RaidBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RaidBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RaidBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * Rack - houses servers and other rack-mounted components.
 */
class RackBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return li.cil.oc.common.blockentity.RackBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RackBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.RACK.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.RackBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RackBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * Assembler - creates robots and other complex items.
 */
class AssemblerBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return li.cil.oc.common.blockentity.AssemblerBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.AssemblerBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.ASSEMBLER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.AssemblerBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.AssemblerBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * Disassembler - breaks down items into components.
 */
class DisassemblerBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return li.cil.oc.common.blockentity.DisassemblerBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DisassemblerBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.DISASSEMBLER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.DisassemblerBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DisassemblerBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}

/**
 * Printer - creates 3D printed objects.
 */
class PrinterBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return li.cil.oc.common.blockentity.PrinterBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.PrinterBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.PRINTER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.PrinterBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.PrinterBlockEntity
            blockEntity?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}
