/**
	* Created by jason on 12/14/15.
	*/

import akka.actor._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

case class MiniWorkMessage(name:String)
case object Problem

class MiniMaster extends Actor {

	val worker1 = context.actorOf(Props[MiniWorker], "Worker_1")
	val worker2 = context.actorOf(Props[MiniWorker], "Worker_2")

	worker1 ! MiniWorkMessage("Jason") // Should allow Worker to stop nicely.
	worker2 ! MiniWorkMessage("Alex") // Should make master kill worker, raising an ActorKilledException from worker.

	override val supervisorStrategy = OneForOneStrategy() {
		case _: ActorKilledException => Stop
		case _: Exception => Escalate
	}

	override def receive = {
		case Problem => println(s"About to kill ${sender.path.name}"); sender ! Kill
		case t:Throwable => throw(t)
	}
}

class MiniWorker extends Actor {

	val myName = self.path.name

	override def receive = {
		case MiniWorkMessage(s:String) =>
			if (s.head.toLower == 'a') {
				println(s"Worker $myName observed a problem with input $s and will notify master.")
				sender ! Problem
			}else{
				println(s"Worker $myName: Processing of $s performed successfully. Actor will be stopped.")
				context.stop(self)
			}
			case t:Throwable => throw(t)
	}

	override def preStart(): Unit = {
		println(s"preStart() on $myName")
	}

	override def postStop(): Unit = {
		println(s"postStop() on $myName")
	}
}

object SupervisionStrategies{
	def main(args:Array[String]) {
		val system = ActorSystem("Test_System")
		val master = system.actorOf(Props(new MiniMaster()), name = "My_MiniMaster")
		system.terminate()
	}
}
