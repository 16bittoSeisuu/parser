# Kotlin Multiplatform Parser Combinators

[日本語版はこちら](README-ja.md) | Japanese version

## Overview

A library that provides data structure manipulation tools and parser combinators for Kotlin Multiplatform.

## Design Principles

- **Immutability**: All data structures are immutable by default
- **Type Safety**: Leverages Kotlin's type system and Arrow's functional programming concepts
- **Memory Efficiency**: Uses persistent data structures to minimize memory allocation
- **Composability**: Parser combinators allow building complex parsers from simple ones

## Features

### Cursor & Zipper Data Structures

- **Cursor**: Navigate through collections with position tracking
- **Zipper**: Immutable data structure for efficient list traversal
- **PersistentListZipper**: Memory-efficient zipper implementation using persistent data structures

### Parser Framework

- Functional parser combinators
- Support for parsing "context" (used in advanced parsers)
- `Continuation` for continuous parsing of collections
- Extension functions for easy parser composition

## Quick Start

### Basic Cursor Usage

```kotlin
// Create a cursor from a list
val list = listOf(1, 2, 3, 4, 5)
val cursor = list.cursor()

// Navigate through the collection
val moved = cursor.moveRight(2) // Move 2 positions right
val back = moved.moveLeft()    // Move 1 position left

// Access current element (if in bounds)
cursor.fold(
  onOutOfBounds = { println("Out of bounds at index ${it.index}") },
  onZipper = { println("Current element: ${it.peek}") }
)
```

### Parser Usage

```kotlin
// Use parser with tokens
suspend fun parseExample() {
  val input = "1 + (2 + 3) * 4"
  val parser = ...
  val result = parser.parse(input)

  result.fold(
    { (result, remaining) -> 
      println("Parsed: $result")
      println("Remaining: $remaining")
    },
    { (error) -> 
      println("Parse error: $error")
    }
  )
}
```

### Building Simple Parsers

Example of parsing an Int from a string:

```kotlin
// Create a number parser
val intParser: Lexer<String, Int> =
  lexer("int parser") {
    // Get sign (optional)
    val sign = option { +'+'; 1 } ?: option { +'-'; -1 }

    var ret = 0
    while (true) {
      ret = ret * 10 + (option { val c = +Char::isDigit; c.digitToInt() } ?: break)
    }

    (sign ?: 1) * ret
  }

// Usage example
suspend fun main() {
  listOf("123", "-456", "+789", "abc", "12.34").forEach { input ->
    intParser.parse(input).fold(
      { (result, _) -> println("'$input' -> $result") },
      { error -> println("'$input' -> Error: $error") },
    )
  }
}

// Output:
// '123' -> 123
// '-456' -> -456
// '+789' -> 789
// 'abc' -> 0
// '12.34' -> 12
```

## Sample Project

The repository includes a sample Pratt parser implementation that demonstrates building a simple calculator with support
for:

- Basic arithmetic operations (+ - * / ^)
- Unary operations (+ -)
- Factorial operation (!)
- Proper operator precedence

To run the calculator sample:

```bash
./gradlew :sample:pratt:run
```

## Dependencies

- **Kotlin Multiplatform**: Cross-platform compatibility
- **ArrowKt (Arrow Core, Arrow FX Coroutines)**: Functional programming utilities and type-safe error handling
- **Kotlinx Collections Immutable**: Persistent data structures
- **Kotest**: Testing framework for multiplatform projects

## Building

```bash
# Build all modules
./gradlew build

# Run sample application
./gradlew :sample:pratt:run
```

## License

See [LICENSE](LICENSE) file for details.

## Third Party Licenses

See [THIRD_PARTY_LICENCES](THIRD_PARTY_LICENCES) directory for third-party license information.
