# Kotlin Multiplatform パーサーコンビネーター

## 概要

Kotlin Multiplatform向けのデータ構造操作ツールとパーサーコンビネーターを提供するライブラリです。

## 設計原則

- **イミュータビリティ**: すべてのデータ構造がデフォルトでイミュータブル
- **型安全性**: Kotlinの型システムとArrowの関数型プログラミング概念を活用
- **メモリ効率**: 永続化データ構造を使用してメモリ割り当てを最小化
- **合成可能性**: パーサーコンビネータにより単純なパーサーから複雑なパーサーを構築可能

## 機能

### Cursor & Zipper データ構造

- **Cursor**: 位置追跡機能付きのコレクションナビゲーション
- **Zipper**: 効率的なリスト走査のためのイミュータブルデータ構造
- **PersistentListZipper**: 永続化データ構造を使用したメモリ効率的なZipper実装

### パーサーフレームワーク

- 関数型パーサーコンビネーター
- パース時の"コンテキスト"のサポート (高度なパーサーで使います)
- コレクションを連続でパースするための`Continuation`
- 簡単なパーサー合成のための拡張関数

## クイックスタート

### 基本的なCursorの使用方法

```kotlin
// リストからカーソルを作成
val list = listOf(1, 2, 3, 4, 5)
val cursor = list.cursor()

// コレクション内を移動
val moved = cursor.moveRight(2) // 右に2つ移動
val back = moved.moveLeft()    // 左に1つ移動

// 現在の要素にアクセス（範囲内の場合）
cursor.fold(
  onOutOfBounds = { println("インデックス ${it.index} で範囲外") },
  onZipper = { println("現在の要素: ${it.peek}") }
)
```

### パーサーの使用方法

```kotlin
// トークンリストでパーサーを使用
suspend fun parseExample() {
  val input = "1 + (2 + 3) * 4"
  val parser = ...
  val result = parser.parse(input)

  result.fold(
    { (result, remaining) -> 
      println("パース結果: $result")
      println("残り: $remaining")
    },
    { (error) ->
    println("パースエラー: $error")
    }
  )
}
```

### 簡単なパーサーの組み立て方

文字列からIntをパースする例：

```kotlin
// 数字パーサーを作成
val intParser: Lexer<String, Int> =
  lexer("int parser") {
    // 符号を取得（オプション）
    val sign = option { +'+'; 1 } ?: option { +'-'; -1 }

    var ret = 0
    while (true) {
      ret = ret * 10 + (option { val c = +Char::isDigit; c.digitToInt() } ?: break)
    }

    (sign ?: 1) * ret
  }

// 使用例
suspend fun main() {
  listOf("123", "-456", "+789", "abc", "12.34").forEach { input ->
    intParser.parse(input).fold(
      { (result, _) -> println("'$input' -> $result") },
      { error -> println("'$input' -> エラー: $error") },
    )
  }
}

// 出力:
// '123' -> 123
// '-456' -> -456
// '+789' -> 789
// 'abc' -> 0
// '12.34' -> 12
```

## サンプルプロジェクト

リポジトリには、以下をサポートする簡単な電卓の構築を実演するPrattパーサーの実装サンプルが含まれています：

- 基本算術演算（+ - * / ^）
- 単項演算（+ -）
- 階乗演算（!）
- 適切な演算子優先順位

電卓サンプルを実行するには：

```bash
./gradlew :sample:pratt:run
```

## 依存関係

- **Kotlin Multiplatform**: クロスプラットフォーム互換性
- **ArrowKt (Arrow Core, Arrow FX Coroutines)**: 関数型プログラミングユーティリティと型安全なエラーハンドリング
- **Kotlinx Collections Immutable**: 永続化データ構造
- **Kotest**: マルチプラットフォームプロジェクト用テストフレームワーク

## ビルド方法

```bash
# すべてのモジュールをビルド
./gradlew build

# サンプルアプリケーションを実行
./gradlew :sample:pratt:run
```

## ライセンス

詳細は[LICENSE](LICENSE)ファイルをご覧ください。

## サードパーティライセンス

サードパーティライセンス情報については、[THIRD_PARTY_LICENCES](THIRD_PARTY_LICENCES)ディレクトリをご覧ください。
