package ethernet

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import java.util.Calendar
import scala.math._
import scala.util.Random

object HeaderGeneratorSim extends App {

  val headerConfig = HeaderGeneratorGenerics()

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
    .compile(new HeaderGenerator(headerConfig))
    .doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)

      def initPort(): Unit = {
        dut.io.metaIn.payload.IpAddr #= 0
        dut.io.metaIn.payload.dstPort #= 0
        dut.io.metaIn.payload.MacAddr #= 0
        dut.io.metaIn.payload.srcPort #= 0
        dut.io.metaIn.dataLen #= 0
        dut.io.metaIn.valid #= false
//        dut.io.metaIn.packetMTU #= PacketMTUEnum.mtu1024
        dut.io.headerAxisOut.ready #= true
      }
      initPort()

      dut.clockDomain.waitRisingEdge(50)

//      var base : BigInt = "c0a80100".asHex
//      for (i <- 0 until 5) {
//        writeCache(base+i, BigInt(Random.nextInt(65535)))
//      }
//      dut.clockDomain.waitRisingEdge(50)
//      StreamReadyRandomizer(dut.io.headerAxisOut, dut.clockDomain)
      loadMeta()
      dut.clockDomain.waitRisingEdge(3)
      loadMeta()
      dut.clockDomain.waitRisingEdge(50)

      def loadMeta(): Unit = {
        dut.io.metaIn.payload.MacAddr #= Random
          .nextLong()
          .abs % 281474976710655L
        dut.io.metaIn.payload.IpAddr #= Random.nextInt().abs
        dut.io.metaIn.payload.dstPort #= "123".asHex
        dut.io.metaIn.payload.dataLen #= 1024
        dut.io.metaIn.valid #= true
        dut.clockDomain.waitRisingEdge()
        dut.io.metaIn.valid #= false
//        dut.clockDomain.waitActiveEdgeWhere(!dut.io.metaIn.ready.toBoolean)
      }

//      def writeCache(addr : BigInt, data : BigInt): Unit ={
//        dut.arpCache.io.writeEna #= true
//        dut.arpCache.io.ipAddrIn #= addr
//        dut.arpCache.io.macAddrIn #= data
//        dut.clockDomain.waitRisingEdge()
//        dut.arpCache.io.writeEna #= false
//      }
      simSuccess()

    }
}

object HeaderGeneratorInst extends App {
  val headerConfig = HeaderGeneratorGenerics()
  SpinalConfig(
    targetDirectory = "./verilog",
    oneFilePerComponent = false,
    removePruned = true,
    rtlHeader = s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new HeaderGenerator(headerConfig))
    .printPruned()
}
