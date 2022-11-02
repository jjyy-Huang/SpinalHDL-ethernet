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
		BYTE_WD = 8,
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
			SimTimeout(50000)
			def driverInit(): Unit ={
				dut.io.headerAxisIn.port.user #= 0
				dut.io.headerAxisIn.port.keep #= 0
				dut.io.headerAxisIn.port.data #= 0
				dut.io.headerAxisIn.port.valid #= false

				dut.io.dataAxisIn.port.keep #= 0
				dut.io.dataAxisIn.port.data #= 0
				dut.io.dataAxisIn.port.valid #= false
				dut.io.dataAxisIn.port.last #= false

				dut.io.dataAxisOut.port.ready #= true
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
						if (dut.io.dataAxisIn.port.valid.toBoolean && dut.io.dataAxisIn.port.ready.toBoolean) {
							var data: BigInt = dut.io.dataAxisIn.port.data.toBigInt
							var keep: BigInt = dut.io.dataAxisIn.port.keep.toBigInt
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
							if (dut.io.dataAxisIn.port.ready.toBoolean) {
								dut.io.dataAxisIn.port.data.randomize()
								times += 1
							}
							dut.io.dataAxisIn.port.valid #= true
							dut.io.dataAxisIn.port.keep #= pow(2, config.DATA_BYTE_WD).toInt - 1
							dut.io.dataAxisIn.port.last #= false
							dut.clockDomain.waitRisingEdge()
							PushDataInScoreBoard()
							dut.io.dataAxisIn.port.valid #= false
						}
						dut.io.dataAxisIn.port.data.randomize()
						dut.io.dataAxisIn.port.valid #= true
						dut.io.dataAxisIn.port.last #= true
						dut.io.dataAxisIn.port.keep #= SumUpPow(Random.nextInt(config.DATA_BYTE_WD) + 1)
						dut.clockDomain.waitRisingEdge()
						while (!dut.io.dataAxisIn.port.ready.toBoolean) {
							dut.clockDomain.waitRisingEdge()
						}
						dut.io.dataAxisIn.port.data #= 0
						dut.io.dataAxisIn.port.valid #= false
						dut.io.dataAxisIn.port.last #= false
						dut.io.dataAxisIn.port.keep #= pow(2, config.DATA_BYTE_WD).toInt - 1
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
						dut.io.headerAxisIn.port.data.randomize()
						dut.io.headerAxisIn.port.valid #= true
						dut.io.headerAxisIn.port.keep #= SumDownPow(t)
						dut.io.headerAxisIn.port.user #= t
					}

					def PushHeaderInScoreBoard(): Unit ={
						if (dut.io.headerAxisIn.port.valid.toBoolean && dut.io.headerAxisIn.port.ready.toBoolean) {
							var data: BigInt = dut.io.headerAxisIn.port.data.toBigInt
							var keep: BigInt = dut.io.headerAxisIn.port.keep.toBigInt
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
						dut.io.headerAxisIn.port.data #= 0
						dut.io.headerAxisIn.port.valid #= false
						dut.io.headerAxisIn.port.keep #= 0
						dut.io.headerAxisIn.port.user #= 0
					}

					while (true) {
						if (!dut.getHeader.toBoolean) {
							SetHeader()
							dut.clockDomain.waitRisingEdge()
							PushHeaderInScoreBoard()
							ResetHeader()
						}
						dut.clockDomain.waitRisingEdge(Random.nextInt(10))
					}
				}
			}

			def RandomReady(): Unit ={
				fork {
					while (true) {
						if (Random.nextFloat() < 0.05f) {
							dut.io.dataAxisOut.port.ready #= false
							dut.clockDomain.waitRisingEdge(Random.nextInt(5))
							dut.io.dataAxisOut.port.ready #= true
						}
						dut.clockDomain.waitRisingEdge()
					}
				}
			}

			def Monitor(): Unit ={
				fork {
					while (true) {
						if (dut.io.dataAxisOut.port.valid.toBoolean && dut.io.dataAxisOut.port.ready.toBoolean) {
							var data: BigInt = dut.io.dataAxisOut.port.data.toBigInt
							var keep: BigInt = dut.io.dataAxisOut.port.keep.toBigInt
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
//			RandomReady()
			Monitor()


			dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 10000)

			//	      simSuccess()
			}

}

