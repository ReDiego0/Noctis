package org.ReDiego0.noctis.party

import com.palmergames.bukkit.towny.TownyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.ReDiego0.noctis.config.NoctisConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PartyManager(private val config: NoctisConfig) {

    private val playerPartyMap = ConcurrentHashMap<UUID, UUID>()
    private val parties = ConcurrentHashMap<UUID, Party>()
    private val invites = ConcurrentHashMap<UUID, UUID>()

    fun getParty(partyId: UUID): Party? = parties[partyId]

    fun getPlayerParty(playerUUID: UUID): Party? {
        val id = playerPartyMap[playerUUID] ?: return null
        return parties[id]
    }

    fun createParty(leader: Player): Party? {
        if (playerPartyMap.containsKey(leader.uniqueId)) return null

        val party = Party(leader.uniqueId)
        parties[party.id] = party
        playerPartyMap[leader.uniqueId] = party.id
        return party
    }

    fun disbandParty(partyId: UUID) {
        val party = parties.remove(partyId) ?: return
        party.getMembers().forEach { playerPartyMap.remove(it.uuid) }

        val mm = MiniMessage.miniMessage()
        party.broadcast(mm.deserialize("<red><bold>Party disuelta por el líder."))
    }

    fun invitePlayer(leader: Player, target: Player): String {
        val party = getPlayerParty(leader.uniqueId) ?: return "no_party"
        if (party.getLeader()?.uuid != leader.uniqueId) return "not_leader"
        if (getPlayerParty(target.uniqueId) != null) return "target_has_party"

        if (party.getSize() >= config.partyMaxSize) return "party_full"

        if (!canGroupTogether(leader, target)) {
            return "geo_conflict"
        }

        invites[target.uniqueId] = party.id
        return "success"
    }

    fun acceptInvite(player: Player): Boolean {
        val partyId = invites.remove(player.uniqueId) ?: return false
        val party = parties[partyId] ?: return false

        if (party.addMember(player)) {
            playerPartyMap[player.uniqueId] = party.id
            party.broadcast(MiniMessage.miniMessage().deserialize("<green>${player.name} se ha unido al grupo."))
            return true
        }
        return false
    }

    fun leaveParty(player: Player) {
        val party = getPlayerParty(player.uniqueId) ?: return

        if (party.getLeader()?.uuid == player.uniqueId) {
            disbandParty(party.id)
        } else {
            party.removeMember(player.uniqueId)
            playerPartyMap.remove(player.uniqueId)
            party.broadcast(MiniMessage.miniMessage().deserialize("<yellow>${player.name} abandonó el grupo."))
        }
    }

    fun kickPlayer(leader: Player, targetName: String): Boolean {
        val party = getPlayerParty(leader.uniqueId) ?: return false
        if (party.getLeader()?.uuid != leader.uniqueId) return false

        val targetMember = party.getMembers().find { it.getPlayer()?.name.equals(targetName, ignoreCase = true) } ?: return false
        if (targetMember.uuid == leader.uniqueId) return false

        party.removeMember(targetMember.uuid)
        playerPartyMap.remove(targetMember.uuid)

        targetMember.getPlayer()?.sendMessage(MiniMessage.miniMessage().deserialize("<red>Has sido expulsado del grupo."))
        party.broadcast(MiniMessage.miniMessage().deserialize("<yellow>${targetMember.getPlayer()?.name ?: "Unknown"} fue expulsado."))
        return true
    }

    private fun canGroupTogether(p1: Player, p2: Player): Boolean {
        val towny = TownyAPI.getInstance()
        val res1 = towny.getResident(p1)
        val res2 = towny.getResident(p2)

        if (res1 == null || res2 == null) return true
        if (!res1.hasTown() && !res2.hasTown()) return true
        if (!res1.hasTown() || !res2.hasTown()) return true

        val town1 = res1.townOrNull!!
        val town2 = res2.townOrNull!!

        if (town1 == town2) return true
        if (town1.hasNation() && town2.hasNation()) {
            if (town1.nation == town2.nation) return true
            if (town1.nation.hasAlly(town2.nation)) return true
        }

        return false
    }
}