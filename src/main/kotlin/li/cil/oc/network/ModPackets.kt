package li.cil.oc.network

import li.cil.oc.OpenComputers
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.UUID

/**
 * Network packet registration and handling for OpenComputers.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object ModPackets {
    
    @SubscribeEvent
    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(OpenComputers.MOD_ID)
        
        // Client to Server packets
        registrar.playToServer(
            ComputerPowerPacket.TYPE,
            ComputerPowerPacket.CODEC,
            ::handleComputerPower
        )
        
        registrar.playToServer(
            KeyboardInputPacket.TYPE,
            KeyboardInputPacket.CODEC,
            ::handleKeyboardInput
        )
        
        registrar.playToServer(
            ComponentInteractPacket.TYPE,
            ComponentInteractPacket.CODEC,
            ::handleComponentInteract
        )
        
        registrar.playToServer(
            RobotMovePacket.TYPE,
            RobotMovePacket.CODEC,
            ::handleRobotMove
        )
        
        // Server to Client packets
        registrar.playToClient(
            ScreenUpdatePacket.TYPE,
            ScreenUpdatePacket.CODEC,
            ::handleScreenUpdate
        )
        
        registrar.playToClient(
            ComputerStatePacket.TYPE,
            ComputerStatePacket.CODEC,
            ::handleComputerState
        )
        
        registrar.playToClient(
            HologramUpdatePacket.TYPE,
            HologramUpdatePacket.CODEC,
            ::handleHologramUpdate
        )
        
        registrar.playToClient(
            SoundEffectPacket.TYPE,
            SoundEffectPacket.CODEC,
            ::handleSoundEffect
        )
        
        registrar.playToClient(
            EnergyUpdatePacket.TYPE,
            EnergyUpdatePacket.CODEC,
            ::handleEnergyUpdate
        )
    }
    
    // === Packet Handlers ===
    
    private fun handleComputerPower(packet: ComputerPowerPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player()
            val level = player.level() as? net.minecraft.server.level.ServerLevel ?: return@enqueueWork
            
            val blockEntity = level.getBlockEntity(packet.pos)
            // Toggle computer power based on packet
        }
    }
    
    private fun handleKeyboardInput(packet: KeyboardInputPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player()
            val level = player.level() as? net.minecraft.server.level.ServerLevel ?: return@enqueueWork
            
            // Forward keyboard input to the appropriate screen/computer
            val blockEntity = level.getBlockEntity(packet.screenPos)
            // Send key event to connected computer
        }
    }
    
    private fun handleComponentInteract(packet: ComponentInteractPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player()
            val level = player.level() as? net.minecraft.server.level.ServerLevel ?: return@enqueueWork
            
            // Handle component interaction
        }
    }
    
    private fun handleRobotMove(packet: RobotMovePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Handle robot movement command
        }
    }
    
    private fun handleScreenUpdate(packet: ScreenUpdatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update screen display on client
            val level = Minecraft.getInstance().level ?: return@enqueueWork
            val blockEntity = level.getBlockEntity(packet.pos)
            // Update screen buffer
        }
    }
    
    private fun handleComputerState(packet: ComputerStatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update computer state on client
        }
    }
    
    private fun handleHologramUpdate(packet: HologramUpdatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update hologram display
        }
    }
    
    private fun handleSoundEffect(packet: SoundEffectPacket, context: IPayloadContext) {
        context.enqueueWork {
            // Play sound effect
        }
    }
    
    private fun handleEnergyUpdate(packet: EnergyUpdatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update energy display
        }
    }
    
    // === Send Helpers ===
    
    fun sendToServer(packet: CustomPacketPayload) {
        PacketDistributor.sendToServer(packet)
    }
    
    fun sendToPlayer(player: ServerPlayer, packet: CustomPacketPayload) {
        PacketDistributor.sendToPlayer(player, packet)
    }
    
    fun sendToAllTracking(level: Level, pos: BlockPos, packet: CustomPacketPayload) {
        val chunk = level.getChunkAt(pos)
        // PacketDistributor.sendToPlayersTrackingChunk(level as ServerLevel, chunk.pos, packet)
    }
    
    fun sendToAll(packet: CustomPacketPayload) {
        PacketDistributor.sendToAllPlayers(packet)
    }
}

// === Packet Definitions ===

/**
 * Client -> Server: Toggle computer power
 */
data class ComputerPowerPacket(
    val pos: BlockPos,
    val powerOn: Boolean
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ComputerPowerPacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "computer_power")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ComputerPowerPacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeBoolean(packet.powerOn)
            },
            { buf ->
                ComputerPowerPacket(
                    buf.readBlockPos(),
                    buf.readBoolean()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Client -> Server: Keyboard input
 */
data class KeyboardInputPacket(
    val screenPos: BlockPos,
    val char: Int,
    val code: Int,
    val isPressed: Boolean,
    val player: UUID
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<KeyboardInputPacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "keyboard_input")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, KeyboardInputPacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.screenPos)
                buf.writeInt(packet.char)
                buf.writeInt(packet.code)
                buf.writeBoolean(packet.isPressed)
                buf.writeUUID(packet.player)
            },
            { buf ->
                KeyboardInputPacket(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readUUID()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Client -> Server: Component interaction
 */
data class ComponentInteractPacket(
    val componentAddress: String,
    val method: String,
    val args: ByteArray
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ComponentInteractPacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "component_interact")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ComponentInteractPacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeUtf(packet.componentAddress)
                buf.writeUtf(packet.method)
                buf.writeByteArray(packet.args)
            },
            { buf ->
                ComponentInteractPacket(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readByteArray()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentInteractPacket) return false
        return componentAddress == other.componentAddress &&
               method == other.method &&
               args.contentEquals(other.args)
    }
    
    override fun hashCode(): Int {
        var result = componentAddress.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}

/**
 * Client -> Server: Robot movement command
 */
data class RobotMovePacket(
    val entityId: Int,
    val direction: Int  // 0-5 for Direction ordinal, 6 = turn left, 7 = turn right
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<RobotMovePacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "robot_move")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, RobotMovePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeInt(packet.entityId)
                buf.writeInt(packet.direction)
            },
            { buf ->
                RobotMovePacket(
                    buf.readInt(),
                    buf.readInt()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Server -> Client: Screen content update
 */
data class ScreenUpdatePacket(
    val pos: BlockPos,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val data: ByteArray  // Encoded screen data
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ScreenUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "screen_update")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ScreenUpdatePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeInt(packet.x)
                buf.writeInt(packet.y)
                buf.writeInt(packet.width)
                buf.writeInt(packet.height)
                buf.writeByteArray(packet.data)
            },
            { buf ->
                ScreenUpdatePacket(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readByteArray()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenUpdatePacket) return false
        return pos == other.pos &&
               x == other.x && y == other.y &&
               width == other.width && height == other.height &&
               data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Server -> Client: Computer state change
 */
data class ComputerStatePacket(
    val pos: BlockPos,
    val isRunning: Boolean,
    val energy: Double,
    val maxEnergy: Double,
    val componentCount: Int
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ComputerStatePacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "computer_state")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ComputerStatePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeBoolean(packet.isRunning)
                buf.writeDouble(packet.energy)
                buf.writeDouble(packet.maxEnergy)
                buf.writeInt(packet.componentCount)
            },
            { buf ->
                ComputerStatePacket(
                    buf.readBlockPos(),
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readInt()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Server -> Client: Hologram projector update
 */
data class HologramUpdatePacket(
    val pos: BlockPos,
    val data: ByteArray,  // Compressed voxel data
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val offsetZ: Float
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<HologramUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "hologram_update")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, HologramUpdatePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeByteArray(packet.data)
                buf.writeFloat(packet.scale)
                buf.writeFloat(packet.offsetX)
                buf.writeFloat(packet.offsetY)
                buf.writeFloat(packet.offsetZ)
            },
            { buf ->
                HologramUpdatePacket(
                    buf.readBlockPos(),
                    buf.readByteArray(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HologramUpdatePacket) return false
        return pos == other.pos &&
               data.contentEquals(other.data) &&
               scale == other.scale &&
               offsetX == other.offsetX &&
               offsetY == other.offsetY &&
               offsetZ == other.offsetZ
    }
    
    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + scale.hashCode()
        return result
    }
}

/**
 * Server -> Client: Play a sound effect
 */
data class SoundEffectPacket(
    val pos: BlockPos,
    val soundId: String,
    val volume: Float,
    val pitch: Float
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<SoundEffectPacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "sound_effect")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SoundEffectPacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeUtf(packet.soundId)
                buf.writeFloat(packet.volume)
                buf.writeFloat(packet.pitch)
            },
            { buf ->
                SoundEffectPacket(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readFloat(),
                    buf.readFloat()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Server -> Client: Energy level update
 */
data class EnergyUpdatePacket(
    val pos: BlockPos,
    val stored: Double,
    val capacity: Double,
    val throughput: Double
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<EnergyUpdatePacket>(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "energy_update")
        )
        
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, EnergyUpdatePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeBlockPos(packet.pos)
                buf.writeDouble(packet.stored)
                buf.writeDouble(packet.capacity)
                buf.writeDouble(packet.throughput)
            },
            { buf ->
                EnergyUpdatePacket(
                    buf.readBlockPos(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
                )
            }
        )
    }
    
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

/**
 * Helper object for creating screen update packets with delta encoding.
 */
object ScreenPacketHelper {
    
    /**
     * Encode screen data for network transmission.
     * Uses run-length encoding for efficiency.
     */
    fun encodeScreenData(
        buffer: Array<Array<Char>>,
        foreground: Array<Array<Int>>,
        background: Array<Array<Int>>,
        width: Int,
        height: Int
    ): ByteArray {
        val output = mutableListOf<Byte>()
        
        // Simple encoding: char + fg + bg for each cell
        // Could be optimized with RLE or delta encoding
        for (y in 0 until height) {
            for (x in 0 until width) {
                val char = buffer[y][x]
                val fg = foreground[y][x]
                val bg = background[y][x]
                
                // Character (2 bytes for unicode support)
                output.add((char.code shr 8).toByte())
                output.add((char.code and 0xFF).toByte())
                
                // Foreground color (3 bytes RGB)
                output.add((fg shr 16).toByte())
                output.add((fg shr 8).toByte())
                output.add((fg and 0xFF).toByte())
                
                // Background color (3 bytes RGB)
                output.add((bg shr 16).toByte())
                output.add((bg shr 8).toByte())
                output.add((bg and 0xFF).toByte())
            }
        }
        
        return output.toByteArray()
    }
    
    /**
     * Decode screen data from network packet.
     */
    fun decodeScreenData(
        data: ByteArray,
        width: Int,
        height: Int,
        buffer: Array<Array<Char>>,
        foreground: Array<Array<Int>>,
        background: Array<Array<Int>>
    ) {
        var index = 0
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (index + 8 > data.size) return
                
                // Character
                val charHigh = data[index++].toInt() and 0xFF
                val charLow = data[index++].toInt() and 0xFF
                buffer[y][x] = ((charHigh shl 8) or charLow).toChar()
                
                // Foreground
                val fgR = data[index++].toInt() and 0xFF
                val fgG = data[index++].toInt() and 0xFF
                val fgB = data[index++].toInt() and 0xFF
                foreground[y][x] = (fgR shl 16) or (fgG shl 8) or fgB
                
                // Background
                val bgR = data[index++].toInt() and 0xFF
                val bgG = data[index++].toInt() and 0xFF
                val bgB = data[index++].toInt() and 0xFF
                background[y][x] = (bgR shl 16) or (bgG shl 8) or bgB
            }
        }
    }
}

/**
 * Helper for encoding hologram data.
 */
object HologramPacketHelper {
    
    /**
     * Encode hologram voxel data with compression.
     */
    fun encodeHologramData(
        voxels: Array<Array<Array<Int>>>,
        width: Int,
        height: Int,
        depth: Int
    ): ByteArray {
        val output = mutableListOf<Byte>()
        
        // Run-length encode the voxel data
        var runValue = 0
        var runLength = 0
        
        for (z in 0 until depth) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val value = voxels[z][y][x]
                    
                    if (value == runValue && runLength < 255) {
                        runLength++
                    } else {
                        if (runLength > 0) {
                            output.add(runLength.toByte())
                            output.add(runValue.toByte())
                        }
                        runValue = value
                        runLength = 1
                    }
                }
            }
        }
        
        // Flush remaining run
        if (runLength > 0) {
            output.add(runLength.toByte())
            output.add(runValue.toByte())
        }
        
        return output.toByteArray()
    }
    
    /**
     * Decode hologram voxel data.
     */
    fun decodeHologramData(
        data: ByteArray,
        width: Int,
        height: Int,
        depth: Int,
        voxels: Array<Array<Array<Int>>>
    ) {
        var dataIndex = 0
        var x = 0
        var y = 0
        var z = 0
        
        while (dataIndex + 1 < data.size) {
            val runLength = data[dataIndex++].toInt() and 0xFF
            val value = data[dataIndex++].toInt() and 0xFF
            
            for (i in 0 until runLength) {
                if (z >= depth) return
                
                voxels[z][y][x] = value
                
                x++
                if (x >= width) {
                    x = 0
                    y++
                    if (y >= height) {
                        y = 0
                        z++
                    }
                }
            }
        }
    }
}
