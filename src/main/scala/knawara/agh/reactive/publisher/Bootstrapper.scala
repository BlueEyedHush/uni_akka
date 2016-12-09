package knawara.agh.reactive.publisher

import akka.actor.ActorSystem

object Bootstrapper {
  def main(args: Array[String]): Unit = {
    ActorSystem("publishersystem").actorOf(AuctionPublisher.Actor.props())
  }
}
