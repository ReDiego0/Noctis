package org.ReDiego0.noctis.economy

import com.palmergames.bukkit.towny.TownyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EconomyCommands(
    private val currencyManager: CurrencyManager,
    private val database: BankDatabase,
    private val taxTask: TaxTask
) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()
    private val townyAPI = TownyAPI.getInstance()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val cmdName = command.name.lowercase()

        if (cmdName == "noctiseco") {
            if (!sender.hasPermission("noctis.admin.economy")) {
                sender.sendMessage(mm.deserialize("<red>Sin permiso."))
                return true
            }
            if (args.isEmpty()) {
                sender.sendMessage(mm.deserialize("<yellow>Uso: /noctiseco <give|forcetax>"))
                return true
            }

            when (args[0].lowercase()) {
                "give" -> {
                    if (args.size < 3) {
                        sender.sendMessage(mm.deserialize("<red>Uso: /noctiseco give <jugador> <cantidad>"))
                        return true
                    }
                    val target = Bukkit.getPlayer(args[1])
                    if (target == null) {
                        sender.sendMessage(mm.deserialize("<red>Jugador no encontrado."))
                        return true
                    }
                    val amount = args[2].toIntOrNull()
                    if (amount == null || amount <= 0) {
                        sender.sendMessage(mm.deserialize("<red>Cantidad inválida."))
                        return true
                    }
                    currencyManager.give(target, amount)
                    sender.sendMessage(mm.deserialize("<green>Diste $amount a ${target.name}"))
                }
                "forcetax" -> {
                    taxTask.forceCollection()
                    sender.sendMessage(mm.deserialize("<green>Cobro de impuestos forzado."))
                }
                else -> sender.sendMessage(mm.deserialize("<red>Subcomando desconocido."))
            }
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Solo jugadores."))
            return true
        }

        if (cmdName == "bank") {
            if (args.isEmpty()) {
                sender.sendMessage(mm.deserialize("<yellow>Uso: <gray>/bank <deposit|balance>"))
                return true
            }

            val resident = townyAPI.getResident(sender)
            if (resident == null || !resident.hasTown()) {
                sender.sendMessage(mm.deserialize("<red>No perteneces a una ciudad."))
                return true
            }
            val town = resident.townOrNull!!

            when (args[0].lowercase()) {
                "balance", "ver", "saldo" -> {
                    val bal = database.getBalance(town.uuid)
                    sender.sendMessage(mm.deserialize("<green>Banco de ${town.name}: <white>$bal"))
                }
                "deposit", "depositar" -> {
                    if (args.size < 2) {
                        sender.sendMessage(mm.deserialize("<red>Uso: /bank deposit <cantidad>"))
                        return true
                    }
                    val amount = args[1].toIntOrNull()
                    if (amount == null || amount <= 0) {
                        sender.sendMessage(mm.deserialize("<red>Cantidad inválida."))
                        return true
                    }

                    if (currencyManager.take(sender, amount)) {
                        database.addBalance(town.uuid, amount)
                        sender.sendMessage(mm.deserialize("<green>Depositaste <white>$amount <green>al banco de la ciudad."))

                        if (!taxTask.isTownProtected(town.uuid)) {
                            taxTask.tryReactivate(town.uuid, town.name)
                        }
                    } else {
                        sender.sendMessage(mm.deserialize("<red>No tienes suficiente combustible."))
                    }
                }
            }
            return true
        }

        if (cmdName == "payfuel") {
            if (args.size < 2) {
                sender.sendMessage(mm.deserialize("<red>Uso: /payfuel <jugador> <cantidad>"))
                return true
            }
            val target = Bukkit.getPlayer(args[0])
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>Jugador no encontrado."))
                return true
            }
            val amount = args[1].toIntOrNull()
            if (amount == null || amount <= 0) {
                sender.sendMessage(mm.deserialize("<red>Cantidad inválida."))
                return true
            }

            if (currencyManager.take(sender, amount)) {
                currencyManager.give(target, amount)
                sender.sendMessage(mm.deserialize("<green>Enviaste <white>$amount <green>a ${target.name}."))
                target.sendMessage(mm.deserialize("<green>Recibiste <white>$amount <green>de ${sender.name}."))
            } else {
                sender.sendMessage(mm.deserialize("<red>No tienes suficientes items."))
            }
            return true
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        val cmdName = command.name.lowercase()

        if (cmdName == "noctiseco") {
            if (args.size == 1) return listOf("give", "forcetax").filter { it.startsWith(args[0], true) }
            if (args.size == 2 && args[0].equals("give", true)) return null
        }
        if (cmdName == "bank") {
            if (args.size == 1) return listOf("deposit", "balance").filter { it.startsWith(args[0], true) }
        }
        if (cmdName == "payfuel") {
            if (args.size == 1) return null
        }
        return emptyList()
    }
}