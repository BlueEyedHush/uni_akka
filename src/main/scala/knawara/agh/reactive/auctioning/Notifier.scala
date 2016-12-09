package knawara.agh.reactive.auctioning

import akka.actor.SupervisorStrategy.Restart
import akka.event.Logging

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.{OneForOneStrategy, Actor, Props, ActorRef}
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

class Notifier extends Actor {
  val log = Logging(context.system, this)
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

  override val supervisorStrategy = OneForOneStrategy(){
    case _ => Restart
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
  def props(request: Notify, publisher: ActorRef) = Props(new NotifierRequest(request, publisher))
}

class NotifierRequest(request: Notify, publisher: ActorRef) extends Actor {
  val message = s"[${request.auctionTitle}] ${request.buyer.path.name} is winning with price ${request.price}"
  publisher ! Publish(message)
  context.stop(self)

  override def receive = {
    case _ => println("message")
  }
}
