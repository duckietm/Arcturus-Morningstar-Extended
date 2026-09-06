-- Restore the pre-upgrade bcrypt hash for the existing remote hotel account.
-- Username deliberately includes its trailing dot.
UPDATE `users`
SET `password` = '$2y$12$qLV1YNZNLJPZcuA.rLN5M.3wEfMQn8X23i/qL3OF7ZOKtQ8KEFIEO'
WHERE `username` = 'Invincibile.';
