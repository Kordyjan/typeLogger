//> using scala "3.3.0-RC3"
//> using file "src"

def examples: Unit =
  import Types.*
  import typelogger.*

  // Use the `logInferredType` method to log the type that would be inferred for the expression
  logInferredType(10 * 4.2)

  // You can pass a block of code to it...
  logInferredType:
    val x = 10
    val a = A(15)
    if x > 5 then a else B("hello")

  // ... or use it as an extension method.
  List(1, 2, 3).logInferredType

  // If you wnated to see the computed types before the inference you can use the `logComputedType` method instead.
  logComputedType:
    val x = 10
    val a = A(15)
    if x > 5 then a else B("hello")

  // You can nest the calls to both functions.
  logInferredType:
    first(
      first(List("abc", "def", "ghi")).logComputedType
    ).logComputedType


  // Both functions can give you simplified and dealiased form o type
  returnDeepAliased(Nil)
    .logComputedType
    .logInferredType

  // `logComputedType` can also show how the type is widened
  val x = new Instance
  logComputedType(x)
  logComputedType(x.inner)

end examples

object Types:
  trait Divergent
  case class A(value: Int) extends Divergent
  case class B(value: String) extends Divergent
  case class C(valiue: Double) extends Divergent

  type Element[T] = T match
    case List[t] => t
    case String  => Char

  def first(xs: List[_] | String): Element[xs.type] = xs match
    case x: List[_] => x.head
    case s: String  => s.head

  type DeepAlias[T] = List[Option[Element[T]]]

  def returnDeepAliased[T](t: T): DeepAlias[T] = ???

  trait Base:
    type Inner
    def inner: Inner

  class Instance extends Base:
    type Inner = Int
    def inner = 10
end Types

