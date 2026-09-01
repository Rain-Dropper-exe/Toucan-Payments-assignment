# AI Usage Disclosure

## 1. Tools Used
- Claude
- Gemini Code Assist
- Locally-run Qwen 3.8 (18B) model

---

## 2. What They Were Used For
- **Architecture Scaffolding**: Generated standard Spring Boot package and class skeletons (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `validation`) after deciding the domain requirements, entity structures, database approach, and the four business operations.
- **Integration Test Drafting**: Formulated the MockMvc integration test suite based on specified edge-case scenarios and state transition rules.
- **JPA & Persistence Mappings**: Refined JPA column mappings (`@Column(precision = 19, scale = 2)`), string enums (`@Enumerated(EnumType.STRING)`), and automated audit timestamps (`@CreationTimestamp`, `@UpdateTimestamp`).
- **Convention & Organization**: Clean naming conventions and package boundaries separating API contracts (DTOs) from database entities.

---

## 3. Significant AI Generation / Suggestions
- Initial boilerplate structure for controllers, services, repositories, and exception classes.
- Standard MockMvc request builder templates (`post()`, `get()`, `patch()`, `jsonPath()`, `status()`).
- Initial advice on consolidating separate error handlers into a central `@RestControllerAdvice` component.

---

## 4. What I Changed, Corrected, or Rejected — and Why
- **Rejected Forking the Starter Repository**: Since the starter repository was public, I cloned it and pushed to an independent private Git repository instead of creating a public fork (a fork would have remained publicly visible and linked).
- **Consolidated Global Exception Handling**: Instead of writing separate try-catch blocks in each controller, I structured a centralized `GlobalExceptionHandler` with clean, numbered handlers returning uniform `{ "error": "...", "message": "..." }` responses.
- **Strict Entity Encapsulation**: Replaced the AI's default pattern of generating public getters and setters for all entity fields with read-only fields and controlled mutation methods, preventing external callers from arbitrarily modifying settled financial data.
- **Removed Dead Code / Unused Constructors**: The AI initially generated an all-arguments constructor on `TransactionResponse` by mirroring the request DTO. Because responses are only ever constructed from saved JPA entities, this was recognized as dead code and removed in favor of a single entity-mapping constructor.
- **Double-Layered Validation**: Enforced custom scale validation both programmatically in the service layer (`BigDecimal.scale() <= 2`) and via Bean Validation (`@Digits(integer=17, fraction=2)`).
- **Separation of Concerns**: Kept DTO contracts strictly decoupled from JPA entities rather than collapsing them together.

---

## 5. What the AI Got Wrong That I Had to Fix
- **Unused Response-DTO Constructor**: Generated automatically via pattern matching without realizing the response DTO is only ever instantiated from a persistent entity.
- **Premature Test Imports**: Test imports for unbuilt classes were prematurely included on early iterations, causing build compilation errors that had to be cleaned up.
- **Datasource Configuration Mismatch**: An initial datasource URL configuration didn't match the in-memory H2 database name, corrected to `jdbc:h2:mem:transactions`.

---

## 6. How I Verified the Final Result Actually Works
- **Automated Test Suite**: Executed `.\mvnw.cmd clean test` from a clean state — 14 tests, 0 failures, 0 errors, `BUILD SUCCESS`.
- **Manual API Testing**: Manually executed requests across all four endpoints (Create, Get by ID, Status Update, Customer Lookup) in Postman, validating both happy paths and edge cases (terminal state transitions, scale rejections, duplicate IDs).
- **Database State Inspection**: Inspected the underlying `TRANSACTIONS` table in the H2 Web Console to confirm records, column values, and timestamps were correctly persisted.
