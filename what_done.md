# What Was Done

Implementation of the lab task: 10 RestAssured tests against the Swagger Petstore API
(<https://petstore.swagger.io/v2>).

## Project Changes

### 1. `pom.xml`

The starter POM had RestAssured but no test runner. Added:

- **JUnit Jupiter 5.10.2** — test framework.
- **Hamcrest 2.2** — matchers used in `.body(...)` assertions.
- **Maven Surefire plugin 3.2.5** — runs JUnit 5 tests with `mvn test`.

`json-schema-validator` scope was changed to `test` (it is only needed at test time).

### 2. `src/test/java/org/example/PetstoreApiTests.java` (new)

Single test class containing 10 tests. Base URL is set once in `@BeforeAll`:

```java
RestAssured.baseURI = "https://petstore.swagger.io";
RestAssured.basePath = "/v2";
```

A helper `samplePet(id, name, status)` builds the request body used by the
create/update/delete scenarios.

## Test Coverage

Every test asserts at least two of: **status code**, **`Content-Type`**, **data values**,
**data structure**. Both positive and negative scenarios are covered.

### Positive (7)

| # | Test                        | Endpoint                              | Key assertions |
|---|-----------------------------|---------------------------------------|----------------|
| 1 | `findPetsByStatusAvailable` | `GET /pet/findByStatus?status=available` | 200, JSON, list size > 0, every `status == "available"` |
| 2 | `createPetSuccessfully`     | `POST /pet`                           | 200, JSON, echoed `id`/`name`/`status`/`category.name`/`photoUrls` |
| 3 | `getPetByIdReturnsCreatedPet` | `POST /pet` then `GET /pet/{id}`    | 200, JSON, fields match, `category` has `id`+`name`, `photoUrls` not null |
| 4 | `updatePetSuccessfully`     | `POST /pet` then `PUT /pet`           | 200, JSON, updated `name` and `status` reflected |
| 5 | `deletePetSuccessfully`     | `POST /pet` then `DELETE /pet/{id}`   | 200, response `code == 200`, `message == id` |
| 6 | `getStoreInventoryStructure`| `GET /store/inventory`                | 200, JSON, non-empty map, every value is an `Integer` |
| 7 | `findPetsByStatusSold`      | `GET /pet/findByStatus?status=sold`   | 200, JSON, every `status == "sold"` |

### Negative (3)

| #  | Test                             | Endpoint                          | Key assertions |
|----|----------------------------------|-----------------------------------|----------------|
| 8  | `getPetByNonExistentIdReturns404`| `GET /pet/9999999999`             | 404, JSON, `{code:1, type:"error", message:"Pet not found"}` |
| 9  | `getPetWithInvalidIdFormat`      | `GET /pet/not-a-number`           | 404, JSON, non-null `code` and `message` |
| 10 | `deleteNonExistentPetReturns404` | `DELETE /pet/9999999998`          | 404 |

## Issue Fixed During Execution

The first run failed 3/10 with `Expected: <987654321L> Actual: <987654321>`. Petstore returns
pet `id` as a JSON integer, which Rest-Assured's JsonPath deserializes to `Integer` when the
value fits in an `int`; Hamcrest's `equalTo` is type-strict, so `Long != Integer`.

Fix: changed the local `petId` variables in tests 2–4 from `long` to `int` so the
`equalTo(petId)` matcher compares `Integer` to `Integer`.

## Result

```
mvn test
...
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 10 tests pass against the live Petstore API in ~9 seconds.

## How to Run

From the project root:

```bash
mvn test
```
