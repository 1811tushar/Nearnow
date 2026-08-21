-- Development-only provisioning examples.
-- Do not run blindly in production.

-- 1) Promote an existing user to vendor.
UPDATE users
SET role = 'vendor'
WHERE email = 'vendor@example.com';

-- 2) Promote an existing user to rider.
UPDATE users
SET role = 'rider'
WHERE email = 'rider@example.com';

-- 3) Promote an existing user to warehouse manager.
UPDATE users
SET role = 'warehouse_manager'
WHERE email = 'warehouse@example.com';

-- 4) Assign a warehouse manager to an existing store.
UPDATE stores
SET warehouse_manager_user_id = (
    SELECT id FROM users WHERE email = 'warehouse@example.com'
)
WHERE id = 1;

-- 5) IMPORTANT: after role changes, log in again to receive a fresh JWT.
-- The current JWT embeds the role that existed when the token was issued.
