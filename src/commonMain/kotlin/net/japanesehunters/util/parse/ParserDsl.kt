package net.japanesehunters.util.parse

import arrow.core.Either
import arrow.core.raise.either
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper
import net.japanesehunters.util.collection.fold
import net.japanesehunters.util.parse.ParsingDsl.RestMatchResult

fun <Tok : Any, Ctx : Any, Err, R> parser(
  name: String,
  block:
    suspend ParsingDsl<Tok, Ctx, Err>.(
      ctxTypeInfer: Ctx,
    ) -> R,
) = object : ContinuationParser<Tok, Ctx, Err, R> {
  context(ctx: Ctx)
  override suspend fun parse(
    input: Cursor<Tok>,
  ): Continuation<Tok, Ctx, Err, R> {
    val scope = ParsingDslImpl<Tok, Ctx, Err>(input, ctx, null)
    val out =
      try {
        scope.block(ctx)
      } catch (e: ParsingDslImpl.Raise) {
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

interface ParsingDsl<Tok : Any, Ctx : Any, Err> {
  @ParsingDslMarker
  val tokens: List<Tok>
    get() =
      startingCursor.toRestList().take(cursor.index - startingCursor.index)

  val startingCursor: Cursor<Tok>

  val cursor: Cursor<Tok>

  var ctx: Ctx

  interface ErrorProvider<Tok : Any, Ctx : Any, Err> :
    ParsingDsl<Tok, Ctx, Err> {
    @ParsingDslMarker
    fun Cursor<Tok>.zipperOrFail(): Zipper<Tok>

    @ParsingDslMarker
    operator fun Tok.unaryPlus(): Tok = +{ c: Tok -> c == this@unaryPlus }

    @ParsingDslMarker
    operator fun Iterable<Tok>.unaryPlus(): List<Tok> = map { +it }

    @ParsingDslMarker
    operator fun ((Tok) -> Boolean).unaryPlus(): Tok

    @ParsingDslMarker
    operator fun (
    (rest: Iterable<Tok>) -> RestMatchResult
    ).unaryPlus(): List<Tok>

    @ParsingDslMarker
    suspend operator fun Parser<Tok, Ctx, Tok>.unaryPlus(): Tok

    @ParsingDslMarker
    suspend operator fun Parser<
      Tok,
      Ctx,
      Iterable<Tok>,
    >.unaryPlus(): List<Tok>

    @ParsingDslMarker
    suspend operator fun <E, R> ContinuationParser<
      Tok,
      Ctx,
      E,
      R,
    >.unaryPlus(): R

    @ParsingDslMarker
    suspend operator fun <E, R> ContinuationParser<
      Tok,
      Ctx,
      E,
      Iterable<R>,
    >.unaryPlus(): List<R>
  }

  fun fail(error: Err): Nothing

  suspend fun <R> withError(
    onError: () -> Err,
    block: suspend ErrorProvider<Tok, Ctx, Err>.() -> R,
  ): R = withError({ _ -> onError() }, block)

  suspend fun <R> withError(
    onError: (Cursor<Tok>) -> Err,
    block: suspend ErrorProvider<Tok, Ctx, Err>.() -> R,
  ): R

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
  (rest: Iterable<Tok>) -> RestMatchResult
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
  suspend infix fun <E, R> ContinuationParser<Tok, Ctx, E, R>.orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E, R> ContinuationParser<Tok, Ctx, E, R>.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    Iterable<R>,
  >.orFail(
    onError: () -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend infix fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    Iterable<R>,
  >.orFail(
    onError: (Cursor<Tok>) -> Err,
  ) = withError(onError) { +this@orFail }

  @ParsingDslMarker
  suspend operator fun <R> ContinuationParser<
    Tok,
    Ctx,
    Err,
    R,
  >.unaryPlus(): R =
    withError({ _ ->
      throw IllegalStateException("unreachable")
    }) { +this@unaryPlus }

  @ParsingDslMarker
  suspend operator fun <R> ContinuationParser<
    Tok,
    Ctx,
    Err,
    Iterable<R>,
  >.unaryPlus(): List<R> =
    withError({ _ ->
      throw IllegalArgumentException("unreachable")
    }) { +this@unaryPlus }

  @Suppress("LEAKED_IN_PLACE_LAMBDA")
  @ParsingDslMarker
  suspend fun <E, R> catch(
    block: suspend ParsingDsl<Tok, Ctx, E>.() -> R,
  ): Either<E, R>

  @ParsingDslMarker
  suspend fun <R> option(
    block: suspend ErrorProvider<Tok, Ctx, Any>.() -> R,
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
}

private class ParsingDslImpl<Tok : Any, Ctx : Any, Err>(
  input: Cursor<Tok>,
  override var ctx: Ctx,
  private val parent: ParsingDslImpl<Tok, Ctx, Err>? = null,
) : ParsingDsl<Tok, Ctx, Err> {
  override val startingCursor = input
  override var cursor = input
    get() = parent?.cursor ?: field
    set(value) = if (parent == null) field = value else parent.cursor = value

  override fun fail(error: Err) = throw Raise(error)

  data class Raise(
    val err: Any?,
  ) : Exception()

  override suspend fun <R> withError(
    onError: (Cursor<Tok>) -> Err,
    block: suspend ParsingDsl.ErrorProvider<Tok, Ctx, Err>.() -> R,
  ): R = ParsingDslErrorProviderImpl(this, onError).block()

  override suspend fun <E, R> catch(
    block: suspend ParsingDsl<Tok, Ctx, E>.() -> R,
  ): Either<E, R> =
    either {
      val catch =
        parser("catch") {
          block()
        }
      with(ctx) {
        catch.parse(cursor)
      }.fold(
        { res, cur ->
          cursor = cur
          res
        },
        { res, zip, newCtx ->
          cursor = zip
          ctx = newCtx
          res
        },
        {
          raise(it)
        },
      )
    }
}

private class ParsingDslErrorProviderImpl<Tok : Any, Ctx : Any, Err>(
  private val parent: ParsingDslImpl<Tok, Ctx, Err>,
  private val onError: (at: Cursor<Tok>) -> Err,
) : ParsingDsl.ErrorProvider<Tok, Ctx, Err>,
  ParsingDsl<Tok, Ctx, Err> by parent {
  override fun Cursor<Tok>.zipperOrFail(): Zipper<Tok> =
    fold(
      { fail(onError(it)) },
      { it },
    )

  override fun ((Tok) -> Boolean).unaryPlus(): Tok =
    with(parent) {
      cursor.fold(
        { fail(onError(it)) },
        { zipper ->
          val peek = zipper.peek
          if (this@unaryPlus(peek)) {
            cursor = zipper.moveRight()
            peek
          } else {
            fail(onError(zipper))
          }
        },
      )
    }

  override operator fun (
  (rest: Iterable<Tok>) -> RestMatchResult
  ).unaryPlus(): List<Tok> =
    with(parent) {
      cursor.fold(
        { fail(onError(it)) },
        { zipper ->
          val rest = zipper.toRestList()
          when (val result = this@unaryPlus(rest)) {
            is ParsingDsl.Ok -> {
              cursor = zipper.moveRight(result.proceed)
              rest.take(result.proceed)
            }

            is ParsingDsl.Err ->
              fail(onError(zipper.moveRight(result.proceed)))
          }
        },
      )
    }

  @ParsingDslMarker
  override suspend operator fun Parser<Tok, Ctx, Tok>.unaryPlus(): Tok =
    with(parent) {
      with(ctx) {
        +parse(cursor)
      }
    }

  @ParsingDslMarker
  override suspend operator fun Parser<
    Tok,
    Ctx,
    Iterable<Tok>,
  >.unaryPlus(): List<Tok> =
    with(parent) {
      with(ctx) {
        +parse(cursor)
      }
    }

  suspend fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    R,
  >.parse(
    onFailure: (Cursor<Tok>, E) -> Err,
  ): R =
    with(parent) {
      with(ctx) {
        parse(cursor)
      }.fold(
        { res, cur ->
          cursor = cur
          res
        },
        { res, zip, newCtx ->
          cursor = zip
          ctx = newCtx
          res
        },
        { fail(onFailure(cursor, it)) },
      )
    }

  @ParsingDslMarker
  override suspend operator fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    R,
  >.unaryPlus(): R =
    parse { cursor, _ -> onError(cursor) }

  @ParsingDslMarker
  override suspend operator fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    Iterable<R>,
  >.unaryPlus(): List<R> =
    parse { cursor, _ -> onError(cursor) }.toList()

  @ParsingDslMarker
  override suspend operator fun <R> ContinuationParser<
    Tok,
    Ctx,
    Err,
    R,
  >.unaryPlus(): R =
    parse { _, e -> fail(e) }

  @ParsingDslMarker
  override suspend operator fun <R> ContinuationParser<
    Tok,
    Ctx,
    Err,
    Iterable<R>,
  >.unaryPlus(): List<R> =
    parse { _, e -> fail(e) }.toList()
}
