<div align="center">

# CivitDeck

**CivitAI のモデル・画像・プロンプトを Android & iOS でネイティブにブラウズ**

Kotlin Multiplatform (KMP) で構築

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-6366F1?style=flat-square)]()

[English](README.md) | [日本語](#features)

</div>

---

## CivitDeck とは？

[CivitAI](https://civitai.com/) は最大のオープンソース画像生成AIコミュニティです。数十万のモデル、LoRA、AI生成画像がホストされています。しかし、**公式モバイルアプリは存在しません**。

CivitDeck はそのギャップを埋めます。モデルの検索、画像の閲覧、プロンプトの確認、お気に入りの保存 — すべてスマートフォンから。

## 機能

- **モデル検索・ブラウズ** — タイプ（Checkpoint、LoRA等）、ソート、期間、タグでフィルタリング
- **画像ギャラリー** — スタッガードグリッド表示、フルスクリーンビューア、ピンチズーム対応
- **プロンプトメタデータ** — 生成パラメータ（プロンプト、モデル、サンプラー、シード）を表示、ワンタップでコピー
- **お気に入り** — モデル・画像をローカルに保存してオフラインでもアクセス
- **クロスプラットフォーム** — KMP共通コードベースからネイティブAndroid（Jetpack Compose）& iOS（SwiftUI）

## 技術スタック

| レイヤー | 技術 |
|---------|------|
| **共通 (KMP)** | Ktor Client, Kotlinx Serialization, SQLDelight, Koin |
| **Android** | Jetpack Compose, Material Design 3, Coil |
| **iOS** | SwiftUI |
| **アーキテクチャ** | Clean Architecture + MVI |
| **CI/CD** | GitHub Actions |

## はじめ方

### 前提条件

- Android Studio Ladybug 以降
- Xcode 15+（iOS向け）
- JDK 17+

### ビルド・実行

```bash
# クローン
git clone https://github.com/omooooori/CivitDeck.git
cd CivitDeck

# Android
./gradlew :androidApp:installDebug

# iOS
cd iosApp
pod install
open CivitDeck.xcworkspace
```

## コントリビュート

コントリビュート歓迎です！ガイドラインは [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。

このプロジェクトが役に立ったら、**スター**をお願いします — CivitDeck を多くの人に届ける助けになります。

## ライセンス

MIT License — [LICENSE](LICENSE) を参照

## 作者

**RIO** ([@omooooori](https://github.com/omooooori))

東京拠点のモバイルアプリ開発者 — Android | iOS | KMP
