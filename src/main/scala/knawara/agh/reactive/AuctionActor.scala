package knawara.agh.reactive

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
case object Dead extends State

/* data */
case class AuctionData(val price: Long = 0L, val buyer: Option[ActorRef] = None)

class AuctionActor extends FSM[State, AuctionData] {
  startWith(Created, AuctionData())

  when(Created) {
    case Event(PlaceBid(_), _) => goto(Activated)
    case Event(BidTimerExpired, _) => goto(Ignored)
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, _) => goto(Dead)
    case Event(Relist(), _) => goto(Activated)
  }

  when(Activated) {
    case Event(PlaceBid(_), _) => goto(Activated)
  }

  when(Sold) {
    case Event(BidTimerExpired, _) => goto(Sold)
  }

  onTransition {
    case _ -> Created => println("setting bid timer")
    case _ -> Dead => println("cleanup auction actor")
    case _ -> Ignored => println("set delete timer")
    case _ -> Activated => println("validate bid, preventing transition if needed")
    case _ -> Sold => println("set delete timer & notify buyer")
  }
}