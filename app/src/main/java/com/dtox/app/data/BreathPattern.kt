package com.dtox.app.data

enum class PhaseKind { IN, SIP, HOLD, OUT }

data class Phase(
    val kind: PhaseKind,
    val seconds: Int,
    val label: String? = null,
)

/**
 * A breathing pattern is just a list of phases. Kept deliberately tiny so
 * the whole "engine" is a few lines of arithmetic.
 *
 * [unlockCost] is the gem price in the shop; null means free from the start.
 */
enum class BreathPattern(
    val id: String,
    val label: String,
    val phases: List<Phase>,
    val unlockCost: Int? = null,
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
    ),
    SIGH(
        id = "sigh",
        label = "Sigh 3·1·6",
        phases = listOf(
            Phase(PhaseKind.IN, 3, "Breathe in"),
            Phase(PhaseKind.SIP, 1, "Second sip"),
            Phase(PhaseKind.OUT, 6, "Long exhale"),
        ),
    ),
    COHERENT(
        id = "coherent",
        label = "Coherent 6·6",
        phases = listOf(
            Phase(PhaseKind.IN, 6),
            Phase(PhaseKind.OUT, 6),
        ),
        unlockCost = 80,
    ),
    EXTENDED(
        id = "extended",
        label = "Extended exhale 4·8",
        phases = listOf(
            Phase(PhaseKind.IN, 4),
            Phase(PhaseKind.OUT, 8),
        ),
        unlockCost = 80,
    );

    val cycleSeconds: Int get() = phases.sumOf { it.seconds }

    companion object {
        fun from(id: String): BreathPattern = entries.firstOrNull { it.id == id } ?: BOX
    }
}

fun phaseLabel(phase: Phase): String = phase.label ?: when (phase.kind) {
    PhaseKind.IN -> "Breathe in"
    PhaseKind.SIP -> "Second sip"
    PhaseKind.HOLD -> "Hold"
    PhaseKind.OUT -> "Breathe out"
}
