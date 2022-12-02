# Setting Up the Frontend

These are instructions to copy over all the relevant files and setup
the MySQL database. This only needs to be done once (barring minor
updates to the frontend website pages).

First archive the [`pass-cli-server/server-files`](https://github.com/nlct/pass/tree/main/pass-cli-server/server-files) directory:

```bash
cd pass-cli-server
tar zcvf server-files.tgz --exclude-backups --exclude='*.swp' --exclude-vcs server-files/
```

FTP the archive to the server. (Change the port number, username and
server as applicable). For Alice, this is:

```bash
cd ../dist
sftp -P 22 ans@cmp-server-01.example.com
```
(enter password)

```
put server-files.tgz
exit
```

SSH to the server.

```bash
ssh -P 22 ans@cmp-server-01.example.com
```
(enter password) and unpack the archive:

```bash
tar zxvf server-files.tgz
```

The `backend` subdirectory contains the files which will be copied
over in the [backend setup](setupbackend.md).

## MySQL Database

The `passdb` database will need to be created by a MySQL admin user
(so Alice may need to ask IT support to do this). A user then needs to
be created with the appropriate permissions to access this database.
Alice will need to make a note of the username and password, which
will need to go in the `passdb_connect()` function in `config.php`
(see below).

The `pass.sql` file has the code to create all the tables for this
database, but first this file will need to be edited to replace the following
configuration values:

 - `support@example.com` (value in the `envelope_from` setting):
   this is the "envelope from" address used whenever the frontend or
   backend sends an email. This must be set before any emails are
   sent, which means it must be set correctly before any account can be
   created. Change the `value_constraints` regular expression as
   applicable.

 - `no-reply@example.com` (value in the `mail_from` setting):
   this is the "from" address used whenever the frontend or
   backend sends an email. This must be set before any emails are
   sent, which means it must be set correctly before any account can be
   created. Change the `value_constraints` regular expression as
   applicable.

 - `support` (value in the `test_user_redirect` setting):
   this is only applicable if the test user is required to test the
   frontend from a student account. For example, Alice can set this
   to her own username `ans`. This may be altered later in the
   frontend.

If your university usernames or registration numbers are longer than 12
characters, you will have to edit the `users` table to make the
`username` and `regnum` fields bigger.

The tables can be created from the command line (replace _username_
and _password_ with the MySQL username and password and don't copy
the "Enter password:" or `>` prompts):
<pre>
mysql -u <em>username</em> -p
Enter password: <em>password</em>
&gt; source pass.sql
&gt; quit
</pre>

## Website Files

The `server-files` directory should have the `inc` and `html`
sub-directories. These contain files that should be copied to
`/var/www/inc` and `/var/www/html`, but note that there are some
template files that will need renaming and editing.

### Packages and Classes

The `inc` files should be:

 - `composer.json` (PHP composer project, see below)
 - `config.php` (configuration file created from `config.php-template`, see below)
 - `general.php` (general functions)
 - `Pass.php` (main class)
 - `PassSessionHandler.php` (session handler class)
 - `PassConfig.php` (configuration class created from
   `PassConfig.php-template`, see below)

Copy `composer.json` and the `.php` files over to `/var/www/inc`:

```bash
cp ~/server-files/inc/*.{php,json} /var/www/inc/
chmod 664 /var/www/inc/*.php /var/www/inc/composer.json
chgrp web-admin /var/www/inc/*.php  /var/www/inc/composer.json
```

The configuration file needs to be copied and renamed:

```bash
cp ~/server-files/inc/config.php-template /var/www/inc/config.php
cd /var/www/inc
chmod 640 config.php
chgrp www-data config.php
```

Note that 'other' ('world') doesn't have read access for `config.php` but the
web scripts need to read the file so the group is changed to
`www-data` rather than `web-admin`. Alice, as the owner of the file,
has RW access. The `passdocker` user also has read access
since the `passdocker` user belongs to the `www-data` group. Once
the file has been edited (see below), you may prefer to change the
permissions to 440 (read-only access) to prevent accidental deletion,
since this file shouldn't be added to version control.

The following functions in `config.php` need editing (Alice will likely need to ask
IT support for this information):

 - `passdb_connect` : replace `***INSERT USER HERE***` and
   `***INSERT PASSWORD HERE***` with the MySQL `passdb` user name and password (see
   above). This function is used by both the frontend and backend to connect to
   the database.

 - `connectProducer`: replace `***INSERT USER HERE***` and
   `***INSERT PASSWORD HERE***` with the RabbitMQ _producer_ username and password
   (and change the port, if applicable). The producer user is the RabbitMQ
   user that has permission to add messages to the queue.

 - `getHashedVerifier` : replace `***KEY HERE***` with your own key
    (for example, use your password manager's random generator to
    create one). This function is used to
    create the [verifier HMAC hash](README.md#tokens).

 - `encrypt2FAkey` and `decrypt2FAkey` : replace `***INSERT KEY HERE***`
    in both functions with the same key. The actual key needs to be
    256-bits, but since the `config.php` file needs to be edited in
    a text editor, the key should be supplied as a 64-digit
    hexadecimal string (see below). These functions are used for
    encrypting and decrypting the TOTP secret key.

 - `encryptDeviceData` and `decryptDeviceData` : replace `***INSERT KEY HERE***`
    with a 64-digit hexadecimal string (see below).
    These functions are used for encrypting and decrypting the
    device data when the trust setting is used.

 - `encryptRecoveryCode` and `decryptRecoveryCode` : replace `***INSERT KEY HERE***`
    with a 64-digit hexadecimal string (see below). These functions are
    used for encrypting and decrypting the account recovery codes.

A random 64-digit hexadecimal key can be created using:
```bash
php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'
```

Alice saves all these passwords and keys in her password manager's
secure vault in case the configuration file is accidentally
deleted.

Next copy the `PassConfig.php-template` file to
`/var/www/inc/PassConfig.php`:

```bash
cp ~/server-files/inc/PassConfig.php-template PassConfig.php
chmod 664 PassConfig.php
chgrp web-admin PassConfig.php
```

This file now needs to be edited:

 - `PORT` : the port number, for example, 22 (this is just used in the admin 
   "Backend Maintenance" page to provide instructions on how to upgrade the
   Docker image);

 - `SERVER` : the server name, for example, `cmp-server-01.example.com`
   (again, this information is just for admin instructions);

 - `EMAIL_DOMAIN` : any emails will be sent to the username with
   this domain, for example, `example.com` (this should match the
   email domain in `pass.sql`);

 - `WEB_PROTOCOL` : this should be either `https` or `http`,
   depending on whether or not TLS/SSL should be used;

 - `DOMAIN` : the web domain to access the Server Pass frontend, for
   example, `serverpass.cmp.example.com`;

 - `UNIVERSITY_NAME` : the common (or short) form of the university's name,
   for example, "Uni of Ex";

 - `UNIVERSITY_LONG_NAME` : the university's proper name, for
   example, "University of Example";

 - `SCHOOL_SHORT` : the school's short name, for example, "CMP";

 - `SCHOOL_NAME` : the school's long name, for example,
   "School of Computing Sciences"

 - `USER_NAME_PATTERN` : the pattern to validate a username in an HTML
   `input` element;

 - `USER_NAME_PREG` : the PHP regular expression to validate a username server-side;

 - `REG_NUM_PATTERN` : the pattern to validate a registration number in an HTML
   `input` element;

 - `REG_NUM_PREG` : the PHP regular expression to validate a registration number
   server-side;

 - `DUMMY_REG_NUMBER` : a dummy registration number for staff and
   admin users who don't have a registration number (the `regnum`
   field in the database imposes uniqueness, so this avoids the
   problem of staff inventing a registration number that doesn't clash);

 - `REG_NUM_EXAMPLE` : used as a placeholder in the registration
   number field to provide the student with an example of what their
   registration number looks like (this string will be forbidden if
   a student tries to use it as their actual registration number,
   which has happened — repeatedly);

 - `PASS_RESOURCES_HREF` : the URL where all the PASS files can be
   accessed, for example, `http://cmp.example.com/pass/` (only used in an
   example for staff or admin users in the frontend FAQ page, the
   actual URLs need to be in the XML files);

 - `SUBMISSION_SITE` : the name of the learning portal used by
   students to upload their assignments;

 - `SUBMISSION_SITE_HREF` : the URL of the learning portal used by
   students to upload their assignments, for example, `https://learn.example.com/`
   (only used to remind students where they need to
   submit the PDF that PASS creates for them);

 - `USERNAME_LABEL` : textual label for the username HTML `input` element;

 - `USERNAMES_LABEL` : textual label for the list of username HTML `input`
   elements for group projects;

 - `USERNAME_OTHER_LABEL` : an alternative name or description in case the student
   doesn't understand the meaning of the `USERNAME_LABEL` text;

 - `REGNUM_LABEL` : textual label for the registration number HTML
   `input` element;

 - `WEBMAIL` : the name of the university's email application (for
   information in the frontend FAQ);

 - `LEGAL_HREF` : the URL of the university's legal page (terms and
   conditions etc), which is placed in the footer of every page of
   the frontend website;

 - `PASS_DOWNLOAD_SITE` : the URL where admin or IT support can
   download new versions of PASS, for example, `https://cmp.example.com/pass-updates`
   (used in admin instructions).

The admin instructions are provided in the event that Alice and her
colleagues form a team with one person working on developing
the PASS applications, one person working on the frontend
website, and one person dealing with the backend.

There are also some third-party libraries needed. Most of these can
be installed with [PHP composer](https://getcomposer.org/). The dependency
information is in the `composer.json` file copied to `/var/www/inc` earlier.
The libraries are: 

 - [`php-amqplib/php-amqplib`](https://github.com/php-amqplib/php-amqplib)
   LGPL licence (to access the message queue);
 - [`PHPGangsta/GoogleAuthenticator`](https://github.com/PHPGangsta/GoogleAuthenticator)
   BSD-2-Clause license
   (for time-based one-time password two factor authentication).

The following assumes that `composer.phar` has been installed as
`/usr/local/bin/composer` and made executable (if not, use
`php composer.phar install`).

```bash
cd /var/www/inc
composer install
chmod -R o-w vender
chgrp -R web-admin vendor
```

The other third-party code is `qrcode.php` (MIT licence)
which is on [GitHub](https://github.com/kazuhikoarase/qrcode-generator/).
Download and copy to the `/var/www/inc` directory. This is used
rather than the `PHPGangsta/GoogleAuthenticator` QR code generator as
`qrcode.php` can generate the image without having to access a
third-party website (which would be blocked because of the server
firewall restrictions).

### Web Pages

The remaining website files need to be copied over to the
`/var/www/html` directory.

```
cp -R ~/server-files/html/* /var/www/html/
cd /var/www/html
chmod 664 *.php
chgrp web-admin *.php images styles pass_adm_area
chmod 664 styles/*.*
chgrp web-admin styles/*.*
chmod 644 images/*.*
chgrp web-admin images/*.*
chmod 664 pass_adm_area/*.*
chgrp web-admin pass_adm_area/*.*
```

If you want to rename the `pass_adm_area` (admin) subdirectory,
remember to edit `PassConfig.php` and change the value of `ADMIN_HREF`.

The `styles/header.html` and `styles/footer.shtml` files are
templates for each page. They have comment placeholders that are
replaced with the appropriate links. The `footer.shtml` template
also has a server-side include (SSI) command for the last modified
date. I originally considered including SHTML pages in addition to
the PHP pages, but the navigation links would have had to be
hard-coded and I decided just to have placeholders. That SSI command
is just treated as another placeholder, so there's no need for SSI
support.

The admin menu is only available for admin users. The template for
that menu is `styles/admin_menu.html`. Again the comments are
placeholders.

The file `styles/style.html` is inserted into the page header. This
sets the viewport, stylesheet (`styles/style.css`) and shortcut
icon (favicon). The style is quite simplistic.

There are two upload pages. The primary one is `upload.php` which
has a drag and drop area and a file selector that can select
multiple files, but this requires JavaScript to implement. In the
event that JavaScript isn't supported, `upload_fallback.php` is used
instead, which doesn't use JavaScript.

Unlike Pass GUI, the Server Pass frontend can't search the user's
device for project files. This is a general security and privacy
protection required by web browsers. The drag and drop and
multi-file selector makes it easier for the student to select
multiple files at once. Unfortunately, this means that unwanted
extra files can get uploaded where the student selects all files in
a directory, regardless of whether or not they are source code. The
other drawback is that any project that requires sub-directories
will have to have the relative path specified for each file, unless
the relative path has been included for required files.

The frontend will reject any files with extensions and filenames
that PASS rejects. Any files with binary content that manage to
by-pass this and get uploaded will cause the Pass CLI Server
application in the backend container to fail.

The `template.php` file is an example file that illustrates how the
`Pass` class works.

There are four types of website user:

 - `admin` : these users can access all the pages on the website and
   all the content on those pages;

 - `staff` : these users can access all the pages in the document
   root but not the pages in the admin subdirectory;

 - `student` : these users can access most of the pages in the
   document root, but there may be parts of those pages that aren't
   available;

 - `guest` : a user who isn't logged in can only access a
   limited subset of pages (home page, partial FAQ, login, create
   or verify account, password reset, resend verification).

You will need to create an account for yourself as an admin user.
Once you have set up all the files as described above, visit the
site in your web browser and create an account for yourself. You
should get an email with a verification token. If you don't, check
the PHP error log file for any error messages. You may need help
from your IT support to ensure that sendmail is configured
correctly.

Once you have verified your account, you will need to access MySQL
from the command line to change your role to `admin`. Once you have
an admin account, you can then change the role for your colleagues
from the "Admin: Users" web page (either to 'admin', if an
additional admin user is required, or to 'staff').

For example, Alice directs her browser to `serverpass.cmp.example.com`,
which shows her the home page as a guest user. She can then follow
the link to create an account. Once she has received the
verification email and verified her account, she can then go back to
the SSH command line and start the MySQL shell:

<pre>
mysql -u <em>username</em> -p
Enter password: <em>password</em>
&gt; USE passdb;
&gt; SELECT * FROM users;
</pre>
This should show one line from the users table with a primary ID of 1
(first column).
<pre>
&gt; UPDATE users SET role='admin' WHERE id=1;
&gt; quit
</pre>

Alice can now log in to her account on `serverpass.cmp.example.com`
and she should have the "Admin" menu in the navigation bar at the
top of the page.

---

 - &#x23EE; [Server Pass Setup](setupinit.md)
 - &#x23ED; Installation ⏵ [Building the PASS Docker Image](buildingimage.md)
 - &#x23F6; [Server Pass Summary](README.md)
