package li.cil.oc.client.os.libs

import net.minecraft.core.Direction

/**
 * Side/Direction constants for SkibidiOS2.
 * Compatible with SkibidiLuaOS Sides.lua and OpenComputers sides API.
 */
object Sides {
    
    // Side constants (matches Minecraft directions)
    const val DOWN = 0
    const val UP = 1
    const val NORTH = 2
    const val SOUTH = 3
    const val WEST = 4
    const val EAST = 5
    
    // Alternate names
    const val BOTTOM = DOWN
    const val TOP = UP
    const val BACK = NORTH
    const val FRONT = SOUTH
    const val RIGHT = WEST
    const val LEFT = EAST
    
    // Relative sides (for robots)
    const val NEGY = DOWN
    const val POSY = UP
    const val NEGZ = NORTH
    const val POSZ = SOUTH
    const val NEGX = WEST
    const val POSX = EAST
    
    // Forward/Back for entities
    const val FORWARD = FRONT
    const val BACKWARD = BACK
    
    /**
     * Get opposite side.
     */
    fun opposite(side: Int): Int {
        return when (side) {
            DOWN -> UP
            UP -> DOWN
            NORTH -> SOUTH
            SOUTH -> NORTH
            WEST -> EAST
            EAST -> WEST
            else -> side
        }
    }
    
    /**
     * Get side name.
     */
    fun name(side: Int): String {
        return when (side) {
            DOWN -> "down"
            UP -> "up"
            NORTH -> "north"
            SOUTH -> "south"
            WEST -> "west"
            EAST -> "east"
            else -> "unknown"
        }
    }
    
    /**
     * Get side from name.
     */
    fun fromName(name: String): Int {
        return when (name.lowercase()) {
            "down", "bottom", "negy" -> DOWN
            "up", "top", "posy" -> UP
            "north", "back", "negz" -> NORTH
            "south", "front", "posz", "forward" -> SOUTH
            "west", "right", "negx" -> WEST
            "east", "left", "posx" -> EAST
            else -> -1
        }
    }
    
    /**
     * Get side as Minecraft Direction.
     */
    fun toDirection(side: Int): Direction {
        return when (side) {
            DOWN -> Direction.DOWN
            UP -> Direction.UP
            NORTH -> Direction.NORTH
            SOUTH -> Direction.SOUTH
            WEST -> Direction.WEST
            EAST -> Direction.EAST
            else -> Direction.UP
        }
    }
    
    /**
     * Get side from Minecraft Direction.
     */
    fun fromDirection(direction: Direction): Int {
        return when (direction) {
            Direction.DOWN -> DOWN
            Direction.UP -> UP
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST
            Direction.EAST -> EAST
        }
    }
    
    /**
     * Rotate side horizontally (around Y axis).
     */
    fun rotateY(side: Int, times: Int = 1): Int {
        if (side == UP || side == DOWN) return side
        
        val horizontal = arrayOf(NORTH, EAST, SOUTH, WEST)
        val idx = horizontal.indexOf(side)
        if (idx < 0) return side
        
        return horizontal[(idx + times).mod(4)]
    }
    
    /**
     * Get rotation from one horizontal side to another.
     */
    fun getRotation(from: Int, to: Int): Int {
        if (from == UP || from == DOWN || to == UP || to == DOWN) return 0
        
        val horizontal = arrayOf(NORTH, EAST, SOUTH, WEST)
        val fromIdx = horizontal.indexOf(from)
        val toIdx = horizontal.indexOf(to)
        if (fromIdx < 0 || toIdx < 0) return 0
        
        return (toIdx - fromIdx).mod(4)
    }
    
    /**
     * Get relative side based on facing direction.
     */
    fun toRelative(side: Int, facing: Int): Int {
        if (side == UP || side == DOWN) return side
        if (facing == UP || facing == DOWN) return side
        
        val rotation = getRotation(SOUTH, facing)
        return rotateY(side, rotation)
    }
    
    /**
     * Get absolute side from relative based on facing.
     */
    fun toAbsolute(relative: Int, facing: Int): Int {
        if (relative == UP || relative == DOWN) return relative
        if (facing == UP || facing == DOWN) return relative
        
        val rotation = getRotation(SOUTH, facing)
        return rotateY(relative, -rotation)
    }
    
    /**
     * Offset coordinates by side.
     */
    fun offset(x: Int, y: Int, z: Int, side: Int): Triple<Int, Int, Int> {
        return when (side) {
            DOWN -> Triple(x, y - 1, z)
            UP -> Triple(x, y + 1, z)
            NORTH -> Triple(x, y, z - 1)
            SOUTH -> Triple(x, y, z + 1)
            WEST -> Triple(x - 1, y, z)
            EAST -> Triple(x + 1, y, z)
            else -> Triple(x, y, z)
        }
    }
    
    /**
     * Get unit vector for side.
     */
    fun unitVector(side: Int): Triple<Int, Int, Int> {
        return when (side) {
            DOWN -> Triple(0, -1, 0)
            UP -> Triple(0, 1, 0)
            NORTH -> Triple(0, 0, -1)
            SOUTH -> Triple(0, 0, 1)
            WEST -> Triple(-1, 0, 0)
            EAST -> Triple(1, 0, 0)
            else -> Triple(0, 0, 0)
        }
    }
    
    /**
     * Check if side is horizontal.
     */
    fun isHorizontal(side: Int): Boolean {
        return side in arrayOf(NORTH, SOUTH, WEST, EAST)
    }
    
    /**
     * Check if side is vertical.
     */
    fun isVertical(side: Int): Boolean {
        return side == UP || side == DOWN
    }
    
    /**
     * Get all sides.
     */
    fun all(): List<Int> = listOf(DOWN, UP, NORTH, SOUTH, WEST, EAST)
    
    /**
     * Get horizontal sides only.
     */
    fun horizontal(): List<Int> = listOf(NORTH, SOUTH, WEST, EAST)
    
    /**
     * Get vertical sides only.
     */
    fun vertical(): List<Int> = listOf(DOWN, UP)
}
