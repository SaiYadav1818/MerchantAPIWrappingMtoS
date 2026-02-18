@echo off
REM Transaction Status API Test Script
REM This script tests the fixed Transaction Status API

echo.
echo ============================================================================
echo Transaction Status API - Test Suite
echo ============================================================================
echo.

set BASE_URL=http://localhost:8080

REM Note: The application must be running before executing these tests

echo.
echo TEST 1: Get Payment Status (Example with TXN prefix)
echo Command: curl -X GET "%BASE_URL%/api/payment/status/TXN123456"
echo Expected: 404 (Transaction not found if no data in DB)
echo.
timeout /t 2 /nobreak

echo.
echo TEST 2: Check Transaction Exists
echo Command: curl -X GET "%BASE_URL%/api/payment/exists/TXN123456"
echo Expected: {"exists": false, "txnid": "TXN123456"}
echo.
timeout /t 2 /nobreak

echo.
echo TEST 3: Invalid Input - Empty Transaction ID
echo Command: curl -X GET "%BASE_URL%/api/payment/status/{empty}"
echo Expected: 400 Bad Request
echo.

echo.
echo ============================================================================
echo To run these tests manually, use the following commands:
echo ============================================================================
echo.
echo Test 1 - Get Payment Status:
echo.
echo   curl -X GET "http://localhost:8080/api/payment/status/TXN1234567890"^
echo     -H "Content-Type: application/json"
echo.
echo.
echo Test 2 - Check if Transaction Exists:
echo.
echo   curl -X GET "http://localhost:8080/api/payment/exists/TXN1234567890"^
echo     -H "Content-Type: application/json"
echo.
echo.
echo Expected Success Response (200 OK):
echo.
echo {
echo   "txnid": "TXN1234567890",
echo   "status": "SUCCESS",
echo   "amount": 1000.00,
echo   "bank_ref_num": "BANK123456789",
echo   "created_at": "2024-01-15 14:30:45",
echo   "updated_at": "2024-01-15 14:30:45"
echo ...
echo }
echo.
echo Expected Error Response (404 Not Found):
echo.
echo {
echo   "status": "FAILURE",
echo   "errorCode": "TRANSACTION_NOT_FOUND",
echo   "message": "Transaction not found with txnid: TXN999999",
echo   "timestamp": 1705329045000
echo }
echo.
echo ============================================================================
echo API is ready at: http://localhost:8080/api/payment/status/{txnid}
echo ============================================================================
