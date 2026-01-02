package org.ReDiego0.noctis.economy

import com.palmergames.bukkit.towny.TownyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EconomyCommands(
    private val currencyManager: CurrencyManager,
    private val database: BankDatabase,
    private val taxTask: TaxTask
) : CommandExecutor {

    private val mm = MiniMessage.miniMessage()
    private val townyAPI = TownyAPI.getInstance()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (label.equals("noctiseco", ignoreCase = true)) {
            if (!sender.hasPermission("noctis.admin.economy")) {
                sender.sendMessage(mm.deserialize("<red>Sin permiso."))
                return true
            }
            if (args.isEmpty()) return false

            when (args[0].lowercase()) {
                "give" -> {
                    val target = Bukkit.getPlayer(args[1]) ?: return true
                    val amount = args[2].toIntOrNull() ?: return true
                    currencyManager.give(target, amount)
                    sender.sendMessage(mm.deserialize("<green>Diste $amount a ${target.name}"))
                }
                "forcetax" -> {
                    taxTask.forceCollection()
                    sender.sendMessage(mm.deserialize("<green>Cobro de impuestos forzado."))
                }
            }
            return true
        }

        if (sender !is Player) return true

        if (label.equals("bank", ignoreCase = true)) {
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
                "balance", "ver" -> {
                    val bal = database.getBalance(town.uuid)
                    sender.sendMessage(mm.deserialize("<green>Banco de ${town.name}: <white>$bal"))
                }
                "deposit" -> {
                    val amount = args.getOrNull(1)?.toIntOrNull()
                    if (amount == null || amount <= 0) {
                        sender.sendMessage(mm.deserialize("<red>Cantidad inválida."))
                        return true
                    }
                    if (currencyManager.take(sender, amount)) {
                        database.addBalance(town.uuid, amount)
                        sender.sendMessage(mm.deserialize("<green>Depositaste <white>$amount <green>al banco de la ciudad."))
                    } else {
                        sender.sendMessage(mm.deserialize("<red>No tienes suficiente combustible en el inventario."))
                    }
                }
            }
            return true
        }

        if (label.equals("payfuel", ignoreCase = true)) {
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
            if (amount == null || amount <= 0) return true

            if (currencyManager.take(sender, amount)) {
                currencyManager.give(target, amount)
                sender.sendMessage(mm.deserialize("<green>Enviaste <white>$amount <green>a ${target.name}."))
                target.sendMessage(mm.deserialize("<green>Recibiste <white>$amount <green>de ${sender.name}."))
            } else {
                sender.sendMessage(mm.deserialize("<red>No tienes suficientes items."))
            }
            return true
        }

        return true
    }
}