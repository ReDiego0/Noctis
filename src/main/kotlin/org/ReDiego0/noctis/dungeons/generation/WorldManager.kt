package org.ReDiego0.noctis.dungeons.generation

import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.ReDiego0.noctis.Noctis
import org.bukkit.GameRules
import java.util.concurrent.atomic.AtomicInteger

class WorldManager(private val plugin: Noctis) {

    private val WORLD_NAME = "noctis_dungeons"
    private var dungeonWorld: World? = null

    private val dungeonIndex = AtomicInteger(0)
    private val SPACING = 5000

    init {
        loadWorld()
    }

    private fun loadWorld() {
        if (Bukkit.getWorld(WORLD_NAME) == null) {
            plugin.logger.info("Creando mundo de instancias ($WORLD_NAME)...")
            val creator = WorldCreator(WORLD_NAME)
            creator.generator(VoidGenerator())
            dungeonWorld = creator.createWorld()
        } else {
            dungeonWorld = Bukkit.getWorld(WORLD_NAME)
        }

        dungeonWorld?.let { world ->
            world.setGameRule(GameRules.SPAWN_MOBS, false) // Solo MythicMobs controlados
            world.setGameRule(GameRules.ADVANCE_TIME, false)
            world.setGameRule(GameRules.ADVANCE_WEATHER, false)
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false)
            world.time = 18000 // Medianoche
            world.save()
        }
    }

    fun getWorld(): World? = dungeonWorld

    /**
     * Obtiene una ubicación segura y vacía para generar una nueva Dungeon.
     * Retorna (X, 100, 0)
     */
    fun getNextFreeLocation(): Location {
        val world = dungeonWorld ?: throw IllegalStateException("El mundo de dungeons no está cargado.")

        // Incrementar índice de forma Thread-Safe
        val index = dungeonIndex.getAndIncrement()
        val x = index * SPACING

        // Altura 100 para evitar el void damage accidental
        return Location(world, x.toDouble(), 100.0, 0.0)
    }
}