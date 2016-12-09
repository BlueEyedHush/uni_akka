package knawara.agh.reactive.auctioning

import akka.event.Logging

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.{Props, ActorRef, Actor}
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

  override def receive = {
    case ResolveSuccess => context.become(receiveResolved, true)
    case ResolveFailure => context.stop(self)
    // add buffer
  }

  def receiveResolved: Receive  = {
    case Notify(title, buyer, price) =>
      publisher ! Publish(s"[$title] ${buyer.path.name} is winning with price $price")
  }
}
