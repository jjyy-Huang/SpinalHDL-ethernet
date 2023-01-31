package ethernet

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis._

import HeaderGeneratorControlCmd._
import EthernetProtocolConstant._
import UserConfiguration._

case class TxGenerics(
    DATA_WIDTH: Int = DATA_WIDTH,
    DATA_BYTE_CNT: Int = DATA_BYTE_CNT,
    OCTETS: Int = BYTE_WIDTH,
    DATA_USE_TLAST: Boolean = true,
    DATA_USE_TUSER: Boolean = true,
    DATA_USE_TKEEP: Boolean = true,
    DATA_USE_TSTRB: Boolean = false,
    DATA_TUSER_WIDTH: Int = 1,
    DATA_BUFFER_DEPTH: Int = 256,
    HEADER_BUFFER_DEPTH: Int = 16
)
class TxTop(txConfig: TxGenerics, headerConfig: HeaderGeneratorGenerics) extends Component {
  val dataAxisCfg = Axi4StreamConfig(
    dataWidth = txConfig.DATA_BYTE_CNT,
    userWidth = txConfig.DATA_TUSER_WIDTH,
    useStrb = txConfig.DATA_USE_TSTRB,
    useKeep = txConfig.DATA_USE_TKEEP,
    useLast = txConfig.DATA_USE_TLAST,
    useUser = txConfig.DATA_USE_TUSER
  )
  val io = new Bundle {
    val metaIn = slave Stream MetaData()
    val dataAxisIn = slave(Axi4Stream(dataAxisCfg))
    val dataAxisOut = master(Axi4Stream(dataAxisCfg))
  }

  val dataBuffered = StreamPayloadResetIfInvalid(io.dataAxisIn.queue(txConfig.DATA_BUFFER_DEPTH))
  val metaBuffered = io.metaIn.queue(headerConfig.INPUT_BUFFER_DEPTH)

  val headerGenerator = new HeaderGenerator(headerConfig)
  headerGenerator.io.metaIn << metaBuffered

  val headerBuffered = StreamPayloadResetIfInvalid(headerGenerator.io.headerAxisOut.queue(txConfig.HEADER_BUFFER_DEPTH))

  val invalidData = Bool()
  val forkedStream = StreamFork(dataBuffered, 2)
  val dataBufferedReg = forkedStream(0) stage()
  val subStream = forkedStream(1) combStage()

  val streamMuxReg = Reg(UInt(1 bits)) init 0
  val streamJoinReg0 = Reg(Bool()) init False
  val withSubStreamReg = Reg(Bool()) init False
  val withSubStream = withSubStreamReg || headerBuffered.lastFire


  val selectedStream = StreamMux(streamMuxReg, Vec(headerBuffered, dataBufferedReg)
                        ) throwWhen(invalidData)

  val joinedStream = SubStreamJoin(
    StreamPayloadResetIfInvalid(selectedStream),
    subStream,
    withSubStream
  ) m2sPipe ()

  when(forkedStream(1).lastFire) {
    withSubStreamReg := False
  } elsewhen (headerBuffered.lastFire.rise()) {
    withSubStreamReg := True
  }

  when(headerBuffered.lastFire) {
    streamMuxReg := 1
  } elsewhen (dataBufferedReg.lastFire | invalidData) {
    streamMuxReg := 0
  }

  when(forkedStream(1).lastFire) {
    streamJoinReg0 := True
  } elsewhen (selectedStream.lastFire | invalidData) {
    streamJoinReg0 := False
  }

  val maskReg = Reg(Bits(DATA_BYTE_CNT bits)) init 0
  val shiftLenReg = Reg(UInt(CONTROL_SHIFT_WIDTH bits)) init 0
  val packetLen = selectedStream.user(CONTROL_PACKETLEN_RANGE).asUInt - 1 // 0 until packetLen

  val maskStage = dataBufferedReg.clone()
  val isUserCmd =
    (selectedStream.user(CONTROL_HEADER_RANGE) === CONTROL_HEADER)
  val cntTrigger = headerBuffered.lastFire

  when(selectedStream.fire & isUserCmd) {
    maskReg := generateByteMask(selectedStream.user(CONTROL_SHIFT_RANGE).asUInt)
    shiftLenReg := selectedStream.user(CONTROL_SHIFT_RANGE).asUInt
  }
  val tCntMaxWidth = log2Up((MTU + ETH_HEADER_LENGTH) / DATA_BYTE_CNT)
  val transactionCounter = new StreamTransactionCounter(tCntMaxWidth)
  transactionCounter.io.ctrlFire := cntTrigger
  transactionCounter.io.targetFire := maskStage.fire
  transactionCounter.io.count := packetLen

  invalidData := transactionCounter.io.done & streamJoinReg0

  maskStage.arbitrationFrom(joinedStream)
  maskStage.data := byteMaskData(~maskReg, joinedStream.payload._1.data) |
                      byteMaskData(maskReg, joinedStream.payload._2.data)
  maskStage.keep := byteMaskData(~maskReg, joinedStream.payload._1.keep) |
                      byteMaskData(maskReg, joinedStream.payload._2.keep)
  maskStage.user := 0
  maskStage.last := transactionCounter.io.last

  val maskedStage = maskStage m2sPipe () s2mPipe ()
  val shiftLenRegNxt = RegNext(shiftLenReg) init 0

  val shiftStage = maskedStage.clone()
  shiftStage.arbitrationFrom(maskedStage)
  shiftStage.data := rotateLeftByte(maskedStage.data, shiftLenRegNxt)
  shiftStage.keep := rotateLeftBit(maskedStage.keep, shiftLenRegNxt)
  shiftStage.user := 0
  shiftStage.last := maskedStage.last

  io.dataAxisOut <-/< shiftStage

}
