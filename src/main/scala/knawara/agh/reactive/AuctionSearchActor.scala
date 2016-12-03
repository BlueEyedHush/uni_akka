package knawara.agh.reactive

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.collection.mutable

case class RegisterAuction(val title: AuctionTitle, val ref: ActorRef)
case class LookupAuction(val title: AuctionTitle)
case class LookupResult(val title: AuctionTitle, val auction: Option[ActorRef])

class AuctionSearchActor extends Actor {
  val log = Logging(context.system, this)
  val aucitonRegistry = mutable.Map[AuctionTitle, ActorRef]()

  override def receive = {
    case RegisterAuction(title, ref) =>
      log.debug(s"registering: ${title.title}")
      aucitonRegistry + (title -> ref)
    case LookupAuction(searchTerm) => sender() ! LookupResult(title = searchTerm, auction = aucitonRegistry.get(searchTerm))
    case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
  }
}
