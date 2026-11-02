// "2026-11-20" -> "20/11/2026". Splitting the string avoids new Date(), which reads
// "yyyy-MM-dd" as midnight UTC and can show the previous day in some time zones.
export function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}
