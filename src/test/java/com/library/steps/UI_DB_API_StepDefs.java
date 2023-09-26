package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UI_DB_API_StepDefs {

    /**
     * User story 01
     */
    String token;
    RequestSpecification reqSpecification;
    Response response;
    ValidatableResponse validatableResponse;

    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        token = LibraryAPI_Util.getToken(userType);

    }
    @Given("Accept header is {string}")
    public void accept_header_is(String acceptHeader) {
        reqSpecification = RestAssured.given().log().all()
                .header("x-library-token",token)
                .accept(acceptHeader);
    }
    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endPoint) {
        response = reqSpecification.when()
                .get(endPoint).prettyPeek();

        validatableResponse = response.then();
    }
    @Then("status code should be {int}")
    public void status_code_should_be(Integer statusCode) {
        validatableResponse.statusCode(statusCode);
    }
    @Then("Response Content type is {string}")
    public void response_content_type_is(String contentType) {
        validatableResponse.contentType(contentType);
    }
    @Then("{string} field should not be null")
    public void field_should_not_be_null(String field) {
        validatableResponse.body(field,is(notNullValue()));
    }

    /**
     * User story 02
     */
    String id;
    @Given("Path param is {string}")
    public void path_param_is(String idPathParam){
        reqSpecification.pathParam("id",Integer.parseInt(idPathParam));
        id = idPathParam;
    }
    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String idUser) {
        validatableResponse.body(idUser, is(id));
    }
    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> fields) {
        for (String each : fields) {
            validatableResponse.body(each,is(notNullValue()));
        }
    }

    /**
     * User story 03
     */
    //----Scenario-1-----
    Map<String,Object> randomBodyMap;
    //Map<String,Object> randomUserBodyMap;
    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String contentType) {
        reqSpecification.contentType(contentType);
    }
    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String randomBookBody) {
        Map<String,Object> requestBookBodyMap = new LinkedHashMap<>();
        //Map<String,Object> requestUserBodyMap = new LinkedHashMap<>();
        switch (randomBookBody){
            case "book":
                requestBookBodyMap = LibraryAPI_Util.getRandomBookMap();
                break;
            case "user":
                requestBookBodyMap = LibraryAPI_Util.getRandomUserMap();
                break;
            default:
                throw new IllegalArgumentException("Please check body data");
        }

        reqSpecification.formParams(requestBookBodyMap);
        randomBodyMap = requestBookBodyMap;
        //randomUserBodyMap = requestUserBodyMap;
    }
    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endPoint) {
        response = reqSpecification.when().post(endPoint).prettyPeek();
        validatableResponse = response.then();
    }
    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String path, String expectedValue) {
        validatableResponse.body(path, is(expectedValue));
    }

    //-----Scenario-2-----
    LoginPage loginPage = new LoginPage();
    BookPage bookPage = new BookPage();
    @Given("I logged in Library UI as {string}")
    public void i_logged_in_library_ui_as(String userType) {
        loginPage.login(userType);
    }
    @Given("I navigate to {string} page")
    public void i_navigate_to_page(String modulePage) {
        bookPage.navigateModule(modulePage);
    }
    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {

        //API part
        Map<String,Object> apiBookInfo = new LinkedHashMap<>();
        apiBookInfo = randomBodyMap;
        //apiBookInfo.remove("book_category_id");

        //UI part
        ////tr[@role='row']//a[@onclick='Books.edit_book("+response.path("book_id")+")']
        String bookId = response.path("book_id");

        bookPage.search.sendKeys((String) randomBodyMap.get("name"));
        BrowserUtil.waitFor(2);
        bookPage.editBookById(bookId).click();
        BrowserUtil.waitFor(2);

        //DB part
        DB_Util.runQuery("select * from books order by id desc;");
        Map<String, Object> dbBookInfo = DB_Util.getRowMap(1);

        //Assertions
        //API vs DB
        assertEquals(apiBookInfo.get("name"),dbBookInfo.get("name"));
        assertEquals(apiBookInfo.get("isbn"),dbBookInfo.get("isbn"));
        assertEquals(String.valueOf(apiBookInfo.get("year")),dbBookInfo.get("year"));
        assertEquals(apiBookInfo.get("author"),dbBookInfo.get("author"));
        assertEquals(apiBookInfo.get("description"),dbBookInfo.get("description"));

        //API vs UI
        assertEquals(apiBookInfo.get("name"),bookPage.bookName.getAttribute("value"));
        assertEquals(apiBookInfo.get("isbn"),bookPage.isbn.getAttribute("value"));
        assertEquals(String.valueOf(apiBookInfo.get("year")),bookPage.year.getAttribute("value"));
        assertEquals(apiBookInfo.get("author"),bookPage.author.getAttribute("value"));
        assertEquals(apiBookInfo.get("description"),bookPage.description.getAttribute("value"));

    }

    /**
     * User story 04
     */
    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {
        Map<String,Object> apiUserInfo = new LinkedHashMap<>();
        apiUserInfo=randomBodyMap;
        System.out.println("apiUserInfo = " + apiUserInfo);

        DB_Util.runQuery("select * from users where email = '"+randomBodyMap.get("email")+"'");
        //DB_Util.runQuery("select * from users order by id desc ;");
        Map<String, Object> dbUserInfo = DB_Util.getRowMap(1);
        dbUserInfo.remove("id");
        dbUserInfo.remove("image");
        dbUserInfo.remove("extra_data");
        dbUserInfo.remove("is_admin");
        dbUserInfo.put("password", ConfigurationReader.getProperty("librarian_password"));
        System.out.println("dbUserInfo = " + dbUserInfo);

        //assertEquals(apiUserInfo,dbUserInfo);
        assertEquals(apiUserInfo.get("full_name"),dbUserInfo.get("full_name"));
        assertEquals(apiUserInfo.get("email"),dbUserInfo.get("email"));
        assertEquals(apiUserInfo.get("password"),dbUserInfo.get("password"));
        assertEquals(String.valueOf(apiUserInfo.get("user_group_id")),dbUserInfo.get("user_group_id"));
        assertEquals(apiUserInfo.get("status"),dbUserInfo.get("status"));
        assertEquals(apiUserInfo.get("address"),dbUserInfo.get("address"));

    }
    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {
        loginPage.login((String) randomBodyMap.get("email"), (String) randomBodyMap.get("password"));
    }
    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {
        assertTrue(bookPage.createdUserName((String) randomBodyMap.get("full_name")).isDisplayed());
    }


    /**
     * User story 05
     */
    //String userToken;
    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {
        token = LibraryAPI_Util.getToken(email, password);

    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {
        reqSpecification.formParams("token",token);
    }


}
