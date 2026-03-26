package li.cil.oc.common.container

import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.init.ModMenus
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.SlotItemHandler

/**
 * Base container menu for OpenComputers GUIs.
 */
abstract class OCContainerMenu(
    menuType: MenuType<*>,
    containerId: Int,
    protected val playerInventory: Inventory,
    protected val levelAccess: ContainerLevelAccess
) : AbstractContainerMenu(menuType, containerId) {
    
    protected var hotbarStart: Int = 0
    protected var inventoryStart: Int = 0
    protected var playerSlotCount: Int = 0
    
    /**
     * Adds the player inventory and hotbar slots.
     */
    protected fun addPlayerInventory(inventory: Inventory, xOffset: Int = 8, yOffset: Int = 84) {
        inventoryStart = slots.size
        
        // Main inventory (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(inventory, col + row * 9 + 9, xOffset + col * 18, yOffset + row * 18))
            }
        }
        
        hotbarStart = slots.size
        
        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(inventory, col, xOffset + col * 18, yOffset + 58))
        }
        
        playerSlotCount = slots.size - inventoryStart
    }
    
    /**
     * Handles shift-clicking items between slots.
     */
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        
        if (slot.hasItem()) {
            val stack = slot.item
            result = stack.copy()
            
            if (index < inventoryStart) {
                // Move from container to player inventory
                if (!moveItemStackTo(stack, inventoryStart, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Move from player inventory to container
                if (!moveItemStackTo(stack, 0, inventoryStart, false)) {
                    return ItemStack.EMPTY
                }
            }
            
            if (stack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            
            if (stack.count == result.count) {
                return ItemStack.EMPTY
            }
            
            slot.onTake(player, stack)
        }
        
        return result
    }
    
    override fun stillValid(player: Player): Boolean {
        return levelAccess.evaluate({ level, pos ->
            level.getBlockEntity(pos)?.let { be ->
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
            } ?: false
        }, true)
    }
}

/**
 * Computer case menu.
 */
class CaseMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val data: ContainerData = SimpleContainerData(4)
) : OCContainerMenu(ModMenus.CASE.get(), containerId, playerInventory, levelAccess) {
    
    var blockEntity: CaseBlockEntity? = null
    
    constructor(
        containerId: Int,
        playerInventory: Inventory,
        pos: BlockPos
    ) : this(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos)) {
        blockEntity = playerInventory.player.level().getBlockEntity(pos) as? CaseBlockEntity
    }
    
    init {
        // Component slots (arranged in a column on the left)
        blockEntity?.let { be ->
            // Add component slots
            for (i in 0 until be.inventory.slots.coerceAtMost(12)) {
                val row = i % 6
                val col = i / 6
                addSlot(ComponentSlot(be.inventory, i, 8 + col * 18, 18 + row * 18))
            }
        }
        
        // Add player inventory
        addPlayerInventory(playerInventory, 8, 140)
        
        // Add data slots for syncing
        addDataSlots(data)
    }
    
    // Data accessors
    val isRunning: Boolean get() = data.get(0) != 0
    val energy: Int get() = data.get(1)
    val maxEnergy: Int get() = data.get(2)
    val tier: Int get() = data.get(3)
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): CaseMenu {
            val pos = buf.readBlockPos()
            return CaseMenu(containerId, playerInventory, pos)
        }
    }
}

/**
 * Screen/keyboard menu (read-only display).
 */
class ScreenMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess
) : OCContainerMenu(ModMenus.SCREEN.get(), containerId, playerInventory, levelAccess) {
    
    // Screen menus have no inventory slots - they just display the screen
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): ScreenMenu {
            val pos = buf.readBlockPos()
            return ScreenMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos))
        }
    }
}

/**
 * Disk drive menu.
 */
class DiskDriveMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val diskSlot: IItemHandler? = null
) : OCContainerMenu(ModMenus.DISK_DRIVE.get(), containerId, playerInventory, levelAccess) {
    
    init {
        diskSlot?.let { handler ->
            addSlot(DiskSlot(handler, 0, 80, 35))
        }
        
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): DiskDriveMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DiskDriveBlockEntity
            return DiskDriveMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.diskSlot)
        }
    }
}

/**
 * Raid menu (3 disk slots).
 */
class RaidMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val diskSlots: IItemHandler? = null
) : OCContainerMenu(ModMenus.RAID.get(), containerId, playerInventory, levelAccess) {
    
    init {
        diskSlots?.let { handler ->
            for (i in 0 until 3) {
                addSlot(DiskSlot(handler, i, 62 + i * 18, 35))
            }
        }
        
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): RaidMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RaidBlockEntity
            return RaidMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.diskSlots)
        }
    }
}

/**
 * Server rack menu.
 */
class RackMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val serverSlots: IItemHandler? = null
) : OCContainerMenu(ModMenus.RACK.get(), containerId, playerInventory, levelAccess) {
    
    init {
        serverSlots?.let { handler ->
            for (i in 0 until 4) {
                addSlot(ServerSlot(handler, i, 80, 17 + i * 18))
            }
        }
        
        addPlayerInventory(playerInventory, 8, 104)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): RackMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.RackBlockEntity
            return RackMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.serverSlots)
        }
    }
}

/**
 * Assembler menu.
 */
class AssemblerMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val componentSlots: IItemHandler? = null
) : OCContainerMenu(ModMenus.ASSEMBLER.get(), containerId, playerInventory, levelAccess) {
    
    init {
        componentSlots?.let { handler ->
            // Case slot (center top)
            addSlot(ComponentSlot(handler, 0, 80, 17))
            
            // Component grid (4x4)
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    addSlot(ComponentSlot(handler, 1 + row * 4 + col, 44 + col * 18, 35 + row * 18))
                }
            }
        }
        
        addPlayerInventory(playerInventory, 8, 120)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): AssemblerMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.AssemblerBlockEntity
            return AssemblerMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.componentSlots)
        }
    }
}

/**
 * Disassembler menu.
 */
class DisassemblerMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val inputSlot: IItemHandler? = null,
    private val outputSlots: IItemHandler? = null
) : OCContainerMenu(ModMenus.DISASSEMBLER.get(), containerId, playerInventory, levelAccess) {
    
    init {
        inputSlot?.let { handler ->
            addSlot(ComponentSlot(handler, 0, 26, 35))
        }
        
        outputSlots?.let { handler ->
            for (i in 0 until 9) {
                addSlot(OutputSlot(handler, i, 98 + (i % 3) * 18, 17 + (i / 3) * 18))
            }
        }
        
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): DisassemblerMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.DisassemblerBlockEntity
            return DisassemblerMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.inputSlot, be?.outputSlots)
        }
    }
}

/**
 * Charger menu.
 */
class ChargerMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val chargeSlots: IItemHandler? = null
) : OCContainerMenu(ModMenus.CHARGER.get(), containerId, playerInventory, levelAccess) {
    
    init {
        chargeSlots?.let { handler ->
            // 2x4 grid of charging slots
            for (row in 0 until 2) {
                for (col in 0 until 4) {
                    addSlot(ChargeSlot(handler, row * 4 + col, 53 + col * 18, 26 + row * 18))
                }
            }
        }
        
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): ChargerMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.ChargerBlockEntity
            return ChargerMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.chargeSlots)
        }
    }
}

/**
 * Transposer menu.
 */
class TransposerMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess
) : OCContainerMenu(ModMenus.TRANSPOSER.get(), containerId, playerInventory, levelAccess) {
    
    init {
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): TransposerMenu {
            val pos = buf.readBlockPos()
            return TransposerMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos))
        }
    }
}

/**
 * Printer menu.
 */
class PrinterMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val materialSlot: IItemHandler? = null,
    private val inkSlot: IItemHandler? = null,
    private val outputSlot: IItemHandler? = null
) : OCContainerMenu(ModMenus.PRINTER.get(), containerId, playerInventory, levelAccess) {
    
    init {
        materialSlot?.let { handler ->
            addSlot(MaterialSlot(handler, 0, 26, 26))
        }
        
        inkSlot?.let { handler ->
            addSlot(InkSlot(handler, 0, 26, 53))
        }
        
        outputSlot?.let { handler ->
            addSlot(OutputSlot(handler, 0, 134, 35))
        }
        
        addPlayerInventory(playerInventory, 8, 84)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): PrinterMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val be = level.getBlockEntity(pos) as? li.cil.oc.common.blockentity.PrinterBlockEntity
            return PrinterMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), be?.materialSlot, be?.inkSlot, be?.outputSlot)
        }
    }
}

/**
 * Robot menu.
 */
class RobotMenu(
    containerId: Int,
    playerInventory: Inventory,
    levelAccess: ContainerLevelAccess,
    private val componentSlots: IItemHandler? = null,
    private val mainInventory: IItemHandler? = null
) : OCContainerMenu(ModMenus.ROBOT.get(), containerId, playerInventory, levelAccess) {
    
    init {
        componentSlots?.let { handler ->
            // Internal component slots on the left
            for (i in 0 until handler.slots.coerceAtMost(8)) {
                addSlot(ComponentSlot(handler, i, 8, 18 + i * 18))
            }
        }
        
        mainInventory?.let { handler ->
            // Robot inventory (4x4 grid)
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    if (index < handler.slots) {
                        addSlot(RobotSlot(handler, index, 62 + col * 18, 18 + row * 18))
                    }
                }
            }
        }
        
        addPlayerInventory(playerInventory, 8, 104)
    }
    
    companion object {
        fun create(containerId: Int, playerInventory: Inventory, buf: net.minecraft.network.FriendlyByteBuf): RobotMenu {
            // Robots might use entity ID instead of block position
            val entityId = buf.readVarInt()
            val level = playerInventory.player.level()
            val entity = level.getEntity(entityId) as? li.cil.oc.common.entity.RobotEntity
            return RobotMenu(
                containerId, 
                playerInventory, 
                ContainerLevelAccess.NULL, 
                entity?.componentInventory, 
                entity?.mainInventory
            )
        }
    }
}

// ========================================
// Custom Slot Types
// ========================================

/**
 * Slot for OC components (CPU, RAM, etc.)
 */
class ComponentSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        // Check if item is a valid component
        return true // TODO: actual component validation
    }
}

/**
 * Slot for floppy disks.
 */
class DiskSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        // Accept floppy disks and HDDs
        return true // TODO: validate disk item
    }
    
    override fun getMaxStackSize(): Int = 1
}

/**
 * Slot for server blades.
 */
class ServerSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        return true // TODO: validate server item
    }
    
    override fun getMaxStackSize(): Int = 1
}

/**
 * Output-only slot (cannot insert).
 */
class OutputSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean = false
}

/**
 * Slot for items that can be charged.
 */
class ChargeSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        // Accept items that can hold OC energy
        return true // TODO: check for energy capability
    }
}

/**
 * Slot for printer material (chamelium).
 */
class MaterialSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        return true // TODO: validate chamelium
    }
}

/**
 * Slot for ink cartridges.
 */
class InkSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        return true // TODO: validate ink
    }
}

/**
 * Slot for robot inventory items.
 */
class RobotSlot(
    handler: IItemHandler,
    index: Int,
    x: Int,
    y: Int
) : SlotItemHandler(handler, index, x, y)

/**
 * Simple container data implementation.
 */
class SimpleContainerData(private val size: Int) : ContainerData {
    private val values = IntArray(size)
    
    override fun get(index: Int): Int = values[index]
    override fun set(index: Int, value: Int) { values[index] = value }
    override fun getCount(): Int = size
}
