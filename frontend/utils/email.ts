const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

export function getEmailValidationError(email: string) {
  const normalizedEmail = normalizeEmail(email);

  if (!normalizedEmail) return 'Email is required';
  if (!EMAIL_PATTERN.test(normalizedEmail))
    return 'Enter a valid email address';
  return null;
}
