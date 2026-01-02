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
        val matName = plugin.config.getString("economy.item-material", "AMETHYST_SHARD")!!
        val material = Material.getMaterial(matName) ?: Material.AMETHYST_SHARD

        val item = ItemStack(material, amount)
        val meta = item.itemMeta

        val displayName = plugin.config.getString("economy.currency-name", "<aqua>Energy Cell")!!
        meta.displayName(mm.deserialize(displayName))

        val cmd = plugin.config.getInt("economy.custom-model-data", 0)
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
        var toRemove = amount
        for (item in player.inventory.contents) {
            if (item != null && isValidCurrency(item)) {
                val currentAmount = item.amount
                if (currentAmount <= toRemove) {
                    toRemove -= currentAmount
                    item.amount = 0
                } else {
                    item.amount = currentAmount - toRemove
                    toRemove = 0
                }
            }
            if (toRemove <= 0) break
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