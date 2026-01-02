package org.ReDiego0.noctis.config

import org.bukkit.Material
import org.ReDiego0.noctis.Noctis
import java.util.EnumMap

class NoctisConfig(private val plugin: Noctis) {

    // Gameplay Settings
    var worldName: String = "world"
    var baseRadiation: Double = 1.5
    var nightMultiplier: Double = 3.0
    var townyCleanupRate: Double = 20.0
    var maxMitigationCap: Double = 0.90
    var criticalDamage: Double = 2.0

    // Economy Settings
    var currencyName: String = "<gradient:#00ffff:#0088ff>Célula de Energía</gradient>"
    var currencyMaterial: Material = Material.AMETHYST_SHARD
    var currencyModelData: Int = 0
    var taxCost: Int = 10
    var taxIntervalHours: Long = 24
    var taxIntervalMinutes: Long = 0

    // Party Settings
    var partyMaxSize: Int = 5

    // Jobs Settings
    var jobsUseGroups: Boolean = false

    // Visual Settings
    var barSymbol: String = "|"
    var barLength: Int = 20
    var colorLowStart: String = "#55FF55"
    var colorLowEnd: String = "#00AA00"
    var colorMidStart: String = "#FFFF55"
    var colorMidEnd: String = "#FFAA00"
    var colorHighStart: String = "#FF5555"
    var colorHighEnd: String = "#AA0000"

    // Armor Cache (Optimización O(1))
    private val armorCache = EnumMap<Material, Double>(Material::class.java)

    init {
        load()
    }

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        val config = plugin.config

        // Load Basic Settings
        worldName = config.getString("settings.world-name", "world")!!
        baseRadiation = config.getDouble("settings.base-radiation", 1.5)
        nightMultiplier = config.getDouble("settings.night-multiplier", 3.0)
        townyCleanupRate = config.getDouble("settings.towny-cleanup-rate", 20.0)
        maxMitigationCap = config.getDouble("settings.max-mitigation-cap", 0.90)
        criticalDamage = config.getDouble("settings.critical-damage", 2.0)

        // Economy
        currencyName = config.getString("economy.currency-name", "<gradient:#00ffff:#0088ff>Célula de Energía</gradient>")!!
        val matName = config.getString("economy.item-material", "AMETHYST_SHARD")!!
        currencyMaterial = Material.getMaterial(matName) ?: Material.AMETHYST_SHARD
        currencyModelData = config.getInt("economy.custom-model-data", 0)

        taxCost = config.getInt("economy.taxes.cost", 10)
        taxIntervalHours = config.getLong("economy.taxes.interval-hours", 24)
        taxIntervalMinutes = config.getLong("economy.taxes.interval-minutes", 0)

        // Party
        partyMaxSize = config.getInt("party.max-size", 5)

        // Jobs
        jobsUseGroups = config.getBoolean("jobs.use-groups", false)

        // Load Visuals
        barSymbol = config.getString("visuals.bar-symbol", "|")!!
        barLength = config.getInt("visuals.bar-length", 20)
        colorLowStart = config.getString("visuals.color-low-start", "#55FF55")!!
        colorLowEnd = config.getString("visuals.color-low-end", "#00AA00")!!
        colorMidStart = config.getString("visuals.color-mid-start", "#FFFF55")!!
        colorMidEnd = config.getString("visuals.color-mid-end", "#FFAA00")!!
        colorHighStart = config.getString("visuals.color-high-start", "#FF5555")!!
        colorHighEnd = config.getString("visuals.color-high-end", "#AA0000")!!

        // Load Armor Map
        armorCache.clear()
        val armorSection = config.getConfigurationSection("armor-protection")
        armorSection?.getKeys(false)?.forEach { key ->
            val mat = Material.getMaterial(key.uppercase())
            if (mat != null) {
                armorCache[mat] = armorSection.getDouble(key)
            } else {
                plugin.logger.warning("Material invalido en config: $key")
            }
        }
    }

    fun getArmorProtection(material: Material): Double {
        return armorCache.getOrDefault(material, 0.0)
    }
}