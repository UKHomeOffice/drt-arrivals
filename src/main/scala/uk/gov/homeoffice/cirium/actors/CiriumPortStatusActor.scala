package uk.gov.homeoffice.cirium.actors

import akka.actor.{ Actor, ActorLogging, Props }
import org.joda.time.DateTime
import uk.gov.homeoffice.cirium.services.entities.CiriumFlightStatus

import scala.collection.mutable

object CiriumPortStatusActor {

  final case object GetStatuses

  final case object RemoveExpired

  def props(
    hoursOfHistory: Int = 24,
    currentTimeMillisFunc: () => Long = () => new DateTime().getMillis): Props = Props(classOf[CiriumPortStatusActor], hoursOfHistory, currentTimeMillisFunc)
}

class CiriumPortStatusActor(
  hoursOfHistory: Int,
  nowMillis: () => Long) extends Actor with ActorLogging {

  import CiriumPortStatusActor._

  val statuses: mutable.Map[Int, CiriumFlightStatus] = mutable.Map[Int, CiriumFlightStatus]()

  val expireAfterMillis: Long = hoursOfHistory * 60 * 60 * 1000

  def receive: Receive = {
    case GetStatuses =>
      val replyTo = sender()
      log.info(s"Sending ${statuses.size} flight statuses")
      replyTo ! statuses.values.toList

    case RemoveExpired =>
      val forRemoval = statuses.collect {
        case (key, status) if status.arrivalDate.millis < (nowMillis() - expireAfterMillis) =>
          key
      }

      log.info(s"Removing ${statuses.size} expired flight statuses")

      statuses --= forRemoval

    case s: CiriumFlightStatus =>
      statuses(s.flightId) = s

    case other =>
      log.error(s"Got this unexpected message ${other}")
  }
}
