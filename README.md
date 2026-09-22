# MCGF AI Companion — Minecraft 1.21.1 Java (Fabric)

AI bạn đồng hành chơi cùng bạn trong Minecraft 1.21.1 Java Edition, dạng **Fabric mod**.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC: `1.21.1` | Loader: `Fabric 0.16.14` | Java: `21` | Yarn: `1.21.1+build.3`

## Lộ trình module (làm từng bước, bạn kiểm tra từng bước)

- [x] **M1 — Core:** scaffold Fabric 1.21.1, config `config/mcgf.json`, lệnh `/gf help|hello|version|name|say`
- [x] **M2 — Follow:** companion sói đi theo, `/gf spawn|follow|stay|here|goto|dismiss`, tự teleport khi lạc
- [x] **M3 — Chat AI (hiện tại):** chat `@gf <câu hỏi>`, Gemini online + fallback offline, `/gf ask|ai|forget|apikey`
- [ ] **M4 — Sinh tồn:** đào, nhặt đồ, đánh quái, tự ăn, `/gf mine|attack|collect`
- [ ] **M5 — Polish:** GUI config, persist, build release `.jar`

**Quy tắc:** xong 1 module → bạn test → OK mới làm module tiếp. Chưa push khi bạn chưa duyệt.

## Yêu cầu máy

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (`java -version` phải ra 21)
- Gradle 8.10.2 (dùng `./gradlew` kèm theo, lần đầu cần mạng để tải MC + mappings)

## Chạy thử Module 2

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-0.3.0-m3.jar -> copy vào .minecraft/mods/ (Fabric 1.21.1)
```

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
```

## Cấu trúc Module 3

```
settings.gradle / build.gradle / gradle.properties
src/main/java/com/khoand/mcgf/
  MinecraftGFMod.java      — entrypoint + server tick (auto-teleport) + GfBrain
  GfBrain.java             — nghe chat @gf, gọi Gemini async, fallback offline
  GfClient.java            — client: đăng ký renderer sói
  GfEntities.java          — EntityType mcgf:companion
  GfCompanionEntity.java   — entity kế thừa WolfEntity (máu 40, tốc 0.35, dmg 4)
  GfCompanionManager.java  — spawn/follow/stay/goto/here/dismiss
  GfConfig.java            — config json (+ aiEnabled, maxHistory, geminiApiKey)
  GfCommands.java          — /gf (M1 + M2 + M3: ask/ai/forget/apikey)
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

## Bước tiếp theo (M4, chưa làm)

Sinh tồn: companion tự đào mỏ, nhặt đồ, đánh quái theo lệnh, tự ăn hồi máu.
Lệnh dự kiến: `/gf mine`, `/gf attack`, `/gf collect`.
