package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Lab task: 10 RestAssured tests against the Swagger Petstore API.
 * Base URL: https://petstore.swagger.io/v2
 *
 * Covers both positive and negative scenarios with validations of:
 *   - HTTP status code
 *   - Content-Type header
 *   - Data values
 *   - Data structure
 */
public class PetstoreApiTests {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://petstore.swagger.io";
        RestAssured.basePath = "/v2";
    }

    private Map<String, Object> samplePet(long id, String name, String status) {
        Map<String, Object> category = new HashMap<>();
        category.put("id", 1);
        category.put("name", "dogs");

        Map<String, Object> pet = new HashMap<>();
        pet.put("id", id);
        pet.put("category", category);
        pet.put("name", name);
        pet.put("photoUrls", new String[]{"https://example.com/photo.jpg"});
        pet.put("status", status);
        return pet;
    }

    // ---------------------------------------------------------------------
    // POSITIVE TESTS
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("1. GET /pet/findByStatus?status=available returns 200 and a JSON array")
    void findPetsByStatusAvailable() {
        given()
                .accept(ContentType.JSON)
                .queryParam("status", "available")
        .when()
                .get("/pet/findByStatus")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", isA(java.util.List.class))
                .body("size()", greaterThan(0))
                .body("status", everyItem(equalTo("available")));
    }

    @Test
    @DisplayName("2. POST /pet creates a new pet and returns the same fields")
    void createPetSuccessfully() {
        int petId = 987654321;
        Map<String, Object> pet = samplePet(petId, "Rex", "available");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(pet)
        .when()
                .post("/pet")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(petId))
                .body("name", equalTo("Rex"))
                .body("status", equalTo("available"))
                .body("category.name", equalTo("dogs"))
                .body("photoUrls", hasItem("https://example.com/photo.jpg"));
    }

    @Test
    @DisplayName("3. GET /pet/{id} retrieves a previously created pet with matching data")
    void getPetByIdReturnsCreatedPet() {
        int petId = 987654322;
        Map<String, Object> pet = samplePet(petId, "Buddy", "available");

        // create first
        given().contentType(ContentType.JSON).body(pet)
                .when().post("/pet")
                .then().statusCode(200);

        // then retrieve
        given()
                .accept(ContentType.JSON)
                .pathParam("petId", petId)
        .when()
                .get("/pet/{petId}")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(petId))
                .body("name", equalTo("Buddy"))
                .body("category", hasKey("id"))
                .body("category", hasKey("name"))
                .body("photoUrls", notNullValue());
    }

    @Test
    @DisplayName("4. PUT /pet updates an existing pet's fields")
    void updatePetSuccessfully() {
        int petId = 987654323;
        Map<String, Object> pet = samplePet(petId, "OldName", "available");

        given().contentType(ContentType.JSON).body(pet)
                .when().post("/pet")
                .then().statusCode(200);

        Map<String, Object> updated = samplePet(petId, "NewName", "sold");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updated)
        .when()
                .put("/pet")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(petId))
                .body("name", equalTo("NewName"))
                .body("status", equalTo("sold"));
    }

    @Test
    @DisplayName("5. DELETE /pet/{id} removes an existing pet")
    void deletePetSuccessfully() {
        long petId = 987654324L;
        Map<String, Object> pet = samplePet(petId, "ToDelete", "available");

        given().contentType(ContentType.JSON).body(pet)
                .when().post("/pet")
                .then().statusCode(200);

        given()
                .pathParam("petId", petId)
        .when()
                .delete("/pet/{petId}")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", equalTo(String.valueOf(petId)));
    }

    @Test
    @DisplayName("6. GET /store/inventory returns a JSON object with numeric counts")
    void getStoreInventoryStructure() {
        Response response = given()
                .accept(ContentType.JSON)
        .when()
                .get("/store/inventory")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        Map<String, Object> inventory = response.jsonPath().getMap("$");
        org.junit.jupiter.api.Assertions.assertFalse(inventory.isEmpty(),
                "Inventory map should not be empty");
        inventory.forEach((key, value) ->
                org.junit.jupiter.api.Assertions.assertTrue(value instanceof Integer,
                        "Inventory value for '" + key + "' should be an integer"));
    }

    @Test
    @DisplayName("7. GET /pet/findByStatus?status=sold returns only sold pets")
    void findPetsByStatusSold() {
        given()
                .accept(ContentType.JSON)
                .queryParam("status", "sold")
        .when()
                .get("/pet/findByStatus")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", isA(java.util.List.class))
                .body("status", everyItem(equalTo("sold")));
    }

    // ---------------------------------------------------------------------
    // NEGATIVE TESTS
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("8. GET /pet/{id} with a non-existent id returns 404 with error body")
    void getPetByNonExistentIdReturns404() {
        long missingId = 9_999_999_999L;

        given()
                .accept(ContentType.JSON)
                .pathParam("petId", missingId)
        .when()
                .get("/pet/{petId}")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo(1))
                .body("type", equalTo("error"))
                .body("message", equalTo("Pet not found"));
    }

    @Test
    @DisplayName("9. GET /pet/{id} with a non-numeric id returns 404")
    void getPetWithInvalidIdFormat() {
        given()
                .accept(ContentType.JSON)
                .pathParam("petId", "not-a-number")
        .when()
                .get("/pet/{petId}")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", notNullValue())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("10. DELETE /pet/{id} with a non-existent id returns 404")
    void deleteNonExistentPetReturns404() {
        long missingId = 9_999_999_998L;

        given()
                .pathParam("petId", missingId)
        .when()
                .delete("/pet/{petId}")
        .then()
                .statusCode(404);
    }
}
