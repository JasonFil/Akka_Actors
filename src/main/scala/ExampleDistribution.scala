/**
	* Created by jason on 12/8/15.
	*/

import akka.actor._
import akka.pattern.gracefulStop
import scala.concurrent.Await

object ExampleDistribution{
	def main(args:Array[String]) {
		require(args.length >= 2, "Please provide the number of workers and number of example batches.")
		val numWorkers = args(0).toInt
		val numBatches = args(1).toInt
		require(numWorkers >= 2, s"Number of workers = $numWorkers was invalid: need to provide at least 2 workers. ")
		require(numBatches >= 5, s"Number of batches = $numBatches was invalid: need to provide at least 5 batches.")
		val system = ActorSystem("Test_System")
		val dataset = 1 to 1000 // Presumable to be replaced by the CAVIAR representation at some point.
		val master = system.actorOf(Props(new Master(numWorkers, numBatches, dataset)), name = "My_Master")
		//master ! Kill
		//println("Exiting application....")
		system.terminate()
	}
}