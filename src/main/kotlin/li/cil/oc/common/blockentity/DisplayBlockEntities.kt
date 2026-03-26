package li.cil.oc.common.blockentity

import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import java.util.UUID

/**
 * Screen block entity - displays text from connected computers.
 * 
 * Screens can be multi-block, connecting adjacent screens into one display.
 * Resolution depends on tier and number of connected screens.
 */
class ScreenBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.SCREEN.get(), pos, state), Environment {
    
    companion object {
        // Max resolution per tier (single block)
        val TIER_RESOLUTION = mapOf(
            1 to Pair(50, 16),    // Tier 1: 50x16
            2 to Pair(80, 25),    // Tier 2: 80x25
            3 to Pair(160, 50)    // Tier 3: 160x50
        )
        
        // Max color depth per tier
        val TIER_COLORS = mapOf(
            1 to 1,      // Monochrome
            2 to 16,     // 16 colors
            3 to 256     // 256 colors
        )
    }
    
    // ========================================
    // Screen Data
    // ========================================
    
    private var tier: Int = 1
    
    // Text buffer
    private var width: Int = 50
    private var height: Int = 16
    private var buffer: Array<CharArray> = Array(height) { CharArray(width) { ' ' } }
    private var foregroundColors: Array<IntArray> = Array(height) { IntArray(width) { 0xFFFFFF } }
    private var backgroundColors: Array<IntArray> = Array(height) { IntArray(width) { 0x000000 } }
    
    // Cursor position
    private var cursorX: Int = 0
    private var cursorY: Int = 0
    private var cursorBlink: Boolean = true
    
    // Multi-block
    private var origin: BlockPos? = null  // Origin of multi-block screen
    private var multiWidth: Int = 1
    private var multiHeight: Int = 1
    
    // Touch input
    private var isPrecise: Boolean = false
    private var invertTouchMode: Boolean = false
    
    // ========================================
    // Network
    // ========================================
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("screen", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    // ========================================
    // Screen API
    // ========================================
    
    fun getResolution(): Pair<Int, Int> = Pair(width, height)
    
    fun setResolution(w: Int, h: Int): Boolean {
        val maxRes = TIER_RESOLUTION[tier] ?: Pair(50, 16)
        val newW = w.coerceIn(1, maxRes.first * multiWidth)
        val newH = h.coerceIn(1, maxRes.second * multiHeight)
        
        if (newW != width || newH != height) {
            // Resize buffers
            val newBuffer = Array(newH) { y ->
                CharArray(newW) { x ->
                    if (y < height && x < width) buffer[y][x] else ' '
                }
            }
            val newFg = Array(newH) { y ->
                IntArray(newW) { x ->
                    if (y < height && x < width) foregroundColors[y][x] else 0xFFFFFF
                }
            }
            val newBg = Array(newH) { y ->
                IntArray(newW) { x ->
                    if (y < height && x < width) backgroundColors[y][x] else 0x000000
                }
            }
            
            width = newW
            height = newH
            buffer = newBuffer
            foregroundColors = newFg
            backgroundColors = newBg
            
            setChanged()
            return true
        }
        return false
    }
    
    fun getMaxResolution(): Pair<Int, Int> {
        val maxRes = TIER_RESOLUTION[tier] ?: Pair(50, 16)
        return Pair(maxRes.first * multiWidth, maxRes.second * multiHeight)
    }
    
    fun getColorDepth(): Int = TIER_COLORS[tier] ?: 1
    
    fun set(x: Int, y: Int, value: String, vertical: Boolean = false): Boolean {
        if (x < 0 || y < 0 || y >= height || x >= width) return false
        
        var cx = x
        var cy = y
        
        for (char in value) {
            if (vertical) {
                if (cy >= height) break
                buffer[cy][cx] = char
                cy++
            } else {
                if (cx >= width) break
                buffer[cy][cx] = char
                cx++
            }
        }
        
        setChanged()
        markForSync()
        return true
    }
    
    fun get(x: Int, y: Int): Char? {
        if (x < 0 || y < 0 || y >= height || x >= width) return null
        return buffer[y][x]
    }
    
    fun fill(x: Int, y: Int, w: Int, h: Int, char: Char): Boolean {
        val startX = x.coerceIn(0, width - 1)
        val startY = y.coerceIn(0, height - 1)
        val endX = (x + w).coerceIn(0, width)
        val endY = (y + h).coerceIn(0, height)
        
        for (cy in startY until endY) {
            for (cx in startX until endX) {
                buffer[cy][cx] = char
            }
        }
        
        setChanged()
        markForSync()
        return true
    }
    
    fun copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int): Boolean {
        // Copy a region to another location
        val temp = Array(h) { dy ->
            CharArray(w) { dx ->
                val sx = x + dx
                val sy = y + dy
                if (sx in 0 until width && sy in 0 until height) {
                    buffer[sy][sx]
                } else ' '
            }
        }
        
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val destX = tx + dx
                val destY = ty + dy
                if (destX in 0 until width && destY in 0 until height) {
                    buffer[destY][destX] = temp[dy][dx]
                }
            }
        }
        
        setChanged()
        markForSync()
        return true
    }
    
    fun setForeground(color: Int): Int {
        val old = 0xFFFFFF // Would track current
        // Set for next writes
        return old
    }
    
    fun setBackground(color: Int): Int {
        val old = 0x000000 // Would track current
        // Set for next writes
        return old
    }
    
    // ========================================
    // Touch Handling
    // ========================================
    
    fun handleTouch(player: Player, hitResult: BlockHitResult) {
        val localHit = hitResult.location.subtract(
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble()
        )
        
        // Convert to screen coordinates
        // This depends on the facing direction
        val facing = blockState.getValue(li.cil.oc.common.block.ScreenBlock.FACING)
        
        // Calculate touch coordinates
        val (touchX, touchY) = when (facing) {
            Direction.NORTH -> Pair(1.0 - localHit.x, 1.0 - localHit.y)
            Direction.SOUTH -> Pair(localHit.x, 1.0 - localHit.y)
            Direction.WEST -> Pair(1.0 - localHit.z, 1.0 - localHit.y)
            Direction.EAST -> Pair(localHit.z, 1.0 - localHit.y)
            else -> Pair(localHit.x, localHit.y)
        }
        
        // Convert to character position
        val charX = (touchX * width).toInt().coerceIn(0, width - 1)
        val charY = (touchY * height).toInt().coerceIn(0, height - 1)
        
        // Send touch signal to connected computers
        sendTouchSignal(charX, charY, player)
    }
    
    private fun sendTouchSignal(x: Int, y: Int, player: Player) {
        // Find connected components and send touch signal
        val node = _node ?: return
        
        // Signal format: ("touch", screenAddress, x, y, button, playerName)
        node.sendToReachable("computer.signal", "touch", node.address(), x + 1, y + 1, 0, player.gameProfile.name)
    }
    
    // ========================================
    // Tick
    // ========================================
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                val neighborNode = neighbor.node()
                if (neighborNode != null && _node != null) {
                    _node?.connect(neighborNode)
                }
            }
        }
    }
    
    private fun markForSync() {
        level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
    }
    
    // ========================================
    // Environment Implementation
    // ========================================
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        // Handle screen component messages
    }
    
    // ========================================
    // Persistence
    // ========================================
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        
        tag.putInt("Tier", tier)
        tag.putInt("Width", width)
        tag.putInt("Height", height)
        tag.putInt("CursorX", cursorX)
        tag.putInt("CursorY", cursorY)
        tag.putBoolean("CursorBlink", cursorBlink)
        
        // Save screen buffer (compressed)
        val bufferBuilder = StringBuilder()
        for (row in buffer) {
            bufferBuilder.append(row)
            bufferBuilder.append('\n')
        }
        tag.putString("Buffer", bufferBuilder.toString())
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        tier = tag.getInt("Tier").coerceAtLeast(1)
        width = tag.getInt("Width").coerceAtLeast(1)
        height = tag.getInt("Height").coerceAtLeast(1)
        cursorX = tag.getInt("CursorX")
        cursorY = tag.getInt("CursorY")
        cursorBlink = tag.getBoolean("CursorBlink")
        
        // Load screen buffer
        if (tag.contains("Buffer")) {
            val bufferStr = tag.getString("Buffer")
            val rows = bufferStr.split('\n')
            buffer = Array(height) { y ->
                CharArray(width) { x ->
                    if (y < rows.size && x < rows[y].length) rows[y][x] else ' '
                }
            }
            foregroundColors = Array(height) { IntArray(width) { 0xFFFFFF } }
            backgroundColors = Array(height) { IntArray(width) { 0x000000 } }
        }
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("Width", width)
        tag.putInt("Height", height)
        
        val bufferBuilder = StringBuilder()
        for (row in buffer) {
            bufferBuilder.append(row)
            bufferBuilder.append('\n')
        }
        tag.putString("Buffer", bufferBuilder.toString())
        
        return tag
    }
    
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}

/**
 * Keyboard block entity - receives player input.
 */
class KeyboardBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.KEYBOARD.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var activeUser: UUID? = null
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("keyboard", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun startTyping(player: Player) {
        activeUser = player.uuid
        // Open text input GUI or enable keyboard input mode
    }
    
    fun keyDown(char: Char, code: Int, player: Player) {
        val node = _node ?: return
        node.sendToReachable("computer.signal", "key_down", node.address(), char.code, code, player.gameProfile.name)
    }
    
    fun keyUp(char: Char, code: Int, player: Player) {
        val node = _node ?: return
        node.sendToReachable("computer.signal", "key_up", node.address(), char.code, code, player.gameProfile.name)
    }
    
    fun clipboard(text: String, player: Player) {
        val node = _node ?: return
        node.sendToReachable("computer.signal", "clipboard", node.address(), text, player.gameProfile.name)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Redstone I/O block entity - provides advanced redstone control.
 */
class RedstoneIOBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.REDSTONE_IO.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Output values per side (0-15)
    private val outputs = IntArray(6) { 0 }
    // Input values per side (0-15)
    private val inputs = IntArray(6) { 0 }
    // Bundled cable support
    private val bundledOutputs = Array(6) { IntArray(16) { 0 } }
    private val bundledInputs = Array(6) { IntArray(16) { 0 } }
    
    // Wake threshold for computers
    private var wakeThreshold: Int = 0
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("redstone", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    // ========================================
    // Redstone API
    // ========================================
    
    fun getInput(side: Direction): Int = inputs[side.ordinal]
    
    fun getOutput(side: Direction): Int = outputs[side.ordinal]
    
    fun setOutput(side: Direction, value: Int) {
        val clamped = value.coerceIn(0, 15)
        if (outputs[side.ordinal] != clamped) {
            outputs[side.ordinal] = clamped
            setChanged()
            level?.updateNeighborsAt(blockPos, blockState.block)
        }
    }
    
    fun setAllOutputs(value: Int) {
        val clamped = value.coerceIn(0, 15)
        var changed = false
        for (i in outputs.indices) {
            if (outputs[i] != clamped) {
                outputs[i] = clamped
                changed = true
            }
        }
        if (changed) {
            setChanged()
            level?.updateNeighborsAt(blockPos, blockState.block)
        }
    }
    
    fun getBundledInput(side: Direction, color: Int): Int {
        return bundledInputs[side.ordinal].getOrElse(color) { 0 }
    }
    
    fun setBundledOutput(side: Direction, color: Int, value: Int) {
        val clamped = value.coerceIn(0, 255)
        if (color in 0..15 && bundledOutputs[side.ordinal][color] != clamped) {
            bundledOutputs[side.ordinal][color] = clamped
            setChanged()
        }
    }
    
    fun checkInputChanged() {
        val level = level ?: return
        var changed = false
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val signal = level.getSignal(neighborPos, dir)
            if (inputs[dir.ordinal] != signal) {
                inputs[dir.ordinal] = signal
                changed = true
            }
        }
        
        if (changed) {
            setChanged()
            sendRedstoneSignal()
            
            // Wake connected computer if threshold met
            if (wakeThreshold > 0 && inputs.any { it >= wakeThreshold }) {
                wakeComputer()
            }
        }
    }
    
    private fun sendRedstoneSignal() {
        val node = _node ?: return
        node.sendToReachable("computer.signal", "redstone_changed", node.address())
    }
    
    private fun wakeComputer() {
        val node = _node ?: return
        node.sendToReachable("computer.start")
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "redstone.setOutput" -> {
                val args = message.data()
                if (args.size >= 2) {
                    val side = args[0] as? Int ?: return
                    val value = args[1] as? Int ?: return
                    if (side in 0..5) {
                        setOutput(Direction.entries[side], value)
                    }
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putIntArray("Outputs", outputs)
        tag.putIntArray("Inputs", inputs)
        tag.putInt("WakeThreshold", wakeThreshold)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        tag.getIntArray("Outputs").copyInto(outputs, 0, 0, minOf(6, tag.getIntArray("Outputs").size))
        tag.getIntArray("Inputs").copyInto(inputs, 0, 0, minOf(6, tag.getIntArray("Inputs").size))
        wakeThreshold = tag.getInt("WakeThreshold")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}
