# Parser Utilities for Kotlin Multiplatform

[日本語版はこちら](README-ja.md) | Japanese version

## Overview

A Kotlin Multiplatform library providing powerful parsing utilities and data structure manipulation tools. This library offers efficient cursor-based navigation through collections and a flexible parser framework for building custom parsers.

## Features

### 🎯 Cursor & Zipper Data Structures
- **Cursor**: Navigate through collections with position tracking
- **Zipper**: Immutable data structure for efficient list traversal
- **PersistentListZipper**: Memory-efficient zipper implementation using persistent data structures

### 📝 Parser Framework
- Functional parser combinators
- Support for custom parsing contexts
- Continuation-based parsing for complex scenarios
- Extension functions for easy parser composition

### 🔧 Collection Utilities
- Extension functions for List and NonEmptyList
- Character sequence parsing support
- Bounds-safe navigation with OutOfBounds handling

## Quick Start

### Basic Cursor Usage

```kotlin
import net.japanesehunters.util.collection.*

// Create a cursor from a list
val list = listOf(1, 2, 3, 4, 5)
val cursor = list.cursor()

// Navigate through the collection
val moved = cursor.moveRight(2) // Move 2 positions right
val back = moved.moveLeft(1)    // Move 1 position left

// Access current element (if in bounds)
cursor.fold(
  onOutOfBounds = { println("Out of bounds at index ${it.index}") },
  onZipper = { println("Current element: ${it.peek}") }
)
```

### String Parsing Example

```kotlin
import net.japanesehunters.util.collection.*

// Parse a string character by character
val text = "Hello, World!"
val charCursor = text.cursor()

// Navigate through characters
charCursor.fold(
  onOutOfBounds = { /* Handle out of bounds */ },
  onZipper = { zipper ->
    println("First character: ${zipper.peek}")
    val next = zipper.moveRight()
    // Continue parsing...
  }
)
```

### Parser Usage

```kotlin
import net.japanesehunters.util.parse.*

// Use parser with a list of tokens
suspend fun parseExample() {
  val tokens = listOf("hello", "world")
  val result = someParser.parse(tokens)

  result.fold(
    { (result, remaining) -> 
      println("Parsed: $result")
      println("Remaining: $remaining")
    },
    { error -> 
      println("Parse error: $error")
    }
  )
}
```

## Sample Project

The repository includes a sample Pratt parser implementation that demonstrates building a calculator with support for:
- Basic arithmetic operations (+ - * / ^)
- Unary operations (+ -)
- Factorial operation (!)
- Proper operator precedence

To run the calculator sample:

```bash
./gradlew :sample:pratt:run
```

## Architecture

### Core Components

- **Cursor Interface**: Base abstraction for navigating collections
- **Zipper Interface**: Extended cursor with guaranteed current element
- **PersistentListZipper**: Concrete implementation using immutable collections
- **Parser Framework**: Functional parsing with continuation support

### Design Principles

- **Immutability**: All data structures are immutable by default
- **Type Safety**: Leverages Kotlin's type system and Arrow's functional programming concepts
- **Memory Efficiency**: Uses persistent data structures to minimize memory allocation
- **Composability**: Parser combinators allow building complex parsers from simple ones

## Dependencies

- **Kotlin Multiplatform**: Cross-platform compatibility
- **Arrow Core**: Functional programming utilities
- **Kotlinx Collections Immutable**: Persistent data structures
- **Kotest**: Testing framework

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run sample application
./gradlew :sample:pratt:run
```

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

See [LICENSE](LICENSE) file for details.

## Third Party Licenses

See [THIRD_PARTY_LICENCES](THIRD_PARTY_LICENCES) directory for third-party license information.
