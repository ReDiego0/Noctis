package org.ReDiego0.noctis.dungeons.listeners

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.ReDiego0.noctis.dungeons.DungeonManager

class DungeonPlayerListener(private val manager: DungeonManager) : Listener {

    private val mm = MiniMessage.miniMessage()

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (event.player.world.name == "noctis_dungeons" && event.player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (event.player.world.name == "noctis_dungeons" && event.player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onExitTrigger(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.PHYSICAL) return
        val player = event.player
        if (player.world.name != "noctis_dungeons") return

        val block = event.clickedBlock ?: return
        val dungeon = manager.getDungeonByPlayer(player) ?: return
        val room = dungeon.getCurrentRoom() ?: return

        // Ubicación esperada de la salida
        val exitLoc = room.getExitLocation()

        // Verificar si el bloque clickeado está en la ubicación de salida (con tolerancia)
        if (block.location.distanceSquared(exitLoc) < 1.5) {

            if (room.isCompleted) {
                dungeon.advanceToNextRoom()
            } else {
                player.sendMessage(mm.deserialize("<red>La salida está bloqueada. Completa el objetivo primero."))
                player.playSound(player.location, org.bukkit.Sound.BLOCK_CHEST_LOCKED, 1f, 0.5f)
            }

            // Cancelar interacción física para que no rompan cultivos o usen items
            event.isCancelled = true
        }
    }
}