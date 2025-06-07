import kotlin.reflect.KClass

sealed interface Ast {
  fun evaluate(): Any
}

sealed interface LitAst<T : Any> : Ast {
  val value: T

  override fun evaluate(): Any = value
}

sealed interface ArithmeticAst : Ast {
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
          else -> throw TypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      is Float ->
        when (rightValue) {
          is Int -> calc(leftValue, rightValue)
          is Float -> calc(leftValue, rightValue)
          else -> throw TypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      is String ->
        when (rightValue) {
          is String -> calc(leftValue, rightValue)
          else -> throw TypeMismatchError(
            leftValue::class,
            rightValue::class,
            operation,
          )
        }

      else -> throw TypeMismatchError(
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

data class Plus(
  override val left: Ast,
  override val right: Ast,
) : ArithmeticAst {
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
) : ArithmeticAst {
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
    throw TypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Times(
  override val left: Ast,
  override val right: Ast,
) : ArithmeticAst {
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
    throw TypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Div(
  override val left: Ast,
  override val right: Ast,
) : ArithmeticAst {
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
    throw TypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class Mod(
  override val left: Ast,
  override val right: Ast,
) : ArithmeticAst {
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
    throw TypeMismatchError(
      left::class,
      right::class,
      operation,
    )
}

data class TypeMismatchError(
  val left: KClass<*>,
  val right: KClass<*>,
  val operation: String,
) : Exception("Cannot $operation ${left.simpleName} and ${right.simpleName}") {
  override fun toString() = message!!
}
