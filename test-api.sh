#!/bin/bash

# Netflix++ API Testing Script
# This script tests all major endpoints of the Netflix++ backend

BASE_URL="${1:-http://localhost:8080}"
USERNAME="testuser_$(date +%s)"
PASSWORD="password123"
EMAIL="${USERNAME}@example.com"

echo "🧪 Netflix++ API Testing Suite"
echo "================================"
echo "Base URL: $BASE_URL"
echo "Test User: $USERNAME"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to make requests
function test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local headers=$4
    local description=$5

    echo -e "${YELLOW}[TEST]${NC} $description"
    echo "  $method $endpoint"

    if [ "$method" = "GET" ] || [ "$method" = "DELETE" ]; then
        if [ -z "$headers" ]; then
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json")
        else
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" $headers)
        fi
    else
        if [ -z "$headers" ]; then
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json" -d "$data")
        else
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" $headers -d "$data")
        fi
    fi

    # Check if response is empty
    if [ -z "$response" ]; then
        echo -e "${RED}✗ FAILED - No response from server${NC}"
        return 1
    fi

    # Check if response contains error
    if echo "$response" | grep -q '"error"'; then
        echo -e "${RED}✗ FAILED${NC}"
        echo "  Response: $response"
        return 0
    else
        echo -e "${GREEN}✓ PASSED${NC}"
    fi
    echo ""
}

# Test 1: Register
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📝 Authentication Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

register_data="{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\", \"email\": \"$EMAIL\"}"
test_endpoint "POST" "/api/auth/register" "$register_data" "" "Register new user"

# Test 2: Login
login_data="{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}"
echo -e "${YELLOW}[TEST]${NC} Login user"
echo "  POST /api/auth/login"

login_response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$login_data")

if [ -z "$login_response" ]; then
    echo -e "${RED}✗ FAILED - No response from server${NC}"
    echo "Make sure the backend is running on $BASE_URL"
    exit 1
fi

TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4 2>/dev/null || echo "")

if [ -n "$TOKEN" ] && [ "$TOKEN" != "" ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
    echo "  Token: ${TOKEN:0:20}..."
    echo ""
else
    echo -e "${RED}✗ FAILED${NC}"
    echo "  Response: $login_response"
    echo ""
    echo "Backend might not be running. Start it with: ./gradlew run"
    exit 1
fi

# Test 3: Get Profile
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "👤 User Profile Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "GET" "/api/user/profile" "" "-H 'Authorization: Bearer $TOKEN'" "Get user profile"

# Test 4: Update Profile
update_data="{\"firstName\": \"Test\", \"lastName\": \"User\"}"
test_endpoint "PUT" "/api/user/profile" "$update_data" "-H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json'" "Update user profile"

# Test 5: Get Watchlist
test_endpoint "GET" "/api/user/watchlist" "" "-H 'Authorization: Bearer $TOKEN'" "Get watchlist"

# Test 6: Get Watch History
test_endpoint "GET" "/api/user/history" "" "-H 'Authorization: Bearer $TOKEN'" "Get watch history"

# Test 7: Movie Endpoints
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎬 Movie Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "GET" "/api/movies?page=1&limit=5" "" "" "Get all movies (paginated)"

test_endpoint "GET" "/api/movies/featured" "" "" "Get featured movies"

test_endpoint "GET" "/api/movies/recent?limit=5" "" "" "Get recent movies"

test_endpoint "GET" "/api/movies/1" "" "" "Get movie by ID"

test_endpoint "GET" "/api/movies/search/bunny" "" "" "Search movies"

# Test 8: Streaming Endpoints
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📺 Streaming Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "GET" "/api/stream/1/links" "" "-H 'Authorization: Bearer $TOKEN'" "Get streaming links"

test_endpoint "GET" "/api/stream/1/info" "" "-H 'Authorization: Bearer $TOKEN'" "Get playback info"

# Test 9: Mesh P2P Endpoints
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🕸️  Mesh P2P Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "GET" "/api/mesh/health" "" "" "Mesh health check"

test_endpoint "GET" "/api/mesh/chunks/1" "" "" "Get chunk list"

test_endpoint "GET" "/api/mesh/peers" "" "" "Get active peers"

peer_data="{\"peerId\": \"peer-test-$(date +%s)\", \"address\": \"192.168.1.100\", \"port\": 9001}"
test_endpoint "POST" "/api/mesh/register" "$peer_data" "" "Register peer"

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✓ All tests completed!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
