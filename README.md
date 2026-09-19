# MCGF AI Companion — Minecraft 1.21.1 Java (Fabric)

AI bạn đồng hành chơi cùng bạn trong Minecraft 1.21.1 Java Edition, dạng **Fabric mod**.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC: `1.21.1` | Loader: `Fabric 0.16.14` | Java: `21` | Yarn: `1.21.1+build.3`

## Lộ trình module (làm từng bước, bạn kiểm tra từng bước)

- [x] **M1 — Core:** scaffold Fabric 1.21.1, config `config/mcgf.json`, lệnh `/gf help|hello|version|name|say`
- [x] **M2 — Follow (hiện tại):** companion sói đi theo, `/gf spawn|follow|stay|here|goto|dismiss`, tự teleport khi lạc
- [ ] **M3 — Chat AI:** Gemini (miễn phí) + fallback offline, prefix `@gf`, tiếng Việt
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
# build/libs/mcgf-ai-companion-0.2.0-m2.jar -> copy vào .minecraft/mods/ (Fabric 1.21.1)
```

## Test trong game (Singleplayer, cheats ON hoặc OP)

```
/gf help
/gf spawn      # gọi companion sói ra, tự thuần + đặt tên
/gf follow     # đi theo + đánh quái cùng bạn
/gf stay       # ngồi yên
/gf here       # kéo về cạnh bạn
/gf goto 100 64 200   # đến tọa độ và đứng yên
/gf dismiss    # cho biến mất
/gf name Luna  # đổi tên (cả bảng tên trên đầu sói)
/gf version    # phải ra 0.2.0-m2
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
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash"
}
```

## Test Module 1 (cũ)

```
/gf hello
/gf say xin chào mọi người
```

## Cấu trúc Module 2

```
settings.gradle / build.gradle / gradle.properties
src/main/java/com/khoand/mcgf/
  MinecraftGFMod.java      — entrypoint + server tick (auto-teleport)
  GfClient.java            — client: đăng ký renderer sói
  GfEntities.java          — EntityType mcgf:companion
  GfCompanionEntity.java   — entity kế thừa WolfEntity (máu 40, tốc 0.35, dmg 4)
  GfCompanionManager.java  — spawn/follow/stay/goto/here/dismiss
  GfConfig.java            — config json (+ teleportDistance)
  GfCommands.java          — /gf (M1 + M2)
src/main/resources/fabric.mod.json
```

## Ghi chú kỹ thuật 1.21.1

- Dùng Yarn `1.21.1+build.3`, Loom `1.7.4`, `options.release = 21`.
- Lệnh dùng `CommandRegistrationCallback` (Fabric API v2) — chuẩn cho 1.21.1.
- `environment: "*"` để chạy cả client + dedicated server.
- Companion kế thừa `WolfEntity`: tái dùng AI follow/tấn công + renderer sói vanilla,
  không cần model/texture riêng → nhẹ, ổn định, hợp cả client lẫn server.
  M4 có thể thay bằng entity custom hoàn toàn nếu bạn muốn ngoại hình riêng.

## Bước tiếp theo (M3, chưa làm)

Chat AI: gõ `@gf <câu hỏi>` trên chat → gọi Gemini, fallback offline khi chưa có key.
Bạn chuẩn bị sẵn Gemini API key (miễn phí) để test M3.
