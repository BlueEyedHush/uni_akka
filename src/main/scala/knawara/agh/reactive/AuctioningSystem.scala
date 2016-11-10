package knawara.agh.reactive

import akka.actor._

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
case class Relist()

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

object Auction {
  /* in-state messages */
  val BID_TIMER_EXPIRED = "bte"
  val DELETE_TIMER_EXPIRED = "dte"
}

/* states */
sealed trait AuctionState
case object AuctionCreated extends AuctionState
case object Ignored extends AuctionState
case object Activated extends AuctionState
case object Sold extends AuctionState
case object Dead extends AuctionState

/* data */
case class AuctionData(val price: Long = 0L, val buyer: Option[ActorRef] = None)

class Auction extends FSM[AuctionState, AuctionData] {
  startWith(AuctionCreated, AuctionData())

  when(AuctionCreated) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(Auction.BID_TIMER_EXPIRED, _) => goto(Ignored)
  }

  when(Ignored) {
    case Event(Auction.DELETE_TIMER_EXPIRED, _) => goto(Dead)
    case Event(Relist(), _) => goto(Activated)
  }

  when(Activated) {
    case Event(PlaceBid(_), _) => goto(Activated)
  }

  when(Sold) {
    case Event(Auction.BID_TIMER_EXPIRED, _) => goto(Sold)
  }

  onTransition {
    case _ -> AuctionCreated => println("setting bid timer")
    case _ -> Dead => println("cleanup auction actor")
    case _ -> Ignored => println("set delete timer")
    case _ -> Activated => println("validate bid, preventing transition if needed")
    case _ -> Sold => println("set delete timer & notify buyer")
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