# Kotlin Multiplatform パーサーユーティリティ

## 概要

Kotlin Multiplatform向けの強力なパーシングユーティリティとデータ構造操作ツールを提供するライブラリです。このライブラリは、コレクションの効率的なカーソルベースナビゲーションと、カスタムパーサー構築のための柔軟なパーサーフレームワークを提供します。

## 機能

### 🎯 Cursor & Zipper データ構造
- **Cursor**: 位置追跡機能付きのコレクションナビゲーション
- **Zipper**: 効率的なリスト走査のためのイミュータブルデータ構造
- **PersistentListZipper**: 永続化データ構造を使用したメモリ効率的なZipper実装

### 📝 パーサーフレームワーク
- 関数型パーサーコンビネータ
- カスタムパーシングコンテキストのサポート
- 複雑なシナリオ向けの継続ベースパーシング
- 簡単なパーサー合成のための拡張関数

### 🔧 コレクションユーティリティ
- ListとNonEmptyList用の拡張関数
- 文字列パーシングサポート
- OutOfBounds処理による境界安全なナビゲーション

## クイックスタート

### 基本的なCursorの使用方法

```kotlin
import net.japanesehunters.util.collection.*

// リストからカーソルを作成
val list = listOf(1, 2, 3, 4, 5)
val cursor = list.cursor()

// コレクション内を移動
val moved = cursor.moveRight(2) // 右に2つ移動
val back = moved.moveLeft(1)    // 左に1つ移動

// 現在の要素にアクセス（境界内の場合）
cursor.fold(
  onOutOfBounds = { println("インデックス ${it.index} で境界外") },
  onZipper = { println("現在の要素: ${it.peek}") }
)
```

### 文字列パーシングの例

```kotlin
import net.japanesehunters.util.collection.*

// 文字列を文字単位でパース
val text = "Hello, World!"
val charCursor = text.cursor()

// 文字間を移動
charCursor.fold(
  onOutOfBounds = { /* 境界外の処理 */ },
  onZipper = { zipper ->
    println("最初の文字: ${zipper.peek}")
    val next = zipper.moveRight()
    // パーシングを続行...
  }
)
```

### パーサーの使用方法

```kotlin
import net.japanesehunters.util.parse.*

// トークンリストでパーサーを使用
suspend fun parseExample() {
  val tokens = listOf("hello", "world")
  val result = someParser.parse(tokens)

  result.fold(
    { (result, remaining) -> 
      println("パース結果: $result")
      println("残り: $remaining")
    },
    { error -> 
      println("パースエラー: $error")
    }
  )
}
```

## サンプルプロジェクト

リポジトリには、以下をサポートする計算機の構築を実演するPrattパーサーの実装サンプルが含まれています：
- 基本算術演算（+ - * / ^）
- 単項演算（+ -）
- 階乗演算（!）
- 適切な演算子優先順位

計算機サンプルを実行するには：

```bash
./gradlew :sample:pratt:run
```

## アーキテクチャ

### コアコンポーネント

- **Cursor Interface**: コレクションナビゲーションの基本抽象化
- **Zipper Interface**: 現在要素が保証された拡張カーソル
- **PersistentListZipper**: イミュータブルコレクションを使用した具体実装
- **Parser Framework**: 継続サポート付きの関数型パーシング

### 設計原則

- **イミュータビリティ**: すべてのデータ構造がデフォルトでイミュータブル
- **型安全性**: Kotlinの型システムとArrowの関数型プログラミング概念を活用
- **メモリ効率**: 永続化データ構造を使用してメモリ割り当てを最小化
- **合成可能性**: パーサーコンビネータにより単純なパーサーから複雑なパーサーを構築可能

## 依存関係

- **Kotlin Multiplatform**: クロスプラットフォーム互換性
- **Arrow Core**: 関数型プログラミングユーティリティ
- **Kotlinx Collections Immutable**: 永続化データ構造
- **Kotest**: テストフレームワーク

## ビルド方法

```bash
# すべてのモジュールをビルド
./gradlew build

# テストを実行
./gradlew test

# サンプルアプリケーションを実行
./gradlew :sample:pratt:run
```

## 貢献

貢献を歓迎します！イシューやプルリクエストをお気軽に提出してください。

## ライセンス

詳細は[LICENSE](LICENSE)ファイルをご覧ください。

## サードパーティライセンス

サードパーティライセンス情報については、[THIRD_PARTY_LICENCES](THIRD_PARTY_LICENCES)ディレクトリをご覧ください。
