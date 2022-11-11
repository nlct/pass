-- Replace example.com with your domain
-- Replace 'support@example.com' with your IT support email
-- Replace 'support' with your IT support username

USE passdb;

CREATE TABLE sessions
(
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   session_id CHAR(26),
   data TEXT,
   date_touched INT
);

CREATE TABLE users
(
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(12) NOT NULL UNIQUE,
   regnum VARCHAR(12) DEFAULT NULL UNIQUE,
   password VARCHAR(255) NOT NULL,
   code VARCHAR(255) DEFAULT NULL,
   role ENUM('student', 'staff', 'admin') DEFAULT 'student',
   status ENUM('active', 'pending', 'blocked') DEFAULT 'pending',
   account_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   mfa ENUM('on','off') NOT NULL DEFAULT 'off',
   2fa_key_verified ENUM('true','false') NOT NULL DEFAULT 'false',
   2fa_key VARCHAR(224) DEFAULT NULL
);

CREATE TABLE submissions
(
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  uploaded_by INT NOT NULL,
  upload_time CHAR(25) NOT NULL,
  course VARCHAR(64) NOT NULL,
  assignment VARCHAR(64) NOT NULL,
  token CHAR(10) NOT NULL,
  status ENUM('uploaded','queued','processing','processed') DEFAULT 'uploaded',
  exit_code INT
);

CREATE TABLE projectgroup
(
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   submission_id INT NOT NULL,
   user_id INT NOT NULL
);

CREATE TABLE whos_online
(
   user_id INT NOT NULL PRIMARY KEY,
   start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
   time_last_clicked DATETIME,
   last_page_url VARCHAR(256)
);

CREATE TABLE action_recorder
(
   id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
   user_id INT,
   time_stamp DATETIME DEFAULT CURRENT_TIMESTAMP,
   action VARCHAR(256),
   comments VARCHAR(2048)
);

CREATE TABLE tokens
(
  id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT UNSIGNED NOT NULL,
  selector BINARY(16) NOT NULL,
  verifier BINARY(32) NOT NULL,
  expires DATETIME NOT NULL
);

CREATE TABLE recovery_codes
(
  id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT UNSIGNED NOT NULL,
  selector BINARY(12) NOT NULL,
  verifier VARCHAR(128) NOT NULL
);

CREATE TABLE skip_totp
(
  id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT UNSIGNED NOT NULL,
  selector BINARY(16) NOT NULL,
  verifier BINARY(32) NOT NULL,
  expires DATETIME NOT NULL,
  device VARCHAR(512) DEFAULT NULL
);

CREATE TABLE config
(
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   setting VARCHAR(32) NOT NULL,
   value VARCHAR(1024) NOT NULL,
   value_type ENUM('int','match','text') NOT NULL,
   value_constraints VARCHAR(64) NOT NULL,
   description VARCHAR(1024) NOT NULL
);

INSERT INTO config SET setting='debug', value='1', value_type='int', 
value_constraints='[-1,2]', 
description='Debug setting. Allowed values: <dl><dt>-1</dt><dd>No debugging messages but any courses with debug="true" will be available for staff or admin users.</dd><dt>0</dt><dd>off</dd><dt>1</dt><dd>enable courses with debug="true" and display debugging messages</dd><dt>2</dt><dd>display extra debugging information for admin users</dd></dd></dl>You may need to log out and log back in again if you change this setting to enable/disable debug courses.';

INSERT INTO config SET setting='timeout', value='60', value_type='int',
value_constraints='[1,]',
description='Timeout value (seconds) written to PASS settings when a project is uploaded. Processes started by PASS will timeout after this value.';

INSERT INTO config SET setting='envelope_from', value='support@example.com',
value_type='match', value_constraints='/^[a-zA-Z0-9_\\-\.]+@example.com$/',
description='Envelope from setting used when sending email messages.';

INSERT INTO config SET setting='mail_from', value='no-reply@example.com',
value_type='match', value_constraints='/^[a-zA-Z0-9_\\-\\.]+@example.com$/',
description='From setting used when sending email messages.';

INSERT INTO config SET setting='banner_message', value='',
value_type='text', value_constraints='[0,1024]',
description='Banner message to display at the top of every page.';

INSERT INTO config SET setting='max_items_per_page', value=50,
value_type='int', value_constraints='[1,500]',
description='Maximum number of items to display on a page when listing search results.';

INSERT INTO config SET setting='upload_refresh', value='10',
value_type='int', value_constraints='[1,]',
description='Number of seconds to refresh upload list if there is an unprocessed entry.';

INSERT INTO config SET setting='reset_link_timeout', value='30',
value_type='int', value_constraints='[1,]',
description='Number of minutes password reset token remains valid.';

INSERT INTO config SET setting='verify_link_timeout', value='30',
value_type='int', value_constraints='[1,]',
description='Number of minutes account verification token remains valid.';

INSERT INTO config SET setting='mode', value='0', value_type='int', 
value_constraints='[0,1]', 
description='Website mode. In maintenance mode, only admin users can access the site. The login page will also be available, but you will need to bookmark or remember the login page URL. Allowed values: 0=normal, 1=maintenance. If you switch mode, you may first want to check if anyone is online and force them to logout by clearing the session data.';

INSERT INTO config SET setting='allow_test_user', value='0', value_type='int', 
value_constraints='[0,1]', 
description='Permits the test user account to access the site in maintenance mode. Users who are not logged in will be able to access: create account, verify account, resend verification, forgotten password and reset password. Allowed values: 0=false, 1=true.';

INSERT INTO config SET setting='test_user', value='abc01xyz',
value_type='match', value_constraints='/^[a-zA-Z0-9]+$/',
description='User name for test account.';

INSERT INTO config SET setting='test_user_redirect', value='support',
value_type='match', value_constraints='/^[a-zA-Z0-9_\\-\.]+$/',
description='Redirect test user emails to this user\'s email account.';

INSERT INTO config SET setting='help_reference', value='your lecturer',
value_type='text', value_constraints='[0,1024]',
description='In the event of an error, the user is told to get help from this reference.';

