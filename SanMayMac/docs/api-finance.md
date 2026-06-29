# API Finance (FE)

## Base
- Base URL: /api/finance
- Auth header: Authorization: Bearer <accessToken>

> Ghi chú: Workshop UI có facade tổng hợp ở [api-workshop.md](api-workshop.md) cho bank account, payouts và dashboard.

## Customer Endpoints (ROLE_CUSTOMER)

### Wallet & Transactions
- GET /wallet
  - Muc dich: Lay thong tin vi (so du khong, so du dang cho).
  - Response: { "userId": 1, "availableBalance": 1000000, "pendingBalance": 500000, "aiTokenBalance": 50 }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 404 Vi khong ton tai

- GET /transactions
  - Muc dich: Lay lich su giao dich cua khach hang (loc theo loai hoac trang thai).
  - Query params:
    - type (optional): ORDER_PAYMENT, AI_TOKEN, REFUND, etc.
    - status (optional): SUCCESS, PENDING, FAILED
  - Response: [{ "id": 1, "orderId": 5, "amount": 500000, "type": "ORDER_PAYMENT", "direction": "OUT", "status": "SUCCESS", "description": "Order payment", "createdAt": "2024-01-01T10:00:00Z" }]
  - Loi co the gap:
    - 401 Chua dang nhap

### Payments
- POST /orders/{orderId}/pay
  - Muc dich: Thanh toan don hang.
  - Query params:
    - paymentMethod (optional): WALLET, CREDIT_CARD, BANK_TRANSFER, etc.
    - transactionCode (optional): Ma giao dich tu ben thu 3
    - amount (optional): So tien thanh toan (neu khong, dung tong tien don)
  - Response: { "id": 1, "orderId": 5, "amount": 500000, "type": "ORDER_PAYMENT", "direction": "OUT", "status": "SUCCESS", "description": "Order payment", "createdAt": "2024-01-01T10:00:00Z" }
  - Loi co the gap:
    - 400 Don hang khong ton tai
    - 403 Khong co quyen thanh toan don hang nay
    - 409 Don hang da huy
    - 409 So tien thanh toan khong hop le
    - 401 Chua dang nhap

### AI Tokens
- POST /ai-tokens/purchase
  - Muc dich: Mua AI token.
  - Body: { "tokenCount": 100, "amount": 100000 }
  - Response: { "id": 2, "amount": 100000, "type": "AI_TOKEN", "direction": "OUT", "status": "SUCCESS", "description": "AI token purchase", "createdAt": "2024-01-01T11:00:00Z" }
  - Loi co the gap:
    - 400 So token hoac so tien khong hop le
    - 401 Chua dang nhap

---

## Workshop Endpoints (ROLE_WORKSHOP)

### Wallet & Bank Account
- GET /wallet
  - Muc dich: Lay thong tin vi cua xuong (so du khong va so du dang cho).
  - Response: { "userId": 2, "availableBalance": 5000000, "pendingBalance": 2000000, "aiTokenBalance": 0 }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 404 Vi khong ton tai

- GET /transactions
  - Muc dich: Lay lich su giao dich cua xuong.
  - Query params:
    - type (optional): ESCROW_HOLD, ESCROW_RELEASE, PAYOUT, REFUND, COMMISSION_FEE, etc.
    - status (optional): SUCCESS, PENDING, FAILED
  - Response: [{ "id": 3, "orderId": 5, "amount": 500000, "type": "ESCROW_HOLD", "direction": "IN", "status": "PENDING", "description": "Escrow hold", "createdAt": "2024-01-01T10:00:00Z" }]
  - Loi co the gap:
    - 401 Chua dang nhap

- PUT /bank-account
  - Muc dich: Them hoac cap nhat tai khoan ngan hang.
  - Body: { "bankName": "Vietcombank", "accountNo": "1234567890", "accountName": "Shop Name" }
  - Response: { "id": 1, "bankName": "Vietcombank", "accountNo": "1234567890", "accountName": "Shop Name", "isVerified": false }
  - Loi co the gap:
    - 400 Thong tin tai khoan khong hop le
    - 401 Chua dang nhap

- GET /bank-account
  - Muc dich: Xem tai khoan ngan hang da dang ky.
  - Response: { "id": 1, "bankName": "Vietcombank", "accountNo": "1234567890", "accountName": "Shop Name", "isVerified": true }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 404 Chua dang ky tai khoan ngan hang

### Payouts
- POST /payouts
  - Muc dich: Yeu cau rut tien (phuong nhan: tai khoan ngan hang).
  - Body: { "amount": 1000000 }
  - Response: { "id": 1, "workshopId": 2, "amount": 1000000, "status": "PENDING", "adminNote": null, "createdAt": "2024-01-01T12:00:00Z", "approvedAt": null }
  - Loi co the gap:
    - 400 So tien rut khong hop le
    - 401 Chua dang nhap
    - 404 Chua dang ky tai khoan ngan hang
    - 409 Tai khoan ngan hang chua xac minh
    - 409 So du khong du

- GET /payouts
  - Muc dich: Xem danh sach yeu cau rut tien cua xuong.
  - Response: [{ "id": 1, "workshopId": 2, "amount": 1000000, "status": "PENDING", "adminNote": null, "createdAt": "2024-01-01T12:00:00Z", "approvedAt": null }]
  - Loi co the gap:
    - 401 Chua dang nhap

### Revenue & Reports
- GET /revenue
  - Muc dich: Lay bao cao doanh thu cua xuong.
  - Query params:
    - from (optional): Ngay bat dau (YYYY-MM-DD)
    - to (optional): Ngay ket thuc (YYYY-MM-DD)
    - groupBy (optional): day (mac dinh) hoac month
  - Response: { "groupBy": "day", "items": [{ "date": "2024-01-01", "revenue": 5000000 }, { "date": "2024-01-02", "revenue": 3000000 }] }
  - Loi co the gap:
    - 400 Khoang thoi gian khong hop le
    - 401 Chua dang nhap

---

## Admin Endpoints (ROLE_ADMIN)

### Payout Management
- GET /admin/payouts
  - Muc dich: Xem tat ca yeu cau rut tien (co the loc theo trang thai).
  - Query params:
    - status (optional): PENDING, APPROVED, REJECTED, COMPLETED, FAILED
  - Response: [{ "id": 1, "workshopId": 2, "amount": 1000000, "status": "PENDING", "adminNote": null, "createdAt": "2024-01-01T12:00:00Z", "approvedAt": null }]
  - Loi co the gap:
    - 401 Chua dang nhap
    - 403 Khong co quyen

- POST /admin/payouts/{payoutId}/approve
  - Muc dich: Chap nhan yeu cau rut tien.
  - Body: { "adminNote": "Approved" }
  - Response: { "id": 1, "workshopId": 2, "amount": 1000000, "status": "APPROVED", "adminNote": "Approved", "createdAt": "2024-01-01T12:00:00Z", "approvedAt": "2024-01-01T13:00:00Z" }
  - Loi co the gap:
    - 404 Yeu cau rut khong ton tai
    - 409 Yeu cau rut khong o trang thai PENDING
    - 401 Chua dang nhap
    - 403 Khong co quyen

- POST /admin/payouts/{payoutId}/reject
  - Muc dich: Tu choi yeu cau rut tien va hoan tien cho vi xuong.
  - Body: { "adminNote": "Insufficient documents" }
  - Response: { "id": 1, "workshopId": 2, "amount": 1000000, "status": "REJECTED", "adminNote": "Insufficient documents", "createdAt": "2024-01-01T12:00:00Z", "approvedAt": "2024-01-01T13:00:00Z" }
  - Loi co the gap:
    - 404 Yeu cau rut khong ton tai
    - 409 Yeu cau rut khong o trang thai PENDING
    - 401 Chua dang nhap
    - 403 Khong co quyen

### Cashflow & Commission
- GET /admin/cashflow
  - Muc dich: Xem tong quan dong tien he thong.
  - Query params:
    - from (optional): Ngay bat dau (YYYY-MM-DD)
    - to (optional): Ngay ket thuc (YYYY-MM-DD)
  - Response: { "escrowBalance": 10000000, "workshopAvailableBalance": 20000000, "platformRevenue": 500000 }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 403 Khong co quyen

- GET /admin/commission
  - Muc dich: Lay cau hinh ti le hoa hong hien tai.
  - Response: { "id": 1, "commissionRate": 0.05 }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 403 Khong co quyen

- PUT /admin/commission
  - Muc dich: Cap nhat ti le hoa hong (5% = 0.05).
  - Body: { "commissionRate": 0.05 }
  - Response: { "id": 1, "commissionRate": 0.05 }
  - Loi co the gap:
    - 400 Ti le hoa hong khong hop le
    - 401 Chua dang nhap
    - 403 Khong co quyen

---

## Notes

### Transaction Types
- ORDER_PAYMENT: Khach hang thanh toan don hang
- ESCROW_HOLD: Tien thanh toan cua khach hang duoc giu lai khi don hang da thanh toan
- ESCROW_RELEASE: Tien ho tro cho xuong khi don hang hoan thanh
- AI_TOKEN: Mua AI token
- REFUND: Hoan tien cho khach hang
- PAYOUT: Rut tien cua xuong
- COMMISSION_FEE: Hoa hong cua platform

### Transaction Directions
- IN: Tien vao (nhan tien)
- OUT: Tien ra (tra tien)

### Payout Statuses
- PENDING: Dang cho duyet
- APPROVED: Da duyet
- REJECTED: Bi tu choi
- COMPLETED: Hoan thanh
- FAILED: That bai

### Commission Calculation
- Commission = Order Amount × Commission Rate
- Example: Neu don hang 1,000,000 VND va ti le 5% (0.05), tien hoa hong = 50,000 VND
- Xuong se nhan: 1,000,000 - 50,000 = 950,000 VND
