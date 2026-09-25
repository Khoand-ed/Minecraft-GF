# MCGF AI Companion — Minecraft 1.21.1 Java (Fabric)

AI bạn đồng hành chơi cùng bạn trong Minecraft 1.21.1 Java Edition, dạng **Fabric mod**.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC: `1.21.1` | Loader: `Fabric 0.16.14` | Java: `21` | Yarn: `1.21.1+build.3`

## Lộ trình module (làm từng bước, bạn kiểm tra từng bước)

- [x] **M1 — Core:** scaffold Fabric 1.21.1, config `config/mcgf.json`, lệnh `/gf help|hello|version|name|say`
- [x] **M2 — Follow:** companion sói đi theo, `/gf spawn|follow|stay|here|goto|dismiss`, tự teleport khi lạc
- [x] **M3 — Chat AI:** chat `@gf <câu hỏi>`, Gemini online + fallback offline, `/gf ask|ai|forget|apikey`
- [x] **M4 — Sinh tồn:** `/gf attack|stop|mine|collect|feed`, tự hồi máu ngoài giao tranh
- [x] **M5 — Polish (hiện tại):** lưu trí nhớ AI, `/gf config|prefix`, CI build ra `.jar`

**Quy tắc:** xong 1 module → bạn test → OK mới làm module tiếp. Chưa push khi bạn chưa duyệt.

## Yêu cầu máy

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (`java -version` phải ra 21)
- Gradle 8.10.2 (dùng `./gradlew` kèm theo, lần đầu cần mạng để tải MC + mappings)

## Chạy thử / lấy file mod (v1.0.0)

Cách 1 — CI tự build (khuyên dùng, không cần cài Gradle):
1. Vào tab **Actions** của repo → workflow **Build mod** → lần chạy mới nhất.
2. Tải artifact **mcgf-jar** → được `mcgf-ai-companion-1.0.0.jar`.
3. Copy vào `.minecraft/mods/` (profile Fabric 1.21.1 + Fabric API).

Cách 2 — build local:

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-1.0.0.jar -> copy vào .minecraft/mods/ (Fabric 1.21.1)
```

## Sinh tồn cùng companion (Module 4, cần `/gf spawn` trước)

```
/gf attack    # đánh quái hostile gần nhất (16 block)
/gf stop      # dừng đánh, quay về theo bạn
/gf mine      # nhìn vào block (trong 6 block) rồi gọi → pet đào giúp, rớt đồ thật
/gf collect   # hút đồ rơi + xp trong 10 block vào túi bạn
/gf feed      # cho pet ăn thịt trong túi bạn để hồi máu
```

Kết quả đúng:
- `/gf attack` → sói lao vào cắn quái, chat báo tên quái.
- `/gf mine` → block vỡ, rớt đồ như tự đào (bedrock báo không đào được).
- `/gf collect` → đồ bay vào túi, báo số đống + xp.
- Pet mất máu ngoài giao tranh tự hồi 1 máu/5 giây (kèm hạt vui vẻ).
- Hỏi AI cũng được: `@gf làm sao cho bạn ăn?` → AI chỉ `/gf feed`.

## Chat AI với companion (Module 3)

Không key vẫn chơi được (trả lời offline tiếng Việt). Có key Gemini (miễn phí) thì thông minh hẳn:

```
# 1. Lấy key miễn phí tại https://aistudio.google.com/apikey
# 2. Trong game (cần OP/cheats):
/gf apikey AIza...key-cua-ban
@gf creeper là gì?
@gf tui đang ở đâu?
/gf ask làm sao tìm kim cương?
/gf forget        # xóa trí nhớ hội thoại
/gf ai off        # chỉ trả lời offline
/gf ai on         # bật lại AI online
```

Kết quả đúng:
- Gõ `@gf ...` trên chat → cả server thấy `[GF] <trả lời>`.
- Chưa có key → trả lời offline + gợi ý nạp key; có key → Gemini trả lời (biết cả vị trí/máu của bạn).
- Key lưu trong `config/mcgf.json` (file này đã gitignore, không push lên GitHub).

## Test Module 2 (cũ, Singleplayer, cheats ON hoặc OP)

```
/gf help
/gf spawn      # gọi companion sói ra, tự thuần + đặt tên
/gf follow     # đi theo + đánh quái cùng bạn
/gf stay       # ngồi yên
/gf here       # kéo về cạnh bạn
/gf goto 100 64 200   # đến tọa độ và đứng yên
/gf dismiss    # cho biến mất
/gf name Luna  # đổi tên (cả bảng tên trên đầu sói)
/gf version    # phải ra 0.3.0-m3
```

Kết quả đúng:
- Sói hiện ra cạnh bạn, trên đầu có tên (mặc định `GF`), tự đi theo.
- Đánh quái tấn công bạn (AI sói vanilla) — M4 sẽ nâng cấp thêm.
- Lạc quá `teleportDistance` (mặc định 24 block) thì tự teleport về.
- File `config/mcgf.json` có thêm `teleportDistance`:
```json
{
  "companionName": "GF",
  "chatPrefix": "@gf",
  "followDistance": 3.0,
  "teleportDistance": 24.0,
  "replyInVietnamese": true,
  "aiEnabled": true,
  "maxHistory": 8,
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash"
}
```

## Test Module 1 (cũ)

```
/gf hello
/gf say xin chào mọi người
/gf version    # phải ra 1.0.0
```

## Config trong game (Module 5)

```
/gf config       # xem toàn bộ cấu hình (key che ****)
/gf prefix @bot  # đổi prefix chat (cần OP), chat "@bot xin chào" để thử
```

Trí nhớ AI tự lưu vào `config/mcgf_history.json` khi tắt server
và nạp lại khi mở — cả 2 file config đều đã gitignore.
Pet sói là entity thật nên tự lưu theo world, relog không mất.

## Cấu trúc v1.0.0

```
settings.gradle / build.gradle / gradle.properties
.github/workflows/build.yml   — CI: build + up artifact .jar mỗi push main
src/main/java/com/khoand/mcgf/
  MinecraftGFMod.java      — entrypoint + tick (teleport/heal) + nap/luu tri nho
  GfBrain.java             — nghe chat @gf, gọi Gemini async, fallback offline, persist
  GfSurvival.java          — attack/stop/mine/collect/feed/tickHeal
  GfClient.java (src/client) — client: đăng ký renderer sói
  GfEntities.java          — EntityType mcgf:companion
  GfCompanionEntity.java   — entity kế thừa WolfEntity (máu 40, tốc 0.35, dmg 4)
  GfCompanionManager.java  — spawn/follow/stay/goto/here/dismiss
  GfConfig.java            — config json
  GfCommands.java          — /gf (full M1-M5)
src/main/resources/fabric.mod.json
```

## Ghi chú kỹ thuật 1.21.1

- Dùng Yarn `1.21.1+build.3`, Loom `1.7.4`, `options.release = 21`.
- Lệnh dùng `CommandRegistrationCallback` (Fabric API v2) — chuẩn cho 1.21.1.
- `environment: "*"` để chạy cả client + dedicated server.
- Companion kế thừa `WolfEntity`: tái dùng AI follow/tấn công + renderer sói vanilla,
  không cần model/texture riêng → nhẹ, ổn định, hợp cả client lẫn server.
  M4 có thể thay bằng entity custom hoàn toàn nếu bạn muốn ngoại hình riêng.
- Chat AI gọi Gemini qua `java.net.http` (JDK sẵn, không thêm dependency),
  chạy thread riêng + `server.execute()` trả lời → không lag server.
  Key chỉ nằm ở `config/mcgf.json` local (đã gitignore).
- Sinh tồn tôn trọng luật survival: đào bằng `tryBreakBlock` (rớt đồ/mòn tool thật),
  ăn thịt thật từ túi chủ, nhặt đồ bằng `insertStack` vào túi chủ.
- CI (`.github/workflows/build.yml`): mỗi push lên `main` tự build bằng Gradle 8.10.2 + JDK 21
  và đăng `.jar` ở Artifacts — vừa là release vừa kiểm tra compile.

## Checklist test full v1.0.0

```
/gf version   → 1.0.0
/gf config    → hiện cấu hình
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
@gf xin chào (offline) → /gf apikey <key> → @gf creeper là gì? (online)
Tắt/mở server → AI vẫn nhớ hội thoại cũ (/gf forget để xóa)
```
