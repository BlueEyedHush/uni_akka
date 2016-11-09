package knawara.agh.reactive

import akka.actor.{Props, ActorSystem, Actor}
import akka.actor.Actor.Receive

class AuctioningSystem extends Actor {
  override def receive(): Actor.Receive = ???
}

class Buyer extends Actor {
  override def receive(): Receive = ???
}

class Auction extends Actor {
  override def receive(): Actor.Receive = ???
}

object Bootstrapper {
  private def initializeActorSystem() = {
    val asystem = ActorSystem("AuctioningSystem")
    asystem.actorOf(Props[AuctioningSystem])
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}