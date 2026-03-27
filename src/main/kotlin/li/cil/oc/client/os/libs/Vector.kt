package li.cil.oc.client.os.libs

import kotlin.math.*

/**
 * 2D and 3D vector math library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Vector.lua
 */
object Vector {
    
    /**
     * 2D Vector class.
     */
    data class Vec2(val x: Double, val y: Double) {
        constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
        constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
        
        operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
        operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
        operator fun times(scalar: Double) = Vec2(x * scalar, y * scalar)
        operator fun times(scalar: Int) = this * scalar.toDouble()
        operator fun div(scalar: Double) = Vec2(x / scalar, y / scalar)
        operator fun unaryMinus() = Vec2(-x, -y)
        
        val length: Double get() = sqrt(x * x + y * y)
        val lengthSquared: Double get() = x * x + y * y
        
        fun normalize(): Vec2 {
            val len = length
            return if (len > 0) this / len else Vec2(0.0, 0.0)
        }
        
        fun dot(other: Vec2): Double = x * other.x + y * other.y
        
        fun cross(other: Vec2): Double = x * other.y - y * other.x
        
        fun distance(other: Vec2): Double = (this - other).length
        
        fun angle(): Double = atan2(y, x)
        
        fun angleTo(other: Vec2): Double = atan2(other.y - y, other.x - x)
        
        fun rotate(angle: Double): Vec2 {
            val cos = cos(angle)
            val sin = sin(angle)
            return Vec2(x * cos - y * sin, x * sin + y * cos)
        }
        
        fun lerp(other: Vec2, t: Double): Vec2 {
            return Vec2(x + (other.x - x) * t, y + (other.y - y) * t)
        }
        
        fun floor(): Vec2 = Vec2(floor(x), floor(y))
        fun ceil(): Vec2 = Vec2(ceil(x), ceil(y))
        fun round(): Vec2 = Vec2(kotlin.math.round(x), kotlin.math.round(y))
        
        fun toInt(): Pair<Int, Int> = Pair(x.toInt(), y.toInt())
        
        override fun toString() = "Vec2($x, $y)"
        
        companion object {
            val ZERO = Vec2(0.0, 0.0)
            val ONE = Vec2(1.0, 1.0)
            val UP = Vec2(0.0, -1.0)
            val DOWN = Vec2(0.0, 1.0)
            val LEFT = Vec2(-1.0, 0.0)
            val RIGHT = Vec2(1.0, 0.0)
            
            fun fromAngle(angle: Double, length: Double = 1.0): Vec2 {
                return Vec2(cos(angle) * length, sin(angle) * length)
            }
        }
    }
    
    /**
     * 3D Vector class.
     */
    data class Vec3(val x: Double, val y: Double, val z: Double) {
        constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
        constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble())
        
        operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
        operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
        operator fun times(scalar: Double) = Vec3(x * scalar, y * scalar, z * scalar)
        operator fun times(scalar: Int) = this * scalar.toDouble()
        operator fun div(scalar: Double) = Vec3(x / scalar, y / scalar, z / scalar)
        operator fun unaryMinus() = Vec3(-x, -y, -z)
        
        val length: Double get() = sqrt(x * x + y * y + z * z)
        val lengthSquared: Double get() = x * x + y * y + z * z
        
        fun normalize(): Vec3 {
            val len = length
            return if (len > 0) this / len else Vec3(0.0, 0.0, 0.0)
        }
        
        fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
        
        fun cross(other: Vec3): Vec3 = Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
        
        fun distance(other: Vec3): Double = (this - other).length
        
        fun lerp(other: Vec3, t: Double): Vec3 {
            return Vec3(
                x + (other.x - x) * t,
                y + (other.y - y) * t,
                z + (other.z - z) * t
            )
        }
        
        fun rotateX(angle: Double): Vec3 {
            val cos = cos(angle)
            val sin = sin(angle)
            return Vec3(x, y * cos - z * sin, y * sin + z * cos)
        }
        
        fun rotateY(angle: Double): Vec3 {
            val cos = cos(angle)
            val sin = sin(angle)
            return Vec3(x * cos + z * sin, y, -x * sin + z * cos)
        }
        
        fun rotateZ(angle: Double): Vec3 {
            val cos = cos(angle)
            val sin = sin(angle)
            return Vec3(x * cos - y * sin, x * sin + y * cos, z)
        }
        
        fun floor(): Vec3 = Vec3(floor(x), floor(y), floor(z))
        fun ceil(): Vec3 = Vec3(ceil(x), ceil(y), ceil(z))
        fun round(): Vec3 = Vec3(kotlin.math.round(x), kotlin.math.round(y), kotlin.math.round(z))
        
        fun toInt(): Triple<Int, Int, Int> = Triple(x.toInt(), y.toInt(), z.toInt())
        
        fun xy(): Vec2 = Vec2(x, y)
        fun xz(): Vec2 = Vec2(x, z)
        fun yz(): Vec2 = Vec2(y, z)
        
        override fun toString() = "Vec3($x, $y, $z)"
        
        companion object {
            val ZERO = Vec3(0.0, 0.0, 0.0)
            val ONE = Vec3(1.0, 1.0, 1.0)
            val UP = Vec3(0.0, 1.0, 0.0)
            val DOWN = Vec3(0.0, -1.0, 0.0)
            val NORTH = Vec3(0.0, 0.0, -1.0)
            val SOUTH = Vec3(0.0, 0.0, 1.0)
            val EAST = Vec3(1.0, 0.0, 0.0)
            val WEST = Vec3(-1.0, 0.0, 0.0)
        }
    }
    
    // Factory functions
    fun vec2(x: Double, y: Double) = Vec2(x, y)
    fun vec2(x: Int, y: Int) = Vec2(x, y)
    fun vec3(x: Double, y: Double, z: Double) = Vec3(x, y, z)
    fun vec3(x: Int, y: Int, z: Int) = Vec3(x, y, z)
    
    // Utility functions
    fun distance2D(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    fun distance3D(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    fun manhattanDistance2D(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        return abs(x2 - x1) + abs(y2 - y1)
    }
    
    fun manhattanDistance3D(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
        return abs(x2 - x1) + abs(y2 - y1) + abs(z2 - z1)
    }
    
    fun chebyshevDistance2D(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        return maxOf(abs(x2 - x1), abs(y2 - y1))
    }
    
    fun chebyshevDistance3D(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
        return maxOf(abs(x2 - x1), abs(y2 - y1), abs(z2 - z1))
    }
}
