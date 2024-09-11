package net.horizonsend.ion.server.features.multiblock.type.starship.mininglasers

import net.horizonsend.ion.server.features.multiblock.Multiblock
import net.horizonsend.ion.server.features.multiblock.type.PowerStoringMultiblock
import net.horizonsend.ion.server.features.multiblock.type.starship.SubsystemMultiblock
import net.horizonsend.ion.server.features.starship.active.ActiveControlledStarship
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.misc.MiningLaserSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.getFacing
import net.horizonsend.ion.server.miscellaneous.utils.leftFace
import net.horizonsend.ion.server.miscellaneous.utils.rightFace
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

abstract class MiningLaserMultiblock : Multiblock(), SubsystemMultiblock<MiningLaserSubsystem>, PowerStoringMultiblock {
	override val name = "mininglaser"
	abstract val range: Double

	abstract val beamOrigin: Triple<Int, Int, Int>
	abstract val mineRadius: Int
	abstract val beamCount: Int
	abstract val maxBroken: Int
	abstract val sound: String
	abstract val side: BlockFace
	abstract val tier: Int
	abstract val mirrored: Boolean

	fun getOutput(sign: Sign): Inventory {
		val direction = sign.getFacing().oppositeFace

		return if (!mirrored)
			(sign.block.getRelative(direction)
				.getRelative(side.oppositeFace)
				.getRelative(direction.leftFace)
				.getState(false) as InventoryHolder
					).inventory
		else
			(sign.block.getRelative(direction)
				.getRelative(side.oppositeFace)
				.getRelative(direction.rightFace)
				.getState(false) as InventoryHolder
					).inventory
	}

	override fun createSubsystem(starship: ActiveStarship, pos: Vec3i, face: BlockFace): MiningLaserSubsystem {
		if (starship is ActiveControlledStarship) {
			return MiningLaserSubsystem(starship, pos, face, this)
		} else {
			throw IllegalStateException("Mining lasers can be only used on Player starships")
		}
	}


	abstract fun getFirePointOffset(): Vec3i
}