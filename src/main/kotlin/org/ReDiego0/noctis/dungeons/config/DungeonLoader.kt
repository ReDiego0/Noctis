package org.ReDiego0.noctis.dungeons.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.Vector
import org.ReDiego0.noctis.Noctis
import java.io.File

class DungeonLoader(private val plugin: Noctis) {

    private val schematicMap = mutableMapOf<String, SchematicData>()
    private val dungeonMap = mutableMapOf<String, DungeonData>()

    fun loadAll() {
        schematicMap.clear()
        dungeonMap.clear()

        loadSchematics()
        loadDungeons()

        plugin.logger.info("Cargados: ${schematicMap.size} Schematics y ${dungeonMap.size} Dungeons.")
    }

    fun getSchematic(id: String): SchematicData? = schematicMap[id]
    fun getDungeon(id: String): DungeonData? = dungeonMap[id]
    fun getSchematicsByTags(mustHave: Set<String>, cantHave: Set<String>): List<SchematicData> {
        return schematicMap.values.filter { schem ->
            schem.tags.containsAll(mustHave) && schem.tags.none { cantHave.contains(it) }
        }
    }

    private fun loadSchematics() {
        val file = File(plugin.dataFolder, "schematics.yml")
        if (!file.exists()) plugin.saveResource("schematics.yml", false)

        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("schematics") ?: return

        for (key in section.getKeys(false)) {
            try {
                val path = "schematics.$key"
                val filename = config.getString("$path.file") ?: continue
                val tags = config.getStringList("$path.tags").map { it.uppercase() }.toSet()

                // Parse Offsets "x,y,z"
                val spawnVec = parseVector(config.getString("$path.spawn-offset", "0,1,0")!!)
                val exitVec = parseVector(config.getString("$path.exit-trigger", "0,1,0")!!)

                var combatLogic: CombatLogic? = null
                if (config.contains("$path.combat-logic")) {
                    val cPath = "$path.combat-logic"
                    val trigger = CombatTrigger.valueOf(config.getString("$cPath.start-trigger", "ON_ENTRY")!!)
                    val condition = RoomObjective.valueOf(config.getString("$cPath.clear-condition", "ELIMINATION")!!)
                    val targetId = config.getString("$cPath.target-mob-id")

                    val waves = mutableListOf<WaveData>()
                    val wavesSection = config.getConfigurationSection("$cPath.waves")
                    wavesSection?.getKeys(false)?.forEach { wKey ->
                        val mobs = config.getStringList("$cPath.waves.$wKey.mobs")
                        val delay = config.getLong("$cPath.waves.$wKey.delay-after", 0)
                        waves.add(WaveData(mobs, delay))
                    }
                    combatLogic = CombatLogic(trigger, waves, condition, targetId)
                }

                var puzzleLogic: PuzzleLogic? = null
                if (config.contains("$path.puzzle-logic")) {
                    val pPath = "$path.puzzle-logic"
                    val inputs = mutableMapOf<String, PuzzleInput>()

                    config.getConfigurationSection("$pPath.inputs")?.getKeys(false)?.forEach { iKey ->
                        val type = PuzzleInputType.valueOf(config.getString("$pPath.inputs.$iKey.type")!!)
                        val loc = parseVector(config.getString("$pPath.inputs.$iKey.location")!!)
                        val mats = config.getStringList("$pPath.inputs.$iKey.valid-materials")
                            .mapNotNull { Material.getMaterial(it) }.toSet()

                        inputs[iKey] = PuzzleInput(type, loc, mats)
                    }

                    val condType = PuzzleConditionType.valueOf(config.getString("$pPath.completion-condition.type", "SINGLE_ACTIVATE")!!)
                    val targets = config.getStringList("$pPath.completion-condition.required-inputs") // o targets en data
                    val window = config.getLong("$pPath.completion-condition.time-window-ms", 0)
                    val order = config.getStringList("$pPath.completion-condition.order")

                    val condition = PuzzleCondition(condType, targets, window, order)
                    puzzleLogic = PuzzleLogic(inputs, condition)
                }

                schematicMap[key] = SchematicData(key, filename, tags, spawnVec, exitVec, combatLogic, puzzleLogic)

            } catch (e: Exception) {
                plugin.logger.warning("Error cargando schematic '$key': ${e.message}")
            }
        }
    }

    private fun loadDungeons() {
        val file = File(plugin.dataFolder, "dungeons.yml")
        if (!file.exists()) plugin.saveResource("dungeons.yml", false)

        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("dungeons") ?: return

        for (key in section.getKeys(false)) {
            try {
                val path = "dungeons.$key"
                val name = config.getString("$path.display-name", "Dungeon")!!
                val rads = config.getDouble("$path.radiation-multiplier", 1.0)

                val layers = mutableListOf<DungeonLayer>()
                val layersList = config.getMapList("$path.layers") // Esto es tricky con Bukkit config

                @Suppress("UNCHECKED_CAST")
                val rawLayers = config.getList("$path.layers") as List<Map<String, Any>>?
                rawLayers?.forEach { map ->
                    val count = (map["count"] as? Number)?.toInt() ?: 1
                    val filtersMap = map["filters"] as? Map<String, List<String>>

                    val must = filtersMap?.get("MUST_HAVE")?.map { it.uppercase() }?.toSet() ?: emptySet()
                    val cant = filtersMap?.get("CANT_HAVE")?.map { it.uppercase() }?.toSet() ?: emptySet()

                    layers.add(DungeonLayer(count, LayerFilters(must, cant)))
                }

                val rewardMoney = config.getDouble("$path.rewards.money", 0.0)
                val rewardCmds = config.getStringList("$path.rewards.commands")

                dungeonMap[key] = DungeonData(key, name, rads, layers, DungeonRewards(rewardMoney, rewardCmds))

            } catch (e: Exception) {
                plugin.logger.warning("Error cargando dungeon '$key': ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun parseVector(str: String): Vector {
        val parts = str.split(",").map { it.trim().toDouble() }
        return Vector(parts[0], parts[1], parts[2])
    }
}