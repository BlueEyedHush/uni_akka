package knawara.agh.reactive.auctioning

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestFSMRef}
import knawara.agh.reactive.auctioning.AuctionSearch
import org.scalatest.{FlatSpec, Matchers}

class AuctionSearchTests extends FlatSpec with Matchers {
  "AuctionSearch actor" should "return registered auction" in {
    val ta = testActor()
    val sender = TestProbe()
    val auction = TestProbe()

    ta ! AuctionSearch.Register(new AuctionTitle("auction"), auction.ref)
    sender.send(ta, AuctionSearch.Lookup("auction"))
    sender.expectMsgPF(){
      case AuctionSearch.Result(_, auctionList) =>
        assert(auctionList.head.equals(auction.ref))
        assert(auctionList.size == 1)
    }
  }

  it should "corrrectly repsond to partial query" in {
    val ta = testActor()
    val sender = TestProbe()
    val auction = TestProbe()

    ta ! AuctionSearch.Register(new AuctionTitle("auction"), auction.ref)
    sender.send(ta, AuctionSearch.Lookup("uct"))
    sender.expectMsgPF(){
      case AuctionSearch.Result(_, auctionList) =>
        assert(auctionList.head.equals(auction.ref))
        assert(auctionList.size == 1)
    }
  }

  it should "return nothing when querying non-existent auction" in {
    val ta = testActor()
    val sender = TestProbe()
    val auction = TestProbe()

    ta ! AuctionSearch.Register(new AuctionTitle("auction"), auction.ref)
    sender.send(ta, AuctionSearch.Lookup("notpresent"))
    sender.expectMsgPF(){
      case AuctionSearch.Result(_, auctionList) =>
        assert(auctionList.isEmpty)
    }
  }

  implicit val system = ActorSystem("test")

  private def testActor() = {
    TestActorRef(new AuctionSearch.Actor)
  }
}
