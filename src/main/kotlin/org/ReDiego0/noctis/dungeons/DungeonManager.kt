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

        plugin.logger.info("DungeonManager inicializado.")
    }

    fun getDungeonByPlayer(player: Player): DungeonInstance? {
        return playerInstanceMap[player.uniqueId]
    }

    /**
     * Inicia una nueva Dungeon para una Party.
     * método complejo que orquesta todo (Generación Procedural).
     */
    fun startDungeon(party: Party, dungeonId: String) {
        val data = loader.getDungeon(dungeonId) ?: return // Validar ID
        val worldOrigin = worldManager.getNextFreeLocation()

        // 1. Crear la instancia vacía
        val instance = DungeonInstance(
            dungeonData = data,
            party = party,
            originLocation = worldOrigin
        )

        // 2. Generación Procedural (Seleccionar salas)
        var currentZ = 0.0
        val padding = 50.0 // Espacio vacío entre salas para que no se vean/toquen

        for (layer in data.layers) {
            // Buscar candidatos que cumplan los filtros del layer
            val candidates = loader.getSchematicsByTags(layer.filters.mustHaveTags, layer.filters.cantHaveTags)

            if (candidates.isEmpty()) {
                plugin.logger.severe("¡Error Crítico! No hay schematics para el layer con tags: ${layer.filters.mustHaveTags}")
                continue
            }

            // Elegir X salas al azar de los candidatos
            for (i in 0 until layer.count) {
                val selectedSchem = candidates.random()

                // Calcular posición real en el mundo
                // X es fijo para la dungeon, Z avanza
                val roomOrigin = worldOrigin.clone().add(0.0, 0.0, currentZ)

                // 3. Pegar Físicamente (Async pero bloqueamos lógica hasta terminar o usamos callback)
                // Nota: Por simplicidad en MVP, asumimos que paster es rápido.
                // En prod idealmente precargaríamos o pausaríamos a los jugadores.
                paster.paste(selectedSchem.filename, roomOrigin)

                // 4. Crear Instancia de Sala
                val roomInstance = RoomInstance(selectedSchem, roomOrigin)
                instance.rooms.add(roomInstance)

                // Avanzar Z para la siguiente sala (distancia del schematic + padding)
                // Como no leemos el tamaño real del .schem aquí, usamos una estimación o el exit-trigger.
                // Mejor aproximación: Usar la distancia del exit trigger Z + un margen seguro.
                currentZ += selectedSchem.exitTriggerOffset.z + padding
            }
        }

        if (instance.rooms.isEmpty()) {
            party.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Error al generar la dungeon. Contacta a un admin."))
            return
        }

        // 5. Registrar y Teletransportar
        activeDungeons[instance.id] = instance
        party.getMembers().forEach { member ->
            playerInstanceMap[member.uuid] = instance
        }

        // Mover a la sala 1
        instance.currentRoomIndex = -1 // Hack para que advanceToNextRoom ponga 0
        instance.advanceToNextRoom()

        plugin.logger.info("Dungeon iniciada: ${data.displayName} para party de ${party.getLeader()?.uuid}")
    }

    // Método para limpiar cuando termina (se llamará después)
    fun stopDungeon(instance: DungeonInstance) {
        activeDungeons.remove(instance.id)
        instance.party.getMembers().forEach { playerInstanceMap.remove(it.uuid) }
        // TODO: Teletransportar jugadores fuera si siguen dentro
    }
}