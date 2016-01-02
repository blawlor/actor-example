package ie.corkjug.actors

import akka.actor.{ActorLogging, Props, Actor}
import ie.corkjug.actors.Homework.{AssignmentResult, Assignment}
import ie.corkjug.actors.Prefect.ReadyForHomework
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Teacher {
  trait TeacherMessage
  case class HomeworkAssignment(assignment: Assignment) extends TeacherMessage
  case class HomeworkResults(assignmentResult: AssignmentResult) extends TeacherMessage
  def props(classSize: Int) = Props(classOf[Teacher], classSize)
}

class Teacher(classSize: Int) extends Actor with ActorLogging {
  import ie.corkjug.actors.Teacher._

  log.debug(s"Teacher has created a class of size $classSize")
  val schoolClass = context.actorOf(SchoolClass.props(classSize))
  var gaveHomeworkAt: Option[Long] = None

  override def receive = {
    case ReadyForHomework => giveHomework()
    case HomeworkResults(_) =>
      correctHomework(gaveHomeworkAt.get)
      giveBreathingSpace()
  }

  private def giveHomework() = {
    log.debug("Giving homework")
    gaveHomeworkAt = Some(System.currentTimeMillis())
    schoolClass ! new HomeworkAssignment(createAssignment)
  }

  private def correctHomework(startTime: Long) ={
    val timeTaken = System.currentTimeMillis() - startTime
    log.debug(s"It took $timeTaken milliseconds to complete the homework")
  }

  private def giveBreathingSpace() = {
    context.system.scheduler.scheduleOnce(5 seconds, self, ReadyForHomework)
  }

  // These assignments always have all subjects
  private def createAssignment: Assignment = new Assignment(Homework.subjects.toSet)
}
