import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._
import messages._

/**
	* Master node class. Distributes the data to workers and synchronizes them.
	* Might end up performing work on the entire distributed batch if the
	* nodes fail locally and report it.
	*/
class Master(numWorkers:Int, numBatches:Int, dataset:Seq[Int]) extends Actor{

	/* Constructor does the following:
	 * (1) Splits the dataset into batches. Each of those batches will be parallelized along our worker nodes.
	 * (2) Initializes the array of workers.
	 */
	val batches = dataset.grouped(dataset.size / numBatches).toIndexedSeq
	private var currBatchInd = 0

	val workers:Seq[ActorRef] = 1 to numWorkers map (i=>context.actorOf(Props[Worker], s"Worker_$i"))
	println("Master about to distribute first batch.")
	distributeCurrentBatch()

	private def distributeCurrentBatch(): Unit = {
		val currentBatch = batches(currBatchInd)
		val workerBatches = currentBatch.grouped(currentBatch.size / numWorkers).toIndexedSeq
		workers zip workerBatches foreach {case(a, d)=> a ! WorkMessage(d)}
	}

	override val supervisorStrategy = AllForOneStrategy() {
		case _: ActorKilledException => Stop
		case _: Throwable => Escalate
	}

	def receive = readMSgs

	def readMSgs:Receive = {
		case ProblemMessage(pData:Seq[Int], workerName:String) =>
			context become ignoreWorkerProblems // So that other ProblemMessages don't trigger needless work.
			self ! Process(workerName)
		case _ => throw new RuntimeException("Unknown message received at master.")
	}

	def ignoreWorkerProblems:Receive = {
		case ProblemMessage(pData:Seq[Int], workerName:String) => println(s"Master is ignoring message $pData from worker $workerName")
		case Process(workerName:String) =>
			println(s"Master received some data from $workerName and will take care of batch #$currBatchInd itself.")
			workers foreach (i => { println(s"Master about to send a kill message to worker ${i.path.name}"); i ! Kill })
			currBatchInd += 1
			if(currBatchInd == batches.length) {
				println("System terminating.")
				context.system.terminate()
			} else {
				println(s"Master finished processing batch #${currBatchInd-1}, and will now distribute #$currBatchInd.")
				distributeCurrentBatch() // TODO: See if this will give you dead letters.
				context become readMSgs
			}
			//case _: ActorKilledException => Stop
		case _ => throw new RuntimeException("Unknown message received at master.")
	}

}
