package knawara.agh.reactive.performance

import akka.actor.Actor.Receive
import akka.actor._
import akka.actor.{Actor => AkkaActor}
package object Buyer {
  case class PerformanceResults(val timeMillis: Long)

  object Actor {
    def props(auctionQueries: Set[String]): Props = Props(new Actor(auctionQueries))
  }

  class Actor(auctionQueries: Set[String]) extends AkkaActor with ActorLogging {
    val registryActorSelection = context.actorSelection("/user/system/mastersearch")

    val startTime = System.currentTimeMillis()
    auctionQueries.foreach(query => {
      registryActorSelection ! AuctionSearch.Lookup(query)
    })

    var responsesCount = 0

    override def receive = {
      case AuctionSearch.Result =>
        responsesCount += 1
        if(responsesCount == auctionQueries.size) {
          val duration = System.currentTimeMillis() - startTime
          context.parent ! PerformanceResults(duration)
          context.stop(self)
        }
      case msg => log.debug("Unknown message received: {}", msg)
    }
  }
}

