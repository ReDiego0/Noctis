package org.ReDiego0.noctis.party

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class PartyRole { LEADER, MEMBER }

data class PartyMember(val uuid: UUID, var role: PartyRole) {
    fun getPlayer(): Player? = Bukkit.getPlayer(uuid)
    fun isOnline(): Boolean = getPlayer() != null
}

class Party(val leaderUUID: UUID) {

    val id: UUID = UUID.randomUUID()
    private val members = ConcurrentHashMap<UUID, PartyMember>()
    private val maxSize = 5

    init {
        members[leaderUUID] = PartyMember(leaderUUID, PartyRole.LEADER)
    }

    fun getMembers(): List<PartyMember> = members.values.toList()

    fun getLeader(): PartyMember? = members.values.find { it.role == PartyRole.LEADER }

    fun addMember(player: Player): Boolean {
        if (members.size >= maxSize) return false
        if (members.containsKey(player.uniqueId)) return false

        members[player.uniqueId] = PartyMember(player.uniqueId, PartyRole.MEMBER)
        return true
    }

    fun removeMember(uuid: UUID) {
        members.remove(uuid)
    }

    fun contains(uuid: UUID): Boolean = members.containsKey(uuid)

    fun broadcast(message: Component) {
        members.values.forEach { it.getPlayer()?.sendMessage(message) }
    }

    fun isEmpty(): Boolean = members.isEmpty()

    fun getSize(): Int = members.size
}