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
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Hologram projector block entity.
 *
 * Renders a 3D voxel hologram above the projector block.
 * The hologram is a 48x48x48 voxel grid (Tier 1: 48x48x48, Tier 2: 48x48x48 with color).
 *
 * Each voxel is stored as a byte (0 = empty, 1-255 = palette index for T1;
 * full 32-bit ARGB for T2).
 *
 * Rendering is done client-side by the HologramRenderer.
 */
class HologramBlockEntity(
    pos: BlockPos,
    state: BlockState,
    val tier: Int = 0
) : BlockEntity(ModBlockEntities.HOLOGRAM.get(), pos, state), Environment {

    companion object {
        const val WIDTH = 48
        const val HEIGHT = 48

        // Energy costs
        const val SET_COST = 0.5
        const val FILL_COST = 2.0
        const val CLEAR_COST = 1.0
    }

    private var _node: Node? = null
    override fun node(): Node? = _node

    // Voxel data: [x + WIDTH * (y + HEIGHT * z)]
    val voxels = IntArray(WIDTH * HEIGHT * WIDTH) { 0 }  // 0 = empty

    // Color palette (up to 3 colors for T1, 3 × 16 for T2)
    val palette = Array(3) { intArrayOf(0xFF0000, 0x00FF00, 0x0000FF) }

    // Display scale and translation
    var scale: Float = 1.0f
    var translationX: Float = 0.0f
    var translationY: Float = 0.0f
    var translationZ: Float = 0.0f

    // Whether the hologram is visible at all
    var isVisible: Boolean = true

    // Whether data changed (for dirty checking on client)
    var isDirty = false
        private set

    fun markClean() { isDirty = false }

    // ------- Component API -------

    fun componentMethods(): Map<String, (li.cil.oc.api.machine.Context, Array<Any?>) -> Array<Any?>> = mapOf(
        "clear"           to ::clear,
        "get"             to ::get,
        "set"             to ::set,
        "fill"            to ::fill,
        "copy"            to ::copy,
        "getPaletteColor" to ::getPaletteColor,
        "setPaletteColor" to ::setPaletteColor,
        "setScale"        to ::setScale,
        "getScale"        to ::getScale,
        "setTranslation"  to ::setTranslation,
        "getTranslation"  to ::getTranslation,
        "maxResolution"   to ::maxResolution,
    )

    private fun inBounds(x: Int, y: Int, z: Int): Boolean =
        x in 0 until WIDTH && y in 0 until HEIGHT && z in 0 until WIDTH

    private fun idx(x: Int, y: Int, z: Int) = x + WIDTH * (y + HEIGHT * z)

    private fun clear(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        voxels.fill(0)
        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun get(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "x required")
        val y = (args.getOrNull(1) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "y required")
        val z = (args.getOrNull(2) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "z required")
        if (!inBounds(x, y, z)) return arrayOf(false, "out of bounds")
        return arrayOf(voxels[idx(x, y, z)])
    }

    private fun set(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "x required")
        val y = (args.getOrNull(1) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "y required")
        val z = (args.getOrNull(2) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "z required")
        val value = (args.getOrNull(3) as? Double)?.toInt() ?: 0

        if (!inBounds(x, y, z)) return arrayOf(false, "out of bounds")

        // For tier 1, clamp to palette index 0-3
        val storedValue = if (tier == 0) value.coerceIn(0, 3) else value

        voxels[idx(x, y, z)] = storedValue
        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun fill(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "x required")
        val y = (args.getOrNull(1) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "y required")
        val z = (args.getOrNull(2) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "z required")
        val sx = (args.getOrNull(3) as? Double)?.toInt() ?: return arrayOf(false, "sx required")
        val sy = (args.getOrNull(4) as? Double)?.toInt() ?: return arrayOf(false, "sy required")
        val sz = (args.getOrNull(5) as? Double)?.toInt() ?: return arrayOf(false, "sz required")
        val value = (args.getOrNull(6) as? Double)?.toInt() ?: 0

        val x2 = (x + sx - 1).coerceIn(0, WIDTH - 1)
        val y2 = (y + sy - 1).coerceIn(0, HEIGHT - 1)
        val z2 = (z + sz - 1).coerceIn(0, WIDTH - 1)

        for (xi in x.coerceIn(0, WIDTH - 1)..x2) {
            for (yi in y.coerceIn(0, HEIGHT - 1)..y2) {
                for (zi in z.coerceIn(0, WIDTH - 1)..z2) {
                    voxels[idx(xi, yi, zi)] = value
                }
            }
        }
        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun copy(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "x required")
        val y = (args.getOrNull(1) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "y required")
        val z = (args.getOrNull(2) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "z required")
        val sx = (args.getOrNull(3) as? Double)?.toInt() ?: return arrayOf(false, "sx required")
        val sy = (args.getOrNull(4) as? Double)?.toInt() ?: return arrayOf(false, "sy required")
        val sz = (args.getOrNull(5) as? Double)?.toInt() ?: return arrayOf(false, "sz required")
        val tx = (args.getOrNull(6) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "tx required")
        val ty = (args.getOrNull(7) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "ty required")
        val tz = (args.getOrNull(8) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "tz required")

        val temp = Array(sx) { Array(sy) { IntArray(sz) } }

        // Copy source
        for (xi in 0 until sx) for (yi in 0 until sy) for (zi in 0 until sz) {
            val sx2 = x + xi; val sy2 = y + yi; val sz2 = z + zi
            temp[xi][yi][zi] = if (inBounds(sx2, sy2, sz2)) voxels[idx(sx2, sy2, sz2)] else 0
        }

        // Write to dest
        for (xi in 0 until sx) for (yi in 0 until sy) for (zi in 0 until sz) {
            val dx = tx + xi; val dy = ty + yi; val dz = tz + zi
            if (inBounds(dx, dy, dz)) voxels[idx(dx, dy, dz)] = temp[xi][yi][zi]
        }

        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun getPaletteColor(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val idx = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(0)
        if (idx < 0 || idx >= palette.size) return arrayOf(false, "palette index out of range")
        return arrayOf(palette[0][idx.coerceIn(0, 2)])
    }

    private fun setPaletteColor(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        val palIdx = (args.getOrNull(0) as? Double)?.toInt()?.minus(1) ?: return arrayOf(false, "index required")
        val color = (args.getOrNull(1) as? Double)?.toInt() ?: return arrayOf(false, "color required")
        if (palIdx < 0 || palIdx > 2) return arrayOf(false, "palette index out of 1-3 range")
        val old = palette[0][palIdx]
        palette[0][palIdx] = color and 0xFFFFFF
        isDirty = true
        setChanged()
        return arrayOf(old)
    }

    private fun setScale(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        scale = ((args.getOrNull(0) as? Double)?.toFloat() ?: 1.0f).coerceIn(0.33f, 3.0f)
        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun getScale(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> = arrayOf(scale.toDouble())

    private fun setTranslation(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> {
        translationX = ((args.getOrNull(0) as? Double)?.toFloat() ?: 0f).coerceIn(-0.5f, 0.5f)
        translationY = ((args.getOrNull(1) as? Double)?.toFloat() ?: 0f).coerceIn(-0.5f, 0.5f)
        translationZ = ((args.getOrNull(2) as? Double)?.toFloat() ?: 0f).coerceIn(-0.5f, 0.5f)
        isDirty = true
        setChanged()
        return arrayOf()
    }

    private fun getTranslation(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> =
        arrayOf(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble())

    private fun maxResolution(ctx: li.cil.oc.api.machine.Context, args: Array<Any?>): Array<Any?> =
        arrayOf(WIDTH.toDouble(), HEIGHT.toDouble(), WIDTH.toDouble())

    // ------- Tick -------

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(1000.0)
                .withComponent("hologram", ComponentVisibility.NEIGHBORS)
                .build()
            connectToNetwork()
        }
    }

    private fun connectToNetwork() {
        val world = level ?: return
        if (world.isClientSide) return
        for (dir in Direction.entries) {
            val neighborBE = world.getBlockEntity(blockPos.relative(dir))
            if (neighborBE is Environment) {
                val myNode = _node
                val neighborNode = neighborBE.node()
                if (myNode != null && neighborNode != null) {
                    myNode.connect(neighborNode)
                }
            }
        }
    }

    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}

    // ------- NBT -------

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("Visible", isVisible)
        tag.putFloat("Scale", scale)
        tag.putFloat("TransX", translationX)
        tag.putFloat("TransY", translationY)
        tag.putFloat("TransZ", translationZ)
        
        // Save palette
        val paletteTag = IntArray(3) { palette[0][it] }
        tag.putIntArray("Palette", paletteTag)
        
        // Save voxels as compressed byte array (RLE)
        tag.putByteArray("Voxels", compressVoxels())
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        isVisible = tag.getBoolean("Visible")
        scale = tag.getFloat("Scale").coerceIn(0.33f, 3f).let { if (it == 0f) 1f else it }
        translationX = tag.getFloat("TransX")
        translationY = tag.getFloat("TransY")
        translationZ = tag.getFloat("TransZ")
        
        val paletteArr = tag.getIntArray("Palette")
        if (paletteArr.size == 3) {
            for (i in 0..2) palette[0][i] = paletteArr[i]
        }
        
        val compressed = tag.getByteArray("Voxels")
        if (compressed.isNotEmpty()) {
            decompressVoxels(compressed)
        }
        isDirty = true
    }

    /** Simple RLE compression for voxel data (many voxels are 0) */
    private fun compressVoxels(): ByteArray {
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < voxels.size) {
            val value = voxels[i]
            if (value == 0) {
                var count = 0
                while (i < voxels.size && voxels[i] == 0 && count < 255) { i++; count++ }
                out.add(0) // sentinel
                out.add(count.toByte())
            } else {
                out.add((value and 0xFF).toByte())
                i++
            }
        }
        return out.toByteArray()
    }

    private fun decompressVoxels(data: ByteArray) {
        var i = 0
        var pos = 0
        while (i < data.size && pos < voxels.size) {
            val b = data[i].toInt() and 0xFF
            if (b == 0 && i + 1 < data.size) {
                val count = data[i + 1].toInt() and 0xFF
                for (j in 0 until count) {
                    if (pos < voxels.size) voxels[pos++] = 0
                }
                i += 2
            } else {
                voxels[pos++] = b
                i++
            }
        }
    }
}
