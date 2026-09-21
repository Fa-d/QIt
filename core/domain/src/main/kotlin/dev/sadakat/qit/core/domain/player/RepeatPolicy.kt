package dev.sadakat.qit.core.domain.player

/** What happens when an ayah finishes. */
sealed interface RepeatStep {
    /** Carry on to the next ayah. */
    data object Advance : RepeatStep

    /** Play from the start of [ayah] again. */
    data class JumpTo(val ayah: Int) : RepeatStep

    /** Stop (pause) here: a counted range has played its last round. */
    data object Finish : RepeatStep
}

/** How many times the current repeat unit (an ayah, or the whole range) has played so far. */
@JvmInline
value class RepeatProgress(val plays: Int = 1)

/**
 * The rules of [RepeatSetting], as pure functions the player adapter applies at ayah boundaries.
 * The basmala (ayah 0) never repeats.
 */
object RepeatPolicy {

    data class Decision(val step: RepeatStep, val progress: RepeatProgress)

    /** The decision when [finishedAyah] has played to its end. */
    fun afterAyah(setting: RepeatSetting, progress: RepeatProgress, finishedAyah: Int): Decision = when {
        finishedAyah < 1 -> advance()

        setting is RepeatSetting.Ayah -> again(setting.times, progress, restartAt = finishedAyah) ?: advance()

        setting is RepeatSetting.Range && finishedAyah == setting.to ->
            again(setting.times, progress, restartAt = setting.from) ?: Decision(RepeatStep.Finish, RepeatProgress())

        else -> advance()
    }

    /** Whether the player must stop at the end of [ayah] to apply the repeat (anything but [RepeatStep.Advance]). */
    fun intervenesAfter(setting: RepeatSetting, progress: RepeatProgress, ayah: Int): Boolean =
        afterAyah(setting, progress, ayah).step != RepeatStep.Advance

    /**
     * The setting and progress after the listener moved to [targetAyah] themselves (next/previous,
     * tapping an ayah): an ayah repeat starts counting afresh; a range stays while the move is inside
     * it and is dropped once playback leaves it.
     */
    fun onManualMove(
        setting: RepeatSetting,
        progress: RepeatProgress,
        targetAyah: Int,
    ): Pair<RepeatSetting, RepeatProgress> {
        val staysInRange = setting is RepeatSetting.Range && targetAyah in setting.from..setting.to
        return when {
            staysInRange -> setting to progress
            setting is RepeatSetting.Range -> RepeatSetting.Off to RepeatProgress()
            else -> setting to RepeatProgress()
        }
    }

    /** Where playback should jump when [setting] is chosen while [currentAyah] plays; null to stay. */
    fun entryAyah(setting: RepeatSetting, currentAyah: Int): Int? =
        if (setting is RepeatSetting.Range && currentAyah !in setting.from..setting.to) setting.from else null

    private fun again(times: Int?, progress: RepeatProgress, restartAt: Int): Decision? =
        if (times == null || progress.plays < times) {
            Decision(RepeatStep.JumpTo(restartAt), RepeatProgress(progress.plays + 1))
        } else {
            null
        }

    private fun advance() = Decision(RepeatStep.Advance, RepeatProgress())
}
