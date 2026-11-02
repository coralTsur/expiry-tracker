package com.coral.expirytracker;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coral.expirytracker.item.ItemController;
import com.coral.expirytracker.item.ItemService;

// No @Import(CorsConfig.class): @WebMvcTest includes WebMvcConfigurer beans in its slice,
// so these tests also prove that CorsConfig is picked up the normal way.
@WebMvcTest(ItemController.class)
class CorsConfigTest {

	private static final String FRONTEND_ORIGIN = "http://localhost:5173";
	private static final String OTHER_ORIGIN = "http://evil.example.com";

	private final MockMvc mockMvc;

	@MockitoBean
	private ItemService service;

	CorsConfigTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void preflight_fromFrontendOrigin_returns200WithAllowOriginAndMethods() throws Exception {
		mockMvc.perform(options("/api/items")
						.header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")));
	}

	@Test
	void preflight_forPatchRedeem_fromFrontendOrigin_isAllowed() throws Exception {
		mockMvc.perform(options("/api/items/1/redeem")
						.header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
	}

	@Test
	void preflight_withContentTypeHeader_isAllowed() throws Exception {
		mockMvc.perform(options("/api/items")
						.header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						containsStringIgnoringCase("content-type")));
	}

	@Test
	void get_fromFrontendOrigin_returnsAllowOriginHeader() throws Exception {
		when(service.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/items")
						.header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));
	}

	@Test
	void preflight_fromOtherOrigin_isRejectedWith403() throws Exception {
		mockMvc.perform(options("/api/items")
						.header(HttpHeaders.ORIGIN, OTHER_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(content().string("Invalid CORS request"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void get_fromOtherOrigin_isRejectedWith403AndServiceNotCalled() throws Exception {
		mockMvc.perform(get("/api/items")
						.header(HttpHeaders.ORIGIN, OTHER_ORIGIN))
				.andExpect(status().isForbidden())
				.andExpect(content().string("Invalid CORS request"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

		verify(service, never()).findAll();
	}

	@Test
	void get_withoutOriginHeader_isNotAffectedByCors() throws Exception {
		when(service.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
