<h1 align="center">
  <img src="https://cdn.discordapp.com/icons/1283550915214835764/9ef2b3db8593d7cc069b964a5e74d231.webp" alt="Noctis Logo" width="128">
  <br>NOCTIS
</h1>

<p align="center">
    <img src="https://img.shields.io/badge/kotlin-2.0.21-blue.svg?style=flat-square"/>
    <img src="https://img.shields.io/badge/platform-paper--1.21-gray.svg?style=flat-square"/>
    <img src="https://img.shields.io/badge/status-production-red.svg?style=flat-square"/>
</p>

Arquitectura modular para simulación ambiental, control económico estricto y PvE instanciado.

## Modulos

### 1. Módulo Ambiental (Radiation)
Lógica de presión constante sobre el jugador.
* **Acumulación:** Incremento por tick (ciclo diurno base / ciclo nocturno x3).
* **Saturación:** Al alcanzar `100.0` unidades, se aplica daño verdadero (*Health Damage*) ignorando armadura vanilla.
* **Mitigación:** Cálculo de reducción porcentual basado en Material de armadura equipado.
* **Zonas Seguras:** Validación de `TownBlock` mediante **TownyAPI**. La radiación se anula si el `Town` posee estado de protección activo en caché.

### 2. Módulo Económico (Fuel Economy)
Sistema de mantenimiento basado en items físicos (`AMETHYST_SHARD` con NBT data).
* **Ciclo Fiscal:** Tarea asíncrona (`TaxTask`) ejecutada a intervalos configurables.
* **Cobro:** Deducción directa del banco de la ciudad (`TownyEconomyHandler`).
* **Estado:**
    * **Solvente:** Se extiende el timestamp de protección en base de datos plana (`town_banks.yml`).
    * **Insolvente:** Se purga la UUID de la ciudad de la lista blanca de radiación.

### 3. Módulo Dungeons (Instancing)
Motor de generación procedural en mundo dedicado (`VoidGenerator`).
* **Grid System:** Asignación de coordenadas X incrementales (+5000) para aislamiento de instancias.
* **Renderizado:** Pegado asíncrono de esquemáticos (`.schem`) utilizando la API de **FAWE** y cálculo de vectores de offset.
* **Ciclo de Vida:** Gestión de estados (`Active` -> `Completed` -> `Cleanup`). Garbage Collector interno elimina entidades y referencias en RAM al finalizar.
* **Lógica:** Máquina de estados para control de oleadas (MythicMobs) y triggers de interacción (bloques/items).

### 4. Módulo Jobs (Attributes)
Modificadores pasivos de estadísticas.
* **Implementación:** Inyección de `AttributeModifiers` persistentes (UUIDs fijos) en el jugador.
* **Sincronización:** Hook con **Vault** para actualización dinámica al detectar cambios de grupo/permiso.

### 5. Módulo Party (Diplomacy)
Agrupación lógica de jugadores para instancias compartidas.
* **Validación:** Cross-check de relaciones diplomáticas vía **Towny**. Impide la creación de grupos entre residentes de Naciones enemigas o neutrales.

---

## Referencia de Comandos

### Jobs & User
| Comando | Permiso | Función Técnica |
| :--- | :--- | :--- |
| `/noctisjobs join <job>` | `noctis.jobs.join` | Asocia el permiso/grupo del trabajo y recalcula atributos. |
| `/noctisjobs refresh <target/all> ` | `noctis.admin.jobs` | Fuerza la re-aplicación de modificadores de atributos. |

### Economy
| Comando | Permiso | Función Técnica |
| :--- | :--- | :--- |
| `/noctiseco give <target> <n>` | `noctis.admin.economy` | Genera ItemStack de moneda con NBT proprietario. |
| `/bank balance` | - | Consulta saldo persistente de la ciudad del jugador. |
| `/bank deposit <n>` | - | Transfiere item de inventario a saldo de ciudad y trigger de reactivación. |
| `/payfuel <target> <n>` | - | Transferencia P2P de items validados. |

### Dungeons & Party
| Comando | Permiso | Función Técnica |
| :--- | :--- | :--- |
| `/party <create/invite/kick>` | - | Gestión de objeto `Party` en memoria. |
| `/dungeon start <id>` | `noctis.dungeons.play` | Inicializa `DungeonInstance` y rutina de generación. |

---

## Configuración

Directorio raíz: `/plugins/Noctis/`

* **`config.yml`**: Parámetros globales (tasas de radiación, intervalos fiscales, mitigación).
* **`dungeons.yml`**: Definición de objetos `DungeonData` (capas, filtros de tags, rewards).
* **`schematics.yml`**: Mapeo de archivos físicos a objetos `SchematicData` (vectores, lógica de script).

---

## API & Placeholders

Namespace: `noctis_`

| Identificador | Tipo | Retorno |
| :--- | :--- | :--- |
| `value` | Double | Nivel de radiación actual del jugador. |
| `bar` | String | Representación visual procesada (MiniMessage/Legacy). |
| `town_fuel` | Integer | Saldo de combustible asociado al Town del jugador. |

---

## Requerimientos

**Requerimientos Hard (Runtime):**
1. **Towny Advanced:** Resolución de plots y diplomacia.
2. **FastAsyncWorldEdit (FAWE):** Manipulación de mundo I/O.
3. **MythicMobs:** Manejo de entidades custom en instancias.

**Requerimientos Soft:**
1. **Vault:** Abstracción de permisos/chat.
2. **PlaceholderAPI:** Exposición de variables.
