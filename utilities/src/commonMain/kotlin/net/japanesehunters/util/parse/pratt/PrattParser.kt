package net.japanesehunters.util.parse.pratt

import arrow.core.NonEmptyCollection
import arrow.core.split
import net.japanesehunters.util.JvmNameJvmOnly
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
  ContinuationParser<Tok, LedContext<Tok, Ast>, Err, Ast>

fun <Tok : Any, Ast> astParser(
  name: String,
  block: suspend NudDsl<Tok, Any?, Ast>.() -> Ast,
) = parser<Tok, Any, Any?, Ast>(name) {
  val body =
    parser("$name (body)") a@{
      this@a.block()
    }
  val context =
    NudContext(
      body,
      0,
    )
  with(context) {
    +body
  }
}

fun <Tok : Any, Ast> astParser(
  name: String,
  nudParsers: NonEmptyCollection<NudParser<Tok, Any?, Ast>>,
  ledParsers: NonEmptyCollection<LedParser<Tok, Any?, Ast>>,
  cmp: Comparator<Ast>,
) = astParser(name) {
  val nudCtx = NudContext(ctx.exprParser, ctx.minBindingPower)
  val (restNudParser, firstNudParser) = nudParsers.split()!!
  val nud =
    with(nudCtx) {
      +select(firstNudParser, *restNudParser.toTypedArray(), cmp = cmp)
    }

  val (restLedParser, firstLedParser) = ledParsers.split()!!
  var ret = nud

  while (true) {
    val ledCtx = LedContext(ret, ctx.minBindingPower, ctx.exprParser)
    ret =
      option {
        with(ledCtx) {
          +select(
            firstLedParser,
            *restLedParser.toTypedArray(),
            cmp = cmp,
          )
        }
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
  block()
}

enum class Associativity {
  LEFT,
  RIGHT,
}

sealed interface PrattContext<Tok : Any, Ast> {
  val exprParser: NudParser<Tok, Any?, Ast>
  val minBindingPower: Int

  fun withPower(newPower: Int): PrattContext<Tok, Ast>
}

data class NudContext<Tok : Any, Ast>(
  override val exprParser: NudParser<Tok, Any?, Ast>,
  override val minBindingPower: Int,
) : PrattContext<Tok, Ast> {
  override fun withPower(newPower: Int): NudContext<Tok, Ast> =
    copy(minBindingPower = newPower)
}

data class LedContext<Tok : Any, Ast>(
  val leftExpr: Ast,
  override val minBindingPower: Int,
  override val exprParser: NudParser<Tok, Any?, Ast>,
) : PrattContext<Tok, Ast> {
  override fun withPower(newPower: Int): LedContext<Tok, Ast> =
    copy(minBindingPower = newPower)
}

fun <Tok : Any, Ast> PrattContext<Tok, Ast>.resetPower() = withPower(0)

typealias NudDsl<Tok, Err, Ast> =
  ParsingDsl<Tok, PrattContext<Tok, Ast>, Err, Ast>

typealias LedDsl<Tok, Err, Ast> =
  ParsingDsl<Tok, LedContext<Tok, Ast>, Err, Ast>

@get:JvmNameJvmOnly("nudExpr")
val <Tok : Any, Ast> NudDsl<Tok, *, Ast>.expr:
  NudParser<Tok, Any?, Ast>
  get() = ctx.exprParser

@get:JvmNameJvmOnly("ledExpr")
val <Tok : Any, Ast> LedDsl<Tok, *, Ast>.expr:
  ContinuationParser<Tok, LedContext<Tok, Ast>, Any?, Ast>
  get() =
    parser("${ctx.exprParser}") {
      val ctx = NudContext(ctx.exprParser, ctx.minBindingPower)
      with(ctx) {
        +ctx.exprParser
      }
    }

val <Tok : Any, Ast> LedDsl<Tok, *, Ast>.left: Ast
  get() = ctx.leftExpr

@JvmNameJvmOnly("nudBind")
inline fun <
  Tok : Any,
  Err,
  Ast,
> NudDsl<Tok, Err, Ast>.bind(
  power: Int,
  crossinline onInsufficientPower: (at: Cursor<Tok>, actual: Int) -> Err,
) = bindInternal(power, Associativity.RIGHT, onInsufficientPower)

@JvmNameJvmOnly("ledBind")
inline fun <
  Tok : Any,
  Err,
  Ast,
> LedDsl<Tok, Err, Ast>.bind(
  power: Int,
  associativity: Associativity,
  crossinline onInsufficientPower: (at: Cursor<Tok>, actual: Int) -> Err,
) = bindInternal(power, associativity, onInsufficientPower)

inline fun <
  Tok : Any,
  Ctx : PrattContext<Tok, Ast>,
  Err,
  Ast,
> ParsingDsl<Tok, Ctx, Err, Ast>.bindInternal(
  power: Int,
  associativity: Associativity,
  crossinline onInsufficientPower: (at: Cursor<Tok>, actual: Int) -> Err,
) {
  if (power * 2 < ctx.minBindingPower) {
    fail(onInsufficientPower(cursor, ctx.minBindingPower))
  }
  @Suppress("UNCHECKED_CAST")
  ctx =
    ctx
      .withPower(
        power * 2 +
          when (associativity) {
            Associativity.LEFT -> 1
            Associativity.RIGHT -> 0
          },
      ) as Ctx // nudCtx.withPower = nudCtx, ledCtx.withPower = ledCtx
}
