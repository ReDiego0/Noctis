package org.ReDiego0.noctis.jobs

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class JobCommand(private val manager: JobManager) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("noctis.admin.jobs")) {
            sender.sendMessage(mm.deserialize("<red>Sin permiso."))
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        val action = args[0].lowercase()

        if (action == "refresh") {
            if (args.size < 2) {
                sender.sendMessage(mm.deserialize("<red>Uso: /noctisjobs refresh <player|all>"))
                return true
            }

            val targetName = args[1]

            if (targetName.lowercase() == "all") {
                val onlinePlayers = Bukkit.getOnlinePlayers()
                onlinePlayers.forEach { manager.refreshJobBuffs(it) }
                sender.sendMessage(mm.deserialize("<green>Actualizados trabajos de <yellow>${onlinePlayers.size} <green>jugadores."))
                return true
            }

            val target = Bukkit.getPlayer(targetName)
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>Jugador no encontrado."))
                return true
            }

            manager.refreshJobBuffs(target)
            val currentJob = JobType.getJob(target)
            sender.sendMessage(mm.deserialize("<green>Trabajo de ${target.name} actualizado: <gold>${currentJob.displayName}"))
            return true
        }

        sendHelp(sender)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("refresh").filter { it.startsWith(args[0].lowercase()) }
        if (args.size == 2 && args[0].lowercase() == "refresh") {
            val names = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
            names.add("all")
            return names.filter { it.startsWith(args[1], true) }
        }
        return emptyList()
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<yellow>/noctisjobs refresh <player|all> <gray>- Recargar buffs de trabajos."))
    }
}