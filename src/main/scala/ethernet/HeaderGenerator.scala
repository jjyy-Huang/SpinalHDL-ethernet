package ethernet

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import scala.math._
import ethernet.HeaderProtocolEnum._

import java.util.Calendar


case class EthernetHeader(OCTETS: Int = 8, HEADER_WIDTH : Int = 32) extends Bundle {
  val dstMAC = Bits(6*OCTETS bits) setAsReg() init 0
  val srcMAC = Bits(6*OCTETS bits) setAsReg() init 0
  val ethType = Bits(2*OCTETS bits) setAsReg() init 0

  def setDstMAC(dstMAC: Bits): Unit = {
    this.dstMAC := dstMAC
  }

  def setBroadcast(): Unit = {
    this.dstMAC := B"48'xff_ff_ff_ff_ff_ff"
  }

  def setSrcMAC(srcMAC: Bits): Unit = {
    this.srcMAC := srcMAC
  }

  def setAsIPv4(): Unit = {
    this.ethType := B"16'x08_00"
  }

  def setAsArp(): Unit = {
    this.ethType := B"16'x08_06"
  }

//  def apply()

}

case class IPv4Header(OCTETS: Int = 8, HEADER_WIDTH : Int = 32) extends Bundle {

  val protocolVersion = Bits(4 bits)  setAsReg() init 0
  val internetHeaderLength = Bits(4 bits) setAsReg() init 0
  val differentiatedServicesCodePoint = Bits(6 bits)  setAsReg() init 0
  val explicitCongestionNotification = Bits(2 bits) setAsReg() init 0
  val ipLen = Bits(2*OCTETS bits) setAsReg() init 0
  val identification = Bits(2*OCTETS bits)  setAsReg() init 0
  val flags = Bits(3 bits)  setAsReg() init 0
  val fragmentOffset = Bits(13 bits)  setAsReg() init 0
  val ttl = Bits(OCTETS bits) setAsReg() init 0
  val protocol = Bits(OCTETS bits)  setAsReg() init 0
  val checksum = Bits(2*OCTETS bits)  setAsReg() init 0
  val srcAddr = Bits(4*OCTETS bits) setAsReg() init 0
  val dstAddr = Bits(4*OCTETS bits) setAsReg() init 0


  def setProtocolVersion(): Unit = {
    this.protocolVersion := B"4'x4"
  }

  def setInternetHeaderLength(iHL: Bits): Unit = {
    this.internetHeaderLength := iHL
  }

  def setDifferentiatedServicesCodePoint(dSCP: Bits): Unit = {
    this.differentiatedServicesCodePoint := dSCP
  }

  def setExplicitCongestionNotification(eCN: Bits): Unit = {
    this.explicitCongestionNotification := eCN
  }

  def setIpLen(ipLen: Bits): Unit = {
    this.ipLen := ipLen
  }

  def setIdentification(id: Bits): Unit = {
    this.identification := id
  }

  def setFlags(flags: Bits): Unit = {
    this.flags := flags
  }

  def setFragmentOffset(fragmentOffset: Bits): Unit = {
    this.fragmentOffset := fragmentOffset
  }

  def setTTL(ttl: Bits): Unit = {
    this.ttl := ttl
  }

  def setProtocol(protocol: Bits): Unit = {
    this.protocol := protocol
  }

  def setAsUDP(): Unit = {
    this.protocol := B"8'x11"
  }

  def setChecksum(checksum : Bits): Unit = {
    this.checksum := checksum
  }

  def setSrcAddr(srcAddr: Bits): Unit = {
    this.srcAddr := srcAddr
  }

  def setDstAddr(dstAddr: Bits): Unit = {
    this.dstAddr := dstAddr
  }

}

case class UDPHeader(OCTETS : Int = 8, HEADER_WIDTH : Int = 32) extends Bundle {

  val srcPort = Bits(2*OCTETS bits) setAsReg() init 0
  val dstPort = Bits(2*OCTETS bits) setAsReg() init 0
  val len = Bits(2*OCTETS bits) setAsReg() init 0
  val checksum = Bits(2*OCTETS bits)  setAsReg() init 0

  def dontCareSrcPort(): Unit ={
    srcPort := B"16'x00_00"
  }

  def setSrcPort(srcPort : Bits): Unit = {
    this.srcPort := srcPort
  }

  def setDstPort(dstPort : Bits): Unit = {
    this.dstPort := dstPort
  }

  def setLen(len : Bits): Unit = {
    this.len := len
  }

  def dontCareChecksum(): Unit = {
    this.checksum := B"16'x00_00"
  }

  def setChecksum(checksum : Bits): Unit = {
    this.checksum := checksum
  }

}

case class ARPHeader(OCTETS : Int = 8, HEADER_WIDTH : Int = 32) extends Bundle {

  val hardwareType = Bits(2*OCTETS bits)  setAsReg() init 0
  val protocolType = Bits(2*OCTETS bits)  setAsReg() init 0
  val hardwareLen = Bits(OCTETS bits) setAsReg() init 0
  val protocolLen = Bits(OCTETS bits) setAsReg() init 0
  val operation = Bits(2*OCTETS bits) setAsReg() init 0
  val senderHardwareAddr = Bits(6*OCTETS bits)  setAsReg() init 0
  val senderProtocolAddr = Bits(4*OCTETS bits)  setAsReg() init 0
  val targetHardwareAddr = Bits(6*OCTETS bits)  setAsReg() init 0
  val targetProtocolAddr = Bits(4*OCTETS bits)  setAsReg() init 0

  def setHardwareType(hType : Bits): Unit = {
    this.hardwareType := hType
  }

  def setProtocolType(pType : Bits): Unit = {
    this.protocolType := pType
  }

  def setHardwareLen(hLen : Bits): Unit = {
    this.hardwareLen := hLen
  }

  def setProtocolLen(pLen : Bits): Unit = {
    this.protocolLen := pLen
  }

  def arpRequest(): Unit = {
    this.operation := B"16'x00_01"
  }
  def arpReply(): Unit = {
    this.operation := B"16'x00_02"
  }

  def setSenderHardwareAddr(sHA : Bits): Unit = {
    this.senderHardwareAddr := sHA
  }

  def setSenderProtocolAddr(sPA : Bits): Unit = {
    this.senderProtocolAddr := sPA
  }

  def setTargetHardwareAddr(tHA : Bits): Unit = {
    this.targetHardwareAddr := tHA
  }

  def setTargetProtocolAddr(tPA : Bits): Unit = {
    this.targetProtocolAddr := tPA
  }

}

object HeaderGeneratorErrorEnum extends SpinalEnum {
  // TODO
}
case class FrameHeader(OCTETS : Int = 8) extends Bundle {
  val eth = EthernetHeader()
  val ipv4 = IPv4Header()
  val udp = UDPHeader()
  val arp = ARPHeader()

  def getHeaderWidth(header : Seq[Bundle]): Int = {
    var len : Int = 0
    header.foreach { protocol =>
      protocol.elements.foreach { case(filed, data) =>
        len += data.getBitsWidth
      }
    }
    len
  }
  def assignAsBigEnd(data : Bits) : Bits = {
    val res = Bits(data.getBitsWidth bits)
    res := data.subdivideIn(OCTETS bits, false).reverse.asBits()
    res
  }
  def mergeHeader(header : Seq[Bundle]): Bits = {
    var offset: Int = 0
    val headerWidth : Int = getHeaderWidth(header)
    var width: Int = 0

    val tmp = Bits(pow(2, log2Up(headerWidth)).toInt bits) setAsReg() init 0
    header.foreach { protocol =>
      protocol.elements.foreach { case (fieldName, value) =>
        width = value.getBitsWidth
        tmp(offset, width bits) := assignAsBigEnd(value.asBits)
        offset += width
      }
    }
    tmp
  }

  def generate(protocol: String): Bits = {
    protocol match {
      case "UDP" => val header = mergeHeader(Seq(eth, ipv4, udp))
        header
      case "ARP" => val header = mergeHeader(Seq(eth, arp))
        header

    }
  }
}

class HeaderGenerator(headerConfig : TxGenerics, arpCacheConfig : ArpCacheGenerics) extends Component {
  val io = new Bundle {
    val metaIn = slave Stream TxConfiguration(headerConfig)
    val headerOut = master Stream Bits(headerConfig.DATA_WIDTH bits)
    val finishGenerate = out Bool() setAsReg() init False
    val headerType = out (HeaderProtocolEnum())
    val packetLen = out UInt(headerConfig.PACKET_LEN_WIDTH bits) setAsReg() init 0

    val writemac = in Bits(48 bits)
    val writeean = in Bool()
  }

  val arpCache = new ARPCache(arpCacheConfig)
  val metaReg = RegNextWhen(io.metaIn.payload, io.metaIn.fire)
  val header = FrameHeader()
  val ipLen = Reg(UInt(headerConfig.PACKET_LEN_WIDTH bits)) init 0
  val udpLen = Reg(UInt(headerConfig.PACKET_LEN_WIDTH bits)) init 0
  val ipId = Reg(UInt(16 bits)) init 0
  val ipSum = UInt(32 bits)
  val ipChecksum = Bits(16 bits)
  ipSum := U"32'x4500" + ipLen.resize(32) + ipId.resize(32) + U"32'x8011"
  ipChecksum := ~(ipSum.takeHigh(16).asUInt + ipSum.takeLow(16).asUInt).asBits

  arpCache.io.ipAddrIn := io.metaIn.dstIpAddr
  arpCache.io.macAddrIn := io.writemac
  arpCache.io.writeEna := io.writeean


  val headerGenerateState = new StateMachine {
    val udpFrame = header.generate("UDP").subdivideIn(2 slices)
    val arpFrame = header.generate("ARP").subdivideIn(2 slices)

    val sendingCnt = Counter(0 until 2)
    io.headerType := HeaderProtocolEnum.none
    io.metaIn.ready := False
    io.headerOut.valid := False
    io.headerOut.payload := 0

    val IDLE: State = new State with EntryPoint {
      onEntry {
        io.finishGenerate := False
      }
      whenIsActive {
        sendingCnt.clear()
        io.metaIn.ready := True
        when(io.metaIn.fire) {
          goto(GET_MAC)
        }
      }
    }

    val GET_MAC: State = new State {
      arpCache.io.readEna := False
      onEntry {
        arpCache.io.readEna := True
        ipLen := io.metaIn.payload.packetLen.resized + U(28, headerConfig.PACKET_LEN_WIDTH bits)
        udpLen := io.metaIn.payload.packetLen.resized + U(8, headerConfig.PACKET_LEN_WIDTH bits)
        ipId := ipId + 1
      }
      whenIsActive {
        when(arpCache.io.cacheHit) {
          goto(UDP_PACKET_PREPARE)
        } elsewhen (arpCache.io.cacheMiss) {
          goto(ARP_REQUEST_PREPARE)
        }
      }
    }


    //    val ARP_REPLY : State = new State {
    //
    //    }

    val ARP_REQUEST_PREPARE : State = new State{
      onEntry {
        io.packetLen := U(64, headerConfig.PACKET_LEN_WIDTH bits)
        header.eth.setBroadcast()
        header.eth.setSrcMAC(headerConfig.SRC_MAC_ADDR.asHex)
        header.eth.setAsArp()
        header.arp.setHardwareType(headerConfig.ARP_HTYPE.asHex)
        header.arp.setProtocolType(headerConfig.ARP_PTYPE.asHex)
        header.arp.setHardwareLen(headerConfig.ARP_HLEN.asHex)
        header.arp.setProtocolLen(headerConfig.ARP_PLEN.asHex)
        header.arp.arpRequest()
        header.arp.setSenderHardwareAddr(headerConfig.SRC_MAC_ADDR.asHex)
        header.arp.setSenderProtocolAddr(headerConfig.SRC_IP_ADDR.asHex)
        header.arp.setTargetHardwareAddr("0".asHex)
        header.arp.setTargetProtocolAddr(metaReg.dstIpAddr)
      }
      whenIsActive(goto(ARP_REQUEST_SEND))
    }

    val ARP_REQUEST_SEND: State = new State {
      onEntry {
        io.finishGenerate := True
      }
      whenIsActive {
        io.headerType := HeaderProtocolEnum.arp
        io.headerOut.valid := True
        io.headerOut.payload := arpFrame(sendingCnt.value)
        when(io.headerOut.fire) {
          sendingCnt.increment()
          when(sendingCnt.willOverflow) {
            goto(IDLE)
          }
        }
      }
    }

    val ARP_WAITING_ANSWER : State = new State {
    }

    val UDP_PACKET_PREPARE : State = new State {
      onEntry {
        io.packetLen := io.metaIn.payload.packetLen.resized + U(42, headerConfig.PACKET_LEN_WIDTH bits)
        header.eth.setDstMAC(arpCache.io.macAddrOut)
        header.eth.setSrcMAC(headerConfig.SRC_MAC_ADDR.asHex)
        header.eth.setAsIPv4()
        header.ipv4.setProtocolVersion()
        header.ipv4.setInternetHeaderLength(headerConfig.IPV4_IHL.asHex)
        header.ipv4.setDifferentiatedServicesCodePoint(headerConfig.IPV4_DSCP.asHex)
        header.ipv4.setExplicitCongestionNotification(headerConfig.IPV4_ECN.asHex)
        header.ipv4.setIpLen(ipLen.asBits.resized)
        header.ipv4.setIdentification(ipId.asBits)
        header.ipv4.setFlags(B"3'b000")
        header.ipv4.setFragmentOffset(B"13'b0")
        header.ipv4.setTTL(headerConfig.IPV4_TTL.asHex)
        header.ipv4.setAsUDP()
        header.ipv4.setChecksum(ipChecksum)
        header.ipv4.setSrcAddr(headerConfig.SRC_IP_ADDR.asHex)
        header.ipv4.setDstAddr(metaReg.dstIpAddr)
        header.udp.setSrcPort(metaReg.srcPort)
        header.udp.setDstPort(metaReg.dstPort)
        header.udp.setLen(udpLen.asBits.resized)
        header.udp.setChecksum(B(0).resized)
      }
      whenIsActive {
        goto(UDP_PACKET_SEND)
      }
    }

    val UDP_PACKET_SEND: State = new State {
      onEntry {
        io.finishGenerate := True
      }

      whenIsActive {
        io.headerType := HeaderProtocolEnum.udp
        io.headerOut.payload := (sendingCnt.value === 0) ? udpFrame(0) | udpFrame(1).rotateRight(10*headerConfig.OCTETS)
        io.headerOut.valid := True
        when(io.headerOut.fire) {
          sendingCnt.increment()
          when (sendingCnt.willOverflow) {
            goto(IDLE)
          }
        }
      }
    }

  }
}




