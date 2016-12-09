package knawara.agh.reactive.auctioning

import akka.actor.SupervisorStrategy.Restart
import akka.event.Logging
import akka.util.Timeout
import knawara.agh.reactive.auctioning.NotifierRequest.SendFailed

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor._
import knawara.agh.reactive.auctioning.Notifier.{Notify, ResolveFailure, ResolveSuccess}
import knawara.agh.reactive.publisher.AuctionPublisher.Publish

object Notifier {
  case class Notify(val auctionTitle: String,
                    val buyer: ActorRef,
                    val price: Long)

  case object ResolveSuccess
  case object ResolveFailure

  def props() = Props(new Notifier)
}

class Notifier extends Actor with ActorLogging {
  var publisher: ActorRef = null

  import scala.concurrent.ExecutionContext.Implicits.global
  context.actorSelection("akka.tcp://publishersystem@127.0.0.1:2553/user/publisher").resolveOne(30 seconds).onComplete {
    case Success(actor) =>
      publisher = actor
      self ! ResolveSuccess

    case Failure(ex) =>
      log.error(ex, "Trying to resolve publisher failed")
      self ! ResolveFailure
  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10){
    case SendFailed(ex) =>
      log.error(ex, "sending notification to publisher failed, restarting")
      Restart
  }

  override def receive = {
    case ResolveSuccess => context.become(receiveResolved, true)
    case ResolveFailure => context.stop(self)
    // add buffer
  }

  def receiveResolved: Receive  = {
    case notification: Notify => context.actorOf(NotifierRequest.props(notification, publisher))
  }
}

object NotifierRequest {
  case class SendFailed(ex: Throwable) extends Throwable

  def props(request: Notify, publisher: ActorRef) = Props(new NotifierRequest(request, publisher))
}

class NotifierRequest(request: Notify, publisher: ActorRef) extends Actor {
  import akka.pattern.ask

  val message = s"[${request.auctionTitle}] ${request.buyer.path.name} is winning with price ${request.price}"

  import context.dispatcher
  implicit val timeout = Timeout(20 seconds)
  (publisher ? Publish(message)).onComplete {
    case Success(_) => context.stop(self)
    case Failure(ex) => self ! SendFailed(ex)
  }

  override def receive = {
    case ex: SendFailed => throw ex
  }
}
