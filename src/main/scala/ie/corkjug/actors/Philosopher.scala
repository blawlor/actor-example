package ie.corkjug.actors

import akka.actor.{Props, ActorLogging, Actor}
import ie.corkjug.actors.Homework.{Philosophy}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random


object Philosopher {
  trait PhilosopherMessage
  case object CopyPhilosophyHomework extends PhilosopherMessage
  case object CopyComplete extends PhilosopherMessage
  val random: Random = Random
  class MoralCrisis(message: String = null, cause: Throwable = null) extends Throwable(message, cause)

  def props(studentId: Int) = Props(classOf[Philosopher], studentId)
}

class Philosopher(studentId: Int) extends Actor with ActorLogging {
  import Philosopher._

  override def receive: Receive = {
    case CopyPhilosophyHomework =>
      considerExistence()
      context.system.scheduler.scheduleOnce(Homework.timeToCopyInMillis milliseconds, self, CopyComplete)
    case CopyComplete =>
      context.parent ! Student.CopyComplete(Philosophy)
  }

  private def considerExistence() = {
    val lifeIsALottery = random.nextInt(50)
    if (lifeIsALottery == 1) {
      throw new MoralCrisis(s"Philosopher is experiencing a moral crisis while copying Kant for student $studentId")
    }
  }
}
