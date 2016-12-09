package knawara.agh.reactive.auctioning

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.{BroadcastGroup, RoundRobinGroup}

object MasterSearch {
  def props(workersCount: Int = 3) = Props(new MasterSearch(workersCount))
}

class MasterSearch(workersCount: Int = 3) extends Actor with ActorLogging {
  val workers = (1 to workersCount).map(id => context.actorOf(AuctionSearch.Actor.props()).path.toStringWithoutAddress)
  val searchGateway = context.actorOf(RoundRobinGroup(workers).props(), "search")
  val registerGateway = context.actorOf(BroadcastGroup(workers).props(), "register")

  override def receive = {
    case msg: AuctionSearch.Register => registerGateway.forward(msg)
    case msg: AuctionSearch.Lookup => searchGateway.forward(msg)
    case msg => log.debug("Unrecognized message {}", msg)
  }
}
