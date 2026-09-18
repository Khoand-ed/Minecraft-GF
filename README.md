# MCGF AI Companion — Minecraft 1.21.1 Java (Fabric)

AI bạn đồng hành chơi cùng bạn trong Minecraft 1.21.1 Java Edition, dạng **Fabric mod**.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC: `1.21.1` | Loader: `Fabric 0.16.14` | Java: `21` | Yarn: `1.21.1+build.3`

## Lộ trình module (làm từng bước, bạn kiểm tra từng bước)

- [x] **M1 — Core (hiện tại):** scaffold Fabric 1.21.1, config `config/mcgf.json`, lệnh `/gf help|hello|version|name|say`
- [ ] **M2 — Follow/Move:** companion đi theo, `/gf follow|stay|goto`, pathfinding cơ bản
- [ ] **M3 — Chat AI:** Gemini (miễn phí) + fallback offline, prefix `@gf`, tiếng Việt
- [ ] **M4 — Sinh tồn:** đào, nhặt đồ, đánh quái, tự ăn, `/gf mine|attack|collect`
- [ ] **M5 — Polish:** GUI config, persist, build release `.jar`

**Quy tắc:** xong 1 module → bạn test → OK mới làm module tiếp. Chưa push khi bạn chưa duyệt.

## Yêu cầu máy

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (`java -version` phải ra 21)
- Gradle 8.10.2 (dùng `./gradlew` kèm theo, lần đầu cần mạng để tải MC + mappings)

## Chạy thử Module 1

```powershell
# 1. Mở thư mục dự án
cd D:\MCGF

# 2a. Lần đầu: tải wrapper jar (chỉ cần 1 lần, cần mạng)
# Nếu chưa có gradle/wrapper/gradle-wrapper.jar, tải bằng Python:
python -c "import urllib.request; urllib.request.urlretrieve('https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.jar')"

# 2b. Build mod
.\gradlew.bat build

# 3. File jar nằm ở:
# build/libs/mcgf-ai-companion-0.1.0-m1.jar
# Copy vào .minecraft/mods/ (profile Fabric 1.21.1) rồi mở game.
```

Hoặc mở bằng IntelliJ IDEA → Open → chọn `D:\MCGF` → đợi sync → Run `Minecraft Client`.

## Test trong game (Singleplayer, cheats ON hoặc OP)

```
/gf help
/gf hello
/gf version
/gf name Luna
/gf say xin chào mọi người
```

Kết quả đúng:
- Hiện `[MCGF]` / `[GF]` (hoặc tên mới) màu xanh.
- File `config/mcgf.json` tự tạo, ví dụ:
```json
{
  "companionName": "GF",
  "chatPrefix": "@gf",
  "followDistance": 3.0,
  "replyInVietnamese": true,
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash"
}
```

## Cấu trúc Module 1

```
settings.gradle / build.gradle / gradle.properties
src/main/java/com/khoand/mcgf/
  MinecraftGFMod.java  — entrypoint
  GfConfig.java        — config json
  GfCommands.java      — /gf
src/main/resources/fabric.mod.json
```

## Ghi chú kỹ thuật 1.21.1

- Dùng Yarn `1.21.1+build.3`, Loom `1.7.4`, `options.release = 21`.
- Lệnh dùng `CommandRegistrationCallback` (Fabric API v2) — chuẩn cho 1.21.1.
- `environment: "*"` để chạy cả client + dedicated server.

## Bước tiếp theo (M2, chưa làm)

Hỏi bạn trước khi code: companion là **fake-player đi theo** hay **mob pet (kiểu sói/golem)**? Mặc định tôi sẽ làm fake-player đơn giản + `/gf follow|stay`.
