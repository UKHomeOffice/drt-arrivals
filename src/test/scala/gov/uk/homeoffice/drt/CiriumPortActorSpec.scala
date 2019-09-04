package gov.uk.homeoffice.drt

import akka.actor.ActorSystem
import akka.pattern.AskableActorRef
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import gov.uk.homeoffice.drt.actors.CiriumPortStatusActor
import gov.uk.homeoffice.drt.actors.CiriumPortStatusActor.{ GetStatuses, RemoveExpired }
import gov.uk.homeoffice.drt.services.entities._
import org.joda.time.DateTime
import org.specs2.mutable.SpecificationLike

import scala.concurrent.Await
import scala.concurrent.duration._

class CiriumPortActorSpec extends TestKit(ActorSystem("testActorSystem", ConfigFactory.empty())) with SpecificationLike {
  sequential
  isolated

  "Flight statuses be deleted after a given period" >> {
    val portStatusActor = system.actorOf(
      CiriumPortStatusActor.props(1, () => DateTime.parse("2019-09-04T11:51:00.000Z").getMillis),
      "test-status-actor")
    implicit lazy val timeout: Timeout = 3.seconds
    val askablePortStatusActor: AskableActorRef = portStatusActor

    val statusToExpire = MockFlightStatus(1, "2019-09-04T10:50:59.000Z")
    val statusToKeep = MockFlightStatus(2, "2019-09-04T11:51:01.000Z")

    portStatusActor ! statusToExpire
    portStatusActor ! statusToKeep

    portStatusActor ! RemoveExpired

    val result = Await.result(askablePortStatusActor ? GetStatuses, 1 second)
    val expected = List(statusToKeep)

    result === expected
  }
}

object MockFlightStatus {

  def apply(id: Int, scheduledDate: String) = CiriumFlightStatus(
    id,
    "TST",
    "TST",
    "TST",
    "1000",
    "TST",
    "LHR",
    CiriumDate(scheduledDate, None),
    CiriumDate(scheduledDate, None),
    "A",
    CiriumOperationalTimes(
      Some(CiriumDate("2019-07-15T09:10:00.000Z", Option("2019-07-15T10:10:00.000"))),
      Some(CiriumDate("2019-07-15T09:10:00.000Z", Option("2019-07-15T10:10:00.000"))),
      Some(CiriumDate("2019-07-15T09:37:00.000Z", Option("2019-07-15T10:37:00.000"))),
      Some(CiriumDate("2019-07-15T09:37:00.000Z", Option("2019-07-15T10:37:00.000"))),
      Some(CiriumDate("2019-07-15T11:05:00.000Z", Option("2019-07-15T13:05:00.000"))),
      Some(CiriumDate("2019-07-15T11:05:00.000Z", Option("2019-07-15T13:05:00.000")))),
    List(CiriumCodeshare("CZ", "1000", "L"), CiriumCodeshare("DL", "2000", "L")),
    Some(CiriumAirportResources(None, None, Some("A"), None, None)),
    Seq())
}

