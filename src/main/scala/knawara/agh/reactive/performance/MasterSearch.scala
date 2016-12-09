package knawara.agh.reactive.performance

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.{ScatterGatherFirstCompletedGroup, BroadcastGroup, RoundRobinGroup}
import scala.concurrent.duration._

import scala.concurrent.duration.Duration

object MasterSearch {
  def props(workersCount: Int = 3) = Props(new MasterSearch(workersCount))
}

class MasterSearch(workersCount: Int = 3) extends Actor with ActorLogging {
  val config = context.system.settings.config
  val useScatterGather = config.getBoolean("auctioningsys.search.scattergather")

  val workers = (1 to workersCount).map(id => context.actorOf(AuctionSearch.Actor.props()).path.toStringWithoutAddress)
  val searchGateway = if(!useScatterGather) {
    context.actorOf(RoundRobinGroup(workers).props(), "search")
  } else {
    context.actorOf(ScatterGatherFirstCompletedGroup(workers, 1 minute).props(), "search")
  }
  val registerGateway = context.actorOf(BroadcastGroup(workers).props(), "register")

  override def receive = {
    case msg: AuctionSearch.Register => registerGateway.forward(msg)
    case msg: AuctionSearch.Lookup => searchGateway.forward(msg)
    case msg => log.debug("Unrecognized message {}", msg)
  }
}
