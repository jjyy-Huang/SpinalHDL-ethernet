package merge

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.math._
case class MergeGenerics (
	DATA_WD : Int,
	DATA_BYTE_WD : Int,
	BYTE_CNT_WD : Int
)

class AXI4StreamInsertTopLevel(config : MergeGenerics) extends Component {

	val io = new Bundle {
		val dataIn_AXIS = AXI4SDataInInterface(config.DATA_BYTE_WD)
		val headerIn_AXIS = AXI4SHeaderInInterface(config.DATA_BYTE_WD)
		val dataOut_AXIS = AXI4SInsertedInterface(config.DATA_BYTE_WD)
	}
	noIoPrefix()

	val getHeader = Bool() setAsReg() init False simPublic()
	val receiveComplete = Bool setAsReg() init False

	when (io.headerIn_AXIS.port.fire) {
		getHeader := True
	} elsewhen (io.dataOut_AXIS.port.isLast) {
		getHeader := False
	} otherwise {
		getHeader := getHeader
	}

	when (io.dataIn_AXIS.port.isLast && getHeader && io.dataIn_AXIS.port.fire) {
		receiveComplete := True
	} elsewhen (io.dataOut_AXIS.port.isLast) {
		receiveComplete := False
	} otherwise {
		receiveComplete := receiveComplete
	}

	val selectedMode = UInt(config.DATA_BYTE_WD bits) setAsReg() init 0
	when (io.headerIn_AXIS.port.fire) {
		selectedMode := io.headerIn_AXIS.port.user.asUInt
	} otherwise {
		selectedMode := selectedMode
	}

	val remainCnt = UInt(config.DATA_BYTE_WD bits) setAsReg() init 0
	when(io.dataIn_AXIS.port.isLast) {
		remainCnt := selectedMode + CountOne(io.dataIn_AXIS.port.keep)
	} otherwise {
		remainCnt := remainCnt
	}

	val delayCycle = UInt(2 bits)
	val lastKeep = Bits(config.DATA_BYTE_WD bits)



	val delayCnt = UInt(4 bits) setAsReg() init 0
	when (io.dataOut_AXIS.port.isLast) {
		delayCnt := 0
	} elsewhen (receiveComplete && io.dataOut_AXIS.port.ready) {
		delayCnt := delayCnt + 1
	} otherwise {
		delayCnt := delayCnt
	}

	switch (remainCnt) {
		for (i <- 1 to config.DATA_BYTE_WD * 2) {
			is (i) {
				def sumPow(a: Int): Int = {
					var sum: Int = 0
					for (j <- config.DATA_BYTE_WD - a until  config.DATA_BYTE_WD) {
						sum += pow(2, j).toInt
					}
					sum
				}
				if (i <= config.DATA_BYTE_WD) {
					delayCycle := 0
					lastKeep := B(sumPow(i))
				} else {
					delayCycle := 1
					lastKeep := B(sumPow(i - config.DATA_BYTE_WD))
				}
			}
		}
		default {
			delayCycle := 0
			lastKeep := 15
		}
	}

	val dataCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0, Bool setAsReg() init False)
	val issueCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0)
	val issueCacheReady = Bool()
	val bypassData = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits), Bool)

	issueCacheReady := io.dataOut_AXIS.port.ready

	io.dataIn_AXIS.port.ready := issueCacheReady && getHeader
	io.headerIn_AXIS.port.ready := issueCacheReady && !getHeader

	bypassData.zipWithIndex.foreach { case (u, i) =>
		u._1 := io.dataIn_AXIS.port.data(8 * (i + 1) - 1 downto 8 * i)
		u._2 := io.dataIn_AXIS.port.keep(i)
	}

	when (io.dataIn_AXIS.port.fire) {
		dataCache.zipWithIndex.foreach { case (u, i) =>
			u._1 := io.dataIn_AXIS.port.data( 8 * (i + 1) - 1 downto 8 * i)
			u._2 := io.dataIn_AXIS.port.keep(i)
		}
	} elsewhen (io.dataOut_AXIS.port.isLast) {
		dataCache.foreach { u => u._1 := 0; u._2 := False }
	} otherwise {
		dataCache.foreach{ u => u._1 := u._1; u._2 := u._2}
	}

	val tmp = Array.concat(bypassData, dataCache)
	val mappedData = Array.fill(config.DATA_BYTE_WD)(Bits(8 bits), Bool)
	mappedData.zipWithIndex.foreach { case (u, idx) =>
		switch(selectedMode) {
			for (i <- 0 to config.DATA_BYTE_WD) {
				is(i) {
					u._1 := tmp(config.DATA_BYTE_WD - 1 - idx + i)._1
					u._2 := tmp(config.DATA_BYTE_WD - 1 - idx + i)._2
				}
			}
			default {
				u._1 := B(0)
				u._2 := False
			}
		}
	}

	val mappedHeader = Array.fill(config.DATA_BYTE_WD)(Bits(8 bits), Bool)
	mappedHeader.zipWithIndex.foreach { case (u, idx) =>
		switch(io.headerIn_AXIS.port.user.resize(config.BYTE_CNT_WD).asUInt) {
			for (i <- 0 until config.DATA_BYTE_WD - idx) {
				is (config.DATA_BYTE_WD - i) {
					u._1 := io.headerIn_AXIS.port.data(8 * (config.DATA_BYTE_WD - idx - i) - 1 downto 8 * (config.DATA_BYTE_WD - 1 - idx - i))
					u._2 := True
				}
				default {
					u._1 := B(0)
					u._2 := False
				}
			}
		}
	}

	issueCache.zipWithIndex.foreach { case (u, idx) =>
		when (!getHeader) {
			when (mappedHeader(idx)._2) {
				u := mappedHeader(idx)._1
			} otherwise {
				u := u
			}
		} otherwise {
			when (io.dataIn_AXIS.port.fire) {
				when (mappedData(idx)._2 && issueCacheReady) {
					u := mappedData(idx)._1
				} otherwise {
					u := u
				}
			} elsewhen (receiveComplete && issueCacheReady) {
				u := mappedData(idx)._1
			} otherwise {
				u := u
			}
		}
	}


	val issueCacheLoaded = Bool() setAsReg() init False
	val combineData= issueCache.reduce(_ ## _)

	when (io.dataIn_AXIS.port.fire) {
		issueCacheLoaded := True
	} elsewhen (io.dataOut_AXIS.port.isLast) {
		issueCacheLoaded := False
	} elsewhen (receiveComplete && issueCacheReady) {
		issueCacheLoaded := True
	} elsewhen (issueCacheLoaded && issueCacheReady) {
		issueCacheLoaded := False
	} otherwise {
		issueCacheLoaded := issueCacheLoaded
	}
	io.dataOut_AXIS.port.valid := issueCacheLoaded
	io.dataOut_AXIS.port.data := combineData

	when (receiveComplete && io.dataOut_AXIS.port.ready && delayCnt === delayCycle) {
		io.dataOut_AXIS.port.last := True
		io.dataOut_AXIS.port.keep := lastKeep
	} otherwise {
		io.dataOut_AXIS.port.last := False
		io.dataOut_AXIS.port.keep := Bits (config.DATA_BYTE_WD bits) setAllTo True
	}
}
