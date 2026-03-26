package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.entity.DroneEntity
import li.cil.oc.common.entity.RobotEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Entity type registration for OpenComputers.
 */
object ModEntities {
    
    private val ENTITIES: DeferredRegister<EntityType<*>> = DeferredRegister.create(
        Registries.ENTITY_TYPE,
        OpenComputers.MOD_ID
    )
    
    // ========================================
    // Entity Types
    // ========================================
    
    val ROBOT: DeferredHolder<EntityType<*>, EntityType<RobotEntity>> = ENTITIES.register("robot") {
        EntityType.Builder.of(::RobotEntity, MobCategory.MISC)
            .sized(0.9f, 0.9f)
            .clientTrackingRange(64)
            .updateInterval(1)
            .fireImmune()
            .build("robot")
    }
    
    val DRONE: DeferredHolder<EntityType<*>, EntityType<DroneEntity>> = ENTITIES.register("drone") {
        EntityType.Builder.of(::DroneEntity, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(64)
            .updateInterval(1)
            .fireImmune()
            .build("drone")
    }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(eventBus: IEventBus) {
        ENTITIES.register(eventBus)
    }
}
