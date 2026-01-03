package org.ReDiego0.noctis.dungeons.instance

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
        // Aquí luego dispararemos lógica de inicio (mensaje, timer, etc)
    }

    fun complete() {
        if (isCompleted) return
        isCompleted = true
        isActive = false
        // Aquí luego spawnearemos el Portal/NPC de salida
    }
}