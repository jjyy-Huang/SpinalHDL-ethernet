package ethernet
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.StreamDriver
import spinal.lib._

import scala.util.Random
class TestSubStreamJoin extends Component {
  val io = new Bundle {
    val main = slave(Stream(UInt(8 bits)))
    val sub = slave(Stream(Bits(16 bits)))
    val withSub = in Bool()

    val merge = master(Stream(TupleBundle2(main.payloadType, sub.payloadType)))
  }
  noIoPrefix()

  io.merge <-/< SubStreamJoin(io.main, io.sub, io.withSub)
}

object TestSubStreamJoinSim extends App {

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
  // dont use vcs
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
      new TestSubStreamJoin
    )
    .doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
      SimTimeout(50000)
      StreamDriver(dut.io.main, dut.clockDomain) { payload =>
        payload.randomize()
        true
      }

      StreamDriver(dut.io.sub, dut.clockDomain) { payload =>
        payload.randomize()
        true
      }
      val a = fork {
        while(true) {
          dut.io.withSub #= Random.nextFloat() < 0.8
          dut.io.merge.ready #= Random.nextFloat() < 0.8
          dut.clockDomain.waitSampling()
        }
      }

      dut.clockDomain.waitSampling(1000)
      simSuccess()
    }
}
