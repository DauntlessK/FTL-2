package ftl2.math

import ftl2.f

class ConstFPoint(
    override val xf: Float,
    override val yf: Float
) : FPoint {
    constructor(other: IPoint) : this(other.x.f, other.y.f)
    constructor(other: FPoint) : this(other.xf, other.yf)

    override val fConst: ConstFPoint get() = this

    companion object {
        val ZERO: ConstFPoint = ConstFPoint(0f, 0f)
    }
}
