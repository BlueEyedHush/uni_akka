package knawara.agh.reactive.publisher

import akka.actor.{Actor => AkkaActor, Props}

package object AuctionPublisher {
  case class Publish(val message: String)

  object Actor {
    def props() = Props(new Actor)
  }

  class Actor extends AkkaActor {
    override def receive = {
      case Publish(message) => println(message)
    }
  }
}

