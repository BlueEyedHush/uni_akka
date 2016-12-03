package knawara.agh.reactive

import akka.actor._
import akka.event.Logging
import scala.util.Random

package object Buyer {
  case object BidTick

  sealed trait State
  case object WaitingForAuctionList extends State
  case object Bidding extends State

  sealed trait Data
  case object Empty extends Data
  case class BiddingInProgressData(val auction: ActorRef, val bidTimerCanceller: Cancellable) extends Data

  object Actor {
    def props(query: String): Props = Props(new Actor(query))

    private def scheduleBidTick(target: ActorRef) = {
      import Bootstrapper.asystem.dispatcher
      import scala.concurrent.duration._

      Bootstrapper.asystem.scheduler.schedule(1 second, 500 millis , target, BidTick)
    }
  }

  class Actor(auctionQuery: String) extends FSM[State, Data] {
    val registryActorSelection = context.actorSelection("/user/system/registry")
    registryActorSelection ! LookupAuction(auctionQuery)

    startWith(WaitingForAuctionList, Empty)

    when(WaitingForAuctionList) {
      case Event(LookupResult(_, results), _) =>
        val ho = results.headOption
        if(ho.isDefined) {
          log.debug("[{}] received Auctions ActorRef, starting bidding", self.path.name)
          val bidTimerCanceller = Actor.scheduleBidTick(self)
          goto(Bidding) using BiddingInProgressData(ho.get, bidTimerCanceller)
        } else {
          log.debug("[{}] Query didn't return any auctions, shutting down buyer", self.path.name)
          stop(FSM.Normal)
        }
      case Event(msg, _) =>
        log.debug("[{}] got new message: {} while in {} state", self.path.name, msg.toString, "Bidding")
        stay
    }

    when(Bidding) {
      case Event(BidTick, BiddingInProgressData(auctionRef, _)) =>
        auctionRef ! Auction.PlaceBid(Random.nextInt(100000))
        stay
      case Event(Auction.BidTooSmall, _) =>
        log.debug("[{}] bid was too small", self.path.name)
        stay
      case Event(Auction.AlreadyEnded, BiddingInProgressData(_, canceller)) =>
        log.debug("[{}] received auction-end information, stopping", self.path.name)
        canceller.cancel()
        stop(FSM.Normal)
        stay
      case Event(msg, _) =>
        log.debug("[{}] got new message: {} while in {} state", self.path.name, msg.toString, "Bidding")
        stay
    }

    initialize()
  }
}

