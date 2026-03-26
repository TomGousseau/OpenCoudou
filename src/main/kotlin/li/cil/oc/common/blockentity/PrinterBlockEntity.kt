package li.cil.oc.common.blockentity

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
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
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * 3D Printer block entity.
 *
 * Consumes Chamelium and ink to print custom 3D-shaped blocks.
 * Printing is defined by a table of {shape, color, texture} specs sent from Lua.
 *
 * The printer operates in two steps:
 *   1. The Lua program calls printer.setLabel, printer.addShape, etc. to configure the model
 *   2. The Lua program calls printer.commit() to start printing
 *   3. The print completes after PRINT_TICKS ticks and outputs a printed item
 *
 * Printed items use the generic "printed_block" item with NBT encoding the shapes.
 */
class PrinterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.PRINTER.get(), pos, state), Environment {

    companion object {
        const val PRINT_TICKS = 200       // ~10 seconds
        const val MAX_SHAPES = 24         // Max shapes per model
        const val MAX_STATES = 2          // Off/On states
        const val CHAMELIUM_SLOT = 0
        const val INK_SLOT = 1
        const val OUTPUT_SLOT = 2
        const val INV_SIZE = 3
    }

    // ------- State -------

    private var _node: Node? = null
    override fun node(): Node? = _node

    private val inventory = Array(INV_SIZE) { ItemStack.EMPTY }

    // Current model being built
    private var label: String = ""
    private var tooltip: String = ""
    private val shapes = mutableListOf<PrintShape>()
    private var isButtonMode = false      // If true, cycles states on use
    private var lightLevel = 0

    // Printing progress
    var isPrinting = false
        private set
    var printProgress = 0      // 0 - PRINT_TICKS
        private set

    // Number of copies to print
    private var pendingCopies = 0

    // ------- Component API -------

    data class PrintShape(
        val minX: Int, val minY: Int, val minZ: Int,
        val maxX: Int, val maxY: Int, val maxZ: Int,
        val texture: String,
        val tint: Int,          // ARGB color tint
        val state: Int          // 0 = off-state, 1 = on-state
    )

    fun componentMethods(): Map<String, (Context, Array<Any?>) -> Array<Any?>> = mapOf(
        "setLabel"        to ::setLabel,
        "getLabel"        to ::getLabel,
        "setTooltip"      to ::setTooltip,
        "getTooltip"      to ::getTooltip,
        "addShape"        to ::addShape,
        "resetShapes"     to ::resetShapes,
        "setButtonMode"   to ::setButtonMode,
        "isButtonMode"    to ::isButtonMode_,
        "setLightLevel"   to ::setLightLevel,
        "getLightLevel"   to ::getLightLevel,
        "commit"          to ::commit,
        "reset"           to ::reset_,
        "status"          to ::status,
        "getProgress"     to ::getProgress,
        "getChameliumLevel" to ::getChameliumLevel,
        "getInkLevel"     to ::getInkLevel,
    )

    private fun setLabel(ctx: Context, args: Array<Any?>): Array<Any?> {
        val l = args.getOrNull(0) as? String ?: ""
        label = l.take(32)
        return arrayOf(true)
    }

    private fun getLabel(ctx: Context, args: Array<Any?>): Array<Any?> = arrayOf(label)

    private fun setTooltip(ctx: Context, args: Array<Any?>): Array<Any?> {
        tooltip = (args.getOrNull(0) as? String ?: "").take(256)
        return arrayOf(true)
    }

    private fun getTooltip(ctx: Context, args: Array<Any?>): Array<Any?> = arrayOf(tooltip)

    private fun addShape(ctx: Context, args: Array<Any?>): Array<Any?> {
        if (shapes.size >= MAX_SHAPES) {
            return arrayOf(false, "too many shapes (max $MAX_SHAPES)")
        }

        val minX = (args.getOrNull(0) as? Double)?.toInt() ?: return arrayOf(false, "minX required")
        val minY = (args.getOrNull(1) as? Double)?.toInt() ?: return arrayOf(false, "minY required")
        val minZ = (args.getOrNull(2) as? Double)?.toInt() ?: return arrayOf(false, "minZ required")
        val maxX = (args.getOrNull(3) as? Double)?.toInt() ?: return arrayOf(false, "maxX required")
        val maxY = (args.getOrNull(4) as? Double)?.toInt() ?: return arrayOf(false, "maxY required")
        val maxZ = (args.getOrNull(5) as? Double)?.toInt() ?: return arrayOf(false, "maxZ required")
        val texture = args.getOrNull(6) as? String ?: "minecraft:block/stone"
        val tint = (args.getOrNull(7) as? Double)?.toInt() ?: 0xFFFFFF
        val state = (args.getOrNull(8) as? Double)?.toInt() ?: 0

        // Validate bounds
        if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 16 || maxZ > 16) {
            return arrayOf(false, "shape coordinates must be in range 0-16")
        }
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return arrayOf(false, "invalid shape dimensions (min >= max)")
        }

        shapes.add(PrintShape(minX, minY, minZ, maxX, maxY, maxZ, texture, tint, state.coerceIn(0, MAX_STATES - 1)))
        return arrayOf(true, shapes.size)
    }

    private fun resetShapes(ctx: Context, args: Array<Any?>): Array<Any?> {
        shapes.clear()
        return arrayOf(true)
    }

    private fun setButtonMode(ctx: Context, args: Array<Any?>): Array<Any?> {
        isButtonMode = args.getOrNull(0) as? Boolean ?: false
        return arrayOf(true)
    }

    private fun isButtonMode_(ctx: Context, args: Array<Any?>): Array<Any?> = arrayOf(isButtonMode)

    private fun setLightLevel(ctx: Context, args: Array<Any?>): Array<Any?> {
        lightLevel = ((args.getOrNull(0) as? Double)?.toInt() ?: 0).coerceIn(0, 15)
        return arrayOf(true)
    }

    private fun getLightLevel(ctx: Context, args: Array<Any?>): Array<Any?> = arrayOf(lightLevel)

    private fun commit(ctx: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Double)?.toInt() ?: 1
        if (isPrinting) return arrayOf(false, "already printing")
        if (shapes.isEmpty()) return arrayOf(false, "no shapes defined")

        // Check materials
        val chameliumCount = countChamelium()
        val inkLevel = getInkLevelValue()

        val shapesCount = shapes.filter { it.state == 0 }.size
        val requiredChamelium = shapesCount
        val requiredInk = shapesCount * 0.01f

        if (chameliumCount < requiredChamelium) {
            return arrayOf(false, "insufficient Chamelium (need $requiredChamelium)")
        }
        if (inkLevel < requiredInk) {
            return arrayOf(false, "insufficient ink (need ${(requiredInk * 100).toInt()}%)")
        }

        // Consume materials
        consumeChamelium(requiredChamelium)
        consumeInk(requiredInk)

        // Start printing
        isPrinting = true
        printProgress = 0
        pendingCopies = count.coerceIn(1, 64)
        setChanged()

        return arrayOf(true)
    }

    private fun reset_(ctx: Context, args: Array<Any?>): Array<Any?> {
        if (isPrinting) return arrayOf(false, "printer is busy")
        shapes.clear()
        label = ""
        tooltip = ""
        isButtonMode = false
        lightLevel = 0
        return arrayOf(true)
    }

    private fun status(ctx: Context, args: Array<Any?>): Array<Any?> {
        return when {
            isPrinting -> arrayOf("busy", printProgress.toDouble() / PRINT_TICKS)
            pendingCopies > 0 -> arrayOf("idle", 1.0, pendingCopies)
            else -> arrayOf("idle", 1.0)
        }
    }

    private fun getProgress(ctx: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(if (isPrinting) printProgress.toDouble() / PRINT_TICKS else 1.0)
    }

    private fun getChameliumLevel(ctx: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(countChamelium())
    }

    private fun getInkLevel(ctx: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(getInkLevelValue())
    }

    // ------- Material Helpers -------

    private fun countChamelium(): Int {
        val stack = inventory[CHAMELIUM_SLOT]
        // Check for Chamelium item
        return if (!stack.isEmpty && stack.`is`(ModItems.CHAMELIUM)) stack.count else 0
    }

    private fun getInkLevelValue(): Float {
        val stack = inventory[INK_SLOT]
        if (stack.isEmpty || !stack.`is`(ModItems.INK_CARTRIDGE)) return 0f
        return stack.tag?.getFloat("InkLevel") ?: 1.0f
    }

    private fun consumeChamelium(amount: Int) {
        val stack = inventory[CHAMELIUM_SLOT]
        if (!stack.isEmpty) {
            stack.shrink(amount)
            if (stack.isEmpty) inventory[CHAMELIUM_SLOT] = ItemStack.EMPTY
        }
        setChanged()
    }

    private fun consumeInk(amount: Float) {
        val stack = inventory[INK_SLOT]
        if (!stack.isEmpty && stack.`is`(ModItems.INK_CARTRIDGE)) {
            val tag = stack.orCreateTag
            val newLevel = (tag.getFloat("InkLevel") - amount).coerceAtLeast(0f)
            tag.putFloat("InkLevel", newLevel)
            if (newLevel <= 0f) {
                // Replace with empty cartridge
                inventory[INK_SLOT] = ItemStack(ModItems.INK_CARTRIDGE_EMPTY)
            }
        }
        setChanged()
    }

    // ------- Tick -------

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return

        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(500.0)
                .withComponent("printer", ComponentVisibility.NEIGHBORS)
                .build()
            connectToNetwork()
        }

        if (!isPrinting) return

        printProgress++

        if (printProgress >= PRINT_TICKS) {
            // Finish printing
            isPrinting = false
            printProgress = 0
            pendingCopies--

            // Create the printed item
            val printedItem = createPrintedItem()
            val outputSlot = inventory[OUTPUT_SLOT]
            if (outputSlot.isEmpty) {
                inventory[OUTPUT_SLOT] = printedItem
            } else if (outputSlot.`is`(printedItem.item) && ItemStack.isSameItemSameComponents(outputSlot, printedItem)) {
                outputSlot.grow(1)
            } else {
                // Output slot full — drop on ground
                val serverLevel = level as? ServerLevel
                serverLevel?.addFreshEntity(
                    net.minecraft.world.entity.item.ItemEntity(
                        level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, printedItem
                    )
                )
            }

            setChanged()

            // Signal completion
            _node?.sendToReachable("computer.signal", "print_complete")

            // Start next copy if any
            if (pendingCopies > 0) {
                // Check if we have materials for next copy
                val shapesCount = shapes.filter { it.state == 0 }.size
                val requiredChamelium = shapesCount
                val requiredInk = shapesCount * 0.01f
                if (countChamelium() >= requiredChamelium && getInkLevelValue() >= requiredInk) {
                    consumeChamelium(requiredChamelium)
                    consumeInk(requiredInk)
                    isPrinting = true
                    printProgress = 0
                } else {
                    pendingCopies = 0
                    _node?.sendToReachable("computer.signal", "print_paused", "out_of_materials")
                }
            }
        }
    }

    private fun createPrintedItem(): ItemStack {
        val stack = ItemStack(ModItems.PRINTED_BLOCK)
        val tag = stack.orCreateTag

        tag.putString("Label", label)
        tag.putString("Tooltip", tooltip)
        tag.putBoolean("ButtonMode", isButtonMode)
        tag.putInt("LightLevel", lightLevel)

        val shapesTag = ListTag()
        for (shape in shapes) {
            val shapeTag = CompoundTag()
            shapeTag.putInt("MinX", shape.minX)
            shapeTag.putInt("MinY", shape.minY)
            shapeTag.putInt("MinZ", shape.minZ)
            shapeTag.putInt("MaxX", shape.maxX)
            shapeTag.putInt("MaxY", shape.maxY)
            shapeTag.putInt("MaxZ", shape.maxZ)
            shapeTag.putString("Texture", shape.texture)
            shapeTag.putInt("Tint", shape.tint)
            shapeTag.putInt("State", shape.state)
            shapesTag.add(shapeTag)
        }
        tag.put("Shapes", shapesTag)

        if (label.isNotBlank()) {
            stack.hoverName = net.minecraft.network.chat.Component.literal(label)
        }

        return stack
    }

    // ------- Environment -------

    private fun connectToNetwork() {
        val world = level ?: return
        val pos = blockPos
        if (world.isClientSide) return

        for (dir in Direction.entries) {
            val neighborPos = pos.relative(dir)
            val neighborBE = world.getBlockEntity(neighborPos)
            if (neighborBE is Environment) {
                val neighborNode = neighborBE.node()
                val myNode = _node
                if (neighborNode != null && myNode != null) {
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
        tag.putBoolean("Printing", isPrinting)
        tag.putInt("PrintProgress", printProgress)
        tag.putInt("PendingCopies", pendingCopies)
        tag.putString("Label", label)
        tag.putString("Tooltip", tooltip)
        tag.putBoolean("ButtonMode", isButtonMode)
        tag.putInt("LightLevel", lightLevel)

        // Save inventory
        val invTag = ListTag()
        for (i in inventory.indices) {
            if (!inventory[i].isEmpty) {
                val slotTag = CompoundTag()
                slotTag.putByte("Slot", i.toByte())
                inventory[i].save(slotTag, registries)
                invTag.add(slotTag)
            }
        }
        tag.put("Inventory", invTag)

        // Save shapes
        val shapesTag = ListTag()
        for (shape in shapes) {
            val shapeTag = CompoundTag()
            shapeTag.putInt("MinX", shape.minX)
            shapeTag.putInt("MinY", shape.minY)
            shapeTag.putInt("MinZ", shape.minZ)
            shapeTag.putInt("MaxX", shape.maxX)
            shapeTag.putInt("MaxY", shape.maxY)
            shapeTag.putInt("MaxZ", shape.maxZ)
            shapeTag.putString("Texture", shape.texture)
            shapeTag.putInt("Tint", shape.tint)
            shapeTag.putInt("State", shape.state)
            shapesTag.add(shapeTag)
        }
        tag.put("Shapes", shapesTag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        isPrinting = tag.getBoolean("Printing")
        printProgress = tag.getInt("PrintProgress")
        pendingCopies = tag.getInt("PendingCopies")
        label = tag.getString("Label")
        tooltip = tag.getString("Tooltip")
        isButtonMode = tag.getBoolean("ButtonMode")
        lightLevel = tag.getInt("LightLevel")

        // Load inventory
        val invTag = tag.getList("Inventory", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until invTag.size) {
            val slotTag = invTag.getCompound(i)
            val slot = slotTag.getByte("Slot").toInt()
            if (slot in inventory.indices) {
                inventory[slot] = ItemStack.parseOptional(registries, slotTag)
            }
        }

        // Load shapes
        shapes.clear()
        val shapesTag = tag.getList("Shapes", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until shapesTag.size) {
            val shapeTag = shapesTag.getCompound(i)
            shapes.add(PrintShape(
                shapeTag.getInt("MinX"), shapeTag.getInt("MinY"), shapeTag.getInt("MinZ"),
                shapeTag.getInt("MaxX"), shapeTag.getInt("MaxY"), shapeTag.getInt("MaxZ"),
                shapeTag.getString("Texture"),
                shapeTag.getInt("Tint"),
                shapeTag.getInt("State")
            ))
        }
    }

    // Simple item inventory access
    fun getItem(slot: Int): ItemStack = if (slot in inventory.indices) inventory[slot] else ItemStack.EMPTY
    fun setItem(slot: Int, stack: ItemStack) {
        if (slot in inventory.indices) {
            inventory[slot] = stack
            setChanged()
        }
    }
}

// Reference to ModItems — forward decl since it's in registration files
private object ModItems {
    // These reference the actual item registry objects
    val CHAMELIUM get() = li.cil.oc.common.init.ModItems.CHAMELIUM.get()
    val INK_CARTRIDGE get() = li.cil.oc.common.init.ModItems.INK_CARTRIDGE.get()
    val INK_CARTRIDGE_EMPTY get() = li.cil.oc.common.init.ModItems.INK_CARTRIDGE_EMPTY.get()
    val PRINTED_BLOCK get() = li.cil.oc.common.init.ModItems.PRINTED_BLOCK.get()
}
