package org.ReDiego0.noctis.dungeons.generation

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import org.ReDiego0.noctis.Noctis
import java.io.File
import java.io.FileInputStream

class SchematicPaster(private val plugin: Noctis) {

    private val schematicFolder = File(plugin.dataFolder, "schematics")

    init {
        if (!schematicFolder.exists()) schematicFolder.mkdirs()
    }

    fun paste(filename: String, location: Location): Boolean {
        val file = File(schematicFolder, filename)
        if (!file.exists()) {
            plugin.logger.severe("No se encuentra el schematic: ${file.path}")
            return false
        }

        val format = ClipboardFormats.findByFile(file)
        if (format == null) {
            plugin.logger.severe("Formato de schematic desconocido: $filename")
            return false
        }

        return try {
            FileInputStream(file).use { fis ->
                val reader = format.getReader(fis)
                val clipboard = reader.read()
                val weWorld = BukkitAdapter.adapt(location.world)

                WorldEdit.getInstance().newEditSession(weWorld).use { editSession ->

                    val vector = BlockVector3.at(location.x, location.y, location.z)
                    val operation = ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(vector)
                        .ignoreAirBlocks(true)
                        .build()

                    Operations.complete(operation)
                }
                true
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error pegando schematic $filename: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}