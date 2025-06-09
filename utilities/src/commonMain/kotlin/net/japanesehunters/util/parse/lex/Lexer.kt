package net.japanesehunters.util.parse.lex

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.OutOfBounds
import net.japanesehunters.util.collection.cursor
import net.japanesehunters.util.parse.ContinuationParser
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.ParsingDslMarker
import net.japanesehunters.util.parse.map
import net.japanesehunters.util.parse.mapErr
import net.japanesehunters.util.parse.or
import net.japanesehunters.util.parse.parser

typealias Lexer<Err, Out> = ContinuationParser<Char, Any, Err, Out>

typealias LexingDsl<Err, Out> = ParsingDsl<Char, Any, Err, Out>

typealias LexingDslErrorProvider<Err, Out> = ParsingDsl.ErrorProvider<Char, Any, Err, Out>

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

data class NotDigit(
  val at: Cursor<Char>,
)

val any: ContinuationParser<Char, Any, OutOfBounds<Char>, Char> =
  lexer("any lexer") {
    { _: Char -> true } orFail
      { cur: Cursor<Char> ->
        OutOfBounds(cur.list, cur.index)
      }
  }

val digit =
  lexer("digit lexer") {
    Char::isDigit orFail ::NotDigit
  }

data class NotLineSeparator(
  val at: Cursor<Char>,
)

data class NotCrLf(
  val at: Cursor<Char>,
)

data class NotLf(
  val at: Cursor<Char>,
)

val lineSeparator =
  lexer("CrLf lexer") {
    "\r\n" orFail ::NotCrLf
  } or
    lexer("Lf lexer") {
      "\n" orFail ::NotLf
    } mapErr { (_, lf) ->
      NotLineSeparator(lf.at)
    }

inline fun <E, R> lexer(
  name: String,
  crossinline block: suspend LexingDsl<E, R>.() -> R,
) = parser(name) { block() }

val LexingDsl<*, *>.string get() = tokens.joinToString("")

@ParsingDslMarker
context(dsl: LexingDsl<E, O>)
suspend infix fun <E, O> CharSequence.orFail(
  onError: (at: Cursor<Char>) -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<E, O>)
suspend infix fun <E, O> CharSequence.orFail(onError: () -> E): String =
  dsl.withError({ _ -> onError() }) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<E, O>)
suspend infix fun <E, O> (
  (CharSequence) -> ParsingDsl.RestMatchResult
).orFail(
  onError: () -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(dsl: LexingDsl<E, O>)
suspend infix fun <E, O> (
  (CharSequence) -> ParsingDsl.RestMatchResult
).orFail(
  onError: (at: Cursor<Char>) -> E,
): String = dsl.withError(onError) { +this@orFail }

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<E, O>)
suspend operator fun <E, O> CharSequence.unaryPlus() = with(errorProvider) {
  (+toList()).joinToString("")
}

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<E, O>)
suspend operator fun <E, O> ((CharSequence) -> ParsingDsl.RestMatchResult).unaryPlus() =
  with(errorProvider) {
    (
      +{ list: Iterable<Char> ->
        this@unaryPlus(list.joinToString(""))
      }
      ).joinToString("")
  }

@ParsingDslMarker
context(errorProvider: LexingDslErrorProvider<E, O>)
suspend operator fun <E, O> ContinuationParser<
  Char,
  Any,
  E,
  CharSequence,
  >.unaryPlus(): String =
  with(errorProvider) {
    (+(this@unaryPlus map { it.toList() })).joinToString("")
  }
