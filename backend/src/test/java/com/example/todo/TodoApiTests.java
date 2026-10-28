package com.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// H2 is used only for tests. Running the actual app still requires MySQL.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:todo_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class TodoApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void clearTestDatabase() {
        todoRepository.deleteAll();
    }

    @Test
    void createListEditCompleteReopenAndDelete() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"title\":\"  Study Java  \",\"completed\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study Java"))
                .andExpect(jsonPath("$.completed").value(false));

        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Study Spring Boot  \",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study Spring Boot"))
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study Spring Boot\",\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        assertEquals("Study Spring Boot", todoRepository.findById(id).orElseThrow().getTitle());

        mockMvc.perform(delete("/api/todos/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listTasksInIdOrder() throws Exception {
        mockMvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"First\"}"));
        mockMvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Second\"}"));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    void rejectMissingBlankAndOversizedTitles() throws Exception {
        String[] bodies = {
                "{}",
                "{\"title\":null}",
                "{\"title\":\"   \"}",
                "{\"title\":\"" + "a".repeat(256) + "\"}"
        };

        for (String body : bodies) {
            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, todoRepository.count());
    }

    @Test
    void acceptMaximumLengthAndKeepTaskAfterInvalidEdit() throws Exception {
        mockMvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(255) + "\"}"))
                .andExpect(status().isCreated());
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/todos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"completed\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/todos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(256) + "\",\"completed\":true}"))
                .andExpect(status().isBadRequest());

        Todo saved = todoRepository.findById(id).orElseThrow();
        assertEquals("a".repeat(255), saved.getTitle());
        assertEquals(false, saved.isCompleted());
    }

    @Test
    void missingTasksReturnNotFound() throws Exception {
        mockMvc.perform(put("/api/todos/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Missing\",\"completed\":false}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/todos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowFrontendCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/todos/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
