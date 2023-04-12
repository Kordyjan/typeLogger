package typelogger

import quoted.*

extension (inline t: Any)
  transparent inline def logComputedType: Any = ${ logComputedTypeImpl('t) }

extension [T](inline t: T)
  transparent inline def logInferredType: T = ${ logInferredTypeImpl[T]('t) }

private def logComputedTypeImpl(t: Expr[Any])(using Quotes): Expr[Any] =
  import quotes.reflect.*

  val tpe = t.asTerm.tpe
  val position = findCallPart(t)
  val msg = renderType(tpe) + renderWidened(tpe)

  report.info(msg, position)
  t
end logComputedTypeImpl

private def logInferredTypeImpl[T: Type](t: Expr[T])(using Quotes): Expr[T] =
  import quotes.reflect.*

  val inferredType = TypeRepr.of[T]
  val position = findCallPart(t)
  report.info(s"${renderType(inferredType)}", position)
  t
end logInferredTypeImpl

private def findCallPart(using Quotes)(
    paramExpr: Expr[Any]
): quotes.reflect.Position =
  import quotes.reflect.*

  def findCallPartInRange(callPosition: Position): Option[Position] =
    callPosition.sourceCode
      .flatMap { text =>
        val wordRegex = """\w+""".r
        wordRegex.findFirstMatchIn(text).map(m => (m.start, m.end))
      }
      .map { case (start, end) =>
        quotes.reflect.Position(
          callPosition.sourceFile,
          callPosition.start + start,
          callPosition.start + end
        )
      }

  val macroPosition = Position.ofMacroExpansion
  val paramPosition = paramExpr.asTerm.pos
  val frontRange =
    Position(macroPosition.sourceFile, macroPosition.start, paramPosition.start)
  val backRange =
    Position(macroPosition.sourceFile, paramPosition.end, macroPosition.end)

  List(frontRange, backRange)
    .flatMap(findCallPartInRange)
    .headOption
    .getOrElse(macroPosition)
end findCallPart

private def renderType(using Quotes)(tpe: quotes.reflect.TypeRepr): String =
  import quotes.reflect.*
  val rendered = tpe.show
  val simplified = tpe.simplified.dealias.simplified.show
  if (rendered == simplified) rendered else s"$rendered (=:= $simplified)"

private def renderWidened(using Quotes)(tpe: quotes.reflect.TypeRepr): String =
  import quotes.reflect.*
  val widened = tpe.widen
  if widened.show == tpe.show then ""
  else
    val rendered = renderType(widened)
    s" <:< $rendered"
