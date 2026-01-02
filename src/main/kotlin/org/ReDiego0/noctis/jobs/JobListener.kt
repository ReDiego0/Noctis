package org.ReDiego0.noctis.jobs

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.ReDiego0.noctis.Noctis
import kotlin.random.Random

class JobListener(
    private val plugin: Noctis,
    private val manager: JobManager
) : Listener {

    private val mm = MiniMessage.miniMessage()
    private val keyCraftState = NamespacedKey(plugin, "fabricator_pending")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        manager.refreshJobBuffs(event.player)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            manager.refreshJobBuffs(event.player)
        }, 5L)
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            val player = (event.entity as? Player) ?: return
            if (JobType.getJob(player) == JobType.ARCHITECT) {
                event.damage *= 0.5
            }
        }
    }

    @EventHandler
    fun onBuild(event: BlockPlaceEvent) {
        val player = event.player
        if (JobType.getJob(player) == JobType.ARCHITECT) {
            if (event.block.type == Material.BEACON || event.block.type.name.contains("ORE")) return

            if (Random.nextInt(100) < 5) {
                val hand = event.hand
                val item = if (hand == EquipmentSlot.HAND) player.inventory.itemInMainHand else player.inventory.itemInOffHand
                item.amount = item.amount
                player.inventory.addItem(ItemStack(event.block.type))
            }
        }
    }

    @EventHandler
    fun onMine(event: BlockBreakEvent) {
        val player = event.player
        if (JobType.getJob(player) != JobType.PROSPECTOR) return

        val block = event.block
        val y = block.y

        if (y in -64..50 && block.type.name.contains("ORE")) {
            val tool = player.inventory.itemInMainHand
            if (!tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) {
                if (Random.nextBoolean()) {
                    val drops = block.getDrops(tool)
                    if (drops.isNotEmpty()) {
                        block.world.dropItemNaturally(block.location, ItemStack(drops.first().type))
                    }
                }
            }
        }
    }

    @EventHandler
    fun onHarvest(event: BlockBreakEvent) {
        val player = event.player
        if (JobType.getJob(player) != JobType.BIOCHEMIST) return

        val block = event.block
        val data = block.blockData
        if (data is Ageable) {
            if (data.age == data.maximumAge) {
                block.getDrops(player.inventory.itemInMainHand).forEach { drop ->
                    block.world.dropItemNaturally(block.location, drop)
                }
                event.expToDrop *= 2
            }
        }
    }

    private enum class SmithingBonus(val display: String, val weight: Int) {
        SHARP("<red>[+1 Daño]", 15),
        HARD("<blue>[+1 Dureza]", 15),
        DURABLE("<green>[Reforzado]", 20),
        VITALITY("<light_purple>[+1 ♥ Vitalidad]", 10),
        SWIFT("<white>[+5% Ligereza]", 10),
        IMPACT("<gray>[Golpe Pesado]", 10),
        BALANCED("<aqua>[Equilibrado]", 10),
        LUCKY("<yellow>[Afortunado]", 5),
        LEGENDARY("<gold><bold>[★ OBRA MAESTRA ★]", 5);
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        val result = recipe.result ?: return
        val typeName = result.type.name

        if (!typeName.contains("NETHERITE") && !typeName.contains("DIAMOND")) return

        val player = event.viewers.firstOrNull() as? Player ?: return

        if (JobType.getJob(player) == JobType.FABRICATOR) {
            val meta = result.itemMeta
            val pdc = meta.persistentDataContainer

            if (!pdc.has(keyCraftState, PersistentDataType.BYTE)) {
                pdc.set(keyCraftState, PersistentDataType.BYTE, 1.toByte())

                val lore = meta.lore() ?: ArrayList()
                lore.add(Component.empty())
                lore.add(mm.deserialize("<gray>Forjado por: <white>${player.name}"))
                lore.add(mm.deserialize("<yellow>Bonus: <obfuscated>???"))
                lore.add(mm.deserialize("<dark_gray>(Finaliza para revelar)"))

                meta.lore(lore)

                val previewItem = result.clone()
                previewItem.itemMeta = meta
                event.inventory.result = previewItem
            }
        }
    }

    @EventHandler
    fun onCraftSuccess(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return

        if (!event.isShiftClick) {
            val currentItem = event.currentItem
            if (currentItem != null && isPendingReveal(currentItem)) {
                finalizeItem(currentItem)
            }
            return
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            player.inventory.contents.forEach { item ->
                if (item != null && isPendingReveal(item)) {
                    finalizeItem(item)
                }
            }
        }, 1L)
    }

    private fun isPendingReveal(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        val pdc = item.itemMeta.persistentDataContainer
        return pdc.has(keyCraftState, PersistentDataType.BYTE)
    }

    private fun finalizeItem(item: ItemStack) {
        val meta = item.itemMeta
        meta.persistentDataContainer.remove(keyCraftState)

        val lore = meta.lore() ?: ArrayList()
        // Limpieza de líneas temporales (detectadas por contenido o posición)
        // Eliminamos las ultimas 2 lineas que son el bonus oculto y el aviso
        if (lore.size >= 2) {
            lore.removeAt(lore.size - 1)
            lore.removeAt(lore.size - 1)
        }

        val bonus = pickRandomBonus()
        lore.add(mm.deserialize(bonus.display))
        meta.lore(lore)

        val type = item.type
        val equipmentSlot = type.equipmentSlot
        val defaultAttributes = type.getDefaultAttributeModifiers(equipmentSlot)

        defaultAttributes.forEach { attribute, modifier ->
            meta.addAttributeModifier(attribute, modifier)
        }

        val keyBonus = NamespacedKey(plugin, "fabricator_bonus")
        val typeName = item.type.name

        val slotGroup = if (typeName.contains("SWORD") || typeName.contains("AXE") ||
            typeName.contains("HOE") || typeName.contains("SHOVEL") ||
            typeName.contains("PICKAXE")) {
            EquipmentSlotGroup.HAND
        } else {
            EquipmentSlotGroup.ARMOR
        }

        when (bonus) {
            SmithingBonus.SHARP -> meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(keyBonus, 1.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.HARD -> meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, AttributeModifier(keyBonus, 1.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.DURABLE -> {
                val current = meta.getEnchantLevel(org.bukkit.enchantments.Enchantment.UNBREAKING)
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, current + 1, true)
            }
            SmithingBonus.VITALITY -> meta.addAttributeModifier(Attribute.MAX_HEALTH, AttributeModifier(keyBonus, 2.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.SWIFT -> meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, AttributeModifier(keyBonus, 0.05, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.IMPACT -> meta.addAttributeModifier(Attribute.ATTACK_KNOCKBACK, AttributeModifier(keyBonus, 0.5, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.BALANCED -> meta.addAttributeModifier(Attribute.ATTACK_SPEED, AttributeModifier(keyBonus, 0.10, AttributeModifier.Operation.ADD_SCALAR, slotGroup))
            SmithingBonus.LUCKY -> meta.addAttributeModifier(Attribute.LUCK, AttributeModifier(keyBonus, 1.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
            SmithingBonus.LEGENDARY -> {
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(keyBonus, 0.1, AttributeModifier.Operation.ADD_SCALAR, slotGroup))
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, AttributeModifier(keyBonus, 0.1, AttributeModifier.Operation.ADD_SCALAR, slotGroup))
                meta.addAttributeModifier(Attribute.MAX_HEALTH, AttributeModifier(keyBonus, 2.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup))
                meta.setEnchantmentGlintOverride(true)
            }
        }

        item.itemMeta = meta
    }

    private fun pickRandomBonus(): SmithingBonus {
        val totalWeight = SmithingBonus.entries.sumOf { it.weight }
        var randomValue = Random.nextInt(totalWeight)
        for (bonus in SmithingBonus.entries) {
            randomValue -= bonus.weight
            if (randomValue < 0) return bonus
        }
        return SmithingBonus.DURABLE
    }
}