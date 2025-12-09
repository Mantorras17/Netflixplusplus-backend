# 🔧 IMPORTANT: API Path Correction

## ⚠️ ALL endpoints require `/api/` prefix!

Your backend is running on **port 8080** and Jersey servlet is mounted at **`/api/*`**

### ❌ Wrong
```bash
curl http://localhost:8080/auth/login      # ERROR 405
curl http://localhost:8080/movies           # ERROR 405
curl http://localhost:8080/user/profile     # ERROR 405
```

### ✅ Correct
```bash
curl http://localhost:8080/api/auth/login           # OK
curl http://localhost:8080/api/movies              # OK
curl http://localhost:8080/api/user/profile        # OK
curl http://localhost:8080/api/stream/1/info       # OK
curl http://localhost:8080/api/mesh/health         # OK
```

---

## Complete Endpoint List (with `/api/` prefix)

### Authentication
- `POST /api/auth/register` - Create new user
- `POST /api/auth/login` - Login and get token
- `POST /api/auth/change-password` - Change password (requires token)

### User Profile
- `GET /api/user/profile` - Get your profile (requires token)
- `PUT /api/user/profile` - Update profile (requires token)
- `GET /api/user/watchlist` - Get watchlist (requires token)
- `POST /api/user/watchlist/{movieId}` - Add to watchlist (requires token)
- `GET /api/user/history` - Get watch history (requires token)

### Movies
- `GET /api/movies` - List all movies
- `GET /api/movies/featured` - Get featured movies
- `GET /api/movies/recent` - Get recent movies
- `GET /api/movies/{id}` - Get movie details
- `GET /api/movies/search/{query}` - Search movies

### Streaming
- `GET /api/stream/{movieId}/links` - Get streaming links (requires token)
- `GET /api/stream/{movieId}/download/{resolution}` - Download video (requires token)
- `GET /api/stream/{movieId}/info` - Get playback info (requires token)

### Mesh P2P
- `GET /api/mesh/health` - Check health
- `GET /api/mesh/chunks/{movieId}` - Get chunk list
- `GET /api/mesh/chunk/{movieId}/{chunkIndex}` - Get specific chunk
- `GET /api/mesh/peers` - Get active peers
- `POST /api/mesh/register` - Register new peer

### Admin
- `POST /api/admin/movies` - Upload movie (requires admin token)
- `PUT /api/admin/movies/{id}` - Update movie (requires admin token)
- `DELETE /api/admin/movies/{id}` - Delete movie (requires admin token)
- `POST /api/admin/users` - Create user (requires admin token)
- `DELETE /api/admin/users/{username}` - Delete user (requires admin token)

---

## Quick Test with Correct Paths

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123","email":"test@test.com"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123"}' | jq -r '.token')

# Test authenticated endpoint
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"
```

---

## Running Test Scripts

Both test scripts have been updated to use the correct `/api/` paths:

```bash
# Simple test (5 quick tests)
bash simple-test.sh http://localhost:8080

# Full test (all endpoints)
bash test-api.sh http://localhost:8080
```

---

## Server Info

| Component | URL | Port |
|-----------|-----|------|
| HTTP API | http://localhost:8080 | 8080 |
| API Prefix | `/api/*` | - |
| Mesh HTTP | http://localhost:8081 | 8081 |
| Mesh TCP | localhost | 8082 |

Check `src/main/java/org/netflixpp/config/Config.java` to see all port settings.
