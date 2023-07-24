package net.starlegacy.feature.starship.subsystem.shield

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.horizonsend.ion.server.miscellaneous.Vec3i
import net.horizonsend.ion.server.miscellaneous.commands.debugRed
import net.horizonsend.ion.server.miscellaneous.minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.horizonsend.ion.server.IonServerComponent
import net.starlegacy.feature.starship.active.ActivePlayerStarship
import net.starlegacy.feature.starship.active.ActiveStarship
import net.starlegacy.feature.starship.active.ActiveStarships
import net.starlegacy.feature.starship.event.StarshipActivatedEvent
import net.starlegacy.feature.starship.event.StarshipDeactivatedEvent
import net.starlegacy.util.*
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.boss.BarColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.min
import kotlin.math.sqrt

object StarshipShields : IonServerComponent() {
	var LAST_EXPLOSION_ABSORBED = false

	val updatedStarships = ConcurrentHashMap.newKeySet<ActiveStarship>()
	private var explosionPowerOverride: Double? = null

	override fun onEnable() {
		Tasks.syncRepeat(5L, 5L) {
			updateShieldBars()
		}
	}

	private data class ShieldPos(val worldID: UUID, val pos: Vec3i)

	private val shields = mutableMapOf<ShieldPos, Int>()

	@EventHandler
	fun onActivate(event: StarshipActivatedEvent) {
		val starship = event.starship
		val worldID = starship.serverLevel.world.uid

		for (shield in starship.shields) {
			val shieldPos = ShieldPos(worldID, shield.pos)
			shield.power = shields.remove(shieldPos) ?: continue
		}
	}

	@EventHandler
	fun onDeactivate(event: StarshipDeactivatedEvent) {
		val starship = event.starship
		val worldID = starship.serverLevel.world.uid

		for (shield in starship.shields) {
			val shieldPos = ShieldPos(worldID, shield.pos)
			shields[shieldPos] = shield.power
		}
	}

	fun withExplosionPowerOverride(value: Double, block: () -> Unit) {
		try {
			explosionPowerOverride = value
			block()
		} finally {
			explosionPowerOverride = null
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	fun onBlockExplode(event: BlockExplodeEvent) {
		val block = event.block

		val blockList = event.blockList()
		val power = explosionPowerOverride ?: getExplosionPower(block, blockList)
		onShieldImpact(block.location.toCenterLocation(), blockList, power)
		if (LAST_EXPLOSION_ABSORBED) {
			event.isCancelled = true
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	fun onEntityExplode(event: EntityExplodeEvent) {
		val location = event.location
		val block = location.block
		val blockList = event.blockList()
		val power = explosionPowerOverride ?: getExplosionPower(block, blockList)
		onShieldImpact(location.toCenterLocation(), blockList, power)
		if (LAST_EXPLOSION_ABSORBED) {
			event.isCancelled = true
		}
	}

	private fun getExplosionPower(center: Block, blockList: List<Block>): Double {
		val x = center.x.d()
		val y = center.y.d()
		val z = center.z.d()
		val biggestDistance = blockList.maxOfOrNull { distanceSquared(it.x.d(), it.y.d(), it.z.d(), x, y, z) } ?: 0.0
		return sqrt(biggestDistance)
	}

	private fun updateShieldBars() {
		val iterator = updatedStarships.iterator()
		while (iterator.hasNext()) {
			val ship = iterator.next()
			if (ship is ActivePlayerStarship) {
				updateShieldBars(ship)
			}
		}
	}

	@Synchronized
	fun updateShieldBars(ship: ActivePlayerStarship) {
		for ((name, bossBar) in ship.shieldBars) {
			var amount = 0
			var isReinforced = false
			var total = 0.0
			val percents = ArrayList<Double>()

			for (subsystem in ship.shields) {
				if (subsystem.name != name) continue
				amount++
				isReinforced = subsystem.isReinforcementActive()
				val subsystemPercent = subsystem.powerRatio
				total += subsystemPercent
				percents.add(subsystemPercent)
			}

			val percent = total / amount.toDouble()

			bossBar.progress = min(percent, 1.0)
			val titleColor = percentColor(percent, isReinforced)

			val barColor = when {
				isReinforced -> BarColor.PURPLE
				percent <= 0.05 -> BarColor.RED
				percent <= 0.25 -> BarColor.YELLOW
				percent <= 0.55 -> BarColor.GREEN
				else -> BarColor.BLUE
			}

			val extraPercents: String = if (percents.size > 1) {
				" (${
				percents.joinToString(separator = " + ") {
					"${percentColor(it, isReinforced)}${formatPercent(it)}%$titleColor"
				}
				})"
			} else {
				""
			}
			val title = "${SLTextStyle.GRAY}$name$titleColor ${formatPercent(total)}%$extraPercents"
			if (bossBar.title != title) {
				bossBar.setTitle(title)
			}
			if (bossBar.color != barColor) {
				bossBar.color = barColor
			}
		}
	}

	private fun percentColor(percent: Double, reinforced: Boolean): SLTextStyle = when {
		reinforced -> SLTextStyle.LIGHT_PURPLE
		percent <= 0.05 -> SLTextStyle.RED
		percent <= 0.10 -> SLTextStyle.GOLD
		percent <= 0.25 -> SLTextStyle.YELLOW
		percent <= 0.40 -> SLTextStyle.GREEN
		percent <= 0.55 -> SLTextStyle.DARK_GREEN
		percent <= 0.70 -> SLTextStyle.AQUA
		percent <= 0.85 -> SLTextStyle.DARK_AQUA
		else -> SLTextStyle.BLUE
	}

	private fun formatPercent(percent: Double): Double = (percent * 1000).toInt().toDouble() / 10.0

	private val flaringBlocks = PerWorld { LongOpenHashSet() }
	private val flaringChunks = PerWorld { LongOpenHashSet() }

	private fun onShieldImpact(location: Location, blockList: MutableList<Block>, power: Double) {
		LAST_EXPLOSION_ABSORBED = false

		val world: World = location.world
		val nmsWorld = world.minecraft
		val chunkKey: Long = location.chunk.chunkKey
		val size: Int = blockList.size

		if (blockList.isEmpty()) {
			return
		}

		val flaringBlocks: LongOpenHashSet = flaringBlocks[world]
		val flaringChunks: LongOpenHashSet = flaringChunks[world]

		val canFlare = !flaringChunks.contains(chunkKey)

		val flaredBlocks = LongOpenHashSet()

		val protectedBlocks = HashSet<Block>()
		for (starship in ActiveStarships.getInWorld(world)) {
			processShip(
				starship,
				world,
				location,
				blockList,
				size,
				power,
				protectedBlocks,
				canFlare,
				flaringBlocks,
				flaredBlocks,
				nmsWorld
			)
		}

		scheduleUnflare(canFlare, flaredBlocks, flaringChunks, chunkKey, flaringBlocks, world, nmsWorld)

		blockList.removeAll(protectedBlocks)

		if (blockList.isEmpty()) {
			LAST_EXPLOSION_ABSORBED = true
			location.world.playSound(location, Sound.ENTITY_IRON_GOLEM_HURT, 8.0f, 0.5f)
		}
	}

	private fun processShip(
		starship: ActiveStarship,
		world: World,
		location: Location,
		blockList: MutableList<Block>,
		size: Int,
		radius: Double,
		protectedBlocks: HashSet<Block>,
		canFlare: Boolean,
		flaringBlocks: LongOpenHashSet,
		flaredBlocks: LongOpenHashSet,
		nmsLevel: Level
	) {
		// ignore if it's over 500 blocks away
		if (starship.centerOfMass.toLocation(world).distanceSquared(location) > 250_000) {
			return
		}

		val blocks = blockList.filter { b -> starship.contains(b.x, b.y, b.z) }

		if (blocks.isEmpty()) {
			return
		}

		val damagedPercent = blocks.size.toFloat() / size.toFloat()

		shieldLoop@
		for (shield: ShieldSubsystem in starship.shields) {
			processShield(
				shield,
				radius,
				protectedBlocks,
				blocks,
				damagedPercent,
				canFlare,
				flaringBlocks,
				flaredBlocks,
				nmsLevel,
				starship
			)
		}
	}

	private fun processShield(
		shield: ShieldSubsystem,
		power: Double,
		protectedBlocks: HashSet<Block>,
		blocks: List<Block>,
		damagedPercent: Float,
		canFlare: Boolean,
		flaringBlocks: LongOpenHashSet,
		flaredBlocks: LongOpenHashSet,
		nmsLevel: Level,
		starship: ActiveStarship
	): Boolean {
		val containedBlocks = blocks.filter { shield.containsBlock(it) }

		if (containedBlocks.isEmpty()) {
			return false
		}

		val percent = shield.powerRatio
		if (percent < 0.01) {
			return false
		}

		if (!shield.isIntact()) {
			return false
		}

		protectedBlocks.addAll(containedBlocks)

		var usage: Int = (shield.getPowerUsage(power) * damagedPercent).toInt()

		if (shield.isReinforcementActive()) {
			usage = (usage * 0.1f).toInt()
		}

		if (canFlare && protectedBlocks.isNotEmpty() && percent > 0.01f) {
			addFlare(containedBlocks, shield, flaringBlocks, flaredBlocks, nmsLevel)
		}

		starship.controller?.playerPilot?.debugRed("shield damage = ${shield.power} - $usage = ${shield.power - usage}")
		shield.power = shield.power - usage

		if (usage > 0) {
			updatedStarships.add(starship)
		}

		return true
	}

	private fun addFlare(
		containedBlocks: List<Block>,
		shield: ShieldSubsystem,
		flaringBlocks: LongOpenHashSet,
		flaredBlocks: LongOpenHashSet,
		nmsLevel: Level
	) {
		val percent = shield.powerRatio

		val flare: BlockState = when {
			shield.isReinforcementActive() -> Material.MAGENTA_STAINED_GLASS
			percent <= 0.05 -> Material.RED_STAINED_GLASS
			percent <= 0.10 -> Material.ORANGE_STAINED_GLASS
			percent <= 0.25 -> Material.YELLOW_STAINED_GLASS
			percent <= 0.40 -> Material.LIME_STAINED_GLASS
			percent <= 0.55 -> Material.GREEN_STAINED_GLASS
			percent <= 0.70 -> Material.CYAN_STAINED_GLASS
			percent <= 0.85 -> Material.LIGHT_BLUE_STAINED_GLASS
			else -> Material.BLUE_STAINED_GLASS
		}.createBlockData().nms

		for (block in containedBlocks) {
			val bx = block.x
			val by = block.y
			val bz = block.z

			val blockKey: Long = block.blockKey

			if (!flaringBlocks.add(blockKey) || !flaredBlocks.add(blockKey)) {
				continue
			}

			val pos = BlockPos(bx, by, bz)
			val packet = ClientboundBlockUpdatePacket(pos, flare)
			nmsLevel.getChunkAt(pos).playerChunk?.broadcast(packet, false)
		}
	}

	private fun scheduleUnflare(
		canFlare: Boolean,
		flaredBlocks: LongOpenHashSet,
		flaringChunks: LongOpenHashSet,
		chunkKey: Long,
		flaringBlocks: LongOpenHashSet,
		world: World,
		nmsLevel: Level
	) {
		if (!canFlare || flaredBlocks.isEmpty()) {
			return
		}

		flaringChunks.add(chunkKey)
		flaringBlocks.addAll(flaredBlocks)

		Tasks.syncDelay(3) {
			flaringChunks.remove(chunkKey)

			for (key: Long in flaredBlocks.iterator()) {
				flaringBlocks.remove(key)

				val data = world.getBlockAtKey(key).blockData.nms

				if (data.block is BaseEntityBlock) {
					world.getBlockAtKey(key).state.update(false, false)
					continue
				}

				val pos = BlockPos(blockKeyX(key), blockKeyY(key), blockKeyZ(key))
				val packet = ClientboundBlockUpdatePacket(pos, data)
				nmsLevel.getChunkAt(pos).playerChunk?.broadcast(packet, false)
			}
		}
	}

	fun cartesianProduct(a: Set<*>, b: Set<*>, vararg sets: Set<*>): Set<List<*>> =
		(setOf(a, b).plus(sets))
			.fold(listOf(listOf<Any?>())) { acc, set ->
				acc.flatMap { list -> set.map { element -> list + element } }
			}
			.toSet()
}
