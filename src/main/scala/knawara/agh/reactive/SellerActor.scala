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

  val auctions = auctionTitles.map(t => {
    import scala.concurrent.duration._
    val duration = (Random.nextInt(5) + 15) seconds
    val actorRef = context.actorOf(AuctionActor.props(duration), s"auction-${t.title}")
    actorRef
  })

  override def receive = {
    case GiveMeYourAuctions => sender() ! SellerAuctions(auctions)
    case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
  }
}
