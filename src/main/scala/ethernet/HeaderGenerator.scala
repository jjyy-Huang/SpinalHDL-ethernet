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

case class HeaderGeneratorGenerics(
    SRC_IP_ADDR: String = "c0a80101",
    SRC_MAC_ADDR: String = "123456789abc",
    IPV4_IHL: String = "5",
    IPV4_DSCP: String = "0",
    IPV4_ECN: String = "0",
    IPV4_TTL: String = "40",
    ARP_HTYPE: String = "0001",
    ARP_PTYPE: String = "0800",
    ARP_HLEN: String = "06",
    ARP_PLEN: String = "04",
    DATA_WIDTH: Int = 256,
    DATA_BYTE_CNT: Int = 32,
    OCTETS: Int = 8,
    DATA_USE_TLAST: Boolean = true,
    DATA_USE_TUSER: Boolean = true,
    DATA_USE_TKEEP: Boolean = true,
    DATA_USE_TSTRB: Boolean = false,
    DATA_TUSER_WIDTH: Int = 1,
    INPUT_BUFFER_DEPTH: Int = 4
)
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
  val dstMacAddr = Bits(config.MAC_ADDR_WIDTH bits)
  val dstIpAddr = Bits(config.IP_ADDR_WIDTH bits)
  val dstPort = Bits(config.PORT_WIDTH bits)
  val srcPort = Bits(config.PORT_WIDTH bits)
  val packetMTU = PacketMTUEnum()

  override def asMaster(): Unit = {
    out(dataLen, dstMacAddr, dstIpAddr, dstPort, srcPort, packetMTU)
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

class HeaderGenerator(
    HeaderGeneratorConfig: HeaderGeneratorGenerics,
//    arpCacheConfig: ArpCacheGenerics,
    MetaConfig: MetaInterfaceGenerics
) extends Component {
  val headerAxisOutConfig = Axi4StreamConfig(
    dataWidth = HeaderGeneratorConfig.DATA_BYTE_CNT,
    userWidth = HeaderGeneratorConfig.DATA_TUSER_WIDTH,
    useKeep = HeaderGeneratorConfig.DATA_USE_TKEEP,
    useLast = HeaderGeneratorConfig.DATA_USE_TLAST,
    useUser = HeaderGeneratorConfig.DATA_USE_TUSER
  )
  val io = new Bundle {
    val metaIn = slave Stream MetaInterface(MetaConfig)

    val headerAxisOut = master(Axi4Stream(headerAxisOutConfig))
  }
  val ipLenReg = Reg(UInt(16 bits)) init 0
  val udpLenReg = Reg(UInt(16 bits)) init 0
  val ipIdReg = Reg(UInt(16 bits)) init 0
  val ipFragmentOffset = Reg(UInt(13 bits)) init 0
  val ipChecksumReg = Reg(UInt(16 bits)) init 0
  val udpChecksumReg = Reg(UInt(16 bits)) init 0
  val generateDone = Bool()
  val ipFlags = Bits(3 bits)

  val packetLenMax = ((1500+14) / HeaderGeneratorConfig.DATA_BYTE_CNT.toFloat).ceil.toInt
  val packetLen = Reg(UInt(log2Up(packetLenMax) bits)) init 0
  println(log2Up(packetLenMax))

  val dataLoaded = Reg(Bool()) init False
  val metaRegs = Reg(MetaInterface(MetaConfig))

  val sendingCycle = U(1, 1 bits) // depend on (max frame width) / (data out width) - 1
  val sendingCnt = Counter(2)

  val metaSetValid = !dataLoaded | generateDone
  io.metaIn.ready := True & metaSetValid
  when(io.metaIn.fire) {
    dataLoaded := True
  } elsewhen (generateDone) {
    dataLoaded := False
  }

  val needFragment = Reg(Bool()) init False
  ipFlags := B"2'b0" ## needFragment
  when(io.metaIn.fire) {
    metaRegs.srcPort := io.metaIn.srcPort
    metaRegs.dstPort := io.metaIn.dstPort
    metaRegs.dstIpAddr := io.metaIn.dstIpAddr
    metaRegs.dstMacAddr := io.metaIn.dstMacAddr
    when(io.metaIn.dataLen > 1472) {
      metaRegs.dataLen := io.metaIn.dataLen - 1472
      needFragment := True
      ipLenReg := 1500
      udpLenReg := 1480
      ipFragmentOffset := 0
      packetLen := 48 - 1
    } otherwise {
      needFragment := False
      ipLenReg := io.metaIn.dataLen.resize(16) + 28
      udpLenReg := io.metaIn.dataLen.resize(16) + 8
      ipFragmentOffset := 0
      packetLen := ((((io.metaIn.dataLen + 42) % HeaderGeneratorConfig.DATA_BYTE_CNT) =/= 0) ? U(1, log2Up(packetLenMax) bits) | U(0, log2Up(packetLenMax) bits)) + ((io.metaIn.dataLen + 42) >> 5).takeLow(log2Up(packetLenMax)).asUInt - 1
    }
  } elsewhen (io.headerAxisOut.lastFire & needFragment) {
    when(metaRegs.dataLen > 1480) {
      needFragment := True
      ipLenReg := 1500
      metaRegs.dataLen := metaRegs.dataLen - 1480
      ipFragmentOffset := ipFragmentOffset + 185
      packetLen := 48 - 1
    } otherwise {
      needFragment := False
      ipLenReg := metaRegs.dataLen.resize(16) + 20
      ipFragmentOffset := ipFragmentOffset + 185
      packetLen := ((((io.metaIn.dataLen + 34) % HeaderGeneratorConfig.DATA_BYTE_CNT) =/= 0) ? U(1, log2Up(packetLenMax) bits) | U(0, log2Up(packetLenMax) bits)) + ((io.metaIn.dataLen + 42) >> 5).takeLow(log2Up(packetLenMax)).asUInt - 1
    }
  }

  when(generateDone) {
    ipIdReg := ipIdReg + 1
  }

  val ethHeader = EthernetHeader(
    Array(
      metaRegs.dstMacAddr,
      HeaderGeneratorConfig.SRC_MAC_ADDR.asHex,
      B"16'x08_00"
    )
  )

  val ipv4Header = IPv4Header(
    Array(
      B"4'x4",
      HeaderGeneratorConfig.IPV4_IHL.asHex,
      HeaderGeneratorConfig.IPV4_DSCP.asHex,
      HeaderGeneratorConfig.IPV4_ECN.asHex,
      ipLenReg.asBits,
      ipIdReg.asBits,
      ipFlags,
      ipFragmentOffset.asBits,
      HeaderGeneratorConfig.IPV4_TTL.asHex,
      B"8'x11",
      ipChecksumReg.asBits,
      HeaderGeneratorConfig.SRC_IP_ADDR.asHex,
      metaRegs.dstIpAddr
    )
  )
  val udpHeader = UDPHeader(
    Array(
      metaRegs.srcPort,
      metaRegs.dstPort,
      udpLenReg.asBits,
      udpChecksumReg.asBits
    )
  )

  val ethIpUdpHeader = generate(Seq(ethHeader, ipv4Header, udpHeader))
  val ethIpHeader = generate(Seq(ethHeader, ipv4Header))
  val ethIpUdpLastShiftByte = calLastShiftByte(
    Seq(ethHeader, ipv4Header, udpHeader)
  )
  val ethIpLastShiftByte = calLastShiftByte(Seq(ethHeader, ipv4Header))
  val udpSent = Reg(Bool()) init False

  when(!udpSent) {
    io.headerAxisOut.data := (sendingCnt.value % 2 === 0) ?
      ethIpUdpHeader(sendingCnt) |
      ethIpUdpHeader(sendingCnt).rotateRight(
        ethIpUdpLastShiftByte * HeaderGeneratorConfig.OCTETS
      )
    //  32'xffff_ffff or 32'xffc0_0000
    io.headerAxisOut.keep := (sendingCnt.value % 2 === 0) ?
      Bits(HeaderGeneratorConfig.DATA_BYTE_CNT bits).setAll() |
      B(
        HeaderGeneratorConfig.DATA_BYTE_CNT bits,
        (HeaderGeneratorConfig.DATA_BYTE_CNT - 1 downto HeaderGeneratorConfig.DATA_BYTE_CNT - ethIpUdpLastShiftByte) -> true,
        default -> false
      )
    io.headerAxisOut.user := (sendingCnt.value % 2 === 0) ?
      generateControlSignal(0, True, packetLen) | generateControlSignal(10, False, packetLen)
  } otherwise {
    io.headerAxisOut.data := (sendingCnt.value % 2 === 0) ?
      ethIpHeader(sendingCnt) |
      ethIpHeader(sendingCnt).rotateRight(
        ethIpLastShiftByte * HeaderGeneratorConfig.OCTETS
      )
    //  32'xffff_ffff or 32'xc000_0000
    io.headerAxisOut.keep := (sendingCnt.value % 2 === 0) ?
      Bits(HeaderGeneratorConfig.DATA_BYTE_CNT bits).setAll() |
      B(
        HeaderGeneratorConfig.DATA_BYTE_CNT bits,
        (HeaderGeneratorConfig.DATA_BYTE_CNT - 1 downto HeaderGeneratorConfig.DATA_BYTE_CNT - ethIpLastShiftByte) -> true,
        default -> false
      )
    io.headerAxisOut.user := (sendingCnt.value % 2 === 0) ?
      generateControlSignal(0, True, packetLen) | generateControlSignal(2, False, packetLen)
  }

  io.headerAxisOut.valid := dataLoaded
  when(io.headerAxisOut.fire) {
    sendingCnt.increment()
    when(sendingCnt.value % 2 === 1) {
      io.headerAxisOut.last := True
    } otherwise {
      io.headerAxisOut.last := False
    }
    when(sendingCnt.value === sendingCycle & needFragment) {
      udpSent := True
      generateDone := False
    } elsewhen (sendingCnt.value === sendingCycle & !needFragment) {
      generateDone := True
      udpSent := False
    } otherwise {
      generateDone := False
    }
  } otherwise {
    io.headerAxisOut.last := False
    generateDone := False
  }

  def getHeaderWidth(header: Seq[Array[Bits]]): Int = {
    var len: Int = 0
    header.foreach { protocol =>
      protocol.foreach { data =>
        len += data.getBitsWidth
      }
    }
    len
  }

  def mergeHeader(header: Seq[Array[Bits]]): Vec[Bits] = {
    val headerWidth: Int = getHeaderWidth(header)

    val tmp = header.flatten
      .reduce(_ ## _)
      .subdivideIn(HeaderGeneratorConfig.OCTETS bits)
      .reduce(_ ## _)
      .resize(
        ((headerWidth / HeaderGeneratorConfig.DATA_WIDTH.toFloat).ceil * HeaderGeneratorConfig.DATA_WIDTH).toInt bits
      )
      .subdivideIn(HeaderGeneratorConfig.DATA_WIDTH bits)

    tmp
  }

  def generate(headers: Seq[Array[Bits]]): Vec[Bits] = {
    val header = mergeHeader(headers)
    header
  }

  def calLastShiftByte(header: Seq[Array[Bits]]): Int = {
    val headerWidth: Int = getHeaderWidth(header)
    val ret =
      (headerWidth % HeaderGeneratorConfig.DATA_WIDTH) / HeaderGeneratorConfig.OCTETS
    ret
  }

  /**
  *   31  24      19          13     12      7    0
  *   ┌────┬──────┬───────────┬──────┬───────┬────┐
  *   │0x5A│      │ packetLen │ LSel │ Shift │0xA5│
  *   └────┴──────┴───────────┴──────┴───────┴────┘
  **/
  def generateControlSignal(shiftLen: Int, lastSelect: Bool, packetLen: UInt): Bits = {
    val controlHeader = B"8'xa5"
    val controlTail = B"8'x5a"
    val shiftBits = B(shiftLen, 5 bits)
    val ret = controlTail ## B(0, 4 bits) ## packetLen ## lastSelect ## shiftBits ## controlHeader
    ret
  }
}
