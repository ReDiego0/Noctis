package org.ReDiego0.noctis.radiation

import com.palmergames.bukkit.towny.TownyAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.scheduler.BukkitRunnable
import org.ReDiego0.noctis.Noctis
import org.ReDiego0.noctis.config.NoctisConfig
import org.ReDiego0.noctis.economy.TaxTask
import java.util.UUID

class RadiationTask(
    private val plugin: Noctis,
    private val manager: RadiationManager,
    private val config: NoctisConfig,
    private val taxTask: TaxTask
) : BukkitRunnable() {

    override fun run() {
        val world = Bukkit.getWorld(config.worldName) ?: return
        val isNight = world.time in 13000..23000
        val currentMultiplier = if (isNight) config.nightMultiplier else 1.0

        val affectedPlayers = mutableMapOf<UUID, Double>()

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.world.name != config.worldName) continue
            if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) continue

            var isSafeZone = false
            val towny = TownyAPI.getInstance()
            val location = player.location

            if (!towny.isWilderness(location)) {
                val townBlock = towny.getTownBlock(location)
                if (townBlock != null && townBlock.hasTown()) {
                    val town = townBlock.town
                    if (taxTask.isTownProtected(town.uuid)) {
                        isSafeZone = true
                    }
                }
            }

            if (isSafeZone) {
                manager.modifyRadiation(player.uniqueId, -config.townyCleanupRate)
            } else {
                val mitigation = calculateMitigation(player.inventory.armorContents)
                val effectiveRad = (config.baseRadiation * currentMultiplier) * (1.0 - mitigation)

                val newTotal = manager.modifyRadiation(player.uniqueId, effectiveRad)
                affectedPlayers[player.uniqueId] = newTotal
            }
        }

        if (affectedPlayers.isNotEmpty()) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                applyEffects(affectedPlayers)
            })
        }
    }

    private fun calculateMitigation(armorContents: Array<out org.bukkit.inventory.ItemStack?>): Double {
        var mitigation = 0.0
        for (item in armorContents) {
            if (item == null || item.type == Material.AIR) continue
            mitigation += config.getArmorProtection(item.type)
        }
        return mitigation.coerceAtMost(config.maxMitigationCap)
    }

    private fun applyEffects(playerData: Map<UUID, Double>) {
        for ((uuid, rads) in playerData) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            if (rads >= 100.0) {
                if (player.health > config.criticalDamage) {
                    player.health = player.health - config.criticalDamage
                } else {
                    player.health = 0.0
                }
                player.sendActionBar(Component.text("¡RADIACIÓN CRÍTICA!", NamedTextColor.DARK_RED))
            }
        }
    }
}