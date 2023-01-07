package ethernet
import spinal.core._
import spinal.core.internals.Operator
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.fsm._

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
    DATA_TUSER_WIDTH: Int = 32,
    INPUT_BUFFER_DEPTH: Int = 4
)
case class MetaInterfaceGenerics(
    PACKET_LEN_WIDTH: Int = 13,
    IP_ADDR_WIDTH: Int = 32,
    PORT_WIDTH: Int = 16,
    MAC_ADDR_WIDTH: Int = 48
)

case class MetaInterface(config: MetaInterfaceGenerics) extends Bundle with IMasterSlave {
  val packetLen = UInt(config.PACKET_LEN_WIDTH bits)
  val dstMacAddr = Bits(config.MAC_ADDR_WIDTH bits)
  val dstIpAddr = Bits(config.IP_ADDR_WIDTH bits)
  val dstPort = Bits(config.PORT_WIDTH bits)
  val srcPort = Bits(config.PORT_WIDTH bits)
  val packetMTU = PacketMTUEnum()

  override def asMaster(): Unit = {
    out(packetLen, dstMacAddr, dstIpAddr, dstPort, srcPort, packetMTU)
  }
}

case class EthernetProtocolHeaderConstructor(initField: Map[String, Int])
    extends Bundle {
  def constructHeader(): Array[Bits] = {
    val fieldName: Array[String] = initField.keys.toArray
    val fieldWidth: Array[Int] = initField.values.toArray
    val protocolField = Array.tabulate[Bits](initField.size) { index =>
      val tmp: Bits =
        Bits(fieldWidth(index) bits) setName fieldName(index)
      tmp
    }
    protocolField
  }

}
// interface workshop
trait FrameHeader {
  val frameFieldInit: Map[String, Int]
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
  val frameFieldInit = Map(
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
  val frameFieldInit = Map(
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
    "checksum" -> 2 * 8,
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
  val frameFieldInit = Map(
    "srcPort" -> 2 * 8,
    "dstPort" -> 2 * 8,
    "len" -> 2 * 8,
    "checksum" -> 2 * 8
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
  val frameFieldInit = Map(
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

object HeaderGenerator {
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
    var offset: Int = 0
    val headerWidth: Int = getHeaderWidth(header)
    var width: Int = 0

    val tmp = header.flatten
      .reduce(_ ## _)
      .subdivideIn(8 bits)
      .reduce(_ ## _)
      .subdivideIn(256 bits)

    tmp
  }

  def generate(protocol: String, headers: Seq[Array[Bits]]): Vec[Bits] = {
    protocol match {
      case "UDP" =>
        val header = mergeHeader(headers)
        header
      case "ARP" =>
        val header = mergeHeader(headers)
        header

    }
  }
}

class HeaderGenerator(
    HeaderGeneratorConfig: HeaderGeneratorGenerics,
    arpCacheConfig: ArpCacheGenerics,
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
    val metaIn = slave Stream (MetaInterface(
      MetaConfig
    )) continueWhen (headerGeneratorState.stateReg === headerGeneratorState.IDLE)

    val arpCacheRW = master(ARPCacheInterface(arpCacheConfig))
    val arpReply = slave Stream (ARPCacheReplyInterface(arpCacheConfig))

    val headerAxisOut = master(Axi4Stream(headerAxisOutConfig))
  }
  val metaRegs = RegNextWhen(io.metaIn.payload, io.metaIn.fire)
  val ipLen = Reg(UInt(io.metaIn.payload.packetLen.getWidth bits)) init 0
  val udpLen = Reg(UInt(io.metaIn.payload.packetLen.getWidth bits)) init 0
  val ipId = Reg(UInt(16 bits)) init 0

  //  val needFragment = Reg(Bool()) init False
  //  val cnt = Counter(32)

  val headerGeneratorState = new StateMachine {
    val sendingCycle = Reg(UInt(4 bits)) init 2
    val sendingCnt = Counter(6)
    val IDLE: State = new State with EntryPoint {
      onEntry {
        io.headerAxisOut.last := False
      }
      whenIsActive {
        when(io.metaIn.fire) {
          goto(GET_MAC)
        }
      }
      onExit {
        ipLen := metaRegs.packetLen + 28
        udpLen := metaRegs.packetLen + 8
        ipId := ipId + 1
      }
    }

    val GET_MAC: State = new State {
      onEntry {
        io.arpCacheRW.ipAddrWrite := io.metaIn.dstIpAddr
        io.arpCacheRW.readEna := True
      }
      whenIsActive {
        io.arpCacheRW.readEna := False
        when(io.arpCacheRW.cacheHit) {
          goto(UDP_SEND)
        } elsewhen (io.arpCacheRW.cacheMiss) {
          goto(ARP_REQUEST_SEND)
        }
      }
    }
    val ARP_REQUEST_SEND: State = new State {
      val ethHeader = EthernetHeader(
        Array(
          B"48'xff_ff_ff_ff_ff_ff",
          HeaderGeneratorConfig.SRC_MAC_ADDR.asHex,
          B"16'x08_06"
        )
      )
      val arpHeader = ARPHeader(
        Array(
          HeaderGeneratorConfig.ARP_HTYPE.asHex,
          HeaderGeneratorConfig.ARP_PTYPE.asHex,
          HeaderGeneratorConfig.ARP_HLEN.asHex,
          HeaderGeneratorConfig.ARP_PLEN.asHex,
          B"16'x00_01",
          HeaderGeneratorConfig.SRC_MAC_ADDR.asHex,
          HeaderGeneratorConfig.SRC_IP_ADDR.asHex,
          "0".asHex,
          metaRegs.dstIpAddr
        )
      )
      val packetHeader =
        HeaderGenerator.generate("ARP", Seq(ethHeader, arpHeader))
      onEntry {}
    }

    val ARP_REQUEST_WAITING_REPLY: State = new State {
      val timer = Timeout(5 ms)
      onEntry {
        timer.clear()
      }
      whenIsActive {
        when(io.arpReply.fire) {
          io.arpCacheRW.writeEna.set()
          io.arpCacheRW.macAddrWrite := io.arpReply.macAddr
          io.arpCacheRW.ipAddrWrite := io.arpReply.ipAddr
          goto(UDP_SEND)
        } elsewhen (timer.stateRise) {
          goto(ARP_REQUEST_NO_REPLY)
        }
      }
    }

    val ARP_REQUEST_NO_REPLY: State = new State {}

    val ARP_REPLY_SEND: State = new State {}

    val UDP_SEND: State = new State {
      val ethHeader = EthernetHeader(
        Array(
          io.arpCacheRW.macAddrRead,
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
          ipLen,
          ipId,
          B"3'b010",
          HeaderGeneratorConfig.IPV4_TTL,
          B"8'x11",
          B"16'x0",
          HeaderGeneratorConfig.SRC_IP_ADDR,
          metaRegs.dstIpAddr
        )
      )
      val udpHeader = UDPHeader(
        Array(
          metaRegs.srcPort,
          metaRegs.dstPort,
          udpLen.asBits,
          B"16'x0"
        )
      )
      val packetHeader =
        HeaderGenerator.generate("UDP", Seq(ethHeader, ipv4Header, udpHeader))

      onEntry {
        sendingCnt.clear()
      }

      whenIsActive {
        io.headerAxisOut.data := (sendingCnt.value % 2 === 0) ?
          packetHeader(sendingCnt) |
          packetHeader(sendingCnt).rotateRight(
            10 * HeaderGeneratorConfig.OCTETS
          )
        io.headerAxisOut.keep := (sendingCnt.value % 2 === 0) ?
          B"32'xff_ff_ff_ff" | B"32'xff_c0_00_00"
        io.headerAxisOut.valid := True
        io.headerAxisOut.last := False
        when(io.headerAxisOut.fire) {
          sendingCnt.increment()
          when(sendingCnt.value % 2 === 1) {
            io.headerAxisOut.last := True
          }

          when(sendingCnt.valueNext === sendingCycle) {
            goto(IDLE)
          }
        }
      }
    }
  }

}
