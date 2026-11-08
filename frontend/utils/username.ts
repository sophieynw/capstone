export const USERNAME_FORMAT_ERROR =
  'Username may contain only letters, numbers, periods, underscores, and hyphens, and must begin and end with a letter or number.';

const VALID_USERNAME = /^[a-z0-9](?:[a-z0-9._-]*[a-z0-9])?$/;

export function normalizeUsername(username: string) {
  return username.trim().toLowerCase();
}

export function getUsernameValidationError(username: string) {
  const normalizedUsername = normalizeUsername(username);

  if (!normalizedUsername) {
    return 'Username is required';
  }
  if (normalizedUsername.length < 3) {
    return 'Username must be at least 3 characters';
  }
  if (!VALID_USERNAME.test(normalizedUsername)) {
    return USERNAME_FORMAT_ERROR;
  }

  return null;
}
