package org.ReDiego0.noctis.dungeons.instance

import net.kyori.adventure.text.minimessage.MiniMessage
import org.ReDiego0.noctis.dungeons.config.CombatTrigger
import org.bukkit.Location
import org.bukkit.entity.Player
import org.ReDiego0.noctis.dungeons.config.DungeonData
import org.ReDiego0.noctis.party.Party
import java.util.UUID

class DungeonInstance(
    val id: UUID = UUID.randomUUID(),
    val dungeonData: DungeonData,
    val party: Party,
    val originLocation: Location // Donde empieza la primera sala (X, 100, 0)
) {

    private val mm = MiniMessage.miniMessage()

    // Lista ordenada de salas que se generaron para esta run
    val rooms = mutableListOf<RoomInstance>()

    // Índice de la sala actual (0 = primera sala)
    var currentRoomIndex: Int = 0

    // Estado
    var isEnded: Boolean = false
    val startTime: Long = System.currentTimeMillis()

    fun getCurrentRoom(): RoomInstance? {
        if (currentRoomIndex < 0 || currentRoomIndex >= rooms.size) return null
        return rooms[currentRoomIndex]
    }

    fun broadcast(msg: String) {
        party.broadcast(mm.deserialize(msg))
    }

    fun getAlivePlayers(): List<Player> {
        return party.getMembers()
            .mapNotNull { it.getPlayer() }
            .filter { it.isValid && !it.isDead && it.world.name == "noctis_dungeons" }
    }

    /**
     * Mueve a la party a la siguiente sala.
     * @return true si avanzaron, false si ya terminaron la dungeon.
     */
    fun advanceToNextRoom(): Boolean {
        val current = getCurrentRoom()
        current?.complete() // Asegurar que la anterior se cierre

        currentRoomIndex++
        val nextRoom = getCurrentRoom()

        if (nextRoom != null) {
            // Teletransportar a todos al spawn de la nueva sala
            val spawn = nextRoom.getPlayerSpawnLocation()

            // Forzar carga de chunk por si acaso
            spawn.chunk.load()

            getAlivePlayers().forEach { p ->
                p.teleport(spawn)
                p.sendMessage(mm.deserialize("<green>Avanzando a la siguiente sala..."))
            }

            nextRoom.start()
            return true
        } else {
            // No hay más salas == Dungeon Terminada
            finishDungeon(true)
            return false
        }
    }

    fun finishDungeon(success: Boolean) {
        isEnded = true
        if (success) {
            broadcast("<gold><bold>¡Dungeon Completada!")
            // Dar recompensas
        } else {
            broadcast("<red><bold>Dungeon Fallida.")
        }
        // El Manager se encargará de limpiar/expulsar jugadores después
    }

    fun spawnRoomMobs(room: RoomInstance) {
        val combat = room.schematic.combatLogic ?: return
        if (combat.trigger == CombatTrigger.ON_ENTRY) {

            val mythic = io.lumine.mythic.bukkit.MythicBukkit.inst()

            combat.waves.firstOrNull()?.mobs?.forEach { mobEntry ->
                // mobEntry es "Zombie : 3"
                val parts = mobEntry.split(":")
                val mobId = parts[0].trim()
                val amount = parts.getOrNull(1)?.trim()?.toInt() ?: 1

                // Spawnear en el centro de la sala o en puntos aleatorios del schematic
                // Para empezar, spawneamos cerca del centro relativo + random
                repeat(amount) {
                    val spawnLoc = room.pasteLocation.clone().add(room.schematic.spawnOffset) // Temporal: Spawnean donde el jugador
                    // TODO: Usar marcadores (Esponjas) detectadas al pegar

                    val mob = mythic.mobManager.spawnMob(mobId, spawnLoc)
                    if (mob != null) {
                        room.activeMobs.add(mob.uniqueId)
                    }
                }
            }
        }
    }
}