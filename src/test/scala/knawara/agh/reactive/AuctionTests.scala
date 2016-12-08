package knawara.agh.reactive

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestKit, TestFSMRef}
import org.scalatest.{FlatSpecLike, AsyncFlatSpec, Matchers}
import akka.pattern.{after => afterDelay}
import scala.concurrent.Future
import scala.concurrent.duration._

class AuctionTests extends AsyncFlatSpec with Matchers {
  // auction ends not before and not after specified time
  // auction is deleted after combined time
  // buy request is registered
  // buyer offer changes the price
  // smaller offer is rejected (price)
  // smaller offer is rejected (buyer)

  // handle pathological states - messages in invalid states

  // stability under random messages ordering

  implicit val actorSystem = ActorSystem("syncTestSystem")

  "AuctionActor" should "successfully initialize and end up in 'Created' state" in {
    assert(testActor().stateName.equals(Auction.Created))
  }

  it should "not end too early" in {
    val ta = testActor()
    afterDelay(4 seconds, actorSystem.scheduler)(Future(assert(ta.stateName.equals(Auction.Created))))
  }

  it should "end after specified time passes auction should end" in {
    val ta = testActor()
    afterDelay(6 seconds, actorSystem.scheduler)(Future(assert(ta.stateName.equals(Auction.Ignored))))
  }

  it should "register buyer correctly" in {
    val ta = testActor()
    val probe = TestProbe()

    probe.send(ta, Auction.PlaceBid(10L))
    val buyer = ta.stateData.asInstanceOf[Auction.ActiveData].buyer
    assert(buyer.equals(probe.ref))
  }

  it should "register buyer with correct price" in {
    val ta = testActor()
    ta ! Auction.PlaceBid(10L)
    val state = ta.stateData.asInstanceOf[Auction.ActiveData]
    assert(state.price.equals(10L))
  }

  private def testActor() = {
    TestFSMRef(new Auction.Actor(5 seconds, 5 seconds))
  }
}
