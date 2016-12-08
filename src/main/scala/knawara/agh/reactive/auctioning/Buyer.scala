package knawara.agh.reactive.auctioning

import akka.actor._
import akka.event.Logging
import scala.util.Random

package object Buyer {
  case object InitialTimeoutExpired

  sealed trait State
  case object WaitingForInitialTimeout extends State
  case object WaitingForAuctionList extends State
  case object Bidding extends State

  sealed trait Data
  case object Empty extends Data
  case class BiddingInProgressData(val auction: ActorRef) extends Data

  object Actor {
    def props(query: String, budget: Long): Props = Props(new Actor(query, budget))
  }

  class Actor(auctionQuery: String, budget: Long) extends FSM[State, Data] {
    import scala.concurrent.duration._
    setTimer("initial-timeout", InitialTimeoutExpired, 1 second)

    startWith(WaitingForInitialTimeout, Empty)

    when(WaitingForInitialTimeout) {
      case Event(InitialTimeoutExpired, _) =>
        val registryActorSelection = context.actorSelection("/user/system/registry")
        registryActorSelection ! AuctionSearch.Lookup(auctionQuery)
        goto(WaitingForAuctionList)
      case Event(msg, _) =>
        log.debug("[{}] got new message: {} while in {} state", self.path.name, msg.toString, "WaitingForInitialTimeout")
        stay
    }

    when(WaitingForAuctionList) {
      case Event(AuctionSearch.Result(_, results), _) =>
        val ho = results.headOption
        if(ho.isDefined) {
          log.debug("[{}] received Auctions ActorRef, bidding smallest possible amount", self.path.name)
          ho.get ! Auction.PlaceBid(1L)
          goto(Bidding) using BiddingInProgressData(ho.get)
        } else {
          log.debug("[{}] Query didn't return any auctions, shutting down buyer", self.path.name)
          stop(FSM.Normal)
        }
      case Event(msg, _) =>
        log.debug("[{}] got new message: {} while in {} state", self.path.name, msg.toString, "WaitingForAuctionList")
        stay
    }

    when(Bidding) {
      case Event(Auction.BidTooSmall(currentPrice), BiddingInProgressData(auctionRef)) =>
        bidIfPossible(currentPrice, auctionRef)
        stay
      case Event(Auction.Outbidden(currentPrice), BiddingInProgressData(auctionRef)) =>
        bidIfPossible(currentPrice, auctionRef)
        stay
      case Event(Auction.AlreadyEnded, _) =>
        log.debug("[{}] received auction-end information, stopping", self.path.name)
        stop(FSM.Normal)
      case Event(Auction.YouWon, _) =>
        log.debug("[{}] won", self.path.name)
        stop(FSM.Normal)
      case Event(msg, _) =>
        log.debug("[{}] got new message: {} while in {} state", self.path.name, msg.toString, "Bidding")
        stay
    }

    initialize()

    private def bidIfPossible(currentItemPrice: Long, auction: ActorRef) = {
      val newOffer = currentItemPrice + 1
      if(newOffer < budget) {
        log.debug("[{}] bid was too small, responding with {}", self.path.name, newOffer)
        auction ! Auction.PlaceBid(newOffer)
      } else {
        log.debug("[{}] bid was too small, budget exhausted", self.path.name)
        stop(FSM.Normal)
      }
    }
  }
}

