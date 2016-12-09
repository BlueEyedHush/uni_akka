package knawara.agh.reactive.publisher

import akka.actor.{Actor => AkkaActor, Props}
import akka.event.Logging

package object AuctionPublisher {
  case class Publish(val message: String)
  case object Success

  object Actor {
    def props() = Props(new Actor)
  }

  class Actor extends AkkaActor {
    val log = Logging(context.system, this)
    log.debug(s"Publisher initialization as ${self.path}")

    override def receive = {
      case Publish(message) =>
        println(message)
        sender() ! Success
    }
  }
}

