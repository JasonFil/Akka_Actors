/**
	* Created by jason on 12/14/15.
	*/
import akka.actor._
import messages._

/**
	* Worker node class. Perform work on given batch, and send a ProblemMessage to the Master
	* if a problem occurs.
	*/
class Worker extends Actor {
	val myName = self.path.name
	println(s"Worker $myName came to existence.")
	def receive = {
		case WorkMessage(data:Seq[Int]) =>
			println(s"Worker $myName is now working on piece of data: $data")
			val sublist = data.filter(_%30==0) // As an example.
			if(sublist.nonEmpty) {
				println(s"Worker $myName is sending a problem message to master.")
				sender ! ProblemMessage(sublist, myName)
				println(s"Worker $myName just sent a problem message to master.")
			}
			else {
				println(s"Worker $myName is terminating normally.")
				context.stop(self)
			}
		case _ => throw new RuntimeException(s"Unknown message received at worker $myName.")
	}

	override def postStop(): Unit = {
		println(s"Worker $myName's postStop() hook was called.")
	}
}