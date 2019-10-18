package utils

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.{Attributes, Inlet, Outlet, UniformFanInShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.{immutable, mutable}

// Strongly inspired by https://github.com/akka/akka/pull/25317/files

object Merge {
  def mergeSortedN[T: Ordering](sources: immutable.Seq[Source[T, _]]): Source[T, NotUsed] = {
    sources match {
      case immutable.Seq()   => Source.empty[T]
      case immutable.Seq(s1: Source[T, _]) => s1.mapMaterializedValue(_ => NotUsed)
      case s1 +: s2 +: ss   => Source.combine(s1, s2, ss: _*)(new MergeSortedN[T](_))
    }
  }
}


class MergeSortedN[T: Ordering](inputPorts: Int)
  extends GraphStage[UniformFanInShape[T, T]] {
  val inlets: immutable.IndexedSeq[Inlet[T]] = Vector.tabulate(inputPorts)(i => Inlet[T]("MergeSortedN.in" + i))
  val out: Outlet[T] = Outlet[T]("MergeSortedN.out")
  override val shape: UniformFanInShape[T, T] = UniformFanInShape(out, inlets: _*)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {
      private val bufferedElements = new mutable.TreeSet[(T, Int)]()
      private val inletsBeingPulled = new mutable.HashSet[Inlet[T]]
      private var runningInlets = inputPorts
      private def canPush = inletsBeingPulled.isEmpty && bufferedElements.nonEmpty && isAvailable(out)
      private def upstreamsClosed = runningInlets == 0

      override def preStart(): Unit = {
        for (i <- inlets) {
          tryPull(i)
          inletsBeingPulled.add(i)
        }
      }

      private def maybeCompleteStage(): Unit = {
        if (bufferedElements.isEmpty && upstreamsClosed) {
          completeStage()
        }
      }

      private def maybePush(): Unit = {
        if (canPush) {
          val next = bufferedElements.firstKey
          bufferedElements.remove(next)
          push(out, next._1)
          val inlet = inlets(next._2)
          if (!isClosed(inlet)) {
            pull(inlet)
            inletsBeingPulled.add(inlet)
          }
        }

        maybeCompleteStage()
      }

      for (i <- inlets.indices) {
        val inlet = inlets(i)
        val index = i
        setHandler(inlet, new InHandler {
          override def onPush(): Unit = {
            val element = grab(inlet)
            bufferedElements.add((element, index))
            inletsBeingPulled.remove(inlet)
            maybePush()
          }

          override def onUpstreamFinish(): Unit = {
            inletsBeingPulled.remove(inlet)
            runningInlets -= 1
            maybePush()
          }
        })
      }

      override def onPull(): Unit = {
        maybePush()
      }

      setHandler(out, this)
    }

  override def toString = s"MergeSortedN($inputPorts)"
}
