package net.japanesehunters.util.parse

import net.japanesehunters.util.collection.Cursor

/**
 * A functional interface representing a parser.
 *
 * @param Tok The type of elements in the input sequence. For example, this
 *            could be `Char` for string parsing or `Token` for token sequences.
 * @param Ctx The type representing additional context that the parser can use.
 *            This could be any contextual information required during parsing,
 *            such as binding power or a root parser object in Pratt Parsing.
 *            If no additional context is needed, `Any` can be used.
 * @param Out The result type produced by the parser upon processing the input.
 */
fun interface Parser<in Tok : Any, in Ctx : Any, out Out> {
  context(ctx: Ctx)
  suspend fun parse(input: Cursor<Tok>): Out
}
