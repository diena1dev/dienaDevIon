package net.horizonsend.ion.discord.features.redis

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import net.horizonsend.ion.common.IonComponent
import net.horizonsend.ion.common.extensions.CommonPlayer
import net.horizonsend.ion.common.utils.discord.Channel
import net.horizonsend.ion.common.utils.redis.RedisActions
import net.horizonsend.ion.common.utils.redis.actions.RedisResponseAction
import net.horizonsend.ion.discord.utils.Messages

object Messaging : IonComponent() {
	val discord_action = { (channel, serializedEmbed): Pair<Channel, String> ->
		Messages.sendFromJson(Channel.GLOBAL, serializedEmbed)
	}.registerRedisAction("notify-discord", runSync = false)

	val getPlayersAction = object : RedisResponseAction<String, List<CommonPlayer>>(
		"get-players",
		object : TypeToken<String>() {}.type,
		false
	) {
		override fun createReply(data: String): List<CommonPlayer> {
			return listOf()
		}
	}.apply { RedisActions.register(this) }

	fun getPlayers(serverName: String) = runBlocking { getPlayersAction.request(serverName).get() }

//	val getPlayerAction = object : RedisReplyAction<String, List<CommonPlayer>>(
//		"get-players",
//		object : TypeToken<Collection<String>>() {}.type,
//		false
//	) {
//		override fun onReceive(data: String) {
//			TODO("Not yet implemented")
//		}
//
//		override fun createReply(): List<CommonPlayer> {
//			TODO("Not yet implemented")
//		}
//	}
}