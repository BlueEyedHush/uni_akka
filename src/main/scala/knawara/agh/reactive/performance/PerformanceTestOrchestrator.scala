package knawara.agh.reactive.performance

import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import knawara.agh.reactive.performance.AuctionSearch.Registered

import scala.util.Random

class PerformanceTestOrchestrator extends Actor {
  val log = Logging(context.system, this)

  val masterSearch = context.actorOf(MasterSearch.props(), "mastersearch")

  val titlesNum = 5000
  val queriesNum = 100

  private[this] val titles = (0 until titlesNum).map(id => new AuctionTitle(s"$id"))
  val seller = context.actorOf(Seller.Actor.props(titles), "seller")

  override def receive = {
    case Seller.AllRegistered =>
      log.info("All auctions registered, starting buyer")
      val queries = (0 until queriesNum).map(id => Random.nextInt(titlesNum).toString)
      context.actorOf(Buyer.Actor.props(queries), "buyer")
    case Buyer.PerformanceResults(millis) =>
      println(s"$titlesNum titles, $queriesNum queries. Duration: $millis ms")
      context.system.terminate()
    case _ @ msg => log.debug(s"AuctionSystem got new message: ${msg.toString}")
  }
}

object PerformanceBootstrapper {
  def createActorSystem() = {
    val originalConfig = ConfigFactory.load()
    val overriddenConfig = originalConfig.getConfig("auctioningsys").withFallback(originalConfig)
    ActorSystem("auctioningsystem", Some(overriddenConfig)).actorOf(Props[PerformanceTestOrchestrator], "system")
  }

  def main(args: Array[String]): Unit = {
    createActorSystem()
  }
}