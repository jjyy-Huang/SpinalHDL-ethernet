package merge

import spinal.core._
import spinal.core.sim._
import spinal.lib
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream

import scala.math._
case class MergeGenerics (
	DATA_WD : Int,
	DATA_BYTE_WD : Int,
	BYTE_WD : Int,
	BYTE_CNT_WD : Int
)

class AXI4StreamInsertTopLevel(config : MergeGenerics) extends Component {
	val io = new Bundle {
		val dataAxisIn = AXI4SDataInInterface(config.DATA_BYTE_WD)
		val headerAxisIn = AXI4SHeaderInInterface(config.DATA_BYTE_WD)
		val dataAxisOut = AXI4SInsertedInterface(config.DATA_BYTE_WD)
	}
	noIoPrefix()

	val getHeader = Bool() setAsReg() init False simPublic()
	val bypassMask = Bits(config.DATA_BYTE_WD bits) setAsReg() init 0
	val headerKeep = bypassMask
	val dataKeep = Bits(config.DATA_BYTE_WD bits) setAsReg() init 0
	val receiveComplete = Bool() setAsReg() init False

	val oneMoreCycle = Bool() setAsReg() init True

	when (io.dataAxisIn.port.lastFire) {
		oneMoreCycle := (headerKeep & io.dataAxisIn.port.keep).orR
	} elsewhen (io.dataAxisOut.port.lastFire) {
		oneMoreCycle := True
	} otherwise {
		oneMoreCycle := oneMoreCycle
	}

	when (io.headerAxisIn.port.fire) {
		getHeader := True
		bypassMask := io.headerAxisIn.port.keep
	} elsewhen (io.dataAxisOut.port.lastFire) {
		getHeader := False
	} otherwise {
		getHeader := getHeader
		bypassMask := bypassMask
	}

	when (io.dataAxisIn.port.lastFire) {
		dataKeep := io.dataAxisIn.port.keep
	} otherwise {
		dataKeep := dataKeep
	}

	when (io.dataAxisIn.port.lastFire) {
		receiveComplete := True
	} elsewhen (io.dataAxisOut.port.lastFire) {
		receiveComplete := False
	} otherwise {
		receiveComplete := receiveComplete
	}

	val rotateRightBits = UInt(config.BYTE_CNT_WD bits) setAsReg() init 0
	when (io.headerAxisIn.port.fire) {
		rotateRightBits := io.headerAxisIn.port.user.asUInt.resized
	} otherwise {
		rotateRightBits := rotateRightBits
	}

	val inputStreams = Vec(io.headerAxisIn.port.toBitStream(), io.dataAxisIn.port.toBitStream())
	val selectedStream = StreamMux(getHeader.asUInt, inputStreams)
	val forkedStreams = StreamFork(selectedStream, 2, true)

	val firstStage = forkedStreams(0) stage() s2mPipe() continueWhen(selectedStream.fire | receiveComplete)
	when (io.dataAxisIn.port.last.fall()) {
		when (!oneMoreCycle) {
			firstStage.setIdle()
		}
	}
	val secondStage = firstStage.clone()
	val bypassPath = forkedStreams(1) combStage()

	def byteMaskData(byteMask : Bits, data : Bits): Bits = {
		var dataWidth = data.getWidth / config.BYTE_WD
		var maskWidth = byteMask.getWidth
		require(maskWidth == dataWidth, s"ByteMaskData maskWidth${maskWidth} != dataWidth${dataWidth}")
		val vecByte = data.subdivideIn(maskWidth slices)
		vecByte.zipWithIndex.foreach { case (byte, idx) =>
			when (byteMask(idx)) {
				byte.clearAll()
			}
		}
		val maskedData = vecByte.reverse.reduceLeft(_ ## _)
		maskedData
	}

	secondStage <-/< firstStage.translateWith(byteMaskData(~bypassMask, firstStage.payload) |
																						byteMaskData(bypassMask, bypassPath.payload))

	bypassPath.ready := secondStage.ready

	def rotateRightByte(data : Bits, bias : UInt): Bits = {
		var result = cloneOf(data)
		result := data
		for (i <- bias.bitsRange) {
			result \= (bias(i) ? result.rotateRight(config.BYTE_WD << i) | result)
		}
		result
	}

	val commitStage = secondStage.clone()
	commitStage <-/< secondStage.translateWith(rotateRightByte(secondStage.payload, rotateRightBits))

	var delay : Int = 4
	val delayReg = Vec(Bool() setAsReg() init False , delay)
	val delayLoaded = Bool() setAsReg() init False
	when(io.dataAxisOut.port.isLast) {
		delayReg.foreach(reg => reg := False)
		delayLoaded.clear()
	} elsewhen (io.dataAxisIn.port.lastFire & io.dataAxisOut.port.ready & !delayLoaded) {
		delayLoaded.set()
		when (oneMoreCycle) {
			delayReg(1) := True
		} otherwise {
			delayReg(2) := True
		}
	} elsewhen (io.dataAxisIn.port.lastFire & !delayLoaded) {
		delayLoaded.set()
		when(oneMoreCycle) {
			delayReg(0) := True
		} otherwise {
			delayReg(1) := True
		}
	} elsewhen (receiveComplete && io.dataAxisOut.port.ready & delayLoaded) {
		for (i <- 1 until delay) {
			delayReg(i) := delayReg(i - 1)
		}
	}


	val lastKeep = Bits(config.DATA_BYTE_WD bits) setAsReg() init 0

	def rotateRightNoWarning(data : Bits, bias: UInt): Bits = {
		var result = cloneOf(data)
		result := data
		for (i <- bias.bitsRange) {
			result \= (bias(i) ? result.rotateRight(1 << i) | result)
		}
		result
	}
	when (receiveComplete & io.dataAxisOut.port.ready) {
		when (oneMoreCycle) {
			lastKeep := ((headerKeep ## dataKeep) rotateRight rotateRightBits)(2 * config.DATA_BYTE_WD - 1 downto config.DATA_BYTE_WD)
		} otherwise {
			lastKeep := ((headerKeep ## dataKeep) >> rotateRightBits).resized
		}
	} otherwise {
		lastKeep := lastKeep
	}

	io.dataAxisOut.port.valid := commitStage.valid
	io.dataAxisOut.port.data := commitStage.payload

	when(receiveComplete && io.dataAxisOut.port.ready && delayReg(delay-1)) {
		io.dataAxisOut.port.last := True
		io.dataAxisOut.port.keep := lastKeep
	} otherwise {
		io.dataAxisOut.port.last := False
		io.dataAxisOut.port.keep := Bits(config.DATA_BYTE_WD bits) setAllTo True
	}

	commitStage.ready := io.dataAxisOut.port.ready
}
