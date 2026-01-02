package org.ReDiego0.noctis.economy

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.ReDiego0.noctis.Noctis
import org.ReDiego0.noctis.config.NoctisConfig

class CurrencyManager(private val plugin: Noctis, private val config: NoctisConfig) {

    private val mm = MiniMessage.miniMessage()
    private val currencyKey = NamespacedKey(plugin, "noctis_currency")

    fun getItem(amount: Int): ItemStack {
        val item = ItemStack(config.currencyMaterial, amount)
        val meta = item.itemMeta

        val nameComponent = mm.deserialize(config.currencyName)
        meta.displayName(nameComponent)

        val cmd = config.currencyModelData
        if (cmd > 0) meta.setCustomModelData(cmd)

        meta.persistentDataContainer.set(currencyKey, PersistentDataType.BYTE, 1.toByte())

        item.itemMeta = meta
        return item
    }

    fun isValidCurrency(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false
        if (!item.hasItemMeta()) return false
        return item.itemMeta.persistentDataContainer.has(currencyKey, PersistentDataType.BYTE)
    }

    fun give(player: Player, amount: Int) {
        val item = getItem(amount)
        val left = player.inventory.addItem(item)
        left.values.forEach { didNotFit ->
            player.world.dropItemNaturally(player.location, didNotFit)
        }
    }

    fun take(player: Player, amount: Int): Boolean {
        if (count(player) < amount) return false
        var remaining = amount
        val inventory = player.inventory.contents

        for (i in inventory.indices) {
            val item = inventory[i]
            if (item != null && isValidCurrency(item)) {
                if (item.amount <= remaining) {
                    remaining -= item.amount
                    player.inventory.setItem(i, null) // Eliminación segura
                } else {
                    item.amount -= remaining
                    remaining = 0
                }
            }
            if (remaining <= 0) break
        }
        return true
    }

    fun count(player: Player): Int {
        var total = 0
        for (item in player.inventory.contents) {
            if (item != null && isValidCurrency(item)) {
                total += item.amount
            }
        }
        return total
    }
}