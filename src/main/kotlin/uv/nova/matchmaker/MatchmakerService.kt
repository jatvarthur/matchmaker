package uv.nova.matchmaker

import kotlin.concurrent.thread

class MatchService(val queueService: TicketQueueService) {

    lateinit var worker: Thread

    fun start() {
        worker = thread(start = true, name = "matchmaker-worker-thread") {

        }
    }

    fun shutdown() {
        worker.join()
    }

}
