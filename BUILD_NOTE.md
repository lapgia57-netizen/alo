# Hướng dẫn build APK – Sổ Thu Chi (Native Android Java)

## Yêu cầu
- JDK 17+
- Android SDK (API 34)
- `google-services.json` đúng project Firebase `tienbothi`, package `com.moneytp.sothuchi`

## Build local

```bash
# Cấp quyền
chmod +x gradlew

# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

APK nằm tại:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## GitHub Actions

File: `.github/workflows/build.yml`

- Push / PR vào `main` hoặc `master` → build **Debug APK**
- `workflow_dispatch` hoặc push `main` → thêm job **Release APK**

Artifact tải về từ tab **Actions** → run tương ứng → **SoThuChi-Debug-APK** / **SoThuChi-Release-APK**

## Tối ưu đã áp dụng
1. **DataRepository**: cache ConcurrentHashMap + LiveData + parse trên background thread
2. Firebase **disk persistence** + `keepSynced`
3. Pre-computed balance & chart aggregates (không duyệt list trên UI thread)
4. **DiffUtil** trong RecyclerView adapter
5. Material Design light theme (dễ đọc số liệu)

## Lưu ý Firebase
- Thay `app/google-services.json` bằng file thật từ Firebase Console
- Bật Email/Password Authentication
- Realtime Database rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    }
  }
}
```
