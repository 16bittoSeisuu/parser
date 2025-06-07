package net.japanesehunters.util.collection

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@ConsistentCopyVisibility
data class PersistentListZipper<T> private constructor(
  private val lefts: PersistentList<T> = persistentListOf(),
  override val peek: T,
  private val rights: PersistentList<T> = persistentListOf(),
) : Zipper<T> {
  override val list get() = toList()

  override val index get() = lefts.size

  override fun moveRight(n: Int): Cursor<T> {
    require(0 <= n) { "Cannot move negative elements right." }
    if (n == 0) return this
    val next =
      rights.firstOrNull()
        ?: return OutOfBounds(toList(), lefts.size + 1)
    val result =
      PersistentListZipper(
        lefts = lefts.add(0, peek),
        peek = next,
        rights = rights.removeAt(0),
      )
    return if (1 < n) {
      result.moveRight(n - 1)
    } else {
      result
    }
  }

  override fun moveLeft(n: Int): Cursor<T> {
    require(0 <= n) { "Cannot move negative elements left." }
    if (n == 0) return this
    val prev =
      lefts.firstOrNull()
        ?: return OutOfBounds(this.toList(), -1)
    val result =
      PersistentListZipper(
        lefts = lefts.removeAt(0),
        peek = prev,
        rights = rights.add(0, peek),
      )
    return if (1 < n) {
      result.moveLeft(n - 1)
    } else {
      result
    }
  }

  override fun toPassedList(): List<T> = lefts.reversed()

  override fun toRestList(): NonEmptyList<T> = nonEmptyListOf(peek) + rights

  override fun toList(): NonEmptyList<T> =
    (lefts.reversed() + toRestList()).toNonEmptyListOrNull()!!

  override fun toString(): String {
    val sb = StringBuilder()
    sb.append("[")
    lefts.reversed().forEach {
      sb.append("$it, ")
    }
    sb.append(">>$peek, ")
    rights.forEach {
      sb.append("$it, ")
    }
    sb.deleteRange(sb.length - 2, sb.length)
    sb.append("]")
    return sb.toString()
  }

  companion object {
    operator fun <T> invoke(
      collection: List<T>,
      index: Int = 0,
    ): Either<OutOfBounds<T>, PersistentListZipper<T>> =
      either {
        val list =
          collection.toNonEmptyListOrNull()
            ?: raise(OutOfBounds(emptyList(), index))
        invoke(
          collection = list,
          index = index,
        ).bind()
      }

    operator fun <T> invoke(
      collection: NonEmptyList<T>,
    ): PersistentListZipper<T> = invoke(collection, 0).getOrNull()!!

    operator fun <T> invoke(
      collection: NonEmptyList<T>,
      index: Int,
    ): Either<OutOfBounds<T>, PersistentListZipper<T>> =
      either {
        if (index !in collection.indices) {
          raise(OutOfBounds(collection, index))
        }
        val lefts = collection.take(index).reversed().toPersistentList()
        val current = collection[index]
        val rights = collection.drop(index + 1).toPersistentList()
        PersistentListZipper(
          lefts = lefts,
          peek = current,
          rights = rights,
        )
      }
  }
}
