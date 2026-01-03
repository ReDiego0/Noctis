package org.ReDiego0.noctis.dungeons

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.ReDiego0.noctis.party.PartyManager

class DungeonCommand(
    private val dungeonManager: DungeonManager,
    private val partyManager: PartyManager
) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (!sender.hasPermission("noctis.dungeons.play")) {
            sender.sendMessage(mm.deserialize("<red>Sin permiso."))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<yellow>Uso: /dungeon start <id>"))
            return true
        }

        if (args[0].equals("start", true)) {
            if (args.size < 2) {
                sender.sendMessage(mm.deserialize("<red>Falta la ID de la dungeon."))
                return true
            }

            val dungeonId = args[1]
            val dungeonData = dungeonManager.loader.getDungeon(dungeonId)

            if (dungeonData == null) {
                sender.sendMessage(mm.deserialize("<red>Dungeon '$dungeonId' no encontrada."))
                return true
            }

            // Verificar Party
            val party = partyManager.getPlayerParty(sender.uniqueId)
            if (party == null) {
                // Crear party temporal de 1 solo si no tiene (Opcional, o forzar a crear party)
                // Por ahora forzamos que tenga party
                sender.sendMessage(mm.deserialize("<red>Debes estar en un grupo (incluso solo). Usa /party create."))
                return true
            }

            if (party.getLeader()?.uuid != sender.uniqueId) {
                sender.sendMessage(mm.deserialize("<red>Solo el líder del grupo puede iniciar la incursión."))
                return true
            }

            // Validar que todos estén cerca (Opcional pero recomendado)
            if (!party.areAllMembersNearby(sender.location, 20.0)) {
                sender.sendMessage(mm.deserialize("<red>Todos los miembros deben estar cerca para entrar."))
                return true
            }

            sender.sendMessage(mm.deserialize("<green>Iniciando generación de ${dungeonData.displayName}..."))
            dungeonManager.startDungeon(party, dungeonId)
            return true
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("start").filter { it.startsWith(args[0], true) }

        if (args.size == 2 && args[0].equals("start", true)) {
            // Sugerir IDs de dungeons cargadas (necesitas exponer el mapa en el loader o un método getIds)
            // Por ahora hardcodeado o accesible via reflection/getter
            return listOf("tutorial_vault") // Ejemplo
        }
        return emptyList()
    }
}