# MCGF AI Companion — Minecraft 1.21.1 Java (Fabric)

AI bạn đồng hành chơi cùng bạn trong Minecraft 1.21.1 Java Edition, dạng **Fabric mod**.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC: `1.21.1` | Loader: `Fabric 0.16.14` | Java: `21` | Yarn: `1.21.1+build.3`

## Lộ trình module (làm từng bước, bạn kiểm tra từng bước)

- [x] **M1 — Core:** scaffold Fabric 1.21.1, config `config/mcgf.json`, lệnh `/gf help|hello|version|name|say`
- [x] **M2 — Follow:** companion đi theo, `/gf spawn|follow|stay|here|goto|dismiss`, tự teleport khi lạc
- [x] **M3 — Chat AI:** chat `@gf <câu hỏi>`, Gemini online + fallback offline, `/gf ask|ai|forget|apikey`
- [x] **M4 — Sinh tồn:** `/gf attack|stop|mine|collect|feed`, tự hồi máu ngoài giao tranh
- [x] **M5 — Polish:** lưu trí nhớ AI, `/gf config|prefix`, CI build ra `.jar`
- [x] **M6 — Việc tự động:** `/gf minevein|chop|farm`, kho pet riêng + `/gf bag|give|deposit`
- [x] **M7 — Dáng người (hiện tại):** companion như player, skin custom (file/URL) + skin mặc định

**Quy tắc:** xong 1 module → bạn test → OK mới làm module tiếp. Chưa push khi bạn chưa duyệt.

## Yêu cầu máy

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (`java -version` phải ra 21)
- Gradle 8.10.2 (dùng `./gradlew` kèm theo, lần đầu cần mạng để tải MC + mappings)

## Chạy thử / lấy file mod (v1.2.0)

Cách 1 — CI tự build (khuyên dùng, không cần cài Gradle):
1. Vào tab **Actions** của repo → workflow **Build mod** → lần chạy mới nhất.
2. Tải artifact **mcgf-jar** → được `mcgf-ai-companion-1.2.0.jar`.
3. Copy vào `.minecraft/mods/` (profile Fabric 1.21.1 + Fabric API).

Cách 2 — build local:

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-1.2.0.jar -> copy vào .minecraft/mods/ (Fabric 1.21.1)
```

## Việc tự động + kho pet (Module 6)

```
/gf minevein  # đào cả vỉa quặng gần nhất (tối đa 32 block, bán kính 24)
              # quặng: coal/iron/copper/gold/redstone/lapis/diamond/emerald
/gf chop      # đốn cả cây gần nhất
/gf farm      # thu lúa chín 8 block quanh pet + tự trồng lại
/gf stop      # hủy việc đang làm
/gf bag       # xem kho pet (9 ô)
/gf give      # lấy hết đồ từ kho pet (túi đầy thì rơi dưới chân)
/gf deposit   # cất kho pet vào rương/thùng gần nhất (8 block quanh pet)
```

Kết quả đúng:
- Pet dịch chuyển tới từng block, vung tay + hạt vỡ, làm 1 block/4 tick.
- Đồ **rơi ra đất đúng survival** rồi pet hút vào kho riêng (không vào túi bạn ngay).
- Xong việc chat báo số block + tổng món trong kho.
- Pet sói cũ (world đã chơi) tự thành dáng người, **giữ nguyên kho đồ**.

## Ngoại hình người + skin (Module 7)

Companion giờ trông như **player** (model tay thường), tên hiện trên đầu như cũ.
`/gf stay` cho pose ngồi sneak cho dễ nhận biết.

Skin theo thứ tự ưu tiên (không cần build lại mod):
1. File `config/mcgf_skin.png` (PNG 64x64, thả vào thư mục config rồi relog).
2. `skinUrl` trong `config/mcgf.json` (link PNG trực tiếp).
3. Skin mặc định trong mod (áo hoodie xanh + quần jean).

```
/gf config    # xem cấu hình, gồm skinFile/skinUrl đang dùng
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
/gf name Luna  # đổi tên (cả bảng tên trên đầu companion)
/gf version    # phải ra 1.2.0
```

Kết quả đúng:
- Companion dáng người hiện ra cạnh bạn, trên đầu có tên (mặc định `GF`), tự đi theo.
- Đánh quái tấn công bạn (tự bảo vệ chủ + theo lệnh `/gf attack`).
- Lạc quá `teleportDistance` (mặc định 24 block) thì tự teleport về.
- File `config/mcgf.json` đầy đủ:
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
  "geminiModel": "gemini-2.0-flash",
  "skinFile": "mcgf_skin.png",
  "skinUrl": ""
}
```

## Test Module 1 (cũ)

```
/gf hello
/gf say xin chào mọi người
/gf version    # phải ra 1.2.0
```

## Config trong game (Module 5)

```
/gf config       # xem toàn bộ cấu hình (key che ****)
/gf prefix @bot  # đổi prefix chat (cần OP), chat "@bot xin chào" để thử
```

Trí nhớ AI tự lưu vào `config/mcgf_history.json` khi tắt server
và nạp lại khi mở — cả 2 file config đều đã gitignore.
Pet là entity thật nên tự lưu theo world (kể cả kho đồ), relog không mất.

## Cấu trúc v1.2.0

```
settings.gradle / build.gradle / gradle.properties
.github/workflows/build.yml   — CI: build + up artifact .jar mỗi push main
src/main/java/com/khoand/mcgf/
  MinecraftGFMod.java      — entrypoint + tick (job/teleport/heal) + nap/luu tri nho
  GfBrain.java             — nghe chat @gf, gọi Gemini async, fallback offline, persist
  GfSurvival.java          — attack/stop/mine/collect/feed/tickHeal
  GfAutoSkills.java        — job minevein/chop/farm + give/deposit/bag
  GfInv.java               — helper nhet do vao moi loai kho
  GfEntities.java          — EntityType mcgf:companion (0.6 x 1.8)
  GfCompanionEntity.java   — TameableEntity dang nguoi + kho rieng (NBT MCGFBag)
  GfCompanionManager.java  — spawn/follow/stay/goto/here/dismiss
  GfConfig.java            — config json
  GfCommands.java          — /gf (full M1-M7)
src/client/java/com/khoand/mcgf/
  GfClient.java            — dang ky renderer + nap skin
  GfCompanionRenderer.java — model nguoi tay thuong
  GfSkins.java             — skin file > URL > mac dinh
src/main/resources/
  fabric.mod.json
  assets/mcgf/textures/entity/companion.png  — skin mac dinh
```

## Ghi chú kỹ thuật 1.21.1

- Dùng Yarn `1.21.1+build.3`, Loom `1.7.4`, `options.release = 21`.
- Lệnh dùng `CommandRegistrationCallback` (Fabric API v2) — chuẩn cho 1.21.1.
- `environment: "*"` để chạy cả client + dedicated server.
- Companion dáng người (`TameableEntity` + goals follow/bảo vệ), renderer model người
  tay thường, skin file > URL > mặc định. Pet sói cũ tự chuyển dáng, giữ kho đồ.
- Việc tự động chạy job 1 block/4 tick, tối đa 32 block: drops đúng survival,
  rương đôi chỉ tính nửa kề bên khi deposit.
- Chat AI gọi Gemini qua `java.net.http` (JDK sẵn, không thêm dependency),
  chạy thread riêng + `server.execute()` trả lời → không lag server.
  Key chỉ nằm ở `config/mcgf.json` local (đã gitignore).
- Sinh tồn tôn trọng luật survival: đào bằng `tryBreakBlock` (rớt đồ/mòn tool thật),
  ăn thịt thật từ túi chủ, nhặt đồ bằng `insertStack` vào túi chủ.
- CI (`.github/workflows/build.yml`): mỗi push lên `main` tự build bằng Gradle 8.10.2 + JDK 21
  và đăng `.jar` ở Artifacts — vừa là release vừa kiểm tra compile.

## Checklist test full v1.2.0

```
/gf version   → 1.2.0
/gf config    → hiện cấu hình (gồm skin)
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
/gf minevein → đợi đào xong → /gf bag → /gf give → /gf deposit (đặt rương cạnh pet)
/gf chop → /gf farm (cần ruộng lúa chín gần pet)
/gf prefix @bot → chat "@bot xin chào"
/gf apikey <key> → @gf creeper là gì? (online)
Thả file PNG 64x64 vào config/mcgf_skin.png → relog → pet đổi skin
Tắt/mở server → AI vẫn nhớ hội thoại cũ, kho pet còn nguyên
```
