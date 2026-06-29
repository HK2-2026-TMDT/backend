# API Auth (FE)

## Base
- Base URL: /api/auth
- Auth header: Khong bat buoc cho cac endpoint trong file nay (tru refresh token theo luong dang nhap).

## Authentication

- POST /firebase
  - Muc dich: Dang nhap bang Firebase ID token.
  - Body: { "idToken": "string" }
  - Response data: { "accessToken": "string", "tokenType": "Bearer", "expiresIn": 3600, "userId": 1, "role": "CUSTOMER", "refreshToken": "string" }
  - Loi co the gap:
    - 400 idToken khong hop le
    - 401 Firebase token khong xac thuc duoc

- POST /login
  - Muc dich: Dang nhap bang email/password.
  - Body: { "email": "user@example.com", "password": "secret" }
  - Response data: { "accessToken": "string", "tokenType": "Bearer", "expiresIn": 3600, "userId": 1, "role": "CUSTOMER", "refreshToken": "string" }
  - Loi co the gap:
    - 400 Email/password khong hop le format
    - 401 Sai thong tin dang nhap
    - 403 Tai khoan chua kich hoat email (neu co ap dung)

- POST /register
  - Muc dich: Dang ky tai khoan moi.
  - Body: { "email": "user@example.com", "password": "secret123", "fullName": "Nguyen Van A" }
  - Rule validate:
    - email: bat buoc, dung format email
    - password: bat buoc, do dai 6-100 ky tu
    - fullName: bat buoc
  - Response data: { "userId": 1, "email": "user@example.com", "verificationToken": "string" }
  - Loi co the gap:
    - 400 Du lieu dang ky khong hop le
    - 409 Email da ton tai

- POST /verify-email
  - Muc dich: Xac minh email sau khi dang ky.
  - Body: { "token": "string" }
  - Response data: null
  - Loi co the gap:
    - 400 Token khong hop le
    - 404 Token khong ton tai hoac het han

## Password Recovery

- POST /forgot-password
  - Muc dich: Tao reset token de dat lai mat khau.
  - Body: { "email": "user@example.com" }
  - Response data: { "resetToken": "string" }
  - Loi co the gap:
    - 400 Email khong hop le
    - 404 Khong tim thay tai khoan

- POST /reset-password
  - Muc dich: Dat lai mat khau bang reset token.
  - Body: { "token": "string", "newPassword": "newSecret123" }
  - Rule validate:
    - token: bat buoc
    - newPassword: bat buoc, do dai 6-100 ky tu
  - Response data: null
  - Loi co the gap:
    - 400 Token hoac mat khau moi khong hop le
    - 404 Token khong ton tai hoac het han

## Token

- POST /refresh-token
  - Muc dich: Lay access token moi bang refresh token.
  - Body: { "refreshToken": "string" }
  - Response data: { "accessToken": "string", "tokenType": "Bearer", "expiresIn": 3600, "userId": 1, "role": "CUSTOMER", "refreshToken": "string" }
  - Loi co the gap:
    - 400 refreshToken khong hop le
    - 401 refreshToken het han/bi thu hoi

## Ghi chu
- Tat ca response deu theo wrapper chung: ApiResponse<T>.
- Cac endpoint tra ve token deu dung AuthResponseRecord:
  - accessToken: JWT dung de goi API can auth
  - tokenType: thuong la "Bearer"
  - expiresIn: thoi gian song access token (giay)
  - userId: ID nguoi dung
  - role: vai tro (CUSTOMER/WORKSHOP/ADMIN)
  - refreshToken: token de cap moi access token
