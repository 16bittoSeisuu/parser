package net.japanesehunters.util.collection

import arrow.core.NonEmptyList

fun <T> List<T>.cursor(): Cursor<T> = cursorAt(index = 0)

fun <T> List<T>.cursorAt(index: Int): Cursor<T> =
  PersistentListZipper(this, index).fold({ it }, { it })

fun <T> NonEmptyList<T>.cursor(): Zipper<T> = PersistentListZipper(this)

fun CharSequence.cursor(): Cursor<Char> = this.toList().cursor()

fun CharSequence.cursorAt(index: Int): Cursor<Char> =
  this.toList().cursorAt(index)

inline fun <T, R> Cursor<T>.fold(
  onOutOfBounds: (OutOfBounds<T>) -> R,
  onZipper: (Zipper<T>) -> R,
) = when (this) {
  is OutOfBounds -> onOutOfBounds(this)
  is Zipper -> onZipper(this)
}

fun <T> Zipper<T>.first(): Zipper<T> =
  moveLeft(1).fold(
    { this },
    { it.first() },
  )

fun <T> Zipper<T>.last(): Zipper<T> =
  moveRight(1).fold(
    { this },
    { it.last() },
  )
