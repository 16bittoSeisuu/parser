package net.japanesehunters.util.parse

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper

sealed interface Continuation<
  out Tok : Any,
  out Ctx : Any,
  out Err,
  out Res,
>

sealed interface Ok<out Tok : Any, out Ctx : Any, out Res> :
  Continuation<Tok, Ctx, Nothing, Res> {
  val result: Res
}

data class Cont<out Tok : Any, out Ctx : Any, out Res>(
  override val result: Res,
  val remainder: Zipper<Tok>,
  val context: Ctx,
) : Ok<Tok, Ctx, Res>

data class Done<out Tok : Any, out Res>(
  override val result: Res,
  val cursor: Cursor<Tok>,
) : Ok<Tok, Nothing, Res>

value class Err<out Err>(
  val error: Err,
) : Continuation<Nothing, Nothing, Err, Nothing>
