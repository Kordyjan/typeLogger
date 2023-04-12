package typelogger

import quoted.*

extension (inline t: Any)
  /** Logs the computed type of the expression.
    *
    * The computed type is the type of the expression as it is seen by the
    * compiler and used to compute types of enclosing expressions. It is the
    * same or narrower (more specific) than type that would be inferred for
    * given expression. The function can also log the widening of the computed
    * type, or its simplified and dealiased form if, they are different than
    * base form.
    *
    * The function has no footprint after the typer phase.
    *
    * @return
    *   the expression itself
    */
  transparent inline def logComputedType: Any = ${ logComputedTypeImpl('t) }

extension [T](inline t: T)
  /** Logs the inferred type of the expression. x By inferred type of the
    * expression `t`, we understand the type that `T` is dealiased to if the
    * expression `t` is passed to the function with signature `def f[T](t: T):
    * Unit`. In rare cases, this can be different than the type inferred for a
    * value defined as `val x = t`.
    *
    * This function doesn't bind the type of the expression to `T` and has no
    * footprint after the typer phase.
    *
    * @return
    *   the expression itself
    */
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
