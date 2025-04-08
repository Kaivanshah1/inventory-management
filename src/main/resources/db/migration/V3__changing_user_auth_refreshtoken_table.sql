ALTER TABLE refresh_tokens
DROP CONSTRAINT IF EXISTS refresh_tokens_userId_fkey;

-- 2. Add the new foreign key constraint in UserAuth table
ALTER TABLE UserAuth
ADD CONSTRAINT fk_userauth_userid FOREIGN KEY (userId) REFERENCES users(id);

-- 3. Add the new foreign key constraint in refresh_tokens table
ALTER TABLE refresh_tokens
ADD CONSTRAINT fk_refreshtokens_userid FOREIGN KEY (userId) REFERENCES users(id);