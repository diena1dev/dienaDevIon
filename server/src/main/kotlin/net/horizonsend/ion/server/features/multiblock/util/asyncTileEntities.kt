package net.horizonsend.ion.server.features.multiblock.util

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.horizonsend.ion.server.miscellaneous.utils.getBlockDataSafe
import net.horizonsend.ion.server.miscellaneous.utils.minecraft
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState as NMSBlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState as BukkitBlockState
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.block.CraftBlockState
import org.bukkit.craftbukkit.block.CraftBlockStates
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import java.lang.reflect.Method

fun getNMSTileEntity(block: Block, loadChunks: Boolean): BlockEntity? {
	val serverLevel: ServerLevel = block.world.minecraft
	val blockPos = (block as CraftBlock).position

	if (serverLevel.isOutsideBuildHeight(blockPos)) {
		return null
	}

	val chunkX = block.x.shr(4)
	val chunkZ = block.z.shr(4)

	if (serverLevel.isLoaded(blockPos)) {
		return serverLevel.getChunk(chunkX, chunkZ).getBlockEntity(blockPos, LevelChunk.EntityCreationType.IMMEDIATE)
	}

	if (!loadChunks) return null

	val chunk = block.world.getChunkAt(chunkX, chunkZ).minecraft
	return chunk.getBlockEntity(blockPos)
}

/**
 * Please don't use this unless it is necessary
 **/
fun getBukkitBlockState(block: Block, loadChunks: Boolean) : BukkitBlockState? {
	// If this is the main thread, we don't need to do laggy reflection
	if (Bukkit.isPrimaryThread()) {
		return block.state
	}

	val world = block.world
	val blockPos = (block as CraftBlock).position
	val data = getBlockDataSafe(world, blockPos.x, blockPos.y, blockPos.z, loadChunks) ?: return null

	val tileEntity = getNMSTileEntity(block, loadChunks)

	val blockState = createBlockState(world, blockPos, data, tileEntity)
	blockState.worldHandle = world.minecraft

	return blockState
}

val getFactoryForMaterial: Method = CraftBlockStates::class.java.getDeclaredMethod("getFactory", Material::class.java).apply {
	isAccessible = true
}

private val blockStateFactory: LoadingCache<Material, Pair<Any, Method>> = CacheBuilder.newBuilder()
	.weakValues()
	.build(CacheLoader.from { material ->
		val factory = getFactoryForMaterial.invoke(null, material)

		factory to factory::class.java.getDeclaredMethod(
			"createBlockState",
			World::class.javaObjectType,
			BlockPos::class.javaObjectType,
			NMSBlockState::class.javaObjectType,
			BlockEntity::class.javaObjectType
		)
	})

fun createBlockState(world: World, blockPos: BlockPos, data: BlockData, tileEntity: BlockEntity?): CraftBlockState {
	val material = CraftMagicNumbers.getMaterial((data as CraftBlockData).state.block)

	val (factory, blockStateFactory) = blockStateFactory[material]

	blockStateFactory.isAccessible = true

	return blockStateFactory.invoke(factory, world, blockPos, data.state, tileEntity) as CraftBlockState
}
