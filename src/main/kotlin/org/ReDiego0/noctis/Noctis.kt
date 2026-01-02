package org.ReDiego0.noctis

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
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

    // Vault Permission Provider
    var perms: Permission? = null

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).verboseOutput(true))
    }

    override fun onEnable() {
        noctisConfig = NoctisConfig(this)
        CommandAPI.onEnable()

        // Setup Vault
        if (!setupPermissions()) {
            logger.warning("Vault no encontrado o sin plugin de permisos. Comandos de Jobs limitados.")
        }

        radiationManager = RadiationManager()
        jobManager = JobManager(this)
        currencyManager = CurrencyManager(this, noctisConfig)
        bankDatabase = BankDatabase(this)
        taxTask = TaxTask(this, bankDatabase)
        partyManager = PartyManager(noctisConfig) // Pasar config aquí

        taxTask.runTaskTimerAsynchronously(this, 1200L, 1200L)
        RadiationTask(this, radiationManager, noctisConfig, taxTask)
            .runTaskTimerAsynchronously(this, 20L, 20L)

        EconomyCommands(currencyManager, bankDatabase, taxTask)
        JobCommand(this, jobManager) // Pasar 'this' (Noctis) aquí
        PartyCommand(partyManager)

        val pm = server.pluginManager
        pm.registerEvents(RadiationListener(this, radiationManager), this)
        pm.registerEvents(org.ReDiego0.noctis.jobs.JobListener(this, jobManager), this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            NoctisExpansion(this, noctisConfig).register()
        }

        logger.info("Noctis Core active. Loaded config for world: ${noctisConfig.worldName}")
        logger.info("Ciclos de Radiación, Economía, Trabajos y Parties activos.")
    }

    override fun onDisable() {
        CommandAPI.onDisable()
        radiationManager.getCacheSnapshot().forEach { (uuid, value) ->
            Bukkit.getPlayer(uuid)?.persistentDataContainer?.set(
                org.bukkit.NamespacedKey(this, "radiation_level"),
                org.bukkit.persistence.PersistentDataType.DOUBLE,
                value
            )
        }
    }

    private fun setupPermissions(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) return false
        val rsp = server.servicesManager.getRegistration(Permission::class.java)
        perms = rsp?.provider
        return perms != null
    }
}