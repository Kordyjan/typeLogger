//> using scala "3.3.0-RC3"
//> using file "src"

package playground

import typelogger.*

trait Divergent
case class A(value: Int) extends Divergent
case class B(value: String) extends Divergent
case class C(valiue: Double) extends Divergent

object Divergent:
  type AB = A | B
  def produceAB(arg: Int | String): AB = arg match
    case n: Int    => A(n)
    case s: String => B(s)

type Element[T] = T match
  case List[t] => t
  case String  => Char

def first(xs: List[_] | String): Element[xs.type] = xs match
  case x: List[_] => x.head
  case s: String  => s.head

type Alias[T] = List[Option[T]]

def returnAliased[T]: Alias[T] = ???

type DeepAlias[T] = List[Option[Element[T]]]

def returnDeepAliased[T]: DeepAlias[T] = ???

@main def run =
  val x = 1
  logComputedType:
    logInferredType:
      first(List(1, 2, 3))

  logComputedType:
    logInferredType:
      returnAliased[Int]

  returnDeepAliased[Nil.type]
    .logComputedType
    .logInferredType
