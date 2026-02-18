# Transaction Status API - Architecture & Data Flow

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     External API Client                          │
│                    (cURL, Postman, etc.)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   GET /api/payment/status/{txnid}
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    HTTP Request Processing                       │
│                   Path Parameter Extraction                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              PaymentStatusController                             │
│  @RestController                                                 │
│  @RequestMapping("/api/payment")                                 │
│                                                                  │
│  @GetMapping("/status/{txnid}")                                  │
│  public ResponseEntity<PaymentStatusResponse>                    │
│    getPaymentStatus(@PathVariable String txnid)                  │
│                                                                  │
│  1. Validate input (txnid not null/empty)                        │
│  2. Call service.getPaymentStatus(txnid)                         │
│  3. Return ResponseEntity.ok(response)                           │
│  4. Throw exceptions (handled by GlobalExceptionHandler)         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   ❌ Invalid Input (null/empty)
                       │        └──→ IllegalArgumentException
                       │              (400 Bad Request)
                       │
                       ├─→ PaymentStatusService.getPaymentStatus()
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              PaymentStatusService                                │
│  @Service                                                        │
│  @Transactional(readOnly = true)  ← Read-Only Optimization      │
│                                                                  │
│  public PaymentStatusResponse getPaymentStatus(String txnid)    │
│                                                                  │
│  1. Log: "Fetching payment status for txnid: {}"                │
│  2. Validate txnid (not null/empty)                              │
│  3. Call repository.findByTxnid(txnid)                           │
│  4. Handle Optional:                                             │
│     - If empty: throw TransactionNotFoundException (404)         │
│     - If present: convert entity to DTO                          │
│  5. Return PaymentStatusResponse DTO                             │
│  6. Log: "Transaction found - Status, Amount, Merchant"         │
│                                                                  │
│  private PaymentStatusResponse convertToResponse(Entity)        │
│  - Maps entity fields to DTO                                    │
│  - Preserves all UDF fields                                      │
│  - Returns fully populated DTO                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ❌ Not Found (Optional.empty())
                       │        └──→ TransactionNotFoundException
                       │              (404 Not Found)
                       │
                       └─→ PaymentTransactionRepository
                           .findByTxnid(txnid)
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│             PaymentTransactionRepository                         │
│  @Repository                                                     │
│  extends JpaRepository<PaymentTransaction, Long>                 │
│                                                                  │
│  Optional<PaymentTransaction> findByTxnid(String txnid)          │
│  - Uses Spring Data JPA derived query                            │
│  - Returns Optional<PaymentTransaction>                          │
│  - Used by service to check existence                            │
│                                                                  │
│  boolean existsByTxnid(String txnid)                             │
│  - Lightweight query for existence check                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                      Database Query
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  H2 Database                                     │
│              payment_transactions table                          │
│                                                                  │
│  SELECT * FROM payment_transactions WHERE txnid = '{txnid}'     │
│                                                                  │
│  Columns:                                                        │
│  - id (Long)                                                    │
│  - txnid (String) ← INDEXED for fast lookup                     │
│  - status (String)                                               │
│  - amount (BigDecimal)                                           │
│  - bank_ref_num (String)                                         │
│  - created_at (LocalDateTime) ← @CreationTimestamp              │
│  - updated_at (LocalDateTime) ← @UpdateTimestamp                │
│  - ... (other fields)                                            │
│                                                                  │
│  Indexes:                                                        │
│  - idx_txnid (UNIQUE) - Fast lookup by txnid                    │
│  - idx_status - For status queries                              │
│  - idx_created_at - For date range queries                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                  Return Row (if found)
                       │
                       └─→ Hibernate ORM Mapping ← @Entity, @Column
                               │
                               ▼
        ┌────────────────────────────────────────────────┐
        │  PaymentTransaction Entity                     │
        │  @Entity                                       │
        │  @Table(name = "payment_transactions")         │
        │                                                │
        │  Public Fields:                                │
        │  - txnid (String)                              │
        │  - status (String)                             │
        │  - amount (BigDecimal)                         │
        │  - createdAt (LocalDateTime)                   │
        │    @CreationTimestamp ← Automatic              │
        │    @Column(updatable = false)                  │
        │  - updatedAt (LocalDateTime)                   │
        │    @UpdateTimestamp ← Automatic                │
        │  - udf1-10 (String) ← Merchant routing         │
        │  - rawResponse (String) ← Full JSON            │
        │  - ... (other fields)                          │
        └────────┬─────────────────────────────────────┘
                 │
                 └─→ Service converts to DTO
                       │
                       ▼
        ┌────────────────────────────────────────────────┐
        │  PaymentStatusResponse DTO                     │
        │  @Data @Builder @NoArgsConstructor             │
        │                                                │
        │  Jackson Serialization:                        │
        │  @JsonProperty("txnid")                        │
        │  private String txnid;                         │
        │                                                │
        │  @JsonProperty("created_at")                   │
        │  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  │
        │  private LocalDateTime createdAt;              │
        │                                                │
        │  @JsonProperty("updated_at")                   │
        │  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  │
        │  private LocalDateTime updatedAt;              │
        │  ... (other fields)                            │
        └────────┬─────────────────────────────────────┘
                 │
                 └─→ Jackson Serialization
                       │
                       ├─→ Registered Modules:
                       │   - JavaTimeModule (handles LocalDateTime)
                       │   @JsonFormat applied per-field
                       │
                       └─→ ObjectMapper.writeValueAsString(dto)
                             │
                             ▼
        ┌────────────────────────────────────────────────┐
        │  JSON Response String                          │
        │ {                                              │
        │   "txnid": "TXN123456",                        │
        │   "status": "SUCCESS",                         │
        │   "amount": 1000.00,                           │
        │   "created_at": "2024-01-15 14:30:45",        │
        │   "updated_at": "2024-01-15 14:30:45"         │
        │   ... (other fields)                           │
        │ }                                              │
        └────────┬─────────────────────────────────────┘
                 │
                 ▼
        ┌────────────────────────────────────────────────┐
        │  ResponseEntity<PaymentStatusResponse>         │
        │  Status: 200 OK                                │
        │  Headers: Content-Type: application/json       │
        │  Body: JSON response                           │
        └────────┬─────────────────────────────────────┘
                 │
           HTTP Response
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│              Client Receives Response                            │
│                HTTP 200 OK                                       │
│                JSON Body with formatted dates                    │
│                and all transaction details                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Exception Handling Flow

```
┌──────────────────────────────────────────┐
│  Exception Thrown in Controller/Service  │
└──────────────┬───────────────────────────┘
               │
               ├─── IllegalArgumentException
               │     (null/empty txnid)
               │           │
               │           ▼
               │    GlobalExceptionHandler
               │    @ExceptionHandler(IllegalArgumentException.class)
               │           │
               │           ▼
               │    ResponseEntity (400 Bad Request)
               │    {
               │      "status": "FAILURE",
               │      "errorCode": "INVALID_ARGUMENT",
               │      "message": "...",
               │      "timestamp": 1705329045000
               │    }
               │
               ├─── TransactionNotFoundException
               │     (txnid not found in DB)
               │           │
               │           ▼
               │    GlobalExceptionHandler
               │    @ExceptionHandler(TransactionNotFoundException.class)
               │           │
               │           ▼
               │    ResponseEntity (404 Not Found)
               │    {
               │      "status": "FAILURE",
               │      "errorCode": "TRANSACTION_NOT_FOUND",
               │      "message": "...",
               │      "timestamp": 1705329045000
               │    }
               │
               ├─── MerchantNotActiveException
               │     (merchant status not ACTIVE)
               │           │
               │           ▼
               │    GlobalExceptionHandler
               │    @ExceptionHandler(MerchantNotActiveException.class)
               │           │
               │           ▼
               │    ResponseEntity (403 Forbidden)
               │    {
               │      "status": "FAILURE",
               │      "errorCode": "MERCHANT_NOT_ACTIVE",
               │      "message": "...",
               │      "timestamp": 1705329045000
               │    }
               │
               └─── Generic Exception
                     (Database error, etc.)
                           │
                           ▼
                    GlobalExceptionHandler
                    @ExceptionHandler(Exception.class)
                           │
                           ▼
                    ResponseEntity (500 Internal Server Error)
                    {
                      "status": "FAILURE",
                      "errorCode": "INTERNAL_SERVER_ERROR",
                      "message": "...",
                      "timestamp": 1705329045000
                    }
```

---

## Date Serialization Flow

```
┌─────────────────────────────────┐
│  Database Row                   │
│  created_at: 2024-01-15 14:30:45│
│  (Stored as TIMESTAMP)          │
└──────────────┬──────────────────┘
               │
    Hibernate ORM Mapping
               │
               ▼
┌─────────────────────────────────┐
│  PaymentTransaction Entity      │
│  @CreationTimestamp             │
│  private LocalDateTime createdAt│
│  Value: LocalDateTime object    │
└──────────────┬──────────────────┘
               │
    Service DTO Conversion
               │
               ▼
┌──────────────────────────────────────────┐
│  PaymentStatusResponse DTO               │
│  @JsonFormat(pattern="yyyy-MM-dd HH:mm") │
│  private LocalDateTime createdAt         │
│  Value: Same LocalDateTime object        │
└──────────────┬───────────────────────────┘
               │
    Jackson Serialization Chain:
    1. Check @JsonFormat annotation
    2. Use pattern: "yyyy-MM-dd HH:mm:ss"
    3. Format LocalDateTime object
    4. Output: String "2024-01-15 14:30:45"
               │
               ▼
┌──────────────────────────────────────────┐
│  JSON Response                           │
│  "created_at": "2024-01-15 14:30:45"   │
└──────────────────────────────────────────┘
```

---

## Component Dependencies

```
PaymentStatusController
    │
    ├─→ [depends on] PaymentStatusService
    │
    └─→ [throws] TransactionNotFoundException
                  IllegalArgumentException


PaymentStatusService
    │
    ├─→ [depends on] PaymentTransactionRepository
    │
    →→ [converts] PaymentTransaction Entity
    │
    └─→ [returns] PaymentStatusResponse DTO


PaymentTransactionRepository
    │
    └─→ [queries] Database via Hibernate


PaymentStatusResponse
    │
    └─→ [uses] Jackson Serialization
                └─→ JavaTimeModule
                └─→ @JsonFormat


PaymentTransaction Entity
    │
    ├─→ [uses] @CreationTimestamp
    │
    ├─→ [uses] @UpdateTimestamp
    │
    └─→ [maps to] payment_transactions table


GlobalExceptionHandler
    │
    ├─→ [handles] TransactionNotFoundException → 404
    │
    └─→ [handles] Generic Exception → 500
```

---

## Data Flow: Success Scenario

```
1. Client Request
   GET /api/payment/status/TXN123456
   
2. Controller receives request
   - Validates txnid ✓ (not null/empty)
   - Calls service.getPaymentStatus("TXN123456")

3. Service processes request
   - Logs: "Fetching payment status for txnid: TXN123456"
   - Calls repository.findByTxnid("TXN123456")
   
4. Repository queries database
   - SQL: SELECT * FROM payment_transactions WHERE txnid = 'TXN123456'
   - Finds row ✓
   - Hibernate maps to PaymentTransaction entity

5. Service receives entity
   - Logs: "Transaction found - Status: SUCCESS, Amount: 1000.00, Merchant: MERCHANT_001"
   - Converts entity to DTO
   - Returns PaymentStatusResponse

6. Controller receives response
   - Wraps in ResponseEntity.ok(response)
   - Returns ResponseEntity<PaymentStatusResponse>

7. Spring converts to HTTP response
   - Status: 200 OK
   - Content-Type: application/json
   - Body: Jackson serialization of DTO

8. Jackson Serialization
   - Registers JavaTimeModule
   - Applies @JsonFormat pattern to dates
   - Applies @JsonProperty for snake_case
   - Produces JSON string

9. HTTP Response sent to client
   HTTP/1.1 200 OK
   Content-Type: application/json
   Content-Length: 1234
   
   {
     "txnid": "TXN123456",
     "status": "SUCCESS",
     "amount": 1000.00,
     "created_at": "2024-01-15 14:30:45",
     "updated_at": "2024-01-15 14:30:45",
     ... (other fields)
   }

10. Client receives response ✅
```

---

## Data Flow: Error Scenarios

### Scenario 1: Transaction Not Found (404)

```
1. Client: GET /api/payment/status/NONEXISTENT

2. Controller
   - Validates txnid ✓ (not null/empty)
   - Calls service.getPaymentStatus("NONEXISTENT")

3. Service
   - Calls repository.findByTxnid("NONEXISTENT")
   - Optional is EMPTY
   - Throws → TransactionNotFoundException("Transaction not found with txnid: NONEXISTENT")

4. Exception bubbles up to GlobalExceptionHandler

5. GlobalExceptionHandler
   - Catches TransactionNotFoundException
   - Creates error response:
     {
       "status": "FAILURE",
       "errorCode": "TRANSACTION_NOT_FOUND",
       "message": "Transaction not found with txnid: NONEXISTENT",
       "timestamp": 1705329045000
     }
   - Wraps in ResponseEntity with HttpStatus.NOT_FOUND (404)

6. Client receives:
   HTTP/1.1 404 Not Found
   Content-Type: application/json
   
   {
     "status": "FAILURE",
     "errorCode": "TRANSACTION_NOT_FOUND",
     "message": "Transaction not found with txnid: NONEXISTENT",
     "timestamp": 1705329045000
   }
```

### Scenario 2: Invalid Input (400)

```
1. Client: GET /api/payment/status/

2. Controller
   - Validates txnid
   - txnid is NULL or EMPTY ✗
   - Throws → IllegalArgumentException("Transaction ID (txnid) is required...")

3. Exception bubbles up to GlobalExceptionHandler

4. GlobalExceptionHandler
   - Catches IllegalArgumentException
   - Creates error response:
     {
       "status": "FAILURE",
       "errorCode": "INVALID_ARGUMENT",
       "message": "Transaction ID (txnid) is required and must not be empty",
       "timestamp": 1705329045000
     }
   - Wraps in ResponseEntity with HttpStatus.BAD_REQUEST (400)

5. Client receives:
   HTTP/1.1 400 Bad Request
   Content-Type: application/json
   
   {
     "status": "FAILURE",
     "errorCode": "INVALID_ARGUMENT",
     "message": "Transaction ID (txnid) is required and must not be empty",
     "timestamp": 1705329045000
   }
```

---

## Key Architectural Improvements

✅ **Layered Architecture**
- Controller → Service → Repository → Database
- Each layer has specific responsibility

✅ **Dependency Injection**
- Constructor injection for services
- Easier testing with mock objects
- Explicit dependencies

✅ **Exception Handling**
- Centralized @RestControllerAdvice
- Consistent error responses
- Proper HTTP status codes

✅ **DTO Pattern**
- No entity exposure
- Serialization control
- API contract stability

✅ **Transaction Management**
- @Transactional(readOnly=true) for performance
- Automatic database transaction handling

✅ **Date Handling**
- JavaTimeModule support
- @JsonFormat for consistency
- Hibernate automatic timestamps

---

**This architecture ensures**:
- Maintainability
- Testability
- Scalability
- Type safety
- Clear separation of concerns
