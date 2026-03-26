package li.cil.oc.common.init

import com.mojang.serialization.Codec
import li.cil.oc.OpenComputers
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.UnaryOperator

/**
 * Registration for OpenComputers data components (item data).
 */
object ModDataComponents {
    private val DATA_COMPONENTS: DeferredRegister.DataComponents = 
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, OpenComputers.MOD_ID)
    
    // ========================================
    // Common Components
    // ========================================
    
    /**
     * Stores the tier of an item (0-3).
     */
    val TIER: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        DATA_COMPONENTS.registerComponentType("tier") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
        }
    
    /**
     * Stores machine data (NBT for complex state).
     */
    val MACHINE_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("machine_data") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    /**
     * Stores filesystem data.
     */
    val FILESYSTEM: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("filesystem") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    /**
     * Stores component address.
     */
    val ADDRESS: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        DATA_COMPONENTS.registerComponentType("address") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        }
    
    /**
     * Stores label (display name override).
     */
    val LABEL: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        DATA_COMPONENTS.registerComponentType("label") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        }
    
    /**
     * Stores energy amount.
     */
    val ENERGY: DeferredHolder<DataComponentType<*>, DataComponentType<Double>> =
        DATA_COMPONENTS.registerComponentType("energy") { builder ->
            builder.persistent(Codec.DOUBLE)
                .networkSynchronized(ByteBufCodecs.DOUBLE)
        }
    
    // ========================================
    // EEPROM Components
    // ========================================
    
    /**
     * Stores EEPROM code.
     */
    val EEPROM_CODE: DeferredHolder<DataComponentType<*>, DataComponentType<ByteArray>> =
        DATA_COMPONENTS.registerComponentType("eeprom_code") { builder ->
            builder.persistent(Codec.BYTE_BUFFER.xmap(
                { it.array() },
                { java.nio.ByteBuffer.wrap(it) }
            )).networkSynchronized(ByteBufCodecs.BYTE_ARRAY)
        }
    
    /**
     * Stores EEPROM data.
     */
    val EEPROM_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<ByteArray>> =
        DATA_COMPONENTS.registerComponentType("eeprom_data") { builder ->
            builder.persistent(Codec.BYTE_BUFFER.xmap(
                { it.array() },
                { java.nio.ByteBuffer.wrap(it) }
            )).networkSynchronized(ByteBufCodecs.BYTE_ARRAY)
        }
    
    /**
     * Stores EEPROM label.
     */
    val EEPROM_LABEL: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        DATA_COMPONENTS.registerComponentType("eeprom_label") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        }
    
    /**
     * Whether EEPROM is read-only.
     */
    val EEPROM_READONLY: DeferredHolder<DataComponentType<*>, DataComponentType<Boolean>> =
        DATA_COMPONENTS.registerComponentType("eeprom_readonly") { builder ->
            builder.persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
        }
    
    // ========================================
    // Network Components
    // ========================================
    
    /**
     * Stores linked card channel.
     */
    val LINKED_CHANNEL: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        DATA_COMPONENTS.registerComponentType("linked_channel") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        }
    
    /**
     * Stores wireless signal strength setting.
     */
    val WIRELESS_STRENGTH: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        DATA_COMPONENTS.registerComponentType("wireless_strength") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
        }
    
    // ========================================
    // Robot/Drone Components
    // ========================================
    
    /**
     * Stores robot configuration.
     */
    val ROBOT_CONFIG: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("robot_config") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    /**
     * Stores robot light color.
     */
    val ROBOT_LIGHT_COLOR: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        DATA_COMPONENTS.registerComponentType("robot_light_color") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
        }
    
    /**
     * Stores robot experience.
     */
    val ROBOT_EXPERIENCE: DeferredHolder<DataComponentType<*>, DataComponentType<Double>> =
        DATA_COMPONENTS.registerComponentType("robot_experience") { builder ->
            builder.persistent(Codec.DOUBLE)
                .networkSynchronized(ByteBufCodecs.DOUBLE)
        }
    
    // ========================================
    // Tablet Components
    // ========================================
    
    /**
     * Stores tablet running state.
     */
    val TABLET_RUNNING: DeferredHolder<DataComponentType<*>, DataComponentType<Boolean>> =
        DATA_COMPONENTS.registerComponentType("tablet_running") { builder ->
            builder.persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
        }
    
    // ========================================
    // Database Components
    // ========================================
    
    /**
     * Stores database entries.
     */
    val DATABASE_ENTRIES: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("database_entries") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    // ========================================
    // Print Components
    // ========================================
    
    /**
     * Stores 3D print shape data.
     */
    val PRINT_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("print_data") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    // ========================================
    // Nanomachine Components
    // ========================================
    
    /**
     * Stores nanomachine configuration.
     */
    val NANOMACHINE_CONFIG: DeferredHolder<DataComponentType<*>, DataComponentType<CompoundTag>> =
        DATA_COMPONENTS.registerComponentType("nanomachine_config") { builder ->
            builder.persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        }
    
    // ========================================
    // Color Components
    // ========================================
    
    /**
     * Stores item/block color.
     */
    val COLOR: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        DATA_COMPONENTS.registerComponentType("color") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
        }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        DATA_COMPONENTS.register(bus)
        OpenComputers.LOGGER.debug("Registered data components")
    }
}
