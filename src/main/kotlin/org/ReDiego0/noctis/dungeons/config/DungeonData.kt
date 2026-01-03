package org.ReDiego0.noctis.dungeons.config

import org.bukkit.Material
import org.bukkit.util.Vector

// ============================
//      SCHEMATIC (LA SALA)
// ============================
data class SchematicData(
    val id: String,
    val filename: String,
    val tags: Set<String>, // Etiquetas: [INDUSTRIAL, FACIL, PUZZLE]

    // Coordenadas relativas desde el punto //copy
    val spawnOffset: Vector,
    val exitTriggerOffset: Vector,

    // Lógicas (Nulas si no se usan en esa sala)
    val combatLogic: CombatLogic? = null,
    val puzzleLogic: PuzzleLogic? = null,
    val survivalLogic: SurvivalLogic? = null
)

data class CombatLogic(
    val trigger: CombatTrigger,
    val waves: List<WaveData>,
    val clearCondition: RoomObjective = RoomObjective.ELIMINATION,
    val targetMobId: String? = null // Solo para ASSASSINATION
)

data class WaveData(
    val mobs: List<String>, // "MythicMobID : Cantidad"
    val delayAfterSeconds: Long
)

data class PuzzleLogic(
    val inputs: Map<String, PuzzleInput>, // ID -> Input
    val condition: PuzzleCondition
)

data class PuzzleInput(
    val type: PuzzleInputType,
    val location: Vector, // Relativo
    val validMaterials: Set<Material> = emptySet(),
    val requiredItemNBT: String? = null
)

data class PuzzleCondition(
    val type: PuzzleConditionType,
    val targets: List<String>, // IDs de inputs requeridos
    val timeWindowMs: Long = 0,
    val sequenceOrder: List<String> = emptyList(),
    val resetOnFail: Boolean = false
)

data class SurvivalLogic(
    val durationSeconds: Int,
    val waves: List<WaveData>,
    val loopWaves: Boolean = false
)

// ============================
//      DUNGEON (EL NIVEL)
// ============================
data class DungeonData(
    val id: String,
    val displayName: String,
    val radiationMultiplier: Double, // Multiplicador de radiación dentro de la instancia
    val layers: List<DungeonLayer>,  // Pisos/Etapas
    val rewards: DungeonRewards
)

data class DungeonLayer(
    val count: Int, // Cuantas salas de este tipo generar
    val filters: LayerFilters
)

data class LayerFilters(
    val mustHaveTags: Set<String>,
    val cantHaveTags: Set<String>
)

data class DungeonRewards(
    val money: Double,
    val commands: List<String> // Comandos de consola
)