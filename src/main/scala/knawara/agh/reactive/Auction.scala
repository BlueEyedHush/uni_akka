package knawara.agh.reactive

import akka.event.Logging

import scala.concurrent.duration._

import akka.actor.{Props, ActorRef, FSM}
import akka.actor.FSM.{->, Event}

package object Auction {
  /* public messages - in */
  case class PlaceBid(val price: Long)
  case object Relist
  /* public messages - out */
  case object YouWon
  case object BidTooSmall
  case object AlreadyEnded
  case class Sold(val auctionRef: ActorRef)
  case class Outbidden(val newPrice: Long)

  /* internal messages */
  case object BidTimerExpired
  case object DeleteTimerExpired

  /* states */
  sealed trait State
  case object Created extends State
  case object Ignored extends State
  case object Activated extends State
  case object Sold extends State

  /* data */
  sealed trait Data
  case object Empty extends Data
  case class ActiveData(val price: Long = 0L, val buyer: ActorRef = null) extends Data

  object Actor {
    val AUCTION_DELETE_TIME = 5 seconds

    def props(auctionDuration: FiniteDuration, notifyOnEnd: ActorRef) =
      Props(new Actor(auctionDuration, AUCTION_DELETE_TIME, notifyOnEnd))
  }

  class Actor(val auctionDuration: FiniteDuration,
              val auctionDeleteTime: FiniteDuration,
              val notifyOnEnd: ActorRef) extends FSM[State, Data] {
    startWith(Created, Empty)

    when(Created) {
      case Event(PlaceBid(offeredPrice), _) => handleLegalBid(offeredPrice, 0L, None)
      case Event(BidTimerExpired, _) => goto(Ignored)
    }

    when(Ignored) {
      case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
      case Event(Relist, _) => goto(Created)
      /* invalid */
      case Event(PlaceBid(_), _) => handlePostauctionBid()
    }

    when(Activated) {
      case Event(PlaceBid(offeredPrice), ActiveData(currentPrice, currentWinner)) =>
        handleLegalBid(offeredPrice, currentPrice, Some(currentWinner))
      case Event(BidTimerExpired, ActiveData(_, buyer)) => handleSold(buyer)
    }

    when(Sold) {
      case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
      case Event(PlaceBid(_), _) => handlePostauctionBid()
    }


    onTransition {
      case _ -> Created => setBidTimer()
      case _ -> Ignored =>
        cancelBidTimer()
        setDeleteTimer()
      case _ -> Sold =>
        cancelBidTimer()
        setDeleteTimer()
    }

    onTermination {
      case StopEvent(FSM.Normal, _, _) => println("delete timer expired, auction cleaned up")
      case StopEvent(FSM.Shutdown, _, _) => println("WARN: someone shutdown this auction")
      case StopEvent(FSM.Failure(cause), _, _) => println(s"ERROR: auction failure, cause: $cause")
    }

    initialize()

    private val BID_TIMER_NAME = "BidTimer"
    private def setBidTimer(): Unit = setTimer(BID_TIMER_NAME, BidTimerExpired, auctionDuration, repeat = false)
    private def cancelBidTimer(): Unit = cancelTimer(BID_TIMER_NAME)

    private val DELETE_TIMER_NAME = "DeleteTimer"
    private def setDeleteTimer(): Unit = setTimer(DELETE_TIMER_NAME, DeleteTimerExpired, auctionDeleteTime, repeat = false)
    private def cancelDeleteTimer(): Unit = cancelTimer(DELETE_TIMER_NAME)

    private def handlePostauctionBid() = {
      sender() ! AlreadyEnded
      stay()
    }

    private def handleLegalBid(bidPrice: Long, currentPrice: Long, currentWinner: Option[ActorRef]) = {
      if (bidPrice > currentPrice) {
        log.debug("[{}] received and accepted bid from [{}] for {}", self.path.name, sender().path.name, bidPrice)
        if(currentWinner.isDefined) currentWinner.get ! Outbidden(newPrice = bidPrice)
        goto(Activated) using ActiveData(price = bidPrice, buyer = sender())
      } else {
        log.debug("[{}] received but rejected bid from [{}] for {}", self.path.name, sender().path.name, bidPrice)
        sender() ! BidTooSmall
        stay
      }
    }

    private def handleSold(buyer: ActorRef) = {
      notifyOnEnd ! Sold(self)
      buyer ! YouWon
      goto(Sold)
    }
  }

}