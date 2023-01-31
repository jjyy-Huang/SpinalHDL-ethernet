package ethernet

import spinal.core.SpinalConfig

import java.util.Calendar

object Main extends App {
  val txConfig = TxGenerics()
  val headerGenConfig = HeaderGeneratorGenerics()
  val RxConfig = RxGenerics()
  val headerRecognizeConfig = HeaderRecognizerGenerics()

  SpinalConfig(
    targetDirectory = "./src/hdl",
    oneFilePerComponent = false,
    removePruned = false,
    rtlHeader = s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new TxTop(txConfig, headerGenConfig))


  SpinalConfig(
    targetDirectory = "./src/hdl",
    oneFilePerComponent = false,
    removePruned = false,
    rtlHeader =
      s"""
         |@Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
         |@Create : ${Calendar.getInstance().getTime}""".stripMargin
  )
    .generateVerilog(new RxTop(RxConfig, headerRecognizeConfig))
}
