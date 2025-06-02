package net.japanesehunters.util.parse

import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.Zipper

inline fun <
  T : Any,
  E,
  R,
  U,
  > Continuation<T, E, R>.fold(
  onOk: (R) -> U,
  onErr: (E) -> U,
): U =
  when (this) {
    is Ok -> onOk(result)
    is Err -> onErr(error)
  }

inline fun <
  T : Any,
  E,
  R,
  U,
  > Continuation<T, E, R>.fold(
  onDone: (R, Cursor<T>) -> U,
  onCont: (R, Zipper<T>) -> U,
  onErr: (E) -> U,
): U =
  when (this) {
    is Done -> onDone(result, cursor)
    is Cont -> onCont(result, remainder)
    is Err -> onErr(error)
  }

inline fun <
  T : Any,
  E,
  R1,
  R2,
  > Continuation<T, E, R1>.map(
  onOk: (R1) -> R2,
): Continuation<T, E, R2> =
  when (this) {
    is Done -> Done(onOk(result), cursor)
    is Cont -> Cont(onOk(result), remainder)
    is Err -> this
  }

inline fun <
  T : Any,
  C : Any,
  E1,
  E2,
  R,
  > Continuation<T, E1, R>.mapErr(
  onOk: (E1) -> E2,
): Continuation<T, E2, R> =
  when (this) {
    is Ok -> this
    is Err -> Err(onOk(error))
  }
