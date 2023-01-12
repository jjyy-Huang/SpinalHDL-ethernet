package ethernet

import spinal.core._
import ethernet._

import java.util.Calendar

object UDPTxMain extends App {
  val txConfig = TxGenerics()
  val headerConfig = HeaderGeneratorGenerics()

  SpinalConfig(
    targetDirectory = "./verilog",
    oneFilePerComponent = false,
    removePruned = false,
    rtlHeader =
      s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new TxTop(txConfig, headerConfig))
}
