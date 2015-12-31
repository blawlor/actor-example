package ie.corkjug.actors

import akka.actor.ActorSystem

object Main {
  def main(args: Array[String]) {
    val system = ActorSystem("school-system")
    // Create a school
    val teacher = system.actorOf(Teacher.props(30))
  }
}
