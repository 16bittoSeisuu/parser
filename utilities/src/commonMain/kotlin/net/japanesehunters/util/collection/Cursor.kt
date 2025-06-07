package net.japanesehunters.util.collection

import arrow.core.NonEmptyList

sealed interface Cursor<out T> {
  val list: List<T>
  val index: Int

  fun moveRight(n: Int = 1): Cursor<T>

  fun moveLeft(n: Int = 1): Cursor<T>

  fun toRestList(): List<T>

  fun toList(): List<T>
}

data class OutOfBounds<out T>(
  override val list: List<T>,
  override val index: Int,
) : Cursor<T> {
  init {
    require(index !in list.indices) {
      "Cannot create OutOfBounds(Cursor), " +
        "index $index is in bounds of collection"
    }
  }

  override fun toString(): String {
    val sb = StringBuilder()
    if (index < 0) {
      sb.append(">>, ")
    }
    sb.append("[")
    list.forEach {
      sb.append("$it, ")
    }
    if (list.isNotEmpty()) {
      sb.deleteRange(sb.length - 2, sb.length)
    }
    sb.append("]")
    if (list.size <= index) {
      sb.append(", >>")
    }
    return sb.toString()
  }

  override fun moveRight(n: Int): Cursor<T> =
    list.cursorAt(index + n).fold(
      { this },
      { it },
    )

  override fun moveLeft(n: Int): Cursor<T> =
    list.cursorAt(index - n).fold(
      { this },
      { it },
    )

  override fun toRestList(): List<T> =
    if (index < 0) {
      toList()
    } else {
      list.drop(index)
    }

  override fun toList() = list.toList()
}

interface Zipper<out T> : Cursor<T> {
  val peek: T

  override fun toRestList(): NonEmptyList<T>

  override fun toList(): NonEmptyList<T>
}
