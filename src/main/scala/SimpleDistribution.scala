/**
	* Created by jason on 12/8/15.
	*/

import akka.actor._
import akka.actor.SupervisorStrategy._

case class WorkMessage(data:Seq[Int])
case class ProblemMessage(problematicData:Seq[Int], workerName:String = "Unknown worker")
case class Process(problematicData:Seq[Int], workerName:String = "Unknown worker")

class Master(numWorkers:Int, dataset:Seq[Int]) extends Actor{

	// Constructor spawns numWorkers actors and sends initial messages.
	val workers:Seq[ActorRef] = 1 to numWorkers map (i=>context.actorOf(Props[Worker], s"Worker_$i"))
	val batches = dataset.grouped(dataset.size / numWorkers).toIndexedSeq
	workers zip(batches) foreach {case(a, d)=> a ! WorkMessage(d)}

	// Register ourselves for Terminated messages
	workers foreach (w=>context.watch(w))

	def receive = readMSgs

	override val supervisorStrategy = OneForOneStrategy() {
		case _:ActorKilledException => Restart // TODO:Try restarting as well.
	}

	def readMSgs:Receive = {
		case ProblemMessage(problematicData:Seq[Int], workerName:String) =>
			println("Received a ProblemMessage in master.")
			context become ignoreWorkerProblems
			self ! Process(problematicData, workerName)
		case _ => throw new RuntimeException("Unknown message received at master.")
	}

	def ignoreWorkerProblems:Receive = {
		case ProblemMessage(pData:Seq[Int], workerName:String) => println(s"Master is ignoring message $pData from worker $workerName")
		case Process(data:Seq[Int], workerName:String) =>
			workers foreach (i=> i ! Kill) // Stop all workers.
			println(s"Master working on data: $data sent to it by worker $workerName.")
		 	//context become readMSgs //TODO: Instead of this, find a way to wait for children's termination before re-launching the data cycle.
			case Terminated(_) => println("Received a Termination message at master")
		case _ => throw new RuntimeException("Unknown message received at master.")
	}
}

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
}

object SimpleDistribution extends App{
	val system = ActorSystem("Test_System")
	val master = system.actorOf(Props(new Master(5, 1 to 1000)), name = "My_Master")
	//system.terminate()
	//master ! Kill
	//println("Exiting application....")
}