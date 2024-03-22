package net.horizonsend.ion.server.miscellaneous.utils

import com.mojang.math.Transformation
import dev.cubxity.plugins.metrics.api.UnifiedMetricsProvider
import io.papermc.paper.adventure.PaperAdventure
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.command.admin.IonCommand
import net.horizonsend.ion.server.command.admin.debug
import net.horizonsend.ion.server.features.machine.AreaShields
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.milkbowl.vault.economy.Economy
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StructureBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.StructureBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.CraftWorldBorder
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.scheduler.BukkitRunnable
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.reflect.jvm.isAccessible

val vaultEconomy = try {
	Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)?.provider
} catch (exception: NoClassDefFoundError) {
	null
}

val metrics = if (Bukkit.getPluginManager().isPluginEnabled("UnifiedMetrics")) UnifiedMetricsProvider.get() else null

val gayColors = arrayOf(
	Color.fromRGB(255, 0, 24),
	Color.fromRGB(255, 165, 44),
	Color.fromRGB(255, 255, 65),
	Color.fromRGB(0, 128, 24),
	Color.fromRGB(0, 0, 249),
	Color.fromRGB(134, 0, 125)
)

/** Used for catching when a function that is not designed to be used async is being used async. */
fun mainThreadCheck() {
	if (!Bukkit.isPrimaryThread()) {
		IonServer.slF4JLogger.warn(
			"This function may be unsafe to use asynchronously.",
			Throwable()
		)
	}
}

fun Player.worldBorderEffect(duration: Long) {
	val start = ClientboundSetBorderWarningDistancePacket(WorldBorder().apply { this.warningBlocks = Int.MAX_VALUE })
	val end = ClientboundSetBorderWarningDistancePacket((this.world.worldBorder as CraftWorldBorder).handle)

	this.minecraft.connection.send(start)

	Tasks.syncDelayTask(duration) {
		this.minecraft.connection.send(end)
	}
}

val Chunk.minecraft: LevelChunk get() = (this as CraftChunk).getHandle(ChunkStatus.FULL) as LevelChunk // ChunkStatus.FULL guarantees a LevelChunk
val Player.minecraft: ServerPlayer get() = (this as CraftPlayer).handle
val World.minecraft: ServerLevel get() = (this as CraftWorld).handle

fun runnable(e: BukkitRunnable.() -> Unit): BukkitRunnable = object : BukkitRunnable() {
	override fun run() = e()
}

@Suppress("UNCHECKED_CAST")
fun <T : Entity> World.castSpawnEntity(location: Location, type: org.bukkit.entity.EntityType) =
	this.spawnEntity(location, type) as T

fun debugHighlightBlock(x: Number, y: Number, z: Number, duration: Long = 5L) = debugAudience.highlightBlock(Vec3i(x.toInt(), y.toInt(), z.toInt()), duration)
fun debugHighlightBlock(pos: Vec3i, duration: Long = 5L) = debugAudience.highlightBlock(pos, duration)
fun debugHighlightBlocks(blocks: Collection<Vec3i>, duration: Long = 5L) = debugAudience.highlightBlocks(blocks, duration)

val debugAudience: ForwardingAudience = ForwardingAudience { IonCommand.debugEnabledPlayers }

fun areaDebugMessage(x: Number, y: Number, z: Number, msg: String) {
	IonCommand.debugEnabledPlayers.mapNotNull { it as? Player }.forEach {
		if (it.location.distanceSquared(
				Location(
					it.world,
					x.toDouble(),
					y.toDouble(),
					z.toDouble()
				)
			) > 30 * 30
		) return@forEach

		it.debug(msg)
	}
}

fun sendEntityPacket(bukkitPlayer: Player, entity: net.minecraft.world.entity.Entity, duration: Long) {
	val player = bukkitPlayer.minecraft
	val conn = player.connection

	conn.send(ClientboundAddEntityPacket(entity))
	entity.entityData.refresh(player)

	Tasks.syncDelayTask(duration) { conn.send(ClientboundRemoveEntitiesPacket(entity.id)) }
}

fun highlightBlock(bukkitPlayer: Player, pos: Vec3i): net.minecraft.world.entity.Entity {
	val player = bukkitPlayer.minecraft
	return Slime(EntityType.SLIME, player.level()).apply {
		setPos(pos.x + 0.5, pos.y + 0.25, pos.z + 0.5)
		this.setSize(1, true)
		setGlowingTag(true)
		isInvisible = true

	}
}

fun displayBlock(bukkitPlayer: Player, blockData: BlockData, pos: Vec3i, scale: Float, glow: Boolean): net.minecraft.world.entity.Entity {
	val player = bukkitPlayer.minecraft
	val offset = (-scale / 2) + 0.5
	return Display.BlockDisplay(EntityType.BLOCK_DISPLAY, player.level()).apply {
		setPos(pos.x + offset, pos.y + offset, pos.z + offset)
		this.blockState = blockData.nms
		setGlowingTag(glow)
		this.setTransformation(Transformation(Vector3f(0f), Quaternionf(), Vector3f(scale), Quaternionf()))
	}
}

fun displayBillboardText(bukkitPlayer: Player, pos: Vec3i, scale: Float, text: Component, opacity: Byte): net.minecraft.world.entity.Entity {
	val player = bukkitPlayer.minecraft
	return Display.TextDisplay(EntityType.TEXT_DISPLAY, player.level()).apply {
		setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
		this.setTransformation(Transformation(Vector3f(0f), Quaternionf(), Vector3f(scale), Quaternionf()))
		this.text = PaperAdventure.asVanilla(text)
		this.textOpacity = opacity
		this.billboardConstraints = Display.BillboardConstraints.CENTER
		this.entityData.set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, 0x00000000)
	}
}

fun Audience.highlightBlock(pos: Vec3i, duration: Long) {
	when (this) {
		is Player -> sendEntityPacket(this, highlightBlock(this, pos), duration)
		is ForwardingAudience -> for (player in audiences().filterIsInstance<Player>()) { sendEntityPacket(player, highlightBlock(player, pos), duration) }
	}
}

fun Audience.highlightBlocks(positions: Collection<Vec3i>, duration: Long) {
	for (pos in positions) this.highlightBlock(pos, duration)
}

fun buildStructureBlock(minPoint: Vec3i, maxPoint: Vec3i, message: String = ""): Pair<BlockState, StructureBlockEntity> {
	val constructor = StructureBlock::class.constructors.first()
	constructor.isAccessible = true

	val block = Blocks.STRUCTURE_BLOCK as StructureBlock
	val state = block.defaultBlockState()

	val (x, y, z) = minPoint
	val blockPos = BlockPos(x, y - 1, z)

	val xDiff = maxPoint.x - minPoint.x
	val yDiff = maxPoint.y - minPoint.y
	val zDiff = maxPoint.z - minPoint.z

	val entity: StructureBlockEntity = block.newBlockEntity(blockPos, state) as StructureBlockEntity
	entity.showBoundingBox = true
	entity.structureName = message

	entity.setStructureSize(net.minecraft.core.Vec3i(xDiff, yDiff, zDiff))

	return state to entity
}

fun Player.highlightRegion(minPoint: Vec3i, maxPoint: Vec3i, structureName: String = "", duration: Long) {
	val (state, entity) = buildStructureBlock(minPoint, maxPoint, structureName)

	showBlockState(state, entity, duration)
}

val airState = Blocks.AIR.defaultBlockState()

fun Player.showBlockState(state: BlockState, blockEntity: BlockEntity, duration: Long) {
	val position = blockEntity.blockPos

	val conn: ServerGamePacketListenerImpl = this.minecraft.connection

	conn.send(ClientboundBlockUpdatePacket(position, state))
	conn.send(ClientboundBlockEntityDataPacket.create(blockEntity))

	Tasks.syncDelayTask(duration) {
		conn.send(ClientboundBlockUpdatePacket(position, airState))
	}
}

fun Player.showBlockState(position: BlockPos, state: BlockState, blockEntity: BlockEntity?) {
	val conn: ServerGamePacketListenerImpl = this.minecraft.connection

	conn.send(ClientboundBlockUpdatePacket(position, state))

	blockEntity?.let {
		conn.send(ClientboundBlockEntityDataPacket.create(blockEntity))
	}
}

/**
 * Abuse explosion regen to allow block changes to regenerate
 *
 * @param source: The source entity
 * @param origin: The origin block
 * @param changedBlocks: The list of blocks (before they were changed)
 * @param yield: The yield of the explosion, for the event call
 * @param bypassAreaShields: Whether this block change will bypass area shields
 *
 * @return whether the explosion was cancelled
 **/
fun regeneratingBlockChange(source: Entity?, origin: Block, changedBlocks: MutableList<Block>, yield: Float, bypassAreaShields: Boolean): BlockExplodeEvent {
	val world = origin.world
	val location = origin.location.toCenterLocation()
	val blockExplodeEvent = BlockExplodeEvent(origin, changedBlocks, yield)

	if (bypassAreaShields) AreaShields.bypassShieldEvents.add(blockExplodeEvent)

	if (source != null) origin.world.createExplosion(source, 1f, false, false) else
		world.createExplosion(location, 1f, false, false)

	world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 10f, 0.5f)

	return blockExplodeEvent
}
