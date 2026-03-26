package li.cil.oc.common.network

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Network handler for client-server communication.
 * 
 * Uses NeoForge's modern networking API with custom payloads.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object NetworkHandler {
    
    // Packet type IDs
    val COMPUTER_STATE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "computer_state")
    val SCREEN_UPDATE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "screen_update")
    val KEYBOARD_INPUT = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "keyboard_input")
    val HOLOGRAM_UPDATE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "hologram_update")
    val COMPONENT_DATA = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "component_data")
    val ROBOT_MOVE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "robot_move")
    val GUI_ACTION = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "gui_action")
    
    @SubscribeEvent
    @JvmStatic
    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(OpenComputers.MOD_ID)
        
        // Client -> Server packets
        registrar.playToServer(
            KeyboardInputPacket.TYPE,
            KeyboardInputPacket.STREAM_CODEC
        ) { packet, context -> handleKeyboardInput(packet, context) }
        
        registrar.playToServer(
            GuiActionPacket.TYPE,
            GuiActionPacket.STREAM_CODEC
        ) { packet, context -> handleGuiAction(packet, context) }
        
        // Server -> Client packets
        registrar.playToClient(
            ComputerStatePacket.TYPE,
            ComputerStatePacket.STREAM_CODEC
        ) { packet, context -> handleComputerState(packet, context) }
        
        registrar.playToClient(
            ScreenUpdatePacket.TYPE,
            ScreenUpdatePacket.STREAM_CODEC
        ) { packet, context -> handleScreenUpdate(packet, context) }
        
        registrar.playToClient(
            HologramUpdatePacket.TYPE,
            HologramUpdatePacket.STREAM_CODEC
        ) { packet, context -> handleHologramUpdate(packet, context) }
    }
    
    // ========================================
    // Packet Handlers
    // ========================================
    
    private fun handleKeyboardInput(packet: KeyboardInputPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            val level = player.level()
            val blockEntity = level.getBlockEntity(packet.pos)
            
            if (blockEntity is li.cil.oc.common.blockentity.KeyboardBlockEntity) {
                when (packet.inputType) {
                    KeyboardInputType.KEY_DOWN -> blockEntity.keyDown(packet.char, packet.code, player)
                    KeyboardInputType.KEY_UP -> blockEntity.keyUp(packet.char, packet.code, player)
                    KeyboardInputType.CLIPBOARD -> blockEntity.clipboard(packet.clipboard ?: "", player)
                }
            }
        }
    }
    
    private fun handleGuiAction(packet: GuiActionPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            // Handle GUI actions (button presses, slot interactions, etc.)
        }
    }
    
    private fun handleComputerState(packet: ComputerStatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update client-side computer state display
        }
    }
    
    private fun handleScreenUpdate(packet: ScreenUpdatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update client-side screen rendering
        }
    }
    
    private fun handleHologramUpdate(packet: HologramUpdatePacket, context: IPayloadContext) {
        context.enqueueWork {
            // Update client-side hologram rendering
        }
    }
    
    // ========================================
    // Send Methods
    // ========================================
    
    fun sendKeyboardInput(pos: BlockPos, inputType: KeyboardInputType, char: Char, code: Int, clipboard: String? = null) {
        PacketDistributor.sendToServer(KeyboardInputPacket(pos, inputType, char, code, clipboard))
    }
    
    fun sendGuiAction(action: String, data: ByteArray) {
        PacketDistributor.sendToServer(GuiActionPacket(action, data))
    }
    
    fun sendComputerStateToPlayer(player: ServerPlayer, pos: BlockPos, running: Boolean, error: String?) {
        PacketDistributor.sendToPlayer(player, ComputerStatePacket(pos, running, error))
    }
    
    fun sendScreenUpdateToTracking(pos: BlockPos, width: Int, height: Int, buffer: String) {
        PacketDistributor.sendToAllPlayers(ScreenUpdatePacket(pos, width, height, buffer))
    }
}

// ========================================
// Packet Definitions
// ========================================

enum class KeyboardInputType {
    KEY_DOWN, KEY_UP, CLIPBOARD
}

/**
 * Client -> Server: Keyboard input events.
 */
data class KeyboardInputPacket(
    val pos: BlockPos,
    val inputType: KeyboardInputType,
    val char: Char,
    val code: Int,
    val clipboard: String?
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<KeyboardInputPacket>(NetworkHandler.KEYBOARD_INPUT)
        
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, KeyboardInputPacket> = object : StreamCodec<FriendlyByteBuf, KeyboardInputPacket> {
            override fun decode(buffer: FriendlyByteBuf): KeyboardInputPacket {
                return KeyboardInputPacket(
                    buffer.readBlockPos(),
                    KeyboardInputType.entries[buffer.readVarInt()],
                    buffer.readChar(),
                    buffer.readVarInt(),
                    if (buffer.readBoolean()) buffer.readUtf() else null
                )
            }
            
            override fun encode(buffer: FriendlyByteBuf, value: KeyboardInputPacket) {
                buffer.writeBlockPos(value.pos)
                buffer.writeVarInt(value.inputType.ordinal)
                buffer.writeChar(value.char.code)
                buffer.writeVarInt(value.code)
                buffer.writeBoolean(value.clipboard != null)
                value.clipboard?.let { buffer.writeUtf(it) }
            }
        }
    }
    
    override fun type() = TYPE
}

/**
 * Client -> Server: GUI interaction.
 */
data class GuiActionPacket(
    val action: String,
    val data: ByteArray
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<GuiActionPacket>(NetworkHandler.GUI_ACTION)
        
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, GuiActionPacket> = object : StreamCodec<FriendlyByteBuf, GuiActionPacket> {
            override fun decode(buffer: FriendlyByteBuf): GuiActionPacket {
                return GuiActionPacket(
                    buffer.readUtf(),
                    buffer.readByteArray()
                )
            }
            
            override fun encode(buffer: FriendlyByteBuf, value: GuiActionPacket) {
                buffer.writeUtf(value.action)
                buffer.writeByteArray(value.data)
            }
        }
    }
    
    override fun type() = TYPE
}

/**
 * Server -> Client: Computer running state.
 */
data class ComputerStatePacket(
    val pos: BlockPos,
    val running: Boolean,
    val error: String?
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ComputerStatePacket>(NetworkHandler.COMPUTER_STATE)
        
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ComputerStatePacket> = object : StreamCodec<FriendlyByteBuf, ComputerStatePacket> {
            override fun decode(buffer: FriendlyByteBuf): ComputerStatePacket {
                return ComputerStatePacket(
                    buffer.readBlockPos(),
                    buffer.readBoolean(),
                    if (buffer.readBoolean()) buffer.readUtf() else null
                )
            }
            
            override fun encode(buffer: FriendlyByteBuf, value: ComputerStatePacket) {
                buffer.writeBlockPos(value.pos)
                buffer.writeBoolean(value.running)
                buffer.writeBoolean(value.error != null)
                value.error?.let { buffer.writeUtf(it) }
            }
        }
    }
    
    override fun type() = TYPE
}

/**
 * Server -> Client: Screen buffer update.
 */
data class ScreenUpdatePacket(
    val pos: BlockPos,
    val width: Int,
    val height: Int,
    val buffer: String
) : CustomPacketPayload {
    
    companion object {
        val TYPE = CustomPacketPayload.Type<ScreenUpdatePacket>(NetworkHandler.SCREEN_UPDATE)
        
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ScreenUpdatePacket> = object : StreamCodec<FriendlyByteBuf, ScreenUpdatePacket> {
            override fun decode(buffer: FriendlyByteBuf): ScreenUpdatePacket {
                return ScreenUpdatePacket(
                    buffer.readBlockPos(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(32767)
                )
            }
            
            override fun encode(buffer: FriendlyByteBuf, value: ScreenUpdatePacket) {
                buffer.writeBlockPos(value.pos)
                buffer.writeVarInt(value.width)
                buffer.writeVarInt(value.height)
                buffer.writeUtf(value.buffer, 32767)
            }
        }
    }
    
    override fun type() = TYPE
}

/**
 * Server -> Client: Hologram voxel update.
 */
data class HologramUpdatePacket(
    val pos: BlockPos,
    val updateType: HologramUpdateType,
    val data: ByteArray
) : CustomPacketPayload {
    
    enum class HologramUpdateType {
        FULL, SET, CLEAR, FILL
    }
    
    companion object {
        val TYPE = CustomPacketPayload.Type<HologramUpdatePacket>(NetworkHandler.HOLOGRAM_UPDATE)
        
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, HologramUpdatePacket> = object : StreamCodec<FriendlyByteBuf, HologramUpdatePacket> {
            override fun decode(buffer: FriendlyByteBuf): HologramUpdatePacket {
                return HologramUpdatePacket(
                    buffer.readBlockPos(),
                    HologramUpdateType.entries[buffer.readVarInt()],
                    buffer.readByteArray()
                )
            }
            
            override fun encode(buffer: FriendlyByteBuf, value: HologramUpdatePacket) {
                buffer.writeBlockPos(value.pos)
                buffer.writeVarInt(value.updateType.ordinal)
                buffer.writeByteArray(value.data)
            }
        }
    }
    
    override fun type() = TYPE
}
