package knawara.agh.reactive

import akka.actor._
import akka.event.Logging
import scala.concurrent.duration._

class AuctioningSystem extends Actor {
  val log = Logging(context.system, this)
  val registry = context.actorOf(Props[AuctionSearchActor], "registry")

  private[this] val titles = Set("1", "2", "3").map(s => new AuctionTitle(s))
  val seller = context.actorOf(SellerActor.props(titles))
  seller ! GiveMeYourAuctions

  override def receive = {
    case SellerAuctions(auctions) =>
      auctions
          .flatMap(actorRef => {
            List.fill(3)(actorRef)
              .zip(Set(1,2,3))
          })
        .foreach({ case (auctionRef, idx) =>
          context.actorOf(BuyerActor.props(auctionRef), s"buyer-${auctionRef.path.name}-$idx")})
    case _ @ msg => log.debug(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object Bootstrapper {
  val asystem = ActorSystem("AuctioningSystem")

  private def initializeActorSystem() = {
    asystem.actorOf(Props[AuctioningSystem], "system")
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}