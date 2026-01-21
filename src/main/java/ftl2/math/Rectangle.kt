package ftl2.math

class Rectangle(val pos: IPoint, val size: IPoint) {
    val centre get() = pos + size.divideTruncate(2f)
}
