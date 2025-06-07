import arrow.core.nonEmptyListOf
import net.japanesehunters.util.collection.fold
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.lex.any
import net.japanesehunters.util.parse.lex.lexer
import net.japanesehunters.util.parse.not
import net.japanesehunters.util.parse.pratt.Associativity
import net.japanesehunters.util.parse.pratt.LedParser
import net.japanesehunters.util.parse.pratt.NudParser
import net.japanesehunters.util.parse.pratt.astParser
import net.japanesehunters.util.parse.pratt.bindOr
import net.japanesehunters.util.parse.pratt.expr
import net.japanesehunters.util.parse.pratt.ledParser
import net.japanesehunters.util.parse.pratt.left
import net.japanesehunters.util.parse.pratt.nudParser
import net.japanesehunters.util.parse.repeat
import net.japanesehunters.util.parse.some

val parser =
  astParser("expression parser") {
    skipWhitespaces()
    val expr =
      astParser(
        "expression parser internal",
        nonEmptyListOf(
          intParser,
          floatParser,
          stringParser,
          parenParser,
        ),
        nonEmptyListOf(
          plus,
          minus,
          times,
          divide,
          mod,
        ),
        cmp = { _, _ -> -1 },
      ) orFail { _ -> "Could not parse expression!" }
    cursor.fold(
      { expr },
      { fail("Could not parse expression!") },
    )
  }

val intParser: NudParser<Char, String, Ast> =
  nudParser("int parser") {
    val digits =
      +lexer("digit parser") {
        Char::isDigit orFail { _ -> "Not an integer!" }
      }.repeat(some)
    val int =
      digits
        .joinToString("")
        .toIntOrNull()
        ?: fail("Could not parse to Int!")
    IntLit(int)
  }

val floatParser: NudParser<Char, String, Ast> =
  nudParser("float parser") {
    val intDigits =
      +lexer("digit parser") {
        Char::isDigit orFail { _ -> "Not a float!" }
      }.repeat(some)
    '.' orFail { _ -> "Not a float!" }
    val fracDigits =
      +lexer("digit parser") {
        Char::isDigit orFail { _ -> "Not a float!" }
      }
    val float =
      (intDigits + '.' + fracDigits)
        .joinToString("")
        .toFloatOrNull()
        ?: fail("Could not parse to Float!")
    FloatLit(float)
  }

val stringParser: NudParser<Char, String, Ast> =
  nudParser("string parser") {
    '"' orFail { _ -> "Not a string literal!" }
    val chars = mutableListOf<Char>()
    while (true) {
      option {
        +!lexer("end") {
          '"' orFail { _ -> null }
        }
      } ?: break
      chars += option { +any } ?: break
    }
    val str = chars.joinToString("")
    StringLit(str)
  }

val parenParser: NudParser<Char, String, Ast> =
  nudParser("paren parser") {
    '(' orFail { _ -> "Not a paren expression!" }
    skipWhitespaces()
    val expr = expr orFail { _ -> "Could not parse expression" }
    skipWhitespaces()
    ')' orFail { _ -> "Not a paren expression!" }
    Paren(expr)
  }

val plus = infix('+', 1, ::Plus)
val minus = infix('-', 1, ::Minus)
val times = infix('*', 2, ::Times)
val divide = infix('/', 2, ::Div)
val mod = infix('%', 2, ::Mod)

fun infix(
  op: Char,
  power: Int,
  factory: (Ast, Ast) -> Ast,
): LedParser<Char, String, Ast> =
  ledParser("plus parser") {
    bindOr(power, Associativity.LEFT) { _, _ -> "Insufficient binding power!" }
    skipWhitespaces()
    op orFail { _ -> "Not a plus expression!" }
    skipWhitespaces()
    val right = expr orFail { _ -> "Could not parse expression" }
    factory(left, right)
  }

suspend fun ParsingDsl<Char, *, *, *>.skipWhitespaces() {
  while (true) {
    option { +Char::isWhitespace } ?: break
  }
}
