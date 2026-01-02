package org.ReDiego0.noctis.radiation

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RadiationManager {

    // Cache en RAM: UUID -> Nivel de Radiación (0.0 a 100.0)
    private val radiationCache = ConcurrentHashMap<UUID, Double>()
    private val MAX_RADIATION = 100.0
    private val MIN_RADIATION = 0.0

    /**
     * Inicializa al jugador en el cache.
     * Usado en PlayerJoinEvent.
     */
    fun loadPlayer(uuid: UUID, value: Double = 0.0) {
        radiationCache[uuid] = value
    }

    /**
     * Elimina al jugador del cache.
     * Usado en PlayerQuitEvent.
     */
    fun unloadPlayer(uuid: UUID) {
        radiationCache.remove(uuid)
    }

    /**
     * Obtiene la radiación actual. Retorna 0.0 si no existe (fail-safe).
     */
    fun getRadiation(uuid: UUID): Double {
        return radiationCache.getOrDefault(uuid, 0.0)
    }

    /**
     * Modifica la radiación de un jugador.
     * @param amount Cantidad a sumar (positivo) o restar (negativo).
     * @return El nuevo valor de radiación.
     */
    fun modifyRadiation(uuid: UUID, amount: Double): Double {
        val current = getRadiation(uuid)
        val newValue = (current + amount).coerceIn(MIN_RADIATION, MAX_RADIATION)

        radiationCache[uuid] = newValue
        return newValue
    }

    /**
     * Setter directo para casos específicos (ej. comandos de admin).
     */
    fun setRadiation(uuid: UUID, value: Double) {
        radiationCache[uuid] = value.coerceIn(MIN_RADIATION, MAX_RADIATION)
    }

    /**
     * Método para obtener todos los datos.
     */
    fun getCacheSnapshot(): Map<UUID, Double> {
        return HashMap(radiationCache)
    }
}