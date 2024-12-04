package net.horizonsend.ion.server.features.starship

import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.configuration.StarshipBalancing
import net.horizonsend.ion.server.features.progression.Levels
import net.horizonsend.ion.server.features.sidebar.SidebarIcon
import net.horizonsend.ion.server.features.starship.destruction.SinkProvider
import net.horizonsend.ion.server.miscellaneous.utils.setDisplayNameAndGet
import net.horizonsend.ion.server.miscellaneous.utils.setLoreAndGet
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Supplier

enum class StarshipType(
	val displayName: String,
	val icon: String = SidebarIcon.GENERIC_STARSHIP_ICON.text,
	val color: String,
	val dynmapIcon: String = "anchor",

	val minSize: Int,
	val maxSize: Int,

	val minLevel: Int,
	val overridePermission: String,

	val containerPercent: Double,
	val concretePercent: Double = 0.3,
	val crateLimitMultiplier: Double,

	menuItemMaterial: Material,
	val isWarship: Boolean,
	val eventship: Boolean = false,
	val poweroverrider: Double = 1.0,

	val maxMiningLasers: Int = 0,
	val miningLaserTier: Int = 0,

	val sinkProvider: SinkProvider.SinkProviders = SinkProvider.SinkProviders.STANDARD,

	val balancingSupplier: Supplier<StarshipBalancing>
) {
	SPEEDER(
		displayName = "Speeder",
		minSize = 25,
		maxSize = 100,
		minLevel = 1,
		containerPercent = 0.25,
		concretePercent = 0.0,
		crateLimitMultiplier = 0.125,
		menuItemMaterial = Material.DEAD_BUSH,
		isWarship = false,
		color = "#ffff32",
		overridePermission = "ion.ships.override.1",
		poweroverrider = 0.0,
		balancingSupplier = IonServer.starshipBalancing::speeder
	),
	AI_SPEEDER(
		displayName = "Speeder",
		minSize = 25,
		maxSize = 100,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.125,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#ffff32",
		poweroverrider = 0.0,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.speeder",
		balancingSupplier = IonServer.starshipBalancing::speeder
	),
	STARFIGHTER(
		displayName = "Starfighter",
		icon = SidebarIcon.STARFIGHTER_ICON.text,
		minSize = 350,
		maxSize = 500,
		minLevel = 1,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.IRON_NUGGET,
		isWarship = true,
		color = "#ff8000",
		overridePermission = "ion.ships.override.1",
		dynmapIcon = "starfighter",
		balancingSupplier = IonServer.starshipBalancing::starfighter
	),
	AI_STARFIGHTER(
		displayName = "Starfighter",
		icon = SidebarIcon.AI_STARFIGHTER_ICON.text,
		minSize = 150,
		maxSize = 500,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#ff8000",
		dynmapIcon = "starfighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.starfighter",
		balancingSupplier = IonServer.starshipBalancing::aiStarfighter
	),
	INTERCEPTOR(
		displayName = "Interceptor",
		icon = SidebarIcon.STARFIGHTER_ICON.text,
		minSize = 150,
		maxSize = 350,
		minLevel = 1,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.IRON_NUGGET,
		isWarship = true,
		color = "#ff8000",
		overridePermission = "ion.ships.override.1",
		dynmapIcon = "starfighter",
		balancingSupplier = IonServer.starshipBalancing::interceptor
	),
	GUNSHIP(
		displayName = "Gunship",
		icon = SidebarIcon.GUNSHIP_ICON.text,
		minSize = 500,
		maxSize = 2000,
		minLevel = 10,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.IRON_INGOT,
		isWarship = true,
		color = "#ff4000",
		overridePermission = "ion.ships.override.10",
		dynmapIcon = "gunship",
		balancingSupplier = IonServer.starshipBalancing::gunship
	),
	AI_GUNSHIP(
		displayName = "Gunship",
		icon = SidebarIcon.AI_GUNSHIP_ICON.text,
		minSize = 500,
		maxSize = 2000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#ff4000",
		dynmapIcon = "gunship",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.gunship",
		balancingSupplier = IonServer.starshipBalancing::aiGunship
	),
	CORVETTE(
		displayName = "Corvette",
		icon = SidebarIcon.CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 20,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.IRON_BLOCK,
		isWarship = true,
		color = "#ff0000",
		overridePermission = "ion.ships.override.20",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = IonServer.starshipBalancing::corvette
	),
	AI_CORVETTE(
		displayName = "Corvette",
		icon = SidebarIcon.AI_CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#ff0000",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.corvette",
		balancingSupplier = IonServer.starshipBalancing::aiCorvette
	),
	AI_CORVETTE_LOGISTIC(
		displayName = "Logistic Corvette",
		icon = SidebarIcon.AI_CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#ff0000",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.corvette",
		balancingSupplier = IonServer.starshipBalancing::aiCorvetteLogistic
	),
	FRIGATE(
		displayName = "Frigate",
		icon = SidebarIcon.FRIGATE_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 40,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.LAPIS_BLOCK,
		isWarship = true,
		color = "#c00000",
		overridePermission = "ion.ships.override.40",
		dynmapIcon = "frigate",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = IonServer.starshipBalancing::frigate
	),
	AI_FRIGATE(
		displayName = "Frigate",
		icon = SidebarIcon.AI_FRIGATE_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#c00000",
		dynmapIcon = "frigate",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.frigate",
		balancingSupplier = IonServer.starshipBalancing::aiFrigate
	),
	DESTROYER(
		displayName = "Destroyer",
		icon = SidebarIcon.DESTROYER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 60,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.GOLD_BLOCK,
		isWarship = true,
		color = "#800000",
		overridePermission = "ion.ships.override.60",
		dynmapIcon = "destroyer",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = IonServer.starshipBalancing::destroyer
	),
	AI_DESTROYER(
		displayName = "Destroyer",
		icon = SidebarIcon.AI_DESTROYER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#800000",
		dynmapIcon = "destroyer",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.destroyer",
		balancingSupplier = IonServer.starshipBalancing::aiDestroyer
	),
	CRUISER(
		displayName = "Cruiser",
		icon = SidebarIcon.BATTLECRUISER_ICON.text,
		minSize = 12000,
		maxSize = 16000,
		minLevel = 70,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.COPPER_BLOCK,
		isWarship = true,
		color = "#FFD700",
		overridePermission = "ion.ships.override.70",
		dynmapIcon = "cruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		sinkProvider = SinkProvider.SinkProviders.CRUISER,
		balancingSupplier = IonServer.starshipBalancing::cruiser,
	),
	AI_CRUISER(
		displayName = "Cruiser",
		icon = SidebarIcon.AI_BATTLECRUISER_ICON.text,
		minSize = 12000,
		maxSize = 16000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#FFD700",
		dynmapIcon = "cruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.cruiser",
		sinkProvider = SinkProvider.SinkProviders.CRUISER,
		balancingSupplier = IonServer.starshipBalancing::aiCruiser
	),
	BATTLECRUISER(
		displayName = "Battlecruiser",
		icon = SidebarIcon.BATTLESHIP_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 80,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.0,
		menuItemMaterial = Material.DIAMOND_BLOCK,
		isWarship = true,
		color = "#0c5ce8",
		dynmapIcon = "battlecruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		overridePermission = "ion.ships.override.80",
		sinkProvider = SinkProvider.SinkProviders.BATTLECRUISER,
		balancingSupplier = IonServer.starshipBalancing::battlecruiser
	),
	AI_BATTLECRUISER(
		displayName = "Battlecruiser",
		icon = SidebarIcon.AI_BATTLESHIP_ICON.text,
		minSize = 12000,
		maxSize = 20000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#0c5ce8",
		dynmapIcon = "battlecruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.battlecruiser",
		sinkProvider = SinkProvider.SinkProviders.BATTLECRUISER,
		balancingSupplier = IonServer.starshipBalancing::aiBattlecruiser
	),
	BATTLESHIP(
		displayName = "Battleship",
		icon = SidebarIcon.BATTLESHIP_ICON.text,
		minSize = 20000,
		maxSize = 32000,
		minLevel = 1000,
		containerPercent = 0.015,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.EMERALD_BLOCK,
		isWarship = true,
		color = "#0c1cff",
		overridePermission = "ion.ships.override.battleship",
		balancingSupplier = IonServer.starshipBalancing::battleship
	),
	AI_BATTLESHIP(
		displayName = "Battleship",
		icon = SidebarIcon.AI_BATTLESHIP_ICON.text,
		minSize = 20000,
		maxSize = 32000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#0c1cff",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.battleship",
		balancingSupplier = IonServer.starshipBalancing::aiBattleship
	),
	DREADNOUGHT(
		displayName = "Dreadnought",
		icon = SidebarIcon.DREADNOUGHT_ICON.text,
		minSize = 32000,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.015,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.NETHERITE_BLOCK,
		isWarship = true,
		color = "#320385",
		overridePermission = "ion.ships.override.dreadnought",
		balancingSupplier = IonServer.starshipBalancing::dreadnought
	),
	AI_DREADNOUGHT(
		displayName = "Dreadnought",
		icon = SidebarIcon.AI_DREADNOUGHT_ICON.text,
		minSize = 32000,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SPONGE,
		isWarship = true,
		color = "#320385",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.dreadnought",
		balancingSupplier = IonServer.starshipBalancing::aiDreadnought
	),
	SHUTTLE(
		displayName = "Shuttle",
		icon = SidebarIcon.SHUTTLE_ICON.text,
		minSize = 100,
		maxSize = 1000,
		minLevel = 1,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.PRISMARINE_SHARD,
		isWarship = false,
		color = "#008033",
		overridePermission = "ion.ships.override.1",
		poweroverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 1,
		dynmapIcon = "shuttle",
		balancingSupplier = IonServer.starshipBalancing::shuttle
	),
	AI_SHUTTLE(
		displayName = "Shuttle",
		icon = SidebarIcon.AI_SHUTTLE_ICON.text,
		minSize = 100,
		maxSize = 1000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#008033",
		poweroverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 1,
		dynmapIcon = "shuttle",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.shuttle",
		balancingSupplier = IonServer.starshipBalancing::aiShuttle
	),
	TRANSPORT(
		displayName = "Transport",
		icon = SidebarIcon.TRANSPORT_ICON.text,
		minSize = 1000,
		maxSize = 2000,
		minLevel = 10,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.PRISMARINE_CRYSTALS,
		isWarship = false,
		color = "#008066",
		overridePermission = "ion.ships.override.10",
		poweroverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 2,
		dynmapIcon = "transport",
		balancingSupplier = IonServer.starshipBalancing::transport
	),
	AI_TRANSPORT(
		displayName = "Transport",
		icon = SidebarIcon.AI_TRANSPORT_ICON.text,
		minSize = 1000,
		maxSize = 2000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#008066",
		poweroverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 2,
		dynmapIcon = "transport",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.transport",
		balancingSupplier = IonServer.starshipBalancing::aiTransport
	),
	LIGHT_FREIGHTER(
		displayName = "Light Freighter",
		icon = SidebarIcon.LIGHT_FREIGHTER_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 20,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.PRISMARINE_SLAB,
		isWarship = false,
		color = "#008099",
		overridePermission = "ion.ships.override.20",
		poweroverrider = 0.7,
		maxMiningLasers = 2,
		miningLaserTier = 2,
		dynmapIcon = "light_freighter",
		balancingSupplier = IonServer.starshipBalancing::lightFreighter
	),
	AI_LIGHT_FREIGHTER(
		displayName = "Light Freighter",
		icon = SidebarIcon.AI_LIGHT_FREIGHTER_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#008099",
		poweroverrider = 0.7,
		maxMiningLasers = 2,
		miningLaserTier = 2,
		dynmapIcon = "light_freighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.light_freighter",
		balancingSupplier = IonServer.starshipBalancing::aiLightFreighter
	),
	MEDIUM_FREIGHTER(
		displayName = "Medium Freighter",
		icon = SidebarIcon.MEDIUM_FREIGHTER_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 40,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.PRISMARINE_STAIRS,
		isWarship = false,
		color = "#0080cc",
		poweroverrider = 0.7,
		maxMiningLasers = 4,
		miningLaserTier = 3,
		dynmapIcon = "medium_freighter",
		overridePermission = "ion.ships.ai.medium_freighter",
		balancingSupplier = IonServer.starshipBalancing::mediumFreighter
	),
	HEAVY_FREIGHTER(
		displayName = "Heavy Freighter",
		icon = SidebarIcon.HEAVY_FREIGHTER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 60,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.PRISMARINE,
		isWarship = false,
		color = "#0080ff",
		overridePermission = "ion.ships.override.60",
		poweroverrider = 0.7,
		maxMiningLasers = 6,
		miningLaserTier = 3,
		dynmapIcon = "heavy_freighter",
		balancingSupplier = IonServer.starshipBalancing::heavyFreighter
	),
	AI_HEAVY_FREIGHTER(
		displayName = "Heavy Freighter",
		icon = SidebarIcon.AI_HEAVY_FREIGHTER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#0080ff",
		poweroverrider = 0.7,
		maxMiningLasers = 6,
		miningLaserTier = 3,
		dynmapIcon = "heavy_freighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.heavy_freighter",
		balancingSupplier = IonServer.starshipBalancing::aiHeavyFreighter
	),
	BARGE(
		displayName = "Barge",
		icon = SidebarIcon.BARGE_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 80,
		containerPercent = 0.075,
		crateLimitMultiplier = 0.0,
		menuItemMaterial = Material.SEA_LANTERN,
		isWarship = false,
		color = "#0c5ce8",
		dynmapIcon = "barge",
		maxMiningLasers = 10,
		miningLaserTier = 4,
		overridePermission = "ion.ships.override.80",
		sinkProvider = SinkProvider.SinkProviders.BARGE,
		balancingSupplier = IonServer.starshipBalancing::barge
	),
	AI_BARGE(
		displayName = "Barge",
		icon = SidebarIcon.BARGE_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 1000,
		containerPercent = 0.075,
		crateLimitMultiplier = 0.0,
		menuItemMaterial = Material.SPONGE,
		isWarship = false,
		color = "#0c5ce8",
		dynmapIcon = "barge",
		maxMiningLasers = 10,
		miningLaserTier = 4,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.barge",
		sinkProvider = SinkProvider.SinkProviders.BARGE,
		balancingSupplier = IonServer.starshipBalancing::barge
	),
	PLATFORM(
		displayName = "Platform",
		minSize = 25,
		maxSize = 2000000,
		minLevel = 1,
		containerPercent = 100.0,
		crateLimitMultiplier = 100.0,
		concretePercent = 0.0,
		menuItemMaterial = Material.BEDROCK,
		isWarship = false,
			color = "#ffffff",
			overridePermission = "ion.ships.override.1",
			poweroverrider = 0.0,
			balancingSupplier = IonServer.starshipBalancing::platformBalancing
	),
	UNIDENTIFIEDSHIP(
		displayName = "UnidentifiedShip",
		minSize = 25,
		maxSize = 250000,
		minLevel = 1000,
		containerPercent = 100.0,
		concretePercent = 0.0,
		crateLimitMultiplier = 100.0,
		menuItemMaterial = Material.MUD_BRICKS,
		isWarship = true,
		color = "#d0e39d",
		overridePermission = "ion.ships.eventship",
		eventship = true,
		poweroverrider = 2.0,
		balancingSupplier = IonServer.starshipBalancing::eventShipBalancing
	),
	AI_SHIP(
		displayName = "AI Ship",
		minSize = 50,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.025,
		concretePercent = 0.0,
		crateLimitMultiplier = 0.5,
		menuItemMaterial = Material.SCULK,
		isWarship = true,
		color = "#d000d0",
		overridePermission = "ion.ships.aiship",
		balancingSupplier = IonServer.starshipBalancing::eventShipBalancing
	);

	val displayNameMiniMessage: String get() = "<$color>$displayName</$color>"
	val displayNameComponent: Component get() = text(displayName, TextColor.fromHexString(color))

	val menuItem: ItemStack = ItemStack(menuItemMaterial)
		.setDisplayNameAndGet(displayNameComponent)
		.setLoreAndGet(listOf(
			"Min Block Count: $minSize",
			"Max Block Count: $maxSize",
			"Min Level: $minLevel",
			"Max Container:Total Blocks Ratio: $containerPercent",
			"Crate Limit Multiplier: $crateLimitMultiplier",
			"Sneak Fly Accel Distance: ${balancingSupplier.get().sneakFlyAccelDistance}",
			"Max Sneak Fly Accel: ${balancingSupplier.get().maxSneakFlyAccel}",
			"Interdiction Range: ${balancingSupplier.get().interdictionRange}",
			"Hyperspace Range Multiplier: ${balancingSupplier.get().hyperspaceRangeMultiplier}",
			"Warship: $isWarship"
		))

	fun canUse(player: Player): Boolean =
			player.hasPermission("starships.anyship") || player.hasPermission(overridePermission) || Levels[player] >= minLevel

	companion object {
		fun getUnlockedTypes(player: Player): List<StarshipType> = entries
			.filter { it.canUse(player) }
			.filter { !it.eventship.and(!player.hasPermission("ion.ships.eventship")) }
			.sortedBy { it.minLevel }
	}
}
