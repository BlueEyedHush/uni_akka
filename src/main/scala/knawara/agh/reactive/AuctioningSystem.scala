package knawara.agh.reactive

import akka.actor._

import scala.concurrent.duration.FiniteDuration
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
case object Relist
case object YouWon
case object AuctionAlreadyEnded

object Buyer {
  def props(auction: ActorRef): Props = Props(new Buyer(auction))

  private def scheduleBidTick(target: ActorRef): Cancellable = {
    import Bootstrapper.asystem.dispatcher
    import scala.concurrent.duration._

    Bootstrapper.asystem.scheduler.schedule(1 second, 500 millis , target, BidTick())
  }
}

class Buyer(auction: ActorRef) extends Actor {
  val bidTickCanceller = Buyer.scheduleBidTick(self)

  override def receive = {
    case BidTick() => auction ! PlaceBid(Random.nextInt(100000))
    case BidTooSmall() => println("Bid was too small")
    case YouWon =>
      println("I won!")
      stop()
    case AuctionAlreadyEnded =>
      println("Auction already ended, stopping this buyer")
      stop()
    case _ @ msg => println(s"Buyer got new message: ${msg.toString}")
  }

  private def stop() = {
    bidTickCanceller.cancel()
    context.stop(self)
  }
}

case object AuctionEnded
case object DeleteAuction

object Auction {
  private val DELETE_TIMEOUT = 5

  private def startTimer(delay: Int, target: ActorRef, message: Any) = {
    import Bootstrapper.asystem.dispatcher
    import scala.concurrent.duration._

    Bootstrapper.asystem.scheduler.scheduleOnce(delay seconds, target, message)
  }

  private def startBidTimer(auctionDuration: Int, target: ActorRef): Cancellable =
    startTimer(auctionDuration, target, AuctionEnded)

  private def startDeleteTimer(target: ActorRef): Cancellable =
    startTimer(DELETE_TIMEOUT, target, DeleteAuction)
}

class Auction extends Actor {
  val AUCTION_DURATION = 5

  var price = 0L
  var highestBidder: Option[ActorRef] = None
  var bidTimerCanceller: Cancellable = null
  var deleteTimerCanceller: Cancellable = null

  bidTimerCanceller = Auction.startBidTimer(AUCTION_DURATION, self)

  override def receive: Actor.Receive = receiveWhenActivated

  def receiveWhenCreated: Actor.Receive = {
    case PlaceBid(offeredPrice) =>
      price = offeredPrice
      highestBidder = Some(sender())
      context.become(receiveWhenActivated, discardOld = true)
    case AuctionEnded =>
      println("Auction ended without winner")
      bidTimerCanceller = null
      deleteTimerCanceller = Auction.startDeleteTimer(self)
      context.become(receiveWhenIgnored, discardOld = true)
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }

  def receiveWhenActivated: Actor.Receive = {
    case PlaceBid(offeredPrice) =>
      println(s"Bid received: $offeredPrice")
      if(offeredPrice > price) {
        price = offeredPrice
        highestBidder = Some(sender())
      }
      else sender() ! BidTooSmall()
    case AuctionEnded =>
      println(s"Auction ended, highest price: $price, winner: ${highestBidder.get.path.name}")
      bidTimerCanceller = null
      highestBidder.get ! YouWon
      deleteTimerCanceller = Auction.startDeleteTimer(self)
      context.become(receiveWhenSold, discardOld = true)
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }

  def receiveWhenIgnored: Actor.Receive = {
    case DeleteAuction =>
      println("Deleting auciton")
      context.stop(self)
    case Relist =>
      deleteTimerCanceller.cancel()
      deleteTimerCanceller = null
      bidTimerCanceller = Auction.startBidTimer(AUCTION_DURATION, self)
      context.become(receiveWhenCreated, discardOld = true)
    case PlaceBid(_) => sender() ! AuctionAlreadyEnded
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }

  def receiveWhenSold: Actor.Receive = {
    case DeleteAuction => {
      println("Deleting auction")
      context.stop(self)
    }
    case PlaceBid(_) => sender() ! AuctionAlreadyEnded
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }
}

class AuctioningSystem extends Actor {
  val auction = context.actorOf(Props[Auction], "auction")
  val buyer = context.actorOf(Buyer.props(auction), "buyer")

  override def receive = {
    case _ @ msg => println(s"AuctionSystem got new message: ${msg.toString}")
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