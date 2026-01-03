package org.ReDiego0.noctis.dungeons.listeners

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.ReDiego0.noctis.dungeons.DungeonManager
import org.ReDiego0.noctis.dungeons.config.RoomObjective

class DungeonMobListener(private val manager: DungeonManager) : Listener {

    @EventHandler
    fun onMythicDeath(event: MythicMobDeathEvent) {
        val killer = event.killer
        if (killer == null || killer.world.name != "noctis_dungeons") return

        // 1. Identificar en qué instancia ocurrió
        val player = if (killer is org.bukkit.entity.Player) killer else return
        val dungeon = manager.getDungeonByPlayer(player) ?: return

        val room = dungeon.getCurrentRoom() ?: return
        if (room.isCompleted) return

        // 2. Verificar si el mob era parte de la sala
        val mobUUID = event.entity.uniqueId
        if (room.activeMobs.contains(mobUUID)) {

            // Remover de la lista de vivos
            room.activeMobs.remove(mobUUID)

            // 3. Verificar Condición de Victoria
            val combatLogic = room.schematic.combatLogic ?: return

            when (combatLogic.clearCondition) {
                RoomObjective.ELIMINATION -> {
                    // Si no quedan mobs vivos, sala completada
                    if (room.activeMobs.isEmpty()) {
                        dungeon.broadcast("<green>¡Sala despejada! El camino se ha abierto.")
                        room.complete()
                        // Aquí spawnearíamos visualmente el portal/NPC de salida
                    } else {
                        // Feedback opcional: "Quedan X enemigos"
                    }
                }
                RoomObjective.ASSASSINATION -> {
                    // Verificar si el mob muerto era el Boss Target
                    val internalName = event.mobType.internalName
                    if (internalName.equals(combatLogic.targetMobId, ignoreCase = true)) {
                        dungeon.broadcast("<gold>¡Objetivo eliminado!")
                        room.complete() // Mueren los minions o no, da igual, el boss cayó
                    }
                }
                else -> {}
            }
        }
    }
}