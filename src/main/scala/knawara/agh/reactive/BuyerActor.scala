package knawara.agh.reactive

import akka.actor.{Actor, Props, ActorRef}
import scala.util.Random

case class BidTick()

object BuyerActor {
  def props(auction: ActorRef): Props = Props(new BuyerActor(auction))

  private def scheduleBidTick(target: ActorRef) = {
    import Bootstrapper.asystem.dispatcher
    import scala.concurrent.duration._

    Bootstrapper.asystem.scheduler.schedule(1 second, 500 millis , target, BidTick())
  }
}

class BuyerActor(auction: ActorRef) extends Actor {
  BuyerActor.scheduleBidTick(self)

  override def receive = {
    case BidTick() => auction ! PlaceBid(Random.nextInt(100000))
    case BidTooSmall => println("Bid was too small")
    case _ @ msg => println(s"Buyer got new message: ${msg.toString}")
  }
}
