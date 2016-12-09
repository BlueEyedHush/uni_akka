package knawara.agh.reactive

import knawara.agh.reactive.auctioning.AuctioningBootstrapper
import knawara.agh.reactive.publisher.PublisherBootstrapper

object Bootstrapper {
  def main(args: Array[String]): Unit = {
    PublisherBootstrapper.createActorSystem()
    AuctioningBootstrapper.createActorSystem()
  }
}
