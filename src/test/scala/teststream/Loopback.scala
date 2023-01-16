package teststream

import ethernet._
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis._

class Loopback(txConfig: TxGenerics,
               headerGenerateConfig: HeaderGeneratorGenerics,
               rxConfig: RxGenerics,
               headerRecognizeConfig: HeaderRecognizerGenerics) extends Component {

  val dataAxisInCfg = Axi4StreamConfig(
    dataWidth = txConfig.DATA_BYTE_CNT,
    userWidth = txConfig.DATA_TUSER_WIDTH,
    useStrb = txConfig.DATA_USE_TSTRB,
    useKeep = txConfig.DATA_USE_TKEEP,
    useLast = txConfig.DATA_USE_TLAST,
    useUser = txConfig.DATA_USE_TUSER
  )

  val dataAxisOutCfg = Axi4StreamConfig(
    dataWidth = rxConfig.DATA_BYTE_CNT,
    userWidth = rxConfig.DATA_TUSER_WIDTH,
    useStrb = rxConfig.DATA_USE_TSTRB,
    useKeep = rxConfig.DATA_USE_TKEEP,
    useLast = rxConfig.DATA_USE_TLAST,
    useUser = rxConfig.DATA_USE_TUSER
  )

  val io = new Bundle {
    val metaIn = slave Stream MetaData()
    val dataAxisIn = slave(Axi4Stream(dataAxisInCfg))

    val metaOut = master Stream MetaData()
    val dataAxisOut = master(Axi4Stream(dataAxisOutCfg))
  }

  val tx = new TxTop(txConfig, headerGenerateConfig)
  val rx = new RxTop(rxConfig, headerRecognizeConfig)

  tx.io.metaIn << io.metaIn
  tx.io.dataAxisIn << io.dataAxisIn

  rx.io.dataAxisIn << tx.io.dataAxisOut

  io.dataAxisOut << rx.io.dataAxisOut
  io.metaOut << rx.io.metaOut

}
