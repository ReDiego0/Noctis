package org.ReDiego0.noctis.jobs

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.ReDiego0.noctis.Noctis

class JobCommand(private val plugin: Noctis, private val manager: JobManager) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "join" -> {
                if (sender !is Player) return true
                if (!sender.hasPermission("noctis.jobs.join")) {
                    sender.sendMessage(mm.deserialize("<red>Sin permiso."))
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /noctisjobs join <trabajo>"))
                    return true
                }

                val jobId = args[1]
                val targetJob = JobType.entries.find { it.id.equals(jobId, true) }

                if (targetJob == null || targetJob == JobType.NONE) {
                    sender.sendMessage(mm.deserialize("<red>Trabajo no válido."))
                    return true
                }
                if (JobType.getJob(sender) == targetJob) {
                    sender.sendMessage(mm.deserialize("<red>Ya eres un ${targetJob.displayName}."))
                    return true
                }

                if (changeJob(sender, targetJob)) {
                    sender.sendMessage(mm.deserialize("<green>¡Ahora eres un <bold>${targetJob.displayName}</bold>!"))
                    manager.refreshJobBuffs(sender)
                } else {
                    sender.sendMessage(mm.deserialize("<red>Error al asignar el trabajo."))
                }
                return true
            }

            "refresh" -> {
                if (!sender.hasPermission("noctis.admin.jobs")) return true
                if (args.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /noctisjobs refresh <all|player>"))
                    return true
                }

                if (args[1].equals("all", true)) {
                    val online = Bukkit.getOnlinePlayers()
                    online.forEach { manager.refreshJobBuffs(it) }
                    sender.sendMessage(mm.deserialize("<green>Refrescados ${online.size} jugadores."))
                } else {
                    val target = Bukkit.getPlayer(args[1])
                    if (target == null) {
                        sender.sendMessage(mm.deserialize("<red>Jugador no encontrado."))
                        return true
                    }
                    manager.refreshJobBuffs(target)
                    sender.sendMessage(mm.deserialize("<green>Trabajo de ${target.name} refrescado."))
                }
                return true
            }
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("join", "refresh").filter { it.startsWith(args[0], true) }

        if (args.size == 2) {
            if (args[0].equals("join", true)) {
                return JobType.entries.filter { it != JobType.NONE }.map { it.id }.filter { it.startsWith(args[1], true) }
            }
            if (args[0].equals("refresh", true)) {
                val list = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
                list.add("all")
                return list.filter { it.startsWith(args[1], true) }
            }
        }
        return emptyList()
    }

    private fun changeJob(player: Player, newJob: JobType): Boolean {
        val perms = plugin.perms ?: return false
        JobType.entries.forEach { job ->
            if (job != JobType.NONE) {
                if (!plugin.noctisConfig.jobsUseGroups) {
                    if (perms.has(player, job.permission)) perms.playerRemove(player, job.permission)
                } else {
                    if (perms.playerInGroup(player, job.id)) perms.playerRemoveGroup(player, job.id)
                }
            }
        }
        return if (!plugin.noctisConfig.jobsUseGroups) perms.playerAdd(player, newJob.permission) else perms.playerAddGroup(player, newJob.id)
    }
}