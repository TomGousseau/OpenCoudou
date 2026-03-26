package li.cil.oc.common.block

import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

/**
 * Capacitor block - stores energy for the OC network.
 */
class CapacitorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.CapacitorBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.CAPACITOR.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.CapacitorBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
    
    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true
    
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.CapacitorBlockEntity
        return blockEntity?.getComparatorOutput() ?: 0
    }
}

/**
 * Power Converter - converts energy from other mods.
 */
class PowerConverterBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.PowerConverterBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.POWER_CONVERTER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.PowerConverterBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Power Distributor - distributes power across the network.
 */
class PowerDistributorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.PowerDistributorBlockEntity(pos, state)
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.POWER_DISTRIBUTOR.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.PowerDistributorBlockEntity)?.tick(lvl, pos, st)
            }
        } else null
    }
}

/**
 * Charger - charges robots, tablets, and drones.
 */
class ChargerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val CHARGING: BooleanProperty = BooleanProperty.create("charging")
    }
    
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CHARGING, false)
        )
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, CHARGING)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(CHARGING, false)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return li.cil.oc.common.blockentity.ChargerBlockEntity(pos, state)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.ChargerBlockEntity
            blockEntity?.openGui(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.CHARGER.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? li.cil.oc.common.blockentity.ChargerBlockEntity)?.tick(lvl, pos, st)
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
            val blockEntity = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.ChargerBlockEntity
            val isPowered = level.hasNeighborSignal(pos)
            blockEntity?.onRedstoneUpdate(isPowered)
        }
    }
}
