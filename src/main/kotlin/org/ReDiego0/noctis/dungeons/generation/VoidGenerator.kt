package org.ReDiego0.noctis.dungeons.generation

import org.bukkit.World
import org.bukkit.generator.ChunkGenerator
import java.util.Random

class VoidGenerator : ChunkGenerator() {
    override fun generateChunkData(
        world: World,
        random: Random,
        x: Int,
        z: Int,
        biome: BiomeGrid
    ): ChunkData {
        return createChunkData(world)
    }
}