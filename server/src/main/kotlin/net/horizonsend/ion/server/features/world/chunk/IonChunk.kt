package net.horizonsend.ion.server.features.world.chunk

import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.multiblock.manager.ChunkMultiblockManager
import net.horizonsend.ion.server.features.transport.ChunkTransportManager
import net.horizonsend.ion.server.features.world.IonWorld.Companion.ion
import net.horizonsend.ion.server.features.world.data.DataFixers
import net.horizonsend.ion.server.listener.SLEventListener
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.CARDINAL_BLOCK_FACES
import net.horizonsend.ion.server.miscellaneous.utils.minecraft
import net.minecraft.world.level.chunk.LevelChunkSection
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType.INTEGER
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class IonChunk(
	val inner: Chunk,
	val region: ChunkRegion,
) {
	var dataVersion = inner.persistentDataContainer.getOrDefault(NamespacedKeys.DATA_VERSION, INTEGER, 0)
		set	(value) {
			world.persistentDataContainer.set(NamespacedKeys.DATA_VERSION, INTEGER, value)
			field = value
		}

	val locationKey = inner.chunkKey

	/** The origin X coordinate of this chunk (in real coordinates) **/
	val originX = inner.x.shl(4)
	/** The origin Z coordinate of this chunk (in real coordinates) **/
	val originZ = inner.z.shl(4)

	/** The X chunk coordinate **/
	val x = inner.x
	/** The Z chunk coordinate **/
	val z = inner.z

	val world get() = inner.world

	val sections: Iterable<IndexedValue<LevelChunkSection>> get() = inner.minecraft.sections.withIndex()

	// TODO
	//  - Ore upgrader
	//  - Explosion Reversal

	val multiblockManager = ChunkMultiblockManager(this, log)
	val transportNetwork: ChunkTransportManager = ChunkTransportManager(this)

	/**
	 * Logic upon loading the chunk
	 **/
	fun onLoad() {
		transportNetwork.setup()
	}

	/**
	 * Logic upon unloading the chunk
	 **/
	fun onUnload() {
		save()
		multiblockManager.onUnload()
		transportNetwork.onUnload()
	}

	/**
	 * Logic upon world save
	 **/
	fun save() {
		transportNetwork.save()
		multiblockManager.save()
	}

	/**
	 * Logic upon world tick
	 **/
	fun tick() {}

	/**
	 * Gets the neighboring chunk in this direction
	 **/
	fun getNeighborIfLoaded(blockFace: BlockFace): IonChunk? {
		require(CARDINAL_BLOCK_FACES.contains(blockFace))

		val newX = x + blockFace.modX
		val newZ = z + blockFace.modZ

		return get(world, newX, newZ)
	}

	companion object : SLEventListener() {
		private val loadingTasks = ConcurrentHashMap.newKeySet<Long>()

		@EventHandler
		fun onChunkLoad(event: ChunkLoadEvent) {
			val key = event.chunk.chunkKey

			if (loadingTasks.contains(key)) {
				IonServer.slF4JLogger.warn("Detected double chuck load for ${event.chunk.x} ${event.chunk.z} in ${event.chunk.world.name}")
				return
			}

			loadingTasks.add(key)

			ChunkRegion.loadChunk(event.chunk)
			loadingTasks.remove(key)
		}

		@EventHandler
		fun onChunkUnload(event: ChunkUnloadEvent) {
			unregisterChunk(event.chunk)
		}

		/**
		 * Handles the creation, registration and loading of the chunk in the IonWorld
		 *
		 * It is imperative that every exception generated be handled
		 **/
		fun registerChunk(chunk: Chunk, region: ChunkRegion): IonChunk {
			val ionWorld = chunk.world.ion

			val ionChunk = IonChunk(chunk, region)
			region.chunks[chunk.chunkKey] = ionChunk

			ionWorld.addChunk(ionChunk)

			ionChunk.onLoad()

			// Upgrade data after it has been loaded
			DataFixers.handleChunkLoad(ionChunk)

			return ionChunk
		}

		/**
		 * Handles the unloading of the chunk
		 **/
		private fun unregisterChunk(chunk: Chunk) {
			val ionWorld = chunk.world.ion

			val removed = ionWorld.removeChunk(chunk) ?: return

			removed.onUnload()
			ChunkRegion.unloadChunk(removed)
		}

		/**
		 * Returns the chunk at the specified chunk coordinates in the world if it is loaded
		 **/
		operator fun get(world: World, x: Int, z: Int): IonChunk? {
			return world.ion.getChunk(x, z)
		}

		fun Chunk.ion(): IonChunk = this.world.ion.getChunk(chunkKey)!!

		fun getXFromKey(key: Long): Int = key.toInt()
		fun getZFromKey(key: Long): Int = (key shr 32).toInt()
	}

	fun iterateBlocks(consumer: Consumer<Block>) {
		for (x in 0..15) for (y in inner.world.minHeight until inner.world.maxHeight) for (z in 0..15) {
			consumer.accept(inner.getBlock(x, y ,z))
		}
	}

	/** Mark this chunk as needing to be saved */
	fun markUnsaved() { inner.minecraft.isUnsaved = true }

	override fun toString(): String {
		return "IonChunk[$x, $z @ ${world.name}]"
	}
}