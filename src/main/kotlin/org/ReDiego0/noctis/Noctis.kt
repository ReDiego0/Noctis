package org.ReDiego0.noctis

import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.ReDiego0.noctis.compat.NoctisExpansion
import org.ReDiego0.noctis.config.NoctisConfig
import org.ReDiego0.noctis.economy.BankDatabase
import org.ReDiego0.noctis.economy.CurrencyManager
import org.ReDiego0.noctis.economy.EconomyCommands
import org.ReDiego0.noctis.economy.TaxTask
import org.ReDiego0.noctis.jobs.JobCommand
import org.ReDiego0.noctis.jobs.JobManager
import org.ReDiego0.noctis.party.PartyCommand
import org.ReDiego0.noctis.party.PartyManager
import org.ReDiego0.noctis.radiation.RadiationListener
import org.ReDiego0.noctis.radiation.RadiationManager
import org.ReDiego0.noctis.radiation.RadiationTask

class Noctis : JavaPlugin() {

    lateinit var radiationManager: RadiationManager private set
    lateinit var noctisConfig: NoctisConfig private set
    lateinit var jobManager: JobManager private set
    lateinit var bankDatabase: BankDatabase private set
    lateinit var currencyManager: CurrencyManager private set
    lateinit var taxTask: TaxTask private set
    lateinit var partyManager: PartyManager private set

    var perms: Permission? = null

    override fun onEnable() {
        noctisConfig = NoctisConfig(this)

        if (!setupPermissions()) {
            logger.warning("Vault no encontrado o sin plugin de permisos. Comandos de Jobs limitados.")
        }

        radiationManager = RadiationManager()
        jobManager = JobManager(this)
        currencyManager = CurrencyManager(this, noctisConfig)
        bankDatabase = BankDatabase(this)
        taxTask = TaxTask(this, bankDatabase)
        partyManager = PartyManager(noctisConfig)

        taxTask.runTaskTimerAsynchronously(this, 1200L, 1200L)
        RadiationTask(this, radiationManager, noctisConfig, taxTask)
            .runTaskTimerAsynchronously(this, 20L, 20L)

        registerEconomyCommands()
        registerJobCommands()
        registerPartyCommands()

        val pm = server.pluginManager
        pm.registerEvents(RadiationListener(this, radiationManager), this)
        pm.registerEvents(org.ReDiego0.noctis.jobs.JobListener(this, jobManager), this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            NoctisExpansion(this, noctisConfig).register()
        }

        logger.info("Noctis Core active. Loaded config for world: ${noctisConfig.worldName}")
    }

    override fun onDisable() {
        if (::radiationManager.isInitialized) {
            radiationManager.getCacheSnapshot().forEach { (uuid, value) ->
                Bukkit.getPlayer(uuid)?.persistentDataContainer?.set(
                    org.bukkit.NamespacedKey(this, "radiation_level"),
                    org.bukkit.persistence.PersistentDataType.DOUBLE,
                    value
                )
            }
        }
    }

    private fun setupPermissions(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) return false
        val rsp = server.servicesManager.getRegistration(Permission::class.java)
        perms = rsp?.provider
        return perms != null
    }

    private fun registerEconomyCommands() {
        val ecoExecutor = EconomyCommands(currencyManager, bankDatabase, taxTask)
        getCommand("noctiseco")?.setExecutor(ecoExecutor)
        getCommand("noctiseco")?.tabCompleter = ecoExecutor

        getCommand("bank")?.setExecutor(ecoExecutor)
        getCommand("bank")?.tabCompleter = ecoExecutor

        getCommand("payfuel")?.setExecutor(ecoExecutor)
        getCommand("payfuel")?.tabCompleter = ecoExecutor
    }

    private fun registerJobCommands() {
        val jobExecutor = JobCommand(this, jobManager)
        getCommand("noctisjobs")?.setExecutor(jobExecutor)
        getCommand("noctisjobs")?.tabCompleter = jobExecutor
    }

    private fun registerPartyCommands() {
        val partyExecutor = PartyCommand(partyManager)
        getCommand("party")?.setExecutor(partyExecutor)
        getCommand("party")?.tabCompleter = partyExecutor
    }
}