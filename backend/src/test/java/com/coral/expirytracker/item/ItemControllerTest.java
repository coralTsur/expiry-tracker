package com.coral.expirytracker.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private ItemService service;

	ItemControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void getAll_returns200AndItemsInServiceOrder() throws Exception {
		when(service.findAll()).thenReturn(List.of(
				minimalItem(2L, "First", LocalDate.of(2026, 11, 1)),
				minimalItem(1L, "Second", LocalDate.of(2026, 12, 1))));

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(2))
				.andExpect(jsonPath("$[0].name").value("First"))
				.andExpect(jsonPath("$[1].id").value(1))
				.andExpect(jsonPath("$[1].name").value("Second"));
	}

	@Test
	void getAll_returnsEmptyArray_whenNoItems() throws Exception {
		when(service.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void getById_returns200WithAll11Fields() throws Exception {
		when(service.findById(1L)).thenReturn(new ItemResponse(
				1L,
				Category.GIFT_CARD,
				"BuyMe",
				LocalDate.of(2026, 11, 20),
				new BigDecimal("200"),
				new BigDecimal("150"),
				"1234",
				"Fox, Castro",
				true,
				14,
				false));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.category").value("GIFT_CARD"))
				.andExpect(jsonPath("$.name").value("BuyMe"))
				.andExpect(jsonPath("$.expirationDate").value("2026-11-20"))
				.andExpect(jsonPath("$.amount").value(200))
				.andExpect(jsonPath("$.currentAmount").value(150))
				.andExpect(jsonPath("$.lastFourDigits").value("1234"))
				.andExpect(jsonPath("$.acceptedStores").value("Fox, Castro"))
				.andExpect(jsonPath("$.reminderEnabled").value(true))
				.andExpect(jsonPath("$.daysBeforeReminder").value(14))
				.andExpect(jsonPath("$.redeemed").value(false));
	}

	@Test
	void getById_returnsNullOptionalFieldsAsNull() throws Exception {
		when(service.findById(1L)).thenReturn(minimalItem(1L, "Passport", LocalDate.of(2026, 11, 20)));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$.amount").hasJsonPath())
				.andExpect(jsonPath("$.amount").value(nullValue()))
				.andExpect(jsonPath("$.currentAmount").hasJsonPath())
				.andExpect(jsonPath("$.currentAmount").value(nullValue()))
				.andExpect(jsonPath("$.lastFourDigits").hasJsonPath())
				.andExpect(jsonPath("$.lastFourDigits").value(nullValue()))
				.andExpect(jsonPath("$.acceptedStores").hasJsonPath())
				.andExpect(jsonPath("$.acceptedStores").value(nullValue()))
				.andExpect(jsonPath("$.daysBeforeReminder").hasJsonPath())
				.andExpect(jsonPath("$.daysBeforeReminder").value(nullValue()));
	}

	@Test
	void getById_returns404WithErrorBody() throws Exception {
		when(service.findById(99L)).thenThrow(new ItemNotFoundException(99L));

		mockMvc.perform(get("/api/items/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("פריט 99 לא נמצא"))
				.andExpect(jsonPath("$.fieldErrors").doesNotHaveJsonPath());
	}

	@Test
	void expiringSoon_returns200AndList() throws Exception {
		when(service.findExpiringSoon()).thenReturn(List.of(minimalItem(1L, "Car test", LocalDate.of(2026, 11, 13))));

		mockMvc.perform(get("/api/items/expiring-soon"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Car test"))
				.andExpect(jsonPath("$[0].expirationDate").value("2026-11-13"));
	}

	@Test
	void expiringSoon_returnsEmptyArray_whenNoItems() throws Exception {
		when(service.findExpiringSoon()).thenReturn(List.of());

		mockMvc.perform(get("/api/items/expiring-soon"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void expiringSoon_isNotRoutedToGetById() throws Exception {
		when(service.findExpiringSoon()).thenReturn(List.of());

		mockMvc.perform(get("/api/items/expiring-soon"))
				.andExpect(status().isOk());

		verify(service).findExpiringSoon();
		verify(service, never()).findById(any());
	}

	// ---- POST /api/items ----

	private static final String ITEM_NOT_SAVED = "הפריט לא נשמר";

	private static final String VALID_JSON = """
			{
			  "category": "GIFT_CARD",
			  "name": "BuyMe",
			  "expirationDate": "2030-03-01",
			  "amount": 200,
			  "currentAmount": 150,
			  "lastFourDigits": "1234",
			  "acceptedStores": "Fox, Castro",
			  "reminderEnabled": true,
			  "daysBeforeReminder": 14
			}
			""";

	@Test
	void post_returns201WithSavedItem() throws Exception {
		when(service.create(any())).thenReturn(savedItem(1L));

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.category").value("GIFT_CARD"))
				.andExpect(jsonPath("$.name").value("BuyMe"))
				.andExpect(jsonPath("$.expirationDate").value("2030-03-01"))
				.andExpect(jsonPath("$.reminderEnabled").value(true))
				.andExpect(jsonPath("$.daysBeforeReminder").value(14))
				.andExpect(jsonPath("$.redeemed").value(false));
	}

	@Test
	void post_passesParsedBodyToService() throws Exception {
		when(service.create(any())).thenReturn(savedItem(1L));

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isCreated());

		ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
		verify(service).create(captor.capture());
		ItemRequest request = captor.getValue();
		assertThat(request.category()).isEqualTo(Category.GIFT_CARD);
		assertThat(request.name()).isEqualTo("BuyMe");
		assertThat(request.expirationDate()).isEqualTo(LocalDate.of(2030, 3, 1));
		assertThat(request.amount()).isEqualByComparingTo("200");
		assertThat(request.currentAmount()).isEqualByComparingTo("150");
		assertThat(request.lastFourDigits()).isEqualTo("1234");
		assertThat(request.acceptedStores()).isEqualTo("Fox, Castro");
		assertThat(request.reminderEnabled()).isTrue();
		assertThat(request.daysBeforeReminder()).isEqualTo(14);
		assertThat(request.redeemed()).isNull();
	}

	@Test
	void post_fieldsNotSent_reachServiceAsNull() throws Exception {
		when(service.create(any())).thenReturn(savedItem(1L));

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isCreated());

		ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
		verify(service).create(captor.capture());
		assertThat(captor.getValue())
				.isEqualTo(new ItemRequest(null, null, null, null, null, null, null, null, null, null));
	}

	@Test
	void post_ignoresIdInBody_andReturnsTheNewId() throws Exception {
		when(service.create(any())).thenReturn(savedItem(1L));
		String bodyWithId = VALID_JSON.replace("\"category\"", "\"id\": 999, \"category\"");

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(bodyWithId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1));

		verify(service).create(any());
	}

	@Test
	void post_returns400WithAllFieldErrors() throws Exception {
		when(service.create(any())).thenThrow(new InvalidItemException(Map.of(
				"name", "יש להזין שם",
				"expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי")));

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(4))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value(ITEM_NOT_SAVED))
				.andExpect(jsonPath("$.fieldErrors.length()").value(2))
				.andExpect(jsonPath("$.fieldErrors.name").value("יש להזין שם"))
				.andExpect(jsonPath("$.fieldErrors.expirationDate").value("התאריך כבר עבר, אנא הזיני תאריך עתידי"));
	}

	@Test
	void post_brokenJson_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody("{\"name\": \"BuyMe\",");
	}

	@Test
	void post_categoryNotInList_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody(VALID_JSON.replace("\"GIFT_CARD\"", "\"FOO\""));
	}

	@Test
	void post_dateInAnotherFormat_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody(VALID_JSON.replace("\"2030-03-01\"", "\"01/03/2030\""));
	}

	@Test
	void post_missingBody_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody("");
	}

	// spec section 3 and 5: daysBeforeReminder is a whole number. The spec has no field message for a fraction,
	// so the only spec-consistent answer is the unreadable-body 400. Silently truncating 7.5 to 7 would break the spec.
	@Test
	void post_fractionalDaysBeforeReminder_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody(VALID_JSON.replace("\"daysBeforeReminder\": 14", "\"daysBeforeReminder\": 7.5"));
	}

	// Jackson cannot read "" as a category, so it is an unreadable body, not a missing category.
	@Test
	void post_emptyCategory_returns400WithoutFieldErrors() throws Exception {
		assertUnreadableBody(VALID_JSON.replace("\"GIFT_CARD\"", "\"\""));
	}

	// Jackson reads "" as a null date, so the service gets the request and reports a missing expirationDate.
	@Test
	void post_emptyExpirationDate_reachesServiceAsMissingDate() throws Exception {
		when(service.create(any())).thenThrow(new InvalidItemException(Map.of("expirationDate", "יש להזין תאריך תוקף")));

		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
						.content(VALID_JSON.replace("\"2030-03-01\"", "\"\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(ITEM_NOT_SAVED))
				.andExpect(jsonPath("$.fieldErrors.length()").value(1))
				.andExpect(jsonPath("$.fieldErrors.expirationDate").value("יש להזין תאריך תוקף"));

		ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
		verify(service).create(captor.capture());
		assertThat(captor.getValue().expirationDate()).isNull();
	}

	private void assertUnreadableBody(String body) throws Exception {
		mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value(ITEM_NOT_SAVED))
				.andExpect(jsonPath("$.fieldErrors").doesNotHaveJsonPath());

		verify(service, never()).create(any());
	}

	// ---- PUT /api/items/{id} ----

	@Test
	void put_returns200WithUpdatedItem() throws Exception {
		when(service.update(eq(5L), any())).thenReturn(savedItem(5L));

		mockMvc.perform(put("/api/items/5").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.name").value("BuyMe"))
				.andExpect(jsonPath("$.expirationDate").value("2030-03-01"));
	}

	@Test
	void put_passesPathIdAndParsedBodyToService() throws Exception {
		when(service.update(eq(5L), any())).thenReturn(savedItem(5L));

		mockMvc.perform(put("/api/items/5").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isOk());

		ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
		verify(service).update(eq(5L), captor.capture());
		ItemRequest request = captor.getValue();
		assertThat(request.category()).isEqualTo(Category.GIFT_CARD);
		assertThat(request.name()).isEqualTo("BuyMe");
		assertThat(request.expirationDate()).isEqualTo(LocalDate.of(2030, 3, 1));
		assertThat(request.amount()).isEqualByComparingTo("200");
		assertThat(request.currentAmount()).isEqualByComparingTo("150");
		assertThat(request.reminderEnabled()).isTrue();
		assertThat(request.daysBeforeReminder()).isEqualTo(14);
		assertThat(request.redeemed()).isNull();
	}

	@Test
	void put_returns404WithErrorBody() throws Exception {
		when(service.update(eq(99L), any())).thenThrow(new ItemNotFoundException(99L));

		mockMvc.perform(put("/api/items/99").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("פריט 99 לא נמצא"))
				.andExpect(jsonPath("$.fieldErrors").doesNotHaveJsonPath());
	}

	@Test
	void put_returns400WithFieldErrors() throws Exception {
		when(service.update(eq(5L), any())).thenThrow(new InvalidItemException(Map.of(
				"currentAmount", "הסכום העדכני לא יכול להיות גדול מהסכום המקורי")));

		mockMvc.perform(put("/api/items/5").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value(ITEM_NOT_SAVED))
				.andExpect(jsonPath("$.fieldErrors.length()").value(1))
				.andExpect(jsonPath("$.fieldErrors.currentAmount")
						.value("הסכום העדכני לא יכול להיות גדול מהסכום המקורי"));
	}

	@Test
	void put_unreadableBody_returns400WithoutFieldErrors_andSkipsService() throws Exception {
		mockMvc.perform(put("/api/items/5").contentType(MediaType.APPLICATION_JSON)
						.content(VALID_JSON.replace("\"GIFT_CARD\"", "\"FOO\"")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value(ITEM_NOT_SAVED))
				.andExpect(jsonPath("$.fieldErrors").doesNotHaveJsonPath());

		verify(service, never()).update(any(), any());
	}

	private ItemResponse savedItem(Long id) {
		return new ItemResponse(id, Category.GIFT_CARD, "BuyMe", LocalDate.of(2030, 3, 1), new BigDecimal("200"),
				new BigDecimal("150"), "1234", "Fox, Castro", true, 14, false);
	}

	private ItemResponse minimalItem(Long id, String name, LocalDate expirationDate) {
		return new ItemResponse(id, Category.OTHER, name, expirationDate, null, null, null, null, false, null, false);
	}

}
