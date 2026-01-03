package org.ReDiego0.noctis.dungeons.config

enum class RoomObjective {
    ELIMINATION,    // Matar todos los mobs de la lista (MythicMobs)
    ASSASSINATION,  // Matar a un Boss específico
    MECHANISM,      // Resolver puzzle de inputs
    SURVIVAL        // Sobrevivir X tiempo
}

enum class PuzzleInputType {
    INTERACT_BLOCK,  // Click derecho en bloque (botón, palanca)
    ITEM_INSERT,     // Meter item en marco/contenedor
    REGION_ENTER,    // Pisar una zona
    MOB_KILL_TRIGGER // Matar un mob específico cuenta como "activar botón"
}

enum class PuzzleConditionType {
    SINGLE_ACTIVATE,    // Basta con activar 1 input
    SIMULTANEOUS_STATE, // Todos los inputs activos a la vez (ventana de tiempo)
    SEQUENCE,           // Orden estricto (A -> B -> C)
    THRESHOLD_COUNTER,  // Activar X cantidad sin importar orden
    ITEM_DELIVERY       // Entregar un item específico (variante de insert)
}

enum class CombatTrigger {
    ON_ENTRY,   // Spawnean apenas entran a la sala
    ON_INTERACT // Spawnean al tocar algo
}