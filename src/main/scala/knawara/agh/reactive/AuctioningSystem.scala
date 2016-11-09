package knawara.agh.reactive

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

import scala.util.Random

/* todo
- bid using scheduler, with delay
- bid fixed price
- bid random price
- buyer - bid on list of auctions
- send back current price
- auction duration
- highest bidder
- handle bidder destruction
 */

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
}

class Buyer(auction: ActorRef) extends Actor {
  auction ! PlaceBid(Random.nextInt())

  override def receive = {
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
  private def initializeActorSystem() = {
    val asystem = ActorSystem("AuctioningSystem")
    asystem.actorOf(Props[AuctioningSystem])
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}