package knawara.agh.reactive

import akka.actor._
import scala.concurrent.duration._

class AuctioningSystem extends Actor {
  val auction = context.actorOf(AuctionActor.props(5 seconds))
  val buyer = context.actorOf(BuyerActor.props(auction))

  override def receive = {
    case _ @ msg => println(s"AuctionSystem got new message: ${msg.toString}")
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