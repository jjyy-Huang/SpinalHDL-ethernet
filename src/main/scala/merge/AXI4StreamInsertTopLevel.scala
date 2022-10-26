package merge

import spinal.core._

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

	val getHeader = Bool() setAsReg() init False
	val transComplete = io.dataOut_AXIS.port.isLast setAsReg() init False
	val receiveComplete = Bool setAsReg() init False

	when (io.headerIn_AXIS.port.fire) {
		getHeader := True
	} elsewhen (transComplete) {
		getHeader := False
	} otherwise {
		getHeader := getHeader
	}

	when (io.dataIn_AXIS.port.isLast) {
		receiveComplete := True
	} otherwise {
		receiveComplete := False
	}

	val selectedMode = UInt(config.BYTE_CNT_WD bits) setAsReg() init 0
	when (io.headerIn_AXIS.port.fire) {
		selectedMode := io.headerIn_AXIS.port.user.resize(config.BYTE_CNT_WD).asUInt
	} otherwise {
		selectedMode := selectedMode
	}

	val dataCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0, Bool setAsReg() init False)
	val issueCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0, Bool setAsReg() init False)
	val issueCacheReady = Bool setAsReg() init False
	val bypassData = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits), Bool)

	issueCacheReady := io.dataOut_AXIS.port.ready || ~io.dataOut_AXIS.port.valid

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
				u._1 := mappedHeader(idx)._1
				u._2 := mappedHeader(idx)._2
			} otherwise {
				u._1 := u._1
				u._2 := u._2
			}
		} otherwise {
			when (io.dataIn_AXIS.port.fire) {
				when (mappedData(idx)._2) {
					u._1 := mappedData(idx)._1
					u._2 := mappedData(idx)._2
				} otherwise {
					u._1 := u._1
					u._2 := u._2
				}
			} elsewhen (receiveComplete && issueCacheReady) {
				u._1 := mappedData(idx)._1
				u._2 := mappedData(idx)._2
			} otherwise {
				u._1 := u._1
				u._2 := u._2
			}
		}
	}


	val issueCacheLoaded = Bool() setAsReg() init False
	val (t1, t2) = issueCache.reduce((a, b) => (a._1 ## b._1, a._2 && b._2))
	val (t3, t4) = issueCache.reduce((a, b) => (a._1 ## b._1, a._2 || b._2))
	when (t2) {
		issueCacheLoaded := True
	} elsewhen (t4 && receiveComplete && issueCacheReady) {
		issueCacheLoaded := True
	} otherwise {
		issueCacheLoaded := False
	}
	io.dataOut_AXIS.port.valid := issueCacheLoaded
	io.dataOut_AXIS.port.data := t1
	io.dataOut_AXIS.port.keep := 0

	when (t4 && receiveComplete && issueCacheReady) {
		io.dataOut_AXIS.port.last := receiveComplete
	} otherwise {
		io.dataOut_AXIS.port.last := False
	}

}
