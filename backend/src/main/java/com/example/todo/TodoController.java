package com.example.todo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "http://localhost:5173")
public class TodoController {

    private final TodoRepository todoRepository;

    // Spring passes a repository into this constructor automatically.
    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping
    public List<Todo> getTodos() {
        return todoRepository.findAll(Sort.by("id").ascending());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo addTodo(@RequestBody Todo request) {
        String title = checkTitle(request.getTitle());

        // Create a new object so the client cannot choose an ID or initial status.
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }

    @PutMapping("/{id}")
    public Todo updateTodo(@PathVariable Long id, @RequestBody Todo request) {
        Todo todo = findTodo(id);
        String title = checkTitle(request.getTitle());

        todo.setTitle(title);
        todo.setCompleted(request.isCompleted());
        return todoRepository.save(todo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(@PathVariable Long id) {
        Todo todo = findTodo(id);
        todoRepository.delete(todo);
    }

    private Todo findTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found."));
    }

    private String checkTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a task title.");
        }

        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Task titles must be 255 characters or fewer.");
        }
        return trimmedTitle;
    }
}
