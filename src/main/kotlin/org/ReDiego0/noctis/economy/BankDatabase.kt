package org.ReDiego0.noctis.economy

import org.bukkit.configuration.file.YamlConfiguration
import org.ReDiego0.noctis.Noctis
import java.io.File
import java.util.UUID

class BankDatabase(private val plugin: Noctis) {

    private val file = File(plugin.dataFolder, "town_banks.yml")
    private lateinit var config: YamlConfiguration

    init {
        load()
    }

    private fun load() {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun save() {
        config.save(file)
    }

    fun getBalance(townUUID: UUID): Int {
        return config.getInt("towns.$townUUID", 0)
    }

    fun addBalance(townUUID: UUID, amount: Int) {
        val current = getBalance(townUUID)
        config.set("towns.$townUUID", current + amount)
        save()
    }

    fun removeBalance(townUUID: UUID, amount: Int): Boolean {
        val current = getBalance(townUUID)
        if (current < amount) return false
        config.set("towns.$townUUID", current - amount)
        save()
        return true
    }

    fun setBalance(townUUID: UUID, amount: Int) {
        config.set("towns.$townUUID", amount)
        save()
    }

    fun setProtectionExpiry(townUUID: UUID, timestamp: Long) {
        config.set("protection_expiry.$townUUID", timestamp)
        save()
    }

    fun getProtectionExpiry(townUUID: UUID): Long {
        return config.getLong("protection_expiry.$townUUID", 0L)
    }

    fun getAllProtectionExpiries(): Map<UUID, Long> {
        val section = config.getConfigurationSection("protection_expiry") ?: return emptyMap()
        val map = mutableMapOf<UUID, Long>()

        for (key in section.getKeys(false)) {
            try {
                val uuid = UUID.fromString(key)
                val time = section.getLong(key)
                if (time > System.currentTimeMillis()) {
                    map[uuid] = time
                }
            } catch (e: Exception) {
            }
        }
        return map
    }

    fun getNextTaxTime(): Long {
        return config.getLong("system.next_tax_time", 0L)
    }

    fun setNextTaxTime(timestamp: Long) {
        config.set("system.next_tax_time", timestamp)
        save()
    }
}