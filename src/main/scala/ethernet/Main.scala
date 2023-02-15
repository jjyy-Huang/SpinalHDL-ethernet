package ethernet

import spinal.core.{SpinalConfig, SpinalVerilog}

import java.util.Calendar

object Main extends App {
  val txConfig = TxGenerics()
  val headerGenConfig = HeaderGeneratorGenerics()
  val RxConfig = RxGenerics()
  val headerRecognizeConfig = HeaderRecognizerGenerics()

  SpinalConfig(
    targetDirectory = "./hdl",
    oneFilePerComponent = true,
    removePruned = false,
    rtlHeader = s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new TxTop(txConfig, headerGenConfig))


  SpinalConfig(
    targetDirectory = "./hdl",
    oneFilePerComponent = true,
    removePruned = false,
    rtlHeader =
      s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new RxTop(RxConfig, headerRecognizeConfig))
}
