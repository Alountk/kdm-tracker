package io.kdmtracker.app.ui.shell

/** One entry of the top bar's stat cluster. */
data class ShellStat(
    val icon: String,
    val value: String,
    val unbacked: Boolean,
)

/**
 * The three stats the design's top bar shows, in its order.
 *
 * Population and deaths are record-backed. The design's third entry — "Reserva de Linternas" — has
 * no backing in the domain (there is no lantern-hoard concept), so it renders with an inert value
 * and is flagged, rather than faked with an unrelated number or hidden.
 */
object ShellStats {
    /** What an unbacked entry shows. */
    const val UNBACKED_VALUE = "—"

    fun of(
        population: Int,
        deaths: Int,
    ): List<ShellStat> =
        listOf(
            ShellStat("group", population.toString(), unbacked = false),
            ShellStat("skull", deaths.toString(), unbacked = false),
            ShellStat("flare", UNBACKED_VALUE, unbacked = true),
        )
}
