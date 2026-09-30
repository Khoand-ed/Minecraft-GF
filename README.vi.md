# MCGF AI Companion

> [English](README.md) | Tiếng Việt | [日本語](README.ja.md) | [Français](README.fr.md)

Mod bạn đồng hành cho **Minecraft 1.21.1 Java Edition (Fabric)**. Gọi ra một người bạn dáng người: đi theo, chiến đấu, đào mỏ, farm, trò chuyện AI và mang kho đồ riêng.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC `1.21.1` | Fabric Loader `0.16.14` | Fabric API `0.102.0+1.21.1` | Java `21`

## Yêu cầu

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (chỉ cần khi build từ source)

## Cài đặt

1. Tải `mcgf-ai-companion-<version>.jar` tại
   https://github.com/Khoand-ed/Minecraft-GF/releases (bản mới nhất).
2. Copy vào `.minecraft/mods/` (profile Fabric 1.21.1 đã có Fabric API).
3. Mở game, chơi world bật cheats (hoặc có OP trên server).

## Chơi nhanh (trong game)

```
/gf spawn    # gọi bạn đồng hành ra
/gf follow   # nó đi theo và bảo vệ bạn
@gf xin chào  # trò chuyện (Việt/Anh/Nhật, không cần key)
/gf lang en  # đổi ngôn ngữ AI: vi | en | ja
```

## Lệnh

### Bạn đồng hành

| Lệnh | Chức năng |
|---|---|
| `/gf spawn` | Gọi ra (thuần của bạn, có tên, relog không mất) |
| `/gf follow` | Đi theo (mặc định) |
| `/gf stay` | Đứng yên (pose ngồi sneak) |
| `/gf here` | Kéo về cạnh bạn |
| `/gf goto <x> <y> <z>` | Đến tọa độ rồi đứng yên |
| `/gf dismiss` | Cho về |
| `/gf name <tên>` | Đổi tên (cả bảng tên trên đầu) |
| `/gf help` | Danh sách lệnh đầy đủ theo nhóm |

Lạc quá `teleportDistance` (mặc định 24 block) thì tự teleport về.

### Chiến đấu & sinh tồn

| Lệnh | Chức năng |
|---|---|
| `/gf attack` | Đánh quái hostile gần nhất (16 block) |
| `/gf stop` | Dừng đánh / hủy việc đang làm |
| `/gf mine` | Nhìn vào block (trong 6 block) → đào giúp, rớt đồ thật |
| `/gf collect` | Hút đồ rơi + xp trong 10 block vào túi bạn |
| `/gf feed` | Cho ăn thịt trong túi bạn để hồi máu |

Ngoài giao tranh tự hồi máu từ từ. Thịt chín hồi nhiều hơn thịt sống.

### Việc tự động (làm từng block, ~1 block / 4 tick)

| Lệnh | Chức năng |
|---|---|
| `/gf minevein` | Đào cả vỉa quặng gần nhất (tối đa 32 block, bán kính 24) |
| `/gf chop` | Đốn cả cây gần nhất |
| `/gf farm` | Thu lúa chín + tự trồng lại |
| `/gf bag` | Xem kho riêng 9 ô của pet |
| `/gf give` | Lấy hết đồ từ kho pet (túi đầy thì rơi dưới chân) |
| `/gf deposit` | Cất kho pet vào rương/thùng gần nhất (8 block) |

Đồ rơi ra đất đúng survival rồi pet hút vào kho riêng. Pet cũ dáng sói tự chuyển thành người, giữ nguyên kho.

### Trang bị (chỉ số thật, lưu theo pet)

| Lệnh | Chức năng |
|---|---|
| `/gf equip` | Mặc vũ khí dame cao nhất + giáp tốt nhất (lấy kho pet trước, rồi túi bạn) |
| `/gf gear` | Xem đồ đang mặc + dame tay + giáp |
| `/gf unequip` | Cởi hết trả về túi bạn |

Dame và giáp tính thật (hệ damage vanilla), hiện trên người: vũ khí tay phải, giáp đúng từng phần.

### Chat AI

Gõ prefix (mặc định `@gf`) ở bất kỳ đâu trên chat:

```
@gf creeper là gì?
@gf tui đang ở đâu?
```

| Lệnh | Chức năng |
|---|---|
| `/gf ask <câu hỏi>` | Hỏi như chat prefix |
| `/gf forget` | Xóa trí nhớ hội thoại |
| `/gf ai on\|off` | Bật/tắt AI online (cần OP) |
| `/gf apikey <key>` | Nạp key Gemini miễn phí (cần OP, key che ****) |

Có key thì gọi Gemini (async, không lag server), biết vị trí/máu/độ đói + hội thoại gần đây. Không key (hoặc lỗi mạng) thì trả lời offline. Ngôn ngữ AI theo `/gf lang` (`vi` | `en` | `ja` | `fr`) cho cả online lẫn offline. Trí nhớ lưu ở `config/mcgf_history.json` khi tắt server, mở lại tự nạp — mỗi player một trí nhớ. Lấy key miễn phí tại https://aistudio.google.com/apikey.

### Linh tinh

| Lệnh | Chức năng |
|---|---|
| `/gf say <text>` | Bắt nhắc lại |
| `/gf hello` | Chào |
| `/gf config` | Xem cấu hình |
| `/gf prefix <p>` | Đổi prefix chat (cần OP) |
| `/gf lang vi\|en\|ja\|fr` | Ngôn ngữ AI (Việt / Anh / Nhật / Pháp) |
| `/gf version` | Xem version |

## Skin

Pet dáng người (tay thường). Thứ tự ưu tiên, không cần build lại mod:

1. `config/mcgf_skin.png` — thả PNG 64x64 vào thư mục config, relog.
2. `skinUrl` trong `config/mcgf.json` — link PNG trực tiếp, dùng khi không có file.
3. Skin mặc định trong mod (áo hoodie xanh + quần jean).

## Config (`config/mcgf.json`)

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

`config/mcgf.json` và `config/mcgf_history.json` đã gitignore — key API không rời khỏi máy bạn.

## Chơi online

- Chạy được trên **server Fabric 1.21.1**: copy mod + Fabric API vào `mods` của server.
- **Mọi người chơi đều phải cài** Fabric + mod (entity custom, client vanilla không vẽ được).
- **Không** chơi được trên Realms hay server Vanilla/Paper/Spigot.
- Lệnh OP: `apikey`, `ai`, `prefix`. Mỗi người một pet + trí nhớ AI riêng.

## Build từ source

Push lên `main` tự kích workflow `Build mod` (Gradle 8.10.2 + JDK 21); file `.jar` nằm ở Artifacts. Hoặc build local:

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-<version>.jar
```

## Checklist test đầy đủ

```
/gf version → version hiện tại
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
/gf minevein → đợi xong → /gf bag → /gf give → /gf deposit (đặt rương cạnh pet)
/gf chop → /gf farm (cần ruộng lúa chín gần pet)
/gf equip → /gf gear (dame/giáp đúng) → vũ khí + giáp hiện trên người
/gf apikey <key> → @gf tui đang ở đâu? (AI biết vị trí bạn)
/gf prefix @bot → chat "@bot xin chào"
/gf lang ja → @gf こんにちは (trả lời tiếng Nhật)
/gf config → hiện cấu hình gồm skin
Thả PNG 64x64 vào config/mcgf_skin.png → relog → pet đổi skin
Tắt/mở server → trí nhớ AI + kho pet còn nguyên
```

## Giấy phép

MIT — xem `LICENSE`.
