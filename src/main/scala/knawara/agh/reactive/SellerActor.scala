package knawara.agh.reactive

import akka.actor.{ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.util.Random

class AuctionTitle(val title: String) extends AnyVal

case object GiveMeYourAuctions
case class SellerAuctions(val auctions: Set[ActorRef])

object SellerActor {
  def props(auctionTitles: Set[AuctionTitle]): Props = Props(new SellerActor(auctionTitles))
}

class SellerActor(auctionTitles: Set[AuctionTitle]) extends Actor {
  val log = Logging(context.system, this)
  val registryActorSelection = context.actorSelection("/user/system/registry")


  val auctions = auctionTitles.map(auctionTitle => {
    import scala.concurrent.duration._
    val duration = (Random.nextInt(5) + 15) seconds
    val actorRef = context.actorOf(AuctionActor.props(duration, self), s"auction-${auctionTitle.title}")
    registryActorSelection ! RegisterAuction(auctionTitle, actorRef)
    actorRef
  })

  override def receive = {
    case GiveMeYourAuctions => sender() ! SellerAuctions(auctions)
    case Sold(ref) => log.debug("[{}] item {} sold", self.path.name, ref.path.name)
    case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
  }
}
