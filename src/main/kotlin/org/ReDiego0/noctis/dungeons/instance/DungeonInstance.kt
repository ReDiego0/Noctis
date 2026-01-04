package org.ReDiego0.noctis.dungeons.instance

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.ReDiego0.noctis.dungeons.config.DungeonData
import org.ReDiego0.noctis.party.Party
import java.util.UUID

class DungeonInstance(
    val id: UUID = UUID.randomUUID(),
    val dungeonData: DungeonData,
    val party: Party,
    val originLocation: Location
) {

    private val mm = MiniMessage.miniMessage()
    val rooms = mutableListOf<RoomInstance>()
    var currentRoomIndex: Int = 0
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

    fun advanceToNextRoom(): Boolean {
        val current = getCurrentRoom()
        current?.complete()

        currentRoomIndex++
        val nextRoom = getCurrentRoom()

        if (nextRoom != null) {
            val spawn = nextRoom.getPlayerSpawnLocation()
            spawn.chunk.load()

            getAlivePlayers().forEach { p ->
                p.teleport(spawn)
                p.sendMessage(mm.deserialize("<green>Avanzando a la siguiente sala..."))
            }

            nextRoom.start() // Esto dispara el spawning de mobs en RoomInstance
            return true
        } else {
            finishDungeon(true)
            return false
        }
    }

    fun finishDungeon(success: Boolean) {
        if (isEnded) return
        isEnded = true

        if (success) {
            broadcast("<gradient:#00ff00:#00ffff><bold>¡DUNGEON COMPLETADA!</bold></gradient>")
            giveRewards()
        } else {
            broadcast("<red><bold>Misión Fallida. Protocolo de extracción iniciado.</bold>")
        }

        val safeSpawn = Bukkit.getWorld("world")?.spawnLocation ?: originLocation
        val plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(this::class.java) as org.ReDiego0.noctis.Noctis

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            party.getMembers().mapNotNull { it.getPlayer() }.forEach { p ->
                p.teleport(safeSpawn)
                p.sendMessage(mm.deserialize("<gray>Has regresado a salvo."))
                p.health = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
            }
            plugin.dungeonManager.stopDungeon(this.id)

        }, 100L) // 5 segundos de espera
    }

    private fun giveRewards() {
        val rewards = dungeonData.rewards
        if (rewards.commands.isNotEmpty()) {
            val console = Bukkit.getConsoleSender()
            getAlivePlayers().forEach { p ->
                rewards.commands.forEach { cmd ->
                    val finalCmd = cmd.replace("%player%", p.name)
                    Bukkit.dispatchCommand(console, finalCmd)
                }
            }
        }
    }
}