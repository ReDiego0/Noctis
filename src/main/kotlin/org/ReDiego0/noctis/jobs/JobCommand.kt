package org.ReDiego0.noctis.jobs

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.ReDiego0.noctis.Noctis

class JobCommand(private val plugin: Noctis, private val manager: JobManager) {

    private val mm = MiniMessage.miniMessage()

    init {
        register()
    }

    private fun register() {
        val root = CommandAPICommand("noctisjobs")
            .withAliases("jobs")

        root.withSubcommand(createRefreshCmd())

        root.withSubcommand(createJoinCmd())

        root.register()
    }

    private fun createJoinCmd(): CommandAPICommand {
        return CommandAPICommand("join")
            .withPermission("noctis.jobs.join")
            .withArguments(
                StringArgument("job_id")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        JobType.entries.filter { it != JobType.NONE }.map { it.id }.toTypedArray()
                    })
            )
            .executesPlayer(PlayerCommandExecutor { player, args ->
                val jobId = args["job_id"] as String
                val targetJob = JobType.entries.find { it.id.equals(jobId, true) }

                if (targetJob == null || targetJob == JobType.NONE) {
                    player.sendMessage(mm.deserialize("<red>Trabajo no válido."))
                    return@PlayerCommandExecutor
                }

                if (JobType.getJob(player) == targetJob) {
                    player.sendMessage(mm.deserialize("<red>Ya eres un ${targetJob.displayName}."))
                    return@PlayerCommandExecutor
                }

                if (changeJob(player, targetJob)) {
                    player.sendMessage(mm.deserialize("<green>¡Ahora eres un <bold>${targetJob.displayName}</bold>!"))
                    manager.refreshJobBuffs(player)
                } else {
                    player.sendMessage(mm.deserialize("<red>Error al asignar el trabajo. Contacta a un admin."))
                }
            })
    }

    private fun changeJob(player: Player, newJob: JobType): Boolean {
        val perms = plugin.perms
        if (perms == null) {
            plugin.logger.severe("Vault no encontrado. No se pueden gestionar trabajos.")
            return false
        }

        JobType.entries.forEach { job ->
            if (job != JobType.NONE) {
                if (!plugin.noctisConfig.jobsUseGroups) {
                    if (perms.has(player, job.permission)) {
                        perms.playerRemove(player, job.permission)
                    }
                } else {
                    if (perms.playerInGroup(player, job.id)) {
                        perms.playerRemoveGroup(player, job.id)
                    }
                }
            }
        }

        if (!plugin.noctisConfig.jobsUseGroups) {
            return perms.playerAdd(player, newJob.permission)
        } else {
            return perms.playerAddGroup(player, newJob.id)
        }
    }

    private fun createRefreshCmd(): CommandAPICommand {
        return CommandAPICommand("refresh")
            .withPermission("noctis.admin.jobs")
            .withSubcommand(
                CommandAPICommand("all")
                    .executes(CommandExecutor { sender, _ ->
                        val onlinePlayers = Bukkit.getOnlinePlayers()
                        onlinePlayers.forEach { manager.refreshJobBuffs(it) }
                        sender.sendMessage(mm.deserialize("<green>Actualizados trabajos de <yellow>${onlinePlayers.size} <green>jugadores."))
                    })
            )
            .withSubcommand(
                CommandAPICommand("player") // nombre intermedio opcional, o argumento directo
                    .withArguments(PlayerArgument("target"))
                    .executes(CommandExecutor { sender, args ->
                        val target = args["target"] as Player
                        manager.refreshJobBuffs(target)
                        val currentJob = JobType.getJob(target)
                        sender.sendMessage(mm.deserialize("<green>Trabajo de ${target.name} actualizado: <gold>${currentJob.displayName}"))
                    })
            )
    }
}