package li.cil.oc.common.item

import li.cil.oc.common.block.WebDisplayBlock
import li.cil.oc.common.blockentity.WebDisplayBlockEntity
import li.cil.oc.common.init.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Laser Pointer - A sword-like item for clicking on WebDisplay blocks
 * from a distance without placing blocks.
 * 
 * Usage:
 * - Left-click (swing) while looking at WebDisplay → Mouse left click
 * - Right-click while looking at WebDisplay → Mouse right click
 * - Shift + Right-click on WebDisplay → Link to that display
 * - Scroll while holding → Scroll on linked display
 */
class LaserPointerItem(properties: Properties) : Item(properties.stacksTo(1)) {
    
    companion object {
        const val LASER_RANGE = 32.0 // Block range for laser pointer
        const val BEAM_COLOR = 0xFF0000 // Red laser beam
    }
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.desc")
            .withStyle(ChatFormatting.GRAY))
        
        val linkedPos = getLinkedDisplay(stack)
        if (linkedPos != null) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.linked",
                linkedPos.x, linkedPos.y, linkedPos.z)
                .withStyle(ChatFormatting.GREEN))
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.unlinked")
                .withStyle(ChatFormatting.YELLOW))
        }
        
        tooltipComponents.add(Component.literal(""))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.usage1")
            .withStyle(ChatFormatting.DARK_GRAY))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.usage2")
            .withStyle(ChatFormatting.DARK_GRAY))
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.laser_pointer.usage3")
            .withStyle(ChatFormatting.DARK_GRAY))
    }
    
    /**
     * Right-click in air - interact with WebDisplay in line of sight.
     */
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        
        val hitResult = performRayTrace(level, player)
        if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
            val blockHit = hitResult as BlockHitResult
            val hitPos = blockHit.blockPos
            val state = level.getBlockState(hitPos)
            
            if (state.block is WebDisplayBlock) {
                if (player.isShiftKeyDown) {
                    // Link to display
                    linkToDisplay(stack, hitPos)
                    if (!level.isClientSide) {
                        player.displayClientMessage(
                            Component.translatable("message.opencomputers.laser_pointer.linked", hitPos.x, hitPos.y, hitPos.z)
                                .withStyle(ChatFormatting.GREEN),
                            true
                        )
                    }
                    level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f)
                    return InteractionResultHolder.success(stack)
                } else {
                    // Right-click on display
                    val screenCoords = calculateScreenCoordinates(player, blockHit, state)
                    if (!level.isClientSide) {
                        val blockEntity = level.getBlockEntity(hitPos)
                        if (blockEntity is WebDisplayBlockEntity) {
                            blockEntity.handleClick(screenCoords.first, screenCoords.second, 1) // 1 = right click
                        }
                    }
                    level.playSound(player, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 1.2f)
                    return InteractionResultHolder.success(stack)
                }
            }
        }
        
        return InteractionResultHolder.pass(stack)
    }
    
    /**
     * Right-click on block.
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        
        if (state.block is WebDisplayBlock) {
            if (player.isShiftKeyDown) {
                // Link to display
                linkToDisplay(stack, pos)
                if (!level.isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("message.opencomputers.laser_pointer.linked", pos.x, pos.y, pos.z)
                            .withStyle(ChatFormatting.GREEN),
                        true
                    )
                }
                level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f)
                return InteractionResult.SUCCESS
            } else {
                // Calculate where on the screen we clicked
                val hitLocation = context.clickLocation
                val blockHit = BlockHitResult(hitLocation, context.clickedFace, pos, context.isInside)
                val screenCoords = calculateScreenCoordinates(player, blockHit, state)
                
                if (!level.isClientSide) {
                    val blockEntity = level.getBlockEntity(pos)
                    if (blockEntity is WebDisplayBlockEntity) {
                        blockEntity.handleClick(screenCoords.first, screenCoords.second, 1) // 1 = right click
                    }
                }
                level.playSound(player, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 1.2f)
                return InteractionResult.SUCCESS
            }
        }
        
        return InteractionResult.PASS
    }
    
    /**
     * Called when player swings the item (left click).
     * We use this to send left-click to WebDisplay.
     */
    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity, hand: InteractionHand): Boolean {
        if (entity !is Player) return false
        val level = entity.level()
        
        val hitResult = performRayTrace(level, entity)
        if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
            val blockHit = hitResult as BlockHitResult
            val hitPos = blockHit.blockPos
            val state = level.getBlockState(hitPos)
            
            if (state.block is WebDisplayBlock) {
                val screenCoords = calculateScreenCoordinates(entity, blockHit, state)
                
                if (!level.isClientSide) {
                    val blockEntity = level.getBlockEntity(hitPos)
                    if (blockEntity is WebDisplayBlockEntity) {
                        blockEntity.handleClick(screenCoords.first, screenCoords.second, 0) // 0 = left click
                    }
                }
                level.playSound(entity, entity.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 1.0f)
                return true // Consume the swing
            }
        }
        
        return false
    }
    
    /**
     * Perform ray trace from player's eyes.
     */
    private fun performRayTrace(level: Level, player: Player): BlockHitResult? {
        val eyePos = player.eyePosition
        val lookVec = player.lookAngle
        val endPos = eyePos.add(lookVec.x * LASER_RANGE, lookVec.y * LASER_RANGE, lookVec.z * LASER_RANGE)
        
        return level.clip(ClipContext(
            eyePos,
            endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ))
    }
    
    /**
     * Calculate screen coordinates from hit result.
     * Returns (x, y) in range [0, 1].
     */
    private fun calculateScreenCoordinates(player: Player, hitResult: BlockHitResult, state: BlockState): Pair<Float, Float> {
        val facing = state.getValue(WebDisplayBlock.FACING)
        val hitVec = hitResult.location
        val blockPos = hitResult.blockPos
        
        // Get relative hit position within the block
        val relX = hitVec.x - blockPos.x
        val relY = hitVec.y - blockPos.y
        val relZ = hitVec.z - blockPos.z
        
        // Convert to screen coordinates based on facing
        val screenX: Float
        val screenY: Float = (1.0 - relY).toFloat().coerceIn(0f, 1f) // Y is inverted (top = 0)
        
        when (facing) {
            net.minecraft.core.Direction.NORTH -> {
                screenX = (1.0 - relX).toFloat() // Mirror X
            }
            net.minecraft.core.Direction.SOUTH -> {
                screenX = relX.toFloat()
            }
            net.minecraft.core.Direction.WEST -> {
                screenX = relZ.toFloat()
            }
            net.minecraft.core.Direction.EAST -> {
                screenX = (1.0 - relZ).toFloat()
            }
            else -> {
                screenX = relX.toFloat()
            }
        }
        
        return Pair(screenX.coerceIn(0f, 1f), screenY)
    }
    
    /**
     * Link this laser pointer to a specific display.
     */
    fun linkToDisplay(stack: ItemStack, pos: BlockPos) {
        val posString = "${pos.x},${pos.y},${pos.z}"
        stack.set(ModDataComponents.BOUND_ADDRESS.get(), posString)
    }
    
    /**
     * Get the linked display position.
     */
    fun getLinkedDisplay(stack: ItemStack): BlockPos? {
        val posString = stack.get(ModDataComponents.BOUND_ADDRESS.get()) ?: return null
        val parts = posString.split(",")
        if (parts.size != 3) return null
        
        return try {
            BlockPos(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Handle scroll input (called from client input handler).
     */
    fun handleScroll(stack: ItemStack, level: Level, player: Player, delta: Int) {
        // First try to scroll on display we're looking at
        val hitResult = performRayTrace(level, player)
        if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
            val blockHit = hitResult as BlockHitResult
            val hitPos = blockHit.blockPos
            val state = level.getBlockState(hitPos)
            
            if (state.block is WebDisplayBlock) {
                if (!level.isClientSide) {
                    val blockEntity = level.getBlockEntity(hitPos)
                    if (blockEntity is WebDisplayBlockEntity) {
                        blockEntity.handleScroll(delta)
                    }
                }
                return
            }
        }
        
        // Otherwise use linked display
        val linkedPos = getLinkedDisplay(stack) ?: return
        val state = level.getBlockState(linkedPos)
        if (state.block !is WebDisplayBlock) return
        
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(linkedPos)
            if (blockEntity is WebDisplayBlockEntity) {
                blockEntity.handleScroll(delta)
            }
        }
    }
}

/**
 * Remote Keyboard - A block/item for typing on WebDisplay blocks remotely.
 * 
 * Usage:
 * - Place as block or hold in hand
 * - Shift + Right-click on WebDisplay to link
 * - Right-click to open keyboard input
 * - Type to send keystrokes to linked WebDisplay
 */
class RemoteKeyboardItem(properties: Properties) : Item(properties.stacksTo(1)) {
    
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        
        tooltipComponents.add(Component.translatable("tooltip.opencomputers.remote_keyboard.desc")
            .withStyle(ChatFormatting.GRAY))
        
        val linkedPos = getLinkedDisplay(stack)
        if (linkedPos != null) {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.remote_keyboard.linked",
                linkedPos.x, linkedPos.y, linkedPos.z)
                .withStyle(ChatFormatting.GREEN))
        } else {
            tooltipComponents.add(Component.translatable("tooltip.opencomputers.remote_keyboard.unlinked")
                .withStyle(ChatFormatting.YELLOW))
        }
    }
    
    /**
     * Right-click to open keyboard input or link to display.
     */
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        
        if (player.isShiftKeyDown) {
            // Try to link to display we're looking at
            val hitResult = performRayTrace(level, player)
            if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
                val blockHit = hitResult as BlockHitResult
                val hitPos = blockHit.blockPos
                val state = level.getBlockState(hitPos)
                
                if (state.block is WebDisplayBlock) {
                    linkToDisplay(stack, hitPos)
                    if (!level.isClientSide) {
                        player.displayClientMessage(
                            Component.translatable("message.opencomputers.remote_keyboard.linked", hitPos.x, hitPos.y, hitPos.z)
                                .withStyle(ChatFormatting.GREEN),
                            true
                        )
                    }
                    level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f)
                    return InteractionResultHolder.success(stack)
                }
            }
        } else {
            // Open keyboard input screen (client-side)
            if (level.isClientSide) {
                openKeyboardScreen(stack, player)
            }
            return InteractionResultHolder.success(stack)
        }
        
        return InteractionResultHolder.pass(stack)
    }
    
    /**
     * Right-click on WebDisplay to link.
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        
        if (state.block is WebDisplayBlock && player.isShiftKeyDown) {
            linkToDisplay(stack, pos)
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.translatable("message.opencomputers.remote_keyboard.linked", pos.x, pos.y, pos.z)
                        .withStyle(ChatFormatting.GREEN),
                    true
                )
            }
            level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.5f)
            return InteractionResult.SUCCESS
        }
        
        return InteractionResult.PASS
    }
    
    private fun performRayTrace(level: Level, player: Player): BlockHitResult? {
        val eyePos = player.eyePosition
        val lookVec = player.lookAngle
        val range = 32.0
        val endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range)
        
        return level.clip(ClipContext(
            eyePos,
            endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ))
    }
    
    /**
     * Link to a display.
     */
    fun linkToDisplay(stack: ItemStack, pos: BlockPos) {
        val posString = "${pos.x},${pos.y},${pos.z}"
        stack.set(ModDataComponents.BOUND_ADDRESS.get(), posString)
    }
    
    /**
     * Get linked display position.
     */
    fun getLinkedDisplay(stack: ItemStack): BlockPos? {
        val posString = stack.get(ModDataComponents.BOUND_ADDRESS.get()) ?: return null
        val parts = posString.split(",")
        if (parts.size != 3) return null
        
        return try {
            BlockPos(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Open keyboard input screen (client-side).
     * Will be implemented with a GUI.
     */
    private fun openKeyboardScreen(stack: ItemStack, player: Player) {
        // This will be implemented client-side to open a text input GUI
        // For now, we'll just send a message
        player.displayClientMessage(
            Component.literal("§7[Remote Keyboard]§r Type in chat with prefix §e!kb §rto send to display"),
            false
        )
    }
    
    /**
     * Send a key event to the linked display.
     */
    fun sendKeyEvent(stack: ItemStack, level: Level, keyCode: Int, char: Char, pressed: Boolean) {
        val linkedPos = getLinkedDisplay(stack) ?: return
        val state = level.getBlockState(linkedPos)
        if (state.block !is WebDisplayBlock) return
        
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(linkedPos)
            if (blockEntity is WebDisplayBlockEntity) {
                if (pressed) {
                    blockEntity.handleKeyPress(keyCode, char)
                } else {
                    blockEntity.handleKeyRelease(keyCode)
                }
            }
        }
    }
    
    /**
     * Send a string of text to the linked display.
     */
    fun sendText(stack: ItemStack, level: Level, text: String) {
        val linkedPos = getLinkedDisplay(stack) ?: return
        val state = level.getBlockState(linkedPos)
        if (state.block !is WebDisplayBlock) return
        
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(linkedPos)
            if (blockEntity is WebDisplayBlockEntity) {
                // Send each character as key press
                for (char in text) {
                    blockEntity.handleKeyPress(char.code, char)
                }
            }
        }
    }
}
