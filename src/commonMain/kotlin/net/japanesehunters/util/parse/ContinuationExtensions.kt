package net.japanesehunters.util.parse

inline fun <
  T : Any,
  E,
  R,
  U,
  > Continuation<T, E, R>.fold(
  onOk: (Ok<T, R>) -> U,
  onErr: (Err<E>) -> U,
): U =
  when (this) {
    is Ok -> onOk(this)
    is Err -> onErr(this)
  }

inline fun <
  T : Any,
  E,
  R,
  U,
  > Continuation<T, E, R>.fold(
  onDone: (Done<T, R>) -> U,
  onCont: (Cont<T, R>) -> U,
  onErr: (Err<E>) -> U,
): U =
  when (this) {
    is Done -> onDone(this)
    is Cont -> onCont(this)
    is Err -> onErr(this)
  }

inline fun <
  T : Any,
  E,
  R1,
  R2,
  > Continuation<T, E, R1>.map(
  onOk: (R1) -> R2,
): Continuation<T, E, R2> =
  fold(
    { (res, cur) -> Done(onOk(res), cur) },
    { (res, zip) -> Cont(onOk(res), zip) },
    { it },
  )

inline fun <
  T : Any,
  E1,
  E2,
  R,
  > Continuation<T, E1, R>.mapErr(
  onOk: (E1) -> E2,
): Continuation<T, E2, R> =
  fold(
    { it },
    { (err) -> Err(onOk(err)) },
  )
