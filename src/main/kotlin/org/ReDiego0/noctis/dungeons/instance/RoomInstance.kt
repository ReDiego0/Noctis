package org.ReDiego0.noctis.dungeons.instance

import io.lumine.mythic.bukkit.MythicBukkit
import org.ReDiego0.noctis.dungeons.config.CombatTrigger
import org.bukkit.Location
import org.ReDiego0.noctis.dungeons.config.SchematicData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RoomInstance(
    val schematic: SchematicData,
    val pasteLocation: Location // La coordenada (0,0,0) relativa en el mundo void
) {
    // Estado de la sala
    var isCompleted: Boolean = false
    var isActive: Boolean = false

    // COMBATE: Mobs vivos (UUIDs de las entidades)
    val activeMobs = ConcurrentHashMap.newKeySet<UUID>()

    // PUZZLE: Inputs activados (para puzzles de secuencia o simultáneos)
    // Map<InputID, Timestamp/State>
    val activatedInputs = ConcurrentHashMap<String, Long>()

    // Ubicación calculada del spawn de esta sala
    fun getPlayerSpawnLocation(): Location {
        return pasteLocation.clone().add(schematic.spawnOffset)
    }

    // Ubicación calculada del trigger de salida (Portal/NPC)
    fun getExitLocation(): Location {
        return pasteLocation.clone().add(schematic.exitTriggerOffset)
    }

    fun start() {
        isActive = true
        spawnMobs()
        // Aquí luego dispararemos lógica de inicio (mensaje, timer, etc)
    }

    private fun spawnMobs() {
        val combat = schematic.combatLogic ?: return
        if (combat.trigger == CombatTrigger.ON_ENTRY) {
            val mythic = MythicBukkit.inst()

            // Spawnear la primera oleada
            combat.waves.firstOrNull()?.mobs?.forEach { mobEntry ->
                // Formato: "Zombie : 3"
                val parts = mobEntry.split(":")
                val mobId = parts[0].trim()
                val amount = parts.getOrNull(1)?.trim()?.toInt() ?: 1

                repeat(amount) {
                    // Spawnear en el centro relativo (mejorar con marcadores en v2)
                    // Usamos el spawn offset + un poco de random para que no se stackeen
                    val spawnLoc = getPlayerSpawnLocation().add(
                        (Math.random() * 4) - 2,
                        0.0,
                        (Math.random() * 4) - 2
                    )

                    try {
                        val mob = mythic.mobManager.spawnMob(mobId, spawnLoc)
                        if (mob != null) {
                            activeMobs.add(mob.uniqueId)
                        }
                    } catch (e: Exception) {
                        org.bukkit.Bukkit.getLogger().warning("[Noctis] Error spawning mob $mobId: ${e.message}")
                    }
                }
            }
        }
    }

    fun complete() {
        if (isCompleted) return
        isCompleted = true
        isActive = false
        // Aquí luego spawnearemos el Portal/NPC de salida
    }
}