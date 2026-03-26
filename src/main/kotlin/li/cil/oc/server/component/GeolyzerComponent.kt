package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.Tags
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Geolyzer component — scans the surrounding terrain, providing block/hardness data
 * in a 3D volume around the block.  Mimics the original OC Geolyzer.
 *
 * scan(x, z, y, w, d, h, ignoreReplaceable) → flat float array of hardness values
 * analyze(side)                              → table with block info for adjacent block
 */
class GeolyzerComponent(
    private val levelGetter: () -> Level?,
    private val posGetter:   () -> BlockPos
) : ComponentBase("geolyzer") {

    companion object {
        /** Maximum scan volume in blocks */
        const val MAX_SCAN_VOLUME = 64
        /** Maximum scan dimension on any single axis */
        const val MAX_DIM = 8
        /** Hardness value assigned to unloaded chunks */
        const val UNLOADED_HARDNESS = -1.0f
        /** Noise amplitude (proportion of hardness) */
        const val NOISE_RATIO = 0.05f
    }

    override fun methods() = mapOf(
        "scan"    to ::scan,
        "analyze" to ::analyze,
        "isSunlit" to ::isSunlit
    )

    // -----------------------------------------------------------------------
    // Callback implementations
    // -----------------------------------------------------------------------

    @Callback(
        doc = """function(x:number, z:number[, y:number, w:number, d:number, h:number, ignoreReplaceable:boolean]):table
 -- Scans a volume centred on this geolyzer. x/z are column offsets, y is
 -- depth offset (negative = below). w/d/h default to 1. Returns a flat array
 -- of hardness values (one per voxel, x-major then y then z).  Uses a small
 -- amount of noise to prevent exact block identification by hardness alone."""
    )
    fun scan(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "x required")
        val z = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "z required")
        val y = (args.getOrNull(2) as? Number)?.toInt() ?: 0
        val w = (args.getOrNull(3) as? Number)?.toInt()?.coerceIn(1, MAX_DIM) ?: 1
        val d = (args.getOrNull(4) as? Number)?.toInt()?.coerceIn(1, MAX_DIM) ?: 1
        val h = (args.getOrNull(5) as? Number)?.toInt()?.coerceIn(1, MAX_DIM) ?: 1
        val ignoreReplaceable = args.getOrNull(6) as? Boolean ?: false

        val volume = w * d * h
        if (volume > MAX_SCAN_VOLUME) {
            return arrayOf(null, "scan volume too large (max $MAX_SCAN_VOLUME)")
        }

        val level = levelGetter() ?: return arrayOf(null, "no level")
        val origin = posGetter()

        val result = FloatArray(volume)
        var index = 0

        for (bx in x until x + w) {
            for (by in y until y + h) {
                for (bz in z until z + d) {
                    val pos = origin.offset(bx, by, bz)
                    result[index++] = getHardness(level, pos, ignoreReplaceable)
                }
            }
        }

        // Return as a Lua-compatible list (1-indexed table)
        val table = mutableMapOf<Int, Double>()
        for (i in result.indices) {
            table[i + 1] = result[i].toDouble()
        }
        return arrayOf(table)
    }

    @Callback(
        doc = """function(side:number):table -- Analyzes the block on the given side (0-5).
 Returns { name, metadata, hardness, harvestLevel, isAir, isLiquid, isSolid }."""
    )
    fun analyze(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 3
        if (side !in 0..5) return arrayOf(null, "invalid side")

        val level = levelGetter() ?: return arrayOf(null, "no level")
        val origin = posGetter()
        val dir = Direction.from3DDataValue(side)
        val pos = origin.relative(dir)

        if (!level.isLoaded(pos)) return arrayOf(null, "chunk not loaded")

        val state = level.getBlockState(pos)
        val block = state.block

        val registryName = BuiltInRegistries.BLOCK.getKey(block)
        val hardness     = state.getDestroySpeed(level, pos)
        val isAir        = state.isAir
        val isLiquid     = block is LiquidBlock
        val isSolid      = state.isSolid

        // Light level at the target block
        val lightLevel   = level.getLightEmission(pos)

        // Redstone power
        val redstonePower = level.getBestNeighborSignal(pos)

        val info = mapOf(
            "name"          to (registryName?.toString() ?: "minecraft:air"),
            "hardness"      to hardness.toDouble(),
            "isAir"         to isAir,
            "isLiquid"      to isLiquid,
            "isSolid"       to isSolid,
            "lightLevel"    to lightLevel,
            "redstonePower" to redstonePower
        )

        return arrayOf(info)
    }

    @Callback(
        doc = """function([x:number, y:number, z:number]):boolean -- Returns whether the block at
 the given offset (relative to the geolyzer) is exposed to direct skylight."""
    )
    fun isSunlit(context: Context, args: Array<Any?>): Array<Any?> {
        val ox = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val oy = (args.getOrNull(1) as? Number)?.toInt() ?: 0
        val oz = (args.getOrNull(2) as? Number)?.toInt() ?: 0

        val level  = levelGetter() ?: return arrayOf(false)
        val origin = posGetter()
        val pos    = origin.offset(ox, oy, oz)

        if (!level.isLoaded(pos)) return arrayOf(false)

        return arrayOf(level.canSeeSky(pos))
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun getHardness(level: Level, pos: BlockPos, ignoreReplaceable: Boolean): Float {
        if (!level.isLoaded(pos)) return UNLOADED_HARDNESS

        val state: BlockState = level.getBlockState(pos)
        val block = state.block

        if (state.isAir) return 0f

        if (ignoreReplaceable && state.canBeReplaced()) return 0f

        val rawHardness = state.getDestroySpeed(level, pos)
        if (rawHardness < 0f) return Float.MAX_VALUE // bedrock / unbreakable

        // Add small noise to prevent trivial material fingerprinting
        val noise = (Math.random() - 0.5).toFloat() * 2f * NOISE_RATIO * max(rawHardness, 0.1f)
        return (rawHardness + noise).coerceAtLeast(0f)
    }
}
