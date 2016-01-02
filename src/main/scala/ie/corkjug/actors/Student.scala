package ie.corkjug.actors

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ie.corkjug.actors.Homework.{Subject, Aptitude}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Student {
  trait StudentMessage
  case class Ready(id: Int) extends StudentMessage
  case class AptitudeReport(id: Int, aptitude: Aptitude) extends StudentMessage
  case class SpecializeIn(subject:Subject) extends StudentMessage
  case class DoHomework(subjects: Set[Subject]) extends StudentMessage
  case class HomeworkDone(id: Int) extends StudentMessage
  case class CopySubject(subject: Subject) extends StudentMessage
  case class CopyComplete(subject: Subject) extends StudentMessage
  case class SubjectComplete(id: Int, subject: Subject) extends StudentMessage
  case class SpecializationComplete(subject: Subject) extends StudentMessage
  def props(id: Int, aptitude: Aptitude, prefect: ActorRef) =
    Props(classOf[Student], id, aptitude, prefect)
}
class Student(id: Int, aptitude: Aptitude, prefect: ActorRef) extends Actor with ActorLogging {
  import ie.corkjug.actors.Student._

  prefect ! Ready(id)

  var subjectsToComplete: Set[Subject] = Set.empty
  var specialization: Option[Subject] = None
  var subjectsToCopy: Set[Subject] = Set.empty

  // Student will need to keep track of the subjects they have either completed or copied.

  override def receive = ready

  def ready:Receive = {
    case SpecializeIn(subject: Subject) =>
      specialization = Some(subject)
    case DoHomework(subjects) =>
      log.debug(s"Student $id has received homework but is only going to do ${specialization.get}")
      subjectsToComplete = subjects
      doSpecialization(specialization.get)
  }

  private def waitingForSpecialization(specializationSubject: Subject):Receive = {
    case SpecializationComplete(subject) =>
      subjectsToComplete = subjectsToComplete - subject
      prefect ! SubjectComplete(id, subject)
      if (subjectsToComplete isEmpty) {
        reset()
        prefect ! HomeworkDone(id)
      } else {
        copySubjects(subjectsToCopy)
      }
    case CopySubject(`specializationSubject`) =>
      log.debug(s"Student $id was given a copy of their specialization $specializationSubject before they finished.")
      subjectsToComplete = subjectsToComplete - specializationSubject
      if (subjectsToComplete isEmpty){
        reset()
        prefect ! HomeworkDone(id)
      } else {
        copySubjects(subjectsToCopy)
      }
    case CopySubject(subjectToCopy) =>
      subjectsToCopy = subjectsToCopy + subjectToCopy
  }

  private def readyToCopy(subjectsToCopy: Set[Subject]): Receive = {
    case CopySubject(subjectToCopy) =>
      copySubjects(subjectsToCopy + subjectToCopy)
  }

  private def waitingForCopy(subjectsToCopy: Set[Subject]):Receive = {
    case CopyComplete(subject) if (subjectsToComplete - subject).isEmpty=>
      reset()
      prefect ! HomeworkDone(id)
    case CopyComplete(subject) if subjectsToCopy.isEmpty =>
      subjectsToComplete = subjectsToComplete - subject
      context.become(readyToCopy(subjectsToCopy))
    case CopyComplete(subject) =>
      subjectsToComplete = subjectsToComplete - subject
      copySubjects(subjectsToCopy)
    case CopySubject(subjectToCopy) =>
      context.become(waitingForCopy(subjectsToCopy + subjectToCopy))
  }

  private def doSpecialization(subject: Subject) = {
    context.become(waitingForSpecialization(subject))
    val timeToCompleteSpecialize = aptitude.timeToComplete(subject)
    log.debug(s"Student $id will need $timeToCompleteSpecialize to complete $subject")
    context.system.scheduler.scheduleOnce(timeToCompleteSpecialize milliseconds, self, new SpecializationComplete(subject))
  }

  private def copySubjects(subjects: Set[Subject]) ={
    if (subjects.nonEmpty){
      log.debug(s"Student $id is copying ${subjects.head}")
      context.system.scheduler.scheduleOnce(Homework.timeToCopyInMillis milliseconds, self, new CopyComplete(subjects.head))
      context.become(waitingForCopy(subjects.tail))
    } else {
      log.debug(s"Student $id is ready for copy commands")
      context.become(readyToCopy(subjects))
    }
  }

  private def reset() = {
    subjectsToComplete = Set.empty
    subjectsToCopy = Set.empty
    context.become(ready)
  }

}
