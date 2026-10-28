# Simple To-do App

A small course project using Java, Spring Boot, a REST API, MySQL, and React.
You can add, list, edit, complete, reopen, and delete tasks. Each task has only
an ID, a title, and a completed status.

## What you need

- JDK 21 or newer supported by Spring Boot 4.1.2 (Java 26 also works).
- Node.js 20.19+ or 22.12+ and npm.
- A local MySQL server, such as MySQL 8.4, running on port 3306.
- Two terminal windows: one for the backend and one for the frontend.

Maven does not need to be installed separately. The included Maven wrapper
downloads Maven the first time you run it. The first Maven and npm runs need
internet access.

## 1. Create the MySQL database

Open MySQL Workbench or sign in using the MySQL command line:

```powershell
mysql -u root -p
```

Run this SQL. Replace the example password with your own local password:

```sql
CREATE DATABASE todo_app CHARACTER SET utf8mb4;
CREATE USER 'todo_user'@'localhost' IDENTIFIED BY 'replace-with-your-local-password';
GRANT ALL PRIVILEGES ON todo_app.* TO 'todo_user'@'localhost';
```

Run this setup once. Hibernate creates the `todos` table when the backend starts.
There is no need to write a table creation script.

## 2. Start the backend

Open PowerShell in the project folder:

```powershell
cd backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.1'
$env:DB_USERNAME = 'todo_user'
$env:DB_PASSWORD = 'replace-with-your-local-password'
.\mvnw.cmd spring-boot:run
```

Change `JAVA_HOME` to the actual folder of your JDK. This also avoids accidentally
using an old Java installation from PATH. Environment variables apply to the
current terminal. Do not put real passwords in source files.

The default database URL is `jdbc:mysql://localhost:3306/todo_app`. If necessary,
set `$env:DB_URL` before starting the backend to use a different local port.

The API runs at `http://localhost:8080/api/todos`. Keep this terminal open.

## 3. Start the frontend

Open a second PowerShell window in the project folder:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open [http://localhost:5173](http://localhost:5173). Use `localhost`, because
the backend allows requests from that origin. The frontend uses port 5173
and will report an error if that port is already occupied.

Stop each server with Ctrl+C in its terminal.

## How the code works

```text
React page -> HTTP request -> TodoController -> TodoRepository -> MySQL
```

- `frontend/src/App.jsx` holds the form, task list, state, and `fetch` calls.
  `useState` remembers values and `useEffect` loads tasks when the page opens.
- `TodoController` accepts HTTP requests, checks titles, and calls the repository.
- `TodoRepository` extends `JpaRepository`. Spring supplies methods such as
  `findAll`, `save`, and `delete`, so we do not implement them ourselves.
- `Todo` is a Java entity. JPA maps its fields to columns in the `todos` table.
- MySQL stores tasks so they remain after refreshing the page or restarting Java.

The backend trims titles and accepts 1 to 255 characters after trimming.
New tasks always start incomplete. The page reloads the list after changes and
keeps entered text if a save fails.

## REST API

| Method | URL | Request body | Success |
| --- | --- | --- | --- |
| GET | `/api/todos` | None | 200, array ordered by ID |
| POST | `/api/todos` | `{"title":"Study Java"}` | 201, created task |
| PUT | `/api/todos/{id}` | `{"title":"Study Spring","completed":true}` | 200, updated task |
| DELETE | `/api/todos/{id}` | None | 204, no body |

PUT sends both the title and completed status. Invalid titles return 400.
Updating or deleting a missing ID returns 404.

Example response:

```json
{"id":1,"title":"Study Java","completed":false}
```

## Checks

From `backend/` with `JAVA_HOME` set:

```powershell
.\mvnw.cmd clean verify
```

The API tests exercise the controller and repository using a temporary H2
database. They do not need MySQL credentials and do not touch your MySQL tasks.
H2 is a test-only dependency, not a replacement for MySQL in the running app.

From `frontend/`:

```powershell
npm.cmd run build
```
