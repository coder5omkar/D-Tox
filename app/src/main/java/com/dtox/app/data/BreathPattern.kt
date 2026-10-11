package com.dtox.app.data

enum class PhaseKind { IN, HOLD, OUT }

data class Phase(val kind: PhaseKind, val seconds: Int)

/**
 * A breathing pattern is just a list of phases. Kept deliberately tiny so
 * the whole "engine" is a few lines of arithmetic.
 */
enum class BreathPattern(
    val id: String,
    val label: String,
    val phases: List<Phase>,
) {
    BOX(
        id = "box",
        label = "Box 4·4·4·4",
        phases = listOf(
            Phase(PhaseKind.IN, 4),
            Phase(PhaseKind.HOLD, 4),
            Phase(PhaseKind.OUT, 4),
            Phase(PhaseKind.HOLD, 4),
        ),
    ),
    CALM(
        id = "calm",
        label = "Calm 4·7·8",
        phases = listOf(
            Phase(PhaseKind.IN, 4),
            Phase(PhaseKind.HOLD, 7),
            Phase(PhaseKind.OUT, 8),
        ),
    );

    val cycleSeconds: Int get() = phases.sumOf { it.seconds }

    companion object {
        fun from(id: String): BreathPattern = entries.firstOrNull { it.id == id } ?: BOX
    }
}
