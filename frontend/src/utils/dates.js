// Date helpers shared by the item form and the row status. Dates are yyyy-MM-dd strings, as in the API.

const MS_PER_DAY = 24 * 60 * 60 * 1000;

// Today in the browser's time zone, as yyyy-MM-dd, the same format as <input type="date">.
export function todayString() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

// Whole days from one yyyy-MM-dd date to another.
// Date.UTC ignores daylight-saving changes, so every day is exactly 24 hours long.
export function daysBetween(fromDate, toDate) {
  const [fromYear, fromMonth, fromDay] = fromDate.split('-').map(Number);
  const [toYear, toMonth, toDay] = toDate.split('-').map(Number);
  const from = Date.UTC(fromYear, fromMonth - 1, fromDay);
  const to = Date.UTC(toYear, toMonth - 1, toDay);
  return Math.round((to - from) / MS_PER_DAY);
}
