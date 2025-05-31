@file:OptIn(ExperimentalContracts::class)

package net.japanesehunters.util.parse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.coroutines.yield
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper
import net.japanesehunters.util.collection.fold
import net.japanesehunters.util.parse.ParsingDsl.SimpleErrorProvider
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents a parser that processes a sequence of tokens and produces a
 * [Continuation] as output. The [Continuation] contains the result of parsing,
 * along with additional context or error information.
 */
typealias ContinuationParser<Tok, Ctx, Err, Res> =
  Parser<Tok, Ctx, Continuation<Tok, Ctx, Err, Res>>

/**
 * Combines two parsers such that the first parser (`this`) is executed,
 * followed by the second parser (`other`) if the first parser succeeds.
 * Returns a parser that produces a `Pair` containing the results of both
 * parsers if both succeed. If either parser fails, the error from the
 * failed parser is returned.
 *
 * @param other The second parser to execute after the current parser.
 * @return A parser that executes both parsers sequentially, producing a
 * pair of their results if successful or an error if either of them fails.
 */
infix fun <
  T : Any,
  C : Any,
  E : Any,
  O1,
  O2,
> ContinuationParser<T, C, E, O1>.then(
  other: ContinuationParser<T, C, E, O2>,
): ContinuationParser<T, C, E, Pair<O1, O2>> =
  parser("${this@then} then $other") {
    yield()
    val first = this@then.parse()
    yield()
    val second = other.parse()
    yield()
    first to second
  }

/**
 * Shortcut for [then].
 */
operator fun <
  T : Any,
  C : Any,
  E : Any,
  O1,
  O2,
> ContinuationParser<T, C, E, O1>.plus(
  other: ContinuationParser<T, C, E, O2>,
) = this then other

/**
 * Tries to parse the current parser (`this`) and returns its value if
 * successful. If parsing `this` fails, the provided `other` parser is
 * attempted. If both parsers fail, an error is returned as a Pair of the
 * errors from both parsers.
 *
 * Note: The `other` parser will only be executed if `this` fails.
 *
 * @param other Another parser to fall back to if the current parser fails.
 * @return A parser, which executes the first parser, and if it fails, tries
 *         another parser.
 */
infix fun <
  T : Any,
  C : Any,
  E1 : Any,
  E2 : Any,
  O1 : O2,
  O2,
> ContinuationParser<T, C, E1, O1>.or(
  other: ContinuationParser<T, C, E2, O2>,
): ContinuationParser<T, C, Pair<E1, E2>, O2> =
  parser("$this or $other") {
    yield()
    catch {
      this@or.parse()
    }.fold(
      { err1 ->
        yield()
        catch {
          other.parse()
        }.fold(
          { err2 ->
            yield()
            fail(err1 to err2)
          },
          { it },
        )
      },
      { it },
    )
  }

fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> select(
  a: ContinuationParser<T, C, E, R>,
  vararg rest: ContinuationParser<T, C, E, R>,
  cmp: Comparator<R>,
): ContinuationParser<T, C, Any, R> =
  parser("select($a, ${rest.joinToString(", ")})") {
    val res = mutableListOf<R>()
    val err = mutableListOf<Pair<ContinuationParser<T, C, E, R>, E>>()

    catch { a.parse() }
      .fold(
        {
          if (it is CriticalParseError) {
            fail(it)
          }
          err += a to it
        },
        { res += it },
      )
    rest.forEach { parser ->
      catch { parser.parse() }
        .fold(
          {
            if (it is CriticalParseError) {
              fail(it)
            }
            err += parser to it
          },
          { res += it },
        )
    }
    if (res.isEmpty()) {
      fail(NoParserSucceeded(startingCursor, err.toList()))
    }
    res.maxWith(cmp)
  }

/**
 * Repeats the execution of this parser a specified number of times.
 *
 * @param repeat The number of times this parser should be executed.
 * @return A parser that produces a list of results obtained by executing
 *         `this` parser the specified number of times.
 */
infix fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  repeat: Int,
): ContinuationParser<T, C, E, List<O>> =
  parser("'$this' repeat $repeat times") {
    (0..<repeat)
      .map {
        yield()
        this@repeat.parse()
      }
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.times(
  repeat: Int,
): ContinuationParser<T, C, E, List<O>> = this repeat repeat

/**
 * Repeats the execution of this parser a specified number of times defined
 * by the given range. The first iteration (determined by `repeat.first`) must
 * succeed, while further iterations may fail. Parsing will stop after
 * `repeat.last` iterations or earlier if parsing fails.
 *
 * @param repeat An integer range specifying the minimum (`repeat.first`) and
 *               maximum (`repeat.last`) number of times this parser will be
 *               executed. The parser must succeed at least `repeat.first`
 *               times. Any additional executions up to `repeat.last` are
 *               optional.
 * @return A parser that produces a list of results obtained by executing
 *         `this` parser within the defined range.
 */
infix fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  repeat: IntRange,
): ContinuationParser<T, C, E, List<O>> =
  parser(
    "'$this' repeat at least ${repeat.first} times " +
      "and up to ${repeat.last} times",
  ) {
    val required = this@repeat.repeat(repeat.first).parse()
    val optional = mutableListOf<O>()
    @Suppress("unused")
    for (i in repeat.first..<repeat.last) {
      yield()
      optional += option { this@repeat.parse() } ?: break
    }
    required + optional
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.times(
  repeat: IntRange,
): ContinuationParser<T, C, E, List<O>> = this repeat repeat

/**
 * Repeats the given parser until it fails, collecting all successful results
 * into a list. This function guarantees to return a successful result even
 * if the parser never succeeds, in which case the result is an empty list.
 *
 * @return A parser that produces a list of the results from repeated
 *         successful executions of the parser.
 */
infix fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  @Suppress("unused") option: RepeatOption.MANY,
): ContinuationParser<T, C, E, List<O>> =
  parser("'$this' repeat many times") {
    val ret = mutableListOf<O>()
    while (true) {
      yield()
      ret +=
        catch { this@repeat.parse() }
          .getOrElse { err ->
            if (err is CriticalParseError) {
              fail(err)
            } else {
              null
            }
          } ?: break
    }
    ret.toList()
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.times(
  @Suppress("unused") option: RepeatOption.MANY,
): ContinuationParser<T, C, E, List<O>> = this repeat option

/**
 * Repeats the given parser until it fails, ensuring that it succeeds at
 * least once. On the first successful parse, it continues parsing repeatedly,
 * collecting all successful results into a `NonEmptyList`.
 * If the initial parse fails, the error from the first failure is returned.
 *
 * @return A parser that produces a `NonEmptyList` of results from one or
 *         more successful executions of the parser.
 */
infix fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  @Suppress("unused") option: RepeatOption.SOME,
): ContinuationParser<T, C, E, NonEmptyList<O>> =
  parser("'$this' repeat many times but at least one must succeed") {
    yield()
    val first = this@repeat.parse()
    val rest = this@repeat.repeat(many).parse()
    NonEmptyList(first, rest)
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E : Any,
  O,
> ContinuationParser<T, C, E, O>.times(
  @Suppress("unused") option: RepeatOption.SOME,
): ContinuationParser<T, C, E, NonEmptyList<O>> = this repeat option

val many = RepeatOption.MANY

val some = RepeatOption.SOME

sealed interface RepeatOption {
  data object MANY : RepeatOption

  data object SOME : RepeatOption
}

operator fun <
  T : Any,
  C : Any,
  E : Any,
  O : Any,
> ContinuationParser<T, C, E, O>.not(): ContinuationParser<T, C, O, E> =
  object : ContinuationParser<T, C, O, E> {
    context(ctx: C)
    override suspend fun parse(input: Cursor<T>): Continuation<T, C, O, E> {
      yield()
      return when (val cont = this@not.parse(input)) {
        is Ok -> Err(cont.result)
        is Err ->
          input.fold(
            { Done(cont.error, it) },
            { Cont(cont.error, it, ctx) },
          )
      }
    }

    override fun toString() = "not ${this@not}"
  }

/**
 * Transforms the parsing result using the given transformation function.
 *
 * @param f A function that transforms the result into type `R2`.
 * @return A parser with the transformed result.
 */
inline infix fun <
  T : Any,
  C : Any,
  E : Any,
  R1,
  R2,
> ContinuationParser<T, C, E, R1>.map(
  crossinline f: (R1) -> R2,
): ContinuationParser<T, C, E, R2> =
  parser("$this") {
    yield()
    this@map.parse().let(f)
  }

/**
 * Transforms the error type using the given transformation function.
 *
 * @param f A function that transforms the error into type `E2`.
 * @return A parser with the transformed error.
 */
inline infix fun <
  T : Any,
  C : Any,
  E1 : Any,
  E2 : Any,
  R,
> ContinuationParser<T, C, E1, R>.mapErr(
  crossinline f: (E1) -> E2,
): ContinuationParser<T, C, E2, R> =
  parser("$this") {
    catch {
      yield()
      this@mapErr.parse()
    }.fold(
      ifLeft = { err -> fail(f(err)) },
      ifRight = { it },
    )
  }

/**
 * Executes the given function if the parsing completes successfully.
 *
 * @param f A function to be executed if the parser succeeds.
 * @return A parser that executes the given function if it succeeds.
 */
inline infix fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> ContinuationParser<T, C, E, R>.onOk(
  crossinline f: (R) -> Unit,
): ContinuationParser<T, C, E, R> =
  map {
    f(it)
    it
  }

/**
 * Executes the given function when the parser encounters an error.
 *
 * @param f A function to be executed when an error occurs.
 * @return A parser that executes the given function if it fails.
 */
inline infix fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> ContinuationParser<T, C, E, R>.onErr(
  crossinline f: (E) -> Unit,
): ContinuationParser<T, C, E, R> =
  mapErr { e ->
    f(e)
    e
  }

/**
 * Converts a `ContinuationParser` into a simple `Parser`.
 * The provided `onError` handler is invoked if the original parser fails.
 *
 * @param onError A function to handle parsing errors. It is called whenever
 *                the original parser encounters an error. This function
 *                is expected to throw or terminate the processing.
 * @return A parser that executes original parser but uses `onError`
 *         for error handling.
 */
inline fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> ContinuationParser<T, C, E, R>.complete(
  crossinline onError: (E) -> Nothing,
): Parser<T, C, R> =
  object : Parser<T, C, R> {
    context(ctx: C)
    override suspend fun parse(input: Cursor<T>): R =
      this@complete
        .parse(input)
        .fold(
          onOk = { it },
          onErr = { onError(it) },
        )

    override fun toString() = this@complete.toString()
  }

data class NoParserSucceeded<Tok : Any, Ctx : Any, Err : Any, R>(
  val input: Cursor<Tok>,
  val errors: Collection<Pair<ContinuationParser<Tok, Ctx, Err, R>, Err>>,
)

interface CriticalParseError

inline fun <Tok : Any, Ctx : Any, Err : Any, R> parser(
  name: String,
  crossinline block:
    suspend ParsingDsl<Tok, Ctx, Err>.(
      ctxTypeInfer: Ctx,
    ) -> R,
) = object : ContinuationParser<Tok, Ctx, Err, R> {
  context(ctx: Ctx)
  override suspend fun parse(
    input: Cursor<Tok>,
  ): Continuation<Tok, Ctx, Err, R> {
    val scope = ParsingDsl<Tok, Ctx, Err>(input, ctx, null)
    val out =
      try {
        scope.block(ctx)
      } catch (e: ParsingDsl.ParsingDslRaise) {
        @Suppress("UNCHECKED_CAST")
        return Err(e.err as Err)
      }
    return scope.cursor.fold(
      onOutOfBounds = { cursor -> Done(out, cursor) },
      onZipper = { rem -> Cont(out, rem, scope.ctx) },
    )
  }

  override fun toString() = name
}

@DslMarker
annotation class ParsingDslMarker

// TODO: Document
open class ParsingDsl<
  Tok : Any,
  Ctx : Any,
  Err : Any,
>(
  input: Cursor<Tok>,
  context: Ctx,
  private val parent: ParsingDsl<Tok, Ctx, Err>?,
) {
  @ParsingDslMarker
  val tokens: List<Tok> get() = tokensInternal.toList()
  internal val tokensInternal: MutableList<Tok> = mutableListOf()
    get() = parent?.tokensInternal ?: field

  @ParsingDslMarker
  val startingCursor: Cursor<Tok> = input

  @ParsingDslMarker
  var cursor: Cursor<Tok> = input
    get() {
      return parent?.cursor ?: field
    }
    internal set(value) {
      parent?.cursor = value
      field = value
    }

  @ParsingDslMarker
  var ctx = context
    get() {
      return parent?.ctx ?: field
    }
    set(value) {
      parent?.ctx = value
      field = value
    }

  @ParsingDslMarker
  fun fail(error: Err): Nothing = throw ParsingDslRaise(error)

  data class ParsingDslRaise(
    val err: Any,
  ) : Exception()

  @ParsingDslMarker
  suspend infix fun Cursor<Tok>.zipperOrFail(onError: () -> Err) =
    withError(onError) { this@zipperOrFail.zipperOrFail() }

  suspend infix fun Cursor<Tok>.zipperOrFail(onError: (Cursor<Tok>) -> Err) =
    withError(onError) { this@zipperOrFail.zipperOrFail() }

  @ParsingDslMarker
  suspend infix fun Tok.orFail(onError: () -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Tok.orFail(onError: (Cursor<Tok>) -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Iterable<Tok>.orFail(onError: () -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Iterable<Tok>.orFail(onError: (Cursor<Tok>) -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun ((Tok) -> Boolean).orFail(onError: () -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun ((Tok) -> Boolean).orFail(onError: (Cursor<Tok>) -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun (
  (rest: Iterable<Tok>) -> RestMatchResult
  ).orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun (
  (Iterable<Tok>) -> RestMatchResult
  ).orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  sealed interface RestMatchResult {
    val proceed: Int
  }

  value class Ok(
    override val proceed: Int,
  ) : RestMatchResult

  value class Err(
    override val proceed: Int,
  ) : RestMatchResult

  @ParsingDslMarker
  suspend infix fun Parser<Tok, Ctx, Tok>.orFail(onError: () -> Err) =
    withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Parser<Tok, Ctx, Tok>.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Parser<Tok, Ctx, Iterable<Tok>>.orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun Parser<Tok, Ctx, Iterable<Tok>>.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<
    Tok,
    Ctx,
    E,
    Tok,
  >.orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<Tok, Ctx, E, Tok>.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<
    Tok,
    Ctx,
    E,
    Iterable<Tok>,
  >.orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E : Any> ContinuationParser<
    Tok,
    Ctx,
    E,
    Iterable<Tok>,
  >.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend operator fun ContinuationParser<
    Tok,
    Ctx,
    Err,
    Tok,
  >.unaryPlus(): Tok {
    val res = this@unaryPlus.parse()
    tokensInternal += res
    return res
  }

  @ParsingDslMarker
  suspend operator fun ContinuationParser<
    Tok,
    Ctx,
    Err,
    Iterable<Tok>,
  >.unaryPlus(): List<Tok> {
    val res = this@unaryPlus.parse()
    tokensInternal += res
    return res.toList()
  }

  @ParsingDslMarker
  suspend fun <O> ContinuationParser<
    Tok,
    Ctx,
    Err,
    O,
  >.parse(): O {
    val cont =
      with(ctx) {
        parse(cursor)
      }
    return cont.fold(
      onDone = { res, cur ->
        cursor = cur
        res
      },
      onCont = { res, zip, newCtx ->
        cursor = zip
        ctx = newCtx
        res
      },
      onErr = { fail(it) },
    )
  }

  open class SimpleErrorProvider<Tok : Any, Ctx : Any, Err : Any>(
    private val originalScope: ParsingDsl<Tok, Ctx, Err>,
    private val onError: (Cursor<Tok>) -> Err,
  ) : ParsingDsl<
      Tok,
      Ctx,
      Err,
    >(
      originalScope.cursor,
      originalScope.ctx,
      originalScope,
    ) {
    @ParsingDslMarker
    fun Cursor<Tok>.zipperOrFail(): Zipper<Tok> =
      fold(
        { fail(onError(it)) },
        { it },
      )

    @ParsingDslMarker
    operator fun Tok.unaryPlus(): Tok = +{ c: Tok -> c == this@unaryPlus }

    @ParsingDslMarker
    operator fun Iterable<Tok>.unaryPlus(): List<Tok> = map { +it }

    @ParsingDslMarker
    operator fun ((Tok) -> Boolean).unaryPlus(): Tok =
      with(originalScope) {
        cursor.fold(
          { fail(onError(it)) },
          { zipper ->
            val peek = zipper.peek
            if (this@unaryPlus(peek)) {
              cursor = zipper.moveRight()
              tokensInternal += peek
              peek
            } else {
              fail(onError(zipper))
            }
          },
        )
      }

    @ParsingDslMarker
    operator fun (
    (rest: Iterable<Tok>) -> RestMatchResult
    ).unaryPlus(): List<Tok> =
      with(originalScope) {
        cursor.fold(
          { fail(onError(it)) },
          { zipper ->
            val rest = zipper.toRestList()
            @Suppress("RemoveRedundantQualifierName")
            when (val result = this@unaryPlus(rest)) {
              is Ok -> {
                cursor = zipper.moveRight(result.proceed)
                val ret = rest.take(result.proceed)
                tokensInternal += ret
                ret
              }
              is ParsingDsl.Err ->
                fail(onError(zipper.moveRight(result.proceed)))
            }
          },
        )
      }

    @ParsingDslMarker
    suspend operator fun Parser<Tok, Ctx, Tok>.unaryPlus(): Tok =
      with(originalScope) {
        with(ctx) {
          +parse(cursor)
        }
      }

    @ParsingDslMarker
    suspend operator fun Parser<
      Tok,
      Ctx,
      Iterable<Tok>,
    >.unaryPlus(): List<Tok> =
      with(originalScope) {
        with(ctx) {
          (+parse(cursor)).toList()
        }
      }

    @ParsingDslMarker
    suspend operator fun <E : Any> ContinuationParser<
      Tok,
      Ctx,
      E,
      Tok,
    >.unaryPlus(): Tok =
      with(originalScope) {
        with(ctx) {
          parse(cursor)
        }.fold(
          { res, cur ->
            cursor = cur
            tokensInternal += res
            res
          },
          { res, zip, newCtx ->
            cursor = zip
            ctx = newCtx
            tokensInternal += res
            res
          },
          { fail(onError(cursor)) },
        )
      }

    @ParsingDslMarker
    suspend operator fun <E : Any> ContinuationParser<
      Tok,
      Ctx,
      E,
      Iterable<Tok>,
    >.unaryPlus(): List<Tok> =
      with(originalScope) {
        with(ctx) {
          parse(cursor)
        }.fold(
          { res, cur ->
            cursor = cur
            tokensInternal += res
            res.toList()
          },
          { res, zip, newCtx ->
            cursor = zip
            ctx = newCtx
            tokensInternal += res
            res.toList()
          },
          { fail(onError(cursor)) },
        )
      }

    override fun toString() = "${super.toString()}(of $originalScope)()"
  }
}

suspend fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> ParsingDsl<T, C, E>.withError(
  onError: () -> E,
  block: suspend SimpleErrorProvider<T, C, E>.() -> R,
): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return withError({ _ -> onError() }, block)
}

suspend fun <
  T : Any,
  C : Any,
  E : Any,
  R,
> ParsingDsl<T, C, E>.withError(
  onError: (Cursor<T>) -> E,
  block: suspend SimpleErrorProvider<T, C, E>.() -> R,
): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return SimpleErrorProvider(this, onError).block()
}

@Suppress("LEAKED_IN_PLACE_LAMBDA")
@ParsingDslMarker
suspend fun <T : Any, C : Any, E : Any, R> ParsingDsl<T, C, *>.catch(
  block: suspend ParsingDsl<T, C, E>.() -> R,
): Either<E, R> =
  either {
    val catch =
      parser("catch") a@{
        val ret = this@a.block() to tokens
        ret
      }
    with(catch) {
      with(ctx) {
        parse(cursor)
      }
    }.fold(
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

@ParsingDslMarker
suspend fun <T : Any, C : Any, R> ParsingDsl<T, C, *>.option(
  block: suspend SimpleErrorProvider<T, C, Any>.() -> R,
): R? {
  class DummyError
  return catch {
    val ret =
      withError({ _ -> DummyError() }) {
        block()
      }
    ret
  }.getOrNull()
}
