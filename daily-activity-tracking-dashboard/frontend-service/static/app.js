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
  warnings: document.querySelector("#warnings"),
  financeMonth: document.querySelector("#finance-month"),
  financeMessage: document.querySelector("#finance-message"),
  financeIncome: document.querySelector("#finance-income"),
  financeExpenses: document.querySelector("#finance-expenses"),
  financeExpenseCaption: document.querySelector("#finance-expense-caption"),
  financeDocumentCount: document.querySelector("#finance-document-count"),
  financeDocumentCaption: document.querySelector("#finance-document-caption"),
  financeCategories: document.querySelector("#finance-categories"),
  financeDocuments: document.querySelector("#finance-documents"),
  financeRows: document.querySelector("#finance-rows")
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

function formatCurrency(amount, currency = "INR") {
  if (amount === null || amount === undefined) return "Not available";
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency,
    maximumFractionDigits: 2
  }).format(amount);
}

function renderFinanceCategories(categories, currency) {
  if (!categories.length) {
    makeEmpty(elements.financeCategories, "No expense categories could be recognized for this month.");
    return;
  }
  const largest = Math.max(...categories.map(category => category.amount), 1);
  elements.financeCategories.replaceChildren(...categories.map(category => {
    const row = document.createElement("div");
    row.className = "category-row";
    const meta = document.createElement("div");
    meta.className = "category-meta";
    const name = document.createElement("span");
    name.className = "category-name";
    name.textContent = category.category;
    const stats = document.createElement("span");
    stats.className = "category-stats";
    stats.textContent = `${formatCurrency(category.amount, currency)} · ${category.transactionCount} items`;
    meta.append(name, stats);
    const track = document.createElement("div");
    track.className = "bar-track";
    const bar = document.createElement("div");
    bar.className = "bar finance-bar";
    bar.style.width = `${Math.max(4, (category.amount / largest) * 100)}%`;
    track.append(bar);
    row.append(meta, track);
    return row;
  }));
}

function renderFinanceDocuments(documents) {
  if (!documents.length) {
    makeEmpty(elements.financeDocuments, "No eligible PDF documents were found.");
    return;
  }
  elements.financeDocuments.replaceChildren(...documents.map(documentInfo => {
    const row = document.createElement("div");
    row.className = "document-row";
    const name = document.createElement("span");
    name.className = "document-name";
    name.textContent = documentInfo.name;
    const status = document.createElement("span");
    status.className = "document-status";
    status.textContent = `${documentInfo.status.replaceAll("_", " ")} · ${documentInfo.pages} pages · ${documentInfo.transactionsFound} rows`;
    row.append(name, status);
    return row;
  }));
}

function renderFinanceTransactions(transactions, currency) {
  if (!transactions.length) {
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 6;
    cell.className = "empty";
    cell.textContent = "No structured transaction rows were recognized. Review the document status and source PDFs.";
    row.append(cell);
    elements.financeRows.replaceChildren(row);
    return;
  }
  elements.financeRows.replaceChildren(...transactions.map(transaction => {
    const row = document.createElement("tr");
    for (const value of [
      transaction.date || "—",
      transaction.type,
      transaction.category,
      transaction.description,
      formatCurrency(transaction.amount, currency),
      transaction.source
    ]) {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.append(cell);
    }
    return row;
  }));
}

function renderFinanceWarnings(warnings) {
  elements.financeMessage.textContent = warnings.join(" ");
  elements.financeMessage.classList.toggle("error", false);
  elements.financeMessage.hidden = warnings.length === 0;
}

async function loadFinancialAnalysis() {
  const month = elements.financeMonth.value;
  if (!month) return;
  elements.financeMessage.textContent = "Reading this month's financial PDFs locally…";
  elements.financeMessage.classList.remove("error");
  elements.financeMessage.hidden = false;
  try {
    const response = await fetch(`/api/finance?month=${encodeURIComponent(month)}`);
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.detail || body.message || `Financial analysis failed (${response.status}).`);
    }
    const data = await response.json();
    elements.financeIncome.textContent = formatCurrency(data.totalIncome, data.currency);
    elements.financeExpenses.textContent = formatCurrency(data.totalExpenditure, data.currency);
    elements.financeExpenseCaption.textContent = ({
      DOCUMENT_TOTALS: "document-stated total; detail rows not added again",
      DOCUMENT_TOTALS_AND_ROWS: "document totals plus rows from files without totals",
      TRANSACTION_ROWS: "sum of recognized transaction rows",
      UNAVAILABLE: "no monthly total recognized"
    })[data.totalExpenditureBasis] || "calculation basis unavailable";
    elements.financeDocumentCount.textContent = `${data.documentsWithReadableText}/${data.documentsFound}`;
    elements.financeDocumentCaption.textContent = "documents with readable text / selected month";
    renderFinanceCategories(data.categories);
    renderFinanceDocuments(data.documents);
    renderFinanceTransactions(data.transactions, data.currency);
    renderFinanceWarnings(data.warnings);
  } catch (error) {
    elements.financeMessage.textContent = `${error.message} Check the financial-analysis service and configured PDF folder.`;
    elements.financeMessage.classList.add("error");
    elements.financeMessage.hidden = false;
  }
}

async function loadFinancialMonths() {
  try {
    const response = await fetch("/api/finance/months");
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.detail || body.message || `Month list request failed (${response.status}).`);
    }
    const months = await response.json();
    elements.financeMonth.replaceChildren(...months.map(month => {
      const option = document.createElement("option");
      option.value = month.value;
      option.textContent = month.label;
      return option;
    }));
    if (!months.length) {
      elements.financeMessage.textContent = "No eligible monthly expenditure PDFs were found.";
      elements.financeMessage.hidden = false;
      elements.financeDocumentCount.textContent = "0";
      return;
    }
    await loadFinancialAnalysis();
  } catch (error) {
    elements.financeMessage.textContent = `${error.message} Check the financial-analysis service and configured PDF folder.`;
    elements.financeMessage.classList.add("error");
    elements.financeMessage.hidden = false;
  }
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
elements.refresh.addEventListener("click", () => {
  loadDashboard();
  loadFinancialAnalysis();
});
elements.clear.addEventListener("click", () => {
  elements.from.value = "";
  elements.to.value = "";
  loadDashboard();
});
elements.financeMonth.addEventListener("change", loadFinancialAnalysis);

loadDashboard();
loadFinancialMonths();
