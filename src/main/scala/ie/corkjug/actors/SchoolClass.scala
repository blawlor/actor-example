package ie.corkjug.actors

import akka.actor.SupervisorStrategy.{Resume, Restart, Stop, Escalate}
import akka.actor.{OneForOneStrategy, ActorLogging, Props, Actor}
import ie.corkjug.actors.Homework.{Assignment, Aptitude}
import ie.corkjug.actors.Prefect.ReadyForHomework
import ie.corkjug.actors.Teacher.{HomeworkResults, HomeworkAssignment}
import scala.concurrent.duration._

object SchoolClass {

  def props(classSize: Int) = Props(classOf[SchoolClass], classSize)
}

class SchoolClass(classSize: Int) extends Actor with ActorLogging {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Student.MoralCrisis => Resume // Need to tell the prefect to send them the homework to copy
      case _: Exception                => Escalate
    }
  // Create a prefect and the rest of the class
  val prefect = context.actorOf(Prefect.props(1, Aptitude(), classSize).withDispatcher("student-dispatcher"))
  2 to classSize foreach {id =>
    context.actorOf(Student.props(id, Aptitude(), prefect).withDispatcher("student-dispatcher"))
  }

  override def receive: Receive = {
    case ReadyForHomework => context.parent ! ReadyForHomework
    case ha@HomeworkAssignment(assignment) => prefect ! ha
    case hr@HomeworkResults(_) =>
      log.debug("Class has finished its homework")
      context.parent ! hr
  }
}
