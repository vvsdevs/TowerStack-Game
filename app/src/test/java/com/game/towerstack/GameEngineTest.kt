package com.game.towerstack

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private val base = Block(0f, 0f, 2f, 2f, 0)
    @Test fun fullOverlap() {
        val cut = Geometry.cut(base, base.copy(level = 1), Axis.X)
        assertEquals(base.copy(level = 1), cut.kept)
        assertTrue(cut.perfect); assertNull(cut.removed)
    }
    @Test fun partialOverlapBothSidesAndAxes() {
        for (axis in Axis.entries) for (sign in listOf(-1f, 1f)) {
            val moving = if (axis == Axis.X) base.copy(x = sign * .5f, level = 1) else base.copy(z = sign * .5f, level = 1)
            val result = Geometry.cut(base, moving, axis)
            val kept = result.kept!!; val removed = result.removed!!
            assertFalse(result.perfect)
            assertEquals(1.5f, if (axis == Axis.X) kept.width else kept.depth, .0001f)
            assertEquals(sign * .25f, if (axis == Axis.X) kept.x else kept.z, .0001f)
            assertEquals(.5f, if (axis == Axis.X) removed.width else removed.depth, .0001f)
            assertEquals(sign * 1.25f, if (axis == Axis.X) removed.x else removed.z, .0001f)
            assertEquals(2f, if (axis == Axis.X) kept.depth else kept.width, .0001f)
        }
    }
    @Test fun edgeContactAndMiss() {
        for (axis in Axis.entries) for (offset in listOf(-3f, -2f, 2f, 3f)) {
            val moving = if (axis == Axis.X) base.copy(x = offset) else base.copy(z = offset)
            val cut = Geometry.cut(base, moving, axis)
            assertNull(cut.kept); assertEquals(moving, cut.removed)
        }
    }
    @Test fun perfectToleranceScalesWithDimension() {
        for (axis in Axis.entries) for (size in listOf(.2f, 2f)) {
            val top = base.copy(width = size, depth = size)
            for (sign in listOf(-1f, 1f)) {
                val distance = size * Geometry.PERFECT_FRACTION * sign
                val inside = if (axis == Axis.X) top.copy(x = distance) else top.copy(z = distance)
                assertTrue(Geometry.cut(top, inside, axis).perfect)
                val outside = if (axis == Axis.X) top.copy(x = distance * 1.01f) else top.copy(z = distance * 1.01f)
                assertFalse(Geometry.cut(top, outside, axis).perfect)
            }
        }
    }
    @Test fun restartResetsRunAndPreventsDoublePlacement() {
        val game = GameEngine(); game.reset()
        repeat(38) { game.tick(.05f) }
        assertNotNull(game.place()); assertEquals(1, game.score); assertEquals(Axis.Z, game.axis)
        assertNull(game.place())
        game.pause(); game.reset()
        assertEquals(Phase.READY, game.phase); assertEquals(0, game.score)
        assertEquals(Axis.X, game.axis); assertEquals(1, game.tower.size)
        assertEquals(0f, game.camera, 0f); assertEquals(0f, game.perfectTime, 0f)
        assertTrue(game.debris.isEmpty()); assertTrue(game.sparks.isEmpty())
        assertEquals(Block(-2.6f, 0f, 2f, 2f, 1), game.moving)
        assertNull(game.place())
    }
    @Test fun pausedTimeDoesNotMoveAndResumeGuardsInput() {
        val game = GameEngine(); game.reset(); game.tick(.05f); game.pause()
        val before = game.moving; game.tick(10f)
        assertEquals(before, game.moving); assertNull(game.place())
        game.resume(); assertEquals(Phase.READY, game.phase); assertNull(game.place())
    }
    @Test fun deltaIsClamped() {
        val a = GameEngine(); val b = GameEngine(); a.reset(); b.reset()
        a.tick(10f); b.tick(.05f); assertEquals(a.moving, b.moving)
    }
}
