package net.japanesehunters.util.parse.pratt

import arrow.core.NonEmptyCollection
import arrow.core.split
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.parse.ContinuationParser
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.parser
import net.japanesehunters.util.parse.select

typealias AstParser<Tok, Ast> =
  ContinuationParser<Tok, Any, Any?, Ast>

typealias NudParser<Tok, Err, Ast> =
  ContinuationParser<Tok, PrattContext<Tok, Ast>, Err, Ast>

typealias LedParser<Tok, Err, Ast> =
  ContinuationParser<Tok, LedContext<Tok, Ast>, Err, LedParseResult<Ast>>

@Suppress("RemoveExplicitTypeArguments", "RedundantWith")
fun <Tok : Any, Ast> astParser(
  name: String,
  block: suspend NudDsl<Tok, Any?, Ast>.() -> Ast,
) = parser<Tok, Any, Any?, Ast>(name) {
  val context =
    NudContext(
      self,
    )

  with(context) {
    +parser<Tok, PrattContext<Tok, Ast>, Any?, Ast>(name) {
      block()
    }
  }
}

fun <Tok : Any, Ast> astParser(
  name: String,
  nudParsers: NonEmptyCollection<NudParser<Tok, Any?, Ast>>,
  ledParsers: NonEmptyCollection<LedParser<Tok, Any?, Ast>>,
  cmp: Comparator<Ast>,
) = parser(name) {
  val nudCtx = NudContext(self)
  val (restNudParser, firstNudParser) = nudParsers.split()!!
  val nud =
    with(nudCtx) {
      +select(firstNudParser, *restNudParser.toTypedArray(), cmp = cmp)
    }

  val (restLedParser, firstLedParser) = ledParsers.split()!!
  var ret = nud
  var bp = 0

  while (true) {
    val ledCtx = LedContext(ret, bp, self)
    ret = option {
      val (led, newBp) =
        with(ledCtx) {
          +select(
            firstLedParser,
            *restLedParser.toTypedArray(),
            cmp = { a, b -> cmp.compare(a.expr, b.expr) },
          )
        }
      bp = newBp
      led
    } ?: break
  }
  ret
}

fun <Tok : Any, Err, Ast> nudParser(
  name: String,
  block: suspend NudDsl<Tok, Err, Ast>.() -> Ast,
) = parser(name) { block() }

fun <Tok : Any, Err, Ast> ledParser(
  name: String,
  block: suspend LedDsl<Tok, Err, Ast>.() -> Ast,
) = parser(name) {
  LedParseResult(block(), ctx.minBindingPower)
}

enum class Associativity {
  LEFT,
  RIGHT,
}

sealed interface PrattContext<Tok : Any, out Ast> {
  val exprParser: AstParser<Tok, Ast>
}

data class NudContext<Tok : Any, out Ast>(
  override val exprParser: AstParser<Tok, Ast>,
) : PrattContext<Tok, Ast>

data class LedContext<Tok : Any, out Ast>(
  val leftExpr: Ast,
  val minBindingPower: Int,
  override val exprParser: AstParser<Tok, Ast>,
) : PrattContext<Tok, Ast>

data class LedParseResult<out Ast>(
  val expr: Ast,
  val nextBindingPower: Int,
)

typealias NudDsl<Tok, Err, Ast> =
  ParsingDsl<Tok, PrattContext<Tok, Ast>, Err, Ast>

typealias LedDsl<Tok, Err, Ast> =
  ParsingDsl<Tok, LedContext<Tok, Ast>, Err, LedParseResult<Ast>>

val <
  Tok : Any,
  Ctx : PrattContext<Tok, Ast>,
  Ast,
> ParsingDsl<Tok, Ctx, *, *>.expr:
  AstParser<Tok, Ast>
  get() = ctx.exprParser

val <Tok : Any, Ast> LedDsl<Tok, *, Ast>.left: Ast
  get() = ctx.leftExpr

inline fun <Tok : Any, Err, Ast> LedDsl<Tok, Err, Ast>.bindOr(
  power: Int,
  associativity: Associativity,
  onInsufficientPower: (at: Cursor<Tok>, actual: Int) -> Err,
) {
  if (power * 2 < ctx.minBindingPower) {
    fail(onInsufficientPower(cursor, ctx.minBindingPower))
  }
  ctx =
    ctx.copy(
      minBindingPower =
        power * 2 +
          when (associativity) {
            Associativity.LEFT -> 1
            Associativity.RIGHT -> 0
          },
    )
}
