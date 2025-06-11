package net.japanesehunters.util.parse.lex

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.OutOfBounds
import net.japanesehunters.util.collection.cursor
import net.japanesehunters.util.parse.ContinuationParser
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.ParsingDslMarker
import net.japanesehunters.util.parse.map
import net.japanesehunters.util.parse.parser

typealias Lexer<Err, Out> = ContinuationParser<Char, Any, Err, Out>

typealias LexingDsl<Ctx, Err, Out> = ParsingDsl<Char, Ctx, Err, Out>

typealias LexingDslErrorProvider<Ctx, Err, Out> = ParsingDsl.ErrorProvider<Char, Ctx, Err, Out>

suspend fun <E, O> Lexer<E, O>.parse(input: CharSequence) =
  with(Any()) { this@parse.parse(input.cursor()) }

fun ignoreCase(str: CharSequence) =
  a@{ other: CharSequence ->
    str.forEachIndexed { index, c ->
      val o = other.getOrNull(index) ?: return@a ParsingDsl.Err(index)
      c.equals(o, ignoreCase = true) || return@a ParsingDsl.Err(index)
    }
    ParsingDsl.Ok(str.length)
  }

val toString = { list: Iterable<Char> -> list.joinToString("") }

val concat = { list: Iterable<CharSequence> -> list.joinToString("") }

val any: ContinuationParser<Char, Any, OutOfBounds<Char>, Char> =
  lexer("any lexer") {
    { _: Char -> true } orFail
      { cur: Cursor<Char> ->
        OutOfBounds(cur.list, cur.index)
      }
  }

inline fun <E, R> lexer(
  name: String,
  crossinline block: suspend LexingDsl<Any, E, R>.() -> R,
) = parser(name) { block() }

val LexingDsl<*, *, *>.string get() = tokens.joinToString("")

@ParsingDslMarker
context(dsl: LexingDsl<C, E, O>)
suspend infix fun <C : Any, E, O> CharSequence.orFail(
  onError: (at: Cursor<Char>) -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<C, E, O>)
suspend infix fun <C : Any, E, O> CharSequence.orFail(onError: () -> E): String =
  dsl.withError({ _ -> onError() }) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<C, E, O>)
suspend infix fun <C : Any, E, O> (
  (CharSequence) -> ParsingDsl.RestMatchResult
).orFail(
  onError: () -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<C, E, O>)
suspend infix fun <C : Any, E, O> (
  (CharSequence) -> ParsingDsl.RestMatchResult
).orFail(
  onError: (at: Cursor<Char>) -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<C, E, O>)
suspend operator fun <C : Any, E, O> CharSequence.unaryPlus() =
  with(errorProvider) {
  (+toList()).joinToString("")
}

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<C, E, O>)
suspend operator fun <C : Any, E, O> ((CharSequence) -> ParsingDsl.RestMatchResult).unaryPlus() =
  with(errorProvider) {
    (
      +{ list: Iterable<Char> ->
        this@unaryPlus(list.joinToString(""))
      }
      ).joinToString("")
  }

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<C, E, O>)
suspend operator fun <C : Any, E, O> ContinuationParser<
  Char,
  Any,
  E,
  CharSequence,
  >.unaryPlus(): String =
  with(errorProvider) {
    (+(this@unaryPlus map { it.toList() })).joinToString("")
  }
