<h1 align="center">
  <br>
  <img src="[https://via.placeholder.com/256x64.png?text=NOCTIS+CORE](https://cdn.discordapp.com/icons/1283550915214835764/9ef2b3db8593d7cc069b964a5e74d231.webp)" alt="Noctis Logo" width="256">
  <br>
</h1>

<h4 align="center">☢️ Núcleo de gestión ambiental y sistema de jobs para PaperMC 1.21+.</h4>

<p align="center">
    <a href="#">
        <img src="https://img.shields.io/badge/kotlin-2.3.0-blue.svg?style=flat-square"/>
    </a>
    <a href="#">
        <img src="https://img.shields.io/badge/platform-paper-gray.svg?style=flat-square"/>
    </a>
    <a href="#">
        <img src="https://img.shields.io/badge/status-active-brightgreen.svg?style=flat-square"/>
    </a>
</p>

## 1. Sistema de Radiación

Mecánica de supervivencia ambiental obligatoria.

* **Ciclo Diario:** La radiación se acumula lentamente de día y se triplica durante la noche (Ticks 13000-23000).
* **Acumulación:** El contador sube de 0 a 100. Al llegar a 100, el jugador recibe daño verdadero constante hasta morir.
* **Mitigación:** Las armaduras reducen la entrada de radiación según su material (Configurable).
* **Limpieza:** Entrar en una ciudad (**Towny**) reduce el contador rápidamente.

## 2. Sistema de Trabajos

Roles inmutables asignados por permiso (`noctis.job.<id>`).

| ID             | Rol | Bonificaciones |
|:---------------| :--- | :--- |
| **architect**  | Construcción | +2 Alcance, Salto mejorado, duplicación de bloques (5%), sin daño de caída. |
| **prospector** | Minería | *Haste* y *Conduit Power* infinitos. Doble drop en menas (RNG). |
| **biochemist** | Agricultura | Doble drop y XP en cultivos maduros. |
| **fabricator** | Herrería | Crea armaduras/herramientas de Diamante/Netherite con atributos ocultos aleatorios (Vida, Daño, Velocidad, etc.). |

## Configuración (`config.yml`)

```yaml
settings:
# ==========================================
#         NOCTIS - RADIACIÓN
# ==========================================

settings:
  world-name: "world"
  # Radiación base por segundo (Día)
  base-radiation: 1.5
  # Multiplicador durante la noche (ej. 1.5 * 3.0 = 4.5 rads/s)
  night-multiplier: 3.0
  # Cantidad a reducir por segundo al estar en una ciudad (Towny)
  towny-cleanup-rate: 20.0
  # Límite máximo de reducción de daño por armadura (0.90 = 90%)
  max-mitigation-cap: 0.90
  # Daño (corazones) por segundo al llegar al 100%
  critical-damage: 2.0

# Porcentajes de protección por pieza (0.01 = 1%, 1.0 = 100%)
# El sistema busca el material exacto.
armor-protection:
  LEATHER_HELMET: 0.02
  LEATHER_CHESTPLATE: 0.02
  LEATHER_LEGGINGS: 0.02
  LEATHER_BOOTS: 0.02
  IRON_HELMET: 0.05
  IRON_CHESTPLATE: 0.05
  IRON_LEGGINGS: 0.05
  IRON_BOOTS: 0.05
  DIAMOND_HELMET: 0.08
  DIAMOND_CHESTPLATE: 0.08
  DIAMOND_LEGGINGS: 0.08
  DIAMOND_BOOTS: 0.08
  NETHERITE_HELMET: 0.12
  NETHERITE_CHESTPLATE: 0.12
  NETHERITE_LEGGINGS: 0.12
  NETHERITE_BOOTS: 0.12

visuals:
  bar-symbol: "|"
  bar-length: 20
  # Colores en formato Hex (MiniMessage)
  # Low: Seguro (< 50%)
  color-low-start: "#55FF55"
  color-low-end: "#00AA00"
  # Mid: Advertencia (50% - 80%)
  color-mid-start: "#FFFF55"
  color-mid-end: "#FFAA00"
  # High: Crítico (> 80%)
  color-high-start: "#FF5555"
  color-high-end: "#AA0000"
```
## Placeholders

Noctis expone datos mediante PlaceholderAPI.

| Placeholder | Descripción |
| :--- | :--- |
| `%noctis_value%` | Nivel actual (Ej: `45.2`) |
| `%noctis_bar%` | Barra visual con gradientes (Auto-conversión a Legacy para compatibilidad). |

## Comandos

* `/jobs refresh <player|all>`
    * **Permiso:** `noctis.admin.jobs`
    * **Función:** Recarga los atributos del trabajo si cambian los permisos.

## Dependencias

1.  **Towny Advanced** (Obligatorio)
2.  **PlaceholderAPI** (Opcional - Muy Recomendado)
