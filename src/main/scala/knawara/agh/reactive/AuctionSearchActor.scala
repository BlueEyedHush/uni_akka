package knawara.agh.reactive

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.collection.mutable

case class RegisterAuction(val title: AuctionTitle, val ref: ActorRef)
case class LookupAuction(val query: String)
case class LookupResult(val query: String, val auction: List[ActorRef])

class AuctionSearchActor extends Actor {
  val log = Logging(context.system, this)
  val aucitonRegistry = mutable.ListBuffer[(AuctionTitle, ActorRef)]()

  override def receive = {
    case RegisterAuction(title, ref) =>
      log.debug(s"registering: ${title.title}")
      aucitonRegistry += ((title, ref))
    case LookupAuction(searchTerm) =>
      val results = aucitonRegistry
        .filter({ case (auctionTitle, ref) =>
          if(auctionTitle.title.contains(searchTerm)) true
          else false
        })
        .map({case (title, actorRef) => actorRef})
      sender() ! LookupResult(query = searchTerm, auction = results.toList)
    case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
  }
}
