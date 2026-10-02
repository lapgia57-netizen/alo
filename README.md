# Sổ Thu Chi – Android Native (Java)

Ứng dụng quản lý thu chi cá nhân viết bằng **Java**, sử dụng **Firebase Auth + Realtime Database**.

## Tính năng

- Đăng nhập / Đăng ký bằng Email + Password (Firebase Auth)
- Ghi nhận **Chi tiêu** và **Thu nhập**
- Thêm / Sửa / Xóa giao dịch (realtime)
- Tính tổng thu, tổng chi, số dư
- **Biểu đồ**:
  - Pie chart chi tiêu theo danh mục
  - Pie chart thu nhập theo danh mục
  - Bar chart thu/chi theo tháng
- Giao diện **màu sáng** (light theme) tối ưu hiển thị dữ liệu

## Cấu trúc dữ liệu Firebase (giữ nguyên từ app gốc)

```
users/
  {uid}/
    chi-tieu/
      {pushId}/
        danh_muc: string
        mo_ta: string
        ngay: "YYYY-MM-DD"
        so_tien: number
        ghi_chu: string | null
        thoi_gian: number (timestamp)
    thu-nhap/
      {pushId}/
        ... (cùng cấu trúc)
```

## Cách build APK

### 1. Chuẩn bị Firebase

1. Vào [Firebase Console](https://console.firebase.google.com/) → project **tienbothi**
2. Thêm app **Android** với package name: `com.moneytp.sothuchi`
3. Tải file `google-services.json` thật và **thay thế** file trong thư mục `app/`
4. Bật **Email/Password** Authentication
5. Realtime Database rules nên cho phép user chỉ đọc/ghi data của chính mình:

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

### 2. Mở project bằng Android Studio

1. Mở Android Studio → **Open** → chọn thư mục `SoThuChiJava`
2. Chờ Gradle sync xong
3. Kết nối máy Android hoặc dùng Emulator
4. Bấm **Run** (▶) để chạy

### 3. Build APK Release

```bash
cd SoThuChiJava
./gradlew assembleRelease
```

File APK nằm tại:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

(Ký APK bằng keystore của bạn trước khi phân phối.)

## Thư viện sử dụng

- Firebase Auth + Realtime Database
- Material Components 3
- MPAndroidChart (biểu đồ)
- AndroidX (AppCompat, RecyclerView, ViewPager2, …)

## Package

`com.moneytp.sothuchi`

## Ghi chú

- Code giữ nguyên 100% logic đọc/ghi Firebase của app Capacitor gốc.
- Giao diện chuyển sang light theme để dữ liệu dễ đọc hơn.
- Biểu đồ tự cập nhật realtime khi có thay đổi dữ liệu.
