package knawara.agh.reactive.performance

import akka.actor.{Actor => AkkaActor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable

package object AuctionSearch {
  case class Register(val title: AuctionTitle, val ref: ActorRef)
  case object Registered
  case class Lookup(val query: String)
  case class Result(val query: String, val auction: List[ActorRef])

  object Actor {
    def props() = Props(new Actor())
  }

  class Actor extends AkkaActor {
    val log = Logging(context.system, this)
    val aucitonRegistry = mutable.ListBuffer[(AuctionTitle, ActorRef)]()

    override def receive = {
      case Register(title, ref) =>
        aucitonRegistry += ((title, ref))
        sender() ! Registered
      case Lookup(searchTerm) =>
        val results = aucitonRegistry
          .filter({ case (auctionTitle, ref) =>
            if(auctionTitle.title.contains(searchTerm)) true
            else false
          })
          .map({case (title, actorRef) => actorRef})
        sender() ! Result(query = searchTerm, auction = results.toList)
      case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
    }
  }
}
