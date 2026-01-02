package org.ReDiego0.noctis.party

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.minimessage.MiniMessage

class PartyCommand(private val manager: PartyManager) {

    private val mm = MiniMessage.miniMessage()

    init {
        register()
    }

    private fun register() {
        CommandAPICommand("party")
            .withAliases("p", "noctisparty")
            .withSubcommand(createCmd())
            .withSubcommand(inviteCmd())
            .withSubcommand(acceptCmd())
            .withSubcommand(leaveCmd())
            .withSubcommand(kickCmd())
            .withSubcommand(infoCmd())
            .register()
    }

    private fun createCmd() = CommandAPICommand("create")
        .executesPlayer(PlayerCommandExecutor { player, _ ->
            if (manager.createParty(player) != null) {
                player.sendMessage(mm.deserialize("<green>Grupo creado. Usa <yellow>/party invite <jugador>"))
            } else {
                player.sendMessage(mm.deserialize("<red>Ya tienes un grupo."))
            }
        })

    private fun inviteCmd() = CommandAPICommand("invite")
        .withArguments(PlayerArgument("target"))
        .executesPlayer(PlayerCommandExecutor { player, args ->
            val target = args["target"] as org.bukkit.entity.Player

            if (player == target) {
                player.sendMessage(mm.deserialize("<red>No puedes invitarte a ti mismo."))
                return@PlayerCommandExecutor
            }

            val result = manager.invitePlayer(player, target)
            when (result) {
                "success" -> {
                    player.sendMessage(mm.deserialize("<green>Invitación enviada a ${target.name}."))
                    target.sendMessage(mm.deserialize("<green>${player.name} te invita a su grupo. <yellow><click:run_command:'/party accept'>[CLICK PARA ACEPTAR]</click>"))
                }
                "no_party" -> player.sendMessage(mm.deserialize("<red>No tienes un grupo. Crea uno primero."))
                "not_leader" -> player.sendMessage(mm.deserialize("<red>Solo el líder puede invitar."))
                "target_has_party" -> player.sendMessage(mm.deserialize("<red>${target.name} ya tiene grupo."))
                "party_full" -> player.sendMessage(mm.deserialize("<red>El grupo está lleno (Máx 5)."))
                "geo_conflict" -> player.sendMessage(mm.deserialize("<dark_red>DENEGADO: <gray>Conflictos diplomáticos impiden esta alianza."))
            }
        })

    private fun acceptCmd() = CommandAPICommand("accept")
        .executesPlayer(PlayerCommandExecutor { player, _ ->
            if (manager.acceptInvite(player)) {
            } else {
                player.sendMessage(mm.deserialize("<red>No tienes invitaciones pendientes."))
            }
        })

    private fun leaveCmd() = CommandAPICommand("leave")
        .executesPlayer(PlayerCommandExecutor { player, _ ->
            if (manager.getPlayerParty(player.uniqueId) == null) {
                player.sendMessage(mm.deserialize("<red>No estás en un grupo."))
                return@PlayerCommandExecutor
            }
            manager.leaveParty(player)
            player.sendMessage(mm.deserialize("<yellow>Has abandonado el grupo."))
        })

    private fun kickCmd() = CommandAPICommand("kick")
        .withArguments(StringArgument("member_name"))
        .executesPlayer(PlayerCommandExecutor { player, args ->
            val targetName = args["member_name"] as String
            if (!manager.kickPlayer(player, targetName)) {
                player.sendMessage(mm.deserialize("<red>No se pudo expulsar (No eres líder o jugador no encontrado)."))
            }
        })

    private fun infoCmd() = CommandAPICommand("info")
        .executesPlayer(PlayerCommandExecutor { player, _ ->
            val party = manager.getPlayerParty(player.uniqueId)
            if (party == null) {
                player.sendMessage(mm.deserialize("<red>No tienes grupo."))
                return@PlayerCommandExecutor
            }

            player.sendMessage(mm.deserialize("<green>=== Información del Grupo ==="))
            player.sendMessage(mm.deserialize("<gray>Líder: <gold>${party.getLeader()?.getPlayer()?.name ?: "Offline"}"))
            player.sendMessage(mm.deserialize("<gray>Miembros (${party.getSize()}/5):"))
            party.getMembers().forEach {
                val status = if (it.isOnline()) "<green>Online" else "<red>Offline"
                player.sendMessage(mm.deserialize(" - <white>${it.getPlayer()?.name ?: "Unknown"} <gray>($status)"))
            }
        })
}