package com.game.towerstack

import kotlin.math.*

enum class Phase { HOME, READY, PLAYING, PAUSED, GAME_OVER }
enum class Axis { X, Z }
data class Block(val x: Float, val z: Float, val width: Float, val depth: Float, val level: Int)
data class Cut(val kept: Block?, val removed: Block?, val perfect: Boolean)
data class Debris(val block: Block, var age: Float = 0f)
data class Spark(val x: Float, val z: Float, val level: Int, val vx: Float, val vy: Float, var age: Float = 0f)

/** Geometry is entirely in world space. Edge contact has no area and is a miss. */
object Geometry {
    const val PERFECT_FRACTION = 0.035f
    fun cut(top: Block, moving: Block, axis: Axis): Cut {
        val center = if (axis == Axis.X) moving.x else moving.z
        val target = if (axis == Axis.X) top.x else top.z
        val size = if (axis == Axis.X) moving.width else moving.depth
        val support = if (axis == Axis.X) top.width else top.depth
        if (abs(center - target) <= size * PERFECT_FRACTION) {
            return Cut(moving.copy(x = top.x, z = top.z), null, true)
        }
        val lo = max(center - size / 2, target - support / 2)
        val hi = min(center + size / 2, target + support / 2)
        if (hi <= lo) return Cut(null, moving, false)
        val kept = if (axis == Axis.X) moving.copy(x = (lo + hi) / 2, width = hi - lo)
            else moving.copy(z = (lo + hi) / 2, depth = hi - lo)
        val edge = if (center > target) center + size / 2 else center - size / 2
        val seam = if (center > target) hi else lo
        val removed = if (axis == Axis.X) moving.copy(x = (edge + seam) / 2, width = abs(edge - seam))
            else moving.copy(z = (edge + seam) / 2, depth = abs(edge - seam))
        return Cut(kept, removed, false)
    }
}

class GameEngine {
    var phase = Phase.HOME; private set
    var score = 0; private set
    var axis = Axis.X; private set
    val tower = mutableListOf<Block>()
    val debris = mutableListOf<Debris>()
    val sparks = mutableListOf<Spark>()
    var moving = Block(-2.6f, 0f, 2f, 2f, 1); private set
    var camera = 0f; private set
    var perfectTime = 0f; private set
    private var direction = 1f
    private var cooldown = 0f
    private var previousPhase = Phase.READY
    init { reset(); home() }
    fun reset() {
        tower.clear(); tower.add(Block(0f, 0f, 2f, 2f, 0))
        debris.clear(); sparks.clear(); score = 0; axis = Axis.X; direction = 1f
        moving = Block(-2.6f, 0f, 2f, 2f, 1)
        camera = 0f; perfectTime = 0f; cooldown = .22f; phase = Phase.READY
    }
    fun home() { phase = Phase.HOME }
    fun pause() { if (phase == Phase.READY || phase == Phase.PLAYING) { previousPhase = phase; phase = Phase.PAUSED } }
    fun resume() { if (phase == Phase.PAUSED) { phase = previousPhase; cooldown = .2f } }
    fun tick(delta: Float) {
        if (phase == Phase.HOME || phase == Phase.PAUSED) return
        val dt = delta.coerceIn(0f, .05f)
        cooldown = max(0f, cooldown - dt); perfectTime = max(0f, perfectTime - dt)
        camera += (max(0f, moving.level - 4f) - camera) * (1f - exp(-5f * dt))
        debris.forEach { it.age += dt }; debris.removeAll { it.age > 1.5f }
        sparks.forEach { it.age += dt }; sparks.removeAll { it.age > .7f }
        if (phase == Phase.GAME_OVER) return
        val top = tower.last()
        val target = if (axis == Axis.X) top.x else top.z
        var position = (if (axis == Axis.X) moving.x else moving.z) + direction * min(3.2f, 1.35f + score * .045f) * dt
        if (position > target + 2.6f) { position = 2 * (target + 2.6f) - position; direction = -1f }
        if (position < target - 2.6f) { position = 2 * (target - 2.6f) - position; direction = 1f }
        moving = if (axis == Axis.X) moving.copy(x = position) else moving.copy(z = position)
    }
    /** Null means an ignored input; otherwise returns the placement result for feedback. */
    fun place(): Cut? {
        if ((phase != Phase.READY && phase != Phase.PLAYING) || cooldown > 0f) return null
        val result = Geometry.cut(tower.last(), moving, axis)
        result.removed?.let { debris.add(Debris(it)) }
        val kept = result.kept
        if (kept == null) { phase = Phase.GAME_OVER; return result }
        tower.add(kept); score++; phase = Phase.PLAYING; cooldown = .18f
        if (result.perfect) {
            perfectTime = .8f
            repeat(14) { i ->
                val angle = i * (2f * PI.toFloat() / 14)
                sparks.add(Spark(kept.x, kept.z, kept.level, cos(angle) * 100f, sin(angle) * 70f - 45f))
            }
        }
        axis = if (axis == Axis.X) Axis.Z else Axis.X
        direction = if (score % 4 < 2) 1f else -1f
        moving = kept.copy(x = kept.x + if (axis == Axis.X) -direction * 2.6f else 0f,
            z = kept.z + if (axis == Axis.Z) -direction * 2.6f else 0f, level = kept.level + 1)
        // Keep only levels which can still be seen; the last support always survives.
        tower.removeAll { it.level < moving.level - 35 }
        return result
    }
}
