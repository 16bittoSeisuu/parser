package net.japanesehunters.util.parse

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper

inline fun <
  T : Any,
  C : Any,
  E,
  R,
  U,
> Continuation<T, C, E, R>.fold(
  onOk: (R) -> U,
  onErr: (E) -> U,
): U =
  when (this) {
    is Ok -> onOk(result)
    is Err -> onErr(error)
  }

inline fun <
  T : Any,
  C : Any,
  E,
  R,
  U,
> Continuation<T, C, E, R>.fold(
  onDone: (R, Cursor<T>) -> U,
  onCont: (R, Zipper<T>, C) -> U,
  onErr: (E) -> U,
): U =
  when (this) {
    is Done -> onDone(result, cursor)
    is Cont -> onCont(result, remainder, context)
    is Err -> onErr(error)
  }

inline fun <
  T : Any,
  C : Any,
  E,
  R1,
  R2,
> Continuation<T, C, E, R1>.map(
  onOk: (R1) -> R2,
): Continuation<T, C, E, R2> =
  when (this) {
    is Done -> Done(onOk(result), cursor)
    is Cont -> Cont(onOk(result), remainder, context)
    is Err -> this
  }

inline fun <
  T : Any,
  C : Any,
  E1,
  E2,
  R,
> Continuation<T, C, E1, R>.mapErr(
  onOk: (E1) -> E2,
): Continuation<T, C, E2, R> =
  when (this) {
    is Ok -> this
    is Err -> Err(onOk(error))
  }
