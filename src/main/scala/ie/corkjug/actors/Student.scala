package ie.corkjug.actors

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ie.corkjug.actors.Homework.{Subject, Aptitude}

object Student {
  trait StudentMessage
  case class Ready(id: Int) extends StudentMessage
  case class AptitudeReport(id: Int, aptitude: Aptitude) extends StudentMessage
  case class SpecializeIn(subject:Subject) extends StudentMessage
  case class DoHomework(subjects: Set[Subject]) extends StudentMessage
  case class HomeworkDone(id: Int) extends StudentMessage
  case class CopySubject(subject: Subject) extends StudentMessage
  case class SubjectComplete(id: Int, subject: Subject) extends StudentMessage

  def props(id: Int, aptitude: Aptitude, prefect: ActorRef) =
    Props(classOf[Student], id, aptitude, prefect)
}
class Student(id: Int, aptitude: Aptitude, prefect: ActorRef) extends Actor with ActorLogging {
  import ie.corkjug.actors.Student._

  prefect ! Ready(id)

  var subjectsToComplete: Set[Subject] = Set.empty
  var specialization: Option[Subject] = None

  // Student will need to keep track of the subjects they have either completed or copied.

  override def receive: Receive = {
    case SpecializeIn(subject: Subject) =>
      specialization = Some(subject)
    case DoHomework(subjects) =>
      log.debug(s"Student $id has received homework but is only going to do ${specialization.get}")
      subjectsToComplete = subjects
      doSpecialization(specialization.get)
    case CopySubject(_) if  subjectsToComplete.isEmpty =>
    case CopySubject(subject) if (subjectsToComplete - subject).isEmpty=>
      log.debug(s"Student $id just needs to copy $subject and then s/he's done")
      copySubject()
      log.debug(s"Student $id has completed the copy")
      prefect ! new HomeworkDone(id)
    case CopySubject(subject) =>
      copySubject()
      subjectsToComplete = subjectsToComplete - subject

  }

  private def doSpecialization(subject: Subject) = {
    val timeToCompleteSpecialize = aptitude.timeToComplete(subject)
    log.debug(s"Student $id will need $timeToCompleteSpecialize to complete $subject")
    Thread.sleep(timeToCompleteSpecialize)
    subjectsToComplete = subjectsToComplete - subject
    prefect ! SubjectComplete(id, subject)
  }

  private def copySubject() ={
    Thread.sleep(Homework.timeToCopyInMillis)
  }

}
