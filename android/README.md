# StudyMate – Android (Kotlin + Jetpack Compose)

App ôn tập từ tài liệu của học sinh: tải PDF / chụp vở → AI tóm tắt → flashcard lặp lại ngắt quãng, quiz, gia sư gợi mở. Mọi nội dung đều có chip **Nguồn · tr. N** mở đúng đoạn gốc.

Tài liệu liên quan: [../docs/FE.md](../docs/FE.md) · [../docs/BE.md](../docs/BE.md) · Hợp đồng API: [../api/openapi.yaml](../api/openapi.yaml)

## Chạy app

Yêu cầu: JDK 17, Android SDK 35, Android Studio bản ổn định (hoặc dòng lệnh).

```bash
./gradlew assembleDebug        # build APK debug
./gradlew installDebug         # cài lên emulator/điện thoại đang kết nối
./gradlew test                 # 24 unit test của 3 module
```

Cấu hình trong `local.properties` (không commit):

| Khoá | Mặc định | Ý nghĩa |
|---|---|---|
| `studymate.useFakeApi` | `true` | `true`: backend giả chạy ngay trong app, không cần server. `false`: gọi backend Go thật |
| `studymate.baseUrl` | `http://10.0.2.2:8080/` | Emulator gọi máy host qua `10.0.2.2`. Điện thoại thật: `http://<IP-laptop>:8080/` (cùng hotspot) |
| `studymate.appToken` | `dev-token` | Header `X-App-Token`, phải trùng `APP_TOKEN` của backend |

Đổi cấu hình xong thì build lại (`./gradlew installDebug`). Bản debug cho phép HTTP; bản release chỉ HTTPS.

## Kiến trúc (Clean Architecture, 3 module)

```
:app     UI Compose + ViewModel (MVVM, luồng dữ liệu một chiều) + Hilt
  └─ phụ thuộc → :data, :domain
:data    Retrofit + OkHttp, Room, đọc/nén file, API giả, binding Hilt
  └─ phụ thuộc → :domain
:domain  Kotlin thuần: model, interface repository, use case, SM-2 (không có Android)
```

| Lớp | Nội dung chính |
|---|---|
| `domain` | `AppResult`/`DomainError` (không ném exception ra ngoài), `Sm2Scheduler`, 13 use case (`ImportDocument`, `ObserveProcessing`, `GetReviewSession`, `ReviewCard`, `SubmitQuiz`, `AskTutor`…) |
| `data/remote` | `StudyMateApi` (Retrofit), DTO snake_case, `apiCall {}` chuyển HTTP/IO/timeout/JSON lỗi → `DomainError`, `AuthInterceptor` |
| `data/local` | Room: `documents` (lưu JSON bộ ôn tập để học offline), `cards` (lịch ôn SM-2), `quiz_attempts`; xoá tài liệu thì xoá theo khoá ngoại CASCADE |
| `data/file` | Kiểm tra PDF (magic bytes, ≤ 10 MB, ≤ 20 trang); nén ảnh về JPEG 1600 px, xoay theo EXIF (backend không phải xử lý HEIC) |
| `data/fake` | `FakeStudyMateApi`: mô phỏng đủ pipeline (7 trạng thái, ~0,9 s/bước), bộ ôn tập mẫu, gia sư theo kịch bản, cùng DTO và mã lỗi với API thật |
| `app/ui` | `theme` (màu, Be Vietnam Pro, `Motion`), `components`, `navigation` (route kiểu an toàn), `feature/*` (mỗi màn: `Route` có state + `Screen` không state + `ViewModel`) |

Quy ước:
- Screen chỉ đọc `StateFlow<UiState>` và gửi `Event`; sự kiện một lần (điều hướng, snackbar) qua `Channel` + `ObserveAsEvents`.
- Mọi chữ hiển thị nằm trong `res/values/strings.xml`.
- Animation theo [FE.md mục 10.1](../docs/FE.md); Compose tự tắt animation khi máy bật giảm chuyển động.
- Công thức `$...$` hiển thị qua `LatexToUnicode` (CO₂, x², →, √). WebView + KaTeX để dành cho công thức phức tạp.

## Ghép với backend Go

**Backend đang triển khai:** `https://studymate-api.nguyenquangminh623.workers.dev` (tài liệu: `/llms.txt`, `/docs/api.md`, `/openapi.yaml`). Hợp đồng của BE khớp với `api/openapi.yaml` của app; BE thêm mã `NOT_IMPLEMENTED` (501) cho endpoint chưa làm, app đã xử lý.

```properties
# local.properties
studymate.baseUrl=https://studymate-api.nguyenquangminh623.workers.dev/
studymate.appToken=<token do nhóm BE cung cấp, không commit>
studymate.useFakeApi=false
```

Chạy local thay vì Worker:

1. Backend implement đúng [`api/openapi.yaml`](../api/openapi.yaml). Dữ liệu mẫu để test: [`samples/pack_sample.json`](../samples/pack_sample.json), [`samples/chunks_sample.json`](../samples/chunks_sample.json).
2. Chạy backend trên laptop (`:8080`), đặt `APP_TOKEN` trùng `studymate.appToken`.
3. Trong `local.properties`: `studymate.useFakeApi=false`, `studymate.baseUrl=...` → `./gradlew installDebug`.
4. Kiểm nhanh bằng `curl`:

```bash
curl -H "X-App-Token: dev-token" -F "file=@quang-hop.pdf" -F "title=Quang hợp" http://localhost:8080/v1/documents
```

**Những điểm app dựa vào (backend phải giữ đúng):**

| Điểm | Chi tiết |
|---|---|
| Field multipart | `file` (1 PDF) hoặc `images` (1–5 JPEG, thứ tự trang), cộng `title` |
| `status` | chữ thường: `uploaded, extracting, ocr, chunking, indexing, generating, validating, ready, failed` |
| Khi `ready` | app gọi ngay `GET /pack` rồi mới mở Hub |
| id thẻ / câu | ổn định trong tài liệu (`f01`, `q01`…). App ghép `<documentId>::<cardId>` để giữ lịch ôn khi tải lại |
| Quiz | đúng 4 `options`, `answer_index` 0–3; câu sai cấu trúc bị app bỏ qua |
| `chunks_index` | phải có trang cho mọi `chunk_id` được tham chiếu, nếu không chip chỉ hiện "Nguồn" |
| Tutor | `citations[].quote` là chuỗi con của đoạn gốc thì app tô vàng đúng câu đó |
| Lỗi | luôn `{"error":{"code","message"}}`; app hiển thị câu tiếng Việt theo `code` |
| `DELETE` | trả `204`; nếu `404` app vẫn xoá dữ liệu trên máy |

## Kiểm thử

| Module | Test |
|---|---|
| `domain` | SM-2 (7 test), use case: giới hạn ảnh, poll trạng thái + đồng bộ pack, bỏ qua lỗi lẻ, chế độ luyện tập, chấm quiz, cắt lịch sử gia sư |
| `data` | Parse `pack_sample.json`, bỏ quiz sai cấu trúc, map lỗi API/HTTP/timeout, header token, JSON snake_case (MockWebServer) |
| `app` | `LatexToUnicode` |

Đã chạy thử trên emulator (Android 15) ở chế độ API giả: tải PDF → xử lý → Hub → ôn 6 thẻ → quiz → gia sư (gợi ý, lời giải, ngoài tài liệu) → xem nguồn → Thư viện.

## Giấy phép bên thứ ba

Font Be Vietnam Pro: SIL OFL 1.1 (`app/src/main/assets/licenses/BeVietnamPro-OFL.txt`). Thư viện AndroidX, Hilt/Dagger, Retrofit, OkHttp, kotlinx: Apache-2.0.
