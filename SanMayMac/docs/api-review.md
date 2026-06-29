# API Review (FE)

## Base
- Base URL: /api/reviews
- Public endpoints: /api/reviews/public/** (không cần đăng nhập)
- Auth header: Authorization: Bearer <accessToken>

> Ghi chú: Workshop UI có thêm facade ở [api-workshop.md](api-workshop.md) để xem reviews/reputation cùng dashboard chung.

## Customer (ROLE_CUSTOMER)
- POST /
  - Mục đích: Tạo đánh giá cho đơn hàng hoàn tất.
  - Body: { "orderId": 1, "productId": 10, "rating": 5, "comment": "string", "imageUrls": ["url1"] }
  - Lỗi có thể gặp:
    - 400 Đơn hàng không tồn tại
    - 400 Sản phẩm không tồn tại (nếu có productId)
    - 409 Đơn hàng chưa hoàn tất
    - 409 Đơn hàng đã được đánh giá
    - 401 Chưa đăng nhập
- PUT /{id}
  - Mục đích: Sửa đánh giá trong vòng 7 ngày.
  - Body: { "rating": 4, "comment": "string", "imageUrls": ["url1"] }
  - Lỗi có thể gặp:
    - 404 Không tìm thấy đánh giá
    - 403 Không có quyền sửa
    - 409 Hết thời hạn sửa đánh giá
    - 409 Đánh giá đã bị xóa
    - 401 Chưa đăng nhập
- GET /me?page=&size=&sort=
  - Mục đích: Lịch sử đánh giá của tôi.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
- GET /me/unreviewed-orders
  - Mục đích: Danh sách đơn hàng đã hoàn tất nhưng chưa đánh giá.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập

## Workshop (ROLE_WORKSHOP)
- GET /workshop?rating=&hasImages=&page=&size=&sort=
  - Mục đích: Xem đánh giá của xưởng, có filter sao/ảnh.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
- POST /{id}/reply
  - Mục đích: Phản hồi đánh giá của khách.
  - Body: { "content": "string" }
  - Lỗi có thể gặp:
    - 403 Không có quyền
    - 404 Không tìm thấy đánh giá
- POST /{id}/report
  - Mục đích: Báo cáo đánh giá nghi ngờ spam/cạnh tranh không lành mạnh.
  - Body: { "reason": "string" }
  - Lỗi có thể gặp:
    - 403 Không có quyền
    - 404 Không tìm thấy đánh giá
    - 409 Đã báo cáo trước đó

## Public/Catalog
- GET /public/workshops/{workshopId}?rating=&hasImages=&page=&size=&sort=
  - Mục đích: Hiển thị đánh giá theo xưởng (public).
- GET /public/products/{productId}?rating=&hasImages=&page=&size=&sort=
  - Mục đích: Hiển thị đánh giá theo sản phẩm mẫu (public).
- GET /public/workshops/{workshopId}/summary
  - Mục đích: Lấy điểm trung bình và tổng số lượt đánh giá của xưởng.
- GET /public/products/{productId}/summary
  - Mục đích: Lấy điểm trung bình và tổng số lượt đánh giá của sản phẩm.

## Admin (ROLE_ADMIN)
- GET /admin?status=&page=&size=&sort=
  - Mục đích: Xem toàn bộ đánh giá (lọc theo status nếu cần).
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
- PUT /admin/{reviewId}/status
  - Mục đích: Ẩn/khôi phục/xóa đánh giá vi phạm.
  - Body: { "status": "ACTIVE|HIDDEN|DELETED" }
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
    - 404 Không tìm thấy đánh giá
- GET /admin/reports?status=
  - Mục đích: Danh sách report đánh giá.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
- PUT /admin/reports/{reportId}/resolve
  - Mục đích: Xử lý report (giữ/ẩn/xóa).
  - Body: { "resolution": "KEEP|HIDE|DELETE", "adminNote": "string" }
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
    - 404 Không tìm thấy report
    - 409 Report đã được xử lý

## Response envelope
- Success: { success: true, message: "...", data: <T>, timestamp: "ISO-8601" }
- Error: { status, error, message, path, fieldErrors, timestamp }

## Pagination
- Query params: page (0-based), size, sort (vd: createdAt,desc)
- Response data là đối tượng Page: { content, totalElements, totalPages, number, size }
