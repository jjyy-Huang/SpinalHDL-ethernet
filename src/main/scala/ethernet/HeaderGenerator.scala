package ethernet

import EthernetProtocolConstant._
import UserConfiguration._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}

/** 15          8      5      4    0
  * ┬───────────┬───────┬──────┬────┐
  * │ packetLen │ Shift │ LSel │0x05│
  * ┴───────────┴───────┴──────┴────┘
  */
object HeaderGeneratorControlCmd {
  val CONTROL_HEADER_RANGE = 3 downto 0
  val CONTROL_HEADER_WIDTH = CONTROL_HEADER_RANGE.length
  val CONTROL_HEADER = 0x05
  val CONTROL_PACKETLEN_RANGE = 15 downto (5+log2Up(DATA_BYTE_CNT))
  val CONTROL_PACKETLEN_WIDTH = CONTROL_PACKETLEN_RANGE.length
  val CONTROL_SHIFT_RANGE = (5+log2Up(DATA_BYTE_CNT)-1) downto 5
  val CONTROL_SHIFT_WIDTH = CONTROL_SHIFT_RANGE.length
  val CONTROL_LSEL_RANGE = 4 downto 4
  val CONTROL_LSEL_WIDTH = CONTROL_LSEL_RANGE.length

}
case class HeaderGeneratorGenerics(
    DATA_WIDTH: Int = DATA_WIDTH,
    DATA_BYTE_CNT: Int = DATA_BYTE_CNT,
    DATA_USE_TLAST: Boolean = true,
    DATA_USE_TUSER: Boolean = true,
    DATA_USE_TKEEP: Boolean = true,
    DATA_USE_TSTRB: Boolean = false,
    DATA_TUSER_WIDTH: Int = 1,
    INPUT_BUFFER_DEPTH: Int = 4
)
class HeaderGenerator(HeaderGeneratorConfig: HeaderGeneratorGenerics) extends Component {
  val headerAxisOutConfig = Axi4StreamConfig(
    dataWidth = HeaderGeneratorConfig.DATA_BYTE_CNT,
    userWidth = HeaderGeneratorConfig.DATA_TUSER_WIDTH,
    useKeep = HeaderGeneratorConfig.DATA_USE_TKEEP,
    useLast = HeaderGeneratorConfig.DATA_USE_TLAST,
    useUser = HeaderGeneratorConfig.DATA_USE_TUSER
  )

  val io = new Bundle {
    val metaIn = slave Stream MetaData()
    val headerAxisOut = master(Axi4Stream(headerAxisOutConfig))
  }

  val ipLenReg = Reg(UInt(IP_LENGTH_WIDTH bits)) init 0
  val udpLenReg = Reg(UInt(UDP_LENGTH_WIDTH bits)) init 0
  val ipIdReg = Reg(UInt(IDENTIFICATION_WIDTH bits)) init 0
  val ipChecksumReg = Reg(UInt(IP_HEADER_CHECKSUM_WIDTH bits)) init 0 //TODO
  val generateDone = Bool()

  val packetLenMax = ((MTU + ETH_HEADER_LENGTH) / DATA_BYTE_CNT.toFloat).ceil.toInt
  val packetLenWidth = log2Up(packetLenMax+1)
  val packetLenReg = Reg(UInt(packetLenWidth bits)) init 0

  val dataLoadedReg = Reg(Bool()) init False

  val sendHeaderCycle = (HEADER_TOTAL_LENGTH / DATA_BYTE_CNT.toFloat).ceil.toInt
  val sendHeaderLastLeft = HEADER_TOTAL_LENGTH % DATA_BYTE_CNT
  val sendingCnt = Counter(sendHeaderCycle)
  val needFragment = Reg(Bool()) init False

  val metaRegs = Reg(MetaData())
  val metaReadySet = !dataLoadedReg | generateDone
  io.metaIn.ready := metaReadySet

  when(io.metaIn.fire) {
    dataLoadedReg := True
  } elsewhen (generateDone) {
    dataLoadedReg := False
  }

  when(io.headerAxisOut.lastFire) {
    ipIdReg := ipIdReg + 1
  }

  val packetTotalLenFirst = io.metaIn.dataLen + HEADER_TOTAL_LENGTH
  val needOneMorePacketFirst = (packetTotalLenFirst & (DATA_BYTE_CNT - 1)) =/= 0
  val packetTotalLenFragmented = metaRegs.dataLen + HEADER_TOTAL_LENGTH
  val needOneMorePacketFragmented = (packetTotalLenFragmented & (DATA_BYTE_CNT - 1)) =/= 0

  when(io.metaIn.fire) {
    metaRegs.srcPort := io.metaIn.srcPort
    metaRegs.dstPort := io.metaIn.dstPort
    metaRegs.dstIpAddr := io.metaIn.dstIpAddr
    metaRegs.dstMacAddr := io.metaIn.dstMacAddr
    metaRegs.srcIpAddr := io.metaIn.srcIpAddr
    metaRegs.srcMacAddr := io.metaIn.srcMacAddr
  }

  when(io.metaIn.fire) {    // first load metadata
    when(io.metaIn.dataLen > MAX_DATA_NUM) {
      metaRegs.dataLen := io.metaIn.dataLen - (MTU - IP_HEADER_LENGTH - UDP_HEADER_LENGTH)
      needFragment := True
      ipLenReg := IP_LENGTH_MAX
      udpLenReg := UDP_LENGTH_MAX
      packetLenReg := packetLenMax
    } otherwise {
      needFragment := False
      ipLenReg := io.metaIn.dataLen.resize(IP_LENGTH_WIDTH) + IP_HEADER_LENGTH + UDP_HEADER_LENGTH
      udpLenReg := io.metaIn.dataLen.resize(IP_LENGTH_WIDTH) + UDP_HEADER_LENGTH
      packetLenReg := (packetTotalLenFirst >> log2Up(DATA_BYTE_CNT)).takeLow(packetLenWidth).asUInt +
        (needOneMorePacketFirst ? U(1, packetLenWidth bits) | U(0, packetLenWidth bits))
    }
  } elsewhen (io.headerAxisOut.lastFire & needFragment) { // there is still data waiting to be sent
    when(metaRegs.dataLen > MAX_DATA_NUM) {
      needFragment := True
      ipLenReg := IP_LENGTH_MAX
      metaRegs.dataLen := metaRegs.dataLen - MAX_DATA_NUM
      packetLenReg := packetLenMax
    } otherwise {
      needFragment := False
      ipLenReg := metaRegs.dataLen.resize(IP_LENGTH_WIDTH) + IP_HEADER_LENGTH + UDP_HEADER_LENGTH
      udpLenReg := metaRegs.dataLen.resize(IP_LENGTH_WIDTH) + UDP_HEADER_LENGTH
      packetLenReg := (packetTotalLenFragmented >> log2Up(DATA_BYTE_CNT)).takeLow(packetLenWidth).asUInt +
        (needOneMorePacketFragmented ? U(1, packetLenWidth bits) | U(0, packetLenWidth bits))
    }
  }


  val ethHeader = EthernetHeader(
    Seq(
      metaRegs.dstMacAddr,
      metaRegs.srcMacAddr,
      ETH_TYPE
    )
  )
  val ipv4Header = IPv4Header(
    Seq(
      IP_VERSION,
      IHL,
      DSCP,
      ECN,
      ipLenReg.asBits,
      ipIdReg.asBits,
      FLAGS,
      FRAGMENT_OFFSET,
      TTL,
      PROTOCOL,
      ipChecksumReg.asBits,
      metaRegs.srcIpAddr,
      metaRegs.dstIpAddr
    )
  )
  val udpHeader = UDPHeader(
    Seq(
      metaRegs.srcPort,
      metaRegs.dstPort,
      udpLenReg.asBits,
      0 // udpChecksum not support now
    )
  )

  val ethIpUdpHeader = mergeHeader(Seq(ethHeader, ipv4Header, udpHeader))

  switch(sendingCnt.value) {
    sendingCnt.valueRange.foreach { idx =>
      if ( idx == sendHeaderCycle-1 ) {
        is(idx) {
          io.headerAxisOut.data := ethIpUdpHeader(sendingCnt).rotateRight(sendHeaderLastLeft * BYTE_WIDTH)
          io.headerAxisOut.keep := B(DATA_BYTE_CNT bits,
            (DATA_BYTE_CNT - 1 downto DATA_BYTE_CNT - sendHeaderLastLeft) -> true,
            default -> false
          )
          io.headerAxisOut.user := generateControlSignal(sendHeaderLastLeft, False, packetLenReg).resize(DATA_BYTE_CNT)
        }
      } else {
        is(idx) {
          io.headerAxisOut.data := ethIpUdpHeader(sendingCnt)
          io.headerAxisOut.keep := Bits(DATA_BYTE_CNT bits).setAll()
          io.headerAxisOut.user := generateControlSignal(0, True, packetLenReg).resize(DATA_BYTE_CNT)
        }
      }
    }
  }

  io.headerAxisOut.valid := dataLoadedReg
  when(io.headerAxisOut.fire) {
    sendingCnt.increment()

    when(sendingCnt.willOverflowIfInc) {
      io.headerAxisOut.last := True
    } otherwise {
      io.headerAxisOut.last := False
    }

    when(sendingCnt.willOverflowIfInc & needFragment) {
      generateDone := False
    } elsewhen (sendingCnt.willOverflowIfInc & !needFragment) {
      generateDone := True
    } otherwise {
      generateDone := False
    }

  } otherwise {
    io.headerAxisOut.last := False
    generateDone := False
  }

  def mergeHeader(header: Seq[Seq[Bits]]): Vec[Bits] = {
    val headerWidth: Int = header.flatten.map(_.getWidth).sum
    val filledHeaderWidth = ((headerWidth / DATA_WIDTH.toFloat).ceil * DATA_WIDTH).toInt
    val tmp = header.flatten
      .reduce(_ ## _)
      .subdivideIn(BYTE_WIDTH bits)
      .reduce(_ ## _)
      .resize( filledHeaderWidth bits)
      .subdivideIn(DATA_WIDTH bits)
    tmp
  }

  import HeaderGeneratorControlCmd._
  def generateControlSignal(shiftLen: Int, lastSelect: Bool, packetLen: UInt): Bits = {
    val shiftBits = B(shiftLen, CONTROL_SHIFT_WIDTH bits)
    val ret = packetLen  ## shiftBits ## lastSelect ##
                      B(CONTROL_HEADER, CONTROL_HEADER_WIDTH bits)
    ret
  }
}
