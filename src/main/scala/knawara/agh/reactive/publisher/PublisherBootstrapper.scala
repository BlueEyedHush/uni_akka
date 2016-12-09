package knawara.agh.reactive.publisher

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, Config}

object PublisherBootstrapper {
  def createActorSystem() = {
    val originalConfig = ConfigFactory.load()
    val overriddenConfig = originalConfig.getConfig("publishersys").withFallback(originalConfig)
    ActorSystem("publishersystem").actorOf(AuctionPublisher.Actor.props(), "publisher")
  }

  def main(args: Array[String]): Unit = {
    createActorSystem()
  }
}
