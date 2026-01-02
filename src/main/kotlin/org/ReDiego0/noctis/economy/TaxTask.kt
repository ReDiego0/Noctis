package org.ReDiego0.noctis.economy

import com.palmergames.bukkit.towny.TownyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.ReDiego0.noctis.Noctis
import java.util.UUID
import java.util.concurrent.TimeUnit

class TaxTask(
    private val plugin: Noctis,
    private val database: BankDatabase
) : BukkitRunnable() {

    private val mm = MiniMessage.miniMessage()
    private val protectedTowns = java.util.concurrent.ConcurrentHashMap.newKeySet<UUID>()

    override fun run() {
        val now = System.currentTimeMillis()
        var nextRun = database.getNextTaxTime()

        val hoursMs = TimeUnit.HOURS.toMillis(plugin.noctisConfig.taxIntervalHours)
        val minutesMs = TimeUnit.MINUTES.toMillis(plugin.noctisConfig.taxIntervalMinutes)
        val totalInterval = hoursMs + minutesMs

        if (nextRun == 0L) {
            nextRun = now + totalInterval
            database.setNextTaxTime(nextRun)
        }

        if (now >= nextRun) {
            collectTaxes()
            database.setNextTaxTime(now + totalInterval)
        }
    }

    fun forceCollection() {
        collectTaxes()
    }

    private fun collectTaxes() {
        plugin.logger.info("=== INICIANDO COBRO DE COMBUSTIBLE NOCTIS ===")

        val towny = TownyAPI.getInstance()
        val cost = plugin.config.getInt("economy.taxes.cost", 10)

        protectedTowns.clear()

        for (town in towny.towns) {
            if (database.removeBalance(town.uuid, cost)) {
                protectedTowns.add(town.uuid)
                val msg = mm.deserialize("<green>[SISTEMA] <gray>Combustible descontado (-$cost). Escudos activos.")
                town.residents.forEach { it.player?.sendMessage(msg) }
            } else {
                val msg = mm.deserialize("<red><bold>[ALERTA] <gray>Fallo crítico de energía. Escudos desactivados.")
                town.residents.forEach { it.player?.sendMessage(msg) }
                plugin.logger.warning("Ciudad ${town.name} sin combustible.")

                Bukkit.broadcast(mm.deserialize("<red>La ciudad ${town.name} ha perdido sus escudos."))
            }
        }
    }

    fun isTownProtected(townUUID: UUID): Boolean {
        return protectedTowns.contains(townUUID)
    }

    fun tryReactivate(townUUID: UUID, townName: String): Boolean {
        if (isTownProtected(townUUID)) return false

        val cost = plugin.config.getInt("economy.taxes.cost", 10)
        if (database.removeBalance(townUUID, cost)) {
            protectedTowns.add(townUUID)

            val msg = mm.deserialize("<green>[SISTEMA] <white>Pago recibido. <aqua>Escudos de radiación REACTIVADOS.")
            TownyAPI.getInstance().getTown(townUUID)?.residents?.forEach { res ->
                res.player?.sendMessage(msg)
            }
            plugin.logger.info("Ciudad $townName reactivó sus escudos tras depósito manual.")
            return true
        }

        return false
    }
}