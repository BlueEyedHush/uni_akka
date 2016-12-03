package knawara.agh.reactive

import akka.actor.{ActorRef, Actor => AkkaActor}
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.collection.mutable

package object AuctionSearch {
  case class Register(val title: AuctionTitle, val ref: ActorRef)
  case class Lookup(val query: String)
  case class Result(val query: String, val auction: List[ActorRef])

  class Actor extends AkkaActor {
    val log = Logging(context.system, this)
    val aucitonRegistry = mutable.ListBuffer[(AuctionTitle, ActorRef)]()

    override def receive = {
      case Register(title, ref) =>
        log.debug(s"registering: ${title.title}")
        aucitonRegistry += ((title, ref))
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
