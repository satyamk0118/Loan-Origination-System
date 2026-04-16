# Loan Origination System (LOS)
### Turno Backend Assignment — Java Spring Boot

---

## Quickstart (Zero Setup — Docker)

> Requires only: **Docker Desktop** installed and running.

```bash
# 1. Unzip and enter the project
unzip loan-origination-system.zip
cd los

# 2. Start PostgreSQL + App with one command
docker-compose up --build

# 3. App is live at http://localhost:8080
```

To stop:
```bash
docker-compose down       # stop containers
docker-compose down -v    # stop + wipe database
```

---

## Local Dev (IntelliJ / VS Code)

### Prerequisites
- Java 17+ · Maven 3.9+ · Docker Desktop (for PostgreSQL only)

```bash
# Start only the database
docker-compose up postgres -d

# Run the app
./mvnw spring-boot:run
# OR: open in IntelliJ → right-click LoanOriginationSystemApplication → Run
```

**IntelliJ**: File → Open → select the `los` folder (auto-detects Maven)
**VS Code**: Open folder → install "Extension Pack for Java"

---

## API Reference

Base URL: `http://localhost:8080`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | `/api/v1/loans` | Submit a loan application |
| GET    | `/api/v1/loans?status=&page=&size=` | Paginated loans by status |
| GET    | `/api/v1/loans/status-count` | Real-time counts per status |
| GET    | `/api/v1/customers/top` | Top 3 customers by approvals |
| PUT    | `/api/v1/agents/{agentId}/loans/{loanId}/decision` | Agent approve/reject |

### Submit Loan — `POST /api/v1/loans`
```json
{ "customerName": "Ravi Kumar", "customerPhone": "+919876543210", "loanAmount": 500000, "loanType": "PERSONAL" }
```
`loanType`: `PERSONAL` | `HOME` | `AUTO` | `BUSINESS`

### Agent Decision — `PUT /api/v1/agents/{id}/loans/{id}/decision`
```json
{ "decision": "APPROVE" }
```
`decision`: `APPROVE` | `REJECT`

### Status values: `APPLIED` · `APPROVED_BY_SYSTEM` · `REJECTED_BY_SYSTEM` · `UNDER_REVIEW` · `APPROVED_BY_AGENT` · `REJECTED_BY_AGENT`

---

## Decision Rules (Background Job)

```
loanAmount > 10,00,000  →  UNDER_REVIEW        (assigned to agent + push notify)
random 20%              →  REJECTED_BY_SYSTEM
otherwise               →  APPROVED_BY_SYSTEM  (SMS to customer)
```

Delay simulates system checks (15–25 seconds, configurable in `application.properties`).

---

## Seeded Agents (auto-created on first run)

| ID | Name  | Manager |
|----|-------|---------|
| 1  | Alice | —       |
| 2  | Dave  | —       |
| 3  | Bob   | Alice   |
| 4  | Carol | Alice   |
| 5  | Eve   | Dave    |

---

## Running Tests
```bash
mvn test        # uses H2 in-memory, no Postgres needed
```

## Postman Collection
Import `LOS_Postman_Collection.json` → set `base_url = http://localhost:8080`
