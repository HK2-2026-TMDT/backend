# API Catalog (FE)

## Base
- Base URL: /api/catalog
- Auth header: Authorization: Bearer <accessToken>

## Public
- GET /categories
  - Muc dich: Danh sach danh muc san pham.
- GET /products?keyword=&categoryId=&workshopId=&minPrice=&maxPrice=&page=&size=&sort=
  - Muc dich: Danh sach san pham public, ho tro tim kiem/loc.
- GET /products/{productId}
  - Muc dich: Chi tiet san pham.
- GET /products/newest
  - Muc dich: Danh sach 20 san pham moi nhat, duoc cache tren Redis.

## Customer (ROLE_CUSTOMER)
- GET /me/favorites/products
  - Muc dich: Danh sach san pham da yeu thich cua user hien tai.
- POST /products/{productId}/favorites
  - Muc dich: Them san pham vao danh sach yeu thich.
- DELETE /products/{productId}/favorites
  - Muc dich: Bo san pham khoi danh sach yeu thich.

## Workshop (ROLE_WORKSHOP)
- POST /products
  - Muc dich: Tao san pham moi (kem variants va images).
  - Body: { "name": "string", "categoryId": 1, "basePrice": 100000, "description": "string", "variants": [{"skuCode":"string","color":"red","size":"M","price":120000,"stockQuantity":10}], "images": [{"imageUrl":"string","isThumbnail":true}] }
- PUT /products/{productId}
  - Muc dich: Cap nhat san pham (co the thay the variants/images).
- DELETE /products/{productId}
  - Muc dich: Xoa san pham.
- GET /products/me?page=&size=&sort=
  - Muc dich: Danh sach san pham cua xưởng.
- POST /products/{productId}/variants
  - Muc dich: Them bien the (variant).
  - Body: { "skuCode": "string", "color": "red", "size": "M", "price": 120000, "stockQuantity": 10 }
- PUT /variants/{variantId}
  - Muc dich: Cap nhat thong tin variant.
- PUT /variants/{variantId}/stock
  - Muc dich: Cap nhat ton kho.
  - Body: { "stockQuantity": 20 }
- DELETE /variants/{variantId}
  - Muc dich: Xoa variant.
- PUT /products/{productId}/images
  - Muc dich: Thay the danh sach anh san pham.
  - Body: [{"imageUrl":"string","isThumbnail":true}]

## Response envelope
- Success: { success: true, message: "...", data: <T>, timestamp: "ISO-8601" }
- Error: { status, error, message, path, fieldErrors, timestamp }

## Product response notes
- `ProductSummaryResponseRecord` co them `isFavorite` de FE biet san pham co trong danh sach yeu thich hay khong.
- `ProductDetailResponseRecord` co them `isFavorite` de hien thi trang chi tiet.

## Redis cache notes
- `categories`: cache danh sach danh muc.
- `newest-products`: cache 20 san pham moi nhat.
- `product-detail`: cache du lieu chi tiet san pham, phan `isFavorite` van duoc tinh theo user hien tai khi tra response.

## Pagination
- Query params: page (0-based), size, sort (vd: createdAt,desc)
- Response data la doi tuong Page: { content, totalElements, totalPages, number, size }
