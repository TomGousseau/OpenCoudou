package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentVisibility
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nanomachines component — represents the nanomachine injector item/block.
 *
 * When a player uses a nanomachine injector, they gain a NanomachineHandler
 * which manages active behaviors.
 *
 * Behaviors are indexed 1-N and can be toggled on/off.
 * Each enabled behavior consumes power from the player each tick.
 *
 * Behaviors:
 *   1    - Strength
 *   2    - Speed
 *   4    - Jump Boost
 *   8    - Night Vision
 *   16   - Haste
 *   32   - Water Breathing
 *   64   - Regeneration
 *   128  - Fire Resistance
 *   256  - Invisibility
 *   512  - Absorption
 *   1024 - Attack boost
 *   2048 - Slow fall
 */
class NanomachinesComponent : ComponentBase("nanomachines") {

    companion object {
        const val MAX_BEHAVIORS = 12
        const val POWER_PER_BEHAVIOR_PER_TICK = 0.5
    }

    // Track players with nanomachines by UUID
    private val playerHandlers = mutableMapOf<UUID, NanomachineHandler>()

    override fun methods() = mapOf(
        "getConfiguration"   to ::getConfiguration,
        "setConfiguration"   to ::setConfiguration,
        "getActive"          to ::getActive,
        "setActive"          to ::setActive,
        "zap"                to ::zap,
        "transmit"           to ::transmit,
        "sense"              to ::sense,
        "input"              to ::nanomachineInput,
        "getOutput"          to ::getOutput,
    )

    fun onPlayerNearby(player: Player) {
        // Called when a nearby player has nanomachines — initialize handler
        val uuid = player.uuid
        if (uuid !in playerHandlers) {
            playerHandlers[uuid] = NanomachineHandler(player)
        }
        playerHandlers[uuid]?.tick()
    }

    // ------- API Methods -------

    private fun getConfiguration(ctx: Context, args: Array<Any?>): Array<Any?> {
        val playerUUID = ctx.signal("request_player_uuid") // placeholder - in real impl would resolve player
        val config = IntArray(MAX_BEHAVIORS) { it + 1 }
        return arrayOf(config.map { it.toDouble() }.toTypedArray())
    }

    private fun setConfiguration(ctx: Context, args: Array<Any?>): Array<Any?> {
        // Randomize which behaviors map to which slots
        return arrayOf(true)
    }

    private fun getActive(ctx: Context, args: Array<Any?>): Array<Any?> {
        val idx = (args.getOrNull(0) as? Double)?.toInt() ?: return arrayOf(false, "behavior index required")
        if (idx < 1 || idx > MAX_BEHAVIORS) return arrayOf(false, "index out of range 1-$MAX_BEHAVIORS")
        return arrayOf(false) // Default off
    }

    private fun setActive(ctx: Context, args: Array<Any?>): Array<Any?> {
        val idx = (args.getOrNull(0) as? Double)?.toInt() ?: return arrayOf(false, "behavior index required")
        val active = args.getOrNull(1) as? Boolean ?: false
        if (idx < 1 || idx > MAX_BEHAVIORS) return arrayOf(false, "index out of range 1-$MAX_BEHAVIORS")
        return arrayOf(true)
    }

    /** Zap nearby entities */
    private fun zap(ctx: Context, args: Array<Any?>): Array<Any?> {
        // Would deal lightning damage to nearby entities
        return arrayOf(true)
    }

    /** Transmit a nanomachine signal to another player */
    private fun transmit(ctx: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Double)?.toInt() ?: return arrayOf(false, "port required")
        val data = args.getOrNull(1) ?: ""
        // Would signal nearby players with nanomachines
        return arrayOf(true)
    }

    /** Sense nearby entities */
    private fun sense(ctx: Context, args: Array<Any?>): Array<Any?> {
        // Returns a list of nearby entity info
        return arrayOf(emptyArray<Any>())
    }

    private fun nanomachineInput(ctx: Context, args: Array<Any?>): Array<Any?> {
        // Get the last received signal
        return arrayOf(null)
    }

    private fun getOutput(ctx: Context, args: Array<Any?>): Array<Any?> {
        val channel = (args.getOrNull(0) as? Double)?.toInt() ?: 0
        return arrayOf(false)
    }
}

/**
 * Manages applied nanomachine effects for a single player.
 */
class NanomachineHandler(private val player: Player) {
    private val enabledBehaviors = BooleanArray(NanomachinesComponent.MAX_BEHAVIORS)
    private var power = 100.0

    val behaviors = arrayOf(
        { applyPotion(MobEffects.STRENGTH, 0) },        // 1: Strength
        { applyPotion(MobEffects.MOVEMENT_SPEED, 0) },  // 2: Speed
        { applyPotion(MobEffects.JUMP, 0) },            // 3: Jump Boost
        { applyPotion(MobEffects.NIGHT_VISION, 0) },    // 4: Night Vision
        { applyPotion(MobEffects.DIG_SPEED, 0) },       // 5: Haste
        { applyPotion(MobEffects.WATER_BREATHING, 0) }, // 6: Water Breathing
        { applyPotion(MobEffects.REGENERATION, 0) },    // 7: Regeneration
        { applyPotion(MobEffects.FIRE_RESISTANCE, 0) }, // 8: Fire Resistance
        { applyPotion(MobEffects.INVISIBILITY, 0) },    // 9: Invisibility
        { applyPotion(MobEffects.ABSORPTION, 0) },      // 10: Absorption
        { player.attackStrengthScale },                  // 11: Attack boost (semi-passive)
        { applyPotion(MobEffects.SLOW_FALLING, 0) },    // 12: Slow fall
    )

    private fun applyPotion(effect: net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, amplifier: Int) {
        player.addEffect(MobEffectInstance(effect, 3 * 20, amplifier, true, false))
    }

    fun tick() {
        val activeCount = enabledBehaviors.count { it }
        val cost = activeCount * NanomachinesComponent.POWER_PER_BEHAVIOR_PER_TICK

        if (power < cost) {
            // Out of power — disable all
            enabledBehaviors.fill(false)
            return
        }

        power -= cost

        for (i in enabledBehaviors.indices) {
            if (enabledBehaviors[i] && i < behaviors.size) {
                behaviors[i]()
            }
        }
    }

    fun setEnabled(index: Int, enabled: Boolean): Boolean {
        if (index !in enabledBehaviors.indices) return false
        enabledBehaviors[index] = enabled
        return true
    }

    fun isEnabled(index: Int): Boolean {
        if (index !in enabledBehaviors.indices) return false
        return enabledBehaviors[index]
    }

    fun addPower(amount: Double) {
        power = (power + amount).coerceAtMost(100.0)
    }
}
