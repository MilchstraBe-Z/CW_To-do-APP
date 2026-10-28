import { useEffect, useState } from 'react';

const API_URL = 'http://localhost:8080/api/todos';

function App() {
  // State remembers values between renders of this component.
  const [todos, setTodos] = useState([]);
  const [title, setTitle] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  // An empty dependency array makes this load run when the page first opens.
  useEffect(() => {
    async function start() {
      try {
        await loadTodos();
      } catch (error) {
        setError(error.message);
      } finally {
        setLoading(false);
      }
    }
    start();
  }, []);

  async function request(url, options = {}) {
    let response;
    try {
      response = await fetch(url, options);
    } catch {
      throw new Error('Cannot reach the server. Check that the backend is running, then try again.');
    }

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.message || 'The request failed. Please try again.');
    }

    // A successful DELETE has no JSON body to read.
    if (response.status === 204) {
      return;
    }
    return response.json();
  }

  async function loadTodos() {
    const data = await request(API_URL);
    setTodos(data);
  }

  async function addTodo(event) {
    event.preventDefault();
    if (title.trim() === '') {
      setError('Enter a task title.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      await request(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: title.trim() }),
      });
      await loadTodos();
      setTitle('');
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function updateTodo(todo, newTitle, completed) {
    await request(`${API_URL}/${todo.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: newTitle, completed: completed }),
    });
    await loadTodos();
  }

  async function toggleTodo(todo) {
    setBusy(true);
    setError('');
    try {
      await updateTodo(todo, todo.title, !todo.completed);
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }

  function startEditing(todo) {
    setEditingId(todo.id);
    setEditTitle(todo.title);
    setError('');
  }

  async function saveTodo(event, todo) {
    event.preventDefault();
    if (editTitle.trim() === '') {
      setError('Enter a task title.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      await updateTodo(todo, editTitle.trim(), todo.completed);
      setEditingId(null);
      setEditTitle('');
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function deleteTodo(todo) {
    setBusy(true);
    setError('');
    try {
      await request(`${API_URL}/${todo.id}`, { method: 'DELETE' });
      await loadTodos();
      if (editingId === todo.id) {
        setEditingId(null);
      }
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function refreshTodos() {
    setLoading(true);
    setError('');
    try {
      await loadTodos();
    } catch (error) {
      setError(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app">
      <h1>My To-do List</h1>
      <p className="intro">A simple place to keep track of your tasks.</p>

      <form className="add-form" onSubmit={addTodo}>
        <label htmlFor="new-task">New task</label>
        <div className="form-row">
          <input
            id="new-task"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="What do you need to do?"
            maxLength={255}
            required
            disabled={busy || loading}
          />
          <button type="submit" disabled={busy || loading}>Add task</button>
        </div>
      </form>

      {error && (
        <div className="error" role="alert">
          <p>{error}</p>
          <button onClick={refreshTodos} disabled={busy || loading}>Reload tasks</button>
        </div>
      )}

      {loading && <p role="status">Loading tasks...</p>}
      {busy && <p role="status">Saving changes...</p>}
      {!loading && !error && todos.length === 0 && <p>No tasks yet. Add your first task above.</p>}

      <ul className="task-list" aria-label="Tasks">
        {todos.map((todo) => (
          <li key={todo.id} className="task">
            {editingId === todo.id ? (
              <form className="edit-form" onSubmit={(event) => saveTodo(event, todo)}>
                <label htmlFor={`edit-${todo.id}`}>Edit task</label>
                <div className="form-row">
                  <input
                    id={`edit-${todo.id}`}
                    type="text"
                    value={editTitle}
                    onChange={(event) => setEditTitle(event.target.value)}
                    maxLength={255}
                    required
                    autoFocus
                    disabled={busy || loading}
                  />
                  <button type="submit" disabled={busy || loading}>Save</button>
                  <button type="button" onClick={() => setEditingId(null)} disabled={busy || loading}>
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <>
                <label className="task-label">
                  <input
                    type="checkbox"
                    checked={todo.completed}
                    onChange={() => toggleTodo(todo)}
                    disabled={busy || loading}
                  />
                  <span className={todo.completed ? 'completed' : ''}>{todo.title}</span>
                </label>
                <div className="task-actions">
                  <button onClick={() => startEditing(todo)} disabled={busy || loading}>Edit</button>
                  <button onClick={() => deleteTodo(todo)} disabled={busy || loading}>Delete</button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>
    </main>
  );
}

export default App;
