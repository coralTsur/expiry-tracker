package com.coral.expirytracker.item;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
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

	private ItemResponse minimalItem(Long id, String name, LocalDate expirationDate) {
		return new ItemResponse(id, Category.OTHER, name, expirationDate, null, null, null, null, false, null, false);
	}

}
