package org.ReDiego0.noctis.jobs

import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.ReDiego0.noctis.Noctis

class JobManager(private val plugin: Noctis) {
    private val keyArchitectReach = NamespacedKey(plugin, "architect_reach")
    private val keyArchitectJump = NamespacedKey(plugin, "architect_jump")

    fun refreshJobBuffs(player: Player) {
        val job = JobType.getJob(player)

        player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.removeModifier(keyArchitectReach)
        player.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(keyArchitectJump)

        player.removePotionEffect(PotionEffectType.HASTE)
        player.removePotionEffect(PotionEffectType.CONDUIT_POWER)

        when (job) {
            JobType.ARCHITECT -> {
                val reachMod = AttributeModifier(
                    keyArchitectReach,
                    2.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND
                )
                player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.addModifier(reachMod)

                val jumpMod = AttributeModifier(
                    keyArchitectJump,
                    0.25,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.FEET
                )
                player.getAttribute(Attribute.JUMP_STRENGTH)?.addModifier(jumpMod)
            }
            JobType.PROSPECTOR -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 0, false, false, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.CONDUIT_POWER, PotionEffect.INFINITE_DURATION, 0, false, false, false))
            }
            else -> { }
        }
    }
}