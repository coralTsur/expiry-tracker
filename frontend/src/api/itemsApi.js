import { API_BASE_URL } from './config';

const NETWORK_ERROR_MESSAGE = 'לא ניתן להתחבר לשרת. נסי שוב מאוחר יותר.';
const SERVER_ERROR_MESSAGE = 'אירעה שגיאה בשרת. נסי שוב מאוחר יותר.';
const NOT_FOUND_MESSAGE = 'הפריט לא נמצא. ייתכן שכבר נמחק.';
// spec section 8, errors: the message of every 400.
const ITEM_NOT_SAVED_MESSAGE = 'הפריט לא נשמר';

// Shared by every endpoint: network failure, non-2xx response, or success.
async function request(path, options) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, options);
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
    let message;
    if (response.status === 400) {
      // A 400 is a problem with the input, not a server failure, so the server's own message is shown.
      message = body?.message ?? ITEM_NOT_SAVED_MESSAGE;
    } else if (response.status === 404) {
      // The server's 404 message contains the item id, which the spec says is never shown.
      message = NOT_FOUND_MESSAGE;
    } else {
      message = SERVER_ERROR_MESSAGE;
    }
    const error = new Error(message);
    error.status = response.status;
    error.fieldErrors = body?.fieldErrors ?? null;
    throw error;
  }

  // 204 No Content has no body, so response.json() would throw.
  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export function fetchItems() {
  return request('/api/items');
}

// The server filters and sorts this list (spec sections 5 and 8), so the client shows it as is.
export function fetchExpiringSoon() {
  return request('/api/items/expiring-soon');
}

// item: the request body from spec section 8, without id. The server answers 201 with the saved item.
export function createItem(item) {
  return request('/api/items', {
    method: 'POST',
    // Without this header Spring does not read the body as JSON.
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(item),
  });
}

export function redeemItem(id) {
  return request(`/api/items/${id}/redeem`, { method: 'PATCH' });
}

export function deleteItem(id) {
  return request(`/api/items/${id}`, { method: 'DELETE' });
}
