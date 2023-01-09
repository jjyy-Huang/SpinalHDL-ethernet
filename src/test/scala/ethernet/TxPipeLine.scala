package ethernet

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.sim._

import java.util.Calendar
import scala.math._
import scala.util
import scala.util.Random

//object TxControlUnitSim extends App {
//
//  val txConfig = TxGenerics()
//
//  val flags = VCSFlags(
//    compileFlags = List(
//      "-kdb ",
//      "-work xil_defaultlib ",
//      "/home/jerry/workspace/hdl/xilinxip_vcs/glbl.v"
//    ),
//    elaborateFlags = List(
//      "-LDFLAGS -Wl,--no-as-needed ",
//      "-kdb ",
//      "xil_defaultlib.glbl "
//    ),
//    runFlags = List(
//    )
//  )
//
//  SimConfig
//    .withVCS(flags)
//    .withVCSSimSetup(
//      "/home/jerry/workspace/hdl/prj/test/synopsys_sim.setup",
//      null
//    )
//    .withFSDBWave
//    .compile(new TxControlUnit(txConfig))
//    .doSim { dut =>
//      dut.clockDomain.forkStimulus(period = 10)
//
//      def initPort(): Unit = {
//        dut.io.headerType #= ProtocolTypeEnum.none
//        dut.io.headerGeneratorFinish #= false
//        dut.io.packetLen #= 0
//        dut.io.dataValid #= false
//      }
//
//      initPort()
//
//      dut.clockDomain.waitRisingEdge(50)
//
//      loadData()
//      dut.clockDomain.waitRisingEdge(50)
//
//      def loadData(): Unit = {
//        dut.io.headerType #= ProtocolTypeEnum.udp
//        dut.io.packetLen #= 32 * 2 + 1
//        dut.io.dataValid #= true
//        dut.io.headerGeneratorFinish #= true
//        dut.clockDomain.waitRisingEdge()
//        dut.clockDomain.waitRisingEdge()
//        dut.io.dataValid #= false
//        dut.io.headerGeneratorFinish #= false
//        dut.clockDomain.waitRisingEdge()
//        dut.io.dataValid #= true
//        dut.clockDomain.waitRisingEdge()
//        dut.io.dataValid #= false
//        dut.clockDomain.waitRisingEdge()
//        //        dut.clockDomain.waitActiveEdgeWhere(!dut.io.metaIn.ready.toBoolean)
//      }
//
//      simSuccess()
//
//    }
//}

case class simConfig(
    sendTimes: Int = 32,
    useRandomPacket: Boolean = true,
    packetLen: Int = 64,
    dstIpAddr: String = "c0a80103",
    dstPort: String = "156",
    dstMacADdr: String = "fccffccffccf"
)
object TxPipeLineSim extends App {

  val txConfig = TxGenerics()
  val metaInterfaceConfig = MetaInterfaceGenerics()
  val headerConfig = HeaderGeneratorGenerics()
  val arpCacheConfig = ArpCacheGenerics()

  val simCfg = simConfig()

  val flags = VCSFlags(
    compileFlags = List(
      "-kdb ",
      "-work xil_defaultlib ",
      "/home/jerry/workspace/hdl/xilinxip_vcs/glbl.v"
    ),
    elaborateFlags = List(
      "-LDFLAGS -Wl,--no-as-needed ",
      "-kdb ",
      "xil_defaultlib.glbl "
    ),
    runFlags = List(
    )
  )

  SimConfig
    .withVCS(flags)
    .withVCSSimSetup(
      "/home/jerry/workspace/hdl/prj/test/synopsys_sim.setup",
      null
    )
    .withFSDBWave
//    .withIVerilog
//    .withWave
    .compile(
      new TxTop(txConfig, metaInterfaceConfig, headerConfig)
    )
    .doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)

      def initPort(): Unit = {
        dut.io.metaIn.payload.MacAddr #= 0
        dut.io.metaIn.payload.IpAddr #= 0
        dut.io.metaIn.payload.dstPort #= 0
        dut.io.metaIn.payload.srcPort #= 0
        dut.io.metaIn.dataLen #= 0
        dut.io.metaIn.valid #= false
//        dut.io.metaIn.packetMTU #= PacketMTUEnum.mtu1024
        dut.io.dataAxisOut.ready #= true

        dut.io.dataAxisIn.valid #= false
        dut.io.dataAxisIn.data #= 0
        dut.io.dataAxisIn.last #= false
        dut.io.dataAxisIn.keep #= 0
        dut.io.dataAxisIn.user #= 0
      }

      initPort()
//      initArpCache()

      dut.clockDomain.waitRisingEdge(50)

//      StreamReadyRandomizer(dut.io.dataAxisOut, dut.clockDomain)
      for (idx <- 0 until simCfg.sendTimes) {
        val sendDataBytes =
          (if (simCfg.useRandomPacket) Random.nextInt(1400).abs
           else simCfg.packetLen)
        driveTransaction(sendDataBytes)
      }

      def driveTransaction(sendDataBytes: Int): Unit = {
        val a = fork {
          dut.clockDomain.waitRisingEdge(Random.nextInt(32).abs)
          loadMeta(sendDataBytes)
        }
        val b = fork {
          dut.clockDomain.waitRisingEdge(Random.nextInt(32).abs)
          loadData(sendDataBytes)
        }
        a.join()
        b.join()
//        dut.clockDomain.waitRisingEdge(Random.nextInt(5).abs)
      }

      def loadMeta(sendDataBytes: Int): Unit = {
        dut.io.metaIn.payload.MacAddr #= simCfg.dstMacADdr.asHex
        dut.io.metaIn.payload.IpAddr #= simCfg.dstIpAddr.asHex
        dut.io.metaIn.payload.dstPort #= simCfg.dstPort.asHex
        dut.io.metaIn.payload.dataLen #= sendDataBytes
        dut.io.metaIn.valid #= true
        dut.clockDomain.waitRisingEdge()
        dut.io.metaIn.valid #= false
      }

      def loadData(sendDataBytes: Int): Unit = {
        for (i <- 0 until (sendDataBytes.toFloat / 32.0f).ceil.toInt) {
          dut.io.dataAxisIn.data #= BigInt(256, Random).toString(16).asHex
          dut.io.dataAxisIn.valid #= true
          dut.io.dataAxisIn.user #= 0
          if (i == (sendDataBytes.toFloat / 32.0f).ceil.toInt - 1) {
            dut.io.dataAxisIn.keep #= (pow(
              2,
              sendDataBytes - i * 32
            ) - 1).toLong.abs
            dut.io.dataAxisIn.last #= true
          } else {
            dut.io.dataAxisIn.last #= false
            dut.io.dataAxisIn.keep #= (pow(2, 32) - 1).toLong.abs
          }
          dut.clockDomain.waitRisingEdge()
          dut.io.dataAxisIn.data #= 0
          dut.io.dataAxisIn.valid #= false
          dut.io.dataAxisIn.last #= false
          dut.io.dataAxisIn.keep #= 0
        }
      }

//      def initArpCache(): Unit = {
//        var base: BigInt = "c0a80100".asHex
//        for (i <- 0 until 5) {
//          writeArpCache(base + i, BigInt(Random.nextInt(65535)))
//        }
//      }
//
//      def writeArpCache(addr: BigInt, data: BigInt): Unit = {
//        dut.headerGenerator.arpCache.io.writeEna #= true
//        dut.headerGenerator.arpCache.io.ipAddrIn #= addr
//        dut.headerGenerator.arpCache.io.macAddrIn #= data
//        dut.clockDomain.waitRisingEdge()
//        dut.headerGenerator.arpCache.io.writeEna #= false
//      }

      dut.clockDomain.waitRisingEdge(50)
      simSuccess()
    }
}
