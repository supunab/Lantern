package lantern.thirdparty

import lms.core._
import lms.util._
import lms.core.stub._
import lms.core.Backend._
import lms.core.virtualize
import lms.core.utils.time
import lms.macros.{SourceContext, RefinedManifest}
import lms.thirdparty.{CLibs, SizeTOps}

import lms.collection.mutable.{StackArrayOps}

trait CuDNNOps extends CuBLASOps with CLibs with StackArrayOps with SizeTOps { b: Base  =>
  /* LMS support for CuDNN library */

  def nullptr[T:Manifest] = cmacro[Array[T]]("nullptr")
  def cNull[T: Manifest] = cmacro[T]("NULL")
  def NULLptr[T:Manifest] = cmacro[Array[T]]("NULL")

  // macros for data layout
  abstract class TensorFormat
  def knchw = cmacro[TensorFormat]("CUDNN_TENSOR_NCHW")

  // macros for data type
  abstract class CuDNNDataType
  def kdouble = cmacro[CuDNNDataType]("CUDNN_DATA_DOUBLE")
  def kfloat = cmacro[CuDNNDataType]("CUDNN_DATA_FLOAT")
  def khalf = cmacro[CuDNNDataType]("CUDNN_DATA_HALF")
  def kint8 = cmacro[CuDNNDataType]("CUDNN_DATA_INT8")
  def kuint8 = cmacro[CuDNNDataType]("CUDNN_DATA_UINT8")
  def kint32 = cmacro[CuDNNDataType]("CUDNN_DATA_INT32")
  def kint8x4 = cmacro[CuDNNDataType]("CUDNN_DATA_INT8x4")
  def kint8x32 = cmacro[CuDNNDataType]("CUDNN_DATA_INT8x32")
  def kuint8x4 = cmacro[CuDNNDataType]("CUDNN_DATA_UINT8x4")

  // macros for activation
  abstract class ActivationType
  def ksigmoid = cmacro[ActivationType]("CUDNN_ACTIVATION_SIGMOID")
  def krelu = cmacro[ActivationType]("CUDNN_ACTIVATION_RELU")
  def ktanh = cmacro[ActivationType]("CUDNN_ACTIVATION_TANH")
  def kclipped_relu = cmacro[ActivationType]("CUDNN_ACTIVATION_CLIPPED_RELU")
  def kelu = cmacro[ActivationType]("CUDNN_ACTIVATION_ELU")

  // macros for nan opt
  abstract class NanOpt
  def knot_prop = cmacro[NanOpt]("CUDNN_NOT_PROPAGATE_NAN")
  def kprop = cmacro[NanOpt]("CUDNN_PROPAGATE_NAN")

  // macros for softmax mode
  abstract class SoftmaxMode
  def kinstance = cmacro[SoftmaxMode]("CUDNN_SOFTMAX_MODE_INSTANCE")
  def kchannel = cmacro[SoftmaxMode]("CUDNN_SOFTMAX_MODE_CHANNEL")

  // macros for reduction ops
  abstract class ReduceTensorOp
  def kradd = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_ADD")
  def krmul = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_MUL")
  def krmin = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_MIN")
  def krmax = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_MAX")
  def kramax = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_AMAX")
  def kravg = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_AVG")
  def krnorm1 = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_NORM1")
  def krnorm2 = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_NORM2")
  def krmul_no_zeros = cmacro[ReduceTensorOp]("CUDNN_REDUCE_TENSOR_MUL_NO_ZEROS")

  // macros for math type
  abstract class MathType
  def kdefault = cmacro[MathType]("CUDNN_DEFAULT_MATH")
  def ktensor_op = cmacro[MathType]("CUDNN_TENSOR_OP_MATH")
  def ktensor_allow_conversion = cmacro[MathType]("CUDNN_TENSOR_OP_MATH_ALLOW_CONVERSION")
  def kfma = cmacro[MathType]("CUDNN_FMA_MATH")

  // macros for conv types
  abstract class CudnnConvolutionMode
  def kconvolution = cmacro[CudnnConvolutionMode]("CUDNN_CONVOLUTION")
  def kcross_correlation = cmacro[CudnnConvolutionMode]("CUDNN_CROSS_CORRELATION")

  abstract class CudnnHandleT
  lazy val cudnnHandle = newStruct[CudnnHandleT]("cudnnHandle_t")

  // cudnnStatus_t and CUDNN_CALL
  abstract class CudnnStatusT
  def cudnnCreate(handle: Rep[CudnnHandleT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreate", Unwrap(handle))(Seq[Int](), Seq(0), Set(0))
  def cudnnDestroy(handle: Rep[CudnnHandleT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnDestroy", Unwrap(handle))(Seq[Int](), Seq(0), Set[Int]())
  def cudnnCall(status: Rep[CudnnStatusT]): Rep[Unit] =
    libFunction[Unit]("CUDNN_CALL", Unwrap(status))(Seq[Int](), Seq[Int](), Set[Int](), Adapter.CTRL)

  // cudnnTensorDescriptor_t struct
  abstract class CudnnTensorDescriptorT
  def getCudnnTensorDescriptorT = newStruct[CudnnTensorDescriptorT]("cudnnTensorDescriptor_t")
  def cudnnCreateTensorDescriptor(desc: Rep[CudnnTensorDescriptorT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreateTensorDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetTensor4dDescriptor(desc: Rep[CudnnTensorDescriptorT], layout: Rep[TensorFormat], dtype: Rep[CuDNNDataType], n: Rep[Int],
      h: Rep[Int], c: Rep[Int], w: Rep[Int]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnSetTensor4dDescriptor", Unwrap(desc), Unwrap(layout), Unwrap(dtype),
      Unwrap(n), Unwrap(h), Unwrap(c), Unwrap(w))(Seq(0), Seq(0), Set[Int]())
  def cudnnSetTensorNdDescriptor(desc: Rep[CudnnTensorDescriptorT], dtype: Rep[CuDNNDataType],  nbDims: Rep[Int],
      dimA: Rep[Array[Int]], strideA: Rep[Array[Int]]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnSetTensorNdDescriptor", Unwrap(desc), Unwrap(dtype), Unwrap(nbDims),
      Unwrap(dimA), Unwrap(strideA))(Seq(0), Seq(0), Set[Int]())
  def cudnnGetTensor4dDescriptor(layout: Rep[TensorFormat], dtype: Rep[CuDNNDataType], shape: Seq[Rep[Int]]) = {
    val desc = getCudnnTensorDescriptorT
    cudnnCall(cudnnCreateTensorDescriptor(desc))
    cudnnCall(cudnnSetTensor4dDescriptor(desc, layout, dtype, shape(0), shape(1), shape(2), shape(3)))
    desc
  }
  def cudnnGetTensor3dDescriptor(dtype: Rep[CuDNNDataType], dim0: Rep[Int], dim1: Rep[Int], dim2: Rep[Int],
      stride0: Rep[Int], stride1: Rep[Int], stride2: Rep[Int]) = {
    val desc = getCudnnTensorDescriptorT
    cudnnCall(cudnnCreateTensorDescriptor(desc))
    val dims = StackArray(dim0, dim1, dim2)
    val strides = StackArray(stride0, stride1, stride2)
    cudnnCall(cudnnSetTensorNdDescriptor(desc, dtype, 3, dims, strides))
    desc
  }
  def cudnnGetRNNParamsSize(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], xDesc: Rep[CudnnTensorDescriptorT],
      sizeInBytes: Var[SizeT], dataType: Rep[CuDNNDataType]) =
    libFunction[CudnnStatusT]("cudnnGetRNNParamsSize", Unwrap(handle), Unwrap(rnnDesc), Unwrap(xDesc), UnwrapV(sizeInBytes),
      Unwrap(dataType))(Seq(0,1,2,4), Seq(3), Set(3))

  // cudnnFilterDescriptor_t struct
  abstract class CudnnFilterDescriptorT
  def getCudnnFilterDescriptorT = newStruct[CudnnFilterDescriptorT]("cudnnFilterDescriptor_t")
  def cudnnCreateFilterDescriptor(desc: Rep[CudnnFilterDescriptorT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreateFilterDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetFilter4dDescriptor(desc: Rep[CudnnFilterDescriptorT], dtype: Rep[CuDNNDataType], layout: Rep[TensorFormat], n: Rep[Int],
      h: Rep[Int], c: Rep[Int], w: Rep[Int]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnSetFilter4dDescriptor", Unwrap(desc), Unwrap(dtype), Unwrap(layout),
      Unwrap(n), Unwrap(h), Unwrap(c), Unwrap(w))(Seq(0), Seq(0), Set[Int]())
  def cudnnGetFilter4dDescriptor(layout: Rep[TensorFormat], dtype: Rep[CuDNNDataType], shape: Seq[Rep[Int]]) = {
    val desc = getCudnnFilterDescriptorT
    cudnnCall(cudnnCreateFilterDescriptor(desc))
    cudnnCall(cudnnSetFilter4dDescriptor(desc, dtype, layout, shape(0), shape(1), shape(2), shape(3)))
    desc
  }
  def cudnnSetFilterNdDescriptor(filterDesc: Rep[CudnnFilterDescriptorT], dataType: Rep[CuDNNDataType], format: Rep[TensorFormat],
      nbDims: Rep[Int], filterDimA: Rep[Array[Int]]) =
    libFunction[CudnnStatusT]("cudnnSetFilterNdDescriptor", Unwrap(filterDesc), Unwrap(dataType), Unwrap(format),
      Unwrap(nbDims), Unwrap(filterDimA))(Seq(0, 1, 2, 4), Seq(0), Set[Int]())
  def cudnnGetFilter3dDescriptor(format: Rep[TensorFormat], dataType: Rep[CuDNNDataType],
    dim0: Rep[Int], dim1: Rep[Int], dim2:Rep[Int]) = {
    val desc = getCudnnFilterDescriptorT
    cudnnCall(cudnnCreateFilterDescriptor(desc))
    val dims = StackArray(dim0, dim1, dim2)
    cudnnCall(cudnnSetFilterNdDescriptor(desc, dataType, format, 3, dims))
    desc
  }
  def sizeOfFloat = cmacro[Int]("sizeof(float)")

  abstract class CudnnReduceTensorIndicesT
  def kreduceTensorNoIndices = cmacro[CudnnReduceTensorIndicesT]("CUDNN_REDUCE_TENSOR_NO_INDICES")
  def kreduceTensorFlattenedIndices = cmacro[CudnnReduceTensorIndicesT]("CUDNN_REDUCE_TENSOR_FLATTENED_INDICES")

  abstract class CudnnIndicesTypeT
  def k8bitIndices = cmacro[CudnnIndicesTypeT]("CUDNN_8BIT_INDICES")
  def k16bitIndices = cmacro[CudnnIndicesTypeT]("CUDNN_16BIT_INDICES")
  def k32bitIndices = cmacro[CudnnIndicesTypeT]("CUDNN_32BIT_INDICES")
  def k64bitIndices = cmacro[CudnnIndicesTypeT]("CUDNN_64BIT_INDICES")

  // cudnnReduceTensorDescriptor_t struct
  abstract class CudnnReduceTensorDescriptorT
  def getCudnnReduceTensorDescriptorT = newStruct[CudnnReduceTensorDescriptorT]("cudnnReduceTensorDescriptor_t")
  def cudnnCreateReduceTensorDescriptor(desc: Rep[CudnnReduceTensorDescriptorT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreateReduceTensorDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetReduceTensorDescriptor(desc: Rep[CudnnReduceTensorDescriptorT], reduceTensorOp: Rep[ReduceTensorOp],
      reduceTensorCompType: Rep[CuDNNDataType], reduceTensorNanOpt: Rep[NanOpt],
      reduceTensorIndices: Rep[CudnnReduceTensorIndicesT], reduceTensorIndicesType: Rep[CudnnIndicesTypeT]) =
    libFunction[CudnnStatusT]("cudnnSetReduceTensorDescriptor", Unwrap(desc), Unwrap(reduceTensorOp),
      Unwrap(reduceTensorCompType), Unwrap(reduceTensorNanOpt), Unwrap(reduceTensorIndices),
      Unwrap(reduceTensorIndicesType))(Seq[Int](), Seq(0), Set[Int]())
  def cudnnGetReduceTensorDescriptorT(reduceTensorOp: Rep[ReduceTensorOp], reduceTensorCompType: Rep[CuDNNDataType]) = {
    val desc = getCudnnReduceTensorDescriptorT
    cudnnCall(cudnnCreateReduceTensorDescriptor(desc))
    cudnnCall(cudnnSetReduceTensorDescriptor(desc, reduceTensorOp, reduceTensorCompType, knot_prop, kreduceTensorNoIndices, k32bitIndices))
    desc
  }
  def cudnnGetReductionWorkspaceSize(handle: Rep[CudnnHandleT], reduceDesc: Rep[CudnnReduceTensorDescriptorT],
      xDesc: Rep[CudnnTensorDescriptorT], outDesc: Rep[CudnnTensorDescriptorT], wsSize: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetReductionWorkspaceSize", Unwrap(handle), Unwrap(reduceDesc), Unwrap(xDesc),
      Unwrap(outDesc), UnwrapV(wsSize))(Seq(0), Seq(4), Set(4))
  def cudnnReduceTensor_(handle: Rep[CudnnHandleT], reduceDesc: Rep[CudnnReduceTensorDescriptorT], indices: Rep[Array[Int]],
      indicesSizeInBytes: Rep[SizeT], workSpace: Rep[Array[Float]], workspaceSizeInBytes: Rep[SizeT], alpha: Var[Float],
      aDesc: Rep[CudnnTensorDescriptorT], A: Rep[Array[Float]], beta: Var[Float], cDesc: Rep[CudnnTensorDescriptorT],
      C: Rep[Array[Float]]) =
    libFunction("cudnnReduceTensor", Unwrap(handle), Unwrap(reduceDesc), Unwrap(indices), Unwrap(indicesSizeInBytes),
      Unwrap(workSpace), Unwrap(workspaceSizeInBytes), UnwrapV(alpha), Unwrap(aDesc), Unwrap(A), UnwrapV(beta),
      Unwrap(cDesc), Unwrap(C))(Seq(0, 5, 6, 8, 9), Seq(2, 4, 11), Set(6, 9))

  // cudnnActivationDescriptor_t struct
  abstract class CudnnActivationDescriptorT
  def getCudnnActivationDescriptor = newStruct[CudnnActivationDescriptorT]("cudnnActivationDescriptor_t")
  def cudnnCreateActivationDescriptor(desc: Rep[CudnnActivationDescriptorT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreateActivationDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetActivationDescriptor(desc: Rep[CudnnActivationDescriptorT], mode: Rep[ActivationType],
      reluNanOpt: Rep[NanOpt], coef: Rep[Double]) =
    libFunction[CudnnStatusT]("cudnnSetActivationDescriptor", Unwrap(desc), Unwrap(mode), Unwrap(reluNanOpt),
      Unwrap(coef))(Seq(0), Seq(0), Set[Int]())
  def cudnnGetActivationDescriptor(mode: Rep[ActivationType]) = {
    val desc = getCudnnActivationDescriptor
    cudnnCall(cudnnCreateActivationDescriptor(desc))
    cudnnCall(cudnnSetActivationDescriptor(desc, mode, kprop, 0.0))
    desc
  }
  def cudnnActivationForward_(handle: Rep[CudnnHandleT], activationDesc: Rep[CudnnActivationDescriptorT], alpha: Var[Float],
      xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]], beta: Var[Float], yDesc: Rep[CudnnTensorDescriptorT], y: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnActivationForward", Unwrap(handle), Unwrap(activationDesc), UnwrapV(alpha), Unwrap(xDesc),
      Unwrap(x), UnwrapV(beta), Unwrap(yDesc), Unwrap(y))(Seq(0, 1, 2, 4), Seq(7), Set(2, 5))
  def cudnnActivationBackward_(handle: Rep[CudnnHandleT], activationDesc: Rep[CudnnActivationDescriptorT], alpha: Var[Float],
      yDesc: Rep[CudnnTensorDescriptorT], y: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT], dy: Rep[Array[Float]],
      xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]], beta: Var[Float], dxDesc: Rep[CudnnTensorDescriptorT],
      dx: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnActivationBackward", Unwrap(handle), Unwrap(activationDesc), UnwrapV(alpha),
      Unwrap(yDesc), Unwrap(y), Unwrap(dyDesc), Unwrap(dy), Unwrap(xDesc), Unwrap(x), UnwrapV(beta), Unwrap(dxDesc),
      Unwrap(dx))(Seq(0, 1, 2, 4, 6, 8), Seq(11), Set(2, 9))

  // Softmax
  abstract class CudnnSoftmaxAlgorithmT
  def cudnnSoftmaxFast = cmacro[CudnnSoftmaxAlgorithmT]("CUDNN_SOFTMAX_FAST")
  def cudnnSoftmaxAccurate = cmacro[CudnnSoftmaxAlgorithmT]("CUDNN_SOFTMAX_ACCURATE")
  def cudnnSoftmaxLog = cmacro[CudnnSoftmaxAlgorithmT]("CUDNN_SOFTMAX_LOG")
  abstract class CudnnSoftmaxMode
  def cudnnSoftmaxModeInstance = cmacro[CudnnSoftmaxMode]("CUDNN_SOFTMAX_MODE_INSTANCE")
  def cudnnSoftmaxModeChannel = cmacro[CudnnSoftmaxMode]("CUDNN_SOFTMAX_MODE_CHANNEL")
  def cudnnSoftmaxForward_(handle: Rep[CudnnHandleT], algo: Rep[CudnnSoftmaxAlgorithmT], mode: Rep[CudnnSoftmaxMode],
      alpha: Var[Float], xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]], beta: Var[Float], yDesc: Rep[CudnnTensorDescriptorT],
      y: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnSoftmaxForward", Unwrap(handle), Unwrap(algo), Unwrap(mode), UnwrapV(alpha),
      Unwrap(xDesc), Unwrap(x), UnwrapV(beta), Unwrap(yDesc), Unwrap(y))(Seq(0, 1, 2, 3, 5, 6), Seq(8), Set(3, 6))
  def cudnnSoftmaxBackward_(handle: Rep[CudnnHandleT], algo: Rep[CudnnSoftmaxAlgorithmT], mode: Rep[CudnnSoftmaxMode],
      alpha: Var[Float], yDesc: Rep[CudnnTensorDescriptorT], yData: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT],
      dy: Rep[Array[Float]], beta: Var[Float], dxDesc: Rep[CudnnTensorDescriptorT], dx: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnSoftmaxBackward", Unwrap(handle), Unwrap(algo), Unwrap(mode),
      UnwrapV(alpha), Unwrap(yDesc), Unwrap(yData), Unwrap(dyDesc), Unwrap(dy), UnwrapV(beta), Unwrap(dxDesc),
      Unwrap(dx))(Seq(0, 3, 5, 7, 8), Seq(10), Set(3, 8))

  // cudnnConvolutionDescriptor_t struct
  abstract class CudnnConvolutionDescriptorT
  def getCudnnConvolutionDescriptorT = newStruct[CudnnConvolutionDescriptorT]("cudnnConvolutionDescriptor_t")
  def cudnnCreateConvolutionDescriptor(desc: Rep[CudnnConvolutionDescriptorT]): Rep[CudnnStatusT] =
    libFunction[CudnnStatusT]("cudnnCreateConvolutionDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetConvolution2dDescriptor(desc: Rep[CudnnConvolutionDescriptorT], padding1: Rep[Int], padding2: Rep[Int],
      strides1: Rep[Int], strides2: Rep[Int], dilation1: Rep[Int], dilation2: Rep[Int], conv_mode: Rep[CudnnConvolutionMode],
      dtype: Rep[CuDNNDataType]) =
    libFunction[CudnnStatusT]("cudnnSetConvolution2dDescriptor", Unwrap(desc), Unwrap(padding1),
      Unwrap(padding2), Unwrap(strides1), Unwrap(strides2), Unwrap(dilation1), Unwrap(dilation2), Unwrap(conv_mode),
      Unwrap(dtype))(Seq(0), Seq(0), Set[Int]())
  def cudnnSetConvolutionMathType(desc: Rep[CudnnConvolutionDescriptorT], mathType: Rep[MathType]) =
    libFunction[CudnnStatusT]("cudnnSetConvolutionMathType", Unwrap(desc), Unwrap(mathType))(Seq(0), Seq(0), Set[Int]())

  def cudnnGetConvolution2dDescriptor(paddings: (Int, Int), strides: (Int, Int), dilations: (Int, Int),
    convMode: Rep[CudnnConvolutionMode], dtype: Rep[CuDNNDataType], mathType: Option[Rep[MathType]]) = {
      val desc = getCudnnConvolutionDescriptorT
      cudnnCall(cudnnCreateConvolutionDescriptor(desc))
      cudnnCall(cudnnSetConvolution2dDescriptor(desc, paddings._1, paddings._2, strides._1, strides._2, dilations._1, dilations._2,
        convMode, dtype))
      mathType match {
        case Some(mt: Rep[MathType]) => cudnnCall(cudnnSetConvolutionMathType(desc, mt))
        case _ => ()
      }
      desc
  }

  // macro for cudnnConvolutionFwdAlgo_t
  abstract class CudnnConvolutionFwdAlgoT
  def kconvFwdAlgoImplicitGemm = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_IMPLICIT_GEMM")
  def kconvFwdAlgoGemm = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_GEMM")
  def kconvFwdAlgoDirect = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_DIRECT")
  def kconvFwdAlgoFFT = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_FFT")
  def kconvFwdAlgoFFTTiling = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_FFT_TILING")
  def kconvFwdAlgoWinograd = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_WINOGRAD")
  def kconvFwdAlgoWinogradNonfused = cmacro[CudnnConvolutionFwdAlgoT]("CUDNN_CONVOLUTION_FWD_ALGO_WINOGRAD_NONFUSED")

  // cudnnConvolutionFwdAlgoPerf_t struct
  abstract class CudnnConvolutionFwdAlgoPerfT
  implicit class CudnnConvolutionFwdAlgoPerfTOps(x: Rep[CudnnConvolutionFwdAlgoPerfT]) {
    val algo = readField[CudnnConvolutionFwdAlgoPerfT, CudnnConvolutionFwdAlgoT](x, "algo")
  }
  def getCudnnConvolutionFwdAlgoPerfT = newStruct[CudnnConvolutionFwdAlgoPerfT]("cudnnConvolutionFwdAlgoPerf_t")
  def cudnnGetConvolutionForwardAlgorithm_v7(handle: Rep[CudnnHandleT], xDesc: Rep[CudnnTensorDescriptorT], wDesc: Rep[CudnnFilterDescriptorT],
      convDesc: Rep[CudnnConvolutionDescriptorT], yDesc: Rep[CudnnTensorDescriptorT], requestedAlgoCount: Rep[Int],
      returnedAlgoCount: Var[Int], perfResult: Rep[Array[CudnnConvolutionFwdAlgoPerfT]]) =
    libFunction[CudnnStatusT]("cudnnGetConvolutionForwardAlgorithm_v7", Unwrap(handle), Unwrap(xDesc),
      Unwrap(wDesc), Unwrap(convDesc), Unwrap(yDesc), Unwrap(requestedAlgoCount), UnwrapV(returnedAlgoCount),
      Unwrap(perfResult))(Seq(0), Seq(6, 7), Set(6))

  // conv work space
  def cudnnGetConvolutionForwardWorkspaceSize(handle: Rep[CudnnHandleT], xDesc: Rep[CudnnTensorDescriptorT], wDesc: Rep[CudnnFilterDescriptorT],
      convDesc: Rep[CudnnConvolutionDescriptorT], yDesc: Rep[CudnnTensorDescriptorT], algo: Rep[CudnnConvolutionFwdAlgoT],
      sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetConvolutionForwardWorkspaceSize", Unwrap(handle), Unwrap(xDesc),
      Unwrap(wDesc), Unwrap(convDesc), Unwrap(yDesc), Unwrap(algo), UnwrapV(sizeInBytes))(Seq(0), Seq(6), Set(6))

  // conv forward
  def cudnnConvolutionForward_[T:Manifest](handle: Rep[CudnnHandleT], alpha: Var[T], xDesc: Rep[CudnnTensorDescriptorT], input: Rep[Array[T]],
      wDesc: Rep[CudnnFilterDescriptorT], filter: Rep[Array[T]], convDesc: Rep[CudnnConvolutionDescriptorT], algo: Rep[CudnnConvolutionFwdAlgoT],
      wsArray: Rep[Array[T]], wsSize: Rep[SizeT], beta: Var[T], yDesc: Rep[CudnnTensorDescriptorT], output: Rep[Array[T]]) = {
    libFunction[CudnnStatusT]("cudnnConvolutionForward", Unwrap(handle), UnwrapV(alpha), Unwrap(xDesc),
      Unwrap(input), Unwrap(wDesc), Unwrap(filter), Unwrap(convDesc), Unwrap(algo), Unwrap(wsArray), Unwrap(wsSize), UnwrapV(beta), Unwrap(yDesc),
      Unwrap(output))(Seq(1, 3, 5, 10, 12), Seq(9, 12), Set(1, 10))
  }

  def cudnnAddTensor[T:Manifest](handle: Rep[CudnnHandleT], alpha: Var[T], aDesc: Rep[CudnnTensorDescriptorT],
      A: Rep[Array[T]], beta: Var[T], cDesc: Rep[CudnnTensorDescriptorT], C: Rep[Array[T]]) =
    libFunction[CudnnStatusT]("cudnnAddTensor", Unwrap(handle), UnwrapV(alpha), Unwrap(aDesc), Unwrap(A),
      UnwrapV(beta), Unwrap(cDesc), Unwrap(C))(Seq(1, 3, 4, 6), Seq(6), Set(1, 4))

  // macro for cudnnConvolutionBwdDataAlgo_t
  abstract class CudnnConvolutionBwdDataAlgoT
  // cudnnConvolutionBwdDataAlgoPerf_t struct
  abstract class CudnnConvolutionBwdDataAlgoPerfT
  implicit class CudnnConvolutionBwdDataAlgoPerfTOps(x: Rep[CudnnConvolutionBwdDataAlgoPerfT]) {
    val algo = readField[CudnnConvolutionBwdDataAlgoPerfT, CudnnConvolutionBwdDataAlgoT](x, "algo")
  }
  def getCudnnConvolutionBwdDataAlgoPerfT = newStruct[CudnnConvolutionBwdDataAlgoPerfT]("cudnnConvolutionBwdDataAlgoPerf_t")
  def cudnnGetConvolutionBackwardDataAlgorithm_v7(handle: Rep[CudnnHandleT], wDesc: Rep[CudnnFilterDescriptorT],
      yDesc: Rep[CudnnTensorDescriptorT], convDesc: Rep[CudnnConvolutionDescriptorT], xDesc: Rep[CudnnTensorDescriptorT],
      requestedAlgoCount: Rep[Int], returnedAlgoCountBwd: Var[Int], perfResultsBwd: Rep[Array[CudnnConvolutionBwdDataAlgoPerfT]]) = {
    libFunction[CudnnStatusT]("cudnnGetConvolutionBackwardDataAlgorithm_v7", Unwrap(handle), Unwrap(wDesc), Unwrap(yDesc),
      Unwrap(convDesc), Unwrap(xDesc), Unwrap(requestedAlgoCount), UnwrapV(returnedAlgoCountBwd), Unwrap(perfResultsBwd))(Seq[Int](),
      Seq(6, 7), Set(6))
  }
  def cudnnGetConvolutionBackwardDataWorkspaceSize(handle: Rep[CudnnHandleT], wDesc: Rep[CudnnFilterDescriptorT],
      dyDesc: Rep[CudnnTensorDescriptorT], convDesc: Rep[CudnnConvolutionDescriptorT], dxDesc: Rep[CudnnTensorDescriptorT],
      algo: Rep[CudnnConvolutionBwdDataAlgoT], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetConvolutionBackwardDataWorkspaceSize", Unwrap(handle), Unwrap(wDesc), Unwrap(dyDesc),
      Unwrap(convDesc), Unwrap(dxDesc), Unwrap(algo), UnwrapV(sizeInBytes))(Seq(0,1,2,3,4,5), Seq(6), Set(6))
  def cudnnConvolutionBackwardData_(handle: Rep[CudnnHandleT], alpha: Var[Float], wDesc: Rep[CudnnFilterDescriptorT],
      w: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT], dy: Rep[Array[Float]], convDesc: Rep[CudnnConvolutionDescriptorT],
      algo: Rep[CudnnConvolutionBwdDataAlgoT], wsData: Rep[Array[Float]], wsSize: Rep[SizeT], beta: Var[Float],
      dxDesc: Rep[CudnnTensorDescriptorT], dx: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnConvolutionBackwardData", Unwrap(handle), UnwrapV(alpha), Unwrap(wDesc), Unwrap(w),
      Unwrap(dyDesc), Unwrap(dy), Unwrap(convDesc), Unwrap(algo), Unwrap(wsData), Unwrap(wsSize), UnwrapV(beta), Unwrap(dxDesc),
      Unwrap(dx))(Seq(0,1,2,3,4,5,6,7,9,10,11), Seq(8,12), Set(1,10))

  def cudnnConvolutionBackwardBias_(handle: Rep[CudnnHandleT], alpha: Var[Float], dyDesc: Rep[CudnnTensorDescriptorT],
      dy: Rep[Array[Float]], beta: Var[Float], dbDesc: Rep[CudnnTensorDescriptorT], db: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnConvolutionBackwardBias", Unwrap(handle), UnwrapV(alpha), Unwrap(dyDesc), Unwrap(dy),
      UnwrapV(beta), Unwrap(dbDesc), Unwrap(db))(Seq(0, 1, 2, 3, 4, 5, 6), Seq(6), Set(1, 4))

  // macro for cudnnConvolutionBwdFilterAlgo_t
  abstract class CudnnConvolutionBwdFilterAlgoT
  // cudnnConvolutionBwdFilterAlgoPerf_t struct
  abstract class CudnnConvolutionBwdFilterAlgoPerfT
  implicit class CudnnConvolutionBwdFilterAlgoPerfTOps(x: Rep[CudnnConvolutionBwdFilterAlgoPerfT]) {
    val algo = readField[CudnnConvolutionBwdFilterAlgoPerfT, CudnnConvolutionBwdFilterAlgoT](x, "algo")
  }
  def getCudnnConvolutionBwdFilterAlgoPerfT = newStruct[CudnnConvolutionBwdFilterAlgoPerfT]("cudnnConvolutionBwdFilterAlgoPerf_t")
  def cudnnGetConvolutionBackwardFilterAlgorithm_v7(handle: Rep[CudnnHandleT], xDesc: Rep[CudnnTensorDescriptorT],
      dyDesc: Rep[CudnnTensorDescriptorT], convDesc: Rep[CudnnConvolutionDescriptorT], dwDesc: Rep[CudnnFilterDescriptorT],
      requestedAlgoCount: Rep[Int], returnedAlgoCount: Var[Int], perfResults: Rep[Array[CudnnConvolutionBwdFilterAlgoPerfT]]) =
    libFunction[CudnnStatusT]("cudnnGetConvolutionBackwardFilterAlgorithm_v7", Unwrap(handle), Unwrap(xDesc), Unwrap(dyDesc),
      Unwrap(convDesc), Unwrap(dwDesc), Unwrap(requestedAlgoCount), UnwrapV(returnedAlgoCount),
      Unwrap(perfResults))(Seq(0, 1, 2, 3, 4), Seq(6, 7), Set(6))
  def cudnnGetConvolutionBackwardFilterWorkspaceSize(handle: Rep[CudnnHandleT], xDesc: Rep[CudnnTensorDescriptorT],
      dyDesc: Rep[CudnnTensorDescriptorT], convDesc: Rep[CudnnConvolutionDescriptorT], dwDesc: Rep[CudnnFilterDescriptorT],
      algo: Rep[CudnnConvolutionBwdFilterAlgoT], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetConvolutionBackwardFilterWorkspaceSize", Unwrap(handle), Unwrap(xDesc), Unwrap(dyDesc),
      Unwrap(convDesc), Unwrap(dwDesc), Unwrap(algo), UnwrapV(sizeInBytes))(Seq(0,1,2,3,4,5), Seq(6), Set(6))
  def cudnnConvolutionBackwardFilter_(handle: Rep[CudnnHandleT], alpha: Var[Float], xDesc: Rep[CudnnTensorDescriptorT],
      x: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT], dy: Rep[Array[Float]], convDesc: Rep[CudnnConvolutionDescriptorT],
      algo: Rep[CudnnConvolutionBwdFilterAlgoT], wsData: Rep[Array[Float]], wsSize: Rep[SizeT], beta: Var[Float],
      dwDesc: Rep[CudnnFilterDescriptorT], dw: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnConvolutionBackwardFilter", Unwrap(handle), UnwrapV(alpha), Unwrap(xDesc),
      Unwrap(x), Unwrap(dyDesc), Unwrap(dy), Unwrap(convDesc), Unwrap(algo), Unwrap(wsData), Unwrap(wsSize),
      UnwrapV(beta), Unwrap(dwDesc), Unwrap(dw))(Seq(0,1,2,3,4,5,6,7,9,10,11), Seq(8,12), Set(1,10))

  // macros for pool mode
  abstract class PoolModes
  def kmax = cmacro[PoolModes]("CUDNN_POOLING_MAX")
  def kaverage_ip = cmacro[PoolModes]("CUDNN_POOLING_AVERAGE_COUNT_INCLUDE_PADDING")
  def kaverage_ep = cmacro[PoolModes]("CUDNN_POOLING_AVERAGE_COUNT_EXCLUDE_PADDING")
  def kmax_d = cmacro[PoolModes]("CUDNN_POOLING_MAX_DETERMINISTIC")

  // cudnnPoolingDescriptor_t
  abstract class CudnnPoolingDescriptorT
  def getCudnnPoolingDescriptor = newStruct[CudnnPoolingDescriptorT]("cudnnPoolingDescriptor_t")
  def cudnnCreatePoolingDescriptor(desc: Rep[CudnnPoolingDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreatePoolingDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetPooling2dDescriptor(desc: Rep[CudnnPoolingDescriptorT], mode: Rep[PoolModes], nanOpt: Rep[NanOpt],
      windowHeight: Rep[Int], windowWidth: Rep[Int], verticalPadding: Rep[Int], horizontalPadding: Rep[Int],
      verticalStride: Rep[Int], horizontalStride: Rep[Int]) =
    libFunction[CudnnStatusT]("cudnnSetPooling2dDescriptor", Unwrap(desc), Unwrap(mode), Unwrap(nanOpt),
      Unwrap(windowHeight), Unwrap(windowWidth), Unwrap(verticalPadding), Unwrap(horizontalPadding),
      Unwrap(verticalStride), Unwrap(horizontalStride))(Seq(0,1,2), Seq(0), Set[Int]())
  def cudnnGetPooling2dDescriptor(mode: Rep[PoolModes], nanOpt: Rep[NanOpt], windowHeight: Rep[Int],
      windowWidth: Rep[Int], verticalPadding: Rep[Int], horizontalPadding: Rep[Int], verticalStride: Rep[Int],
      horizontalStride: Rep[Int]): Rep[CudnnPoolingDescriptorT] = {
    val desc = getCudnnPoolingDescriptor
    cudnnCall(cudnnCreatePoolingDescriptor(desc))
    cudnnCall(cudnnSetPooling2dDescriptor(desc, mode, nanOpt, windowHeight, windowWidth, verticalPadding, horizontalPadding,
      verticalStride, horizontalStride))
    desc
  }
  def cudnnPoolingForward(handle: Rep[CudnnHandleT], poolingDesc: Rep[CudnnPoolingDescriptorT], alpha: Var[Float],
      xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]], beta: Var[Float], yDesc: Rep[CudnnTensorDescriptorT],
      y: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnPoolingForward", Unwrap(handle), Unwrap(poolingDesc), UnwrapV(alpha), Unwrap(xDesc),
      Unwrap(x), UnwrapV(beta), Unwrap(yDesc), Unwrap(y))(Seq(0,1,2,3,4,5,6), Seq(7), Set(2,5))
  def cudnnPoolingBackward(handle: Rep[CudnnHandleT], poolingDesc: Rep[CudnnPoolingDescriptorT], alpha: Var[Float],
      yDesc: Rep[CudnnTensorDescriptorT], y: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT], dy: Rep[Array[Float]],
      xDesc: Rep[CudnnTensorDescriptorT], xData: Rep[Array[Float]], beta: Var[Float], dxDesc: Rep[CudnnTensorDescriptorT],
      dx: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnPoolingBackward", Unwrap(handle), Unwrap(poolingDesc), UnwrapV(alpha), Unwrap(yDesc), Unwrap(y),
      Unwrap(dyDesc), Unwrap(dy), Unwrap(xDesc), Unwrap(xData), UnwrapV(beta), Unwrap(dxDesc),
      Unwrap(dx))(Seq(0,1,2,3,4,5,6,7,8,9,10), Seq(11), Set(2,9))

  // batchnorm
  abstract class CudnnBatchNormMode
  def knormActivation = cmacro[CudnnBatchNormMode]("CUDNN_BATCHNORM_PER_ACTIVATION")
  def knormSpatial = cmacro[CudnnBatchNormMode]("CUDNN_BATCHNORM_SPATIAL")
  def knormSpatialPersistent = cmacro[CudnnBatchNormMode]("CUDNN_BATCHNORM_SPATIAL_PERSISTENT")

  def cudnnBatchNormalizationForwardInference_(handle: Rep[CudnnHandleT], mode: Rep[CudnnBatchNormMode],
      alpha: Var[Float], beta: Var[Float], xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]],
      yDesc: Rep[CudnnTensorDescriptorT], y: Rep[Array[Float]], bnScalaBiasMeanVarDesc: Rep[CudnnTensorDescriptorT],
      bnScale: Rep[Array[Float]], bnBias: Rep[Array[Float]], estimateMean: Rep[Array[Float]],
      estimatedVariance:Rep[Array[Float]], epsilon: Rep[Double]) =
    libFunction[CudnnStatusT]("cudnnBatchNormalizationForwardInference", Unwrap(handle), Unwrap(mode),
      UnwrapV(alpha), UnwrapV(beta), Unwrap(xDesc), Unwrap(x), Unwrap(yDesc), Unwrap(y), Unwrap(bnScalaBiasMeanVarDesc),
      Unwrap(bnScale), Unwrap(bnBias), Unwrap(estimateMean), Unwrap(estimatedVariance),
      Unwrap(epsilon))(Seq(0,1,2,3,4,5,6,8,9,10,11,12), Seq(7), Set(2,3))

  def cudnnBatchNormalizationForwardTraining_(handle: Rep[CudnnHandleT], mode: Rep[CudnnBatchNormMode], alpha: Var[Float],
      beta: Var[Float], xDesc: Rep[CudnnTensorDescriptorT], x: Rep[Array[Float]], yDesc: Rep[CudnnTensorDescriptorT],
      y: Rep[Array[Float]], bnScalaBiasMeanVarDesc: Rep[CudnnTensorDescriptorT], bnScale: Rep[Array[Float]],
      bnBias: Rep[Array[Float]], exponentialAverageFactor: Rep[Double], resultRunningMean: Rep[Array[Float]],
      resultRunningVariance: Rep[Array[Float]], epsilon: Rep[Double], resultSaveMean: Rep[Array[Float]],
      resultSaveInvVariance: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnBatchNormalizationForwardTraining", Unwrap(handle), Unwrap(mode), UnwrapV(alpha),
      UnwrapV(beta), Unwrap(xDesc), Unwrap(x), Unwrap(yDesc), Unwrap(y), Unwrap(bnScalaBiasMeanVarDesc), Unwrap(bnScale),
      Unwrap(bnBias), Unwrap(exponentialAverageFactor), Unwrap(resultRunningVariance), Unwrap(resultRunningVariance),
      Unwrap(epsilon), Unwrap(resultSaveMean),
      Unwrap(resultSaveInvVariance))(Seq(0,1,2,3,4,5,6,8,9,10,12,13), Seq(7,12,13,15,16), Set(2,3))

  def cudnnBatchNormalizationBackward_(handle: Rep[CudnnHandleT], mode: Rep[CudnnBatchNormMode], alphaDataDiff: Var[Float],
      betaDataDiff: Var[Float], alphaParamDiff: Var[Float], betaParamDiff: Var[Float], xDesc: Rep[CudnnTensorDescriptorT],
      x: Rep[Array[Float]], dyDesc: Rep[CudnnTensorDescriptorT], dy: Rep[Array[Float]], dxDesc: Rep[CudnnTensorDescriptorT],
      dx: Rep[Array[Float]], bnScaleBiasDiffDesc: Rep[CudnnTensorDescriptorT], bnScale: Rep[Array[Float]],
      resultBnScaleDiff: Rep[Array[Float]], resultBnBiasDiff: Rep[Array[Float]], epsilon: Rep[Double],
      saveMean: Rep[Array[Float]], savedInvVariance: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnBatchNormalizationBackward", Unwrap(handle), Unwrap(mode), UnwrapV(alphaDataDiff),
      UnwrapV(betaDataDiff), UnwrapV(alphaParamDiff), UnwrapV(betaParamDiff), Unwrap(xDesc), Unwrap(x), Unwrap(dyDesc),
      Unwrap(dy), Unwrap(dxDesc), Unwrap(dx), Unwrap(bnScaleBiasDiffDesc), Unwrap(bnScale), Unwrap(resultBnScaleDiff),
      Unwrap(resultBnBiasDiff), Unwrap(epsilon), Unwrap(saveMean),
      Unwrap(savedInvVariance))(Seq(0,1,2,3,4,5,6,7,8,9,10,12,13,17,18), Seq(11,14,15), Set(2,3,4,5))

  // cudnnRNNDescriptor_t
  abstract class CudnnRNNInputMode
  def klinearInput = cmacro[CudnnRNNInputMode]("CUDNN_LINEAR_INPUT")
  def kskipInput = cmacro[CudnnRNNInputMode]("CUDNN_SKIP_INPUT")
  abstract class CudnnRNNDirectionMode
  def kunidirection = cmacro[CudnnRNNDirectionMode]("CUDNN_UNIDIRECTIONAL")
  def kbidirection = cmacro[CudnnRNNDirectionMode]("CUDNN_BIDIRECTIONAL")
  abstract class CudnnRNNMode
  def krnnRelu = cmacro[CudnnRNNMode]("CUDNN_RNN_RELU")
  def krnnTanh = cmacro[CudnnRNNMode]("CUDNN_RNN_TANH")
  def klstm = cmacro[CudnnRNNMode]("CUDNN_LSTM")
  def kgru = cmacro[CudnnRNNMode]("CUDNN_GRU")
  abstract class CudnnRNNAlgo
  def krnnAlgoStandard = cmacro[CudnnRNNAlgo]("CUDNN_RNN_ALGO_STANDARD")
  def krnnAlgoPersistStatic = cmacro[CudnnRNNAlgo]("CUDNN_RNN_ALGO_PERSIST_STATIC")
  def krnnAlgoPersistDynamic = cmacro[CudnnRNNAlgo]("CUDNN_RNN_ALGO_PERSIST_DYNAMIC")

  abstract class CudnnRNNDescriptorT
  def getCudnnRNNDescriptor = newStruct[CudnnRNNDescriptorT]("CudnnRNNDescriptorT")
  def cudnnCreateRNNDescriptor(desc: Rep[CudnnRNNDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreateRNNDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetRNNDescriptor_v6(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], hiddenSize: Rep[Int],
      numLayers: Rep[Int], dropoutDesc: Rep[CudnnDropoutDescriptorT], inputMode: Rep[CudnnRNNInputMode],
      direction: Rep[CudnnRNNDirectionMode], mode: Rep[CudnnRNNMode], algo: Rep[CudnnRNNAlgo], mathPrec: Rep[CuDNNDataType]) =
    libFunction[CudnnStatusT]("cudnnSetRNNDescriptor_v6", Unwrap(handle), Unwrap(rnnDesc), Unwrap(hiddenSize), Unwrap(numLayers),
      Unwrap(dropoutDesc), Unwrap(inputMode), Unwrap(direction), Unwrap(mode), Unwrap(algo),
      Unwrap(mathPrec))(Seq(0,1,4,5,6,7,8,9), Seq(1), Set[Int]())
  def cudnnGetRNNDescriptor(handle: Rep[CudnnHandleT], hiddenSize: Rep[Int], numLayers: Rep[Int],
      dropoutDesc: Rep[CudnnDropoutDescriptorT], inputMode: Rep[CudnnRNNInputMode], direction: Rep[CudnnRNNDirectionMode],
      mode: Rep[CudnnRNNMode], algo: Rep[CudnnRNNAlgo], mathPrec: Rep[CuDNNDataType]) = {
    val desc = getCudnnRNNDescriptor
    cudnnCall(cudnnCreateRNNDescriptor(desc))
    cudnnCall(cudnnSetRNNDescriptor_v6(handle, desc, hiddenSize, numLayers, dropoutDesc, inputMode, direction, mode, algo, mathPrec))
    desc
  }

  def cudnnSetRNNMatrixMathType(rnnDesc: Rep[CudnnRNNDescriptorT], mType: Rep[MathType]) =
    libFunction[CudnnStatusT]("cudnnSetRNNMatrixMathType", Unwrap(rnnDesc), Unwrap(mType))(Seq(0,1), Seq(0), Set[Int]())

  def cudnnGetRNNWorkspaceSize(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      xDesc: Rep[Array[CudnnTensorDescriptorT]], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetRNNWorkspaceSize", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength), Unwrap(xDesc),
      UnwrapV(sizeInBytes))(Seq(0,1,3), Seq(4), Set(4))

  def cudnnGetRNNTrainingReserveSize(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      xDesc: Rep[Array[CudnnTensorDescriptorT]], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetRNNTrainingReserveSize", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength),
      Unwrap(xDesc), UnwrapV(sizeInBytes))(Seq(0, 1, 3), Seq(4), Set(4))

  def cudnnRNNForwardInference_(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      xDescs: Rep[Array[CudnnTensorDescriptorT]], x: Rep[Array[Float]], hxDesc: Rep[CudnnTensorDescriptorT], hx: Rep[Array[Float]],
      cxDesc: Rep[CudnnTensorDescriptorT], cx: Rep[Array[Float]], wDesc: Rep[CudnnFilterDescriptorT], w: Rep[Array[Float]],
      yDescs: Rep[Array[CudnnTensorDescriptorT]], y: Rep[Array[Float]], hyDesc: Rep[CudnnTensorDescriptorT], hy: Rep[Array[Float]],
      cyDesc: Rep[CudnnTensorDescriptorT], cy: Rep[Array[Float]], wsData: Rep[Array[Float]], wsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnRNNForwardInference", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength),
      Unwrap(xDescs), Unwrap(x), Unwrap(hxDesc), Unwrap(hx), Unwrap(cxDesc), Unwrap(cx), Unwrap(wDesc), Unwrap(w),
      Unwrap(yDescs), Unwrap(y), Unwrap(hyDesc), Unwrap(hy), Unwrap(cyDesc), Unwrap(cy), Unwrap(wsData),
      Unwrap(wsSize))(Seq(0,1,3,4,5,6,7,8,9,10,11,13,15,17,18), Seq(12,14,16,17), Set[Int]())

  def cudnnRNNForwardTraining_(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      xDescs: Rep[Array[CudnnTensorDescriptorT]], x: Rep[Array[Float]], hxDesc: Rep[CudnnTensorDescriptorT], hx: Rep[Array[Float]],
      cxDesc: Rep[CudnnTensorDescriptorT], cx: Rep[Array[Float]], wDesc: Rep[CudnnFilterDescriptorT], w: Rep[Array[Float]],
      yDescs: Rep[Array[CudnnTensorDescriptorT]], y: Rep[Array[Float]], hyDesc: Rep[CudnnTensorDescriptorT], hy: Rep[Array[Float]],
      cyDesc: Rep[CudnnTensorDescriptorT], cy: Rep[Array[Float]], wsData: Rep[Array[Float]], wsSize: Rep[SizeT],
      rsData: Rep[Array[Float]], rsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnRNNForwardTraining", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength), Unwrap(xDescs),
      Unwrap(x), Unwrap(hxDesc), Unwrap(hx), Unwrap(cxDesc), Unwrap(cx), Unwrap(wDesc), Unwrap(w), Unwrap(yDescs), Unwrap(y),
      Unwrap(hyDesc), Unwrap(hy), Unwrap(cyDesc), Unwrap(cy), Unwrap(wsData), Unwrap(wsSize), Unwrap(rsData),
      Unwrap(rsSize))(Seq(0,1,3,4,5,6,7,8,9,10,11,13,15,17,18,19,20), Seq(12,14,16,19), Set[Int]())

  def cudnnRNNBackwardData_(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      yDescs: Rep[Array[CudnnTensorDescriptorT]], y: Rep[Array[Float]], dyDescs: Rep[Array[CudnnTensorDescriptorT]], dy: Rep[Array[Float]],
      dhyDesc: Rep[CudnnTensorDescriptorT], dhy: Rep[Array[Float]], dcyDesc: Rep[CudnnTensorDescriptorT], dcy: Rep[Array[Float]],
      wDesc: Rep[CudnnFilterDescriptorT], w: Rep[Array[Float]], hxDesc: Rep[CudnnTensorDescriptorT], hx: Rep[Array[Float]],
      cxDesc: Rep[CudnnTensorDescriptorT], cx: Rep[Array[Float]], dxDescs: Rep[Array[CudnnTensorDescriptorT]], dx: Rep[Array[Float]],
      dhxDesc: Rep[CudnnTensorDescriptorT], dhx: Rep[Array[Float]], dcxDesc: Rep[CudnnTensorDescriptorT], dcx: Rep[Array[Float]],
      wsData: Rep[Array[Float]], wsSize: Rep[SizeT], rsData: Rep[Array[Float]], rsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnRNNBackwardData", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength), Unwrap(yDescs),
      Unwrap(y), Unwrap(dyDescs), Unwrap(dy), Unwrap(dhyDesc), Unwrap(dhy), Unwrap(dcyDesc), Unwrap(dcy), Unwrap(wDesc),
      Unwrap(w), Unwrap(hxDesc), Unwrap(hx), Unwrap(cxDesc), Unwrap(cx), Unwrap(dxDescs), Unwrap(dx), Unwrap(dhxDesc),
      Unwrap(dhx), Unwrap(dcxDesc), Unwrap(dcx), Unwrap(wsData), Unwrap(wsSize), Unwrap(rsData),
      Unwrap(rsSize))(Seq(0,1,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,21,23,24,25,26), Seq(18,20,22,25), Set[Int]())

  def cudnnRNNBackwardWeights_(handle: Rep[CudnnHandleT], rnnDesc: Rep[CudnnRNNDescriptorT], seqLength: Rep[Int],
      xDescs: Rep[Array[CudnnTensorDescriptorT]], x: Rep[Array[Float]], hxDesc: Rep[CudnnTensorDescriptorT], hx: Rep[Array[Float]],
      yDescs: Rep[Array[CudnnTensorDescriptorT]], y: Rep[Array[Float]], wsData: Rep[Array[Float]], wsSize: Rep[SizeT],
      dwDesc: Rep[CudnnFilterDescriptorT], dw: Rep[Array[Float]], rsData: Rep[Array[Float]], rsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnRNNBackwardWeights", Unwrap(handle), Unwrap(rnnDesc), Unwrap(seqLength), Unwrap(xDescs),
      Unwrap(x), Unwrap(hxDesc), Unwrap(hx), Unwrap(yDescs), Unwrap(y), Unwrap(wsData), Unwrap(wsSize), Unwrap(dwDesc),
      Unwrap(dw), Unwrap(rsData), Unwrap(rsSize))(Seq(0,1,3,4,5,6,7,8,9,10,11,12,13,14), Seq(12), Set[Int]())


  // cudnnCTCLossDescriptor_t
  abstract class CudnnCTCLossDescriptorT
  def getCudnnCTCLossDescriptorT = newStruct[CudnnCTCLossDescriptorT]("cudnnCTCLossDescriptor_t")
  def cudnnCreateCTCLossDescriptor(desc: Rep[CudnnCTCLossDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreateCTCLossDescriptor", Unwrap(desc))(Seq[Int](), Seq(0), Set(0))
  def cudnnSetCTCLossDescriptor(desc: Rep[CudnnCTCLossDescriptorT], dtype: Rep[CuDNNDataType]) =
    libFunction[CudnnStatusT]("cudnnSetCTCLossDescriptor", Unwrap(desc), Unwrap(dtype))(Seq(0), Seq(0), Set[Int]())
  def cudnnGetCTCLossDescriptor(dtype: Rep[CuDNNDataType]) = {
    val desc = getCudnnCTCLossDescriptorT
    cudnnCall(cudnnCreateCTCLossDescriptor(desc))
    cudnnCall(cudnnSetCTCLossDescriptor(desc, dtype))
    desc
  }
  abstract class CudnnCTCLossAlgo
  def ctcDeterm = cmacro[CudnnCTCLossAlgo]("CUDNN_CTC_LOSS_ALGO_DETERMINISTIC")
  def ctcNonDeterm = cmacro[CudnnCTCLossAlgo]("CUDNN_CTC_LOSS_ALGO_NON_DETERMINISTIC")
  def cudnnGetCTCLossWorkspaceSize(handle: Rep[CudnnHandleT], probsDesc: Rep[CudnnTensorDescriptorT], gradDesc: Rep[CudnnTensorDescriptorT],
      labels: Rep[Array[Int]], labelLengths: Rep[Array[Int]], inputLengths: Rep[Array[Int]], algo: Rep[CudnnCTCLossAlgo],
      ctcLossDesc: Rep[CudnnCTCLossDescriptorT], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetCTCLossWorkspaceSize", Unwrap(handle), Unwrap(probsDesc), Unwrap(gradDesc),
      Unwrap(labels), Unwrap(labelLengths), Unwrap(inputLengths), Unwrap(algo), Unwrap(ctcLossDesc),
      UnwrapV(sizeInBytes))(Seq(0, 1, 2, 3, 4, 5, 6, 7), Seq(8), Set(8))
  def cudnnCTCLoss_(handle: Rep[CudnnHandleT], probDesc: Rep[CudnnTensorDescriptorT], probs: Rep[Array[Float]],
      hostLabels: Rep[Array[Int]], hostLabelLengths: Rep[Array[Int]],
      hostInputLengths: Rep[Array[Int]], cost: Rep[Array[Float]], gradientDesc: Rep[CudnnTensorDescriptorT],
      gradients: Rep[Array[Float]], algo: Rep[CudnnCTCLossAlgo], ctcLossDesc: Rep[CudnnCTCLossDescriptorT],
      wsData: Rep[Array[Float]], wsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnCTCLoss", Unwrap(handle), Unwrap(probDesc), Unwrap(probs), Unwrap(hostLabels),
      Unwrap(hostLabelLengths), Unwrap(hostInputLengths), Unwrap(cost), Unwrap(gradientDesc), Unwrap(gradients),
      Unwrap(algo), Unwrap(ctcLossDesc), Unwrap(wsData), Unwrap(wsSize))(Seq(0,1,2,3,4,5,7,9,10), Seq(6,8,10,11), Set[Int]())

  // Weight Gradient Mode
  abstract class CudnnWGradModeT
  def wGradModeAdd = cmacro[CudnnWGradModeT]("CUDNN_WGRAD_MODE_ADD")
  def wGradModeSet = cmacro[CudnnWGradModeT]("CUDNN_WGRAD_MODE_SET")

  // cudnnSeqDataAxis_t struct
  abstract class CudnnSeqDataAxisT
  def seqDataDimCount = cmacro[Int]("CUDNN_SEQDATA_DIM_COUNT")
  def seqDataTimeDim = cmacro[CudnnSeqDataAxisT]("CUDNN_SEQDATA_TIME_DIM")
  def seqDataBatchDim = cmacro[CudnnSeqDataAxisT]("CUDNN_SEQDATA_BATCH_DIM")
  def seqDataBeamDim = cmacro[CudnnSeqDataAxisT]("CUDNN_SEQDATA_BEAM_DIM")
  def seqDataVectDim = cmacro[CudnnSeqDataAxisT]("CUDNN_SEQDATA_VECT_DIM")

  def seqDataTimeDimIdx = cmacro[Int]("CUDNN_SEQDATA_TIME_DIM")
  def seqDataBatchDimIdx = cmacro[Int]("CUDNN_SEQDATA_BATCH_DIM")
  def seqDataBeamDimIdx = cmacro[Int]("CUDNN_SEQDATA_BEAM_DIM")
  def seqDataVectDimIdx = cmacro[Int]("CUDNN_SEQDATA_VECT_DIM")

  // cudnnSeqDataDescriptor_t struct
  abstract class CudnnSeqDataDescriptorT
  def getCudnnSeqDataDescriptorT = newStruct[CudnnSeqDataDescriptorT]("cudnnSeqDataDescriptor_t")
  def cudnnCreateSeqDataDescriptor(desc: Rep[CudnnSeqDataDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreateSeqDataDescriptor", Unwrap(desc))(Seq(), Seq(0), Set(0))

  def cudnnSetSeqDataDescriptor(desc: Rep[CudnnSeqDataDescriptorT], dataType: Rep[CuDNNDataType], nbDims: Rep[Int],
                                dimA: Rep[Array[Int]], axes: Rep[Array[CudnnSeqDataAxisT]],
                                seqLengthArraySize: Rep[SizeT],seqLengthArray: Rep[Array[Int]], paddingFill: Rep[Float]) =
    libFunction[CudnnStatusT]("cudnnSetSeqDataDescriptor", Unwrap(desc), Unwrap(dataType), Unwrap(nbDims), Unwrap(dimA),
      Unwrap(axes), Unwrap(seqLengthArraySize), Unwrap(seqLengthArray), Unwrap(paddingFill))(Seq(1, 2, 3, 4, 5, 6, 7), Seq(0), Set())

  // cudnnDropoutDescriptor_t
  abstract class CudnnDropoutDescriptorT
  def getCudnnDropoutDescriptorT = newStruct[CudnnDropoutDescriptorT]("cudnnDropoutDescriptor_t")

  def cudnnCreateDropoutDescriptor(desc: Rep[CudnnDropoutDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreateDropoutDescriptor", Unwrap(desc))(Seq(), Seq(0), Set(0))

  def cudnnDropoutGetStatesSize(handle: Rep[CudnnHandleT], dropoutBufSize: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnDropoutGetStatesSize", Unwrap(handle), UnwrapV(dropoutBufSize))(Seq(0), Seq(1), Set(1))

  def cudnnDropoutGetReserveSpaceSize(xDesc: Rep[CudnnTensorDescriptorT], sizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnDropoutGetReserveSpaceSize", Unwrap(xDesc), UnwrapV(sizeInBytes))(Seq(0), Seq(1), Set(1))

  def cudnnSetDropoutDescriptor(dropDesc: Rep[CudnnDropoutDescriptorT], handle: Rep[CudnnHandleT], dropoutRate: Rep[Float],
                                dropoutBuf: Rep[Array[Float]], dropoutBufSize: Rep[SizeT], seed: Rep[Long]) =
    libFunction[CudnnStatusT]("cudnnSetDropoutDescriptor", Unwrap(dropDesc), Unwrap(handle), Unwrap(dropoutRate), Unwrap(dropoutBuf),
      Unwrap(dropoutBufSize), Unwrap(seed))(Seq(0, 1, 2, 4, 5), Seq(3), Set[Int]())

  def krandTime = cmacro[Long]("time(NULL)")
  def cudnnGetDropoutDescriptor(handle: Rep[CudnnHandleT], prob: Rep[Float], state: Rep[Array[Float]], stateSize: Rep[SizeT],
      seed: Rep[Long]) = {
    val desc = getCudnnDropoutDescriptorT
    cudnnCall(cudnnCreateDropoutDescriptor(desc))
    cudnnCall(cudnnSetDropoutDescriptor(desc, handle, prob, state, stateSize, seed))
    desc
  }

  def cudnnDropoutForward(handle: Rep[CudnnHandleT], dropoutDesc: Rep[CudnnDropoutDescriptorT], xDesc: Rep[CudnnTensorDescriptorT],
      x: Rep[Array[Float]], yDesc: Rep[CudnnTensorDescriptorT], y: Rep[Array[Float]], rsData: Rep[Array[Float]],
      rsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnDropoutForward", Unwrap(handle), Unwrap(dropoutDesc), Unwrap(xDesc), Unwrap(x),
      Unwrap(yDesc), Unwrap(y), Unwrap(rsData), Unwrap(rsSize))(Seq(0,1,2,3,4,7), Seq(5,6), Set[Int]())

  def cudnnDropoutBackward(handle: Rep[CudnnHandleT], dropoutDesc: Rep[CudnnDropoutDescriptorT], dyDesc: Rep[CudnnTensorDescriptorT],
      dy: Rep[Array[Float]], dxDesc: Rep[CudnnTensorDescriptorT], dx: Rep[Array[Float]], rsData: Rep[Array[Float]], rsSize: Rep[SizeT]) =
    libFunction[CudnnStatusT]("cudnnDropoutBackward", Unwrap(handle), Unwrap(dropoutDesc), Unwrap(dyDesc), Unwrap(dy),
      Unwrap(dxDesc), Unwrap(dx), Unwrap(rsData), Unwrap(rsSize))(Seq(0,1,2,3,4,6,7), Seq(5,6), Set[Int]())

  // attenion modes
  // multiple Q's from same beam maps to the same K, V vectors (i.e., K, V beam size = 1)
  def attnQueryMapAllToOne = cmacro[Int]("CUDNN_ATTN_QUERYMAP_ALL_TO_ONE")
  // multiple Q's from same beam maps to different K, V (i.e., K, V beam size = Q beam size)
  def attnQueryMapOneToOne = cmacro[Int]("CUDNN_ATTN_QUERYMAP_ONE_TO_ONE")
  def attnDisableProjBias = cmacro[Int]("CUDNN_ATTN_DISABLE_PROJ_BIASES")
  def attnEnableProjBias = cmacro[Int]("CUDNN_ATTN_ENABLE_PROJ_BIASES")

  // cudnnAttnDescriptor_t
  abstract class CudnnAttnDescriptorT
  def getCudnnAttnDescriptorT = newStruct[CudnnAttnDescriptorT]("cudnnAttnDescriptor_t")

  def cudnnCreateAttnDescriptor(desc: Rep[CudnnAttnDescriptorT]) =
    libFunction[CudnnStatusT]("cudnnCreateAttnDescriptor", Unwrap(desc))(Seq(), Seq(0), Set(0))

  def cudnnSetAttnDescriptor(desc: Rep[CudnnAttnDescriptorT], attnMode: Rep[Int], nHeads: Rep[Int], smScaler: Rep[Double],
                             dataType: Rep[CuDNNDataType], computePrec: Rep[CuDNNDataType], mathType: Rep[MathType],
                             attnDropoutDesc: Rep[CudnnDropoutDescriptorT], postDropoutDesc: Rep[CudnnDropoutDescriptorT],
                             qSize: Rep[Int], kSize: Rep[Int], vSize: Rep[Int], qProjSize: Rep[Int], kProjSize: Rep[Int],
                             vProjSize: Rep[Int], oProjSize: Rep[Int], qoMaxSeqLength: Rep[Int], kvMaxSeqLength: Rep[Int],
                             maxBatchSize: Rep[Int], maxBeamSize: Rep[Int]) =
    libFunction[CudnnStatusT]("cudnnSetAttnDescriptor", Unwrap(desc), Unwrap(attnMode), Unwrap(nHeads), Unwrap(smScaler),
      Unwrap(dataType), Unwrap(computePrec), Unwrap(mathType), Unwrap(attnDropoutDesc), Unwrap(postDropoutDesc), Unwrap(qSize),
      Unwrap(kSize), Unwrap(vSize), Unwrap(qProjSize), Unwrap(kProjSize), Unwrap(vProjSize), Unwrap(oProjSize), Unwrap(qoMaxSeqLength),
      Unwrap(kvMaxSeqLength), Unwrap(maxBatchSize), Unwrap(maxBeamSize))(1 to 19, Seq(0), Set())

  def cudnnGetMultiHeadAttnBuffers(handle: Rep[CudnnHandleT], attnDesc: Rep[CudnnAttnDescriptorT], weightSizeInBytes: Var[SizeT]
                                   , workSpaceSizeInBytes: Var[SizeT], reserveSpaceSizeInBytes: Var[SizeT]) =
    libFunction[CudnnStatusT]("cudnnGetMultiHeadAttnBuffers", Unwrap(handle), Unwrap(attnDesc), UnwrapV(weightSizeInBytes),
      UnwrapV(workSpaceSizeInBytes), UnwrapV(reserveSpaceSizeInBytes))(Seq(0, 1), Seq(2, 3, 4), Set(2, 3, 4))

  def cudnnMultiHeadAttnForward(handle: Rep[CudnnHandleT], attnDesc: Rep[CudnnAttnDescriptorT], currIdx: Rep[Int],
                                loWinIdx: Rep[Array[Int]], hiWinIdx: Rep[Array[Int]], devSeqLengthsQO: Rep[Array[Int]],
                                devSeqLengthsKV: Rep[Array[Int]], qDesc: Rep[CudnnSeqDataDescriptorT], queries: Rep[Array[Float]],
                                residuals: Rep[Array[Float]], kDesc: Rep[CudnnSeqDataDescriptorT], keys: Rep[Array[Float]],
                                vDesc: Rep[CudnnSeqDataDescriptorT], values: Rep[Array[Float]], oDesc: Rep[CudnnSeqDataDescriptorT],
                                out: Rep[Array[Float]], weightSizeInBytes: Rep[SizeT], weights: Rep[Array[Float]],
                                workSpaceSizeInBytes: Rep[SizeT], workSpace: Rep[Array[Float]], reserveSpaceSizeInBytes: Rep[SizeT],
                                reserveSpace: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnMultiHeadAttnForward", Unwrap(handle), Unwrap(attnDesc), Unwrap(currIdx), Unwrap(loWinIdx),
      Unwrap(hiWinIdx), Unwrap(devSeqLengthsQO), Unwrap(devSeqLengthsKV), Unwrap(qDesc), Unwrap(queries), Unwrap(residuals),
      Unwrap(kDesc), Unwrap(keys), Unwrap(vDesc), Unwrap(values), Unwrap(oDesc), Unwrap(out), Unwrap(weightSizeInBytes),
      Unwrap(weights), Unwrap(workSpaceSizeInBytes), Unwrap(workSpace), Unwrap(reserveSpaceSizeInBytes), Unwrap(reserveSpace))((0 to 14) ++ Seq(16, 18, 20), Seq(15, 17, 19, 21), Set())

  def cudnnMultiHeadAttnBackwardData(handle: Rep[CudnnHandleT], attnDesc: Rep[CudnnAttnDescriptorT], loWinIdx: Rep[Array[Int]],
                                     hiWinIdx: Rep[Array[Int]], devSeqLengthsDQDO: Rep[Array[Int]], devSeqLengthsDKDV: Rep[Array[Int]],
                                     doDesc: Rep[CudnnSeqDataDescriptorT], dout: Rep[Array[Float]], dqDesc: Rep[CudnnSeqDataDescriptorT],
                                     dqueries: Rep[Array[Float]], queries: Rep[Array[Float]], dkDesc: Rep[CudnnSeqDataDescriptorT],
                                     dKeys: Rep[Array[Float]], keys: Rep[Array[Float]], dvDesc: Rep[CudnnSeqDataDescriptorT],
                                     dvalues: Rep[Array[Float]], values: Rep[Array[Float]], weightSizeInBytes: Rep[SizeT],
                                     weights: Rep[Array[Float]], workSpaceSizeInBytes: Rep[SizeT], workSpace: Rep[Array[Float]],
                                     reserveSpaceSizeInBytes: Rep[SizeT], reserveSpace: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnMultiHeadAttnBackwardData", Unwrap(handle), Unwrap(attnDesc), Unwrap(loWinIdx), Unwrap(hiWinIdx),
      Unwrap(devSeqLengthsDQDO), Unwrap(devSeqLengthsDKDV), Unwrap(doDesc), Unwrap(dout), Unwrap(dqDesc), Unwrap(dqueries),
      Unwrap(queries), Unwrap(dkDesc), Unwrap(dKeys), Unwrap(keys), Unwrap(dvDesc), Unwrap(dvalues), Unwrap(values),
      Unwrap(weightSizeInBytes), Unwrap(weights), Unwrap(workSpaceSizeInBytes), Unwrap(workSpace), Unwrap(reserveSpaceSizeInBytes),
      Unwrap(reserveSpace))((0 to 6) ++ Seq(8, 10, 11, 13, 14, 16, 17, 18, 19, 21), Seq(7, 9, 12, 15, 20, 22), Set())

  def cudnnMultiHeadAttnBackwardWeights(handle: Rep[CudnnHandleT], attnDesc: Rep[CudnnAttnDescriptorT], addGrad: Rep[CudnnWGradModeT],
                                        qDesc: Rep[CudnnSeqDataDescriptorT], queries: Rep[Array[Float]], kDesc: Rep[CudnnSeqDataDescriptorT],
                                        keys: Rep[Array[Float]], vDesc: Rep[CudnnSeqDataDescriptorT], values: Rep[Array[Float]],
                                        doDesc: Rep[CudnnSeqDataDescriptorT], dout: Rep[Array[Float]], weightSizeInBytes: Rep[SizeT],
                                        weights: Rep[Array[Float]], dweights: Rep[Array[Float]], workSpaceSizeInBytes: Rep[SizeT],
                                        workSpace: Rep[Array[Float]], reserveSpaceSizeInBytes: Rep[SizeT], reserveSpace: Rep[Array[Float]]) =
    libFunction[CudnnStatusT]("cudnnMultiHeadAttnBackwardWeights", Unwrap(handle), Unwrap(attnDesc), Unwrap(addGrad), Unwrap(qDesc),
      Unwrap(queries), Unwrap(kDesc), Unwrap(keys), Unwrap(vDesc), Unwrap(values), Unwrap(doDesc), Unwrap(dout), Unwrap(weightSizeInBytes),
      Unwrap(weights), Unwrap(dweights), Unwrap(workSpaceSizeInBytes), Unwrap(workSpace), Unwrap(reserveSpaceSizeInBytes),
      Unwrap(reserveSpace))((0 to 12) ++ Seq(14, 16), Seq(13, 15, 17), Set())
}
