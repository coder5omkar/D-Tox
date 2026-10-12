package com.dtox.app.data

sealed interface ExerciseKind {
    /** Guided breathing following a [patternId] for [rounds] cycles. */
    data class Breathing(val patternId: String, val rounds: Int) : ExerciseKind

    /** Step-by-step guidance. Each step advances on a timer or by tapping Next. */
    data class Steps(val steps: List<GuideStep>) : ExerciseKind
}

data class GuideStep(
    val title: String,
    val detail: String,
    /** Seconds before auto-advance, or null to wait for a tap. */
    val seconds: Int? = null,
)

data class Exercise(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val category: String,
    val baseGems: Int,
    val kind: ExerciseKind,
) {
    val durationSeconds: Int
        get() = when (kind) {
            is ExerciseKind.Breathing ->
                BreathPattern.from(kind.patternId).cycleSeconds * kind.rounds
            is ExerciseKind.Steps -> kind.steps.sumOf { it.seconds ?: 15 }
        }
}

/**
 * The practice catalog. Evidence-informed and tiny: breathing exercises,
 * grounding/awareness steps, and one gratitude pause. Everything is local.
 */
val Exercises: List<Exercise> = listOf(
    Exercise(
        id = "three-sighs",
        title = "Three Sighs",
        subtitle = "A 30-second reset",
        emoji = "🌬️",
        category = "Reset",
        baseGems = 1,
        kind = ExerciseKind.Breathing("sigh", 3),
    ),
    Exercise(
        id = "box",
        title = "Box Breathing",
        subtitle = "Calm your nervous system",
        emoji = "🟩",
        category = "Breathe",
        baseGems = 2,
        kind = ExerciseKind.Breathing("box", 8),
    ),
    Exercise(
        id = "calm",
        title = "4·7·8 Breath",
        subtitle = "Wind down, ease into rest",
        emoji = "🌙",
        category = "Breathe",
        baseGems = 1,
        kind = ExerciseKind.Breathing("calm", 4),
    ),
    Exercise(
        id = "sighing",
        title = "Cyclic Sighing",
        subtitle = "Strongest evidence for fast calm",
        emoji = "💨",
        category = "Breathe",
        baseGems = 2,
        kind = ExerciseKind.Breathing("sigh", 6),
    ),
    Exercise(
        id = "grounding",
        title = "5·4·3·2·1 Grounding",
        subtitle = "When your mind races",
        emoji = "🖐️",
        category = "Ground",
        baseGems = 2,
        kind = ExerciseKind.Steps(
            listOf(
                GuideStep("5 things you can see", "Look around slowly. Name them silently.", 25),
                GuideStep("4 things you can hear", "Near sounds, far sounds — just notice.", 25),
                GuideStep("3 things you can touch", "Texture, temperature, weight.", 25),
                GuideStep("2 things you can smell", "Or two smells you like.", 25),
                GuideStep("1 thing you can taste", "Or a taste you remember.", 25),
            ),
        ),
    ),
    Exercise(
        id = "body-scan",
        title = "Body Scan",
        subtitle = "Head to toe, three minutes",
        emoji = "🧘",
        category = "Body",
        baseGems = 3,
        kind = ExerciseKind.Steps(
            listOf(
                GuideStep("Settle", "Sit or lie down. Let your body be heavy.", 20),
                GuideStep("Feet", "Weight, temperature, the points of contact.", 20),
                GuideStep("Legs", "Move slowly up through calves and thighs.", 20),
                GuideStep("Hips & belly", "Feel the belly rise and fall.", 20),
                GuideStep("Chest", "Notice the chest expanding with each breath.", 20),
                GuideStep("Hands & arms", "Warmth, tingling — whatever is there.", 20),
                GuideStep("Shoulders & neck", "Let them soften. Release the holding.", 20),
                GuideStep("Face & jaw", "Unclench. Smooth the forehead.", 20),
                GuideStep("Whole body", "Feel everything at once, breathing.", 20),
            ),
        ),
    ),
    Exercise(
        id = "feet",
        title = "Feel Your Feet",
        subtitle = "Instant grounding, 30 seconds",
        emoji = "🦶",
        category = "Reset",
        baseGems = 1,
        kind = ExerciseKind.Steps(
            listOf(
                GuideStep("Stand or sit", "Press both feet into the floor.", 15),
                GuideStep("Notice", "Weight, temperature, exact contact points.", 15),
            ),
        ),
    ),
    Exercise(
        id = "good-thing",
        title = "One Good Thing",
        subtitle = "A tiny gratitude pause",
        emoji = "🌱",
        category = "Reflect",
        baseGems = 1,
        kind = ExerciseKind.Steps(
            listOf(
                GuideStep("Think of one small thing", "Something that went okay today. Small counts."),
                GuideStep("Picture it", "Where were you? What happened?"),
                GuideStep("Notice", "How does it feel in your body right now?"),
            ),
        ),
    ),
)

fun exerciseById(id: String): Exercise? = Exercises.firstOrNull { it.id == id }
