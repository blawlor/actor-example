package ie.corkjug.actors

object Homework {

  sealed trait Subject

  case object Latin extends Subject

  case object Greek extends Subject

  case object Maths extends Subject

  case object English extends Subject

  case object History extends Subject

  case object Physics extends Subject

  case object Geography extends Subject

  case object Philosophy extends Subject

  val subjects = Seq(Latin, Greek, Maths, English, History, Physics, Geography, Philosophy)

  val timeToCopyInMillis = 500

  val defaultTimeToDoSubjectInMillis = 10000

  def subjectFor(i:Int) = subjects(i%subjects.size)

  object Aptitude {
    def apply() = new Aptitude(randomTimes)
    def randomTimes():Map[Subject, Int] = {
      val speeds:Map[Subject, Int] = Map.empty
      speeds
    }
  }

  case class Aptitude(subjectTimes: Map[Subject, Int] ) {
    def timeToComplete(subject: Subject) = subjectTimes getOrElse (subject, defaultTimeToDoSubjectInMillis)

    override def toString: String = subjectTimes.map{pair => pair._1 +"=>" + pair._2}.mkString("/n")
  }

  case class Assignment(subjects: Set[Subject])

  case class AssignmentResult(subjects: Set[Subject])
}
