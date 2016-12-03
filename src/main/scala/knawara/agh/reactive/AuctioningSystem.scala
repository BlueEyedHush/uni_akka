package knawara.agh.reactive

import akka.actor._
import akka.event.Logging

class AuctioningSystem extends Actor {
  val log = Logging(context.system, this)
  val registry = context.actorOf(Props[AuctionSearchActor], "registry")

  private[this] val titles = Set("1", "2", "3")
  val seller = context.actorOf(SellerActor.props(titles.map(s => new AuctionTitle(s))), "seller-0")
  val buyers = titles
    .flatMap(title => List.fill(3)(title).zip(1 to titles.size))
    .map({
      case (title, buyerId) => context.actorOf(Buyer.Actor.props(title), s"buyer-$title-$buyerId")
    })

  override def receive = {
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