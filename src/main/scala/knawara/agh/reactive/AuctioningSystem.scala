package knawara.agh.reactive

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

import scala.util.Random

/* todo
- buyer - bid on list of auctions
- send back current price
- auction duration
- highest bidder
- handle bidder destruction
 */

case class BidTick()
case class PlaceBid(val price: Long)
case class BidTooSmall()

class AuctioningSystem extends Actor {
  val auction = context.actorOf(Props[Auction])
  val buyer = context.actorOf(Buyer.props(auction))

  override def receive = {
    case _ @ msg => println(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object Buyer {
  def props(auction: ActorRef): Props = Props(new Buyer(auction))

  private def scheduleBidTick(target: ActorRef) = {
    import Bootstrapper.asystem.dispatcher
    import scala.concurrent.duration._

    Bootstrapper.asystem.scheduler.schedule(1 second, 500 millis , target, BidTick())
  }
}

class Buyer(auction: ActorRef) extends Actor {
  Buyer.scheduleBidTick(self)

  override def receive = {
    case BidTick() => auction ! PlaceBid(Random.nextInt(100000))
    case BidTooSmall() => println("Bid was too small")
    case _ @ msg => println(s"Buyer got new message: ${msg.toString}")
  }
}

class Auction extends Actor {
  var price = 0L

  override def receive = {
    case PlaceBid(offeredPrice) => {
      println(s"Bid received: $offeredPrice")
      if(offeredPrice > price) price = offeredPrice
      else sender() ! BidTooSmall()
    }
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }
}

object Bootstrapper {
  val asystem = ActorSystem("AuctioningSystem")

  private def initializeActorSystem() = {
    asystem.actorOf(Props[AuctioningSystem])
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}