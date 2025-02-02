package com.raphtory.internals.storage.pojograph.entities.internal

import com.raphtory.internals.storage.pojograph.OrderedBuffer._

private[raphtory] class PojoEdge(msgTime: Long, srcId: Long, dstId: Long, initialValue: Boolean)
        extends PojoEntity(msgTime, initialValue) {

  def killList(vKills: List[Long]): Unit = history sortedExtend vKills.map(x => (x, false))

  def getSrcId: Long = srcId
  def getDstId: Long = dstId
}
