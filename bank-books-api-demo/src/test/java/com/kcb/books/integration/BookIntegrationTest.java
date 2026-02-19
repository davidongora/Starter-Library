package com.kcb.books.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcb.books.dto.BookRequestDto;
import com.kcb.books.dto.BookResponseDto;
import com.kcb.books.entity.Book;
import com.kcb.books.repository.BookRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test for the Books API.
 *
 * <p>Tests:
 * <ul>
 *   <li>All CRUD endpoints end-to-end with H2 database</li>
 *   <li>Validation error responses</li>
 *   <li><strong>Masking behavior:</strong> verifies that the database stores unmasked values
 *       even after the service layer logs masked representations</li>
 * </ul>
 */
@SpringBootTest(classes = com.kcb.books.BankBooksApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Books API Integration Tests")
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private static final String BASE_URL = "/books";

    private BookRequestDto validRequest() {
        return BookRequestDto.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .email("robert@example.com")
                .phoneNumber("0712345678")
                .publisher("Prentice Hall")
                .build();
    }

    /** Helper to create a book and return its generated ID */
    private Long createBookAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        BookResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BookResponseDto.class);
        return response.getId();
    }

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    // ---- CREATE ----

    @Test
    @DisplayName("POST /books – should create book and return 201")
    void createBook_shouldReturn201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.author", is("Robert C. Martin")))
                // - API response must show unmasked values (database truth)
                .andExpect(jsonPath("$.email", is("robert@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("0712345678")));
    }

    // ---- MASKING INTEGRATION TEST ----

    @Test
    @DisplayName("Masking: database must store unmasked values after service logging")
    void maskingBehavior_databaseMustRemainUnmasked() throws Exception {
        // Create a book with sensitive data
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());

        // - Core masking test: verify the database stores the full unmasked values
        // (The service layer logs masked versions, but persistence must be unchanged)
        Book dbBook = bookRepository.findAll().get(0);

        assertThat(dbBook.getEmail())
                .as("Email in database must be unmasked")
                .isEqualTo("robert@example.com");

        assertThat(dbBook.getPhoneNumber())
                .as("Phone number in database must be unmasked")
                .isEqualTo("0712345678");

        // - API response must return unmasked values
        mockMvc.perform(get(BASE_URL + "/" + dbBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("robert@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("0712345678")));
    }

    // ---- GET ALL ----

    @Test
    @DisplayName("GET /books – should return empty list when no books")
    void getAllBooks_emptyList() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /books – should return list with created books")
    void getAllBooks_withData() throws Exception {
        createBookAndGetId();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Clean Code")));
    }

    // ---- GET BY ID ----

    @Test
    @DisplayName("GET /books/{id} – should return book by ID")
    void getBookById_shouldReturnBook() throws Exception {
        Long id = createBookAndGetId();

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.title", is("Clean Code")));
    }

    @Test
    @DisplayName("GET /books/{id} – should return 404 when book not found")
    void getBookById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999"))
                .andExpect(status().isNotFound());
    }

    // ---- UPDATE ----

    @Test
    @DisplayName("PUT /books/{id} – should update book")
    void updateBook_shouldUpdate() throws Exception {
        Long id = createBookAndGetId();

        BookRequestDto updateRequest = BookRequestDto.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .email("bob@example.com")
                .phoneNumber("0799999999")
                .publisher("Prentice Hall")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Clean Architecture")))
                .andExpect(jsonPath("$.email", is("bob@example.com")));
    }

    @Test
    @DisplayName("PUT /books/{id} – should return 404 when book not found")
    void updateBook_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE ----

    @Test
    @DisplayName("DELETE /books/{id} – should delete book and return 204")
    void deleteBook_shouldDelete() throws Exception {
        Long id = createBookAndGetId();

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /books/{id} – should return 404 when book not found")
    void deleteBook_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/99999"))
                .andExpect(status().isNotFound());
    }

    // ---- VALIDATION ----

    @Test
    @DisplayName("POST /books – should return 400 when title is blank")
    void createBook_shouldReturn400_whenTitleBlank() throws Exception {
        BookRequestDto invalid = BookRequestDto.builder()
                .title("")
                .author("Author")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /books – should return 400 when email is invalid")
    void createBook_shouldReturn400_whenEmailInvalid() throws Exception {
        BookRequestDto invalid = BookRequestDto.builder()
                .title("Title")
                .author("Author")
                .email("not-an-email")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
