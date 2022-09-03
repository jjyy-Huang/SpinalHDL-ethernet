package template

import spinal.core._
import spinal.core.sim._

import scala.math._
import scala.util.Random

// import scala.io.Source

object Sim {
  def main(args: Array[String]) : Unit = {

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
      runFlags = List (

      )
		)

    SimConfig
      .withVCS(flags)
      .withVCSSimSetup("/home/jerry/workspace/hdl/prj/SpinalTemplate/synopsys_sim.setup", null)
      .withFSDBWave
      .compile(new Template)
      .doSim { dut =>
	      dut.clockDomain.forkStimulus(period = 10)

	      var times : Int = 100
				for ( i <- 0 until times ) {
					dut.io.a #= Random.nextBoolean()
					dut.clockDomain.waitRisingEdge()
				}
//	      simSuccess()
      }

  }
}
