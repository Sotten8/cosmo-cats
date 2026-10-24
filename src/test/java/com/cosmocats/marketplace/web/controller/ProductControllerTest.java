package com.cosmocats.marketplace.web.controller;

import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.web.mapper.ProductMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.cosmocats.marketplace.support.TestData.TOYS;
import static com.cosmocats.marketplace.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductMapperImpl.class)
class ProductControllerTest {

    private static final String BASE = "/api/v1/products";
    private static final String ERROR_BASE = "https://cosmo-cats.market/errors/";
    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ProductService productService;

    // Helpers

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Star-Dust Yarn Ball");
        body.put("description", "A ball of yarn that floats.");
        body.put("price", new BigDecimal("19.99"));
        body.put("currency", "GCR");
        body.put("stock", 10);
        body.put("categoryId", TOYS);
        body.put("sellerEmail", "seller@cosmo-cats.market");
        return body;
    }

    private static Map<String, Object> validBodyWith(String field, Object value) {
        Map<String, Object> body = validBody();
        body.put(field, value);
        return body;
    }

    private MockHttpServletRequestBuilder write(HttpMethod method) {
        String url = method == HttpMethod.POST ? BASE : BASE + "/" + ID;
        return request(method, url).contentType(MediaType.APPLICATION_JSON);
    }

    private ResultActions send(HttpMethod method, Object body) throws Exception {
        return mockMvc.perform(write(method).content(objectMapper.writeValueAsString(body)));
    }

    private static void assertProblem(ResultActions result, int status, String typeSlug) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + typeSlug))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty());
    }

    private static void assertViolation(ResultActions result, String field, String code) throws Exception {
        assertProblem(result, 400, "validation");
        result.andExpect(jsonPath("$.errors[?(@.field=='" + field + "')].code", contains(code)));
    }

    // GET /products

    @Nested
    class ListProducts {

        @Test
        void returnsPagedEnvelopeWithDefaults() throws Exception {
            Product product = product(ID, "Star-Dust Yarn Ball");
            when(productService.findAll(0, 10)).thenReturn(new PageResult<>(List.of(product), 0, 10, 1));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Star-Dust Yarn Ball"))
                    .andExpect(jsonPath("$.content[0].price").value(19.99))
                    .andExpect(jsonPath("$.content[0].currency").value("GCR"))
                    .andExpect(jsonPath("$.content[0].sellerEmail").doesNotExist())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void passesPageAndSizeToService() throws Exception {
            when(productService.findAll(2, 5)).thenReturn(new PageResult<>(List.of(), 2, 5, 12));

            mockMvc.perform(get(BASE).param("page", "2").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        void acceptsBoundarySizes() throws Exception {
            when(productService.findAll(0, 1)).thenReturn(new PageResult<>(List.of(), 0, 1, 0));
            when(productService.findAll(0, 100)).thenReturn(new PageResult<>(List.of(), 0, 100, 0));

            mockMvc.perform(get(BASE).param("size", "1")).andExpect(status().isOk());
            mockMvc.perform(get(BASE).param("size", "100")).andExpect(status().isOk());
        }

        @ParameterizedTest
        @MethodSource("com.cosmocats.marketplace.web.controller.ProductControllerTest#invalidPaging")
        void rejectsInvalidPaging(String param, String value) throws Exception {
            ResultActions result = mockMvc.perform(get(BASE).param(param, value));

            assertProblem(result, 400, "validation");
            result.andExpect(jsonPath("$.errors[0].field").value(param));
            verifyNoInteractions(productService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"page", "size"})
        void rejectsNonNumericPaging(String param) throws Exception {
            ResultActions result = mockMvc.perform(get(BASE).param(param, "abc"));

            assertViolation(result, param, "TypeMismatch");
            result.andExpect(jsonPath("$.errors[0].message").value("must be a valid integer"));
        }
    }

    static Stream<Arguments> invalidPaging() {
        return Stream.of(
                Arguments.of("page", "-1"),
                Arguments.of("size", "0"),
                Arguments.of("size", "-5"),
                Arguments.of("size", "101"));
    }

    // GET /products/{id}

    @Nested
    class GetProduct {

        @Test
        void returnsProduct() throws Exception {
            when(productService.findById(ID)).thenReturn(product(ID, "Comet Catnip"));

            mockMvc.perform(get(BASE + "/" + ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.name").value("Comet Catnip"))
                    .andExpect(jsonPath("$.categoryId").value(TOYS.toString()))
                    .andExpect(jsonPath("$.stock").value(10))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        void returns404WhenProductDoesNotExist() throws Exception {
            when(productService.findById(ID)).thenThrow(new ProductNotFoundException(ID));

            ResultActions result = mockMvc.perform(get(BASE + "/" + ID));

            assertProblem(result, 404, "not-found");
            result.andExpect(jsonPath("$.detail").value(containsString(ID.toString())))
                    .andExpect(jsonPath("$.instance").value(BASE + "/" + ID));
        }

        @Test
        void returns400ForMalformedUuid() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE + "/not-a-uuid"));

            assertViolation(result, "id", "TypeMismatch");
            result.andExpect(jsonPath("$.errors[0].message").value("must be a valid UUID"));
            verifyNoInteractions(productService);
        }
    }

    // POST /products

    @Nested
    class CreateProduct {

        @Test
        void createsProductAndReturns201WithLocation() throws Exception {
            when(productService.create(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            ResultActions result = send(HttpMethod.POST, validBody());

            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(BASE + "/")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Star-Dust Yarn Ball"))
                    .andExpect(jsonPath("$.price").value(19.99))
                    .andExpect(jsonPath("$.currency").value("GCR"))
                    .andExpect(jsonPath("$.stock").value(10))
                    .andExpect(jsonPath("$.categoryId").value(TOYS.toString()));
            String id = objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                    .get("id").asText();
            result.andExpect(header().string("Location", endsWith(BASE + "/" + id)));
        }

        @Test
        void trimsNameBeforeHandingItToTheService() throws Exception {
            when(productService.create(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            send(HttpMethod.POST, validBodyWith("name", "  Star Yarn  ")).andExpect(status().isCreated());

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productService).create(captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("Star Yarn");
        }

        @Test
        void acceptsMinimalBodyWithoutOptionalFields() throws Exception {
            when(productService.create(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
            Map<String, Object> body = validBody();
            body.remove("description");
            body.remove("sellerEmail");

            send(HttpMethod.POST, body).andExpect(status().isCreated());
        }

        @Test
        void acceptsBoundaryValues() throws Exception {
            when(productService.create(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
            Map<String, Object> body = validBody();
            body.put("name", "star " + "a".repeat(95));
            body.put("price", new BigDecimal("1000000.00"));
            body.put("stock", 100_000);
            body.put("description", "d".repeat(1000));

            send(HttpMethod.POST, body).andExpect(status().isCreated());
        }

        @Test
        void returns409WhenNameIsAlreadyTaken() throws Exception {
            when(productService.create(any(Product.class))).thenThrow(new DuplicateProductNameException("Star"));

            assertProblem(send(HttpMethod.POST, validBody()), 409, "conflict");
        }

        @Test
        void returns400WhenCategoryDoesNotExist() throws Exception {
            when(productService.create(any(Product.class))).thenThrow(new CategoryNotFoundException(TOYS));

            assertViolation(send(HttpMethod.POST, validBody()), "categoryId", "CategoryNotFound");
        }

        @Test
        void returns500ProblemForUnexpectedErrors() throws Exception {
            when(productService.create(any(Product.class))).thenThrow(new IllegalStateException("boom"));

            ResultActions result = send(HttpMethod.POST, validBody());

            assertProblem(result, 500, "internal");
            result.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(containsString("boom"))));
        }
    }

    // PUT /products/{id}

    @Nested
    class UpdateProduct {

        @Test
        void updatesProduct() throws Exception {
            when(productService.update(eq(ID), any(Product.class)))
                    .thenAnswer(inv -> product(ID, ((Product) inv.getArgument(1)).name()));

            send(HttpMethod.PUT, validBodyWith("name", "Comet Catnip"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.name").value("Comet Catnip"));
        }

        @Test
        void returns404WhenProductDoesNotExist() throws Exception {
            when(productService.update(eq(ID), any(Product.class))).thenThrow(new ProductNotFoundException(ID));

            assertProblem(send(HttpMethod.PUT, validBody()), 404, "not-found");
        }

        @Test
        void returns409WhenNameIsAlreadyTaken() throws Exception {
            when(productService.update(eq(ID), any(Product.class)))
                    .thenThrow(new DuplicateProductNameException("Star"));

            assertProblem(send(HttpMethod.PUT, validBody()), 409, "conflict");
        }

        @Test
        void returns400ForMalformedUuid() throws Exception {
            ResultActions result = mockMvc.perform(request(HttpMethod.PUT, BASE + "/nope")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validBody())));

            assertViolation(result, "id", "TypeMismatch");
            verifyNoInteractions(productService);
        }
    }

    // DELETE /products/{id}

    @Nested
    class DeleteProduct {

        @Test
        void returns204() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(productService).delete(ID);
        }

        @Test
        void returns404WhenProductDoesNotExist() throws Exception {
            org.mockito.Mockito.doThrow(new ProductNotFoundException(ID)).when(productService).delete(ID);

            assertProblem(mockMvc.perform(delete(BASE + "/" + ID)), 404, "not-found");
        }

        @Test
        void returns400ForMalformedUuid() throws Exception {
            assertViolation(mockMvc.perform(delete(BASE + "/123")), "id", "TypeMismatch");
        }
    }

    // Body validation (POST and PUT share the same rules)

    static Stream<Arguments> invalidFields() {
        Stream<Arguments> cases = Stream.of(
                // name
                Arguments.of("name", null, "NotBlank"),
                Arguments.of("name", "", "NotBlank"),
                Arguments.of("name", "   ", "NotBlank"),
                Arguments.of("name", "Fluffy Ball", "CosmicWordCheck"),
                Arguments.of("name", "Starship", "CosmicWordCheck"),
                Arguments.of("name", "star " + "a".repeat(100), "Size"),
                // description
                Arguments.of("description", "d".repeat(1001), "Size"),
                // price
                Arguments.of("price", null, "NotNull"),
                Arguments.of("price", new BigDecimal("0"), "DecimalMin"),
                Arguments.of("price", new BigDecimal("-5.00"), "DecimalMin"),
                Arguments.of("price", new BigDecimal("1000000.01"), "DecimalMax"),
                Arguments.of("price", new BigDecimal("1.234"), "Digits"),
                // currency
                Arguments.of("currency", null, "NotNull"),
                Arguments.of("currency", "gcr", "Pattern"),
                Arguments.of("currency", "GC", "Pattern"),
                Arguments.of("currency", "GCRR", "Pattern"),
                // stock
                Arguments.of("stock", null, "NotNull"),
                Arguments.of("stock", -1, "PositiveOrZero"),
                Arguments.of("stock", 100_001, "Max"),
                // categoryId
                Arguments.of("categoryId", null, "NotNull"),
                // sellerEmail
                Arguments.of("sellerEmail", "not-an-email", "Email"),
                // wrong JSON types
                Arguments.of("stock", "many", "TypeMismatch"),
                Arguments.of("price", "free", "TypeMismatch"),
                Arguments.of("categoryId", "not-a-uuid", "TypeMismatch"));
        return cases.flatMap(c -> Stream.of(HttpMethod.POST, HttpMethod.PUT)
                .map(m -> Arguments.of(m, c.get()[0], c.get()[1], c.get()[2])));
    }

    @ParameterizedTest(name = "{0} with {1}={2} -> {3}")
    @MethodSource("invalidFields")
    void rejectsInvalidField(HttpMethod method, String field, Object value, String code) throws Exception {
        ResultActions result = send(method, validBodyWith(field, value));

        assertViolation(result, field, code);
        result.andExpect(jsonPath("$.instance").value(method == HttpMethod.POST ? BASE : BASE + "/" + ID));
        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void reportsAllInvalidFieldsAtOnce(String methodName) throws Exception {
        Map<String, Object> body = validBody();
        body.put("name", "");
        body.put("price", new BigDecimal("-1"));
        body.put("stock", -3);

        ResultActions result = send(HttpMethod.valueOf(methodName), body);

        assertProblem(result, 400, "validation");
        result.andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*].field", contains("name", "price", "stock")))
                .andExpect(jsonPath("$.detail").value(containsString("Field 'price' must be greater than 0")));
        verifyNoInteractions(productService);
    }

    @Test
    void reportsOnlyOneViolationPerField() throws Exception {
        // empty name violates both @NotBlank and @Size(min = 3)
        ResultActions result = send(HttpMethod.POST, validBodyWith("name", ""));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='name')]", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].code").value("NotBlank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsUnknownFields(String methodName) throws Exception {
        ResultActions result = send(HttpMethod.valueOf(methodName), validBodyWith("color", "pink"));

        assertViolation(result, "color", "UnknownField");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMalformedJson(String methodName) throws Exception {
        ResultActions result = mockMvc.perform(write(HttpMethod.valueOf(methodName)).content("{\"name\": "));

        assertProblem(result, 400, "malformed-request");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMissingBody(String methodName) throws Exception {
        ResultActions result = mockMvc.perform(write(HttpMethod.valueOf(methodName)));

        assertProblem(result, 400, "malformed-request");
        verifyNoInteractions(productService);
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        ResultActions result = mockMvc.perform(request(HttpMethod.POST, BASE)
                .contentType(MediaType.TEXT_PLAIN)
                .content("name=star"));

        assertProblem(result, 415, "unsupported-media-type");
        verifyNoInteractions(productService);
    }

    @Test
    void rejectsUnsupportedHttpMethod() throws Exception {
        ResultActions result = mockMvc.perform(request(HttpMethod.PATCH, BASE + "/" + ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        assertProblem(result, 405, "method-not-allowed");
    }
}
