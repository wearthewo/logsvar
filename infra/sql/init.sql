CREATE DATABASE IF NOT EXISTS monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS alerts CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS security CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON monitoring.* TO 'monitoring'@'%';
GRANT ALL PRIVILEGES ON alerts.* TO 'monitoring'@'%';
GRANT ALL PRIVILEGES ON security.* TO 'monitoring'@'%';
FLUSH PRIVILEGES;
