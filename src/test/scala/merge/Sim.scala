package merge

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

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
			SimTimeout(500000)
			def driverInit(): Unit ={
				dut.io.headerIn_AXIS.port.user #= 0
				dut.io.headerIn_AXIS.port.keep #= 0
				dut.io.headerIn_AXIS.port.data #= 0
				dut.io.headerIn_AXIS.port.valid #= false

				dut.io.dataIn_AXIS.port.keep #= 0
				dut.io.dataIn_AXIS.port.data #= 0
				dut.io.dataIn_AXIS.port.valid #= false
				dut.io.dataIn_AXIS.port.last #= false

				dut.io.dataOut_AXIS.port.ready #= true
			}

			val scoreboard = ScoreboardInOrder[Int]()

			def SumUpPow(a: Int): Int = {
				var sum: Int = 0
				for (j <- config.DATA_BYTE_WD - a until config.DATA_BYTE_WD) {
					sum += pow(2, j).toInt
				}
				sum
			}

			def SumDownPow(a: Int): Int = {
				var sum: Int = 0
				for (j <- 0 until a) {
					sum += pow(2, j).toInt
				}
				sum
			}

			def DataDrive(): Unit ={
				fork {
					def PushDataInScoreBoard(): Unit ={
						if (dut.io.dataIn_AXIS.port.valid.toBoolean && dut.io.dataIn_AXIS.port.ready.toBoolean) {
							var data: BigInt = dut.io.dataIn_AXIS.port.data.toBigInt
							var keep: BigInt = dut.io.dataIn_AXIS.port.keep.toBigInt
//															println(data.toString(16) + "  --  " + keep.toString(2))
							for (i <- config.DATA_BYTE_WD - 1 downto 0) {
								if ((pow(2, i).toInt & keep) >> i == 1) {
//																			println((((0xff << i * 8) & data) >> i * 8).toString(16))
									scoreboard.pushRef((((0xff << i * 8) & data) >> i * 8).toInt)
								}
							}
						}
					}

					def issueSeq(t: Int): Unit = {
						var times = 0
						while (times < t) {
							if (dut.io.dataIn_AXIS.port.ready.toBoolean) {
								dut.io.dataIn_AXIS.port.data.randomize()
								times += 1
							}
							dut.io.dataIn_AXIS.port.valid #= true
							dut.io.dataIn_AXIS.port.keep #= pow(2, config.DATA_BYTE_WD).toInt - 1
							dut.io.dataIn_AXIS.port.last #= false
							dut.clockDomain.waitRisingEdge()
							PushDataInScoreBoard()
							dut.io.dataIn_AXIS.port.valid #= false
						}
						dut.io.dataIn_AXIS.port.data.randomize()
						dut.io.dataIn_AXIS.port.valid #= true
						dut.io.dataIn_AXIS.port.last #= true
						dut.io.dataIn_AXIS.port.keep #= SumUpPow(Random.nextInt(config.DATA_BYTE_WD) + 1)
						dut.clockDomain.waitRisingEdge()
						while (!dut.io.dataIn_AXIS.port.ready.toBoolean) {
							dut.clockDomain.waitRisingEdge()
						}
						dut.io.dataIn_AXIS.port.data #= 0
						dut.io.dataIn_AXIS.port.valid #= false
						dut.io.dataIn_AXIS.port.last #= false
						dut.io.dataIn_AXIS.port.keep #= pow(2, config.DATA_BYTE_WD).toInt - 1
						PushDataInScoreBoard()

						dut.clockDomain.waitRisingEdge(10)
					}

					while (true) {

						if (dut.getHeader.toBoolean) {
							issueSeq(Random.nextInt(16) + 1)
						}

						dut.clockDomain.waitRisingEdge()
					}
				}
			}

			def HeaderDrive(): Unit = {
				fork {
					def SetHeader(): Unit = {
						var t = Random.nextInt(config.DATA_BYTE_WD + 1)
						dut.io.headerIn_AXIS.port.data.randomize()
						dut.io.headerIn_AXIS.port.valid #= true
						dut.io.headerIn_AXIS.port.keep #= SumDownPow(t)
						dut.io.headerIn_AXIS.port.user #= t
					}

					def PushHeaderInScoreBoard(): Unit ={
						if (dut.io.headerIn_AXIS.port.valid.toBoolean && dut.io.headerIn_AXIS.port.ready.toBoolean) {
							var data: BigInt = dut.io.headerIn_AXIS.port.data.toBigInt
							var keep: BigInt = dut.io.headerIn_AXIS.port.keep.toBigInt
							//								println(data.toString(16) + "  --  " + keep.toString(2))
							for (i <- config.DATA_BYTE_WD - 1 downto 0) {
								if ((pow(2, i).toInt & keep) >> i == 1) {
									//										println((((0xff << i * 8) & data) >> i * 8).toString(16))
									scoreboard.pushRef((((0xff << i * 8) & data) >> i * 8).toInt)
								}
							}
						}
					}

					def ResetHeader(): Unit = {
						dut.io.headerIn_AXIS.port.data #= 0
						dut.io.headerIn_AXIS.port.valid #= false
						dut.io.headerIn_AXIS.port.keep #= 0
						dut.io.headerIn_AXIS.port.user #= 0
					}

					while (true) {
						if (!dut.getHeader.toBoolean) {
							SetHeader()
							dut.clockDomain.waitRisingEdge()
							PushHeaderInScoreBoard()
							ResetHeader()
						}
						dut.clockDomain.waitRisingEdge()
					}
				}
			}

			def RandomReady(): Unit ={
				fork {
					while (true) {
						if (Random.nextFloat() < 0.05f) {
							dut.io.dataOut_AXIS.port.ready #= false
							dut.clockDomain.waitRisingEdge(Random.nextInt(4))
							dut.io.dataOut_AXIS.port.ready #= true
						}
						dut.clockDomain.waitRisingEdge()
					}
				}
			}

			def Monitor(): Unit ={
				fork {
					while (true) {
						if (dut.io.dataOut_AXIS.port.valid.toBoolean && dut.io.dataOut_AXIS.port.ready.toBoolean) {
							var data: BigInt = dut.io.dataOut_AXIS.port.data.toBigInt
							var keep: BigInt = dut.io.dataOut_AXIS.port.keep.toBigInt
//							println(data.toString(16) + "  --  " + keep.toString(2))
							for (i <- config.DATA_BYTE_WD - 1 downto 0) {
								if ((pow(2, i).toInt & keep) >> i == 1) {
//									println((((0xff << i * 8) & data) >> i * 8).toString(16))
									scoreboard.pushDut((((0xff << i * 8) & data) >> i * 8).toInt)
								}
							}
						}
						dut.clockDomain.waitRisingEdge()
					}
				}
			}

			driverInit()
			dut.clockDomain.waitRisingEdge(50)
			DataDrive()
			HeaderDrive()
			RandomReady()
			Monitor()


			dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 10000)

			//	      simSuccess()
			}

}

