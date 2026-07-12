const tasks = document.querySelector('#tasks');
const error = document.querySelector('#error');
const form = document.querySelector('#task-form');
const title = document.querySelector('#title');

async function api(path, options = {}) {
  const response = await fetch(`/api/tasks${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!response.ok) throw new Error('Request failed. Please retry.');
  return response.status === 204 ? null : response.json();
}

function render(items) {
  tasks.replaceChildren(...items.map(task => {
    const item = document.createElement('li');
    const label = document.createElement('span');
    label.textContent = task.title;
    if (task.completed) label.className = 'done';
    item.append(label);
    if (!task.completed) {
      const button = document.createElement('button');
      button.textContent = 'Complete';
      button.onclick = async () => { await api(`/${task.id}/complete`, { method: 'PATCH' }); load(); };
      item.append(button);
    }
    return item;
  }));
}

async function load() {
  try { error.textContent = ''; render(await api('')); }
  catch (e) { error.textContent = e.message; }
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    await api('', { method: 'POST', body: JSON.stringify({ title: title.value }) });
    title.value = '';
    load();
  } catch (e) { error.textContent = e.message; }
});

load();

