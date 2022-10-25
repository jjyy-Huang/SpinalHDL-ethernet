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
	val transComplete = io.dataOut_AXIS.port.isLast

	when (io.headerIn_AXIS.port.fire) {
		getHeader := True
	} elsewhen (transComplete) {
		getHeader := False
	} otherwise {
		getHeader := getHeader
	}

	val selectedMode = UInt(config.BYTE_CNT_WD bits) setAsReg() init 0
	when (io.headerIn_AXIS.port.fire) {
		selectedMode := io.headerIn_AXIS.port.user.resize(config.BYTE_CNT_WD).asUInt
	} otherwise {
		selectedMode := selectedMode
	}


	io.dataIn_AXIS.port.ready := io.dataOut_AXIS.port.ready && !getHeader
	io.headerIn_AXIS.port.ready := io.dataOut_AXIS.port.ready && getHeader

	val dataCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0)
	val dataCacheReady = Array.fill(config.DATA_BYTE_WD)(Bool setAsReg() init False)
	val issueCache = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits) setAsReg() init 0)
	val bypassData = Array.fill(config.DATA_BYTE_WD)(Bits(config.DATA_WD/config.DATA_BYTE_WD bits))

	bypassData.zipWithIndex.foreach { case (u, i) =>
		u := io.dataIn_AXIS.port.data(8 * (i + 1) - 1 downto 8 * i)
	}
	when (io.dataIn_AXIS.port.fire) {
		dataCache.zipWithIndex.foreach { case (u, i) =>
			u := io.dataIn_AXIS.port.data( 8 * (i + 1) - 1 downto 8 * i)
		}
	} otherwise {
		dataCache.foreach{ u => u := u}
	}

	val tmp = Array.concat(bypassData, dataCache)
	val mappedData = Array.fill(config.DATA_BYTE_WD)(Bits(8 bits))
	mappedData.zipWithIndex.foreach { case (u, idx) =>
		switch(selectedMode) {
			for (i <- 0 to config.DATA_BYTE_WD) {
				is(i) {
					u := tmp(config.DATA_BYTE_WD - 1 - idx + i)
				}
			}
			default {
				u := B(0)
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
			u := u
		}
	}



	io.dataOut_AXIS.port.valid := False
	io.dataOut_AXIS.port.data := B(0)
	io.dataOut_AXIS.port.keep := B(0)
	io.dataOut_AXIS.port.last := False

}
