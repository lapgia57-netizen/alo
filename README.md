# Sổ Thu Chi Cá Nhân – Capacitor (Android + iOS)

Ứng dụng quản lý thu chi cá nhân chạy trên **Android (APK)** và **iPhone (IPA)**.

- Giữ nguyên 100% cấu hình Firebase
- Giao diện tối ưu Oscal Tiger 12 + iPhone
- Đã tối ưu hiệu năng Capacitor

## Cấu trúc dự án

```
money-tp-everywhere/
├── www/index.html              ← Web app gốc (Firebase)
├── package.json
├── capacitor.config.json
├── .gitignore
└── .github/workflows/
    ├── build-android.yml       ← Build APK
    └── build-ios.yml           ← Build iOS (Simulator + hướng dẫn IPA)
```

## 1. Build Android APK (dễ nhất)

1. Tạo repository GitHub → upload toàn bộ thư mục này
2. Vào **Actions** → **Build Android APK** → **Run workflow**
3. Tải artifact `so-thu-chi-android` → file `.apk`
4. Cài trên Android (bật "Nguồn không xác định")

## 2. Build iOS cho iPhone

### A. Bản Simulator (không cần Apple Developer – chạy trên máy Mac / Simulator)

- Workflow **Build iOS IPA** sẽ tự build bản Simulator
- Tải artifact `so-thu-chi-ios-simulator`
- Mở bằng Xcode Simulator trên Mac

### B. Bản IPA thật cài lên iPhone (cần Apple Developer)

Để build IPA cài được trên iPhone thật bạn cần:

1. Tài khoản **Apple Developer** (99 USD/năm)
2. Tạo App ID: `com.moneytp.everywhere`
3. Tạo Certificate + Provisioning Profile (Ad Hoc hoặc Development)
4. Thêm secrets vào GitHub repository:

| Secret name                  | Mô tả                                      |
|-----------------------------|--------------------------------------------|
| `IOS_CERTIFICATE_BASE64`    | File .p12 encode base64                    |
| `IOS_CERTIFICATE_PASSWORD`  | Mật khẩu file .p12                         |
| `APPSTORE_ISSUER_ID`        | Issuer ID từ App Store Connect API         |
| `APPSTORE_KEY_ID`           | Key ID                                     |
| `APPSTORE_PRIVATE_KEY`      | Nội dung file .p8                          |

Sau đó bỏ comment phần "Build IPA (Release)" trong file  
`.github/workflows/build-ios.yml` rồi chạy lại workflow.

### C. Cách nhanh nhất trên máy Mac (khuyến nghị nếu có Mac)

```bash
npm install
npx cap add ios
npx cap sync ios
npx cap open ios
```

Trong Xcode:
- Chọn team Apple Developer của bạn
- Chọn máy iPhone thật → Build & Run
- Hoặc Archive → Distribute App → Ad Hoc / Development để lấy file `.ipa`

## Cài đặt local đầy đủ

```bash
# Cài dependency
npm install

# Thêm 2 nền tảng
npx cap add android
npx cap add ios

# Đồng bộ web assets
npx cap sync

# Mở Android Studio / Xcode
npx cap open android
npx cap open ios
```

## Thông tin app

| Mục              | Giá trị                    |
|------------------|----------------------------|
| Package / Bundle | `com.moneytp.everywhere`   |
| App name         | Sổ Thu Chi                 |
| Min Android      | API 22 (Android 5.1+)      |
| Min iOS          | iOS 13+                    |
| Capacitor        | 6.2.x                      |
| Firebase         | Giữ nguyên config gốc      |

## Lưu ý quan trọng về iOS

- **Không thể** build IPA cài được lên iPhone thật chỉ với GitHub Actions miễn phí nếu không có chứng chỉ Apple.
- Bản Simulator chỉ chạy trên Simulator của Mac.
- Cách thực tế nhất: dùng Mac + Xcode + tài khoản Apple Developer để Archive ra IPA, hoặc dùng TestFlight.

Nếu bạn cần hướng dẫn chi tiết tạo certificate + provisioning profile, bảo mình.
