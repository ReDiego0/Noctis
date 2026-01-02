package org.ReDiego0.noctis.compat

import com.palmergames.bukkit.towny.TownyAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.ReDiego0.noctis.Noctis
import org.ReDiego0.noctis.config.NoctisConfig
import org.ReDiego0.noctis.jobs.JobType
import kotlin.math.roundToInt

class NoctisExpansion(
    private val plugin: Noctis,
    private val config: NoctisConfig
) : PlaceholderExpansion() {

    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    override fun getIdentifier(): String = "noctis"
    override fun getAuthor(): String = "ReDiego0"
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return ""

        val rads = plugin.radiationManager.getRadiation(player.uniqueId)

        return when (params.lowercase()) {
            "value" -> String.format("%.1f", rads)
            "bar" -> getLegacyBar(rads)
            "job_display" -> {
                JobType.getJob(player).displayName
            }
            "town_fuel" -> {
                getTownFuel(player)
            }
            else -> null
        }
    }

    private fun getTownFuel(player: Player): String {
        val resident = TownyAPI.getInstance().getResident(player)
        if (resident != null && resident.hasTown()) {
            val town = resident.townOrNull
            if (town != null) {
                return plugin.bankDatabase.getBalance(town.uuid).toString()
            }
        }
        return "N/A"
    }

    private fun getLegacyBar(current: Double): String {
        val rawMiniMessage = generateMiniMessageString(current)
        val component = miniMessage.deserialize(rawMiniMessage)
        return legacySerializer.serialize(component)
    }

    private fun generateMiniMessageString(current: Double): String {
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