package com.example.todo;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring supplies the database methods, such as findAll, save, and delete.
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
