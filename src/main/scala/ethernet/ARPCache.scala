package ethernet

import spinal.core._
import spinal.core.sim._
import spinal.lib._

case class ArpCacheGenerics(
                           IP_ADDR_WIDTH : Int = 32,
                           MAC_ADDR_WIDTH : Int = 48,
                           CACHE_DEPTH : Int = 256,
                           CACHE_INDEX_WIDTH : Int = 8,
                           CACHE_TAG_WIDTH : Int = 24
                           )
case class ARPCacheInterface(config: ArpCacheGenerics) extends Bundle with IMasterSlave {
  val ipAddrWrite = Bits (config.IP_ADDR_WIDTH bits) simPublic()
  val macAddrWrite = Bits (config.MAC_ADDR_WIDTH bits) simPublic()
  val writeEna = Bool() simPublic()
  val readEna = Bool() simPublic()

  val macAddrRead = Bits (config.MAC_ADDR_WIDTH bits) setAsReg() init 0
  val cacheHit = Bool() setAsReg() init False
  val cacheMiss = Bool() setAsReg() init False

  override def asMaster(): Unit = {
    out(ipAddrWrite, macAddrWrite, writeEna, readEna)
    in(macAddrRead, cacheHit, cacheMiss)
  }
}

case class ARPCacheReplyInterface(config: ArpCacheGenerics) extends Bundle with IMasterSlave {
  val macAddr = Bits(config.MAC_ADDR_WIDTH bits)
  val ipAddr = Bits(config.IP_ADDR_WIDTH bits)
  override def asMaster(): Unit = {
    out(macAddr, ipAddr)
  }
}

class ARPCache(config : ArpCacheGenerics) extends Component {
  val io = new Bundle {
    val port = slave (ARPCacheInterface(config))
  }
  noIoPrefix()

  val arpCache = new Mem(Bits(config.IP_ADDR_WIDTH + config.MAC_ADDR_WIDTH - config.CACHE_INDEX_WIDTH bits), config.CACHE_DEPTH)

  val index = io.port.ipAddrWrite.takeLow(config.CACHE_INDEX_WIDTH).asUInt
  val tag = io.port.ipAddrWrite.takeHigh(config.CACHE_TAG_WIDTH)

  arpCache.write(
    enable = io.port.writeEna,
    address = index,
    data = tag ## io.port.macAddrWrite
  )

  val outValid = RegNext(io.port.readEna, init = False)
  val readData = arpCache.readSync(
    enable = io.port.readEna,
    address = index
  )

  val isHit = tag === readData.takeHigh(config.CACHE_TAG_WIDTH)

  when(outValid) {
    io.port.cacheHit := isHit
    io.port.cacheMiss := !isHit
  } otherwise {
    io.port.cacheHit := False
    io.port.cacheMiss := False
  }
  io.port.macAddrRead := readData.takeLow(config.MAC_ADDR_WIDTH)

}
