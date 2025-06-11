import arrow.core.nonEmptyListOf
import net.japanesehunters.util.collection.fold
import net.japanesehunters.util.parse.ParsingDsl
import net.japanesehunters.util.parse.lex.any
import net.japanesehunters.util.parse.many
import net.japanesehunters.util.parse.not
import net.japanesehunters.util.parse.pratt.Associativity
import net.japanesehunters.util.parse.pratt.LedParser
import net.japanesehunters.util.parse.pratt.NudParser
import net.japanesehunters.util.parse.pratt.astParser
import net.japanesehunters.util.parse.pratt.bind
import net.japanesehunters.util.parse.pratt.expr
import net.japanesehunters.util.parse.pratt.ledParser
import net.japanesehunters.util.parse.pratt.left
import net.japanesehunters.util.parse.pratt.nudParser
import net.japanesehunters.util.parse.pratt.resetPower
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
          unaryPlus,
          unaryMinus,
        ),
        nonEmptyListOf(
          plus,
          minus,
          times,
          divide,
          mod,
          pow,
          fact,
        ),
        cmp = { _, _ -> -1 },
      ) orFail "Could not parse expression!"
    skipWhitespaces()
    cursor.fold(
      { expr },
      { fail("Could not parse expression!") },
    )
  }

val intParser: NudParser<Char, String, Ast> =
  nudParser("int parser") {
    val digits =
      repeat(some) {
        Char::isDigit orFail "Not an integer!"
      }
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
      repeat(some) {
        Char::isDigit orFail "Not a float!"
      }
    '.' orFail "Not a float!"
    val fracDigits =
      repeat(some) {
        Char::isDigit orFail "Not a float!"
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
    '"' orFail "Not a string literal!"
    val chars =
      repeat(many) {
        option { +!'"' } ?: fail("")
        option { +any } ?: fail("")
      }
    '"' orFail "Not a string literal!"
    val str = chars.joinToString("")
    StringLit(str)
  }

val parenParser: NudParser<Char, String, Ast> =
  nudParser("paren parser") {
    '(' orFail "Not a paren expression!"
    skipWhitespaces()
    val expr =
      with(ctx.resetPower()) {
        expr orFail "Could not parse expression!"
      }
    skipWhitespaces()
    ')' orFail "Not a paren expression!"
    Paren(expr)
  }

val unaryPlus = prefix('+', 3) { UnaryPlus(it) }
val unaryMinus = prefix('-', 3) { UnaryMinus(it) }

val fact = postfix('!', power = 5) { Factorial(it) }

val plus = infix('+', 1, factory = ::Plus)
val minus = infix('-', 1, factory = ::Minus)
val times = infix('*', 2, factory = ::Times)
val divide = infix('/', 2, factory = ::Div)
val mod = infix('%', 2, factory = ::Mod)
val pow = infix('^', 4, Associativity.RIGHT, factory = ::Pow)

fun prefix(
  op: Char,
  power: Int,
  factory: (Ast) -> Ast,
): NudParser<Char, String, Ast> =
  nudParser("$op (unary) parser") {
    op orFail "Not a $op (unary) expression!"
    println("ctx: $ctx")
    bind(power) { _, _ -> "Insufficient binding power!" }
    skipWhitespaces()
    val expr = expr orFail "Could not parse expression!"
    factory(expr)
  }

fun postfix(
  op: Char,
  power: Int,
  factory: (Ast) -> Ast,
): LedParser<Char, String, Ast> =
  ledParser("$op (unary) parser") {
    skipWhitespaces()
    op orFail "Not a $op (unary) expression!"
    bind(power, Associativity.LEFT) { _, _ -> "Insufficient binding power!" }
    factory(left)
  }

fun infix(
  op: Char,
  power: Int,
  associativity: Associativity = Associativity.LEFT,
  factory: (Ast, Ast) -> Ast,
): LedParser<Char, String, Ast> =
  ledParser("$op parser") {
    skipWhitespaces()
    op orFail "Not a $op expression!"
    skipWhitespaces()
    bind(power, associativity) { _, _ -> "Insufficient binding power!" }
    val right = expr orFail "Could not parse expression!"
    factory(left, right)
  }

suspend fun ParsingDsl<Char, *, *, *>.skipWhitespaces() {
  while (true) {
    option { +Char::isWhitespace } ?: break
  }
}
