# API Workshop (FE)

## Base
- Base URL: /api/workshop
- Auth header: Authorization: Bearer <accessToken>

## Dashboard
- GET /dashboard/summary
  - Mục đích: Gộp số dư ví, doanh thu, đơn hàng, đánh giá, payout chờ và số thông báo chưa đọc vào một request.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền
- GET /stats?from=yyyy-MM-dd&to=yyyy-MM-dd&groupBy=day|month
  - Mục đích: Lấy thống kê vận hành theo ngày/tháng, gồm số đơn và doanh thu.
  - Lỗi có thể gặp:
    - 401 Chưa đăng nhập
    - 403 Không có quyền

## Profile (ROLE_WORKSHOP)
- GET /profile
  - Mục đích: Xem hồ sơ xưởng.
- PUT /profile
  - Mục đích: Cập nhật thông tin hồ sơ xưởng.
  - Body: { "shopName": "string", "workshopAddress": "string", "productionCapacity": 0, "description": "string" }
- POST /profile/avatar
  - Mục đích: Upload ảnh đại diện cho xưởng.
  - Form-data: file
- POST /profile/logo
  - Mục đích: Upload logo riêng cho xưởng.
  - Form-data: file

## KYC (ROLE_WORKSHOP)
- GET /kyc
  - Mục đích: Xem trạng thái KYC hiện tại.
- PUT /kyc
  - Mục đích: Gửi/cập nhật hồ sơ KYC bằng URL.
  - Body: { "taxCode": "string", "licenseUrl": "string" }
- POST /kyc/upload
  - Mục đích: Upload file giấy phép/hồ sơ pháp lý.
  - Form-data: file

## Portfolio (ROLE_WORKSHOP)
- GET /portfolio
  - Mục đích: Xem danh sách portfolio.
- POST /portfolio
  - Mục đích: Thêm portfolio bằng URL ảnh.
  - Body: { "title": "string", "imageUrl": "string", "description": "string" }
- POST /portfolio/upload
  - Mục đích: Upload ảnh portfolio thay vì nhập URL thủ công.
  - Query/form params: title, description, image
- DELETE /portfolio/{id}
  - Mục đích: Xóa mục portfolio.

## Reputation and Reviews
- GET /reputation
  - Mục đích: Xem điểm uy tín tổng hợp.
- GET /reviews
  - Mục đích: Xem đánh giá của xưởng, có hỗ trợ filter rating/hasImages.
  - Query params: workshopId, rating, hasImages, page, size, sort
- GET /reviews/summary
  - Mục đích: Lấy điểm trung bình và tổng số review của xưởng hiện tại.

## Orders (ROLE_WORKSHOP)
- GET /orders/{orderId}
  - Mục đích: Xem chi tiết đơn hàng của xưởng.
- GET /orders/{orderId}/timeline
  - Mục đích: Xem timeline/checklist tiến độ sản xuất.
- GET /orders?status=&page=&size=&sort=
  - Mục đích: Xem danh sách đơn hàng của xưởng.
- POST /orders/{orderId}/accept
  - Mục đích: Tiếp nhận đơn hàng.
- POST /orders/{orderId}/reject
  - Mục đích: Từ chối đơn hàng.
- POST /orders/{orderId}/status
  - Mục đích: Cập nhật trạng thái sản xuất.
- POST /orders/{orderId}/tracking
  - Mục đích: Cập nhật mã vận đơn.

## Notifications (ROLE_WORKSHOP)
- GET /notifications
  - Mục đích: Danh sách thông báo.
- GET /notifications/unread-count
  - Mục đích: Đếm số thông báo chưa đọc.
- PUT /notifications/{notificationId}/read
  - Mục đích: Đánh dấu đã đọc.

## Messages (ROLE_WORKSHOP)
- GET /messages/threads
  - Mục đích: Danh sách hội thoại của xưởng.
- GET /messages/{threadId}
  - Mục đích: Xem nội dung hội thoại.
- POST /messages/{threadId}
  - Mục đích: Gửi tin nhắn vào hội thoại.
  - Body: { "content": "string" }

## Finance (ROLE_WORKSHOP)
- GET /bank-account
  - Mục đích: Xem tài khoản ngân hàng nhận tiền.
- PUT /bank-account
  - Mục đích: Cập nhật tài khoản ngân hàng.
  - Body: { "bankName": "string", "accountNo": "string", "accountName": "string" }
- GET /payouts
  - Mục đích: Xem danh sách yêu cầu rút tiền.
- GET /payouts/{payoutId}
  - Mục đích: Xem chi tiết một yêu cầu rút tiền.
- POST /payouts/{payoutId}/cancel
  - Mục đích: Hủy một yêu cầu rút tiền đang chờ.
- GET /revenue?from=&to=&groupBy=
  - Mục đích: Xem báo cáo doanh thu của xưởng.

## Notes
- Các endpoint upload dùng multipart/form-data và lưu file qua thư mục `/uploads/**`.
- Các route này là facade dành cho UI workshop; logic lõi vẫn nằm ở các module `user`, `order`, `finance`, `review`, `message`, `notification`.