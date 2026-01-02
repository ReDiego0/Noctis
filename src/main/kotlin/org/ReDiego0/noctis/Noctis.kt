package org.ReDiego0.noctis

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.ReDiego0.noctis.compat.NoctisExpansion
import org.ReDiego0.noctis.config.NoctisConfig
import org.ReDiego0.noctis.economy.BankDatabase
import org.ReDiego0.noctis.economy.CurrencyManager
import org.ReDiego0.noctis.economy.EconomyCommands
import org.ReDiego0.noctis.economy.TaxTask
import org.ReDiego0.noctis.jobs.JobManager
import org.ReDiego0.noctis.radiation.RadiationListener
import org.ReDiego0.noctis.radiation.RadiationManager
import org.ReDiego0.noctis.radiation.RadiationTask

class Noctis : JavaPlugin() {

    lateinit var radiationManager: RadiationManager
        private set
    lateinit var noctisConfig: NoctisConfig
        private set
    lateinit var jobManager: JobManager
        private set
    lateinit var bankDatabase: BankDatabase
    lateinit var currencyManager: CurrencyManager
    lateinit var taxTask: TaxTask

    override fun onEnable() {
        noctisConfig = NoctisConfig(this)

        radiationManager = RadiationManager()
        jobManager = JobManager(this)
        currencyManager = CurrencyManager(this, noctisConfig)
        bankDatabase = BankDatabase(this)
        taxTask = TaxTask(this, bankDatabase)

        taxTask.runTaskTimerAsynchronously(this, 1200L, 1200L)

        val ecoCmd = EconomyCommands(currencyManager, bankDatabase, taxTask)
        getCommand("noctiseco")?.setExecutor(ecoCmd)
        getCommand("bank")?.setExecutor(ecoCmd)
        getCommand("payfuel")?.setExecutor(ecoCmd)

        server.pluginManager.registerEvents(RadiationListener(this, radiationManager), this)
        server.pluginManager.registerEvents(org.ReDiego0.noctis.jobs.JobListener(this, jobManager), this)
        getCommand("noctisjobs")?.setExecutor(org.ReDiego0.noctis.jobs.JobCommand(jobManager))

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            NoctisExpansion(this, noctisConfig).register()
        }

        RadiationTask(this, radiationManager, noctisConfig, taxTask).runTaskTimerAsynchronously(this, 20L, 20L)

        logger.info("Noctis Core active. Loaded config for world: ${noctisConfig.worldName}")
        logger.info("Ciclos de Radiación y Economía sincronizados.")
    }

    override fun onDisable() {
        radiationManager.getCacheSnapshot().forEach { (uuid, value) ->
            Bukkit.getPlayer(uuid)?.persistentDataContainer?.set(
                org.bukkit.NamespacedKey(this, "radiation_level"),
                org.bukkit.persistence.PersistentDataType.DOUBLE,
                value
            )
        }
    }
}