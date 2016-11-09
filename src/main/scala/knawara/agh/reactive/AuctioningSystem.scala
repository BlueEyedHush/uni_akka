package knawara.agh.reactive

import akka.actor.{ActorRef, Props, ActorSystem, Actor}

class AuctioningSystem extends Actor {
  val auction = context.actorOf(Props[Auction])
  val buyer = context.actorOf(Buyer.props(auction))

  override def receive = {
    case _ @ msg => println(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object Buyer {
  def props(auction: ActorRef): Props = Props(new Buyer(auction))
}

class Buyer(auction: ActorRef) extends Actor {
  auction ! "hello world"

  override def receive = {
    case _ @ msg => println(s"Buyer got new message: ${msg.toString}")
  }
}

class Auction extends Actor {
  override def receive = {
    case _ @ msg => println(s"Auction got new message: ${msg.toString}")
  }
}

object Bootstrapper {
  private def initializeActorSystem() = {
    val asystem = ActorSystem("AuctioningSystem")
    asystem.actorOf(Props[AuctioningSystem])
  }

  def main(args: Array[String]): Unit = {
    initializeActorSystem()
  }
}