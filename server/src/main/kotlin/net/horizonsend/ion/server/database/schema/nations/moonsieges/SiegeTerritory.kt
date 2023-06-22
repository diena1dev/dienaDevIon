package net.horizonsend.ion.server.database.schema.nations.moonsieges

import com.mongodb.client.FindIterable
import net.horizonsend.ion.server.database.DbObject
import net.horizonsend.ion.server.database.Oid
import net.horizonsend.ion.server.database.OidDbObjectCompanion
import net.horizonsend.ion.server.database.schema.nations.Nation
import net.horizonsend.ion.server.database.schema.space.Planet
import org.bukkit.Bukkit
import org.bukkit.World
import org.litote.kmongo.ensureIndex

data class SiegeTerritory(
	override val _id: Oid<SiegeTerritory>,
	val name: String,
	val world: String,
	val nation: Oid<Nation>?,
	var polygonData: ByteArray,
) : DbObject {
	companion object : OidDbObjectCompanion<SiegeTerritory>(
		SiegeTerritory::class,
		{
			ensureIndex(SiegeTerritory::name)
			ensureIndex(SiegeTerritory::nation)
		}
	) {

		fun create() = TODO("")
	}

	fun bukkitWorld(): World = Bukkit.getWorld(world) ?: error("World $world not loaded!")
}
