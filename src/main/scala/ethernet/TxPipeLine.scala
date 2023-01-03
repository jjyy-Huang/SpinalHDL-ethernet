package ethernet

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis._
import spinal.core.Mem
import ethernet.MTUEnum
import spinal.core.internals.Operator
import spinal.lib.fsm._

import java.util.Calendar
import scala.math._

case class TxGenerics (
                        SRC_IP_ADDR : String = "c0a80101",
                        SRC_MAC_ADDR : String = "123456789abc",
                        IPV4_IHL : String = "5",
                        IPV4_DSCP : String = "0",
                        IPV4_ECN : String = "0",
                        IPV4_TTL : String = "40",
                        ARP_HTYPE : String = "0001",
                        ARP_PTYPE : String = "0800",
                        ARP_HLEN : String = "06",
                        ARP_PLEN : String = "04",
                        PACKET_LEN_WIDTH : Int = 13,
                        IP_ADDR_WIDTH : Int = 32,
                        PORT_WIDTH : Int = 16,

                        DATA_WIDTH : Int = 256,
                        DATA_BYTE_CNT : Int = 32,
                        OCTETS: Int = 8,
                        DATA_USE_TLAST : Boolean = true,
                        DATA_USE_TUSER : Boolean = false,
                        DATA_USE_TKEEP : Boolean = true,
                        DATA_USE_TSTRB : Boolean = false,
                        DATA_TUSER_WIDTH: Int = 1,
                        INPUT_BUFFER_DEPTH : Int = 32)

case class TxConfiguration(config : TxGenerics) extends Bundle{
  val packetLen = UInt(config.PACKET_LEN_WIDTH bits)
  val dstIpAddr = Bits(config.IP_ADDR_WIDTH bits)
  val dstPort = Bits(config.PORT_WIDTH bits)
  val srcPort = Bits(config.PORT_WIDTH bits)
  val mtu = MTUEnum()
}

class TxControlUnit(config : TxGenerics) extends Component {
  val io = new Bundle {
    val headerType = in(HeaderProtocolEnum())
    val headerGeneratorFinish = in Bool()
    val packetLen = in UInt(config.PACKET_LEN_WIDTH bits)
    val dataValid = in Bool()

    val streamSelect = out Bool() setAsReg() init False
    val rotateShiftLen = out UInt (log2Up(config.DATA_BYTE_CNT) bits)
    val packetLast = out Bool()
    val packetKeep = out Bits(config.DATA_BYTE_CNT bits)
    val bypassMask = out Bits(config.DATA_BYTE_CNT bits) setAsReg() init 0
  }


  def generateLowBitOne(num: UInt): Bits = {
    val res = Bits(config.DATA_BYTE_CNT bits)
    switch(num) {
      for (idx <- 0 until config.DATA_BYTE_CNT) {
        if (idx == 0) {
          is(idx) {
            res := Bits(config.DATA_BYTE_CNT bits).setAll()
          }
        } else {
          is(idx) {
            res := B(pow(2, idx).toLong - 1, config.DATA_BYTE_CNT bits)
          }
        }
      }
    }
    res
  }

  val shiftLen = UInt(log2Up(config.DATA_BYTE_CNT) bits) setAsReg() init 0
  val rotateShiftDelay2 = RegNext(shiftLen) init 0
  io.rotateShiftLen := rotateShiftDelay2

  val sendingCycle = Reg(UInt(config.PACKET_LEN_WIDTH - 4 bits)) init 0
  val lastKeepNum = Reg(UInt(log2Up(config.DATA_BYTE_CNT) bits)) init 0
  val controlStateMachine = new StateMachine {
    val cyclesCnt = Counter(config.PACKET_LEN_WIDTH - 4 bits) // 8192 >> 5
    shiftLen := 0

    val isLast = Bool() setAsReg() init False
    val isLastDelay = Delay(isLast, 2, init = False)
    io.packetLast := isLastDelay
    val packerkeep = Bits(config.DATA_BYTE_CNT bits) setAsReg() init 0
    val packerKeepDelay = Delay(packerkeep, 2, init = Bits(config.DATA_BYTE_CNT bits).setAll())
    io.packetKeep := packerKeepDelay

    val IDLE: State = new State with EntryPoint {
      onEntry {
        io.streamSelect := False
      }
      whenIsActive{
        io.bypassMask := 0
        isLast := False
        when (io.headerGeneratorFinish) {
          switch (io.headerType) {
            is (HeaderProtocolEnum.udp) {
              goto(UDP_SENDING)
            }
            is (HeaderProtocolEnum.arp) {
              goto(ARP_REQUEST_SENDING)
            }
          }
        }
      }
      onExit {
        sendingCycle := (io.packetLen.dropLow(5).asUInt - 1).resized
        lastKeepNum := io.packetLen.takeLow(5).asUInt
        packerkeep := Bits(config.DATA_BYTE_CNT bits).setAll()
      }
    }

    val ARP_REQUEST_SENDING = new State {
      onEntry {
        shiftLen := 0
      }
      whenIsActive {
        when (io.dataValid) {
          goto(IDLE)
        }
      }
      onExit {
        isLast := True
      }
    }

    val UDP_SENDING = new State {
      onEntry {
        cyclesCnt.clear()
      }
      whenIsActive {
        shiftLen := 10
        io.streamSelect := True
        io.bypassMask := generateLowBitOne(U(22).resized)
        when (io.dataValid) {
          cyclesCnt.increment()
//          add cmp mux
          when (cyclesCnt.value === sendingCycle) {
            packerkeep := generateLowBitOne(lastKeepNum)
            goto(IDLE)
          }
        }
      }
      onExit{
        isLast := True
      }

    }
  }
}

class TxTop(txConfig : TxGenerics, arpCacheConfig : ArpCacheGenerics) extends Component {
  val dataAxisCfg = Axi4StreamConfig(
    dataWidth = txConfig.DATA_BYTE_CNT,
    userWidth = txConfig.DATA_TUSER_WIDTH,
    useStrb = txConfig.DATA_USE_TSTRB,
    useKeep = txConfig.DATA_USE_TKEEP,
    useLast = txConfig.DATA_USE_TLAST,
    useUser = txConfig.DATA_USE_TUSER)

  val io = new Bundle {
    val metaIn = slave Stream TxConfiguration(txConfig)

    val dataAxisIn = slave(Axi4Stream(dataAxisCfg))
    val dataAxisOut = master(Axi4Stream(dataAxisCfg))
  }

  val txControlUnit = new TxControlUnit(txConfig)

  val dataBuffered = Stream(Bits(txConfig.DATA_WIDTH bits))
  val inputDataBuffer = StreamFifo(
    dataType = Bits(txConfig.DATA_WIDTH bits),
    depth = txConfig.INPUT_BUFFER_DEPTH
  )
  inputDataBuffer.io.push << io.dataAxisIn.toBitStream()
  inputDataBuffer.io.pop >> dataBuffered

  val headerGenerator = new HeaderGenerator(txConfig, arpCacheConfig)
  headerGenerator.io.metaIn << io.metaIn
  headerGenerator.io.writeean := False
  headerGenerator.io.writemac := 0

  val inStreams = Vec(headerGenerator.io.headerOut, dataBuffered)
  val selectedStream = StreamMux(txControlUnit.io.streamSelect.asUInt, inStreams) s2mPipe()
  val bypassMask = txControlUnit.io.bypassMask
  val forkStreams = StreamFork(selectedStream, 2, true) // -> false  !care default
  val bypassData = forkStreams(1) s2mPipe()

  txControlUnit.io.headerType := headerGenerator.io.headerType
  txControlUnit.io.headerGeneratorFinish := headerGenerator.io.finishGenerate
  txControlUnit.io.packetLen := headerGenerator.io.packetLen
  txControlUnit.io.dataValid := selectedStream.fire

//  redesign
  def rotateLeftByte(data: Bits, bias: UInt): Bits = {
    var result = cloneOf(data)
    result := data
    for (i <- bias.bitsRange) {
      result \= (bias(i) ? result.rotateLeft(txConfig.OCTETS << i) | result)
    }
    result
//    val res1 = bias.mux {
//      0 -> ,
//      1 ->
//    }
  }

  def byteMaskData(byteMask: Bits, data: Bits): Bits = {
    var dataWidth = txConfig.DATA_BYTE_CNT
    var maskWidth = byteMask.getWidth
    require(maskWidth == dataWidth, s"ByteMaskData maskWidth${maskWidth} != dataWidth${dataWidth}")
    val vecByte = data.subdivideIn(maskWidth slices)
    vecByte.zipWithIndex.foreach { case (byte, idx) =>
      when(byteMask(idx)) {
        byte.clearAll()
      }
    }
    val maskedData = vecByte.reverse.reduceLeft(_ ## _)
    maskedData
  }

  val bufferStage = forkStreams(0) m2sPipe() s2mPipe()
  val rotateShiftStage = bufferStage.clone() // cloneOf
  val commitStage = rotateShiftStage.clone()

  rotateShiftStage <-/< bufferStage.translateWith(byteMaskData(bypassMask, bufferStage.payload) |
                                                  byteMaskData(~bypassMask, bypassData.payload))
  commitStage <-/< rotateShiftStage.translateWith(rotateLeftByte(rotateShiftStage.payload,
                                                                  txControlUnit.io.rotateShiftLen))
//  same direction
// just use payload, not stream
  bypassData.ready := rotateShiftStage.ready

  io.dataAxisOut <-/< Axi4Stream(commitStage)
//  follow design pattern

//  warning -> error

//  assert systemverilog


  def rebuildAxis(): Unit ={

  }

//  no verilog style

//  io.dataAxisOut.data := commitStage.payload
//  io.dataAxisOut.valid := commitStage.valid
//  commitStage.ready := io.dataAxisOut.ready
  io.dataAxisOut.keep := txControlUnit.io.packetKeep
  io.dataAxisOut.last := txControlUnit.io.packetLast
}

//
//object test extends App {
//  SpinalConfig(
//    targetDirectory = "./verilog",
//    oneFilePerComponent = false,
//    removePruned = true,
//    rtlHeader =
//      s"""
//         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
//         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
//  )
//    .generateVerilog(new test)
//}


