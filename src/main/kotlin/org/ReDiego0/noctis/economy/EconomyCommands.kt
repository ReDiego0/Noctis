package org.ReDiego0.noctis.economy

import com.palmergames.bukkit.towny.TownyAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.minimessage.MiniMessage

class EconomyCommands(
    private val currencyManager: CurrencyManager,
    private val database: BankDatabase,
    private val taxTask: TaxTask
) {

    private val mm = MiniMessage.miniMessage()
    private val townyAPI = TownyAPI.getInstance()

    init {
        registerAdminCommands()
        registerBankCommands()
        registerPayCommand()
    }

    private fun registerAdminCommands() {
        CommandAPICommand("noctiseco")
            .withPermission("noctis.admin.economy")
            .withSubcommand(
                CommandAPICommand("give")
                    .withArguments(PlayerArgument("target"))
                    .withArguments(IntegerArgument("amount", 1))
                    .executes(CommandExecutor { sender, args ->
                        val target = args["target"] as org.bukkit.entity.Player
                        val amount = args["amount"] as Int
                        currencyManager.give(target, amount)
                        sender.sendMessage(mm.deserialize("<green>Diste $amount a ${target.name}"))
                    })
            )
            .withSubcommand(
                CommandAPICommand("forcetax")
                    .executes(CommandExecutor { sender, _ ->
                        taxTask.forceCollection()
                        sender.sendMessage(mm.deserialize("<green>Cobro de impuestos forzado."))
                    })
            )
            .register()
    }

    private fun registerBankCommands() {
        CommandAPICommand("bank")
            .withSubcommand(
                CommandAPICommand("balance")
                    .withAliases("ver", "saldo")
                    .executesPlayer(PlayerCommandExecutor { player, _ ->
                        val resident = townyAPI.getResident(player)
                        if (resident == null || !resident.hasTown()) {
                            player.sendMessage(mm.deserialize("<red>No perteneces a una ciudad."))
                            return@PlayerCommandExecutor
                        }
                        val town = resident.townOrNull!!
                        val bal = database.getBalance(town.uuid)
                        player.sendMessage(mm.deserialize("<green>Banco de ${town.name}: <white>$bal"))
                    })
            )
            .withSubcommand(
                CommandAPICommand("deposit")
                    .withAliases("depositar")
                    .withArguments(IntegerArgument("amount", 1))
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val resident = townyAPI.getResident(player)
                        if (resident == null || !resident.hasTown()) {
                            player.sendMessage(mm.deserialize("<red>No perteneces a una ciudad."))
                            return@PlayerCommandExecutor
                        }

                        val amount = args["amount"] as Int
                        val town = resident.townOrNull!!

                        if (currencyManager.take(player, amount)) {
                            database.addBalance(town.uuid, amount)
                            player.sendMessage(mm.deserialize("<green>Depositaste <white>$amount <green>al banco de la ciudad."))

                            if (!taxTask.isTownProtected(town.uuid)) {
                                taxTask.tryReactivate(town.uuid, town.name)
                            }
                        } else {
                            player.sendMessage(mm.deserialize("<red>No tienes suficiente combustible en el inventario."))
                        }
                    })
            )
            .register()
    }

    private fun registerPayCommand() {
        CommandAPICommand("payfuel")
            .withArguments(PlayerArgument("target"))
            .withArguments(IntegerArgument("amount", 1))
            .executesPlayer(PlayerCommandExecutor { player, args ->
                val target = args["target"] as org.bukkit.entity.Player
                val amount = args["amount"] as Int

                if (currencyManager.take(player, amount)) {
                    currencyManager.give(target, amount)
                    player.sendMessage(mm.deserialize("<green>Enviaste <white>$amount <green>a ${target.name}."))
                    target.sendMessage(mm.deserialize("<green>Recibiste <white>$amount <green>de ${player.name}."))
                } else {
                    player.sendMessage(mm.deserialize("<red>No tienes suficientes items."))
                }
            })
            .register()
    }
}