# API Order (FE)

## Base
- Base URL: /api/orders
- Auth header: Authorization: Bearer <accessToken>

> Ghi chú: Workshop UI cũng có facade tại [api-workshop.md](api-workshop.md) với các route `/api/workshop/orders/**` và dashboard/timeline tổng hợp.

## Cart (ROLE_CUSTOMER)
- GET /cart
  - Muc dich: Lay gio hang hien tai, tra ve danh sach item va tong tien tam tinh.
  - Loi co the gap:
    - 401 Chua dang nhap
- POST /cart
  - Muc dich: Them san pham (variant) vao gio hang.
  - Body: { "variantId": 1, "quantity": 2 }
  - Loi co the gap:
    - 400 Variant khong ton tai
    - 401 Chua dang nhap
- PUT /cart/items/{itemId}
  - Muc dich: Cap nhat so luong mot item trong gio.
  - Body: { "quantity": 3 }
  - Loi co the gap:
    - 404 Khong tim thay cart item
    - 401 Chua dang nhap
- DELETE /cart/items/{itemId}
  - Muc dich: Xoa mot item khoi gio hang.
  - Loi co the gap:
    - 404 Khong tim thay cart item
    - 401 Chua dang nhap
- DELETE /cart
  - Muc dich: Lam sach gio hang.
  - Loi co the gap:
    - 401 Chua dang nhap

## Customer Orders (ROLE_CUSTOMER)
- POST /checkout/ready-made
  - Muc dich: Tao don hang tu gio hang (hang mau san).
  - Body: { "addressId": 10, "couponCode": "string" }
  - Loi co the gap:
    - 400 Dia chi khong ton tai
    - 409 Gio hang rong
    - 409 Gio hang chua nhieu xuong
    - 401 Chua dang nhap
- POST /checkout/custom
  - Muc dich: Tao don hang gia cong tu quote da chap nhan.
  - Body: { "quoteId": 5, "addressId": 10 }
  - Response tra ve them `frontDesignUrl` va `backDesignUrl` neu don hang duoc tao tu workflow bidding.
  - Loi co the gap:
    - 400 Quote khong ton tai
    - 409 Quote chua chap nhan
    - 403 Quote khong thuoc ve khach
    - 400 Dia chi khong ton tai
    - 401 Chua dang nhap
- GET /me?status=&page=&size=&sort=
  - Muc dich: Lich su don hang cua toi (co the loc theo status).
  - Loi co the gap:
    - 401 Chua dang nhap
- GET /me/{orderId}
  - Muc dich: Xem chi tiet don hang.
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 403 Khong co quyen xem
    - 401 Chua dang nhap
- POST /me/{orderId}/cancel
  - Muc dich: Huy don hang khi con PENDING.
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Don hang khong the huy
    - 401 Chua dang nhap
- POST /me/{orderId}/confirm-delivery
  - Muc dich: Xac nhan da nhan hang, chuyen trang thai COMPLETED.
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Don hang chua giao
    - 401 Chua dang nhap

## Workshop Orders (ROLE_WORKSHOP)
- GET /workshop?status=&page=&size=&sort=
  - Muc dich: Xem danh sach don hang cua xuong.
  - Loi co the gap:
    - 401 Chua dang nhap
    - 403 Khong co quyen
- POST /workshop/{orderId}/accept
  - Muc dich: Chap nhan don moi (PENDING -> DEPOSITED).
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Don hang khong o trang thai PENDING
    - 403 Khong co quyen
- POST /workshop/{orderId}/reject
  - Muc dich: Tu choi don moi (PENDING -> CANCELLED).
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Don hang khong o trang thai PENDING
    - 403 Khong co quyen
- POST /workshop/{orderId}/status
  - Muc dich: Cap nhat trang thai don hang theo luong.
  - Body: { "status": "DEPOSITED|PRODUCING|SHIPPED" }
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Chuyen trang thai khong hop le
    - 403 Khong co quyen
- POST /workshop/{orderId}/tracking
  - Muc dich: Cap nhat ma van don.
  - Body: { "trackingCode": "string" }
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 403 Khong co quyen

## Admin (ROLE_ADMIN)
- GET /admin?status=&page=&size=&sort=
  - Muc dich: Xem tong don hang tren he thong.
  - Loi co the gap:
    - 401 Chua dang nhap
    - 403 Khong co quyen
- POST /admin/{orderId}/force-cancel
  - Muc dich: Huy don khan cap.
  - Loi co the gap:
    - 404 Don hang khong ton tai
    - 409 Don hang da hoan thanh
    - 403 Khong co quyen
- GET /admin/stats?from=yyyy-MM-dd&to=yyyy-MM-dd&groupBy=day|month
  - Muc dich: Thong ke so don va doanh thu uoc tinh theo ngay/thang.
  - Loi co the gap:
    - 403 Khong co quyen

## Response envelope
- Success: { success: true, message: "...", data: <T>, timestamp: "ISO-8601" }
- Error: { status, error, message, path, fieldErrors, timestamp }

## Order detail notes
- `OrderDetailResponseRecord` co them `frontDesignUrl` va `backDesignUrl` cho don gia cong.

## Pagination
- Query params: page (0-based), size, sort (vd: createdAt,desc)
- Response data la doi tuong Page: { content, totalElements, totalPages, number, size }
