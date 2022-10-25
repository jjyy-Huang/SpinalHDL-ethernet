package merge

import spinal.core._
import java.util.Calendar


object AXI4StreamInsertTopLevelVerilog {
	def main(args: Array[String]): Unit = {
		val config = MergeGenerics(
			DATA_WD = 32,
			DATA_BYTE_WD = 32 / 8,
			BYTE_CNT_WD = 3
		)

		SpinalConfig(
			targetDirectory = "./verilog",
			oneFilePerComponent = false,
			removePruned = false,
			rtlHeader =
				s"""
					 |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
					 |@Create : ${Calendar.getInstance().getTime}""".stripMargin
		)
			.generateVerilog(new AXI4StreamInsertTopLevel(config))
	}
}
