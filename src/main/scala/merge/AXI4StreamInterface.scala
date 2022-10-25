package merge

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis._

case class AXI4SDataInInterface(DATA_BYTE_WIDTH : Int) extends Bundle {
	val cfg = Axi4StreamConfig(DATA_BYTE_WIDTH, useKeep = true, useLast = true)
	val port = slave(Axi4Stream(cfg))
}

case class AXI4SHeaderInInterface(DATA_BYTE_WIDTH : Int) extends Bundle {
	val cfg = Axi4StreamConfig(DATA_BYTE_WIDTH, useKeep = true, userWidth = 1, useUser = true)
	val port = slave(Axi4Stream(cfg))
}

case class AXI4SInsertedInterface(DATA_BYTE_WIDTH : Int) extends Bundle {
	val cfg = Axi4StreamConfig(DATA_BYTE_WIDTH, useKeep = true, useLast = true)
	val port = master(Axi4Stream(cfg))
}







