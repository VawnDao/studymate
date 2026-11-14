# StudyMate

App ôn tập từ tài liệu của chính học sinh: tải PDF hoặc chụp vở → AI tóm tắt → flashcard lặp lại ngắt quãng, quiz bám sát tài liệu, gia sư gợi mở. Mọi nội dung đều trích dẫn trang nguồn.

| Thư mục | Nội dung |
|---|---|
| [`android/`](android/) | App Android (Kotlin + Jetpack Compose, Clean Architecture). Xem [android/README.md](android/README.md) |
| [`api/openapi.yaml`](api/openapi.yaml) | Hợp đồng API giữa app và backend |
| [`samples/`](samples/) | Dữ liệu mẫu (bộ ôn tập, đoạn nguồn) dùng cho test và API giả |
| [`docs/FE.md`](docs/FE.md), [`docs/BE.md`](docs/BE.md) | Tài liệu kỹ thuật cho nhóm app và nhóm backend (Go) |
| [`PLAN.md`](PLAN.md) | Kế hoạch triển khai MVP |

Chạy nhanh app (không cần backend):

```bash
cd android && ./gradlew installDebug
```
