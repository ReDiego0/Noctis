package org.ReDiego0.noctis.party

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PartyCommand(private val manager: PartyManager) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Solo jugadores.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<yellow>Uso: /party <create|invite|accept|leave|kick|info>"))
            return true
        }

        when (args[0].lowercase()) {
            "create" -> {
                if (manager.createParty(sender) != null) {
                    sender.sendMessage(mm.deserialize("<green>Grupo creado. Usa <yellow>/party invite <jugador>"))
                } else {
                    sender.sendMessage(mm.deserialize("<red>Ya tienes un grupo."))
                }
            }
            "invite" -> {
                if (args.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /party invite <jugador>"))
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>Jugador no encontrado."))
                    return true
                }
                if (sender == target) {
                    sender.sendMessage(mm.deserialize("<red>No puedes invitarte a ti mismo."))
                    return true
                }

                val result = manager.invitePlayer(sender, target)
                when (result) {
                    "success" -> {
                        sender.sendMessage(mm.deserialize("<green>Invitación enviada a ${target.name}."))
                        target.sendMessage(mm.deserialize("<green>${sender.name} te invita a su grupo. <yellow><click:run_command:'/party accept'>[CLICK PARA ACEPTAR]</click>"))
                    }
                    "no_party" -> sender.sendMessage(mm.deserialize("<red>No tienes un grupo. Crea uno primero."))
                    "not_leader" -> sender.sendMessage(mm.deserialize("<red>Solo el líder puede invitar."))
                    "target_has_party" -> sender.sendMessage(mm.deserialize("<red>${target.name} ya tiene grupo."))
                    "party_full" -> sender.sendMessage(mm.deserialize("<red>El grupo está lleno."))
                    "geo_conflict" -> sender.sendMessage(mm.deserialize("<dark_red>DENEGADO: <gray>Conflictos diplomáticos."))
                }
            }
            "accept" -> {
                if (!manager.acceptInvite(sender)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes invitaciones pendientes."))
                }
            }
            "leave" -> {
                if (manager.getPlayerParty(sender.uniqueId) == null) {
                    sender.sendMessage(mm.deserialize("<red>No estás en un grupo."))
                } else {
                    manager.leaveParty(sender)
                    sender.sendMessage(mm.deserialize("<yellow>Has abandonado el grupo."))
                }
            }
            "kick" -> {
                if (args.size < 2) return false
                val targetName = args[1]
                if (!manager.kickPlayer(sender, targetName)) {
                    sender.sendMessage(mm.deserialize("<red>No se pudo expulsar."))
                }
            }
            "info" -> {
                val party = manager.getPlayerParty(sender.uniqueId)
                if (party == null) {
                    sender.sendMessage(mm.deserialize("<red>No tienes grupo."))
                    return true
                }
                sender.sendMessage(mm.deserialize("<green>=== Información del Grupo ==="))
                sender.sendMessage(mm.deserialize("<gray>Líder: <gold>${party.getLeader()?.getPlayer()?.name ?: "Offline"}"))
                sender.sendMessage(mm.deserialize("<gray>Miembros (${party.getSize()}/5):"))
                party.getMembers().forEach {
                    val status = if (it.isOnline()) "<green>Online" else "<red>Offline"
                    sender.sendMessage(mm.deserialize(" - <white>${it.getPlayer()?.name ?: "Unknown"} <gray>($status)"))
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("create", "invite", "accept", "leave", "kick", "info").filter { it.startsWith(args[0], true) }

        if (args.size == 2) {
            if (args[0].equals("invite", true)) return null // Jugadores online
            if (args[0].equals("kick", true)) {
                if (sender is Player) {
                    val party = manager.getPlayerParty(sender.uniqueId)
                    if (party != null) {
                        return party.getMembers().mapNotNull { it.getPlayer()?.name }.filter { it.startsWith(args[1], true) }
                    }
                }
            }
        }
        return emptyList()
    }
}