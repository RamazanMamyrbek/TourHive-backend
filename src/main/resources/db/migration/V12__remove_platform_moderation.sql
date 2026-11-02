-- V12: Remove platform moderation and add application price snapshot

-- 1. Migrate any tours in PENDING_REVIEW directly to PUBLISHED, and REJECTED back to DRAFT
UPDATE tours
SET status = 'PUBLISHED',
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    rejection_reason = NULL
WHERE status = 'PENDING_REVIEW';

UPDATE tours
SET status = 'DRAFT',
    rejection_reason = NULL
WHERE status = 'REJECTED';

-- 2. Migrate existing business profiles to VERIFIED without manual platform review
UPDATE business_profiles
SET status = 'VERIFIED',
    rejection_reason = NULL
WHERE status IN ('PENDING', 'REJECTED');

UPDATE users
SET is_business_verified = true
WHERE id IN (SELECT user_id FROM business_profiles WHERE status = 'VERIFIED');

-- 3. Migrate any MODERATOR users to standard USER role
UPDATE users
SET role = 'USER'
WHERE role = 'MODERATOR';

-- 4. Add snapshot columns for agreed tour conditions on tour applications (A37)
ALTER TABLE tour_applications ADD COLUMN IF NOT EXISTS agreed_price NUMERIC(12, 2);
ALTER TABLE tour_applications ADD COLUMN IF NOT EXISTS agreed_currency VARCHAR(3);

UPDATE tour_applications ta
SET agreed_price = t.price,
    agreed_currency = COALESCE(t.currency, 'KZT')
FROM tours t
WHERE ta.tour_id = t.id
  AND ta.agreed_price IS NULL;
