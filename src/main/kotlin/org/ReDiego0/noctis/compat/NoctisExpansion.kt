package org.ReDiego0.noctis.compat

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.ReDiego0.noctis.Noctis
import org.ReDiego0.noctis.config.NoctisConfig
import kotlin.math.roundToInt

class NoctisExpansion(
    private val plugin: Noctis,
    private val config: NoctisConfig
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "noctis"
    override fun getAuthor(): String = "ReDiego0"
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return ""

        val rads = plugin.radiationManager.getRadiation(player.uniqueId)

        return when (params.lowercase()) {
            "value" -> String.format("%.1f", rads)
            "bar" -> generateBar(rads)
            else -> null
        }
    }

    private fun generateBar(current: Double): String {
        val maxBars = config.barLength
        val percent = (current / 100.0).coerceIn(0.0, 1.0)
        val filledBars = (maxBars * percent).roundToInt()
        val emptyBars = maxBars - filledBars

        val colorStart: String
        val colorEnd: String

        if (percent < 0.5) {
            colorStart = config.colorLowStart
            colorEnd = config.colorLowEnd
        } else if (percent < 0.8) {
            colorStart = config.colorMidStart
            colorEnd = config.colorMidEnd
        } else {
            colorStart = config.colorHighStart
            colorEnd = config.colorHighEnd
        }

        return buildString {
            append("<gradient:$colorStart:$colorEnd>")
            repeat(filledBars) { append(config.barSymbol) }
            append("</gradient>")
            append("<gray>")
            repeat(emptyBars) { append(config.barSymbol) }
            append("</gray>")
        }
    }
}