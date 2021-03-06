package ie.corkjug.actors

import akka.actor._
import ie.corkjug.actors.Homework.{AssignmentResult, Subject, Aptitude}
import ie.corkjug.actors.Student._
import ie.corkjug.actors.Teacher.{HomeworkResults, HomeworkAssignment}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Prefect {
  case object ReadyForHomework
  case object HomeworkTimeout
  def props(id: Int, aptitude: Aptitude, classSize: Int) =
    Props(classOf[Prefect], id, aptitude,  classSize)
}

class Prefect(id: Int, aptitude: Aptitude, classSize: Int) extends Actor with ActorLogging {
  import ie.corkjug.actors.Prefect._

  var expecting = classSize -1
  var students: Map[Int, ActorRef] = Map.empty
  var waitingOnStudents: Set[Int] = Set.empty
  var completedSubjects: Set[Subject] = Set.empty

  override def receive = {
    case Ready(studentId) if expecting > 1 =>
      log.debug(s"Student $studentId is ready")
      students = students + (studentId -> sender())
      expecting = expecting - 1
    case Ready(studentId) if expecting == 1 =>
      log.debug(s"Student $studentId is ready")
      students = students + (studentId -> sender())
      expecting = 0
      assignSpecializations()
      log.debug(s"Class is ready")
      context.parent ! ReadyForHomework
    case HomeworkAssignment(assignment) =>
      log.debug(s"Prefect has received homework to distribute")
      waitingOnStudents = students.keysIterator.toSet
      broadcastHomework(assignment.subjects)
      val timeout = context.system.scheduler.scheduleOnce(35 seconds, self, HomeworkTimeout)
      context.become(waitingOnCompletedHomework(timeout))
  }

  def waitingOnCompletedHomework(timeout: Cancellable): Receive = {
    case SubjectComplete(studentId, subject) if completedSubjects contains subject  =>
    // Nothing to do
    case SubjectComplete(studentId, subject) =>
      log.debug(s"Student $studentId has completed $subject")
      completedSubjects = completedSubjects + subject
      broadcastSubjectResult(subject)
    case HomeworkDone(studentId) if (waitingOnStudents - studentId).isEmpty =>
      log.debug(s"Prefect receives completed homework from $studentId and is done")
      reset()
      timeout.cancel()
      context.parent ! new HomeworkResults(new AssignmentResult(completedSubjects))
    case HomeworkDone(studentId) =>
      log.debug(s"Prefect receives completed homework from $studentId")
      waitingOnStudents = waitingOnStudents - studentId
    case HomeworkTimeout =>
      log.warning("Prefect returns incomplete homework after timeout")
      reset()
      context.parent ! new HomeworkResults(new AssignmentResult(completedSubjects))
  }

  private def assignSpecializations() = {
    var i = 0
    students.values.toList.foreach { s =>
      s ! SpecializeIn(Homework.subjectFor(i))
      i = i+1
    }
  }

  def broadcastHomework(subjects: Set[Subject]) = {
    students.valuesIterator.foreach{ s =>
      s ! new DoHomework(subjects)
    }
  }

  def broadcastSubjectResult(subject: Subject): Unit ={
    students.valuesIterator.foreach{ s =>
      s ! new CopySubject(subject)
    }
  }

  private def reset() = {
    completedSubjects = Set.empty
    context.become(receive)
  }

}
