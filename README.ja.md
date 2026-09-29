# MCGF AI Companion

> [English](README.md) | [Tiếng Việt](README.vi.md) | 日本語

**Minecraft 1.21.1 Java Edition (Fabric)** 用の仲間mod。人型の相棒を召喚：ついてくる、共闘、採掘、農業、AIチャット、専用インベントリ付き。

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC `1.21.1` | Fabric Loader `0.16.14` | Fabric API `0.102.0+1.21.1` | Java `21`

## 動作環境

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (ソースからビルドする場合のみ)

## インストール

1. https://github.com/Khoand-ed/Minecraft-GF/releases （最新）から
   `mcgf-ai-companion-<version>.jar` をダウンロード。
2. `.minecraft/mods/` に入れる（Fabric 1.21.1 + Fabric APIのプロファイル）。
3. チートONのワールド（サーバーならOP）でプレイ。

## クイックスタート（ゲーム内）

```
/gf spawn    # 仲間を召喚
/gf follow   # ついてくる・守ってくれる
@gf こんにちは  # チャット（日・英・ベトナム語、キー不要）
/gf lang ja  # AI言語切替: vi | en | ja
```

## コマンド

### 仲間

| コマンド | 内容 |
|---|---|
| `/gf spawn` | 召喚（あなたに懐く、名前付き、リログしても消えない） |
| `/gf follow` | ついてくる（デフォルト） |
| `/gf stay` | その場で待つ（スニークポーズ） |
| `/gf here` | 自分の横にテレポート |
| `/gf goto <x> <y> <z>` | 座標へ移動して待機 |
| `/gf dismiss` | 帰らせる |
| `/gf name <名前>` | 改名（頭上の名前も変わる） |
| `/gf help` | 全コマンド一覧（機能別） |

`teleportDistance`（デフォルト24ブロック）以上離れると自動で戻る。

### 戦闘・サバイバル

| コマンド | 内容 |
|---|---|
| `/gf attack` | 近くの敵対mobを攻撃（16ブロック） |
| `/gf stop` | 戦闘停止・作業キャンセル |
| `/gf mine` | 見ているブロック（6ブロック以内）を掘ってくれる。ドロップは本物 |
| `/gf collect` | 周囲のドロップ＋経験値を回収（10ブロック） |
| `/gf feed` | 持ち物の肉を食べさせて回復 |

非戦闘時は徐々に自然回復。焼いた肉の方が回復量が多い。

### 自動作業（1ブロック/4tickで順番に）

| コマンド | 内容 |
|---|---|
| `/gf minevein` | 近くの鉱脈ごと採掘（最大32ブロック、半径24） |
| `/gf chop` | 近くの木を丸ごと伐採 |
| `/gf farm` | 熟した作物を収穫＋植え直し |
| `/gf bag` | ペット専用9スロットを見る |
| `/gf give` | カバンの中身を受け取る（溢れた分は足元に） |
| `/gf deposit` | 近くのチェスト/樽に保管（8ブロック） |

ドロップはサバイバル通り地面に落ちてからペットが回収。旧ワールドのオオカミ型も自動で人型に変わり、中身は保持。

### 装備（ステータス本物、ペットに保存）

| コマンド | 内容 |
|---|---|
| `/gf equip` | 最強の武器・防具を自動装備（カバン優先、次に持ち物） |
| `/gf gear` | 装備・攻撃力・防御点を表示 |
| `/gf unequip` | 全部脱いで持ち物に戻す |

攻撃力・防御力はバニラのダメージ計算通り。見た目も反映：右手に武器、防具は部位ごとに表示。

### AIチャット

チャットのどこでもプレフィックス（デフォルト `@gf`）で話しかけ：

```
@gf クリーパーってなに？
@gf 今どこにいる？
```

| コマンド | 内容 |
|---|---|
| `/gf ask <質問>` | プレフィックスと同じ |
| `/gf forget` | 会話メモリを消去 |
| `/gf ai on\|off` | オンラインAI切替（OP） |
| `/gf apikey <キー>` | 無料Geminiキー登録（OP、表示はマスク） |

キーあり：Geminiを非同期で呼ぶ（ラグなし）。位置・体力・満腹度＋直近の会話を把握。キーなし（通信エラー時も）オフラインで日本語回答。言語は `/gf lang`（`vi` | `en` | `ja`）。メモリはサーバー停止時に `config/mcgf_history.json` に保存、起動時に復元（プレイヤー別）。無料キーは https://aistudio.google.com/apikey。

### その他

| コマンド | 内容 |
|---|---|
| `/gf say <text>` | 復唱させる |
| `/gf hello` | 挨拶 |
| `/gf config` | 設定表示 |
| `/gf prefix <p>` | プレフィックス変更（OP） |
| `/gf lang vi\|en\|ja` | AI言語（ベトナム語/英語/日本語） |
| `/gf version` | バージョン表示 |

## スキン

仲間はプレイヤー型（クラシック腕）。優先順位（再ビルド不要）：

1. `config/mcgf_skin.png` — 64x64のPNGをconfigフォルダに入れてリログ。
2. `config/mcgf.json` の `skinUrl` — 直接PNGリンク。ファイルがない時に使用。
3. 内蔵デフォルト（青パーカー＋ジーンズ）。

## 設定 (`config/mcgf.json`)

```json
{
  "companionName": "GF",
  "chatPrefix": "@gf",
  "followDistance": 3.0,
  "teleportDistance": 24.0,
  "replyInVietnamese": true,
  "language": "vi",
  "aiEnabled": true,
  "maxHistory": 8,
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash",
  "skinFile": "mcgf_skin.png",
  "skinUrl": ""
}
```

`config/mcgf.json` と `config/mcgf_history.json` はgitignore済み — APIキーは外部に出ません。

## マルチプレイ

- **Fabric 1.21.1サーバー**で動作：サーバーの `mods` にmod＋Fabric API。
- **全員が** Fabric＋mod必須（カスタムエンティティのため）。
- Realms・バニラ/Paper/Spigotサーバーは**不可**。
- OP限定：`apikey`、`ai`、`prefix`。ペットとAIメモリはプレイヤー別。

## ソースからビルド

`main` にpushで `Build mod` ワークフロー（Gradle 8.10.2 + JDK 21）が走り、Artifactsに `.jar`。手元でも：

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-<version>.jar
```

## 動作チェックリスト

```
/gf version → 現バージョン
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
/gf minevein → 完了待ち → /gf bag → /gf give → /gf deposit（ペット横にチェスト）
/gf chop → /gf farm（熟した畑が必要）
/gf equip → /gf gear（攻撃・防御が正しい）→ 武器・防具が見た目に反映
/gf apikey <キー> → @gf 今どこにいる？（位置を把握）
/gf prefix @bot → 「@bot こんにちは」
/gf lang en → @gf hello（英語で返事）
/gf config → スキン含め設定表示
64x64 PNGをconfig/mcgf_skin.pngに → リログ → 見た目変更
サーバー再起動 → AIメモリ・カバン保持
```

## ライセンス

MIT — `LICENSE` 参照。
