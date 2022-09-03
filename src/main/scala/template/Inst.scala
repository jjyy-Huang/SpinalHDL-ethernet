package template

import spinal.core._
import java.util.Calendar

object Inst {
	def main (args: Array[String]): Unit = {
		var data = Calendar.getInstance().getTime
		SpinalConfig(
			targetDirectory = "./verilog",
			oneFilePerComponent = false,
			removePruned = true,
			rtlHeader =
				s"""
					|@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
					|@Create : ${data}""".stripMargin
		)
			.generateVerilog(new Template)
	}
}
