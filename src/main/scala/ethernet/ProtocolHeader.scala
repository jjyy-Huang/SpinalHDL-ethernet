package ethernet
import spinal.core._
import spinal.core.internals.Operator
import spinal.core.sim._
import spinal.lib
import spinal.lib._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.fsm._

import scala.collection.mutable
import scala.math.pow

case class MetaInterfaceGenerics(
    PACKET_LEN_WIDTH: Int = 13,
    IP_ADDR_WIDTH: Int = 32,
    PORT_WIDTH: Int = 16,
    MAC_ADDR_WIDTH: Int = 48
)

case class MetaInterface(config: MetaInterfaceGenerics)
    extends Bundle
    with IMasterSlave {
  val dataLen = UInt(config.PACKET_LEN_WIDTH bits)
  val MacAddr = Bits(config.MAC_ADDR_WIDTH bits)
  val IpAddr = Bits(config.IP_ADDR_WIDTH bits)
  val dstPort = Bits(config.PORT_WIDTH bits)
  val srcPort = Bits(config.PORT_WIDTH bits)
  //  val packetMTU = PacketMTUEnum()

  override def asMaster(): Unit = {
    out(dataLen, MacAddr, IpAddr, dstPort, srcPort)
  }
}

case class EthernetProtocolHeaderConstructor(
    initField: mutable.LinkedHashMap[String, Int]
) extends Bundle {
  def constructHeader(): Array[Bits] = {
    val fieldName: Array[String] = initField.keys.toArray
    val fieldWidth: Array[Int] = initField.values.toArray
    val protocolField = List.tabulate[Bits](initField.size) { index =>
      val tmp: Bits =
        Bits(fieldWidth(index) bits) setName fieldName(index)
      tmp
    }
    protocolField.toArray
  }

}
// interface workshop
trait FrameHeader {
  val frameFieldInit: mutable.LinkedHashMap[String, Int]
  val header: Array[Bits]
}

object EthernetHeader {
  def apply(array: Array[Bits]): Array[Bits] = {
    val gen = new EthernetHeader
    require(
      array.length == gen.header.length,
      s"Initializing parameters not enough! Require ${gen.header.length} but gave ${array.length}"
    )
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := array(idx)
    }
    gen.header
  }

  def unapply(header: Bits): (Array[String], Array[Bits]) = {
    val gen = new EthernetHeader
    val bitWidth = gen.frameFieldInit.values.sum
    require(
      bitWidth == header.getWidth,
      s"Initializing parameters not enough! Require ${bitWidth} but gave ${header.getWidth}"
    )
    val asLittleEnd = header.subdivideIn(bitWidth / 8 slices).reduce(_ ## _)
    val tmp = asLittleEnd
      .sliceBy(gen.frameFieldInit.values.toList.reverse)
      .reverse
      .toArray
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := tmp(idx)
    }
    (gen.frameFieldInit.keys.toArray, gen.header)
  }
}
//methon in compoment

// data strcuture
case class EthernetHeader() extends Bundle with FrameHeader {
  val frameFieldInit = mutable.LinkedHashMap(
    "dstMAC" -> 6 * 8,
    "srcMAC" -> 6 * 8,
    "ethType" -> 2 * 8
  )
  val header: Array[Bits] =
    EthernetProtocolHeaderConstructor(frameFieldInit).constructHeader()
}

object IPv4Header {
  def apply(array: Array[Bits]): Array[Bits] = {
    val gen = new IPv4Header
    require(
      array.length == gen.header.length,
      s"Initializing parameters not enough! Require ${gen.header.length} but gave ${array.length}"
    )
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := array(idx)
    }
    gen.header
  }

  def unapply(header: Bits): (Array[String], Array[Bits]) = {
    val gen = new IPv4Header
    val bitWidth = gen.frameFieldInit.values.sum
    require(
      bitWidth == header.getWidth,
      s"Initializing parameters not enough! Require ${bitWidth} but gave ${header.getWidth}"
    )
    val asLittleEnd = header.subdivideIn(bitWidth / 8 slices).reduce(_ ## _)
    val tmp = asLittleEnd
      .sliceBy(gen.frameFieldInit.values.toList.reverse)
      .reverse
      .toArray
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := tmp(idx)
    }
    (gen.frameFieldInit.keys.toArray, gen.header)
  }
}

case class IPv4Header() extends Bundle with FrameHeader {
  val frameFieldInit = mutable.LinkedHashMap(
    "protocolVersion" -> 4,
    "internetHeaderLength" -> 4,
    "differentiatedServicesCodePoint" -> 6,
    "explicitCongestionNotification" -> 2,
    "ipLen" -> 2 * 8,
    "identification" -> 2 * 8,
    "flags" -> 3,
    "fragmentOffset" -> 13,
    "ttl" -> 8,
    "protocol" -> 8,
    "ipChecksum" -> 2 * 8,
    "srcAddr" -> 4 * 8,
    "dstAddr" -> 4 * 8
  )
  val header: Array[Bits] =
    EthernetProtocolHeaderConstructor(frameFieldInit).constructHeader()
}

object UDPHeader {
  def apply(array: Array[Bits]): Array[Bits] = {
    val gen = new UDPHeader
    require(
      array.length == gen.header.length,
      s"Initializing parameters not enough! Require ${gen.header.length} but gave ${array.length}"
    )
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := array(idx)
    }
    gen.header
  }

  def unapply(header: Bits): (Array[String], Array[Bits]) = {
    val gen = new UDPHeader
    val bitWidth = gen.frameFieldInit.values.sum
    require(
      bitWidth == header.getWidth,
      s"Initializing parameters not enough! Require ${bitWidth} but gave ${header.getWidth}"
    )
    val asLittleEnd = header.subdivideIn(bitWidth / 8 slices).reduce(_ ## _)
    val tmp = asLittleEnd
      .sliceBy(gen.frameFieldInit.values.toList.reverse)
      .reverse
      .toArray
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := tmp(idx)
    }
    (gen.frameFieldInit.keys.toArray, gen.header)
  }

}

case class UDPHeader() extends Bundle with FrameHeader {
  val frameFieldInit = mutable.LinkedHashMap(
    "srcPort" -> 2 * 8,
    "dstPort" -> 2 * 8,
    "len" -> 2 * 8,
    "udpChecksum" -> 2 * 8
  )
  val header: Array[Bits] =
    EthernetProtocolHeaderConstructor(frameFieldInit).constructHeader()
}

object ARPHeader {
  def apply(array: Array[Bits]): Array[Bits] = {
    val gen = new ARPHeader
    require(
      array.length == gen.header.length,
      s"Initializing parameters not enough! Require ${gen.header.length} but gave ${array.length}"
    )
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := array(idx)
    }
    gen.header
  }

  def unapply(header: Bits): (Array[String], Array[Bits]) = {
    val gen = new ARPHeader
    val bitWidth = gen.frameFieldInit.values.sum
    require(
      bitWidth == header.getWidth,
      s"Initializing parameters not enough! Require ${bitWidth} but gave ${header.getWidth}"
    )
    val asLittleEnd = header.subdivideIn(bitWidth / 8 slices).reduce(_ ## _)
    val tmp = asLittleEnd
      .sliceBy(gen.frameFieldInit.values.toList.reverse)
      .reverse
      .toArray
    gen.header.zipWithIndex.foreach { case (data, idx) =>
      data := tmp(idx)
    }
    (gen.frameFieldInit.keys.toArray, gen.header)
  }
}

case class ARPHeader() extends FrameHeader {
  val frameFieldInit = mutable.LinkedHashMap(
    "hardwareType" -> 2 * 8,
    "protocolType" -> 2 * 8,
    "hardwareLen" -> 8,
    "protocolLen" -> 8,
    "operation" -> 2 * 8,
    "senderHardwareAddr" -> 6 * 8,
    "senderProtocolAddr" -> 4 * 8,
    "targetHardwareAddr" -> 6 * 8,
    "targetProtocolAddr" -> 4 * 8
  )
  val header: Array[Bits] =
    EthernetProtocolHeaderConstructor(frameFieldInit).constructHeader()
}
