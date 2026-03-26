package li.cil.oc.common.loot

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModItems
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.loot.IGlobalLootModifier
import net.neoforged.neoforge.common.loot.LootModifier
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import kotlin.random.Random

/**
 * Global loot modifier that adds loot disks to dungeon chests.
 */
class LootDiskModifier(
    conditions: Array<LootItemCondition>,
    private val chance: Float
) : LootModifier(conditions) {
    
    companion object {
        val CODEC: MapCodec<LootDiskModifier> = RecordCodecBuilder.mapCodec { instance ->
            codecStart(instance).and(
                com.mojang.serialization.Codec.FLOAT.fieldOf("chance")
                    .forGetter { it.chance }
            ).apply(instance, ::LootDiskModifier)
        }
    }
    
    override fun doApply(generatedLoot: MutableList<ItemStack>, context: LootContext): MutableList<ItemStack> {
        if (Random.nextFloat() < chance) {
            // Add a random loot disk
            val lootDisks = listOf(
                "openos",
                "network",
                "data",
                "builder"
            )
            
            val disk = ItemStack(ModItems.FLOPPY.get())
            // In real implementation, we'd set the disk's filesystem content
            // based on which loot type was selected
            
            generatedLoot.add(disk)
        }
        
        return generatedLoot
    }
    
    override fun codec(): MapCodec<out IGlobalLootModifier> = CODEC
}

/**
 * Registry for loot modifiers.
 */
object ModLootModifiers {
    val LOOT_MODIFIERS: DeferredRegister<MapCodec<out IGlobalLootModifier>> = 
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OpenComputers.MOD_ID)
    
    val LOOT_DISK = LOOT_MODIFIERS.register("loot_disk") { LootDiskModifier.CODEC }
    
    fun register(bus: IEventBus) {
        LOOT_MODIFIERS.register(bus)
    }
}
