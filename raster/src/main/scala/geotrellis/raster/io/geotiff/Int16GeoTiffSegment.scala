package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

abstract class Int16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asShortBuffer

  val size: Int = bytes.size / 2

  def get(i: Int): Short = buffer.get(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteConstantNoDataCellType | UByteConstantNoDataCellType | ByteCellType | UByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = s2b(get(i)) }
        arr
      case ShortConstantNoDataCellType | UShortConstantNoDataCellType | ShortCellType | UShortCellType =>
        bytes
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = s2f(get(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2s(f(getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    map(z => d2i(f(i2d(z))))
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2s(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2s(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }
}

class Int16RawGeoTiffSegment(bytes: Array[Byte]) extends Int16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i).toInt
  def getDouble(i: Int): Double = get(i).toDouble
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUShortOut(v: Int): Short = v.toShort
  protected def doubleToUShortOut(v: Double): Short = v.toShort
}

class Int16ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends Int16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = s2i(get(i))
  def getDouble(i: Int): Double = s2d(get(i))

  protected def intToUShortOut(v: Int): Short = i2s(v)
  protected def doubleToUShortOut(v: Double): Short = d2s(v)
}

class Int16UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedShortNoDataValue: Short)
    extends Int16GeoTiffSegment(bytes)
       with UserDefinedShortNoDataConversions {
  def getInt(i: Int): Int = uds2i(get(i))
  def getDouble(i: Int): Double = uds2d(get(i))

  protected def intToUShortOut(v: Int): Short = i2uds(v)
  protected def doubleToUShortOut(v: Double): Short = d2uds(v)
}
