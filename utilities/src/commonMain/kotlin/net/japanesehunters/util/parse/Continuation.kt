package net.japanesehunters.util.parse

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper

sealed interface Continuation<
  out Tok : Any,
  out Err,
  out Res,
>

sealed interface Ok<out Tok : Any, out Res> :
  Continuation<Tok, Nothing, Res> {
  val result: Res
  val remainder: Cursor<Tok>

  operator fun component1() = result

  operator fun component2() = remainder
}

data class Cont<out Tok : Any, out Res>(
  override val result: Res,
  override val remainder: Zipper<Tok>,
) : Ok<Tok, Res>

data class Done<out Tok : Any, out Res>(
  override val result: Res,
  override val remainder: Cursor<Tok>,
) : Ok<Tok, Res>

value class Err<out Err>(
  val error: Err,
) : Continuation<Nothing, Err, Nothing> {
  operator fun component1() = error
}
