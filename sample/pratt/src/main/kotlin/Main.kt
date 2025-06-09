import net.japanesehunters.util.parse.fold
import net.japanesehunters.util.parse.lex.parse

suspend fun main() {
  println("=== Simple Calculator ===")
  println("Supported operators:")
  println(" - plus, minus, multiply, divide, power")
  println(" - unary plus, minus")
  println(" - factorial")
  println("Type \"quit\" to exit.")

  while (true) {
    print("> ")
    val expr = readlnOrNull() ?: break
    if (expr == "quit") break
    parser
      .parse(expr)
      .fold(
        { (res, _) ->
          try {
            println("${res.evaluate()}")
          } catch (e: Exception) {
            println("Error: $e")
          }
        },
        { (err) -> println("Error: $err") },
      )
  }
}
