import { API_BASE_URL } from './config';

const NETWORK_ERROR_MESSAGE = 'לא ניתן להתחבר לשרת. נסי שוב מאוחר יותר.';
const SERVER_ERROR_MESSAGE = 'אירעה שגיאה בשרת. נסי שוב מאוחר יותר.';

export async function fetchItems() {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/items`);
  } catch {
    // fetch rejects only when no HTTP response arrived: server down or CORS blocked.
    throw new Error(NETWORK_ERROR_MESSAGE);
  }

  // fetch does not reject on 4xx/5xx, so the status has to be checked here.
  if (!response.ok) {
    let body = null;
    try {
      body = await response.json();
    } catch {
      // The error body is not JSON (for example an HTML error page), so body stays null.
    }
    const error = new Error(SERVER_ERROR_MESSAGE);
    error.status = response.status;
    error.fieldErrors = body?.fieldErrors ?? null;
    throw error;
  }

  return response.json();
}
