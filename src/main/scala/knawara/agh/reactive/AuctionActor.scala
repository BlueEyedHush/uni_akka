package knawara.agh.reactive

import scala.concurrent.duration._

import akka.actor.{Props, ActorRef, FSM}
import akka.actor.FSM.{->, Event}

/* public messages - in */
case class PlaceBid(val price: Long)
case object Relist
/* public messages - out */
case object BidTooSmall
case object AuctionAlreadyEnded

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
case class AuctionData(val price: Long = 0L, val buyer: Option[ActorRef] = None)

object AuctionActor {
  val AUCTION_DELETE_TIME = 5 seconds

  def props(auctionDuration: FiniteDuration) = Props(new AuctionActor(auctionDuration, AUCTION_DELETE_TIME))
}

class AuctionActor(val auctionDuration: FiniteDuration,
                   val auctionDeleteTime: FiniteDuration) extends FSM[State, AuctionData] {
  startWith(Created, AuctionData())

  when(Created) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(BidTimerExpired, _) => goto(Ignored)
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
    case Event(Relist, _) => goto(Activated)
    /* invalid */
    case Event(PlaceBid(_), _) => handlePostauctionBid()
  }

  when(Activated) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(BidTimerExpired, _) => goto(Sold)
  }

  when(Sold) {
    case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
    /* invalid */
    case Event(PlaceBid(_), _) => handlePostauctionBid()
  }


  onTransition {
    case _ -> Created => setBidTimer()
    case _ -> Ignored =>
      cancelBidTimer()
      setDeleteTimer()
    case _ -> Activated => println("validate bid, preventing transition if needed")
    case _ -> Sold =>
      cancelBidTimer()
      setDeleteTimer()
      println("notify buyer")
  }

  onTermination {
    case StopEvent(FSM.Normal, _, _) => println("delete timer expired, auction cleaned up")
    case StopEvent(FSM.Shutdown, _, _) => println("WARN: someone shutdown this auction")
    case StopEvent(FSM.Failure(cause), _, _) => println(s"ERROR: auction failure, cause: ${cause}")
  }

  initialize()

  private val BID_TIMER_NAME = "BidTimer"
  private def setBidTimer(): Unit = setTimer(BID_TIMER_NAME, BidTimerExpired, auctionDuration, repeat = false)
  private def cancelBidTimer(): Unit = cancelTimer(BID_TIMER_NAME)

  private val DELETE_TIMER_NAME = "DeleteTimer"
  private def setDeleteTimer(): Unit = setTimer(DELETE_TIMER_NAME, DeleteTimerExpired, auctionDeleteTime, repeat = false)
  private def cancelDeleteTimer(): Unit = cancelTimer(DELETE_TIMER_NAME)

  private def handlePostauctionBid() = {
    sender() ! AuctionAlreadyEnded
    stay()
  }
}
