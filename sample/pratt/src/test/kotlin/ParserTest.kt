import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import net.japanesehunters.util.parse.fold
import net.japanesehunters.util.parse.lex.parse

class ParserTest : StringSpec() {
  init {
    "'' should fail" {
      "".shouldFail()
    }

    "'1' should be 1" {
      "1" shouldBe 1
    }

    "'1 + 1' should be 2" {
      "1 + 1" shouldBe 2
    }

    "'1 - 1' should be 0" {
      "1 - 1" shouldBe 0
    }

    "'10 * 10' should be 100" {
      "10 * 10" shouldBe 100
    }

    "'10 / 2' should be 5" {
      "10 / 2" shouldBe 5
    }

    "'10 % 3' should be 1" {
      "10 % 3" shouldBe 1
    }

    "'1 + 2 - 3' should be 0" {
      "1 + 2 - 3" shouldBe 0
    }

    "'1 + 2 * 3' should be 7" {
      "1 + 2 * 3" shouldBe 7
    }

    "'2 * (1 + 2)' should be 6" {
      "2 * (1 + 2)" shouldBe 6
    }

    "'0 + 0' should be 0" {
      "0 + 0" shouldBe 0
    }

    "'0 * 100' should be 0" {
      "0 * 100" shouldBe 0
    }

    "'100 * 0' should be 0" {
      "100 * 0" shouldBe 0
    }

    "'0 - 5' should be -5" {
      "0 - 5" shouldBe -5
    }

    "'5 - 0' should be 5" {
      "5 - 0" shouldBe 5
    }

    "'1 / 0' should fail" {
      "1 / 0".shouldFail()
    }

    "'1 % 0' should fail" {
      "1 % 0".shouldFail()
    }

    "'-5' should be -5" {
      "-5" shouldBe -5
    }

    "'-5 + 3' should be -2" {
      "-5 + 3" shouldBe -2
    }

    "'3 + -5' should be -2" {
      "3 + -5" shouldBe -2
    }

    "'-5 * -3' should be 15" {
      "-5 * -3" shouldBe 15
    }

    "'-10 / -2' should be 5" {
      "-10 / -2" shouldBe 5
    }

    "'+5' should be 5" {
      "+5" shouldBe 5
    }

    "'+-5' should be -5" {
      "+-5" shouldBe -5
    }

    "'-+5' should be -5" {
      "-+5" shouldBe -5
    }

    "'--5' should be 5" {
      "--5" shouldBe 5
    }

    "'0!' should be 1" {
      "0!" shouldBe 1
    }

    "'1!' should be 1" {
      "1!" shouldBe 1
    }

    "'3!' should be 6" {
      "3!" shouldBe 6
    }

    "'5!' should be 120" {
      "5!" shouldBe 120
    }

    "'(-5)!' should fail" {
      "(-5)!".shouldFail()
    }

    "'2 ^ 3' should be 8" {
      "2 ^ 3" shouldBe 8
    }

    "'2 ^ 0' should be 1" {
      "2 ^ 0" shouldBe 1
    }

    "'0 ^ 5' should be 0" {
      "0 ^ 5" shouldBe 0
    }

    "'1 ^ 100' should be 1" {
      "1 ^ 100" shouldBe 1
    }

    "'2 ^ -1' should be 0" {
      "2 ^ -1" shouldBe 0
    }

    "'2.0 ^ -1' should be 0.5" {
      "2.0 ^ -1" shouldBe 0.5f
    }

    "'3.14' should be 3.14" {
      "3.14" shouldBe 3.14f
    }

    "'1.5 + 2.5' should be 4.0" {
      "1.5 + 2.5" shouldBe 4.0f
    }

    "'3.0 / 2.0' should be 1.5" {
      "3.0 / 2.0" shouldBe 1.5f
    }

    "'1 + 2.5' should be 3.5f" {
      "1 + 2.5" shouldBe 3.5f
    }

    "'2.5 + 1' should be 3.5f" {
      "2.5 + 1" shouldBe 3.5f
    }

    "'1 + 2 * 3 + 4' should be 11" {
      "1 + 2 * 3 + 4" shouldBe 11
    }

    "'(1 + 2) * (3 + 4)' should be 21" {
      "(1 + 2) * (3 + 4)" shouldBe 21
    }

    "'2 ^ 3 ^ 2' should be 512 (right associative)" {
      "2 ^ 3 ^ 2" shouldBe 512
    }

    "'3! + 2!' should be 8" {
      "3! + 2!" shouldBe 8
    }

    "'(2 + 3)!' should be 120" {
      "(2 + 3)!" shouldBe 120
    }

    "'((1 + 2) * (3 + 4))' should be 21" {
      "((1 + 2) * (3 + 4))" shouldBe 21
    }

    "'(((1)))' should be 1" {
      "(((1)))" shouldBe 1
    }

    "'\"hello\" + \"world\"' should be \"helloworld\"" {
      "\"hello\" + \"world\"" shouldBe "helloworld"
    }

    "'\"test\"' should be \"test\"" {
      "\"test\"" shouldBe "test"
    }

    "'\"hello\" - \"world\"' should fail" {
      "\"hello\" - \"world\"".shouldFail()
    }

    "'\"hello\" * \"world\"' should fail" {
      "\"hello\" * \"world\"".shouldFail()
    }

    "'\"hello\" / \"world\"' should fail" {
      "\"hello\" / \"world\"".shouldFail()
    }

    "'\"hello\" % \"world\"' should fail" {
      "\"hello\" % \"world\"".shouldFail()
    }

    "'\"hello\" ^ \"world\"' should fail" {
      "\"hello\" ^ \"world\"".shouldFail()
    }

    "'-\"hello\"' should fail" {
      "-\"hello\"".shouldFail()
    }

    "'+\"hello\"' should fail" {
      "+\"hello\"".shouldFail()
    }

    "'\"hello\"!' should fail" {
      "\"hello\"!".shouldFail()
    }

    "'  1  +  2  ' should be 3" {
      "  1  +  2  " shouldBe 3
    }

    "'\\t1\\n+\\t2\\n' should be 3" {
      "\t1\n+\t2\n" shouldBe 3
    }

    "'1 2' should fail" {
      "1 2".shouldFail()
    }

    "'1 +' should fail" {
      "1 +".shouldFail()
    }

    "'(1 + 2' should fail" {
      "(1 + 2".shouldFail()
    }

    "'1 + 2)' should fail" {
      "1 + 2)".shouldFail()
    }

    "'()' should fail" {
      "()".shouldFail()
    }

    "'1 ** 2' should fail" {
      "1 ** 2".shouldFail()
    }

    "'1 + @' should fail" {
      "1 + @".shouldFail()
    }

    "'1 + 2 + ... + 100' should succeed" {
      val expression = (1..100).joinToString(" + ")
      val expected = (1..100).sum()
      expression shouldBe expected
    }

    "'1 * 2 * ... * 10' should succeed" {
      val expression = (1..10).joinToString(" * ")
      val expected = (1..10).fold(1) { acc, n -> acc * n }
      expression shouldBe expected
    }

    "'(((...(42)...)))' should be 42" {
      val depth = 20
      val opening = "(".repeat(depth)
      val closing = ")".repeat(depth)
      val expression = "${opening}42${closing}"
      expression shouldBe 42
    }

    "'1! + 2! + 3! + 4! + 5!' should be 153" {
      "1! + 2! + 3! + 4! + 5!" shouldBe 153  // 1 + 2 + 6 + 24 + 120 = 153
    }

    "'\"part1\" + \"part2\" + ... \"part20\"' should concatenate" {
      val parts = (1..20).map { "\"part$it\"" }
      val expression = parts.joinToString(" + ")
      val expected = (1..20).joinToString("") { "part$it" }
      expression shouldBe expected
    }

    "'2 ^ 2 ^ 2 ^ 2' should be 65536" {
      "2 ^ 2 ^ 2 ^ 2" shouldBe 65536  // Right associative: 2^(2^(2^2)) = 2^(2^4) = 2^16 = 65536
    }

    "'10 % 3.5' should be 3.0f" {
      "10 % 3.5" shouldBe 3.0f
    }

    "'10.5 % 3' should be 1.5f" {
      "10.5 % 3" shouldBe 1.5f
    }

    "'-3.14 + 1.14' should be -2.0f" {
      "-3.14 + 1.14" shouldBe -2.0f
    }

    "'3.14!' should fail" {
      "3.14!".shouldFail()
    }

    "'0 ^ 0' should be 1" {
      "0 ^ 0" shouldBe 1  // Mathematical convention
    }

    "'(-2) ^ 3' should be -8" {
      "(-2) ^ 3" shouldBe -8
    }

    "'(-2) ^ 2' should be 4" {
      "(-2) ^ 2" shouldBe 4
    }

    "'(-2) ^ 3' should be -8 (duplicate removed)" {
      "(-2) ^ 3" shouldBe -8
    }

    "'((((1 + 2) * 3) + 4) * 5)' should be 65" {
      "((((1 + 2) * 3) + 4) * 5)" shouldBe 65
    }

    "'\"\"' should be empty string" {
      "\"\"" shouldBe ""
    }

    "'\"hello world\"' should be \"hello world\"" {
      "\"hello world\"" shouldBe "hello world"
    }

    "'\"!@#$%^&*()\"' should be \"!@#$%^&*()\"" {
      "\"!@#$%^&*()\"" shouldBe "!@#$%^&*()"
    }

    "'\"num: \" + \"42\"' should be \"num: 42\"" {
      "\"num: \" + \"42\"" shouldBe "num: 42"
    }

  }

  suspend infix fun CharSequence.shouldBe(result: Any) {
    parser
      .parse(this)
      .fold(
        { (res, _) ->
          res.evaluate() shouldBe result
        },
        { (err) -> fail("Parser should not have failed, but got $err") },
      )
  }

  suspend fun CharSequence.shouldFail() {
    parser
      .parse(this)
      .fold(
        { (res, _) ->
          shouldThrow<Throwable> { res.evaluate() }
        },
        {}
      )
  }
}
