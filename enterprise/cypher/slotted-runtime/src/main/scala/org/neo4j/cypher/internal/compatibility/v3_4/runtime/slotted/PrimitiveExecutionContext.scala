/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compatibility.v3_4.runtime.slotted

import org.neo4j.cypher.internal.compatibility.v3_4.runtime.PipelineInformation
import org.neo4j.cypher.internal.util.v3_4.InternalException
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.cypher.internal.runtime.interpreted.ExecutionContext
import org.neo4j.values.AnyValue

object PrimitiveExecutionContext {
  def empty = new PrimitiveExecutionContext(PipelineInformation.empty)
}

case class PrimitiveExecutionContext(pipeline: PipelineInformation) extends ExecutionContext {

  override val longs = new Array[Long](pipeline.numberOfLongs)
  override val refs = new Array[AnyValue](pipeline.numberOfReferences)

  override def toString(): String = {
    val iter = this.iterator
    val s: StringBuilder = StringBuilder.newBuilder
    s ++= s"\nPrimitiveExecutionContext {\n    $pipeline"
    while(iter.hasNext) {
      val slotValue = iter.next
      s ++= f"\n    ${slotValue._1}%-40s = ${slotValue._2}"
    }
    s ++= "\n}\n"
    s.result
  }

  override def copyTo(target: ExecutionContext, longOffset: Int = 0, refOffset: Int = 0): Unit = target match {
    case other@PrimitiveExecutionContext(otherPipeline) =>
      if (pipeline.numberOfLongs > otherPipeline.numberOfLongs ||
        pipeline.numberOfReferences > otherPipeline.numberOfReferences)
        throw new InternalException("Tried to copy more data into less.")
      else {
        System.arraycopy(longs, 0, other.longs, longOffset, pipeline.numberOfLongs)
        System.arraycopy(refs, 0, other.refs, refOffset, pipeline.numberOfReferences)
      }
    case _ => fail()
  }

  override def copyFrom(input: ExecutionContext, nLongs: Int, nRefs: Int): Unit = input match {
    case other@PrimitiveExecutionContext(otherPipeline) =>
      if (nLongs > pipeline.numberOfLongs || nRefs > pipeline.numberOfReferences)
        throw new InternalException("Tried to copy more data into less.")
      else {
        System.arraycopy(other.longs, 0, longs, 0, nLongs)
        System.arraycopy(other.refs, 0, refs, 0, nRefs)
      }
    case _ => fail()
  }

  override def setLongAt(offset: Int, value: Long): Unit = longs(offset) = value

  override def getLongAt(offset: Int): Long = longs(offset)

  override def setRefAt(offset: Int, value: AnyValue): Unit = refs(offset) = value

  override def getRefAt(offset: Int): AnyValue = {
    val value = refs(offset)
    if (value == null)
      throw new InternalException("Value not initialised")
    value
  }

  override def +=(kv: (String, AnyValue)): Nothing = fail()

  override def -=(key: String): Nothing = fail()

  override def get(key: String): Nothing = fail()

  override def iterator: Iterator[(String, AnyValue)] = {
    // This method implementation is for debug usage only (the debugger will invoke it when stepping).
    // Please do not use in production code.
    val longSlots = pipeline.getLongSlots
    val refSlots = pipeline.getRefSlots
    val longSlotValues = for { i <- 0 until longs.length }
      yield (longSlots(i).toString, ValueUtils.of(longs(i)))
    val refSlotValues = for { i <- 0 until refs.length }
      yield (refSlots(i).toString, refs(i))
    (longSlotValues ++ refSlotValues).iterator
  }

  private def fail(): Nothing = throw new InternalException("Tried using a primitive context as a map")

  override def newWith1(key1: String, value1: AnyValue): ExecutionContext = fail()

  override def newWith2(key1: String, value1: AnyValue, key2: String, value2: AnyValue): ExecutionContext = fail()

  override def newWith3(key1: String, value1: AnyValue, key2: String, value2: AnyValue, key3: String, value3: AnyValue): ExecutionContext = fail()

  override def mergeWith(other: ExecutionContext): ExecutionContext = fail()

  override def createClone(): ExecutionContext = fail()

  override def newWith(newEntries: Seq[(String, AnyValue)]): ExecutionContext = fail()
}
