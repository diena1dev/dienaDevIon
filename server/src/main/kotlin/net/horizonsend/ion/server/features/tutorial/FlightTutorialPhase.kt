package net.horizonsend.ion.server.features.tutorial

import io.papermc.paper.util.Tick
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.text.BOLD
import net.horizonsend.ion.common.utils.text.HORIZONS_END
import net.horizonsend.ion.common.utils.text.miniMessage
import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.starship.DeactivatedPlayerStarships
import net.horizonsend.ion.server.features.starship.PilotedStarships
import net.horizonsend.ion.server.features.starship.control.controllers.player.PlayerController
import net.horizonsend.ion.server.features.starship.control.movement.StarshipControl
import net.horizonsend.ion.server.features.starship.destruction.StarshipDestruction
import net.horizonsend.ion.server.features.starship.event.StarshipComputerOpenMenuEvent
import net.horizonsend.ion.server.features.starship.event.StarshipDetectedEvent
import net.horizonsend.ion.server.features.starship.event.StarshipPilotEvent
import net.horizonsend.ion.server.features.starship.event.StarshipUnpilotEvent
import net.horizonsend.ion.server.features.starship.event.movement.StarshipRotateEvent
import net.horizonsend.ion.server.features.starship.event.movement.StarshipStartCruisingEvent
import net.horizonsend.ion.server.features.starship.event.movement.StarshipStopCruisingEvent
import net.horizonsend.ion.server.features.starship.event.movement.StarshipTranslateEvent
import net.horizonsend.ion.server.features.tutorial.message.ActionMessage
import net.horizonsend.ion.server.features.tutorial.message.PopupMessage
import net.horizonsend.ion.server.features.tutorial.message.TutorialMessage
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.action
import net.horizonsend.ion.server.miscellaneous.utils.colorize
import net.horizonsend.ion.server.miscellaneous.utils.execConsoleCmd
import net.horizonsend.ion.server.miscellaneous.utils.listen
import net.horizonsend.ion.server.miscellaneous.utils.msg
import net.horizonsend.ion.server.miscellaneous.utils.paste
import net.horizonsend.ion.server.miscellaneous.utils.readSchematic
import net.horizonsend.ion.server.miscellaneous.utils.setDisplayNameAndGet
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.BLUE
import net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA
import net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN
import net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.DARK_RED
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.title.Title.title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.time.Duration
import kotlin.math.abs

enum class FlightTutorialPhase(
	override vararg val messages: TutorialMessage,
	override val cancelEvent: Boolean = true,
	override val announceCompletion: Boolean = false,
) : TutorialPhase {
	GET_SHIP_CONTROLLER(
		PopupMessage(text("Tutorial", DARK_AQUA), text("You can leave by doing /tutorialexit", DARK_RED)),
		PopupMessage(text("Tutorial", DARK_AQUA), ofChildren(HORIZONS_END, text(" has unique features to learn like spaceships", DARK_GREEN))),
		PopupMessage(text("Tutorial", DARK_AQUA), text("This tutorial teaches you how to fly a spaceship!")),
		PopupMessage(text("Controller", BLUE), text("First, you need a ship controller")),
		PopupMessage(text("Controller", BLUE), text("Ship controllers are needed to fly ships")),
		PopupMessage(text("Controller", BLUE), text("You can always get one with /kit controller")),
		PopupMessage(text("Controller", BLUE), text("Enter the command: /kit controller", GOLD, BOLD)),
		cancelEvent = false
	) {
		override fun setupHandlers() = on<PlayerCommandPreprocessEvent>({ it.player }) { event, player ->
			if (event.message.removePrefix("/").equals("kit controller", ignoreCase = true)) {
				event.isCancelled = true

				val item = ItemStack(StarshipControl.CONTROLLER_TYPE, 1)

				player.world.dropItem(player.eyeLocation.add(player.location.direction.multiply(0.25)), item)

				moveToNextStep(player)
			}
		}
	},

	PLACE_SHIP_COMPUTER(
		PopupMessage(text("Computer", DARK_PURPLE), text("Now you need a ship computer")),
		PopupMessage(text("Computer", DARK_PURPLE), text("Ship computers are used to start the ship")),
		ActionMessage(text("Computer", DARK_PURPLE), text("You have been given one ship computer")) { player ->
			val item = ItemStack(Material.JUKEBOX, 1).setDisplayNameAndGet("&rStarship Computer".colorize())
			player.inventory.addItem(item).forEach { (_, leftover) ->
				player.world.dropItem(player.eyeLocation, leftover)
			}
		},
		PopupMessage(text("Computer", DARK_PURPLE), text("Place ship computer (black jukebox)", LIGHT_PURPLE, BOLD))
	) {
		override fun setupHandlers() = on<BlockPlaceEvent>({ it.player }) { event, player ->
			if (event.block.type == Material.JUKEBOX) {
				moveToNextStep(player)
			}
		}
	},

	OPEN_COMPUTER_MENU(
		PopupMessage(text("Computer Menu", DARK_AQUA), text("Ship computers are used via their menu")),
		PopupMessage(text("Computer Menu", DARK_AQUA), text("Left click computer with controller (clock)", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipComputerOpenMenuEvent>({ it.player }) { _, player ->
			moveToNextStep(player)
			Tasks.syncDelay(15, player::closeInventory)
		}
	},

	DETECT_SHIP(
		PopupMessage(text("Detection", GOLD), text("Now you need to detect the ship")),
		PopupMessage(text("Detection", GOLD), text("Detecting determines which blocks are your ship")),
		PopupMessage(text("Detection", GOLD), text("Some block types are detected, but not stone etc")),
		PopupMessage(text("Detection", GOLD), text("Use the ship computer to detect")),
		PopupMessage(text("Detection", GOLD), "<yellow><bold>Open the menu again & click <dark_purple><bold>Re-Detect".miniMessage())
	) {
		override fun setupHandlers() = on<StarshipDetectedEvent>({ it.player }) { _, player ->
			moveToNextStep(player)
		}
	},

	PILOT_SHIP(
		PopupMessage(text("Piloting", GREEN), text("Now you need to pilot the ship")),
		PopupMessage(text("Piloting", GREEN), text("Ships only move while they are piloted")),
		PopupMessage(text("Piloting", GREEN), text("Additionally, shields only work while piloted")),
		PopupMessage(text("Piloting", GREEN), text("Right click computer with controller (clock)", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipPilotEvent>({ it.player }) { _, player ->
			moveToNextStep(player)
		}
	},

	SHIFT_FLY_FORWARD(
		PopupMessage(text("Moving", LIGHT_PURPLE), text("You can move ships while piloted")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("There are various ways to move ships")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("The most basic way is 'shift' flying")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("To shift fly, first hold your controller")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("Then, hold the sneak key (default key shift)")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("This moves you the way you're facing")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("For practice, shift fly forwards")),
		PopupMessage(text("Moving", LIGHT_PURPLE), text("Hold the controller, face the window, & sneak", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipTranslateEvent>({ it.starship.playerPilot }) { _, player ->
			moveToNextStep(player)
		}
	},

	SHIFT_FLY_DOWN(
		PopupMessage(text("Moving Down", DARK_GREEN), text("You can shift fly any direction, even down")),
		PopupMessage(text("Moving Down", DARK_GREEN), text("Shift flying down lets you land on a planet")),
		PopupMessage(text("Moving Down", DARK_GREEN), text("Hold the controller, face down, & sneak", GOLD, BOLD)),
		cancelEvent = false // let them keep shift flying forward
	) {
		override fun setupHandlers() = on<StarshipTranslateEvent>({ it.starship.playerPilot }) { event, player ->
			if (event.y < 0) {
				moveToNextStep(player)
			} else {
				player action "&eYou're moving, but not straight down!"
			}
		}
	},

	TURN_RIGHT(
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Besides moving, you can turn your ship")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Ships can face the 4 directions (N/E/S/W)")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("To turn your ship, you can use the helm sign")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Right click the sign with [helm] on it")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Then, holding the controller, click again")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Right click to turn right, left click for left")),
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Hold the controller, right click the helm sign", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipRotateEvent>({ it.starship.playerPilot }) { event, player ->
			if (event.clockwise) {
				moveToNextStep(player)
			}
		}
	},

	TURN_LEFT(
		PopupMessage(text("Rotating", LIGHT_PURPLE), text("Now left click the helm sign", GOLD, BOLD)),
		cancelEvent = false // let them rotate
	) {
		override fun setupHandlers() = on<StarshipRotateEvent>({ it.starship.playerPilot }) { event, player ->
			if (!event.clockwise) {
				moveToNextStep(player)
			}
		}
	},

	CRUISE_START(
		PopupMessage(text("Cruising", BLUE), text("Cruise to move steadily over long distances")),
		PopupMessage(text("Cruising", BLUE), text("Cruising uses thrusters to determine speed")),
		PopupMessage(text("Cruising", BLUE), text("To cruise, right click the [cruise] sign")),
		PopupMessage(text("Cruising", BLUE), text("Right click again to cruise")),
		PopupMessage(text("Cruising", BLUE), text("Cruising works forwards and diagonally of it")),
		PopupMessage(text("Cruising", BLUE), text("If you can't face the right way, turn the ship")),
		PopupMessage(text("Cruising", BLUE), text("Hold the controller & right click cruise sign", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipStartCruisingEvent>({ it.starship.playerPilot }) { event, player ->
			moveToNextStep(player)
		}
	},

	CRUISE_STOP(
		PopupMessage(text("Stop Cruising", BLUE), text("Left click the cruise sign to stop", GOLD, BOLD))
	) {
		override fun setupHandlers() = on<StarshipStopCruisingEvent>({ it.starship.playerPilot }) { event, player ->
			moveToNextStep(player)
		}
	},

	RELEASE_SHIP(
		PopupMessage(text("Releasing", GRAY), text("When done flying, release to stop piloting")),
		PopupMessage(text("Releasing", GRAY), text("Releasing also lets you leave the ship")),
		PopupMessage(text("Releasing", GRAY), text("Type /release or right click the computer", YELLOW, BOLD))
	) {
		override fun setupHandlers() = on<StarshipUnpilotEvent>({ (it.controller as? PlayerController)?.player }) { event, player ->
			event.isCancelled = true
			StarshipDestruction.vanish(event.starship)
			moveToNextStep(player)
		}
	}

	;

	override val entries: List<TutorialPhase> get() = values().toList()

	companion object : TutorialCompanion() {
		override val entries: List<TutorialPhase> get() = FlightTutorialPhase.values().toList()

		private val byOrdinal: Map<Int, FlightTutorialPhase> = values().associateBy(FlightTutorialPhase::ordinal)

		override val WORLD_NAME: String = "FlightTutorial"

		override fun onEnable() {
			listen<StarshipRotateEvent> { event ->
				val player = (event.starship.controller as? PlayerController)?.player ?: return@listen

				if (isTutorialWorld(player.world) && (getPhase(player) ?: LAST).ordinal < TURN_RIGHT.ordinal) {
					event.isCancelled = true
				}
			}

			listen<StarshipStartCruisingEvent> { event ->
				val player = (event.starship.controller as? PlayerController)?.player ?: return@listen

				if (isTutorialWorld(player.world) && (getPhase(player) ?: LAST).ordinal < CRUISE_START.ordinal) {
					event.isCancelled = true
				}
			}

			listen<StarshipTranslateEvent> { event ->
				val player = (event.starship.controller as? PlayerController)?.player ?: return@listen

				if (isTutorialWorld(player.world) && (getPhase(player) ?: LAST).ordinal < SHIFT_FLY_FORWARD.ordinal
				) {
					event.isCancelled = true
				}
			}

			// if someone places a ship computer in an existing one, overwrite it
			listen<BlockPlaceEvent>(priority = EventPriority.LOWEST) { event ->
				if (isTutorialWorld(event.block.world) && event.block.type == Material.JUKEBOX) {
					val loc = event.block.location

					DeactivatedPlayerStarships[loc.world, loc.blockX, loc.blockY, loc.blockZ]?.let { state ->
						log.warn("Deleted computer ${loc.world.name}@${Vec3i(loc)} because someone placed over it")

						DeactivatedPlayerStarships.destroyAsync(state)
					}
				}
			}

			listen<StarshipUnpilotEvent>(priority = EventPriority.LOW) { event ->
				val controller = event.controller

				if (controller !is PlayerController) return@listen

				val player = controller.player

				if (!isTutorialWorld(player.world) || Tutorials.playersInTutorials[player] == FlightTutorialPhase.LAST) {
					return@listen
				}

				player.userError("You unpiloted your starship, stopping tutorial")

				stop(player)

				StarshipDestruction.vanish(event.starship)
				event.isCancelled = true

				player.showTitle(
					title(
						text("Tutorial Cancelled", RED, BOLD),
						text("Unpiloted (right clicked computer) before the tutorial end", GRAY),
						net.kyori.adventure.title.Title.Times.times(
							Duration.of(10, Tick.tick()),
							Duration.of(Int.MAX_VALUE - 20L, Tick.tick()),
							Duration.of(0, Tick.tick())
						)
					)
				)
			}
		}

		override fun teleportToStart(player: Player) {
			execConsoleCmd("warp tutorialstart ${player.name}")
		}

		override fun teleportToEnd(player: Player) {
			execConsoleCmd("warp tutorialend ${player.name}")
			player msg "&l&o&cCheck out this tutorial too&8:&b https://youtu.be/AFfDmmpMXQw"
		}

		override fun startTutorial(player: Player) {
			require(PilotedStarships[player] == null)

			playersInTutorials[player] = FIRST

			val loc: Location = getSafeLocation()
			loadShip(loc)

			player.teleport(loc)
			player.teleport(loc) // teleport a second time, because, well... minecraft
		}

		private const val distance = 1000

		private fun getSafeLocation(): Location {
			var x = distance
			while (getWorld().players.any { abs(it.location.blockX - x) <= distance }) {
				x += distance
			}
			return Location(getWorld(), x.toDouble() + 0.5, (getWorld().maxHeight / 2).toDouble(), 0.5)
		}

		private fun loadShip(loc: Location) {
			val file = File(IonServer.dataFolder, "tutorial_ship.schem")

			if (!file.exists()) {
				error("${file.absolutePath} doesn't exist!")
			}

			val clipboard = readSchematic(file) ?: error("Failed to read ${file.path}")
			clipboard.paste(loc.world, loc.blockX, loc.blockY, loc.blockZ)
		}
	}
}