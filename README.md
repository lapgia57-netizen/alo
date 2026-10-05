# Sổ Thu Chi – Android Native (Java)

Ứng dụng quản lý thu chi cá nhân viết bằng **Java**, sử dụng **Firebase Auth + Realtime Database**.

## Tính năng

- Đăng nhập / Đăng ký bằng Email + Password (Firebase Auth)
- Ghi nhận **Chi tiêu** và **Thu nhập**
- Thêm / Sửa / Xóa giao dịch (realtime)
- Tính tổng thu, tổng chi, số dư
- **Biểu đồ**: Pie (danh mục) + Bar (theo tháng)
- Giao diện **Material Design light theme** tối ưu hiển thị dữ liệu

## Tối ưu data & UI

| Kỹ thuật | Mô tả |
|----------|--------|
| DataRepository singleton | Cache ConcurrentHashMap + LiveData |
| Background parse/sort | Không block UI thread |
| Pre-computed aggregates | Balance + chart data sẵn sàng |
| Firebase persistence | Offline cache + keepSynced |
| DiffUtil RecyclerView | Chỉ rebind item thay đổi |

## Cấu trúc Firebase

```
users/{uid}/chi-tieu/{pushId}
users/{uid}/thu-nhap/{pushId}
  danh_muc, mo_ta, ngay, so_tien, ghi_chu, thoi_gian
```

## Build APK

### Local

```bash
chmod +x gradlew
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions

Workflow: `.github/workflows/build.yml`  
Artifact: **SoThuChi-Debug-APK** / **SoThuChi-Release-APK**

Xem chi tiết trong `BUILD_NOTE.md`.

## Package

`com.moneytp.sothuchi`
