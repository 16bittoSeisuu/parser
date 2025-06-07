package net.japanesehunters.util.parse

import arrow.core.getOrElse
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.PersistentListZipper

/**
 * Shortcut for using a `Parser` with `Ctx` set to `Any`.
 */
suspend fun <T : Any, O> Parser<T, Any, O>.parse(input: Cursor<T>) =
  with(Any()) { this@parse.parse(input) }

/**
 * Shortcut for using a `Parser` with `Ctx` set to `Any` with a [List] of
 * `Tok`.
 */
suspend fun <T : Any, O> Parser<T, Any, O>.parse(input: List<T>) =
  with(Any()) {
    this@parse.parse(PersistentListZipper(input).getOrElse { it })
  }
