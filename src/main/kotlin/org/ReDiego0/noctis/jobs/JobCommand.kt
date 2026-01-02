package org.ReDiego0.noctis.jobs

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.executors.CommandExecutor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class JobCommand(private val manager: JobManager) {

    private val mm = MiniMessage.miniMessage()

    init {
        register()
    }

    private fun register() {
        CommandAPICommand("noctisjobs")
            .withPermission("noctis.admin.jobs")
            .withSubcommand(createRefreshCmd())
            .register()
    }

    private fun createRefreshCmd(): CommandAPICommand {
        return CommandAPICommand("refresh")
            .withArguments(LiteralArgument("all"))
            .executes(CommandExecutor { sender, _ ->
                val onlinePlayers = Bukkit.getOnlinePlayers()
                onlinePlayers.forEach { manager.refreshJobBuffs(it) }
                sender.sendMessage(mm.deserialize("<green>Actualizados trabajos de <yellow>${onlinePlayers.size} <green>jugadores."))
            })
            .withArguments(PlayerArgument("target"))
            .executes(CommandExecutor { sender, args ->
                val target = args["target"] as Player
                manager.refreshJobBuffs(target)
                val currentJob = JobType.getJob(target)
                sender.sendMessage(mm.deserialize("<green>Trabajo de ${target.name} actualizado: <gold>${currentJob.displayName}"))
            })
    }
}