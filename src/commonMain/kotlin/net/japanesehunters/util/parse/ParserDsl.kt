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
    suspend ParsingDsl<Tok, Ctx, Err, R>.(
      ctxTypeInfer: Ctx,
    ) -> R,
) = object : ContinuationParser<Tok, Ctx, Err, R> {
  context(ctx: Ctx)
  override suspend fun parse(input: Cursor<Tok>): Continuation<Tok, Err, R> {
    val scope =
      ParsingDslImpl<Tok, Ctx, Err, R>(
        input,
        ctx,
        // we don't want to reflect this parser's change to parent
        null,
        this,
      )
    val out =
      try {
        scope.block(ctx)
      } catch (e: ParsingDslImpl.Raise) {
        @Suppress("UNCHECKED_CAST")
        return Err(e.err as Err)
      }
    return scope.cursor.fold(
      onOutOfBounds = { cursor -> Done(out, cursor) },
      onZipper = { rem -> Cont(out, rem) },
    )
  }

  override fun toString() = name
}

@DslMarker
annotation class ParsingDslMarker

interface ParsingDsl<Tok : Any, Ctx : Any, Err, Out> {
  @ParsingDslMarker
  val self: ContinuationParser<Tok, Ctx, Err, Out>

  @ParsingDslMarker
  val tokens: List<Tok>
    get() =
      startingCursor.toRestList().take(cursor.index - startingCursor.index)

  @ParsingDslMarker
  val startingCursor: Cursor<Tok>

  @ParsingDslMarker
  val cursor: Cursor<Tok>

  @ParsingDslMarker
  var ctx: Ctx

  interface ErrorProvider<Tok : Any, Ctx : Any, Err, Out> :
    ParsingDsl<Tok, Ctx, Err, Out> {
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
    context(ctx: C)
    suspend operator fun <C : Any, E, R> ContinuationParser<
      Tok,
      C,
      E,
      R,
      >.unaryPlus(): R
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
  context(ctx: C)
  suspend operator fun <C : Any, R> ContinuationParser<
    Tok,
    C,
    Err,
    R,
    >.unaryPlus(): R =
    withError({ _ ->
      throw IllegalStateException("unreachable")
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

private class ParsingDslImpl<Tok : Any, Ctx : Any, Err, Out>(
  input: Cursor<Tok>,
  override var ctx: Ctx,
  private val parent: ParsingDslImpl<Tok, Ctx, Err, Out>? = null,
  override val self: ContinuationParser<Tok, Ctx, Err, Out>
) : ParsingDsl<Tok, Ctx, Err, Out> {
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
    block: suspend ParsingDsl<Tok, Ctx, E, Out>.() -> R,
  ): Either<E, R> =
    either {
      val catch =
        parser("catch") {
          // TODO: quick and dirty
          @Suppress("UNCHECKED_CAST")
          block() as Out
        }
      with(ctx) {
        catch.parse(cursor)
      }.fold(
        { (res, cur) ->
          cursor = cur
          @Suppress("UNCHECKED_CAST")
          res as R
        },
        { (err) ->
          raise(err)
        },
      )
    }
}

private class ParsingDslErrorProviderImpl<Tok : Any, Ctx : Any, Err, Out>(
  private val parent: ParsingDslImpl<Tok, Ctx, Err, Out>,
  private val onError: suspend (at: Cursor<Tok>) -> Err,
) : ParsingDsl.ErrorProvider<Tok, Ctx, Err, Out>,
  ParsingDsl<Tok, Ctx, Err, Out> by parent {
  override suspend fun Cursor<Tok>.zipperOrFail(): Zipper<Tok> =
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

  private suspend inline fun <C : Any, E, R> ContinuationParser<
    Tok,
    C,
    E,
    R,
  >.parse(
    ctx: C,
    onFailure: (Cursor<Tok>, E) -> Err,
  ): R =
    with(parent) {
      with(ctx) {
        parse(cursor)
      }.fold(
        { (res, cur) ->
          cursor = cur
          res
        },
        { (err) ->
          fail(onFailure(cursor, err))
        },
      )
    }

  @ParsingDslMarker
  override suspend operator fun <E, R> ContinuationParser<
    Tok,
    Ctx,
    E,
    R,
  >.unaryPlus(): R =
    parse(ctx) { cursor, _ -> onError(cursor) }

  @ParsingDslMarker
  override suspend operator fun <R> ContinuationParser<
    Tok,
    Ctx,
    Err,
    R,
  >.unaryPlus(): R =
    parse(ctx) { _, e -> fail(e) }

  context(ctx: C)
  override suspend fun <C : Any, E, R> ContinuationParser<
    Tok,
    C,
    E,
    R
    >.unaryPlus(): R = parse(ctx) { cursor, _ -> onError(cursor) }


  context(ctx: C)
  override suspend operator fun <C : Any, R> ContinuationParser<
    Tok,
    C,
    Err,
    R,
    >.unaryPlus(): R =
    parse(ctx) { _, e -> fail(e) }
}
