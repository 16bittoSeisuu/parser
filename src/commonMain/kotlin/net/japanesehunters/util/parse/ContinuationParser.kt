@file:OptIn(ExperimentalContracts::class)

package net.japanesehunters.util.parse

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.fold
import kotlin.contracts.ExperimentalContracts

/**
 * Represents a parser that processes a sequence of tokens and produces a
 * [Continuation] as output. The [Continuation] contains the result of parsing,
 * along with additional context or error information.
 */
typealias ContinuationParser<Tok, Ctx, Err, Res> =
  Parser<Tok, Ctx, Continuation<Tok, Err, Res>>

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
  E,
  O1,
  O2,
> ContinuationParser<T, C, E, O1>.then(
  other: ContinuationParser<T, C, E, O2>,
): ContinuationParser<T, C, E, Pair<O1, O2>> =
  parser("${this@then} then $other") {
    yield()
    val first = +this@then
    yield()
    val second = +other
    yield()
    first to second
  }

/**
 * Shortcut for [then].
 */
operator fun <
  T : Any,
  C : Any,
  E,
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
  E1,
  E2,
  O1 : O2,
  O2,
> ContinuationParser<T, C, E1, O1>.or(
  other: ContinuationParser<T, C, E2, O2>,
): ContinuationParser<T, C, Pair<E1, E2>, O2> =
  parser("$this or $other") {
    yield()
    catch {
      +this@or
    }.fold(
      { err1 ->
        yield()
        catch {
          +other
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

/**
 * Executes multiple parsers asynchronously on the same input and
 * selects the most appropriate parse result based on the given comparator.
 *
 * The method handles results as follows:
 * - If a critical error occurs during any parsing, it is immediately returned.
 * - If no critical errors occur and at least one parser succeeds,
 *   the most optimal result (determined by using the comparator) is returned
 *   as a [Ok].
 * - If no critical errors occur and none of the parsers succeed, a list of
 *   failed parsers paired with their respective errors is returned; type:
 *   `List<ContinuationParser<T, C, E, R>, E>`
 *
 * @param a The primary parser to execute.
 * @param rest Additional parsers to execute simultaneously with `a`.
 * @param cmp A comparator to determine the most optimal parse result
 *            from successful parsers.
 * @return A parser that provides the result of the most optimal parse
 *         or an error if parsing fails.
 */
fun <
  T : Any,
  C : Any,
  E,
  R,
> select(
  a: ContinuationParser<T, C, E, R>,
  vararg rest: ContinuationParser<T, C, E, R>,
  cmp: Comparator<R>,
): ContinuationParser<T, C, Any?, R> =
  object : ContinuationParser<T, C, Any?, R> {
    context(ctx: C)
    override suspend fun parse(input: Cursor<T>): Continuation<T, Any?, R> =
      try {
        coroutineScope {
          val res = mutableListOf<Ok<T, R>>()
          val err = mutableListOf<Pair<ContinuationParser<T, C, E, R>, E>>()

          (listOf(a) + rest.toList())
            .map { parser ->
              yield()
              launch {
                parser
                  .parse(input)
                  .fold(
                    { res += it },
                    { (error) ->
                      when (error) {
                        is CriticalParseError ->
                          throw CriticalParseErrorException(error)

                        else ->
                          err += parser to error
                      }
                    },
                  )
              }
            }.joinAll()
          return@coroutineScope res
            .maxWithOrNull { a, b -> cmp.compare(a.result, b.result) }
            ?: Err(err.toList())
        }
      } catch (e: CriticalParseErrorException) {
        @Suppress("UNCHECKED_CAST")
        Err(e.err as E)
      }

    override fun toString() = "select($a, ${rest.joinToString(", ")})"
  }

private data class CriticalParseErrorException(
  val err: Any,
) : Exception()

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
  E,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  repeat: Int,
): ContinuationParser<T, C, E, List<O>> =
  parser("'$this' repeat $repeat times") {
    (0..<repeat)
      .map {
        yield()
        +this@repeat
      }
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E,
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
  E,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  repeat: IntRange,
): ContinuationParser<T, C, E, List<O>> =
  parser(
    "'$this' repeat at least ${repeat.first} times " +
      "and up to ${repeat.last} times",
  ) {
    val required = +this@repeat.repeat(repeat.first)
    val optional = mutableListOf<O>()
    @Suppress("unused")
    for (i in repeat.first..<repeat.last) {
      yield()
      optional += option { +this@repeat } ?: break
    }
    required + optional
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E,
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
  E,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  @Suppress("unused") option: RepeatOption.MANY,
): ContinuationParser<T, C, E, List<O>> =
  parser("'$this' repeat many times") {
    val ret = mutableListOf<O>()
    while (true) {
      yield()
      ret +=
        catch { +this@repeat }
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
  E,
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
  E,
  O,
> ContinuationParser<T, C, E, O>.repeat(
  @Suppress("unused") option: RepeatOption.SOME,
): ContinuationParser<T, C, E, NonEmptyList<O>> =
  parser("'$this' repeat many times but at least one must succeed") {
    yield()
    val first = +this@repeat
    val rest = +this@repeat.repeat(many)
    NonEmptyList(first, rest)
  }

/**
 * Shortcut for [ContinuationParser.repeat].
 */
operator fun <
  T : Any,
  C : Any,
  E,
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
  E,
  O : Any,
> ContinuationParser<T, C, E, O>.not(): ContinuationParser<T, C, O, E> =
  object : ContinuationParser<T, C, O, E> {
    context(ctx: C)
    override suspend fun parse(input: Cursor<T>): Continuation<T, O, E> {
      yield()
      return when (val cont = this@not.parse(input)) {
        is Ok -> Err(cont.result)
        is Err ->
          input.fold(
            { Done(cont.error, it) },
            { Cont(cont.error, it) },
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
  E,
  R1,
  R2,
> ContinuationParser<T, C, E, R1>.map(
  crossinline f: (R1) -> R2,
): ContinuationParser<T, C, E, R2> =
  parser("$this") {
    yield()
    (+this@map).let(f)
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
  E1,
  E2,
  R,
> ContinuationParser<T, C, E1, R>.mapErr(
  crossinline f: (E1) -> E2,
): ContinuationParser<T, C, E2, R> =
  parser("$this") {
    catch {
      yield()
      +this@mapErr
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
  E,
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
  E,
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
  E,
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
          onOk = { (res, _) -> res },
          onErr = { (err) -> onError(err) },
        )

    override fun toString() = this@complete.toString()
  }

// TODO: add continuation 'critical error' then remove this
interface CriticalParseError
