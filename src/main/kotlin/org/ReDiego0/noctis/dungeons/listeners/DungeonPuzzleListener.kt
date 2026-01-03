package org.ReDiego0.noctis.dungeons.listeners

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.ReDiego0.noctis.dungeons.DungeonManager
import org.ReDiego0.noctis.dungeons.config.PuzzleConditionType
import org.ReDiego0.noctis.dungeons.config.PuzzleInputType

class DungeonPuzzleListener(private val manager: DungeonManager) : Listener {

    private val mm = MiniMessage.miniMessage()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player

        if (player.world.name != "noctis_dungeons") return

        val dungeon = manager.getDungeonByPlayer(player) ?: return
        val room = dungeon.getCurrentRoom() ?: return

        // Si no es sala de mecanismo o ya está completa, ignorar
        val logic = room.schematic.puzzleLogic ?: return
        if (room.isCompleted) return

        // alcular Coordenada Relativa
        // Relativa = Bloque - OrigenSala
        val relVector = block.location.toVector().subtract(room.pasteLocation.toVector())

        // Buscar si coincide con algún Input configurado
        for ((inputId, inputData) in logic.inputs) {

            // Chequeo de ubicación (con pequeña tolerancia por si acaso)
            if (inputData.location.distanceSquared(relVector) < 0.5) {

                // Chequeo de Tipo
                if (inputData.type == PuzzleInputType.INTERACT_BLOCK) {

                    // Chequeo de Material (Si se definió una lista restrictiva)
                    if (inputData.validMaterials.isNotEmpty() && !inputData.validMaterials.contains(block.type)) {
                        return
                    }

                    // --- INPUT VÁLIDO DETECTADO ---
                    handleInputActivation(dungeon, room, logic, inputId)
                    event.isCancelled = true // Evitar que el botón/palanca haga su lógica vanilla si queremos
                    return
                }
            }
        }
    }

    private fun handleInputActivation(
        dungeon: org.ReDiego0.noctis.dungeons.instance.DungeonInstance,
        room: org.ReDiego0.noctis.dungeons.instance.RoomInstance,
        logic: org.ReDiego0.noctis.dungeons.config.PuzzleLogic,
        inputId: String
    ) {
        val player = dungeon.getAlivePlayers().firstOrNull() ?: return // Solo para sonido
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

        // Registrar activación
        room.activatedInputs[inputId] = System.currentTimeMillis()

        // Validar Condición de Victoria
        val condition = logic.condition

        when (condition.type) {
            PuzzleConditionType.SINGLE_ACTIVATE -> {
                // Si este input está en la lista de targets
                if (condition.targets.contains(inputId)) {
                    dungeon.broadcast("<green>Mecanismo activado.")
                    room.complete()
                }
            }
            PuzzleConditionType.SEQUENCE -> {
                // TODO: Implementar validación de lista ordenada vs condition.sequenceOrder
            }
            PuzzleConditionType.SIMULTANEOUS_STATE -> {
                // Verificar si TODOS los targets fueron activados recientemente (window)
                val now = System.currentTimeMillis()
                val window = condition.timeWindowMs

                val allActive = condition.targets.all { targetId ->
                    val lastPress = room.activatedInputs[targetId] ?: 0L
                    (now - lastPress) <= window
                }

                if (allActive) {
                    dungeon.broadcast("<green>Sincronización completada.")
                    room.complete()
                }
            }
            else -> {}
        }
    }
}