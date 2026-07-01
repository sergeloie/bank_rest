# Audit Report - Bank Rest Refactoring

## 1. Summary
This report summarizes the findings from the audit of the `bank_rest` project, focusing on security, database efficiency, business logic, and code quality.

## 2. Issues Found

### CRITICAL
1.  **H2 Console Exposure:** The H2 console (`/h2-console/**`) is open to everyone in `SecurityConfig.java`. This is a critical risk if it remains enabled in production environments.
2.  **Horizontal Privilege Escalation (Card Blocking):** Users could request blocking of arbitrary cards because `CardController` did not verify card ownership. (Fix applied in Task 7/9).
3.  **Vertical Privilege Escalation (500 Error):** Accessing ADMIN endpoints as a USER resulted in a 500 error instead of a 403 Forbidden. (Fix applied in `GlobalExceptionHandler`).
4.  **Transfer Concurrency:** While pessimistic locking is used, we need to ensure it is robustly implemented to prevent deadlocks and data corruption under high concurrent load.

### IMPORTANT
1.  **Missing Indexes:** `card_block_request` table lacks indexes on `person_id` and `card_id`, which could impact performance as the table grows.
2.  **Data Type Consistency:** Ensure all `BigDecimal` fields for financial amounts are mapped consistently.
3.  **Exception Handling:** Some business logic errors (e.g., `InvalidCardOperationException`) were mapped generally, which needed standardizing in `GlobalExceptionHandler`.

### IMPROVEMENT
1.  **Code Style:** Standardize `PreAuthorize` annotation style (mixing class-level and method-level).
2.  **Unused Configuration:** Check for redundant configuration and cleanup temporary README files.

---

## 3. Status of Fixes
- [x] Horizontal Privilege Escalation in `CardController` (Fixed)
- [x] Vertical Privilege Escalation handling via `GlobalExceptionHandler` (Fixed)
- [x] Pessimistic locking in `TransferService` (Verified)
- [ ] Database indexes on `card_block_request` (Pending - Task 8)
- [ ] H2 console restriction in `SecurityConfig` (Pending - Task 7)
