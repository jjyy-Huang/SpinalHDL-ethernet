package ethernet

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import java.util.Calendar
import scala.math._
import scala.util.Random


object HeaderGeneratorSim extends App {

  val txConfig = TxGenerics()
  val arpConfig = ArpCacheGenerics()

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
    .withVCSSimSetup("/home/jerry/workspace/hdl/prj/test/synopsys_sim.setup", null)
    .withFSDBWave
    .compile(new HeaderGenerator(txConfig, arpConfig))
    .doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)

      def initPort(): Unit = {
        dut.io.metaIn.payload.dstIpAddr #= 0
        dut.io.metaIn.payload.dstPort #= 0
        dut.io.metaIn.payload.srcPort #= 0
        dut.io.metaIn.packetLen #= 0
        dut.io.metaIn.valid #= false
        dut.io.metaIn.mtu #= PacketMTUEnum.mtu1024
        dut.io.headerOut.ready #= true
      }
      initPort()

      dut.clockDomain.waitRisingEdge(50)

      var base : BigInt = "c0a80100".asHex
      for (i <- 0 until 5) {
        writeCache(base+i, BigInt(Random.nextInt(65535)))
      }
      dut.clockDomain.waitRisingEdge(50)

      loadMeta()
      dut.clockDomain.waitRisingEdge(50)

      def loadMeta(): Unit = {
        dut.io.metaIn.payload.dstIpAddr #= "c0a80103".asHex
        dut.io.metaIn.payload.dstPort #= "123".asHex
        dut.io.metaIn.payload.packetLen #= 1000
        dut.io.metaIn.valid #= true
        dut.clockDomain.waitRisingEdge()
        dut.io.metaIn.valid #= false
//        dut.clockDomain.waitActiveEdgeWhere(!dut.io.metaIn.ready.toBoolean)
      }

      def writeCache(addr : BigInt, data : BigInt): Unit ={
        dut.arpCache.io.writeEna #= true
        dut.arpCache.io.ipAddrIn #= addr
        dut.arpCache.io.macAddrIn #= data
        dut.clockDomain.waitRisingEdge()
        dut.arpCache.io.writeEna #= false
      }
      	      simSuccess()

    }
}

object HeaderGeneratorInst extends App {
  val txConfig = TxGenerics()
  val arpConfig = ArpCacheGenerics()
  SpinalConfig(
    targetDirectory = "./verilog",
    oneFilePerComponent = false,
    removePruned = true,
    rtlHeader =
      s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new HeaderGenerator(txConfig, arpConfig))
    .printPruned()
}

