package org.ReDiego0.noctis

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.ReDiego0.noctis.compat.NoctisExpansion
import org.ReDiego0.noctis.config.NoctisConfig
import org.ReDiego0.noctis.radiation.RadiationListener
import org.ReDiego0.noctis.radiation.RadiationManager
import org.ReDiego0.noctis.radiation.RadiationTask

class Noctis : JavaPlugin() {

    lateinit var radiationManager: RadiationManager
        private set
    lateinit var noctisConfig: NoctisConfig
        private set

    override fun onEnable() {
        noctisConfig = NoctisConfig(this)

        radiationManager = RadiationManager()

        server.pluginManager.registerEvents(RadiationListener(this, radiationManager), this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            NoctisExpansion(this, noctisConfig).register()
        }

        RadiationTask(this, radiationManager, noctisConfig).runTaskTimerAsynchronously(this, 20L, 20L)

        logger.info("Noctis Core active. Loaded config for world: ${noctisConfig.worldName}")
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