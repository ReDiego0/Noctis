package org.ReDiego0.noctis.dungeons

import org.bukkit.entity.Player
import org.ReDiego0.noctis.Noctis
import org.ReDiego0.noctis.dungeons.config.DungeonLoader
import org.ReDiego0.noctis.dungeons.generation.SchematicPaster
import org.ReDiego0.noctis.dungeons.generation.WorldManager
import org.ReDiego0.noctis.dungeons.instance.DungeonInstance
import org.ReDiego0.noctis.dungeons.instance.RoomInstance
import org.ReDiego0.noctis.party.Party
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DungeonManager(private val plugin: Noctis) {

    val worldManager: WorldManager
    val loader: DungeonLoader
    val paster: SchematicPaster

    private val playerInstanceMap = ConcurrentHashMap<UUID, DungeonInstance>()
    private val activeDungeons = ConcurrentHashMap<UUID, DungeonInstance>()

    init {
        worldManager = WorldManager(plugin)
        loader = DungeonLoader(plugin)
        paster = SchematicPaster(plugin)
        loader.loadAll()

        plugin.logger.info("DungeonManager inicializado. Schematics listos.")
    }

    fun getDungeonByPlayer(player: Player): DungeonInstance? {
        return playerInstanceMap[player.uniqueId]
    }

    fun startDungeon(party: Party, dungeonId: String) {
        val data = loader.getDungeon(dungeonId)
        if (data == null) {
            party.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Error: Dungeon '$dungeonId' no encontrada en config."))
            return
        }

        val worldOrigin = worldManager.getNextFreeLocation()

        val instance = DungeonInstance(
            dungeonData = data,
            party = party,
            originLocation = worldOrigin
        )

        var currentZ = 0.0
        val padding = 50.0

        for (layer in data.layers) {
            val candidates = loader.getSchematicsByTags(layer.filters.mustHaveTags, layer.filters.cantHaveTags)

            if (candidates.isEmpty()) {
                plugin.logger.severe("¡Error Crítico! No hay schematics para tags: ${layer.filters.mustHaveTags}")
                continue
            }

            for (i in 0 until layer.count) {
                val selectedSchem = candidates.random()

                val roomOrigin = worldOrigin.clone().add(0.0, 0.0, currentZ)

                if (!paster.paste(selectedSchem.filename, roomOrigin)) {
                    plugin.logger.warning("Fallo al pegar schematic: ${selectedSchem.filename}")
                }

                val roomInstance = RoomInstance(selectedSchem, roomOrigin)
                instance.rooms.add(roomInstance)
                currentZ += selectedSchem.exitTriggerOffset.z + padding
            }
        }

        if (instance.rooms.isEmpty()) {
            party.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Error de generación. Dungeon vacía."))
            return
        }

        activeDungeons[instance.id] = instance
        party.getMembers().forEach { member ->
            playerInstanceMap[member.uuid] = instance
        }

        instance.currentRoomIndex = -1 // Truco para que el advance ponga el index en 0
        instance.advanceToNextRoom()

        plugin.logger.info("Dungeon iniciada: ${data.displayName} (ID: ${instance.id})")
    }

    fun stopDungeon(instanceId: UUID) {
        val instance = activeDungeons.remove(instanceId) ?: return

        instance.party.getMembers().forEach { member ->
            playerInstanceMap.remove(member.uuid)
        }
        plugin.logger.info("Dungeon finalizada y limpiada: ${instance.id}")
    }

    // Para limpiar al apagar el server
    fun cleanupAll() {
        activeDungeons.keys.toList().forEach { stopDungeon(it) }
    }
}