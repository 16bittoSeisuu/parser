package net.japanesehunters.util.parse

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.japanesehunters.util.collection.Cursor
import net.japanesehunters.util.collection.cursor
import net.japanesehunters.util.collection.cursorAt
import net.japanesehunters.util.parse.lex.concat
import net.japanesehunters.util.parse.lex.ignoreCase
import net.japanesehunters.util.parse.lex.lexer
import net.japanesehunters.util.parse.lex.parse

class LexerTest : StringSpec() {
  init {
    "hello lexer" {
      with(helloLexer) {
        shouldSucceed("hello", "hello")
        shouldSucceed("hellohello", "hello")
        shouldSucceed("hello world", "hello")
        shouldFail<NotHello>("hell") { it.at.index shouldBe 4 }
        shouldFail<NotHello>("Hello") { it.at.index shouldBe 0 }
        shouldFail<NotHello>("abc") { it.at.index shouldBe 0 }
        shouldFail<NotHello>("") { it.at.index shouldBe 0 }

        shouldSucceed("hello", "hello")
      }
    }

    "hello ignore case lexer" {
      with(helloIgnoreCaseLexer) {
        generateAllCaseCombinations("hello").forEach { word ->
          shouldSucceed(word, word)
        }
        shouldFail<NotHelloCaseIgnorable>("") { it.at.index shouldBe 0 }
        shouldFail<NotHelloCaseIgnorable>("hell") { it.at.index shouldBe 4 }
        shouldFail<NotHelloCaseIgnorable>("HELL") { it.at.index shouldBe 4 }
        shouldFail<NotHelloCaseIgnorable>("abc") { it.at.index shouldBe 0 }
      }
    }

    "hello then goodbye lexer" {
      with(helloLexer then goodbyeLexer) {
        shouldSucceed("hellogoodbye", "hello" to "goodbye")
        shouldFail<NotGoodbye>("hello goodbye")
        shouldFail<NotGoodbye>("hellohello")
        shouldFail<NotGoodbye>("hello world")
        shouldFail<NotHello>("")
        shouldFail<NotHello>("Hellogoodbye")
        shouldFail<NotGoodbye>("helloGoodbye")
      }
    }

    "hello or goodbye lexer" {
      with(helloLexer or goodbyeLexer) {
        shouldSucceed("hello", "hello")
        shouldSucceed("goodbye", "goodbye")
        shouldSucceed("hellogoodbye", "hello")
        shouldSucceed("goodbyehello", "goodbye")
        shouldFail<Pair<NotHello, NotGoodbye>>("abc") { (h, _) ->
          h.at.index shouldBe 0
        }
        shouldFail<Pair<NotHello, NotGoodbye>>("") { (h, _) ->
          h.at.index shouldBe 0
        }
      }
    }

    "hello mapped to goodbye lexer" {
      with(helloLexer map { "goodbye" }) {
        shouldSucceed("hello", "goodbye")
        shouldSucceed("hellohello", "goodbye")
        shouldSucceed("hello world", "goodbye")
        shouldFail<NotHello>("abc") { it.at.index shouldBe 0 }
      }
    }

    "hello error mapped to ErrorTest lexer" {
      data class ErrorTest(
        val original: NotHello,
      )
      with(helloLexer mapErr ::ErrorTest) {
        shouldFail<ErrorTest>("hell") { it.original.at.index shouldBe 4 }
        shouldFail<ErrorTest>("Hello") { it.original.at.index shouldBe 0 }
        shouldFail<ErrorTest>("abc") { it.original.at.index shouldBe 0 }
      }
    }

    "hello2 lexer" {
      with(helloLexer repeat 2 map concat) {
        shouldFail<NotHello>("hello") { it.at.index shouldBe 5 }
        shouldSucceed("hellohello", "hellohello")
        shouldSucceed("hellohellohello", "hellohello")
        shouldFail<NotHello>("abc") { it.at.index shouldBe 0 }
      }
    }

    "hello2..3 lexer" {
      with(helloLexer repeat 2..3 map concat) {
        shouldFail<NotHello>("hello") { it.at.index shouldBe 5 }
        shouldSucceed("hellohello", "hellohello")
        shouldSucceed("hellohellohello", "hellohellohello")
        shouldSucceed("hellohellohellohello", "hellohellohello")
        shouldFail<NotHello>("abc") { it.at.index shouldBe 0 }
      }
    }

    "hello repeat many lexer" {
      with(helloLexer repeat many map concat) {
        repeat(100) {
          shouldSucceed("hello".repeat(it), "hello".repeat(it))
        }
        shouldSucceed("abc", "")
        shouldSucceed("hell", "")
      }
    }

    "hello repeat some lexer" {
      with(helloLexer repeat some map concat) {
        shouldFail<NotHello>("") { it.at.index shouldBe 0 }
        repeat(100) {
          if (it == 0) return@repeat
          shouldSucceed("hello".repeat(it), "hello".repeat(it))
        }
        shouldFail<NotHello>("abc") { it.at.index shouldBe 0 }
        shouldFail<NotHello>("hell") { it.at.index shouldBe 4 }
      }
    }

    "not hello lexer" {
      with(!helloLexer) {
        shouldFail<String>("hello") { it shouldBe "hello" }
        shouldFail<String>("hellohello") { it shouldBe "hello" }
        shouldSucceed("", NotHello("".cursor()))
        shouldSucceed("hell", NotHello("hell".cursorAt(4)))
      }
    }

    "hello onOk onErr lexer" {
      var i = false
      with(helloLexer onOk { i = true } onErr { i = false }) {
        parse("hello").also { i shouldBe true }
        parse("hellohello").also { i shouldBe true }
        parse("hello world").also { i shouldBe true }
        parse("").also { i shouldBe false }
        parse("hell").also { i shouldBe false }
        parse("Hello").also { i shouldBe false }
        parse("abc").also { i shouldBe false }
      }
    }
  }

  data class NotHello(
    val at: Cursor<Char>,
  ) {
    override fun toString() = "Expecting 'hello', but mismatch at $at"
  }

  data class NotHelloCaseIgnorable(
    val at: Cursor<Char>,
  ) {
    override fun toString() =
      "Expecting 'hello' (case ignorable), but mismatch at $at"
  }

  class NotGoodbye {
    override fun toString() = "Expecting 'goodbye', but mismatch"
  }

  val helloLexer =
    lexer("'hello' lexer") {
      "hello" orFail ::NotHello
    }

  val helloIgnoreCaseLexer =
    lexer("'hello' ignore case lexer") {
      ignoreCase("hello") orFail ::NotHelloCaseIgnorable
    }

  val goodbyeLexer =
    lexer("'goodbye' lexer") {
      "goodbye" orFail ::NotGoodbye
    }

  suspend inline fun <R> ContinuationParser<Char, Any, Any, R>.shouldSucceed(
    with: String,
    to: R,
  ) {
    withClue(with) {
      val cont = parse(with)
      cont.shouldBeInstanceOf<Ok<*, *, *>>()
      cont.result shouldBe to
    }
  }

  suspend inline fun <reified E> ContinuationParser<
    Char,
    Any,
    Any,
    Any,
  >.shouldFail(
    with: String,
    crossinline block: (E) -> Unit = { },
  ) {
    withClue(with) {
      val cont = parse(with)
      cont.shouldBeInstanceOf<Err<*>>()
      val e = cont.error.shouldBeInstanceOf<E>()
      block(e)
    }
  }

  fun generateAllCaseCombinations(word: CharSequence): List<String> {
    if (word.isEmpty()) return listOf("")

    val lower = word[0].lowercaseChar()
    val upper = word[0].uppercaseChar()

    return generateAllCaseCombinations(word.drop(1))
      .flatMap { listOf("$lower$it", "$upper$it") }
  }

  // Apparently, tests with Kotest do not support
  // context(...) syntax. It compiles, but fails runtime.

  /*
    context(
      parser: ContinuationParser<Char, Any, Any, R>
    )
    suspend inline infix fun <R> String.shouldBeLexedTo(expect: R) {
      this@shouldBeLexedTo {
        val cont = with(parser) { parse(this@shouldBeLexedTo) }
        cont.shouldBeInstanceOf<Ok<*, *, *>>()
        cont.result shouldBe expect
      }
    }

    context(
      parser: ContinuationParser<Char, Any, Any, Any>
    )
    suspend inline fun <reified E> String.shouldFail(
      crossinline block: (E) -> Unit = { },
    ) {
      this@shouldFail {
        val cont = parser.parse(this@shouldFail)
        cont.shouldBeInstanceOf<Err<*>>()
        val e = cont.error.shouldBeInstanceOf<E>()
        block(e)
      }
    }
   */
}
