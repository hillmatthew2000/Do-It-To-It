'use strict';

// Relative so the UI keeps working when the app is served under a context path.
const API = 'api/todos';

const STATUS_LABELS = { PENDING: 'To do', IN_PROGRESS: 'In progress', COMPLETED: 'Done' };
const FILTERS = ['', 'PENDING', 'IN_PROGRESS', 'COMPLETED'];
const SORTS = ['OLDEST', 'NEWEST', 'PRIORITY', 'DUE_DATE', 'DESCRIPTION'];
const VIEW_KEY = 'view';
// Controls inside a task row that keep focus when the list is re-rendered.
const FOCUSABLE_CONTROLS = ['check__input', 'task__edit', 'task__delete'];

const $ = (selector) => document.querySelector(selector);

const el = {
  addForm: $('#add-form'),
  addError: $('#add-error'),
  newDescription: $('#new-description'),
  newPriority: $('#new-priority'),
  newDue: $('#new-due'),
  search: $('#search'),
  statusFilter: $('#status-filter'),
  sort: $('#sort'),
  clearCompleted: $('#clear-completed'),
  list: $('#task-list'),
  empty: $('#empty-state'),
  emptyTitle: $('#empty-title'),
  emptyText: $('#empty-text'),
  loadError: $('#load-error'),
  loadErrorText: $('#load-error-text'),
  retry: $('#retry'),
  progressBar: $('#progress-bar'),
  progressFill: $('#progress-fill'),
  progressSummary: $('#progress-summary'),
  statPending: $('#stat-pending'),
  statInProgress: $('#stat-in-progress'),
  statCompleted: $('#stat-completed'),
  statOverdue: $('#stat-overdue'),
  toasts: $('#toasts'),
  confirmDialog: $('#confirm-dialog'),
  confirmTitle: $('#confirm-title'),
  confirmText: $('#confirm-text'),
  confirmOk: $('#confirm-ok'),
  importButton: $('#import-button'),
  importFile: $('#import-file'),
  shortcutsDialog: $('#shortcuts-dialog'),
  shortcutsButton: $('#shortcuts-button'),
  themeButton: $('#theme-button'),
  taskTemplate: $('#task-template'),
  editTemplate: $('#edit-template'),
};

const state = {
  tasks: [],
  status: '',
  query: '',
  sort: 'OLDEST',
  editingId: null,
  activeId: null,
};

// ---------------------------------------------------------------------------
// API
// ---------------------------------------------------------------------------

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }

  // The task was changed somewhere else, such as another tab or the console, since this page loaded
  // it: 412 for a stale If-Match, 409 if two saves raced each other.
  get changedElsewhere() {
    return this.status === 412 || this.status === 409;
  }

  get notFound() {
    return this.status === 404;
  }
}

// `version` makes a change conditional: the server refuses it with 412 if the task has moved on.
async function request(path, { method = 'GET', body, version } = {}) {
  const init = { method, headers: { Accept: 'application/json' } };
  if (body !== undefined) {
    init.headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }
  if (version !== undefined) {
    init.headers['If-Match'] = `"${version}"`;
  }
  let response;
  try {
    response = await fetch(path, init);
  } catch {
    throw new ApiError('Cannot reach the server. Check that Do-It-To-It is still running.', 0);
  }
  if (!response.ok) {
    throw new ApiError(await problemMessage(response), response.status);
  }
  return response.status === 204 ? null : response.json();
}

// Errors are RFC 9457 problem details; fall back to the HTTP status text.
async function problemMessage(response) {
  try {
    const problem = await response.json();
    return problem.detail || problem.title || response.statusText;
  } catch {
    return response.statusText || `Request failed (${response.status})`;
  }
}

const api = {
  list: ({ status, query, sort }) => {
    const params = new URLSearchParams({ sort });
    if (status) params.set('status', status);
    if (query) params.set('q', query);
    return request(`${API}?${params}`);
  },
  get: (id) => request(`${API}/${id}`),
  stats: () => request(`${API}/stats`),
  create: (task) => request(API, { method: 'POST', body: task }),
  // These take the task as last loaded, so that they only apply if nobody has changed it since.
  replace: ({ id, version }, task) => request(`${API}/${id}`, { method: 'PUT', body: task, version }),
  patch: ({ id, version }, changes) => request(`${API}/${id}`, { method: 'PATCH', body: changes, version }),
  remove: ({ id, version }) => request(`${API}/${id}`, { method: 'DELETE', version }),
  clearCompleted: () => request(`${API}/completed`, { method: 'DELETE' }),
  importTasks: (data) => request(`${API}/import`, { method: 'POST', body: data }),
};

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

let listRequest = 0;

async function loadTasks({ showSkeleton = false } = {}) {
  const requestId = ++listRequest;
  if (showSkeleton) renderSkeleton();
  el.list.setAttribute('aria-busy', 'true');
  try {
    const tasks = await api.list(state);
    if (requestId !== listRequest) return; // a newer search or filter has been issued
    state.tasks = tasks;
    el.loadError.hidden = true;
    renderTasks();
  } catch (error) {
    if (requestId !== listRequest) return;
    el.loadErrorText.textContent = `Could not load tasks. ${error.message}`;
    el.loadError.hidden = false;
    if (showSkeleton) {
      el.list.replaceChildren();
      el.empty.hidden = true;
    }
  } finally {
    if (requestId === listRequest) el.list.setAttribute('aria-busy', 'false');
  }
}

async function loadStats() {
  try {
    renderStats(await api.stats());
  } catch {
    el.progressSummary.textContent = 'Progress unavailable';
  }
}

function refresh() {
  return Promise.all([loadTasks(), loadStats()]);
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

function renderStats(stats) {
  const total = stats.totalTasks;
  const done = stats.completedTasks;
  const percent = total === 0 ? 0 : Math.round((done / total) * 100);

  el.progressFill.style.width = `${percent}%`;
  el.progressBar.setAttribute('aria-valuenow', String(percent));
  el.progressBar.setAttribute('aria-valuetext', `${done} of ${total} tasks done`);
  el.progressSummary.textContent =
    total === 0 ? 'No tasks yet' : `${done} of ${total} done · ${percent}%`;

  el.statPending.textContent = stats.pendingTasks;
  el.statInProgress.textContent = stats.inProgressTasks;
  el.statCompleted.textContent = done;
  el.statOverdue.textContent = stats.overdueTasks;
  el.statOverdue.parentElement.classList.toggle('has-items', stats.overdueTasks > 0);

  el.clearCompleted.disabled = done === 0;
}

function renderSkeleton() {
  const rows = Array.from({ length: 3 }, () => {
    const li = document.createElement('li');
    li.className = 'task task--skeleton';
    li.setAttribute('aria-hidden', 'true');
    li.innerHTML =
      '<span class="skeleton skeleton--box"></span>' +
      '<div class="task__body"><span class="skeleton skeleton--title"></span>' +
      '<span class="skeleton skeleton--meta"></span></div>';
    return li;
  });
  el.list.replaceChildren(...rows);
  el.empty.hidden = true;
}

function renderTasks() {
  const hadFocus = el.list.contains(document.activeElement);
  const focusedControl = hadFocus ? FOCUSABLE_CONTROLS.find((name) => document.activeElement.classList.contains(name)) : null;

  if (!state.tasks.some((task) => task.id === state.activeId)) {
    state.activeId = state.tasks[0]?.id ?? null;
  }
  el.list.replaceChildren(...state.tasks.map(renderTask));
  renderEmptyState();

  if (hadFocus) {
    restoreFocus(focusedControl);
  }
}

function renderTask(task) {
  if (task.id === state.editingId) {
    return renderEditor(task);
  }
  const row = el.taskTemplate.content.firstElementChild.cloneNode(true);
  const done = task.status === 'COMPLETED';
  row.dataset.id = task.id;
  row.tabIndex = task.id === state.activeId ? 0 : -1;
  row.classList.toggle('is-completed', done);

  const checkbox = row.querySelector('.check__input');
  checkbox.checked = done;
  checkbox.tabIndex = -1;
  checkbox.setAttribute('aria-label', done ? `Mark "${task.description}" as not done` : `Mark "${task.description}" as done`);

  row.querySelector('.task__description').textContent = task.description;
  const badges = badgesFor(task);
  row.querySelector('.task__meta').append(...badges);
  // Read the whole task as one phrase when the row itself has focus.
  row.setAttribute(
    'aria-label',
    [task.description, STATUS_LABELS[task.status], ...badges.map((b) => b.textContent)].join(', '),
  );

  const edit = row.querySelector('.task__edit');
  edit.tabIndex = -1;
  edit.setAttribute('aria-label', `Edit "${task.description}"`);
  edit.title = 'Edit (E)';
  const remove = row.querySelector('.task__delete');
  remove.tabIndex = -1;
  remove.setAttribute('aria-label', `Delete "${task.description}"`);
  remove.title = 'Delete (Del)';
  return row;
}

function badgesFor(task) {
  const badges = [];
  if (task.status === 'IN_PROGRESS') {
    badges.push(badge('In progress', 'badge--progress'));
  }
  if (task.priority === 'HIGH') {
    badges.push(badge('High priority', 'badge--high'));
  } else if (task.priority === 'LOW') {
    badges.push(badge('Low priority', 'badge--low'));
  }
  if (task.dueDate) {
    const due = describeDueDate(task);
    badges.push(badge(due.text, due.modifier, 'i-calendar'));
  }
  return badges;
}

function badge(text, modifier, icon) {
  const span = document.createElement('span');
  span.className = modifier ? `badge ${modifier}` : 'badge';
  if (icon) {
    span.insertAdjacentHTML('beforeend', `<svg class="icon" aria-hidden="true"><use href="#${icon}"/></svg>`);
  }
  span.append(text);
  return span;
}

function describeDueDate(task) {
  const due = parseDate(task.dueDate);
  const today = startOfToday();
  const days = Math.round((due - today) / 86_400_000);
  const sameYear = due.getFullYear() === today.getFullYear();
  const dateText = due.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    ...(sameYear ? {} : { year: 'numeric' }),
  });

  if (task.overdue) {
    return { text: days === -1 ? 'Overdue · yesterday' : `Overdue · ${dateText}`, modifier: 'badge--overdue' };
  }
  const open = task.status !== 'COMPLETED';
  if (days === 0) return { text: 'Due today', modifier: open ? 'badge--soon' : '' };
  if (days === 1) return { text: 'Due tomorrow', modifier: open ? 'badge--soon' : '' };
  if (days > 1 && days < 7) {
    return { text: `Due ${due.toLocaleDateString(undefined, { weekday: 'long' })}`, modifier: '' };
  }
  return { text: `Due ${dateText}`, modifier: '' };
}

function parseDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

function renderEditor(task) {
  const row = document.createElement('li');
  row.className = 'task is-editing';
  row.dataset.id = task.id;

  const form = el.editTemplate.content.firstElementChild.cloneNode(true);
  // Templates are cloned per row, so give every control a unique id for its label.
  form.querySelectorAll('.field').forEach((field) => {
    const control = field.querySelector('input, select');
    const id = `edit-${task.id}-${control.name}`;
    control.id = id;
    field.querySelector('label').htmlFor = id;
  });
  const error = form.querySelector('.field-error');
  error.id = `edit-${task.id}-error`;
  form.elements.description.setAttribute('aria-describedby', error.id);

  form.elements.description.value = task.description;
  form.elements.status.value = task.status;
  form.elements.priority.value = task.priority;
  form.elements.dueDate.value = task.dueDate ?? '';
  form.dataset.version = task.version;
  form.setAttribute('aria-label', `Edit "${task.description}"`);

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    saveEdit(task, form);
  });
  form.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      stopEditing();
    }
  });
  form.querySelector('[data-action="cancel"]').addEventListener('click', stopEditing);

  row.append(form);
  return row;
}

function renderEmptyState() {
  const empty = state.tasks.length === 0;
  el.empty.hidden = !empty;
  if (!empty) return;

  if (state.query) {
    el.emptyTitle.textContent = `No tasks match “${state.query}”`;
    el.emptyText.textContent = 'Try a different search or filter.';
  } else if (state.status === 'COMPLETED') {
    el.emptyTitle.textContent = 'Nothing finished yet';
    el.emptyText.textContent = 'Tick a task off and it will show up here.';
  } else if (state.status) {
    el.emptyTitle.textContent = `No tasks ${STATUS_LABELS[state.status].toLowerCase()}`;
    el.emptyText.textContent = 'Everything here is taken care of.';
  } else {
    el.emptyTitle.textContent = 'Nothing to do yet';
    el.emptyText.textContent = 'Add your first task above to get started.';
  }
}

// ---------------------------------------------------------------------------
// Focus handling: the list uses a roving tab stop, so Tab enters the list once
// and J/K or the arrow keys move between tasks.
// ---------------------------------------------------------------------------

function rows() {
  return [...el.list.querySelectorAll('.task[data-id]')];
}

function rowFor(id) {
  return el.list.querySelector(`.task[data-id="${id}"]`);
}

function focusRow(row) {
  if (!row) return;
  rows().forEach((other) => {
    if (!other.classList.contains('is-editing')) other.tabIndex = other === row ? 0 : -1;
  });
  state.activeId = Number(row.dataset.id);
  row.focus();
}

function restoreFocus(controlClass) {
  const row = rowFor(state.activeId);
  if (!row) {
    el.list.focus();
    return;
  }
  if (row.classList.contains('is-editing')) {
    row.querySelector('input[name="description"]').focus();
    return;
  }
  const control = controlClass && row.querySelector(`.${controlClass}`);
  (control || row).focus();
}

function moveFocus(step) {
  const all = rows();
  if (all.length === 0) return;
  const current = all.findIndex((row) => row.contains(document.activeElement));
  const next = current === -1 ? 0 : Math.min(all.length - 1, Math.max(0, current + step));
  focusRow(all[next]);
}

function activeTask() {
  const row = document.activeElement?.closest?.('.task[data-id]');
  if (!row || !el.list.contains(row)) return null;
  return state.tasks.find((task) => task.id === Number(row.dataset.id)) ?? null;
}

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

async function addTask(event) {
  event.preventDefault();
  const description = el.newDescription.value.trim();
  if (!description) {
    showFieldError(el.newDescription, el.addError, 'Describe the task before adding it.');
    el.newDescription.focus();
    return;
  }
  clearFieldError(el.newDescription, el.addError);
  try {
    const task = await api.create({
      description,
      priority: el.newPriority.value,
      dueDate: el.newDue.value || null,
    });
    el.addForm.reset();
    state.activeId = task.id;
    toast(`Added “${task.description}”`);
    await refresh();
  } catch (error) {
    showFieldError(el.newDescription, el.addError, error.message);
  }
  el.newDescription.focus();
}

async function toggleTask(task) {
  const status = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
  state.activeId = task.id;
  try {
    await api.patch(task, { status });
    toast(status === 'COMPLETED' ? `Done: “${task.description}”` : `Reopened “${task.description}”`);
  } catch (error) {
    if (error.changedElsewhere) {
      toast(`“${task.description}” was changed elsewhere, so it was left alone. The list now shows the latest version.`, 'error');
    } else if (error.notFound) {
      toast(`“${task.description}” was deleted elsewhere.`, 'error');
    } else {
      toast(`Could not update the task. ${error.message}`, 'error');
    }
  }
  await refresh();
}

function startEditing(task) {
  state.editingId = task.id;
  state.activeId = task.id;
  renderTasks();
  const input = rowFor(task.id)?.querySelector('input[name="description"]');
  if (input) {
    input.focus();
    input.select();
  }
}

function stopEditing() {
  const id = state.editingId;
  state.editingId = null;
  renderTasks();
  focusRow(rowFor(id));
}

async function saveEdit(task, form) {
  const input = form.elements.description;
  const error = form.querySelector('.field-error');
  const description = input.value.trim();
  if (!description) {
    showFieldError(input, error, 'The description cannot be empty.');
    input.focus();
    return;
  }
  try {
    // PUT rather than PATCH so that clearing the due date is saved too.
    await api.replace(
      { id: task.id, version: form.dataset.version },
      {
        description,
        status: form.elements.status.value,
        priority: form.elements.priority.value,
        dueDate: form.elements.dueDate.value || null,
      },
    );
  } catch (err) {
    if (err.changedElsewhere) {
      await handleEditConflict(task, form, input, error);
    } else if (err.notFound) {
      await handleDeletedWhileEditing(task);
    } else {
      showFieldError(input, error, err.message);
    }
    return;
  }
  state.editingId = null;
  toast('Task updated');
  await refresh();
  focusRow(rowFor(task.id));
}

// Someone else saved the task while it was being edited here. Keep what was typed, but base the next
// save on their version, so that saving again knowingly replaces it and Cancel keeps it.
async function handleEditConflict(task, form, input, error) {
  let latest;
  try {
    latest = await api.get(task.id);
  } catch (err) {
    if (err.notFound) {
      await handleDeletedWhileEditing(task);
    } else {
      showFieldError(input, error, err.message);
    }
    return;
  }
  form.dataset.version = latest.version;
  state.tasks = state.tasks.map((t) => (t.id === latest.id ? latest : t));
  showFieldError(
    input,
    error,
    `This task was changed elsewhere while you were editing. It now reads “${latest.description}”. ` +
      'Save again to replace that with your version, or Cancel to keep it.',
  );
  input.focus();
}

async function handleDeletedWhileEditing(task) {
  state.editingId = null;
  toast(`“${task.description}” was deleted elsewhere, so your changes could not be saved.`, 'error');
  await refresh();
  const row = rowFor(state.activeId);
  if (row) focusRow(row);
  else el.newDescription.focus();
}

async function deleteTask(task) {
  const confirmed = await confirmAction({
    title: 'Delete task?',
    text: `“${task.description}” will be removed permanently.`,
    action: 'Delete',
  });
  if (!confirmed) {
    focusRow(rowFor(task.id));
    return;
  }
  const all = rows();
  const index = all.findIndex((row) => Number(row.dataset.id) === task.id);
  const neighbour = all[index + 1] ?? all[index - 1];
  state.activeId = neighbour ? Number(neighbour.dataset.id) : null;
  try {
    await api.remove(task);
    toast(`Deleted “${task.description}”`);
  } catch (error) {
    if (error.changedElsewhere) {
      state.activeId = task.id;
      toast(`“${task.description}” was changed elsewhere, so it was not deleted. Check the latest version first.`, 'error');
    } else if (error.notFound) {
      toast(`“${task.description}” had already been deleted.`);
    } else {
      toast(`Could not delete the task. ${error.message}`, 'error');
    }
  }
  await refresh();
  const next = rowFor(state.activeId);
  if (next) focusRow(next);
  else el.newDescription.focus();
}

async function clearCompleted() {
  const count = Number(el.statCompleted.textContent) || 0;
  const confirmed = await confirmAction({
    title: 'Clear finished tasks?',
    text: `${count} completed ${count === 1 ? 'task' : 'tasks'} will be removed permanently.`,
    action: 'Clear',
  });
  if (!confirmed) return;
  try {
    const { deleted } = await api.clearCompleted();
    toast(`Cleared ${deleted} completed ${deleted === 1 ? 'task' : 'tasks'}`);
  } catch (error) {
    toast(`Could not clear completed tasks. ${error.message}`, 'error');
  }
  await refresh();
}

function confirmAction({ title, text, action, destructive = true }) {
  el.confirmTitle.textContent = title;
  el.confirmText.textContent = text;
  el.confirmOk.textContent = action;
  el.confirmOk.classList.toggle('button--danger', destructive);
  el.confirmOk.classList.toggle('button--primary', !destructive);
  el.confirmDialog.returnValue = '';
  el.confirmDialog.showModal();
  return new Promise((resolve) => {
    el.confirmDialog.addEventListener('close', () => resolve(el.confirmDialog.returnValue === 'confirm'), {
      once: true,
    });
  });
}

// Imports a JSON export chosen with the file picker. The server validates the whole file and adds
// nothing if any task in it is invalid; this only reads it to say how many tasks will be added.
async function importFromFile() {
  const file = el.importFile.files[0];
  el.importFile.value = ''; // so that choosing the same file again still triggers a change
  if (!file) return;

  let data;
  try {
    data = JSON.parse(await file.text());
  } catch {
    toast(`Nothing was imported: “${file.name}” is not a JSON export.`, 'error');
    return;
  }
  if (Array.isArray(data?.tasks)) {
    const count = data.tasks.length;
    if (count === 0) {
      toast(`“${file.name}” has no tasks in it.`);
      return;
    }
    const confirmed = await confirmAction({
      title: 'Import tasks?',
      text: `${count} ${count === 1 ? 'task' : 'tasks'} from “${file.name}” will be added to your list.`,
      action: 'Import',
      destructive: false,
    });
    if (!confirmed) return;
  }
  try {
    const { imported } = await api.importTasks(data);
    toast(`Imported ${imported} ${imported === 1 ? 'task' : 'tasks'}`);
  } catch (error) {
    toast(`Nothing was imported. ${error.message}`, 'error');
  }
  await refresh();
}

function setStatusFilter(status) {
  state.status = status;
  for (const button of el.statusFilter.querySelectorAll('[role="radio"]')) {
    const selected = button.dataset.status === status;
    button.setAttribute('aria-checked', String(selected));
    button.tabIndex = selected ? 0 : -1;
  }
  saveView();
  loadTasks();
}

function setSort(sort) {
  state.sort = SORTS.includes(sort) ? sort : 'OLDEST';
  el.sort.value = state.sort;
  saveView();
  loadTasks();
}

function toggleTheme() {
  const dark = document.documentElement.dataset.theme !== 'dark';
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  try {
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  } catch {
    // Not persisted; the choice still applies until the page is reloaded.
  }
  updateThemeButton();
}

function updateThemeButton() {
  const dark = document.documentElement.dataset.theme === 'dark';
  el.themeButton.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
}

// ---------------------------------------------------------------------------
// Feedback
// ---------------------------------------------------------------------------

function toast(message, kind = 'info') {
  const item = document.createElement('div');
  item.className = kind === 'error' ? 'toast toast--error' : 'toast';
  item.textContent = message;
  el.toasts.append(item);
  while (el.toasts.children.length > 3) el.toasts.firstElementChild.remove();
  setTimeout(() => item.remove(), kind === 'error' ? 6000 : 3500);
}

function showFieldError(input, message, text) {
  input.setAttribute('aria-invalid', 'true');
  message.textContent = text;
  message.hidden = false;
}

function clearFieldError(input, message) {
  input.removeAttribute('aria-invalid');
  message.hidden = true;
}

// ---------------------------------------------------------------------------
// View preferences
// ---------------------------------------------------------------------------

function saveView() {
  try {
    localStorage.setItem(VIEW_KEY, JSON.stringify({ status: state.status, sort: state.sort }));
  } catch {
    // Preferences are a convenience; ignore storage failures.
  }
}

function restoreView() {
  try {
    const view = JSON.parse(localStorage.getItem(VIEW_KEY) ?? '{}');
    if (FILTERS.includes(view.status)) state.status = view.status;
    if (SORTS.includes(view.sort)) state.sort = view.sort;
  } catch {
    // Fall back to the defaults.
  }
  el.sort.value = state.sort;
  for (const button of el.statusFilter.querySelectorAll('[role="radio"]')) {
    const selected = button.dataset.status === state.status;
    button.setAttribute('aria-checked', String(selected));
    button.tabIndex = selected ? 0 : -1;
  }
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

function isTyping(target) {
  return target instanceof HTMLElement && (target.isContentEditable || /^(INPUT|SELECT|TEXTAREA)$/.test(target.tagName));
}

function onGlobalKeydown(event) {
  if (event.ctrlKey || event.metaKey || event.altKey || event.defaultPrevented) return;
  if (document.querySelector('dialog[open]')) return;

  if (isTyping(event.target)) {
    if (event.key === 'Escape' && event.target === el.search && el.search.value) {
      el.search.value = '';
      onSearchInput();
    }
    // Checkboxes count as inputs but should still respond to list shortcuts.
    if (!event.target.classList.contains('check__input')) return;
  }

  const task = activeTask();
  const onRow = event.target instanceof HTMLElement && event.target.matches('.task[data-id]');
  const key = event.key;
  let handled = true;

  if (key === '/') el.search.focus();
  else if (key === 'n' || key === 'N') el.newDescription.focus();
  else if (key === '?') el.shortcutsDialog.showModal();
  else if (key === 't' || key === 'T') toggleTheme();
  else if (key >= '1' && key <= '4') setStatusFilter(FILTERS[Number(key) - 1]);
  else if (key === 'j' || key === 'J' || key === 'ArrowDown') moveFocus(1);
  else if (key === 'k' || key === 'K' || key === 'ArrowUp') moveFocus(-1);
  else if (key === 'Home' && task) focusRow(rows()[0]);
  else if (key === 'End' && task) focusRow(rows().at(-1));
  else if ((key === 'x' || key === 'X') && task) toggleTask(task);
  else if (key === ' ' && task && onRow) toggleTask(task);
  else if ((key === 'e' || key === 'E' || (key === 'Enter' && onRow)) && task) startEditing(task);
  else if ((key === 'Delete' || key === 'Backspace') && task) deleteTask(task);
  else handled = false;

  if (handled) event.preventDefault();
}

let searchTimer;

function onSearchInput() {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    const query = el.search.value.trim();
    if (query === state.query) return;
    state.query = query;
    loadTasks();
  }, 200);
}

function onListClick(event) {
  const row = event.target.closest('.task[data-id]');
  if (!row || row.classList.contains('is-editing')) return;
  const task = state.tasks.find((t) => t.id === Number(row.dataset.id));
  if (!task) return;

  if (event.target.closest('.task__edit')) startEditing(task);
  else if (event.target.closest('.task__delete')) deleteTask(task);
  else if (event.target.closest('.check')) {
    // Let the click update the checkbox visually, then let the server decide the state.
    if (event.target.classList.contains('check__input')) toggleTask(task);
  } else {
    focusRow(row);
  }
}

function onListDoubleClick(event) {
  const description = event.target.closest('.task__description');
  if (!description) return;
  const row = description.closest('.task[data-id]');
  const task = state.tasks.find((t) => t.id === Number(row.dataset.id));
  if (task) startEditing(task);
}

function onFilterKeydown(event) {
  // Arrow keys move between options, as in a native radio group.
  const step = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.key];
  if (!step) return;
  event.preventDefault();
  event.stopPropagation();
  const index = (FILTERS.indexOf(state.status) + step + FILTERS.length) % FILTERS.length;
  setStatusFilter(FILTERS[index]);
  el.statusFilter.querySelector('[aria-checked="true"]').focus();
}

function init() {
  restoreView();
  updateThemeButton();

  el.addForm.addEventListener('submit', addTask);
  el.newDescription.addEventListener('input', () => clearFieldError(el.newDescription, el.addError));
  el.search.addEventListener('input', onSearchInput);
  el.sort.addEventListener('change', () => setSort(el.sort.value));
  el.statusFilter.addEventListener('click', (event) => {
    const button = event.target.closest('[role="radio"]');
    if (button) setStatusFilter(button.dataset.status);
  });
  el.statusFilter.addEventListener('keydown', onFilterKeydown);
  el.clearCompleted.addEventListener('click', clearCompleted);
  el.importButton.addEventListener('click', () => el.importFile.click());
  el.importFile.addEventListener('change', importFromFile);
  el.retry.addEventListener('click', () => {
    loadTasks({ showSkeleton: true });
    loadStats();
  });
  el.list.addEventListener('click', onListClick);
  el.list.addEventListener('dblclick', onListDoubleClick);
  el.list.addEventListener('focusin', (event) => {
    const row = event.target.closest('.task[data-id]');
    if (row && !row.classList.contains('is-editing')) state.activeId = Number(row.dataset.id);
  });
  el.themeButton.addEventListener('click', toggleTheme);
  el.shortcutsButton.addEventListener('click', () => el.shortcutsDialog.showModal());
  document.addEventListener('keydown', onGlobalKeydown);
  // Pick up changes made in the console or another tab on coming back to this one. Skipped while
  // editing so that nothing typed is lost; a conflicting save is still caught by the server.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && state.editingId === null) refresh();
  });

  loadTasks({ showSkeleton: true });
  loadStats();
}

init();
