# API Bidding (FE)

## Base
- Base URL: /api/bidding
- Auth header: Authorization: Bearer <accessToken>

## Customer (ROLE_CUSTOMER)
- POST /designs
  - Muc dich: Luu 1 ban thiet ke vao thu vien thiet ke cua customer.
  - Content-Type: multipart/form-data
  - Parts: frontDesign, backDesign, name (optional)
  - Response: { "id": 1, "name": "Thiet ke chua dat ten", "frontDesignUrl": "string", "backDesignUrl": "string", "createdAt": "ISO-8601" }
- GET /designs/me
  - Muc dich: Lay danh sach thiet ke da luu cua customer, sap xep moi nhat truoc.
- POST /designs/upload
  - Muc dich: Upload 2 anh canvas sau khi FE render front/back design.
  - Content-Type: multipart/form-data
  - Parts: frontDesign, backDesign
  - Response: { "frontDesignUrl": "string", "backDesignUrl": "string" }
- POST /posts
  - Muc dich: Dang bai tim xuong.
  - Body: { "title": "string", "description": "string", "aiImageUrl": "string", "frontDesignUrl": "string", "backDesignUrl": "string", "attachments": [{"fileUrl": "string", "fileType": "PDF"}] }
  - Loi co the gap:
    - 401 Chua dang nhap
    - 400 Du lieu khong hop le
- GET /posts/me?page=&size=&sort=
  - Muc dich: Danh sach bai dang cua toi.
  - Loi co the gap:
    - 401 Chua dang nhap
- PUT /posts/{postId}
  - Muc dich: Cap nhat bai dang (chi khi OPEN va chua co bao gia ACCEPTED).
  - Body: { "title": "string", "description": "string", "aiImageUrl": "string", "frontDesignUrl": "string", "backDesignUrl": "string", "attachments": [{"fileUrl": "string", "fileType": "PDF"}] }
  - Loi co the gap:
    - 404 Post khong ton tai
    - 409 Post khong the cap nhat
- DELETE /posts/{postId}
  - Muc dich: Xoa bai dang (chi khi OPEN va chua co bao gia ACCEPTED).
  - Loi co the gap:
    - 404 Post khong ton tai
    - 409 Post khong the xoa
- GET /posts/{postId}/quotes?page=&size=&sort=
  - Muc dich: Xem danh sach bao gia cua bai dang.
  - Loi co the gap:
    - 404 Post khong ton tai
    - 403 Khong co quyen
- POST /posts/{postId}/quotes/{quoteId}/accept
  - Muc dich: Chap nhan bao gia va tao don hang gia cong.
  - Body: { "addressId": 10 }
  - Loi co the gap:
    - 404 Post/Quote khong ton tai
    - 409 Post da dong hoac quote khong hop le

## Workshop (ROLE_WORKSHOP)
- GET /posts/open?keyword=&sort=&page=&size=&sort=
  - Muc dich: Bang tin bai dang OPEN, ho tro tim kiem va sap xep.
  - sort: latest | no-quotes
- GET /posts/{postId}
  - Muc dich: Xem chi tiet bai dang.
- GET /posts/{postId}
  - Response tra ve them `frontDesignUrl` va `backDesignUrl` de FE hien thi lai canvas da luu.
- POST /posts/{postId}/quotes
  - Muc dich: Gui bao gia.
  - Body: { "offeredPrice": 1000000, "estimateDays": 7 }
  - Loi co the gap:
    - 404 Post khong ton tai
    - 409 Post khong OPEN
- PUT /quotes/{quoteId}
  - Muc dich: Cap nhat bao gia (chi khi PENDING).
  - Body: { "offeredPrice": 1200000, "estimateDays": 10 }
- POST /quotes/{quoteId}/withdraw
  - Muc dich: Rut bao gia (chi khi PENDING).
- GET /quotes/me?page=&size=&sort=
  - Muc dich: Danh sach bao gia cua toi.

## Admin (ROLE_ADMIN)
- GET /admin/posts?status=&page=&size=&sort=
  - Muc dich: Quan ly bai dang.
- PUT /admin/posts/{postId}/status
  - Muc dich: Khoa/han che bai dang.
  - Body: { "status": "OPEN|CLOSED|FULFILLED" }
- GET /admin/quotes?status=&page=&size=&sort=
  - Muc dich: Theo doi bao gia.

## Response envelope
- Success: { success: true, message: "...", data: <T>, timestamp: "ISO-8601" }
- Error: { status, error, message, path, fieldErrors, timestamp }

## Frontend flow de xay dung yeu cau gia cong
1. FE render canvas mat truoc va mat sau.
2. FE export 2 anh PNG/JPEG tu canvas: `front_design.png`, `back_design.png`.
3. FE goi `POST /api/bidding/designs/upload` voi `multipart/form-data` de nhan `frontDesignUrl` va `backDesignUrl`.
4. FE goi `POST /api/bidding/posts` voi `title`, `description`, `aiImageUrl` neu co, va 2 URL design vua upload.
5. Workshop xem `GET /posts/open` va gui quote.
6. Customer chap nhan quote qua `POST /posts/{postId}/quotes/{quoteId}/accept`; server se copy 2 URL design sang order custom.

## Design response notes
- `BiddingPostDetailRecord` co them `frontDesignUrl` va `backDesignUrl`.
- `OrderDetailResponseRecord` co them `frontDesignUrl` va `backDesignUrl` sau khi tao don gia cong.

## Pagination
- Query params: page (0-based), size, sort (vd: createdAt,desc)
- Response data la doi tuong Page: { content, totalElements, totalPages, number, size }
