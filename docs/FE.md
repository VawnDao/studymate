# StudyMate: Tài liệu FE (Android · Kotlin + Jetpack Compose)

> Dành cho người A (UI) và B (data + logic). Tài liệu tổng: [../PLAN.md](../PLAN.md) · Backend: [BE.md](BE.md)
> Prototype UI: https://claude.ai/artifact/Vjd8ezbM4Rhvg6e55z2ZYN
> Nguồn sự thật cho API: `api/openapi.yaml`. Khi doc này và file đó lệch nhau, theo file đó.

Mục 🔎: kiểm tra lại phiên bản thư viện ổn định mới nhất trước ngày thi.

---

## 1. Phạm vi FE trong MVP

| Làm | Không làm |
|---|---|
| Thêm tài liệu (PDF, chụp vở, chọn ảnh), màn Đang xử lý, Hub tài liệu | Tài khoản, đăng nhập |
| Tóm tắt (dàn ý + khái niệm), Xem nguồn | Gói Free/Pro, paywall, đếm lượt, quảng cáo |
| Flashcard + lịch ôn SM-2 chạy trên máy | iOS |
| Quiz 4 đáp án + giải thích + nguồn | Link YouTube, ghi âm, xuất Anki |
| Gia sư AI (chat gợi mở, có trích dẫn) | Đồng bộ cloud |
| Hôm nay (thẻ đến hạn), Thư viện | Sơ đồ tư duy (chỉ làm nếu dư giờ) |

**Tiêu chí xong:** trên điện thoại Android thật, chụp vở hoặc chọn PDF → thấy tiến độ 4 bước → Hub → ôn thẻ (lịch cập nhật) → làm quiz → hỏi gia sư có chip nguồn → chạm chip mở đúng đoạn gốc.

## 2. Kiến trúc app

```
UI layer       Composable Screen ── collect ── ViewModel (StateFlow<UiState>)
                    │ onEvent(UiEvent)               │
Domain layer        └──────────────► UseCase ◄───────┘      Sm2Scheduler (Kotlin thuần)
Data layer                         Repository ─┬─ RemoteDataSource (Retrofit) ──► Backend /v1
                                               └─ LocalDataSource (Room DAO)
DI: Hilt · Async: Coroutines + Flow · Nav: Navigation Compose
```

**Quy tắc bắt buộc**
1. Screen chỉ đọc `UiState` và gửi `UiEvent`; không gọi Retrofit/Room trực tiếp.
2. `UiState` là `data class` bất biến; dùng `collectAsStateWithLifecycle()`.
3. Domain không import `android.*` → test bằng JUnit thuần.
4. Repository là nguồn sự thật: pack tải về lưu vào Room, các màn học đọc từ Room (học offline được).
5. Mọi lỗi đi qua `AppResult` (mục 6), mỗi màn có 3 trạng thái: Loading · Error (nút Thử lại) · Content.

## 3. Thư viện (đều Apache-2.0 trừ khi ghi khác) 🔎

| Nhu cầu | Thư viện |
|---|---|
| UI | Compose BOM, Material 3 |
| Điều hướng | Navigation Compose (route có kiểu, `@Serializable`) |
| DI | Hilt (+ `hilt-navigation-compose`) |
| Mạng | Retrofit, OkHttp, logging-interceptor (chỉ build debug) |
| JSON | kotlinx.serialization + converter Retrofit |
| CSDL | Room + KSP |
| Ảnh | Coil |
| Lifecycle | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| Công thức | WebView + **KaTeX** (MIT) đóng gói trong `assets/katex/` |
| Test | JUnit, kotlinx-coroutines-test, Turbine, Compose UI test |

Ghim tất cả trong `gradle/libs.versions.toml`. **Không nâng cấp phiên bản trong ngày thi.**

**Cấu hình:** `minSdk 26`, `targetSdk` mới nhất, `compileSdk` mới nhất, JDK 17. `BuildConfig.BASE_URL`, `BuildConfig.APP_TOKEN` lấy từ `local.properties` (không commit).

## 4. Cấu trúc package

```
android/app/src/main/java/<pkg>/studymate/
├─ StudyMateApp.kt                 # @HiltAndroidApp
├─ MainActivity.kt                 # setContent { StudyMateTheme { AppNavHost() } }
├─ ui/
│  ├─ theme/  Color.kt, Type.kt (Be Vietnam Pro), Shape.kt, Theme.kt
│  ├─ components/  SourceChip, FlipCard, StepRow, ChatBubble, MathText, BottomNav, EmptyState, ErrorState
│  ├─ navigation/  Routes.kt, AppNavHost.kt
│  └─ screens/
│     ├─ today/      TodayScreen.kt, TodayViewModel.kt, TodayUiState.kt
│     ├─ library/    …
│     ├─ add/        AddDocumentSheet.kt, AddDocumentViewModel.kt
│     ├─ processing/ …
│     ├─ hub/        …
│     ├─ summary/    …
│     ├─ cards/      …
│     ├─ quiz/       …
│     ├─ tutor/      …
│     └─ source/     SourceSheet.kt, SourceViewModel.kt
├─ domain/
│  ├─ model/    Document, ProcessingStatus, StudyPack, OutlineSection, Concept, Flashcard, QuizItem, TutorMessage, TutorReply, Citation
│  ├─ usecase/  ImportDocumentUseCase, ObserveProcessingUseCase, GetStudyPackUseCase, GetDueCardsUseCase,
│  │            ReviewCardUseCase, SubmitQuizUseCase, AskTutorUseCase, GetChunkUseCase, DeleteDocumentUseCase
│  └─ srs/      Sm2Scheduler.kt, ReviewRating.kt
├─ data/
│  ├─ remote/   StudyMateApi.kt, dto/*.kt, AuthInterceptor.kt, ErrorMapper.kt
│  ├─ local/    AppDatabase.kt, dao/*.kt, entity/*.kt, Converters.kt
│  ├─ repository/ DocumentRepositoryImpl.kt, StudyRepositoryImpl.kt, TutorRepositoryImpl.kt
│  ├─ fake/     FakeRepositories.kt   # đọc assets/pack_sample.json, dùng khi chưa có backend
│  └─ image/    ImageCompressor.kt    # nén JPEG 1600px
└─ di/          NetworkModule.kt, DatabaseModule.kt, RepositoryModule.kt (bật Fake bằng cờ BuildConfig.USE_FAKE)
```

## 5. Điều hướng

| Route | Màn | Ghi chú |
|---|---|---|
| `Today` | Hôm nay | Tab 1, có BottomNav |
| `Library` | Thư viện | Tab 2, có BottomNav |
| `Processing(docId)` | Đang xử lý | Tự chuyển sang `Hub` khi `ready` (popUpTo để Back không quay lại) |
| `Hub(docId)` | Hub tài liệu | |
| `Summary(docId)` · `Cards(docId)` · `Quiz(docId)` · `Tutor(docId)` | Các chế độ học | Ẩn BottomNav |
| Sheet `AddDocument` | Thêm tài liệu | `ModalBottomSheet`, mở từ nút Thêm |
| Sheet `Source(docId, chunkId)` | Xem nguồn | `ModalBottomSheet`, mở từ mọi `SourceChip` |

**BottomNav** (theo prototype mới): thanh trắng nổi, cách mép 16dp, bo 26dp, 3 ô đều nhau (icon trên, nhãn dưới): Hôm nay · **Thêm** (ô tím nổi) · Thư viện. Tab đang chọn: nền tím nhạt sau icon, chữ tím đậm; `Modifier.semantics { selected = true }`.

## 6. Mạng

### 6.1 Retrofit

```kotlin
interface StudyMateApi {
  @Multipart @POST("v1/documents")
  suspend fun uploadPdf(@Part file: MultipartBody.Part, @Part("title") title: RequestBody): DocumentStatusDto
  @Multipart @POST("v1/documents")
  suspend fun uploadImages(@Part images: List<MultipartBody.Part>, @Part("title") title: RequestBody): DocumentStatusDto
  @GET("v1/documents/{id}") suspend fun status(@Path("id") id: String): DocumentStatusDto
  @GET("v1/documents/{id}/pack") suspend fun pack(@Path("id") id: String): StudyPackDto
  @GET("v1/documents/{id}/chunks/{chunkId}") suspend fun chunk(@Path("id") id: String, @Path("chunkId") c: String): ChunkDto
  @POST("v1/documents/{id}/tutor") suspend fun tutor(@Path("id") id: String, @Body body: TutorRequestDto): TutorReplyDto
  @DELETE("v1/documents/{id}") suspend fun delete(@Path("id") id: String)
}
```

- `AuthInterceptor` thêm header `X-App-Token`.
- Timeout OkHttp: connect 10 s, read 60 s (upload), call 90 s.
- JSON: `Json { ignoreUnknownKeys = true; explicitNulls = false }`.

### 6.2 Payload chính (tóm tắt; đầy đủ ở BE.md mục 5)

```json
// GET /v1/documents/{id}
{ "id": "d_7f3a", "status": "generating", "step": 4, "steps_total": 4, "error": null }

// GET /v1/documents/{id}/pack
{ "document": {...}, "summary": { "tldr": "...", "outline": [...], "concepts": [...] },
  "flashcards": [ { "id": "f01", "front": "...", "back": "...", "source_chunk_ids": ["c003"] } ],
  "quiz": [ { "id": "q01", "question": "...", "options": ["..","..","..",".."], "answer_index": 3,
              "explanation": "...", "source_chunk_ids": ["c004"] } ],
  "chunks_index": { "c003": { "page": 2 } } }

// POST /v1/documents/{id}/tutor
req:  { "messages": [ { "role": "user", "content": "..." } ], "hint_level": 1 }
resp: { "reply": "...", "mode": "hint|check|solution|out_of_scope", "next_question": "...",
        "citations": [ { "chunk_id": "c004", "page": 3, "quote": "..." } ], "in_document": true }
```

### 6.3 Lỗi

```kotlin
sealed interface AppError { data object Network; data object Timeout; data class Server(val code: String) ; data object Unknown }
sealed interface AppResult<out T> { data class Ok<T>(val value: T); data class Err(val error: AppError) }
```

`ErrorMapper` đọc `{"error":{"code","message"}}` và ánh xạ sang câu tiếng Việt:

| Mã | Câu hiển thị |
|---|---|
| `FILE_TOO_LARGE` | Tệp lớn hơn 10 MB. Bạn thử chọn tệp nhỏ hơn nhé. |
| `PDF_TOO_LONG` | Tài liệu dài hơn 20 trang. Hãy tách thành nhiều phần. |
| `UNSUPPORTED_TYPE` | Chỉ hỗ trợ PDF, JPG, PNG. |
| `NOT_READY` | Tài liệu đang được xử lý. |
| `NOT_FOUND` | Không tìm thấy tài liệu. |
| `RATE_LIMITED` | Thao tác hơi nhanh, thử lại sau vài giây. |
| `LLM_UNAVAILABLE` | AI đang bận, thử lại sau ít phút. |
| `UNAUTHORIZED`, `INTERNAL`, khác | Có lỗi xảy ra, thử lại nhé. |
| Network / Timeout | Mất kết nối. Kiểm tra mạng rồi thử lại. |

## 7. Lưu trữ trên máy (Room)

```kotlin
@Entity(tableName = "documents") data class DocumentEntity(
  @PrimaryKey val id: String, val title: String, val subject: String?, val pages: Int,
  val sourceType: String, val status: String, val packJson: String?, val createdAt: Long)

@Entity(tableName = "cards", indices = [Index("docId"), Index("dueAt")]) data class CardEntity(
  @PrimaryKey val id: String, val docId: String, val front: String, val back: String,
  val sourceChunkIds: List<String>, val ease: Double = 2.5, val intervalDays: Int = 0,
  val reps: Int = 0, val lapses: Int = 0, val dueAt: Long)

@Entity(tableName = "quiz_attempts") data class QuizAttemptEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0, val docId: String,
  val score: Int, val total: Int, val wrongIds: List<String>, val createdAt: Long)
```

- Khi tải pack: lưu `packJson`, upsert `cards` (giữ nguyên lịch ôn nếu thẻ đã có), `dueAt = now` cho thẻ mới.
- DAO chính: `observeDueCount(now): Flow<Int>`, `dueCards(docId, now)`, `updateCard()`, `observeDocuments(): Flow<List<…>>`.
- Lịch sử chat gia sư chỉ giữ trong ViewModel (không lưu) ở MVP.

## 8. Lịch ôn SM-2 (`domain/srs/Sm2Scheduler.kt`)

Nút: **Quên (0) · Khó (3) · Nhớ (4) · Dễ (5)**.

```kotlin
fun next(c: CardState, q: Int, now: Long): CardState {
  if (q < 3) return c.copy(reps = 0, lapses = c.lapses + 1, intervalDays = 0, dueAt = now + 10.minutes)
  val reps = c.reps + 1
  var interval = when (reps) { 1 -> 1; 2 -> 3; else -> (c.intervalDays * c.ease).roundToInt() }
  if (q == 5) interval = (interval * 1.3).roundToInt()
  val ease = max(1.3, c.ease + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
  return c.copy(reps = reps, intervalDays = interval, ease = ease, dueAt = now + interval.days)
}
```

Nhãn dưới nút đánh giá tính bằng chính hàm này (vd. "< 10 phút", "1 ngày", "3 ngày", "5 ngày").
**Test bắt buộc:** thẻ mới + Nhớ, Quên, Khó, Nhớ 3 lần liên tiếp, Dễ, ease không xuống dưới 1.3.

## 9. Đặc tả từng màn

| Màn | UiState chính | Hành vi | Xong khi |
|---|---|---|---|
| **Hôm nay** | `dueCount`, `dueDocTitle`, `recentDocs`, `streakDays` | Chưa có tài liệu: thẻ "Thêm tài liệu"; có thẻ đến hạn: thẻ tím "Ôn ngay N thẻ" → `Cards` | Số thẻ đến hạn đúng theo Room |
| **Thêm tài liệu** (sheet) | — | 3 lựa chọn: `OpenDocument("application/pdf")`, `TakePicture`, `PickMultipleVisualMedia(5)`. Nén ảnh JPEG 1600px (chạy trên `Dispatchers.Default`) rồi upload → `Processing` | Upload thành công, có `docId` |
| **Đang xử lý** | `steps[4]` (wait/run/done), `progress`, `error?` | Poll `GET /v1/documents/{id}` mỗi 1 s; ánh xạ status → 4 bước (bảng dưới); `ready` → tải pack, lưu Room, sang Hub; `failed` → ErrorState + Thử lại | Hiển thị đúng tiến độ, không rò coroutine khi rời màn |
| **Hub** | `title`, `meta`, `tldr`, số thẻ/câu | 4 ô: Tóm tắt · Flashcard · Quiz · Gia sư; chip nguồn cho TL;DR | Mở được 4 chế độ |
| **Tóm tắt** | `outline[]` (có `expanded`), `concepts[]` | Mở/đóng từng phần; `SourceChip` ở mỗi phần và khái niệm | Chip mở đúng đoạn |
| **Flashcard** | `card`, `isFlipped`, `index/total`, `ratingLabels`, `done`, `summary` | Chạm thẻ để lật (`graphicsLayer { rotationY; cameraDistance }`, 400 ms); sau khi lật hiện 4 nút; đánh giá → `ReviewCardUseCase` → thẻ kế; hết thẻ → màn tổng kết + lịch ôn | `dueAt` cập nhật trong Room |
| **Quiz** | `question`, `options[]` (state: idle/correct/wrong/dim), `answered`, `score`, `done` | Chọn 1 lần; hiện đúng/sai + giải thích + nguồn; "Câu tiếp" / "Xem kết quả"; lưu `QuizAttemptEntity` | Kết quả lưu Room |
| **Gia sư** | `messages[]`, `isTyping`, `suggestions[]`, `hintLevel` | Gửi câu hỏi; nút "Gợi ý thêm" (hint_level+1), "Xem lời giải" (hint_level=3); tin AI có tag theo `mode` và `SourceChip`; danh sách cuộn xuống tin mới nhất (`reverseLayout = true`) | Hiển thị đúng 4 mode; chip mở đúng đoạn |
| **Xem nguồn** (sheet) | `page`, `text`, `highlight` | `GET /chunks/{chunkId}`; tô vàng câu trích (`quote`) nếu có | Mở từ mọi chip |
| **Thư viện** | `documents[]` | Danh sách, chạm → Hub; giữ lâu → Xoá (`DELETE` + xoá Room) | — |

**Ánh xạ trạng thái → 4 bước hiển thị**

| Backend `status` | Bước UI |
|---|---|
| `uploaded`, `extracting` | 1. Đọc tài liệu |
| `ocr` | 2. Nhận diện chữ & công thức (bỏ qua, đánh dấu Xong ngay nếu là PDF có chữ) |
| `chunking`, `indexing` | 3. Tìm ý chính |
| `generating`, `validating` | 4. Soạn thẻ & câu hỏi |
| `ready` | Tất cả Xong → Hub |

## 10. Design system (theo prototype)

| Token | Giá trị |
|---|---|
| Nền | `#F3F4F8` |
| Bề mặt | `#FFFFFF` |
| Chữ chính / phụ / mờ | `#15161C` / `#4A4E5C` / `#626777` |
| Primary | `#5B47F5` (chữ trắng), tint `#ECE9FF`, chữ trên tint `#4733D6` |
| Ô Flashcard / Quiz / Gia sư / Lưu ý | `#DDF3E6` / `#FFE6D6` / `#DDEBFF` / `#FFF3C4` |
| Đúng / Sai | `#17804F` trên `#DDF3E6` · `#B23434`/`#D64545` trên `#FDE3E3` |
| Font | **Be Vietnam Pro** 400–800 (thiết kế cho tiếng Việt; tải bằng Downloadable Fonts hoặc đóng gói `res/font`) |
| Bo góc | Thẻ lớn 28–32dp, card 20–24dp, chip/nút pill 99dp |
| Chạm | Tối thiểu 48dp |

Tương phản chữ ≥ 4.5:1; mọi nút icon có `contentDescription`; hỗ trợ cỡ chữ hệ thống lớn (không cố định chiều cao text).

### 10.1 Animation (theo prototype)

Nguyên tắc: ngắn (150–450 ms), dùng easing "ra nhanh, dừng êm"; chỉ animate `alpha`, `translation`, `scale`, `rotation` (không animate layout nặng); tắt khi người dùng bật giảm chuyển động (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → dùng `snap()`).

```kotlin
object Motion {
  val Emphasized = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
  val Overshoot  = CubicBezierEasing(0.2f, 1.4f, 0.4f, 1f)
  const val Short = 160; const val Medium = 320; const val Long = 450
}
```

| Chỗ | Hiệu ứng | Compose |
|---|---|---|
| Chuyển màn | Mờ dần + trượt lên 14dp, 380 ms | `NavHost(enterTransition = { fadeIn(tween(380, easing = Emphasized)) + slideInVertically(tween(380)) { it / 60 } }, exitTransition = { fadeOut(tween(160)) })` |
| Nhấn nút / thẻ | Thu nhỏ còn 96% khi nhấn, 160 ms | `Modifier.pressScale()` tự viết: `interactionSource.collectIsPressedAsState()` → `animateFloatAsState(if (pressed) .96f else 1f)` → `graphicsLayer { scaleX = s; scaleY = s }` |
| Bottom sheet (Thêm, Xem nguồn) | Nền mờ hiện 220 ms, sheet trượt từ dưới lên 380 ms | `ModalBottomSheet` mặc định của Material 3 (đã có sẵn) |
| Ô trong Hub, lối tắt Hôm nay | Lần lượt hiện (stagger 60 ms/ô): mờ → rõ, trượt lên 12dp, scale 0.98 → 1 | `LaunchedEffect(Unit)` + `Animatable` cho từng ô với `delay(index * 60L)` |
| Bottom nav | Thanh trượt lên khi vào tab; nền tím sau icon đổi màu 280 ms | `animateColorAsState(tween(280))` cho nền pill; `AnimatedVisibility(slideInVertically + fadeIn)` cho cả thanh |
| Thanh tiến độ (Đang xử lý, Flashcard, Quiz) | Chiều rộng chạy mượt 600 ms | `animateFloatAsState(progress, tween(600, easing = Emphasized))` → `LinearProgressIndicator(progress = { p })` |
| Bước đang chạy (Đang xử lý) | Vòng sáng "thở" quanh số bước, lặp 1,2 s | `rememberInfiniteTransition().animateFloat(...)` vẽ `drawBehind` vòng mờ dần |
| Bước vừa xong | Nảy nhẹ (1 → 1.035 → 1) | `Animatable(1f).animateTo(...)` với `keyframes` khi trạng thái đổi sang Xong |
| Mở/đóng phần dàn ý | Nội dung trượt + mờ | `AnimatedVisibility(expandVertically(tween(300)) + fadeIn())` |
| Lật flashcard | Xoay Y 180°, 500 ms | `animateFloatAsState(if (flipped) 180f else 0f, tween(500, easing = Emphasized))`; `graphicsLayer { rotationY = r; cameraDistance = 12 * density }`; hiện mặt sau khi `r > 90f` (mặt sau xoay thêm 180° để không bị ngược chữ) |
| Sang thẻ / câu hỏi tiếp | Thẻ mới trượt từ phải 48dp, xoay 2°, mờ → rõ, 420 ms | `AnimatedContent(targetState = index, transitionSpec = { (slideInHorizontally { it / 8 } + fadeIn()) togetherWith fadeOut(tween(120)) })` |
| Nút đánh giá (Quên…Dễ) | Hiện lên sau khi lật | `AnimatedVisibility(visible = flipped, enter = fadeIn() + slideInVertically { it / 3 })` |
| Quiz chọn đúng | Ô đúng nảy nhẹ + đổi màu xanh | `animateColorAsState` cho nền/viền + scale keyframes 1 → 1.035 → 1 |
| Quiz chọn sai | Ô bị chọn rung ngang 420 ms | `Animatable(0f)` chạy `keyframes { -7f at 80; 6f at 160; -4f at 240; 3f at 320; 0f at 420 }` → `offset { IntOffset(x.dp.roundToPx(), 0) }` |
| Khung giải thích quiz | Trượt lên + mờ → rõ | `AnimatedVisibility(fadeIn() + slideInVertically { it / 4 })` |
| Màn kết quả (Flashcard xong, Quiz xong) | Thẻ kết quả "pop" từ 86% lên 100% | `scaleIn(initialScale = .86f, animationSpec = tween(550, easing = Overshoot)) + fadeIn()` |
| Tin nhắn gia sư | Bong bóng mới nảy từ góc dưới (trái cho AI, phải cho người dùng) | `Modifier.animateItem()` trong `LazyColumn` + `scaleIn(transformOrigin = TransformOrigin(0f, 1f))` cho item mới |
| AI đang trả lời | 3 chấm nhấp nháy lệch pha 200 ms | `rememberInfiniteTransition` cho từng chấm |
| Chip gợi ý | Hiện dần sau khi AI trả lời | `AnimatedVisibility(fadeIn() + slideInVertically { it / 2 })` |
| Đoạn nguồn được tô | Vệt vàng quét từ trái sang phải 700 ms (trễ 250 ms) | `Animatable(0f)` → vẽ `drawBehind { drawRect(Yellow, size = Size(width * p, height)) }` sau dòng chữ được trích |

**Không làm:** parallax, animation lặp liên tục ngoài 2 chỗ (bước đang chạy, chấm đang gõ), hiệu ứng làm chậm thao tác học (mọi animation không chặn chạm).

## 11. `MathText` (công thức)

**Đã triển khai (MVP):** `MathText` dùng `LatexToUnicode` chuyển công thức đơn giản trong `$...$` sang ký tự Unicode rồi hiển thị bằng `Text` (CO₂, C₆H₁₂O₆, x², →, ×, √, a/b, chữ Hy Lạp). Không cần WebView, không tải gì từ mạng, có unit test.

**Roadmap cho công thức phức tạp (ma trận, tích phân nhiều tầng):**
- Có `$...$` phức tạp → `AndroidView(WebView)` nạp `file:///android_asset/katex/index.html`, truyền chuỗi qua `evaluateJavascript` (đã escape), tự đo chiều cao.
- WebView: tắt `javaScriptCanOpenWindowsAutomatically`, không bật truy cập file ngoài `assets`, không tải URL mạng.

## 12. Làm việc khi chưa có backend

- `assets/pack_sample.json` (copy từ `samples/`), `FakeDocumentRepository` giả lập 4 bước (mỗi bước 900 ms), `FakeTutorRepository` trả kịch bản cố định (giống prototype).
- `BuildConfig.USE_FAKE = true` trong 2,5 giờ đầu; chuyển `false` khi backend sẵn sàng (Checkpoint 1).

## 13. Mạng trong ngày thi

- Điện thoại và laptop cùng hotspot: `BASE_URL=http://<ip-laptop>:8080/`.
- `res/xml/network_security_config.xml`: chỉ cho phép cleartext tới đúng IP đó; còn lại bắt buộc HTTPS.
- Nếu dùng tunnel HTTPS (`cloudflared`), đổi `BASE_URL`, không cần ngoại lệ cleartext.

## 14. Chia việc FE & lịch

| Giờ | A: UI | B: Data + logic |
|---|---|---|
| 0:00–0:30 | Theme, font, NavHost, BottomNav | Gradle catalog, Hilt, DTO từ `openapi.yaml`, `pack_sample.json` vào assets |
| 0:30–2:30 | Hôm nay, Thư viện, sheet Thêm, Đang xử lý, Hub, Tóm tắt (dùng Fake) | Room (entity, DAO), `Sm2Scheduler` + test, màn Flashcard, Retrofit + interceptor + ErrorMapper |
| 2:30–3:00 | ✅ Checkpoint 1: chạy hết luồng với Fake | ✅ Checkpoint 1: gọi được `/healthz` và upload thật bằng app |
| 3:00–4:30 | Quiz, Xem nguồn, `MathText`, Empty/Error states | Repository thật: upload + nén ảnh, poll, lưu pack, Gia sư |
| 4:30–5:30 | ✅ Checkpoint 2: đầu-cuối trên máy thật, đóng băng thư viện | ✅ Checkpoint 2 |
| 5:30–6:30 | Chỉnh UI, animation, chữ tiếng Việt | Sửa lỗi, test SM-2, test parse pack |

## 15. Kiểm thử FE

- Unit: `Sm2SchedulerTest`, `PackParsingTest` (đọc `pack_sample.json`), `ErrorMapperTest`, ViewModel Processing với Turbine (status giả → 4 bước).
- UI test: luồng Flashcard (lật → đánh giá → thẻ kế), Quiz (chọn đáp án → hiện giải thích).
- Thủ công trên máy thật: xoay màn, mất mạng giữa lúc poll, chữ cỡ lớn, nút Back ở mỗi màn.

## 16. Bảo mật & quyền riêng tư phía app

- Không thu thập tên, email, số điện thoại; không có tài khoản.
- Không xin quyền bộ nhớ hay danh bạ; camera dùng `TakePicture` (app camera hệ thống) nên không cần quyền CAMERA.
- `APP_TOKEN` chỉ chống người lạ gọi API, **không phải bí mật**; **API key OpenAI không bao giờ có trong app**.
- Logging interceptor chỉ có ở build debug và không log body upload.
- Ảnh chụp lưu ở `cacheDir`, xoá sau khi upload thành công.
- Xoá tài liệu: gọi `DELETE` và xoá toàn bộ dữ liệu liên quan trong Room.
