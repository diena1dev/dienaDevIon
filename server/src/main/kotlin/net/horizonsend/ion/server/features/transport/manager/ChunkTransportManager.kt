package net.horizonsend.ion.server.features.transport.manager

import net.horizonsend.ion.server.features.transport.NewTransport
import net.horizonsend.ion.server.features.transport.manager.extractors.ChunkExtractorManager
import net.horizonsend.ion.server.features.transport.manager.extractors.ExtractorManager
import net.horizonsend.ion.server.features.transport.manager.holders.ChunkCacheHolder
import net.horizonsend.ion.server.features.transport.nodes.cache.FluidTransportCache
import net.horizonsend.ion.server.features.transport.nodes.cache.PowerTransportCache
import net.horizonsend.ion.server.features.transport.nodes.inputs.InputManager
import net.horizonsend.ion.server.features.world.chunk.IonChunk
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.toBlockKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData

class ChunkTransportManager(val chunk: IonChunk) : TransportManager() {
	override val extractorManager: ExtractorManager = ChunkExtractorManager(this)
	override val powerNodeManager = ChunkCacheHolder(this) { PowerTransportCache(it) }
	override val fluidNodeManager = ChunkCacheHolder(this) { FluidTransportCache(it) }
//	val pipeGrid = PowerNodeManager(this) // TODO

	override fun getInputProvider(): InputManager {
		return chunk.region.world.inputManager
	}

	fun setup() {
		powerNodeManager.handleLoad()
		fluidNodeManager.handleLoad()
		NewTransport.registerTransportManager(this)
	}

	fun onUnload() {
		powerNodeManager.handleUnload()
		fluidNodeManager.handleUnload()
		NewTransport.removeTransportManager(this)
	}

	fun invalidateCache(x: Int, y: Int, z: Int) {
		invalidateCache(toBlockKey(x, y, z))
	}

	fun invalidateCache(key: BlockKey) {
		powerNodeManager.cache.invalidate(key)
		fluidNodeManager.cache.invalidate(key)
	}

	fun processBlockRemoval(key: BlockKey) {
		powerNodeManager.cache.invalidate(key)
		fluidNodeManager.cache.invalidate(key)
//		pipeGrid.processBlockRemoval(key)
	}

	fun processBlockChange(block: Block) {
		powerNodeManager.cache.invalidate(toBlockKey(block.x, block.y, block.z))
		fluidNodeManager.cache.invalidate(toBlockKey(block.x, block.y, block.z))
//		pipeGrid.processBlockAddition(key, new)
	}

	fun processBlockChange(position: BlockKey, data: BlockData) {
		powerNodeManager.cache.invalidate(position)
		fluidNodeManager.cache.invalidate(position)
//		pipeGrid.processBlockAddition(key, new)
	}

	fun refreshBlock(position: BlockKey) {
		powerNodeManager.cache.invalidate(position)
		fluidNodeManager.cache.invalidate(position)
//		pipeGrid.processBlockAddition(key, new)
	}
}