package ethernet

import spinal.core._
import spinal.core.sim._

case class ArpCacheGenerics(
                           IP_ADDR_WIDTH : Int = 32,
                           MAC_ADDR_WIDTH : Int = 48,
                           CACHE_DEPTH : Int = 256,
                           CACHE_INDEX_WIDTH : Int = 8,
                           CACHE_TAG_WIDTH : Int = 24
                           )

class ARPCache(config : ArpCacheGenerics) extends Component {
  val io = new Bundle {
    val ipAddrIn = in Bits(config.IP_ADDR_WIDTH bits) simPublic()
    val macAddrIn = in Bits(config.MAC_ADDR_WIDTH bits) simPublic()
    val writeEna = in Bool() simPublic()
    val readEna = in Bool() simPublic()

    val macAddrOut = out Bits(config.MAC_ADDR_WIDTH bits) setAsReg() init 0
    val cacheHit = out Bool() setAsReg() init False
    val cacheMiss = out Bool() setAsReg() init False
  }

  val arpCache = new Mem(Bits(config.IP_ADDR_WIDTH + config.MAC_ADDR_WIDTH - config.CACHE_INDEX_WIDTH bits), config.CACHE_DEPTH)

  val index = io.ipAddrIn.takeLow(config.CACHE_INDEX_WIDTH).asUInt
  val tag = io.ipAddrIn.takeHigh(config.CACHE_TAG_WIDTH)

  arpCache.write(
    enable = io.writeEna,
    address = index,
    data = tag ## io.macAddrIn
  )

  val outValid = RegNext(io.readEna, init = False)
  val readData = arpCache.readSync(
    enable = io.readEna,
    address = index
  )

  val isHit = tag === readData.takeHigh(config.CACHE_TAG_WIDTH)

  when(outValid) {
    io.cacheHit := isHit
    io.cacheMiss := !isHit
  } otherwise {
    io.cacheHit := False
    io.cacheMiss := False
  }
  io.macAddrOut := readData.takeLow(config.MAC_ADDR_WIDTH)

}
