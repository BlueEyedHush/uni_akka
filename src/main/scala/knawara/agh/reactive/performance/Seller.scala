package knawara.agh.reactive.performance

import akka.actor.{Actor => AkkaActor, ActorRef, Props}
import akka.event.Logging

import scala.util.Random

class AuctionTitle(val title: String) extends AnyVal

package object Seller {
  case object GiveMeYourAuctions
  case class SellerAuctions(val auctions: Set[ActorRef])

  object Actor {
    def props(auctionTitles: Set[AuctionTitle]): Props = Props(new Actor(auctionTitles))
  }

  class Actor(auctionTitles: Set[AuctionTitle]) extends AkkaActor {
    val log = Logging(context.system, this)
    val registryActorSelection = context.actorSelection("/user/system/mastersearch")

    val auctions = auctionTitles.map(auctionTitle => {
      import scala.concurrent.duration._
      val duration = (Random.nextInt(5) + 15) seconds
      val actorRef = context.actorOf(Auction.Actor.props(duration, Some(self)), s"auction-${auctionTitle.title}")
      registryActorSelection ! AuctionSearch.Register(auctionTitle, actorRef)
      actorRef
    })

    override def receive = {
      case GiveMeYourAuctions => sender() ! SellerAuctions(auctions)
      case Auction.Sold(ref) => log.debug("[{}] item {} sold", self.path.name, ref.path.name)
      case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
    }
  }

}
