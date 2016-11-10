package knawara.agh.reactive

import akka.actor._
import akka.event.Logging
import scala.concurrent.duration._

class AuctioningSystem extends Actor {
  val log = Logging(context.system, this)

  val auction = context.actorOf(AuctionActor.props(5 seconds), "auction")
  Set(1,2,3).foreach(id => context.actorOf(BuyerActor.props(auction), s"buyer-$id"))

  override def receive = {
    case _ @ msg => log.debug(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object Bootstrapper {
  val asystem = ActorSystem("AuctioningSystem")

  private def initializeActorSystem() = {
    asystem.actorOf(Props[AuctioningSystem])
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}