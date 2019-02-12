package lantern

import scala.util.continuations._
import org.scala_lang.virtualized.virtualize
import org.scala_lang.virtualized.SourceContext

import scala.virtualization.lms._
import scala.virtualization.lms.common._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{Map => MutableMap}
import scala.math._

trait TensorSecOrderApi extends TensorDsl with Diff {

  object TensorF {
    def zerosLike(that: TensorF) = new TensorF(Tensor.zeros_like(that.x), Tensor.zeros_like(that.d))
  }

  class TensorF(val x: Tensor, val d: Tensor) extends Serializable {
    var isInput: Boolean = false // true if it is an input (no need to compute gradient)

    def apply(i: Rep[Int]) = new TensorF(x(i), d(i))
    def apply(i: Int, j: Int) = new TensorF(x(i, j), d(i, j))

    def clip_grad(bound: Float) = {
      d.clipAt(bound)
    }

    def + (that: TensorF): TensorF = new TensorF(x + that.x, d + that.d)
    def + (that: Rep[Float]): TensorF = new TensorF(x + that, d)
    def - (that: TensorF): TensorF = new TensorF(x - that.x, d - that.d)
    def - (that: Rep[Float]): TensorF = new TensorF(x - that, d)
    def * (that: TensorF): TensorF = new TensorF(x * that.x, d * that.x + x * that.d)
    def * (that: Rep[Float]): TensorF = new TensorF(x * that, d * that)
    def dot(that: TensorF): TensorF = new TensorF(x dot that.x, x.dot(that.d) + d.dot(that.x))
    def sum(): TensorF = new TensorF(x.sum(), d.sum())
    def tanh(): TensorF = {
      val value = x.tanh()
      new TensorF(value, d - value * value * d)
    }
    def conv2D_batch(kernel: TensorF, bias: Option[TensorF], strides: Seq[Int], pads: Seq[Int]): (TensorF, Option[TensorF]) = bias match {
      case Some(b) => ???
        // val (value, opValue) = x.conv2D_batch(kernel.x, b.x, strides, pads)
        // val (tangent1, opTangent1) = x.conv2D_batch(kernel.d, None, strides, pads)
        // val (tangent2, opTangent2) = d.conv2D_batch(kernel.x, None, strides, pads)
        // new TensorF(value, tangent1 + tangent2 + b.d)
      case None =>
        val (value, opValue) = x.conv2D_batch(kernel.x, None, strides, pads)
        val (tangent1, opTangent1) = x.conv2D_batch(kernel.d, None, strides, pads)
        val (tangent2, opTangent2) = d.conv2D_batch(kernel.x, None, strides, pads)
        (opValue, opTangent2) match {
          case (Some(opValue), Some(opTangent2)) =>
            (new TensorF(value, tangent1 + tangent2), Some(new TensorF(opValue, opTangent2)))
          case (None, None) =>
            (new TensorF(value, tangent1 + tangent2), None)
        }

    }

    // inplace mutations
    def += (that: TensorF) = {x += that.x; d += that.d}
    def += (that: Rep[Float]) = x += that
    def -= (that: TensorF) = {x -= that.x; d -= that.d}
    def -= (that: Rep[Float]) = x -= that
    def *= (that: TensorF) = {x *= that.x; d *= that.x; d += x * that.d}
    def *= (that: Rep[Float]) = {x *= that; d *= that}
    def add_cartesian(y: TensorF, output: TensorF) = {
      x.add_cartesian(y.x, output.x)
      d.add_cartesian(y.x, output.d); d.add_cartesian(y.d, output.x)
    }
    def add_composition(y: TensorF, output: TensorF) = {
      x.add_composition(y.x, output.x)
      d.add_composition(y.x, output.d); d.add_composition(y.d, output.x)
    }
    // this += y^T dot output
    def add_dotTrans1(y: TensorF, output: TensorF) = {
      x.add_dotTrans1(y.x, output.x)
      d.add_dotTrans1(y.x, output.d); d.add_dotTrans1(y.d, output.x)
    }
    // this += y dot output^T
    def add_dotTrans2(y: TensorF, output: TensorF) = {
      x.add_dotTrans2(y.x, output.x)
      d.add_dotTrans2(y.x, output.d); d.add_dotTrans2(y.d, output.x)
    }
  }

  object TensorFR {
    def apply(x: TensorF) = new TensorFR(x, TensorF.zerosLike(x))
  }

  class TensorFR(val x: TensorF, val d: TensorF) extends Serializable {
    var isInput: Boolean = false

    def apply(i: Rep[Int]) = new TensorFR(x(i), d(i))
    def apply(i: Int, j: Int) = new TensorFR(x(i, j), d(i, j))

    def clip_grad(bound: Float) = {
      d.clip_grad(bound)
    }

    def + (that: TensorFR): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      val y = TensorFR(x + that.x); k(y)
      d += y.d; that.d += y.d
    }
    def * (that: TensorFR): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      val y = TensorFR(x * that.x); k(y)
      d += y.d * that.x; that.d += y.d * x
    }
    def dot (that: TensorFR): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      val y = TensorFR(x dot that.x); k(y)
      (x.x.rank, that.x.x.rank) match {
        case (1, 1) => d += that.x * y.d; that.d += x * y.d
        case (2, 1) => d.add_cartesian(that.x, y.d); that.d.add_composition(x, y.d)
        case (2, 2) => d.add_dotTrans2(y.d, that.x); that.d.add_dotTrans1(x, y.d)
      }
    }
    def sum(): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      val y = TensorFR(x.sum()); k(y)
      d += y.d
    }
    def tanh(): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      val y = TensorFR(x.tanh()); k(y)
      d += y.d; d -= y.x * y.x * y.d
    }
    def conv2D_batch(kernel: TensorFR, bias: Option[TensorFR] = None, strides: Seq[Int] = Seq(1,1), pads: Seq[Int] = Seq(0,0)): TensorFR @diff = shift { (k: TensorFR => Unit) =>
      bias match {
        case Some(b) => ???
        case None =>
          x.conv2D_batch(kernel.x, None, strides, pads) match {
            case (out, Some(opInput)) =>
              val y = TensorFR(out); k(y)
              generateRawComment("conv2D back-propagate sec order")
              val paddings = if (pads.size == 2) (pads(0), pads(1)) else {if (pads.size == 4) (pads(0), pads(2)) else {if (pads.size == 1) (pads(0), pads(0)) else ???}}
              val stridess = if (strides.size == 2) (strides(0), strides(1)) else ???
              val opInputFR = TensorFR(opInput)
              // need to use conv2D_batch_grad backend call wisely
              // messing with (this, kernel, y, opInputFR)
              backend.conv2D_batch_grad(
                new TensorR(this.x.x, this.d.x),
                Some(new TensorR(opInputFR.x.x, opInputFR.d.x)),
                new TensorR(kernel.x.x, kernel.d.x),
                new TensorR(y.x.x, y.d.x),
                None,
                paddings, stridess, (1, 1))
              backend.conv2D_batch_grad(
                new TensorR(this.x.d, this.d.d), //
                Some(new TensorR(opInputFR.x.d, opInputFR.d.d)), //
                new TensorR(kernel.x.d, kernel.d.d), //
                new TensorR(y.x.x, y.d.x), //
                None,
                paddings, stridess, (1, 1))
              backend.conv2D_batch_grad(
                new TensorR(this.x.d, this.d.d), //
                Some(new TensorR(opInputFR.x.x, opInputFR.d.d)), //
                new TensorR(kernel.x.x, kernel.d.d), //
                new TensorR(y.x.d, y.d.d), //
                None,
                paddings, stridess, (1, 1))
            case (out, None) => ???
          }
      }
    }

  }

  // reset for gradients and hessian_vector
  def gradHessV(f: TensorFR => TensorFR @diff)(start: TensorF) = {
    val x = TensorFR(start)
    reset {
      val temp = f(x)
      temp.d.x.setAsOne()
      ()
    }
    val gradient: Tensor = x.d.x
    val hessian_vector: Tensor = x.d.d
    (gradient, hessian_vector)
  }
  def gradHessV(f: () => TensorFR @diff) = {
    val result = Tensor.scalar(0)
    reset {
      val temp = f()
      // Assume that result is scalar
      result.copy_data(temp.x.x)
      temp.d.x.setAsOne()
    }
    result
  }
  def getGradient(x: TensorFR): Tensor = x.d.x
  def getHessV(x: TensorFR): Tensor = x.d.d

}