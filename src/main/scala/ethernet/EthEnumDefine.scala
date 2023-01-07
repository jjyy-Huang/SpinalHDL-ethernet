package ethernet

import spinal.core._

object PacketMTUEnum extends SpinalEnum {
  val mtu256, mtu512, mtu1024, mtu2048, mtu4096 = newElement()
}

object ProtocolTypeEnum extends SpinalEnum {
  val none, arp, udp, icmp, ethernet = newElement()
}