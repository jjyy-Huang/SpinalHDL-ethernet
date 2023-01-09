//package ethernet
//
//
//import spinal.core._
//import spinal.core.sim._
//import spinal.lib._
//import spinal.lib.sim._
//
//import java.util.Calendar
//import scala.math._
//import scala.util.Random
//
//
//// import scala.io.Source
//
//
//object ARPCacheSim extends App {
//
//  val config = ArpCacheGenerics()
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
//
//    )
//  )
//
//  SimConfig
//    .withVCS(flags)
//    .withVCSSimSetup("/home/jerry/workspace/hdl/prj/test/synopsys_sim.setup", null)
//    .withFSDBWave
//    .compile(new ARPCache(config))
//    .doSim { dut =>
//      dut.clockDomain.forkStimulus(period = 10)
//
//      def initPort(): Unit = {
//        dut.io.ipAddrIn #= 0
//        dut.io.macAddrIn #= 0
//        dut.io.readEna #= false
//        dut.io.writeEna #= false
//      }
//      initPort()
//
//      dut.clockDomain.waitRisingEdge(50)
//
//      var base : BigInt = "c0a80000".asHex
//      for (i <- 0 until 256) {
//        writeCache(base+i, BigInt(Random.nextInt(65535)))
//      }
//      dut.clockDomain.waitRisingEdge(50)
//
//
//      for (i <- 0 until 256) {
//        readCache(base + i)
//      }
//      dut.clockDomain.waitRisingEdge(50)
//
//      base = "c0a80100".asHex
//      for (i <- 0 until 10) {
//        readCache(base + i)
//      }
//
//      def writeCache(addr : BigInt, data : BigInt): Unit ={
//        dut.io.writeEna #= true
//        dut.io.ipAddrIn #= addr
//        dut.io.macAddrIn #= data
//        dut.clockDomain.waitRisingEdge()
//        dut.io.writeEna #= false
//      }
//
//      def readCache(addr : BigInt): Unit = {
//        dut.io.readEna #= true
//        dut.io.ipAddrIn #= addr
//        dut.clockDomain.waitRisingEdge()
//        dut.io.readEna #= false
//      }
//
//
//      //	      simSuccess()
//
//    }
//}
//
//object ARPCacheInst extends App {
//  val config = ArpCacheGenerics()
//  SpinalConfig(
//    targetDirectory = "./verilog",
//    oneFilePerComponent = false,
//    removePruned = true,
//    rtlHeader =
//      s"""
//         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
//         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
//  )
//    .generateVerilog(new ARPCache(config))
//    .printPruned()
//}
//
