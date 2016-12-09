package knawara.agh.reactive.auctioning

import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory

class AuctioningSystem extends Actor {
  val log = Logging(context.system, this)
  val registry = context.actorOf(Props[AuctionSearch.Actor], "registry")

  private[this] val titles = Set("1", "2", "3")
  val seller = context.actorOf(Seller.Actor.props(titles.map(s => new AuctionTitle(s))), "seller-0")
  val buyers = titles
    .flatMap(title => List.fill(3)(title).zip(1 to titles.size))
    .map({
      case (title, buyerId) => context.actorOf(Buyer.Actor.props(title, 5L), s"buyer-$title-$buyerId")
    })
  val notifier = context.actorOf(Notifier.props(), "notifier")

  override def receive = {
    case _ @ msg => log.debug(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object AuctioningBootstrapper {
  def createActorSystem() = {
    val originalConfig = ConfigFactory.load()
    val overriddenConfig = originalConfig.getConfig("auctioningsys").withFallback(originalConfig)
    ActorSystem("auctioningsystem", Some(overriddenConfig)).actorOf(Props[AuctioningSystem], "system")
  }

  def main(args: Array[String]): Unit = {
    createActorSystem()
  }
}