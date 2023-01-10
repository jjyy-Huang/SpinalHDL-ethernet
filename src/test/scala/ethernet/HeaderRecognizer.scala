package ethernet

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import java.util.Calendar
import scala.math._
import scala.util.Random

case class HRSimConfig(
    sendTimes: Int = 32,
    useRandomPacket: Boolean = false,
//                      packetLen: Int = 35 + 42,
    packetLen: Int = 145 + 42,
    dstIpAddr: String = "c0a80103",
    dstPort: String = "156",
    dstMacADdr: String = "123456789abc"
)

object HeaderRecognizerSim extends App {

  val headerConfig = HeaderRecognizerGenerics()
  val simCfg = HRSimConfig()

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
    .compile(new HeaderRecognizer(headerConfig))
    .doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
//      val rxData = Array(
//        "cd3c94f63fd4ae7d24238a811d4efeffe9afcd71636806119de8ba50cfc2e924",
//        "99fbabb6567eb34794d73312c44e5b45b589ad6fbb2a8df4d063b27a8a3ebc8b"
//      )
//      val packetdata = Array(
//        "a8c00101a8c000001140000006003f0000450008bc9a78563412cffccffccffc",
//        "8a811d4efeffe9afcd71636806119de8ba50cfc2e92400002b00560100000301",
//        "3312c44e5b45b589ad6fbb2a8df4d063b27a8a3ebc8bcd3c94f63fd4ae7d2423"
//      )
      val rxData = Array(
        "cf87822bebc07382dc6a911b1d55179e25d8d66668c5e69881236379c591d884",
        "68fe49309b241f3bc46afad458d85b2fdc2fe9737f3eb0d11d4c8884b2d16608",
        "924212ae866bc44348c12f1f19b81752ed850431ccc4ffc96da863af8e7c3b24",
        "601f88168fbbcaac8078abf3656d50be7cef2775df336a89ea57d4271832f462",
        "2943b69d9116721d6879cab85730158f33d59482798e7acf33e3b0d262ae2ca4"
      )
      val packetdata = Array(
        "a8c00101a8c00000114000001f00ad0000450008bc9a78563412cffccffccffc",
        "911b1d55179e25d8d66668c5e69881236379c591d88400009900560100000301",
        "fad458d85b2fdc2fe9737f3eb0d11d4c8884b2d16608cf87822bebc07382dc6a",
        "2f1f19b81752ed850431ccc4ffc96da863af8e7c3b2468fe49309b241f3bc46a",
        "abf3656d50be7cef2775df336a89ea57d4271832f462924212ae866bc44348c1",
        "cab85730158f33d59482798e7acf33e3b0d262ae2ca4601f88168fbbcaac8078"
      )
      def initPort(): Unit = {
        dut.io.metaOut.ready #= true
        dut.io.dataAxisOut.ready #= true

        dut.io.dataAxisIn.valid #= false
        dut.io.dataAxisIn.data #= 0
        dut.io.dataAxisIn.last #= false
        dut.io.dataAxisIn.keep #= 0
      }

      initPort()

      dut.clockDomain.waitRisingEdge(50)

      loadData(simCfg.packetLen)
      dut.clockDomain.waitRisingEdge(3)
      dut.clockDomain.waitRisingEdge(50)

      def loadData(sendDataBytes: Int): Unit = {
        for (i <- 0 until (sendDataBytes.toFloat / 32.0f).ceil.toInt) {
          dut.io.dataAxisIn.data #= packetdata(i).asHex
          dut.io.dataAxisIn.valid #= true
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
