# 🔐 Spring Boot Auth System (JWT + Session + Refresh Token)

## 📌 Table of Contents
1. [Overview](#overview)
2. [Key Concepts](#key-concepts)
3. [Architecture](#architecture)
4. [Components Breakdown](#components-breakdown)
5. [Authentication Flows](#authentication-flows)
6. [Code Explanations](#code-explanations)
7. [Revision Notes](#revision-notes)

---

## 📖 Overview

This authentication system is built using **Spring Boot** with **JWT (JSON Web Tokens)** and **Session Management**. It provides:
- ✅ User Registration (Sign Up)
- ✅ User Login with Credentials
- ✅ JWT-based Token Authentication
- ✅ Refresh Token for Token Renewal
- ✅ Session Management
- ✅ Role-based Access Control (RBAC)
- ✅ User Logout

> **Location**: Both `authservice` and `demo` projects contain auth implementations

---

## 🎯 Key Concepts to Remember

### 1. **JWT (JSON Web Tokens)**
- **What**: A token format that contains user information in an encoded format
- **Structure**: 3 parts separated by dots (`.`)
  - **Header**: Token type and algorithm (e.g., `{"type":"JWT","alg":"HS256"}`)
  - **Payload**: Claims (data) like email, sessionId, issued time
  - **Signature**: Secret key used to verify token authenticity
- **Example**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U`
- **Uses**: Stateless authentication between client and server
- **Expiry**: Set to 1 hour in this system

### 2. **Session Management**
- **SessionId**: Unique identifier for each user login
- **Active Sessions**: Only 1 active session per user at a time
- **Old Sessions**: Invalidated when user logs in from different device/browser
- **Purpose**: Track active user sessions in database

### 3. **Refresh Token**
- **What**: A special token that lives longer (7 days) than JWT (1 hour)
- **Purpose**: To get a new access token without asking user to login again
- **Process**: 
  1. JWT expires after 1 hour
  2. Client sends Refresh Token to `/auth/refresh` endpoint
  3. New JWT is issued (if refresh token is valid)

### 4. **Password Security**
- **Hashing**: Using BCrypt algorithm to encrypt passwords
- **Why**: Never store plain text passwords
- **Match Check**: Use `passwordEncoder.matches()` to verify login password

### 5. **Role-Based Access Control (RBAC)**
- **USER**: Normal user role
- **ADMIN**: Administrator role
- **Stored**: In User model as `role` field
- **Checked**: In SecurityConfig using `.hasRole()` and `.hasAnyRole()`

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT / FRONTEND                        │
│                   (Mobile/Web Browser)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTP Requests with JWT Token
                         │ Authorization: Bearer <token>
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              SPRING SECURITY FILTER CHAIN                    │
│  1. JwtFilter (OncePerRequestFilter)                        │
│     - Extracts token from Authorization header             │
│     - Validates token using JwtUtil                        │
│     - Sets SecurityContext for authenticated user          │
│     - Checks session is active in database                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    CONTROLLERS                               │
│                                                              │
│  AuthController:                                            │
│  - POST /auth/login      -> UserService.loginUser()        │
│  - POST /auth/register   -> UserService.createUser()       │
│  - POST /auth/logout     -> UserService.logoutUser()       │
│  - POST /auth/refresh    -> RefreshTokenService.generate() │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼─────────────────┐
        │                │                 │
        ▼                ▼                 ▼
    ┌────────┐      ┌──────────┐     ┌─────────────┐
    │UserServ│      │JwtUtil   │     │RefreshToken │
    │ice     │      │          │     │Service      │
    └────────┘      └──────────┘     └─────────────┘
        │                │                 │
        └────────────────┼──────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   REPOSITORIES (Database)      │
        │  - UserRepository              │
        │  - UserSessionRepository       │
        │  - RefreshTokenRepository      │
        └────────────────────────────────┘
```

---

## 🧩 Components Breakdown

### 1. **Models (Database Entities)**

#### `User.java`
```
What: Represents a user account in the system
Columns:
  - id: Unique identifier (auto-generated)
  - name: User's full name
  - email: Unique email address
  - password: BCrypt encrypted password
  - role: USER or ADMIN
  - createdAt: Account creation timestamp
```

#### `UserSession.java`
```
What: Represents an active session for a logged-in user
Columns:
  - id: Session record ID
  - sessionId: Unique session identifier (UUID)
  - user: Reference to User (Foreign Key)
  - active: Boolean flag (true = active, false = logged out)
  - createdAt: Session creation timestamp

Why Needed:
  - Track which devices/browsers are logged in
  - Invalidate old sessions on new login
  - Validate JWT token claims
```

#### `RefreshToken.java`
```
What: Stores refresh tokens for extending access without re-login
Columns:
  - id: Refresh token record ID
  - token: Random UUID as refresh token
  - userSession: Reference to UserSession
  - expiryDate: When token expires (7 days from creation)
```

### 2. **DTOs (Data Transfer Objects)**

#### `UserRequest.java` (Registration/Signup)
```
Fields:
  - name: @NotBlank (required)
  - email: @Email @NotBlank (must be valid email, required)
  - password: @NotBlank (required)

Used For: POST /auth/register
```

#### `LoginRequest.java` (Login)
```
Fields:
  - email: @Email @NotBlank (required)
  - password: @NotBlank (required)

Used For: POST /auth/login
```

#### `AuthResponse.java` (Auth Endpoints Response)
```
Fields:
  - accessToken: JWT token (expires in 1 hour)
  - refreshToken: Refresh token (expires in 7 days)

Returned By: /auth/login and /auth/refresh endpoints
```

#### `UserResponse.java` (User Data Response)
```
Fields:
  - id: User ID
  - name: User name
  - email: User email

Returned By: /auth/register endpoint
Used For: Returning user data safely (no password)
```

#### `RefreshTokenRequest.java` (Token Refresh)
```
Fields:
  - refreshToken: The refresh token to use for renewal

Used For: POST /auth/refresh
```

### 3. **Services (Business Logic)**

#### `UserService.java`

**Method 1: `createUser(UserRequest request)` - REGISTRATION**
```
Steps:
1. Check if email already exists in database
   - If yes: throw DuplicateEmailException
   
2. Create new User object
   - Set name, email from request
   - Encrypt password using BCryptPasswordEncoder
   - Set default role as "USER"
   
3. Save user to database using UserRepository
4. Return UserResponse (without password)

Why BCrypt:
- One-way encryption
- Cannot decrypt
- Adds salt for extra security
- Each hash is different even for same password
```

**Method 2: `loginUser(LoginRequest request)` - LOGIN**
```
Steps:
1. Find user by email in database
   - If not found: throw UserNotFoundException
   
2. Verify password
   - Use passwordEncoder.matches() to compare:
     * Provided password (plain text)
     * Stored password (BCrypt encrypted)
   - If mismatch: throw IncorrectCredentialsException

3. DEACTIVATE OLD SESSIONS
   - Find all active sessions for this user
   - Mark them as active=false (user logged out from other devices)
   - Delete old refresh tokens associated with old sessions
   - Save updated sessions to database

4. CREATE NEW SESSION
   - Generate new sessionId (UUID)
   - Create UserSession object
   - Link to current user
   - Mark as active=true
   - Save to database

5. GENERATE JWT TOKEN
   - Call JwtUtil.generateToken(email, sessionId)
   - JWT contains:
     * email (subject)
     * sessionId (claim)
     * issued time
     * expiration (1 hour from now)

6. CREATE REFRESH TOKEN
   - Generate random token (UUID)
   - Link to current session
   - Set expiry to 7 days from now
   - Save to database

7. RETURN AuthResponse
   - accessToken: JWT (short-lived)
   - refreshToken: Random token (long-lived)

Important Flow Diagram:
┌──────────────────────┐
│ Check old sessions   │
│ if user logged in    │
│ from 3 devices:      │
│                      │
│ Device 1: ACTIVE ✓   │
│ Device 2: ACTIVE ✓   │
│ Device 3: ACTIVE ✓   │
└──────────────────────┘
         │
         ▼
┌──────────────────────┐
│ Now logging in from  │
│ Device 4             │
│                      │
│ Mark all old         │
│ sessions as INACTIVE │
│                      │
│ Device 1: INACTIVE   │
│ Device 2: INACTIVE   │
│ Device 3: INACTIVE   │
│ Device 4: ACTIVE ✓   │
└──────────────────────┘
```

**Method 3: `logoutUser(String sessionId)` - LOGOUT**
```
Steps:
1. Receive sessionId from request
2. Find UserSession by sessionId
3. Mark as active=false
4. Delete associated refresh token
5. Save changes
6. Return success message

Result: User logged out from all endpoints
```

**Helper Method: `mapToResponse(User user)`**
```
Converts User model to UserResponse
- Copies: id, name, email
- Excludes: password, role (sensitive data)
Why: Always return safe data to client
```

#### `RefreshTokenService.java`

**Method: `generateAccessToken(RefreshTokenRequest request)`**
```
Purpose: Issue new JWT when old one expires

Steps:
1. Find refresh token in database
   - If not found: throw InvalidRefreshTokenException

2. Check if refresh token expired
   - Compare expiryDate with current time
   - If expired: throw InvalidRefreshTokenException

3. Check if associated session is still active
   - If inactive: throw InvalidRefreshTokenException
   (Session might be invalidated by logout/new login)

4. Extract user email and sessionId from session
5. Generate new JWT using JwtUtil.generateToken()
6. Return AuthResponse with new access token
   (Only access token, no new refresh token)

Timeline Example:
┌─────────────────────────────────────────┐
│ User logs in at 10:00 AM                │
│                                         │
│ Access Token: expires at 11:00 AM      │
│ Refresh Token: expires in 7 days       │
└─────────────────────────────────────────┘
              │
              │ At 10:59 AM, access token about to expire
              ▼
┌─────────────────────────────────────────┐
│ Client calls POST /auth/refresh         │
│ Sends: { refreshToken: "uuid..." }      │
└─────────────────────────────────────────┘
              │
              │ Validate refresh token
              ▼
┌─────────────────────────────────────────┐
│ New Access Token issued                 │
│ Expires at 12:00 PM (next hour)        │
│                                         │
│ Old Access Token: INVALID               │
│ Refresh Token: SAME (still valid)       │
└─────────────────────────────────────────┘
```

### 4. **Security Components**

#### `JwtUtil.java` - JWT Generation & Validation

```
CONSTANTS:
SECRET_KEY = "jfsdlfkdjfdsdfeterotuvcmvbasdfghjhgfdsasdfghjhgfds"
EXPIRY = 1 hour (3600 seconds)

Method 1: generateToken(email, sessionId)
─────────────────────────────────────────
Purpose: Create JWT token
Input: 
  - email: User's email
  - sessionId: Active session ID

Process:
  1. Create claims map
     claims.put("sessionId", sessionId)
  
  2. Build JWT using Jwts.builder()
     - Set subject to email
     - Add issued time (now)
     - Set expiration (1 hour from now)
     - Sign with secret key
     - Compact (serialize)
  
Output: JWT string (token)

Example Token Structure:
Header.Payload.Signature
where Payload (BASE64 decoded) looks like:
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "sub": "user@example.com",
  "iat": 1609459200,
  "exp": 1609462800
}
```

```
Method 2: extractEmail(token)
──────────────────────────────
Purpose: Get email from JWT
Input: token (JWT string)
Output: email extracted from "sub" (subject) claim
Process:
  1. Parse and validate token
  2. Extract claims
  3. Get subject field
  4. Return email
```

```
Method 3: extractSessionId(token)
──────────────────────────────────
Purpose: Get sessionId from JWT
Input: token (JWT string)
Output: sessionId from custom claim
Process:
  1. Parse and validate token
  2. Extract claims
  3. Get "sessionId" claim
  4. Return sessionId

Important: Database will verify this sessionId
is still active before allowing access
```

```
Method 4: isTokenValid(token)
──────────────────────────────
Purpose: Quick check if token is valid
Input: token (JWT string)
Output: boolean (true if valid, false if invalid)
Process:
  1. Try to parse and validate token
  2. If parse succeeds: return true
  3. If any exception: return false

What Makes Token Valid:
  ✓ Signature matches secret key
  ✓ Not expired (iat < now < exp)
  ✓ Proper JWT format
  
What Makes Token Invalid:
  ✗ Wrong signature (tampered)
  ✗ Expired
  ✗ Malformed
  ✗ Missing claims
```

```
Method 5: getClaims(token) - PRIVATE
────────────────────────────────────
Purpose: Core parsing and validation
Input: token (JWT string)
Output: Claims object (contains all data)

Process:
  1. Create parser using Jwts.parser()
  2. Set secret key for verification
  3. Build parser
  4. Parse signed claims from token
  5. Extract payload (claims)
  6. Return claims

Why This Method:
  - Single source of token validation
  - All other methods use this
  - If token invalid, throws exception
```

#### `JwtFilter.java` - Request Interceptor

```
What: Intercepts EVERY HTTP request
Type: OncePerRequestFilter (runs once per request)
Purpose: Authenticate user from JWT token

Flow:
┌──────────────────────────┐
│ HTTP Request arrives     │
│ Headers: {              │
│   "Authorization":      │
│   "Bearer <JWT_TOKEN>"   │
│ }                       │
└──────────────────────────┘
         │
         ▼
┌──────────────────────────┐
│ JwtFilter.doFilterInternal│
│ method called             │
└──────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ Step 1: Get Authorization   │
│ header from request          │
│                              │
│ authHeader = request.        │
│   getHeader("Authorization") │
│                              │
│ Check if exists and         │
│ starts with "Bearer "        │
└──────────────────────────────┘
         │
    ┌────┴────┐
    │          │
   No         Yes ✓
    │          │
    │          ▼
    │    ┌────────────────────┐
    │    │ Extract token      │
    │    │ Remove "Bearer "   │
    │    │ token = authHeader │
    │    │  .substring(7)     │
    │    └────────────────────┘
    │          │
    │          ▼
    │    ┌──────────────────┐
    │    │ Extract email    │
    │    │ from token using │
    │    │ JwtUtil.extract  │
    │    │ Email()          │
    │    └──────────────────┘
    │          │
    │          ▼
    │    ┌──────────────────────┐
    │    │ Is email null?       │
    │    │ Is already auth?     │
    │    │ (prevent dup login)  │
    │    └──────────────────────┘
    │          │
    │     ┌────┴──────┐
    │     │           │
    │    No          Yes
    │     │           │
    │     │           └──> Continue to next filter
    │     │                (Skip auth)
    │     │
    │     ▼
    │ ┌──────────────────────┐
    │ │ Validate token using │
    │ │ JwtUtil.isTokenValid │
    │ │ (check signature,    │
    │ │  expiration)         │
    │ └──────────────────────┘
    │          │
    │     ┌────┴──────┐
    │     │           │
    │  Invalid      Valid ✓
    │     │           │
    │     │           ▼
    │     │   ┌─────────────────┐
    │     │   │ Extract sessionId│
    │     │   │ from token      │
    │     │   └─────────────────┘
    │     │          │
    │     │          ▼
    │     │   ┌─────────────────────────┐
    │     │   │ Check database:         │
    │     │   │ Is this session ACTIVE? │
    │     │   │                         │
    │     │   │ Find UserSession by     │
    │     │   │ sessionId where         │
    │     │   │ active=true             │
    │     │   └─────────────────────────┘
    │     │          │
    │     │     ┌────┴──────┐
    │     │     │           │
    │  Not Found   Found ✓
    │     │     │           │
    │     │     │           ▼
    │     │     │   ┌──────────────────┐
    │     │     │   │ Load user from   │
    │     │     │   │ database using   │
    │     │     │   │ email            │
    │     │     │   └──────────────────┘
    │     │     │          │
    │     │     │          ▼
    │     │     │   ┌──────────────────┐
    │     │     │   │ Create           │
    │     │     │   │ UsernamePassword │
    │     │     │   │ AuthenticationTo │
    │     │     │   │ ken with user    │
    │     │     │   │ authorities      │
    │     │     │   │ (roles)          │
    │     │     │   └──────────────────┘
    │     │     │          │
    │     │     │          ▼
    │     │     │   ┌──────────────────┐
    │     │     │   │ Set auth token   │
    │     │     │   │ in SecurityContext│
    │     │     │   │                  │
    │     │     │   │ User now         │
    │     │     │   │ AUTHENTICATED    │
    │     │     │   │ ✓ Can access     │
    │     │     │   │ protected routes │
    │     │     │   └──────────────────┘
    │     │     │
    │     └─────┘
    │
    └──────────────────> Continue to next filter
                         (with or without auth)
```

### 5. **SecurityConfig.java** - Spring Security Configuration

```
Purpose: Configure which routes need authentication
Type: @Configuration with @EnableWebSecurity

Methods:
┌─────────────────────────────────┐
│ securityFilterChain(HttpSecurity)│
└─────────────────────────────────┘
         │
         ▼
Configuration Steps:

Step 1: Disable CSRF
────────────────────
http.csrf().disable();
Why: JWT is stateless, CSRF not applicable
     CSRF (Cross-Site Request Forgery) for form-based auth


Step 2: Set Session Policy
──────────────────────────
http.sessionManagement()
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
Why: STATELESS = no server-side HTTPSession
     Use JWT instead, more scalable


Step 3: Configure Route Authorization
──────────────────────────────────────
http.authorizeHttpRequests()
   .requestMatchers("/auth/**").permitAll()
   │
   ├─ /auth/login      → Anyone can access
   ├─ /auth/register   → Anyone can access
   ├─ /auth/refresh    → Anyone can access
   └─ /auth/logout     → Anyone can access (but needs valid JWT)
   
   .requestMatchers("/admin/**").hasRole("ADMIN")
   │
   ├─ /admin/* → Only ADMIN role
   ├─ Others   → 403 Forbidden
   
   .requestMatchers("/users/**").hasAnyRole("USER","ADMIN")
   │
   ├─ /users/* → USER or ADMIN can access
   ├─ Others   → 403 Forbidden
   
   .anyRequest().authenticated()
   │
   └─ All other routes → Must be authenticated


Step 4: Add JWT Filter
──────────────────────
http.addFilterBefore(jwtFilter, 
                     UsernamePasswordAuthenticationFilter.class);
Why: JwtFilter runs BEFORE default auth filter
     Ensures JWT is checked before anything else


Return: SecurityFilterChain object
```

### 6. **AuthController.java** - HTTP Endpoints

```
Base URL: /auth
All endpoints return ResponseEntity<T>

┌──────────────────────────────────────────────────┐
│ POST /auth/register - User Registration          │
├──────────────────────────────────────────────────┤
│ Request Body:                                    │
│ {                                                │
│   "name": "John Doe",                            │
│   "email": "john@example.com",                   │
│   "password": "securePassword123"                │
│ }                                                │
│                                                  │
│ Response (Status 201 CREATED):                   │
│ {                                                │
│   "id": 1,                                       │
│   "name": "John Doe",                            │
│   "email": "john@example.com"                    │
│ }                                                │
│                                                  │
│ Errors:                                          │
│ - Email validation failed → 400 Bad Request      │
│ - Email already exists → 400 Duplicate           │
│ - Password too weak → 400 Bad Request            │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│ POST /auth/login - User Login                    │
├──────────────────────────────────────────────────┤
│ Request Body:                                    │
│ {                                                │
│   "email": "john@example.com",                   │
│   "password": "securePassword123"                │
│ }                                                │
│                                                  │
│ Response (Status 200 OK):                        │
│ {                                                │
│   "accessToken": "eyJhbGc...",                   │
│   "refreshToken": "550e8400-e29b-41d4..."        │
│ }                                                │
│                                                  │
│ Errors:                                          │
│ - Email not found → 401 Invalid Credentials     │
│ - Wrong password → 401 Invalid Credentials      │
│ - Email validation failed → 400 Bad Request      │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│ POST /auth/logout - User Logout                  │
├──────────────────────────────────────────────────┤
│ Headers Required:                                │
│ Authorization: Bearer <accessToken>              │
│                                                  │
│ Request Body: (empty)                           │
│                                                  │
│ Response (Status 200 OK):                        │
│ "Logged out successfully"                        │
│                                                  │
│ Process:                                         │
│ 1. Filter extracts sessionId from JWT           │
│ 2. Sets it in request attribute                 │
│ 3. UserService.logoutUser() marks session      │
│    as inactive                                  │
│ 4. All future requests with this JWT rejected   │
│                                                  │
│ Errors:                                          │
│ - No/Invalid Authorization header → 401          │
│ - Session already inactive → 200 (idempotent)   │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│ POST /auth/refresh - Refresh Access Token       │
├──────────────────────────────────────────────────┤
│ Request Body:                                    │
│ {                                                │
│   "refreshToken": "550e8400-e29b-41d4..."        │
│ }                                                │
│                                                  │
│ Response (Status 200 OK):                        │
│ {                                                │
│   "accessToken": "eyJhbGc...",                   │
│   "refreshToken": null                           │
│ }                                                │
│                                                  │
│ When to Use:                                     │
│ - Original accessToken expired                  │
│ - Want to continue using app without login      │
│                                                  │
│ Errors:                                          │
│ - Refresh token not found → 401 Invalid Token   │
│ - Refresh token expired → 401 Invalid Token     │
│ - Session inactive/logged out → 401 Invalid     │
└──────────────────────────────────────────────────┘
```

---

## 📊 Authentication Flows

### Flow 1: User Registration
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/register
     │ {name, email, password}
     ▼
┌──────────────────────┐
│ AuthController       │
│ registerUser()       │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ UserService.createUser()         │
│ 1. Check if email exists         │
│    ✓ No → Continue               │
│    ✗ Yes → Throw Exception       │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 2. Hash password with BCrypt     │
│    plainPassword → encrypted     │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 3. Create User object            │
│    - name, email, encrypted pwd  │
│    - role = "USER" (default)     │
│    - createdAt = now             │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 4. Save to database              │
│    INSERT INTO users ...         │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 5. Return UserResponse           │
│    (id, name, email) - NO pwd    │
└────┬─────────────────────────────┘
     │
     ▼
┌────────────────────────────────────┐
│ Client receives:                   │
│ {                                  │
│   "id": 1,                         │
│   "name": "John Doe",              │
│   "email": "john@example.com",     │
│   "message": "User registered"     │
│ }                                  │
│                                    │
│ Status: 201 CREATED                │
│ Now user can login                 │
└────────────────────────────────────┘
```

### Flow 2: User Login
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/login
     │ {email, password}
     ▼
┌──────────────────────┐
│ AuthController       │
│ loginUser()          │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ UserService.loginUser()          │
│                                  │
│ 1. Find user by email in DB      │
│    ✓ Found → Continue            │
│    ✗ Not Found → Throw Exception │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 2. Verify password               │
│    plainPassword vs encrypted    │
│    ✓ Match → Continue            │
│    ✗ No Match → Throw Exception  │
└────┬─────────────────────────────┘
     │
     ▼
┌───────────────────────────────────────┐
│ 3. DEACTIVATE OLD SESSIONS            │
│    Find all sessions where            │
│    user_id=1 AND active=true          │
│                                       │
│    Mobile session (old)    → INACTIVE │
│    Desktop session (old)   → INACTIVE │
│    Tablet session (old)    → INACTIVE │
│                                       │
│    Delete old refresh tokens          │
└────┬──────────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 4. CREATE NEW SESSION            │
│    - sessionId = UUID.random()   │
│    - user_id = 1                 │
│    - active = true               │
│    - createdAt = now             │
│                                  │
│    INSERT INTO user_sessions ... │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 5. GENERATE JWT TOKEN            │
│    JwtUtil.generateToken(email,  │
│                         sessionId)│
│                                  │
│    Payload:                      │
│    {                             │
│      "sub": "john@example...",   │
│      "sessionId": "550e8400...",  │
│      "iat": 1609459200,          │
│      "exp": 1609462800           │
│    }                             │
│                                  │
│    Signed with SECRET_KEY        │
│    Result: eyJhbGc...            │
│    Expires in 1 hour             │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 6. CREATE REFRESH TOKEN          │
│    - token = UUID.random()       │
│    - userSession_id = 1          │
│    - expiryDate = now + 7 days   │
│                                  │
│    INSERT INTO refresh_tokens... │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 7. Build AuthResponse            │
│    {                             │
│      "accessToken": "eyJhbGc..." │
│      "refreshToken": "550e8400..." │
│    }                             │
└────┬─────────────────────────────┘
     │
     ▼
┌────────────────────────────────────┐
│ Client receives:                   │
│ {                                  │
│   "accessToken": "eyJhbGc...",     │
│   "refreshToken": "550e8400...",   │
│   "message": "Login successful"    │
│ }                                  │
│                                    │
│ Status: 200 OK                     │
│                                    │
│ Client stores both tokens locally: │
│ - accessToken → Use in headers    │
│ - refreshToken → Store safely     │
└────────────────────────────────────┘
```

### Flow 3: Making Authenticated Request
```
┌─────────────────────────────┐
│ Client wants to call        │
│ GET /users/profile          │
│                             │
│ Has: accessToken            │
└────┬──────────────────────┬─┘
     │                      │
     ▼                      ▼
┌──────────────────┐   ┌─────────────┐
│ Add to header:   │   │ Check token │
│                  │   │ expires in  │
│ Authorization:   │   │ 30 minutes  │
│ Bearer <token>   │   │ Use now ✓   │
│                  │   └─────────────┘
└────┬─────────────┘
     │
     │ GET /users/profile
     │ Headers: {Authorization: Bearer eyJhbGc...}
     │
     ▼
┌────────────────────────────────┐
│ Spring Security Filter Chain   │
│                                │
│ → JwtFilter.doFilterInternal()│
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 1. Extract Authorization       │
│    header = "Bearer eyJhbGc..."│
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 2. Remove "Bearer " prefix     │
│    token = "eyJhbGc..."       │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 3. Extract email from token    │
│    email = jwtUtil.extractEmail│
│    Result: john@example.com    │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 4. Validate token signature    │
│    jwtUtil.isTokenValid()      │
│    ✓ Valid (correct signature) │
│    ✓ Not expired               │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 5. Extract sessionId from token│
│    sessionId = "550e8400..."   │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────────┐
│ 6. Check database:                 │
│    SELECT * FROM user_sessions     │
│    WHERE sessionId="550e8400..."   │
│    AND active=true                 │
│                                    │
│    ✓ Found and active              │
└────┬────────────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 7. Load UserDetails            │
│    Get user from database      │
│    Load authorities/roles      │
│    Result: User - ADMIN        │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 8. Create AuthenticationToken  │
│    Set in SecurityContext      │
│                                │
│    User is now AUTHENTICATED ✓ │
│    Can access protected routes │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ 9. Continue filter chain       │
│    Request reaches Controller  │
│                                │
│    @GetMapping("/profile")     │
│    public UserResponse get(){} │
│                                │
│    ✓ Can execute controller    │
│    Returns user profile data   │
└────┬─────────────────────────┘
     │
     ▼
┌─────────────────────────────────┐
│ Client receives:                │
│ {                               │
│   "id": 1,                      │
│   "name": "John Doe",           │
│   "email": "john@example.com",  │
│   "role": "ADMIN"               │
│ }                               │
│                                 │
│ Status: 200 OK                  │
└─────────────────────────────────┘
```

### Flow 4: Token Refresh
```
┌──────────────────────────┐
│ 11:59 AM - 1 min left    │
│ on accessToken           │
│                          │
│ Client detects this      │
│ (by checking exp claim)  │
└────┬─────────────────────┘
     │
     ▼
┌─────────────────────────────────┐
│ POST /auth/refresh              │
│                                 │
│ {                               │
│   "refreshToken": "550e8400..." │
│ }                               │
└────┬────────────────────────────┘
     │
     ▼
┌───────────────────────────────────┐
│ RefreshTokenService               │
│ generateAccessToken()             │
│                                   │
│ 1. Find refresh token in DB       │
│    SELECT * FROM refresh_tokens   │
│    WHERE token="550e8400..."      │
│                                   │
│    ✓ Found                        │
└────┬──────────────────────────────┘
     │
     ▼
┌───────────────────────────────────┐
│ 2. Check if refresh token expired │
│    expiryDate > now               │
│    ✓ Not expired (5 days left)    │
└────┬──────────────────────────────┘
     │
     ▼
┌───────────────────────────────────┐
│ 3. Verify session is active       │
│    Check user_sessions            │
│    WHERE sessionId=...            │
│    AND active=true                │
│                                   │
│    ✓ Session still active         │
└────┬──────────────────────────────┘
     │
     ▼
┌────────────────────────────┐
│ 4. Extract user info       │
│    email = john@example... │
│    sessionId = 550e8400... │
└────┬───────────────────────┘
     │
     ▼
┌────────────────────────────┐
│ 5. Generate new JWT        │
│    JwtUtil.generateToken() │
│                            │
│    New accessToken:        │
│    - Exp: now + 1 hour     │
│    - Same user info        │
│    - Same sessionId        │
│    - New signature         │
│                            │
│    Old token is INVALIDATED│
└────┬───────────────────────┘
     │
     ▼
┌────────────────────────────┐
│ 6. Return AuthResponse     │
│    {                       │
│      "accessToken": NEW... │
│      "refreshToken": null  │
│    }                       │
└────┬───────────────────────┘
     │
     ▼
┌──────────────────────────────┐
│ Client receives:             │
│ New accessToken (1 hour)     │
│                              │
│ 12:00 PM - Fresh token       │
│ Can use for next 1 hour      │
│                              │
│ Refresh token unchanged      │
│ Can still use in 4 days      │
└──────────────────────────────┘
```

### Flow 5: User Logout
```
┌──────────────┐
│ User clicks  │
│ "Logout"     │
└────┬─────────┘
     │
     │ POST /auth/logout
     │ Headers: {Authorization: Bearer <token>}
     │
     ▼
┌────────────────────────────┐
│ Spring Security Filters    │
│ → JwtFilter validates JWT  │
│   Extracts sessionId       │
│   Sets in request attr.    │
└────┬───────────────────────┘
     │
     ▼
┌────────────────────────────┐
│ AuthController.logoutUser()│
│ (Receives sessionId)       │
└────┬───────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ UserService.logoutUser()         │
│                                  │
│ 1. Find UserSession by sessionId │
│    SELECT * FROM user_sessions   │
│    WHERE sessionId="550e8400..." │
│                                  │
│    ✓ Found                       │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 2. Mark as inactive              │
│    session.setActive(false)      │
│    UPDATE user_sessions          │
│    SET active=false              │
│    WHERE sessionId="550e8400..." │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 3. Delete refresh token          │
│    DELETE FROM refresh_tokens    │
│    WHERE user_session_id=1       │
│                                  │
│    (Refresh token no longer      │
│     can be used)                 │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ 4. Return success message        │
│    "Logged out successfully"     │
└────┬─────────────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Client receives:         │
│ Status: 200 OK           │
│ Message: Success         │
│                          │
│ Now:                     │
│ ✗ Old accessToken → 401  │
│ ✗ Old refreshToken → 401 │
│                          │
│ Must login again to use  │
│ protected routes         │
└──────────────────────────┘
```

---

## 💻 Code Explanations

### Explanation 1: BCrypt Password Hashing
```java
// Creating encoder
BCryptPasswordEncoder passwordEncoder = 
    new BCryptPasswordEncoder();

// ENCODING (hashing) password during registration
String plainPassword = "myPassword123";
String hashedPassword = passwordEncoder.encode(plainPassword);
// Result: $2a$10$r3aFYL8Q9W5R2b6.mC7w.e...
// (Different each time, but can always verify)

// Database stores: hashedPassword only (never plain)
user.setPassword(hashedPassword);
userRepository.save(user);

// VERIFYING password during login
String loginPassword = "myPassword123";
String storedHash = user.getPassword();
            // ^(from database)

boolean isCorrect = passwordEncoder.matches(
    loginPassword,    // plain text from login form
    storedHash        // encrypted from database
);

if(isCorrect) {
    // Login success
} else {
    // Wrong password
}

// WHY NOT SIMPLE ENCRYPTION?
┌──────────────────────────────────────┐
│ Simple Encryption (BAD):             │
│                                      │
│ plainPass → Encrypt → storedPass     │
│ (Can decrypt if key compromised)     │
│ (Same password = same hash)          │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│ BCrypt Hashing (GOOD):               │
│                                      │
│ plainPass → Hash → storedHash        │
│ (One-way, cannot decrypt)            │
│ (Same password ≠ same hash each time)│
│ (Built-in salt prevents rainbow      │
│  table attacks)                      │
└──────────────────────────────────────┘
```

### Explanation 2: JWT Token Structure
```
Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
       eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwic2Vzc2lvbklkIjoiNTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAwIiwiaWF0IjoxNjA5NDU5MjAwLCJleHAiOjE2MDk0NjI4MDB9.
       6G9_Q8r.3K2-L1p9O5mN8xV2YzC5aB7dE4fH3iJw

Split by dots (.):

PART 1 - HEADER (Base64 decoded):
┌────────────────────────────┐
│ {                          │
│   "alg": "HS256",          │
│   "typ": "JWT"             │
│ }                          │
│                            │
│ Tells us:                  │
│ - Algorithm: HMAC SHA-256  │
│ - Type: JWT                │
└────────────────────────────┘

PART 2 - PAYLOAD (Base64 decoded):
┌───────────────────────────────────┐
│ {                                 │
│   "sub": "john@example.com",     │
│   "sessionId": "550e8400-e29b-... │
│   "iat": 1609459200,              │
│   "exp": 1609462800               │
│ }                                 │
│                                   │
│ sub = subject (who is using it)   │
│ sessionId = our custom claim       │
│ iat = issued at (timestamp)        │
│ exp = expiration (timestamp)       │
│                                   │
│ This is the actual DATA in token   │
└───────────────────────────────────┘

PART 3 - SIGNATURE:
┌──────────────────────────────────────┐
│ HMACSHA256(                          │
│   header + "." + payload,            │
│   SECRET_KEY                         │
│ )                                    │
│                                      │
│ This proves:                         │
│ ✓ Token not tampered                 │
│ ✓ Created by authorized server       │
│ ✓ Using correct secret key           │
│                                      │
│ If anyone changes payload:           │
│ Signature won't match → Token invalid│
└──────────────────────────────────────┘

HOW VALIDATION WORKS:

Step 1: Client sends token
    Authorization: Bearer eyJhbGc...

Step 2: Server receives token, splits by "."
    Part1 = header
    Part2 = payload
    Part3 = signature

Step 3: Server recalculates signature
    newSignature = HMACSHA256(
        Part1 + "." + Part2,
        SECRET_KEY
    )

Step 4: Compare signatures
    if(newSignature == Part3) {
        Token is valid ✓
        Nobody tampered with it
    } else {
        Token invalid ✗
        Reject request
    }

Step 5: Check expiration
    Now = current timestamp
    If(Now > exp claim) {
        Token expired
        Reject, ask for refresh
    } else {
        Token still valid
        Allow request
    }
```

### Explanation 3: Filter Sequence
```java
// SecurityConfig.addFilterBefore()
http.addFilterBefore(
    jwtFilter,                              // Custom filter
    UsernamePasswordAuthenticationFilter    // Spring's default
    .class
);

EXECUTION ORDER (left to right):
┌──────────────────────────────────────────────────────┐
│ Request comes in                                     │
│ ↓                                                    │
│ ┌──────────────────────────────────────────────────┐│
│ │ JwtFilter (runs first)                           ││
│ │ - Checks Authorization header                    ││
│ │ - Validates JWT                                  ││
│ │ - Sets SecurityContext if valid                  ││
│ │ - Passes control to next filter                  ││
│ └──────────────────────────────────────────────────┘│
│ ↓                                                    │
│ ┌──────────────────────────────────────────────────┐│
│ │ UsernamePasswordAuthenticationFilter (default)   ││
│ │ - Checks for form-based authentication           ││
│ │ - Not used here (we have JWT in SecurityContext)││
│ └──────────────────────────────────────────────────┘│
│ ↓                                                    │
│ ┌──────────────────────────────────────────────────┐│
│ │ Other filters (CorsFilter, etc)                  ││
│ └──────────────────────────────────────────────────┘│
│ ↓                                                    │
│ ┌──────────────────────────────────────────────────┐│
│ │ Authorization Filter                             ││
│ │ - Checks if user has required roles              ││
│ │ - Based on endpoint (/admin, /users, etc)       ││
│ └──────────────────────────────────────────────────┘│
│ ↓                                                    │
│ ┌──────────────────────────────────────────────────┐│
│ │ Controller (your endpoint)                       ││
│ │ @GetMapping("/profile")                          ││
│ │ - Can access SecurityContextHolder.getContext()  ││
│ │ - Know who is logged in                          ││
│ └──────────────────────────────────────────────────┘│
│ ↓                                                    │
│ Response goes back                                  │
└──────────────────────────────────────────────────────┘
```

### Explanation 4: Session Invalidation on New Login
```java
// In UserService.loginUser():

// Step 1: Find all ACTIVE sessions for this user
List<UserSession> oldSessions = 
    userSessionRepository.findByUserAndActiveTrue(user);
    // Returns: [Session1, Session2, Session3]
    // (User logged in from 3 devices)

// Step 2: Deactivate all old sessions
for (UserSession session : oldSessions) {
    session.setActive(false);  // Mark inactive
    // Delete old refresh tokens for this session
    refreshTokenRepository.deleteByUserSession(session);
}
userSessionRepository.saveAll(oldSessions);

// Database state AFTER:
/*
user_sessions table:
┌───────┬──────────────────┬─────────┬────────┐
│ id    │ sessionId        │ user_id │ active │
├───────┼──────────────────┼─────────┼────────┤
│ 1     │ 550e8400...      │ 1       │ false  │ ← Old
│ 2     │ 660f9511...      │ 1       │ false  │ ← Old
│ 3     │ 770g0622...      │ 1       │ false  │ ← Old
│ 4     │ 880h1733...      │ 1       │ true   │ ← New
└───────┴──────────────────┴─────────┴────────┘
*/

// WHY DO THIS?
// When user logs in from new device/browser:
// ✓ All old JWT tokens become useless
//   (Session in DB marked inactive)
// ✓ JwtFilter checks: "Is this sessionId active?"
//   Even if JWT is valid, session is inactive
//   → Access Denied
// ✓ Refresh tokens deleted
//   (Can't get new tokens from old session)
// ✓ User forced to login again on old devices
//   (Security feature, prevents token stealing)

// Example Security Scenario:
/*
Day 1: User logs in from Coffee Shop via WiFi
  sessionId: ABC123
  tokens: [JWT, RefreshToken]
  
Day 2: Hacker steals these tokens somehow
  Hacker tries to use tokens...
  
Day 3: Same user logs in from Home via WiFi (new device)
  OLD Session ABC123 → marked inactive
  Hacker's JWT now fails at:
    JwtFilter: "Is sessionId ABC123 active?"
    Database: "No, it's marked inactive"
    → 401 Unauthorized
  Hacker cannot proceed!

This is why we deactivate old sessions.
*/
```

---

## 📚 Revision Notes

### Quick Summary Table

| Component | Purpose | Key Methods |
|-----------|---------|------------|
| **User.java** | User account data | id, email, password, role |
| **UserSession.java** | Track active sessions | sessionId, active flag |
| **RefreshToken.java** | Token renewal | refresh token with expiry |
| **JwtUtil.java** | Create/validate JWT | generateToken(), isTokenValid() |
| **JwtFilter.java** | Intercept requests | doFilterInternal() |
| **UserService.java** | Business logic | createUser(), loginUser(), logoutUser() |
| **RefreshTokenService.java** | Issue new tokens | generateAccessToken() |
| **SecurityConfig.java** | Route authorization | configure() |

### Key Points to Remember (MUST KNOW)

#### 1. **Three Components of Auth**
- **Registration**: User creates account (email, password)
- **Login**: User proves identity (email + password match)
- **Session**: Track that user is still logged in

#### 2. **Two Token Types**
| Token | Lifetime | Purpose |
|-------|----------|---------|
| **Access Token (JWT)** | 1 hour | Use to access APIs |
| **Refresh Token** | 7 days | Use to get new access token |

#### 3. **Three Places JWT Can Fail**
1. **Signature Invalid** → Token tampered (use different secret)
2. **Token Expired** → Time passed `exp` claim
3. **Session Inactive** → Session marked inactive in DB

#### 4. **Password Never Travels**
- ❌ NEVER send password in requests (except login/register)
- ✅ Always send JWT token in `Authorization: Bearer <token>` header
- ✅ Server validates token, knows user is authenticated

#### 5. **One Active Session Per User**
- User can only be logged in on 1 device at a time
- Login on Device 2 → Device 1 automatically logged out
- This is security + simplicity choice

#### 6. **BCrypt is One-Way**
```
Password123 → BCrypt → $2a$10$r3aFYL8Q9W5...
You can NEVER get back Password123 from $2a$10$r3aFYL8Q9W5...
You can only verify by running:
  matches(Password123, $2a$10$r3aFYL8Q9W5...) → true/false
```

#### 7. **JWT in Stateless but Contains State**
- **Stateless**: No session file on server
- **Contains State**: User info (email, sessionId) inside token
- **But Session ID verified**: Against database (Sessions table)

#### 8. **Request Authentication Flow**
```
Has Header?
  ✓ YES: Extract token
         Validate signature & expiry
         Check sessionId active in DB
         Set in SecurityContext
         → Proceed to controller
  
  ✗ NO: Check if endpoint requires auth
        /auth/* → No auth needed
        /users/* → Auth needed → 401
```

### Common Interview Questions

**Q1: What happens if attacker modifies JWT payload?**
A: Signature won't match. When server recalculates signature from modified payload, it gets DIFFERENT hash. Signature check fails → Token invalid.

**Q2: Why can't we use JWT alone without database?**
A: We need to:
1. Track which sessions are active (logout feature)
2. Force user off old sessions (security)
3. Validate refresh tokens (prevent token reuse)
4. These require database

**Q3: What's the difference between access & refresh token?**
A: 
- Access: SHORT expiry (1h), used for every request
- Refresh: LONG expiry (7d), used to get new access token
- If access token stolen, damage limited (1 hour)
- Refresh token stored safely (no sensitive operations)

**Q4: How does logout work?**
A: Mark session as inactive in DB. Even if attacker has valid JWT, filter checks session status. If inactive → 401. Token becomes useless.

**Q5: Why use sessionId in JWT AND session table?**
A: 
- JWT contains sessionId (avoids DB lookup for every request... well, almost)
- Table stores session state (active/inactive)
- JWT fast validation, DB provides control

---

## 🎓 Study Checklist

- [ ] Understand registration flow (password hashing)
- [ ] Understand login flow (JWT + Refresh token generation)
- [ ] Understand JWT token structure (Header.Payload.Signature)
- [ ] Understand JwtFilter role (intercept every request)
- [ ] Understand session management (one per user)
- [ ] Understand refresh token (renew access without re-login)
- [ ] Understand logout (mark session inactive)
- [ ] Understand BCrypt (one-way hashing)
- [ ] Understand SecurityConfig (route authorization)
- [ ] Understand DTOs (UserRequest, LoginRequest, etc)
- [ ] Understand Repositories (UserRepository, etc)
- [ ] Understand @Transactional (atomic operations)
- [ ] Know when to use each endpoint
- [ ] Know why old sessions are invalidated

---

## 📞 Quick Reference

### API Endpoints
```
POST /auth/register      → Create account
POST /auth/login         → Get tokens
POST /auth/logout        → Invalidate session
POST /auth/refresh       → Get new access token
GET  /users/**           → User data (protected)
GET  /admin/**           → Admin only (protected)
```

### Error Codes
```
400 → Bad Request (validation error)
401 → Unauthorized (invalid/expired token)
403 → Forbidden (valid token but no permission)
409 → Conflict (email already exists)
```

### Default Values
```
Access Token Expiry:  1 hour
Refresh Token Expiry: 7 days
User Role:           USER
Password Encoder:    BCrypt
Filter Order:        JwtFilter → Others → Authorization
```

---

**Created for Revision & Quick Learning** ✨
