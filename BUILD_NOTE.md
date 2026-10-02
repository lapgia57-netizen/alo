# Hướng dẫn build Debug APK

## Những gì đã được sửa để project build được

1. Thêm `google-services.json` (bắt buộc vì dùng Firebase plugin)
2. Thêm `res/values/strings.xml`, `themes.xml`
3. Thêm icon launcher (drawable)
4. Sửa `gradle-wrapper.properties` → Gradle 8.4 (tương thích AGP 8.2 + Java 17)
5. Tạo workflow sạch: `.github/workflows/build-debug.yml`

## Cách build trên GitHub Actions

1. Commit toàn bộ thư mục này lên repo
2. Vào tab **Actions** → **Build Debug APK** → **Run workflow**
3. Tải artifact **SoThuChi-Debug-APK**

## Lưu ý

- File `google-services.json` hiện tại là bản tối thiểu để build được.
  Khi chạy thật trên thiết bị, bạn nên thay bằng file chính xác từ Firebase Console (project `tienbothi`, package `com.moneytp.sothuchi`).
