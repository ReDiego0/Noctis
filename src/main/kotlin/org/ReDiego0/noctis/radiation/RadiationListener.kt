package org.ReDiego0.noctis.radiation

import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.ReDiego0.noctis.Noctis

class RadiationListener(private val plugin: Noctis, private val manager: RadiationManager) : Listener {

    private val radKey = NamespacedKey(plugin, "radiation_level")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val pdc = event.player.persistentDataContainer
        val storedValue = pdc.get(radKey, PersistentDataType.DOUBLE) ?: 0.0
        manager.loadPlayer(event.player.uniqueId, storedValue)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        val currentValue = manager.getRadiation(uuid)

        val pdc = event.player.persistentDataContainer
        pdc.set(radKey, PersistentDataType.DOUBLE, currentValue)

        manager.unloadPlayer(uuid)
    }
}