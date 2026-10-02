# Sổ Thu Chi – Phiên bản Java (Android Native)

Chuyển đổi hoàn toàn từ bản web Capacitor (HTML + Firebase JS) sang **Android Native Java**.

## Tính năng chính

- Đăng nhập / Đăng ký bằng Email + Password (Firebase Auth)
- Ghi nhận **Chi tiêu** và **Thu nhập** realtime
- Hiển thị danh sách với **RecyclerView + DiffUtil** (cực nhanh, không flicker)
- Tính tổng Thu / Chi / Số dư realtime
- **Màn hình Biểu đồ phân tích**:
  - PieChart cơ cấu chi tiêu theo danh mục
  - PieChart cơ cấu thu nhập theo danh mục
  - BarChart so sánh Thu vs Chi theo tháng
- Thêm / Sửa / Xóa chứng từ
- Offline persistence của Firebase

## Thuật toán tối ưu tốc độ Load & Hiển thị

### 1. Data Loading (DataRepository)

| Kỹ thuật | Mô tả | Lợi ích |
|----------|-------|---------|
| `ConcurrentHashMap` cache | Giữ toàn bộ data trong memory | Truy cập O(1), không query lại |
| Single `ValueEventListener` | Chỉ 1 listener cho mỗi node | Tránh memory leak + duplicate callback |
| Background parse + sort | `ExecutorService` + `Handler` | UI thread không bị block |
| `keepSynced(true)` | Firebase giữ data disk + memory | Load lần sau gần như tức thì |
| Pre-computed aggregates | Tính tổng + category + tháng sẵn | Biểu đồ mở ra không cần tính lại |

### 2. Hiển thị danh sách (TransactionAdapter)

| Kỹ thuật | Mô tả | Lợi ích |
|----------|-------|---------|
| `ListAdapter` + `DiffUtil` | Chỉ cập nhật item thay đổi | Scroll siêu mượt, không rebind toàn bộ |
| `setHasFixedSize(true)` | RecyclerView biết size cố định | Giảm measure/layout |
| `setItemViewCacheSize(20)` | Cache nhiều ViewHolder | Scroll nhanh khi data nhiều |
| `content-visibility` style | Contain layout | Giảm overdraw |

### 3. Biểu đồ (ChartActivity)

- Dữ liệu đã được aggregate sẵn trong `DataRepository.ChartData`
- Chỉ cần convert Map → Entry list và gọi `setData()`
- Animation Y 700-800ms mượt

## Cấu trúc dự án

```
SoThuChiJava/
├── app/
│   ├── src/main/
│   │   ├── java/com/moneytp/sothuchi/
│   │   │   ├── model/Transaction.java
│   │   │   ├── util/DataRepository.java      ← Thuật toán load nhanh
│   │   │   ├── adapter/TransactionAdapter.java ← DiffUtil
│   │   │   └── ui/
│   │   │       ├── LoginActivity.java
│   │   │       ├── MainActivity.java
│   │   │       ├── AddEditActivity.java
│   │   │       └── ChartActivity.java         ← Biểu đồ phân tích
│   │   ├── res/layout/...
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## Cách build & chạy

1. Mở Android Studio → **Open** thư mục `SoThuChiJava`
2. Thêm file `google-services.json` vào `app/` (lấy từ Firebase Console project `tienbothi`)
3. Sync Gradle
4. Chạy trên emulator hoặc thiết bị thật (minSdk 24)

### Dependency quan trọng

- Firebase BOM 32.7.0
- MPAndroidChart (JitPack)
- Material Components + ViewPager2 + Lifecycle LiveData

## So sánh với bản Web cũ

| Tiêu chí | Bản Web (Capacitor) | Bản Java Native |
|----------|---------------------|-----------------|
| Render list | innerHTML + DocumentFragment | RecyclerView + DiffUtil |
| Load data | JS object + sort mỗi lần | Background thread + ConcurrentHashMap |
| Biểu đồ | Không có | Pie + Bar đầy đủ |
| Offline | Firebase JS | Firebase + keepSynced |
| Hiệu năng | Phụ thuộc WebView | Native GPU + memory managed |

---

**Tác giả chuyển đổi**: Grok (xAI)  
**Nguồn gốc**: alo-main (Capacitor web app)
