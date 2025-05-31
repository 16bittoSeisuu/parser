package net.japanesehunters.util.parse.lex

import arrow.core.Either
import arrow.core.raise.either
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.OutOfBounds
import net.japanesehunters.util.collection.cursor
import net.japanesehunters.util.collection.fold
import net.japanesehunters.util.parse.Cont
import net.japanesehunters.util.parse.Continuation
import net.japanesehunters.util.parse.ContinuationParser
import net.japanesehunters.util.parse.Done
import net.japanesehunters.util.parse.Err
import net.japanesehunters.util.parse.Parser
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.ParsingDslMarker
import net.japanesehunters.util.parse.fold
import net.japanesehunters.util.parse.map
import net.japanesehunters.util.parse.mapErr
import net.japanesehunters.util.parse.or

typealias Lexer<Out> = Parser<Char, Any, Out>

suspend fun <O> Lexer<O>.parse(input: CharSequence) =
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

value class NotDigit(
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
    // Kotlin type inference moment
    lexer<NotLf, String>("Lf lexer") {
      "\n" orFail ::NotLf
    } mapErr { (_, lf) ->
      NotLineSeparator(lf.at)
    }

inline fun <E : Any, R> lexer(
  name: String,
  crossinline block: suspend LexingDsl<E>.() -> R,
): ContinuationParser<Char, Any, E, R> =
  object : ContinuationParser<Char, Any, E, R> {
    context(ctx: Any)
    override suspend fun parse(
      input: Cursor<Char>,
    ): Continuation<Char, Any, E, R> {
      val scope = LexingDsl<E>(input)
      val out =
        try {
          scope.block()
        } catch (e: ParsingDsl.ParsingDslRaise) {
          @Suppress("UNCHECKED_CAST")
          return Err(e.err as E)
        }
      return scope.cursor.fold(
        { cur -> Done(out, cur) },
        { rem -> Cont(out, rem, scope.ctx) },
      )
    }

    override fun toString() = name
  }

class LexingDsl<Err : Any>(
  input: Cursor<Char>,
) : ParsingDsl<Char, Any, Err>(
    input,
    Any(),
    null,
  ) {
  @ParsingDslMarker
  val string: String get() = tokens.joinToString("") { it.toString() }

  @ParsingDslMarker
  suspend infix fun CharSequence.orFail(onError: () -> Err): String =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun CharSequence.orFail(
    onError: (at: Cursor<Char>) -> Err,
  ): String = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun ((CharSequence) -> RestMatchResult).orFail(
    onError: () -> Err,
  ): String = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun ((CharSequence) -> RestMatchResult).orFail(
    onError: (at: Cursor<Char>) -> Err,
  ): String = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<
    Char,
    Any,
    E,
    CharSequence,
  >.orFail(
    onError: () -> Err,
  ): String = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<
    Char,
    Any,
    E,
    CharSequence,
  >.orFail(
    onError: (at: Cursor<Char>) -> Err,
  ): String = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend operator fun ContinuationParser<
    Char,
    Any,
    Err,
    CharSequence,
    >.unaryPlus(): String {
    val res = this@unaryPlus.parse()
    tokensInternal += res.toList()
    return res.toString()
  }

  @ParsingDslMarker
  suspend operator fun ContinuationParser<
    Char,
    Any,
    Err,
    Iterable<CharSequence>,
  >.unaryPlus(): List<String> {
    val res = this@unaryPlus.parse()
    tokensInternal += res.flatMap { it.toList() }
    return res.map { it.toString() }
  }

  // TODO: fix repetition
  @Suppress("LEAKED_IN_PLACE_LAMBDA")
  @ParsingDslMarker
  suspend fun <E : Any, R> catch(
    block: suspend LexingDsl<E>.() -> R,
  ): Either<E, R> =
    either {
      val catch: ContinuationParser<Char, Any, E, Pair<R, List<Char>>> =
        lexer("catch") a@{
          val ret = this@a.block() to tokens
          ret
        }
      catch
        .parse(cursor)
        .fold(
          { (res, tokens), cur ->
            cursor = cur
            tokensInternal += tokens
            res
          },
          { (res, tokens), zip, newCtx ->
            cursor = zip
            tokensInternal += tokens
            ctx = newCtx
            res
          },
          { err ->
            raise(err)
          },
        )
    }

  // TODO: fix repetition
  @ParsingDslMarker
  suspend fun <R> option(block: suspend LexerErrorProvider<Any>.() -> R): R? {
    class DummyError
    return catch {
      val ret =
        withError({ _ -> DummyError() }) {
          block()
        }
      ret
    }.getOrNull()
  }

  @ParsingDslMarker
  suspend fun <R> withError(
    onError: () -> Err,
    block: suspend LexerErrorProvider<Err>.() -> R,
  ) = withError({ _ -> onError() }, block)

  @ParsingDslMarker
  suspend fun <R> withError(
    onError: (Cursor<Char>) -> Err,
    block: suspend LexerErrorProvider<Err>.() -> R,
  ) = LexerErrorProvider(this, onError).block()

  class LexerErrorProvider<Err : Any>(
    dsl: LexingDsl<Err>,
    onError: (Cursor<Char>) -> Err,
  ) : SimpleErrorProvider<Char, Any, Err>(dsl, onError) {
    @ParsingDslMarker
    operator fun CharSequence.unaryPlus() = (+toList()).joinToString("")

    @ParsingDslMarker
    operator fun ((CharSequence) -> RestMatchResult).unaryPlus() =
      (
        +{ list: Iterable<Char> ->
          this(list.joinToString(""))
        }
      ).joinToString("")

    @ParsingDslMarker
    suspend operator fun <E : Any> ContinuationParser<
      Char,
      Any,
      E,
      CharSequence,
      >.unaryPlus(): String =
      (+(this@unaryPlus.map { it.toList() })).joinToString("")
  }
}
