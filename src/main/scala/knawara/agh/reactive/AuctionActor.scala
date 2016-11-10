package knawara.agh.reactive

import akka.actor.{ActorRef, FSM}
import akka.actor.FSM.{->, Event}

case class PlaceBid(val price: Long)
case class BidTooSmall()
case class Relist()

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

class AuctionActor extends FSM[AuctionState, AuctionData] {
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
