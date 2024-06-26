package com.pacuss.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.pacuss.cashcard.model.CashCard;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static com.pacuss.cashcard.utility.Util.loadDetailsFromPropertiesFile;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {
	@Autowired
	TestRestTemplate restTemplate;

	// GET TEST (SINGLE)
	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	// GET TEST (SINGLE WITH UNKNOWN ID)
	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards/1000", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	// GET TEST (MULTIPLE)
	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00);
	}

	// GET TEST (MULTIPLE & PAGINATION & SORTING)
	@Test
	void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	// GET TEST (UNAUTHENTICATED USER)
	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("BAD-USER", "pass")
				.getForEntity("/cash_cards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("Chris", "BAD-PASSWORD")
				.getForEntity("/cash_cards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	// GET TEST (UNAUTHORIZED USER)
	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Chris", loadDetailsFromPropertiesFile("passwd-chris"))
				.getForEntity("/cash_cards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	// GET TEST (AUTHORIZED BUT CANNOT ACCESS OTHER USER'S DETAILS)
	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards/102", String.class); // kumar2's data
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// POST TEST
	@Test
		//@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00, null);
		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.postForEntity("/cash_cards", newCashCard, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity(locationOfNewCashCard, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	// UPDATE TEST
	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard(){
		CashCard cashCardUpdate = new CashCard(null, 90.00, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/99", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(90.00);
	}

	// UPDATE TEST (CARD DOES NOT EXIST)
	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/99999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// UPDATE TEST (UNAUTHENTICATED USER)
	@Test
	void shouldNotUpdateACashCardWhenNotAuthenticated() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("ChrisS", "12345") // User ChrisS does not exist
				.exchange("/cash_cards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	// UPDATE TEST (UNAUTHORIZED USER)
	@Test
	void shouldNotUpdateACashCardWhenNotAuthorized() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Chris", loadDetailsFromPropertiesFile("passwd-chris")) // User Chris exists but not authorized.
				.exchange("/cash_cards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	// UPDATE TEST (UPDATE CARD BELONGING TO ANOTHER USER)
	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard kumarCard = new CashCard(null, 333.33, null);
		HttpEntity<CashCard> request = new HttpEntity<>(kumarCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/102", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// DELETE TEST (CASH CARD THAT EXISTS)
	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCashCard(){
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.getForEntity("/cash_cards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	// DELETE TEST (CASH CARD THAT DOES NOT EXIST)
	@Test
	@DirtiesContext
	void shouldNotDeleteACashCardThatDoesNotExist(){
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/900009", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// DELETE TEST (UPDATE CARD BELONGING TO ANOTHER USER)
	@Test
	@DirtiesContext
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn(){
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Owen", loadDetailsFromPropertiesFile("passwd-owen"))
				.exchange("/cash_cards/102", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}


}


