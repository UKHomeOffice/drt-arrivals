package uk.gov.homeoffice.cirium.actors

import java.lang.management.ManagementFactory

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.AskableActorRef
import akka.util.Timeout
import uk.gov.homeoffice.cirium.actors.CiriumFlightStatusRouterActor._
import uk.gov.homeoffice.cirium.actors.CiriumPortStatusActor.GetPortFeedHealthSummary
import uk.gov.homeoffice.cirium.services.entities.CiriumTrackableStatus

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Failure

object CiriumFlightStatusRouterActor {

  def props(portActors: Map[String, ActorRef]): Props = Props(classOf[CiriumFlightStatusRouterActor], portActors)

  case class GetFlightDeltas(flightId: Int)

  case object GetAllFlightDeltas

  case object GetReadiness

  case object GetHealth

  def upTimeSeconds: Long = ManagementFactory.getRuntimeMXBean.getUptime / 1000

}

case class CiriumFeedHealthStatus(
  isReady: Boolean,
  lastMessage: Option[CiriumTrackableStatus],
  upTime: Long)

class CiriumFlightStatusRouterActor(portActors: Map[String, ActorRef]) extends Actor with ActorLogging {

  var isReady: Boolean = false

  var lastMessage: Option[CiriumTrackableStatus] = None

  def receive: Receive = {

    case GetReadiness =>
      sender() ! isReady

    case GetHealth =>

      sender() ! CiriumFeedHealthStatus(isReady, lastMessage, upTimeSeconds)

    case ts: CiriumTrackableStatus =>

      if (!isReady && ts.isInSync()) {
        isReady = true
        log.info(s"Finished cirium backlog after ${upTimeSeconds} seconds.")
      }

      lastMessage = Option(ts)

      val portCodeForUpdate = ts.status.arrivalAirportFsCode
      portActors.get(portCodeForUpdate).foreach(_ ! ts)

    case Failure(e) =>
      log.error(s"Got an exception", e)
    case other =>
      log.error(s"Got this unexpected message ${other}")
  }

}
