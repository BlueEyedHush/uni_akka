package knawara.agh.reactive.performance

import akka.actor.{Actor => AkkaActor, ActorRef, Props}
import akka.event.Logging
import knawara.agh.reactive.performance.AuctionSearch.Registered

import scala.util.Random

class AuctionTitle(val title: String) extends AnyVal

package object Seller {
  case object GiveMeYourAuctions
  case object AllRegistered
  case class SellerAuctions(val auctions: Set[ActorRef])

  object Actor {
    def props(auctionTitles: Seq[AuctionTitle]): Props = Props(new Actor(auctionTitles))
  }

  class Actor(auctionTitles: Seq[AuctionTitle]) extends AkkaActor {
    val log = Logging(context.system, this)
    val registryActorSelection = context.actorSelection("/user/system/mastersearch")

    val auctions = auctionTitles.map(auctionTitle => {
      import scala.concurrent.duration._
      val duration = 10 minutes
      val actorRef = context.actorOf(Auction.Actor.props(duration, Some(self)), s"auction-${auctionTitle.title}")
      registryActorSelection ! AuctionSearch.Register(auctionTitle, actorRef)
      actorRef
    })

    var registeredCount = 0

    override def receive = {
      case Registered =>
        registeredCount += 1
        if(registeredCount == auctionTitles.size) context.parent ! AllRegistered
      case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
    }
  }

}
