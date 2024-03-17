package com.payrespect.autowhitelist
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit.dispatchCommand
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

data class Request(
    val name:String="",
    val channel:MessageChannelUnion
)

val mutex = Mutex()
var reqlist = listOf<Request>()


class Plugin : JavaPlugin(){
    lateinit var jda : JDA
    override fun onEnable(){
        println("Plugin Enabled.")

        jda = JDABuilder.createDefault(/* your discord bot client id here */)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .build()
        jda.addEventListener(EventListener(this))

        server.scheduler.runTaskTimer(this, fun(){
            runBlocking {
                with(mutex) {
                    if (reqlist.isNotEmpty()) {
                        reqlist.forEach { request ->
                            if(server.whitelistedPlayers.contains(server.getOfflinePlayer(request.name))){
                                request.channel.sendMessage("[${request.name}]님은 이미 등록되어 있습니다.").queue()
                                return@forEach
                            }
                            dispatchCommand(server.consoleSender, "whitelist add ${request.name}")
                            println("[화이트리스트 등록] ${request.name}")
                            request.channel.sendMessage("[${request.name}]님 등록 완료").queue()
                        }
                        reqlist = emptyList<Request>()
                    }

                }
            }
        },0,100
        )
    }

    override fun onDisable(){
        jda.shutdownNow()
    }

}

// Event listener for discord bot
class EventListener(val plugin: Plugin) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        println("Discord Bot READY")
    }

    override fun onMessageReceived(event: MessageReceivedEvent){
        if(event.author.isBot) return
        val content : String = event.message.contentRaw
        if(content.matches(Regex("^[a-zA-Z0-9_]{2,16}$"))){
            runBlocking {
                with(mutex) {
                    reqlist += Request(content,event.channel)
                }
                event.channel.sendMessage("[$content] 신청됨").queue()
            }
        }
    }
}
