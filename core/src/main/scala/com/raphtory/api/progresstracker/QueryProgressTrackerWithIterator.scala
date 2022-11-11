package com.raphtory.api.progresstracker

import com.raphtory.api.analysis.table._
import com.raphtory.api.time.Perspective
import com.raphtory.internals.communication.TopicRepository
import com.raphtory.internals.components.output._
import com.raphtory.internals.components.querymanager._
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
  * Iterator over perspective results with additional query tracking functionality.
  */
class QueryProgressTrackerWithIterator(
    graphID: String,
    jobID: String,
    topics: TopicRepository,
    conf: Config,
    timeout: Duration
) extends QueryProgressTracker(graphID, jobID, conf) {

  private val logger: Logger                  = Logger(LoggerFactory.getLogger(this.getClass))
  private var outputDone: Boolean             = false
  private var jobsDone                        = 0
  private var nextResult: Option[TableOutput] = None
  private val resultsInProgress               = mutable.Map.empty[Perspective, ArrayBuffer[Row]]
  private val perspectiveDoneCounts           = mutable.Map.empty[Perspective, Int]
  private val completedResults                = new LinkedBlockingQueue[Any]

  override def handleMessage(msg: QueryManagement): Unit =
    msg match {
      case _: JobFailed =>
        completedResults.add(msg)
        super.handleMessage(msg)
      case _            => super.handleMessage(msg)
    }

  def handleOutputMessage(msg: OutputMessages): Unit = {
    logger.debug(s"received message $msg")
    msg match {
      case RowOutput(perspective, row)                  =>
        resultsInProgress.getOrElseUpdate(perspective, ArrayBuffer.empty[Row]).append(row)
      case EndPerspective(perspective, totalPartitions) =>
        perspectiveDoneCounts(perspective) = perspectiveDoneCounts.getOrElse(perspective, 0) + 1
        if (perspectiveDoneCounts(perspective) == totalPartitions) {
          perspectiveDoneCounts.remove(perspective)
          resultsInProgress.remove(perspective).map(_.toArray) match {
            case Some(rows) =>
              completedResults.add(TableOutput(getJobId, perspective, rows, conf, topics))
            case None       =>
              completedResults.add(TableOutput(getJobId, perspective, Array.empty, conf, topics))
          }
        }
      case EndOutput(totalPartitions)                   =>
        jobsDone += 1
        if (jobsDone == totalPartitions)
          completedResults.add(EndOutput)
    }
  }

  override def isJobDone: Boolean = outputDone

  private def waitForNextResult(): Any = {
    logger.debug("waiting for result")
    if (timeout.isFinite)
      Option(completedResults.poll(timeout.length, timeout.unit))
    else
      Option(completedResults.take())
  }

  object TableOutputIterator extends Iterator[TableOutput] {
    import TableOutputIterator.TableOutputIteratorException._

    override def hasNext: Boolean =
      if (nextResult.isDefined) {
        logger.debug("hasNext true as result already present")
        true
      }
      else if (outputDone) {
        logger.debug("hasNext false as output complete")
        false
      }
      else
        waitForNextResult() match {
          case Some(res) =>
            res match {
              case JobFailed(error) =>
                throw new JobFailedException(jobID, error)
              case EndOutput        =>
                outputDone = true
                logger.debug("hasNext false as output complete after waiting for result")
                false
              case v: TableOutput   =>
                nextResult = Some(v)
                logger.debug("hasNext true as new result available after waiting")
                true
            }
          case None      =>
            false
        }

    override def next(): TableOutput =
      if (hasNext) {
        val out = nextResult.get
        nextResult = None
        out
      }
      else if (outputDone)
        throw new NoSuchElementException("All perspectives already processed")
      else
        throw new NoSuchElementException("Timed out while waiting for next perspective")

    private object TableOutputIteratorException {

      class JobFailedException(jobID: String, cause: Throwable)
              extends RuntimeException(s"The execution of the query '$jobID' failed", cause)
    }
  }
}

object QueryProgressTrackerWithIterator {

  def apply(
      graphID: String,
      jobID: String,
      topics: TopicRepository,
      conf: Config,
      timeout: Duration
  ): QueryProgressTrackerWithIterator =
    new QueryProgressTrackerWithIterator(graphID, jobID, topics, conf, timeout)
}
