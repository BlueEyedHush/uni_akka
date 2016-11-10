package knawara.agh.reactive

import scala.concurrent.duration._

import akka.actor.{ActorRef, FSM}
import akka.actor.FSM.{->, Event}

/* public messages */
case class PlaceBid(val price: Long)
case class BidTooSmall()
case class Relist()

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

class AuctionActor extends FSM[State, AuctionData] {
  startWith(Created, AuctionData())

  when(Created) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(BidTimerExpired, _) => goto(Ignored)
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
    case Event(Relist(), _) => goto(Activated)
  }

  when(Activated) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(BidTimerExpired, _) => goto(Sold)
  }

  when(Sold) {
    case Event(DeleteTimerExpired, _) => stop(FSM.Normal)
  }

  val BID_TIMER_NAME = "BidTimer"
  onTransition {
    case _ -> Created => setTimer(BID_TIMER_NAME, BidTimerExpired, 5 seconds, repeat = false)
    case _ -> Ignored => {
      cancelTimer(BID_TIMER_NAME)
      println("set delete timer")
    }
    case _ -> Activated => println("validate bid, preventing transition if needed")
    case _ -> Sold => {
      cancelTimer(BID_TIMER_NAME)
      println("set delete timer & notify buyer")
    }
  }

  onTermination {
    case StopEvent(FSM.Normal, _, _) => println("delete timer expired, action cleaned up")
    case StopEvent(FSM.Shutdown, _, _) => println("WARN: someone shutdown this auction")
    case StopEvent(FSM.Failure(cause), _, _) => println(s"ERROR: auction failure, cause: ${cause}")
  }
}
