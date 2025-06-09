import kotlin.math.pow
import kotlin.reflect.KClass

sealed interface Ast {
  fun evaluate(): Any
}

sealed interface LitAst<T : Any> : Ast {
  val value: T

  override fun evaluate(): Any = value
}

sealed interface UnaryOpAst : Ast {
  val operation: String
  val expr: Ast

  fun calc(value: Int): Int

  fun calc(value: Float): Float

  fun calc(value: String): String

  override fun evaluate() =
    when (val value = expr.evaluate()) {
      is Int -> calc(value)
      is Float -> calc(value)
      is String -> calc(value)
      else -> throw UnaryOpTypeMismatchError(
        value::class,
        operation,
      )
    }
}

sealed interface BinaryOpAst : Ast {
  val operation: String
  val left: Ast
  val right: Ast

  fun calc(
    left: Int,
    right: Int,
  ): Int

  fun calc(
    left: Int,
    right: Float,
  ): Float

  fun calc(
    left: Float,
    right: Int,
  ): Float

  fun calc(
    left: Float,
    right: Float,
  ): Float

  fun calc(
    left: String,
    right: String,
  ): String

  override fun evaluate(): Any {
    val leftValue = left.evaluate()
    val rightValue = right.evaluate()

    return when (leftValue) {
      is Int ->
        when (rightValue) {
          is Int -> calc(leftValue, rightValue)
          is Float -> calc(leftValue, rightValue)
          else -> throw BinaryOpTypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      is Float ->
        when (rightValue) {
          is Int -> calc(leftValue, rightValue)
          is Float -> calc(leftValue, rightValue)
          else -> throw BinaryOpTypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      is String ->
        when (rightValue) {
          is String -> calc(leftValue, rightValue)
          else -> throw BinaryOpTypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      else -> throw BinaryOpTypeMismatchError(
        leftValue::class,
        rightValue::class,
        operation,
      )
    }
  }
}

@JvmInline
value class IntLit(
  override val value: Int,
) : LitAst<Int>

@JvmInline
value class FloatLit(
  override val value: Float,
) : LitAst<Float>

@JvmInline
value class StringLit(
  override val value: String,
) : LitAst<String>

data class Paren(
  val expr: Ast,
) : Ast {
  override fun evaluate(): Any = expr.evaluate()
}

data class UnaryPlus(
  override val expr: Ast,
) : UnaryOpAst {
  override val operation: String = "apply unary plus on"

  override fun calc(value: Int) = value

  override fun calc(value: Float) = value

  override fun calc(value: String) =
    throw UnaryOpTypeMismatchError(
      String::class,
      operation,
    )
}

data class UnaryMinus(
  override val expr: Ast,
) : UnaryOpAst {
  override val operation: String = "negate"

  override fun calc(value: Int) = -value

  override fun calc(value: Float) = -value

  override fun calc(value: String) =
    throw UnaryOpTypeMismatchError(
      String::class,
      operation,
    )
}

data class Factorial(
  override val expr: Ast,
) : UnaryOpAst {
  override val operation: String = "calculate factorial of"

  override fun calc(value: Int): Int {
    class NegativeFactorialError(
      value: Int,
    ) : Exception("Cannot calculate factorial of negative number $value") {
      override fun toString() = message!!
    }
    if (value < 0) throw NegativeFactorialError(value)
    if (value == 0) return 1
    return value * calc(value - 1)
  }

  override fun calc(value: Float) =
    throw UnaryOpTypeMismatchError(
      Float::class,
      operation,
    )

  override fun calc(value: String) =
    throw UnaryOpTypeMismatchError(
      String::class,
      operation,
    )
}

data class Plus(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "add"

  override fun calc(
    left: Int,
    right: Int,
  ) = left + right

  override fun calc(
    left: Int,
    right: Float,
  ) = left + right

  override fun calc(
    left: Float,
    right: Int,
  ) = left + right

  override fun calc(
    left: Float,
    right: Float,
  ) = left + right

  override fun calc(
    left: String,
    right: String,
  ) = left + right
}

data class Minus(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "subtract"

  override fun calc(
    left: Int,
    right: Int,
  ) = left - right

  override fun calc(
    left: Int,
    right: Float,
  ) = left - right

  override fun calc(
    left: Float,
    right: Int,
  ) = left - right

  override fun calc(
    left: Float,
    right: Float,
  ) = left - right

  override fun calc(
    left: String,
    right: String,
  ): String =
    throw BinaryOpTypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Times(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "multiply"

  override fun calc(
    left: Int,
    right: Int,
  ) = left * right

  override fun calc(
    left: Int,
    right: Float,
  ) = left * right

  override fun calc(
    left: Float,
    right: Int,
  ) = left * right

  override fun calc(
    left: Float,
    right: Float,
  ) = left * right

  override fun calc(
    left: String,
    right: String,
  ): String =
    throw BinaryOpTypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Div(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "divide"

  override fun calc(
    left: Int,
    right: Int,
  ) = left / right

  override fun calc(
    left: Int,
    right: Float,
  ) = left / right

  override fun calc(
    left: Float,
    right: Int,
  ) = left / right

  override fun calc(
    left: Float,
    right: Float,
  ) = left / right

  override fun calc(
    left: String,
    right: String,
  ): String =
    throw BinaryOpTypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Mod(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "modulo"

  override fun calc(
    left: Int,
    right: Int,
  ) = left % right

  override fun calc(
    left: Int,
    right: Float,
  ) = left % right

  override fun calc(
    left: Float,
    right: Int,
  ) = left % right

  override fun calc(
    left: Float,
    right: Float,
  ) = left % right

  override fun calc(
    left: String,
    right: String,
  ): String =
    throw BinaryOpTypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Pow(
  override val left: Ast,
  override val right: Ast,
) : BinaryOpAst {
  override val operation: String = "raise to power of"

  override fun calc(
    left: Int,
    right: Int,
  ) = left.toFloat().pow(right).toInt()

  override fun calc(
    left: Int,
    right: Float,
  ) = left.toFloat().pow(right)

  override fun calc(
    left: Float,
    right: Int,
  ) = left.pow(right)

  override fun calc(
    left: Float,
    right: Float,
  ) = left.pow(right)

  override fun calc(
    left: String,
    right: String,
  ): String =
    throw BinaryOpTypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class UnaryOpTypeMismatchError(
  val value: KClass<*>,
  val operation: String,
) : Exception("Cannot $operation ${value.simpleName}") {
  override fun toString() = message!!
}

data class BinaryOpTypeMismatchError(
  val left: KClass<*>,
  val right: KClass<*>,
  val operation: String,
) : Exception("Cannot $operation ${left.simpleName} and ${right.simpleName}") {
  override fun toString() = message!!
}
