package com.raphtory.algorithms

import com.raphtory.core.components.graphbuilder.GraphBuilder
import com.raphtory.core.components.graphbuilder.Properties._

/**
  * Basic graph builder object
  *
  * Simply reads a file and splits the line by separator param.
  *
  * Should only be used in tests and not anywhere else.
  */
class BasicGraphBuilder extends GraphBuilder[String] {

  override def parseTuple(line: String): Unit =
    if (line.nonEmpty) {
      val fileLine   = line.split(",").map(_.trim)
      val sourceNode = fileLine(0)
      val srcID      = sourceNode.toLong
      val targetNode = fileLine(1)
      val tarID      = targetNode.toLong
      val timeStamp  = fileLine(2).toLong

      addVertex(timeStamp, srcID, Properties(ImmutableProperty("name", sourceNode)))
      addVertex(timeStamp, tarID, Properties(ImmutableProperty("name", targetNode)))
      addEdge(timeStamp, srcID, tarID)

      logger.debug(s"Finished processing line '$line'.")
    }

}

object BasicGraphBuilder {
  def apply() = new BasicGraphBuilder()
}
