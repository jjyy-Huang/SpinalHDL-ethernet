package template

import spinal.core._

class Template extends Component{
	val io = new Bundle {
		val a = in Bool()
		val b = out Bool()
	}
	noIoPrefix()

	io.b := RegNext(!io.a) init False

}
