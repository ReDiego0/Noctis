package org.ReDiego0.noctis.jobs

import org.bukkit.Material
import org.bukkit.entity.Player

enum class JobType(
    val id: String,
    val displayName: String,
    val permission: String,
    val icon: Material
) {
    PROSPECTOR("prospector", "Prospector", "noctis.job.prospector", Material.GOLDEN_PICKAXE),
    BIOCHEMIST("biochemist", "Bio-Químico", "noctis.job.biochemist", Material.BREWING_STAND),
    FABRICATOR("fabricator", "Fabricador", "noctis.job.fabricator", Material.ANVIL),
    ARCHITECT("architect", "Arquitecto", "noctis.job.architect", Material.SCAFFOLDING),
    SYNTHESIZER("synthesizer", "Sintetizador", "noctis.job.synthesizer", Material.COOKED_BEEF),
    NONE("none", "Desempleado", "", Material.BARRIER);

    companion object {
        fun getJob(player: Player): JobType {
            return entries.firstOrNull { it != NONE && player.hasPermission(it.permission) } ?: NONE
        }
    }
}