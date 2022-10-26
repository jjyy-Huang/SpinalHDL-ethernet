package merge

import spinal.core.Component.push
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream

import scala.math._
import scala.util.Random


// import scala.io.Source


object Sim extends App {

	val config = MergeGenerics(
		DATA_WD = 32,
		DATA_BYTE_WD = 32 / 8,
		BYTE_CNT_WD = 3
	)

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
		.compile(new AXI4StreamInsertTopLevel(config))
		.doSim { dut =>
			dut.clockDomain.forkStimulus(period = 10)
			dut.io.dataOut_AXIS.port.ready #= true
			def driverInit(): Unit ={
				dut.io.headerIn_AXIS.port.valid #= false
				dut.io.headerIn_AXIS.port.keep #= 0
				dut.io.headerIn_AXIS.port.user #= 0
				dut.io.headerIn_AXIS.port.data #= 0

				dut.io.dataIn_AXIS.port.valid #= false
				dut.io.dataIn_AXIS.port.keep #= 0
				dut.io.dataIn_AXIS.port.data #= 0
				dut.io.dataIn_AXIS.port.last #= false

			}
			driverInit()

			for (i <- 0 until 10) {
				dut.clockDomain.waitRisingEdge()
			}
			dut.io.headerIn_AXIS.port.valid #= true
			dut.io.headerIn_AXIS.port.keep #= 15
			dut.io.headerIn_AXIS.port.user #= 4
			dut.io.headerIn_AXIS.port.data #= abs(Random.nextInt() % pow(2, 32)).toInt
			dut.clockDomain.waitRisingEdge()

			dut.io.headerIn_AXIS.port.valid #= false
			dut.io.headerIn_AXIS.port.keep #= 0
			dut.io.headerIn_AXIS.port.user #= 0
			dut.io.headerIn_AXIS.port.data #= 0
			dut.clockDomain.waitRisingEdge(3)

			var times: Int = 4
			for (i <- 0 until times) {
				dut.io.dataIn_AXIS.port.valid #= true
				dut.io.dataIn_AXIS.port.keep #= 15
				dut.io.dataIn_AXIS.port.data #= abs(Random.nextInt() % pow(2, 32)).toInt
				dut.io.dataIn_AXIS.port.last #= false
				dut.clockDomain.waitRisingEdge()
			}

			dut.io.dataIn_AXIS.port.valid #= false
			dut.io.dataOut_AXIS.port.ready #= false
			dut.clockDomain.waitRisingEdge(2)
			dut.io.dataOut_AXIS.port.ready #= true
			dut.clockDomain.waitRisingEdge(2)


			for (i <- 0 until 2) {
				dut.io.dataIn_AXIS.port.valid #= true
				dut.io.dataIn_AXIS.port.keep #= 15
				dut.io.dataIn_AXIS.port.data #= abs(Random.nextInt() % pow(2, 32)).toInt
				dut.io.dataIn_AXIS.port.last #= false
				dut.clockDomain.waitRisingEdge()
			}

			dut.io.dataIn_AXIS.port.valid #= true
			dut.io.dataIn_AXIS.port.keep #= 12
			dut.io.dataIn_AXIS.port.data #= abs(Random.nextInt() % pow(2, 32)).toInt
			dut.io.dataIn_AXIS.port.last #= true
			dut.clockDomain.waitRisingEdge()


			for (i <- 0 until times) {
				dut.io.dataIn_AXIS.port.valid #= false
				dut.io.dataIn_AXIS.port.keep #= 0
				dut.io.dataIn_AXIS.port.data #= 0
				dut.io.dataIn_AXIS.port.last #= false
				dut.clockDomain.waitRisingEdge()
			}

//				spinal.lib.sim.StreamDriver

			//	      simSuccess()
			}

}

