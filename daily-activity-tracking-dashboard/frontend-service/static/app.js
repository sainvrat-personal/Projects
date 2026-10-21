const API_BASE = "/api";

const elements = {
  from: document.querySelector("#from-date"),
  to: document.querySelector("#to-date"),
  apply: document.querySelector("#apply-filter"),
  clear: document.querySelector("#clear-filter"),
  refresh: document.querySelector("#refresh"),
  message: document.querySelector("#message"),
  taskCount: document.querySelector("#task-count"),
  activeDays: document.querySelector("#active-days"),
  knownTime: document.querySelector("#known-time"),
  fileCount: document.querySelector("#file-count"),
  categories: document.querySelector("#categories"),
  daily: document.querySelector("#daily"),
  rows: document.querySelector("#activity-rows"),
  range: document.querySelector("#range-label"),
  warningsPanel: document.querySelector("#warnings-panel"),
  warnings: document.querySelector("#warnings")
};

function showMessage(text, isError = false) {
  elements.message.textContent = text;
  elements.message.classList.toggle("error", isError);
  elements.message.hidden = !text;
}

function formatDuration(minutes) {
  if (!minutes) return "—";
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return hours ? `${hours}h ${remainder}m` : `${remainder}m`;
}

function formatDate(dateText, options = { day: "numeric", month: "short", year: "numeric" }) {
  if (!dateText) return "No dates found";
  return new Date(`${dateText}T12:00:00`).toLocaleDateString(undefined, options);
}

function makeEmpty(container, message) {
  const empty = document.createElement("p");
  empty.className = "empty";
  empty.textContent = message;
  container.replaceChildren(empty);
}

function renderCategories(categories) {
  if (!categories.length) {
    makeEmpty(elements.categories, "No activities found for this date range.");
    return;
  }
  const largest = Math.max(...categories.map(category => category.taskCount), 1);
  const rows = categories.map(category => {
    const row = document.createElement("div");
    row.className = "category-row";
    const meta = document.createElement("div");
    meta.className = "category-meta";
    const name = document.createElement("span");
    name.className = "category-name";
    name.textContent = category.category;
    const stats = document.createElement("span");
    stats.className = "category-stats";
    stats.textContent = `${category.taskCount} tasks · ${category.activeDays} days · ${formatDuration(category.knownDurationMinutes)}`;
    meta.append(name, stats);
    const track = document.createElement("div");
    track.className = "bar-track";
    const bar = document.createElement("div");
    bar.className = "bar";
    bar.style.width = `${Math.max(4, (category.taskCount / largest) * 100)}%`;
    track.append(bar);
    row.append(meta, track);
    return row;
  });
  elements.categories.replaceChildren(...rows);
}

function renderDaily(days) {
  if (!days.length) {
    makeEmpty(elements.daily, "No daily activity found for this date range.");
    return;
  }
  const largest = Math.max(...days.map(day => day.taskCount), 1);
  const rows = days.map(day => {
    const row = document.createElement("div");
    row.className = "day-row";
    const name = document.createElement("span");
    name.className = "day-name";
    name.textContent = `${day.day.slice(0, 3)} · ${formatDate(day.date, { day: "numeric", month: "short" })}`;
    const track = document.createElement("div");
    track.className = "day-track";
    const bar = document.createElement("div");
    bar.className = "day-bar";
    bar.style.width = `${Math.max(3, (day.taskCount / largest) * 100)}%`;
    track.append(bar);
    const count = document.createElement("span");
    count.className = "day-count";
    count.textContent = String(day.taskCount);
    row.append(name, track, count);
    return row;
  });
  elements.daily.replaceChildren(...rows);
}

function renderActivities(activities) {
  if (!activities.length) {
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 4;
    cell.className = "empty";
    cell.textContent = "No activities found for this date range.";
    row.append(cell);
    elements.rows.replaceChildren(row);
    return;
  }
  const rows = activities.map(activity => {
    const row = document.createElement("tr");
    const date = document.createElement("td");
    date.textContent = `${activity.day.slice(0, 3)}, ${formatDate(activity.date)}`;
    const category = document.createElement("td");
    const tag = document.createElement("span");
    tag.className = "tag";
    tag.textContent = activity.category;
    category.append(tag);
    const description = document.createElement("td");
    description.textContent = activity.description;
    const duration = document.createElement("td");
    duration.textContent = formatDuration(activity.durationMinutes);
    row.append(date, category, description, duration);
    return row;
  });
  elements.rows.replaceChildren(...rows);
}

function renderWarnings(warnings) {
  elements.warningsPanel.hidden = warnings.length === 0;
  elements.warnings.replaceChildren(...warnings.map(warning => {
    const item = document.createElement("li");
    item.textContent = warning;
    return item;
  }));
}

async function loadDashboard() {
  showMessage("Reading your weekly activity files…");
  const params = new URLSearchParams();
  if (elements.from.value) params.set("from", elements.from.value);
  if (elements.to.value) params.set("to", elements.to.value);

  try {
    const response = await fetch(`${API_BASE}/dashboard?${params.toString()}`);
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.detail || body.message || `Dashboard request failed (${response.status}).`);
    }
    const data = await response.json();
    elements.taskCount.textContent = data.taskCount.toLocaleString();
    elements.activeDays.textContent = data.activeDays.toLocaleString();
    elements.knownTime.textContent = formatDuration(data.knownDurationMinutes);
    elements.fileCount.textContent = data.filesScanned.toLocaleString();
    elements.range.textContent = data.from && data.to
      ? `${formatDate(data.from)} – ${formatDate(data.to)}`
      : "No activity dates found";
    renderCategories(data.categories);
    renderDaily(data.dailyBreakdown);
    renderActivities(data.activities);
    renderWarnings(data.warnings);
    showMessage("");
  } catch (error) {
    showMessage(`${error.message} Make sure the Java backend is running.`, true);
  }
}

elements.apply.addEventListener("click", loadDashboard);
elements.refresh.addEventListener("click", loadDashboard);
elements.clear.addEventListener("click", () => {
  elements.from.value = "";
  elements.to.value = "";
  loadDashboard();
});

loadDashboard();
