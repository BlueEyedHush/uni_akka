package knawara.agh.reactive

import akka.actor.{Actor, Props, ActorRef}
import akka.event.Logging
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
  val log = Logging(context.system, this)
  val bidTimerCanceller = BuyerActor.scheduleBidTick(self)

  override def receive = {
    case BidTick() => auction ! PlaceBid(Random.nextInt(100000))
    case BidTooSmall => log.debug("[{}] bid was too small", self.path.name)
    case AuctionAlreadyEnded =>
      log.debug("[{}] received auction-end information, stopping", self.path.name)
      bidTimerCanceller.cancel()
      context.stop(self)
    case _ @ msg => log.debug("[{}] got new message: {}", self.path.name, msg.toString)
  }
}
