<?php
/*
 * Server PASS
   Copyright 2022 Nicola L. C. Talbot

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 * Backend maintenance page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Backend Maintenance');
$pass->require_admin();// ensure user is logged in as admin

$pass->page_header();

$pass->debug_message('Debugging mode ON.');
$log_file = htmlentities(ini_get('error_log'));

$inc_path = realpath($_SERVER['DOCUMENT_ROOT'].'/../inc');

?>
<p>The information on this page requires the ability to actually
login (via ssh or sftp) to the server.
See also the documentation on <a rel="noreferrer" href="https://github.com/nlct/pass/tree/main/docs">GitHub</a>.
<ul>
<li><a href="#troubleshooting">Troubleshooting</a>
<li><a href="#restartingrabbitmq">Restarting RabbitMQ</a>
<li><a href="#stoppingbackend">Stopping the PASS Backend</a>
<li><a href="#restartingbackend">Restarting the PASS Backend</a>
<li><a href="#upgrade">Installing a New Version of the Server PASS Backend</a>
<li><a href="#clear">Clearing Old Data</a>
<li><a href="#newpages">Adding a New Page to the Website</a>
<li><a href="#newcli">Creating a New Command Line PHP Script</a>
</ul>

<h2 id="troubleshooting">Troubleshooting</h2>

<p>If a web page doesn't seem to be working check the PHP log file
for errors. This requires logging into the server:
<pre>
<?php echo $pass->getSshInstructions(); ?>
</pre>
The log file is <span class="file"><?php echo $log_file; ?></span>.
Also check the RabbitMQ docker container is still running:
<pre>
docker ps -a
</pre>
If this shows "Exited" for rabbitmq then that means RabbitMQ has
stopped running, which will have stopped the PASS backend as well.
RabbitMQ will need to be restarted before the PASS backend can be
restarted (see below). You will also need to requeue any jobs that were
uploaded after RabbitMQ stopped. This can be done from the
<?php echo $pass->get_admin_upload_dirs_link(); ?> page. Select the
submissions that need requeuing and click on the "Requeue Selected"
button at the bottom.

<p>If PASS has produced error messages relating to invalid characters
there is a Perl script <span class="file"><?php echo $pass->getDockerPath(); ?>/findnonascii.pl</span>
that can be used to find non-ASCII characters in submitted files.
(You can also find this script on <a rel="noreferrer"
href="https://tex.stackexchange.com/a/174737">TeX on StackExchange</a>.)

<h2 id="restartingrabbitmq">Restarting RabbitMQ</h2>

<p>To restart RabbitMQ do:
<pre>
docker start rabbitmq
</pre>
You will need to restart the backend as well (see below).

<h2 id="stoppingbackend">Stopping the PASS Backend</h2>

<p>If you need to take the backend offline, first check <?php echo
$pass->get_admin_whosonline_link(); ?> and <?php echo
$pass->get_upload_lists_link('View Uploads'); ?> to make sure no one
is currently using it or has pending jobs.

<ol>
<li>Use the <?php echo $pass->get_admin_config_link('configuration page'); ?> to add an appropriate banner message to let users know
that the backend is offline. For example, "Backend currently
offline." You may also want to consider switching the Server Pass
website to maintenance mode to prevent anyone from uploading a
project, but remember that is you logout while maintenance mode is
on you will have to remember the login URL as there won't be a link
to it. However, the banner message may be sufficient.

<li>Switch to passdocker user:
<pre>
sudo su - passdocker
</pre>
<li>Find the process ID:
<pre>
ps aux | grep passconsumer
</pre>
<li>If this only shows the grep command then the backend is already
down otherwise kill the process using the process ID <em>N</em> obtained 
from the previous command (if it's currently running):
<pre>
kill -9 <em>N</em>
</pre>
<li>Check for any stray containers that haven't been deleted:
<pre>
docker ps -a
</pre>
(This is only likely to happen if the <span class="file">passconsumer.php</span> script was
interrupted before it was able to delete a container it had started.)
<strong>Be careful not to delete the rabbitmq container!</strong>
</ol>

<h2 id="restartingbackend">Restarting the PASS Backend</h2>

<ol>
<li>Switch to passdocker user:
<pre>
sudo su - passdocker
</pre>
<li>You may want to clear the log files <span
class="file">nohup.out</span> and <span
class="file">passdocker.log</span>. For example:
<pre>
vi nohup.out
:1
dG
:wq
</pre>
similarly for <span class="file">passdocker.log</span>.
Note that if these files have been deleted you will need to create them again with user
"passdocker" (rw) and group "www-data" (r):
<pre>
touch nohup.out passdockerlog
chgrp www-data nohup.out passdocker.log
chmod 0640 nohup.out passdocker.log
</pre>
These files are parsed by the <?php echo $pass->get_admin_viewlogs_link(); ?> page
so it's useful to remove the messages from old assignments to reduce
the clutter.

<li>Start the backend:
<pre>
nohup ./passconsumer.php &amp;
</pre>
This should produce the message:
<pre>
nohup: ignoring input and appending output to 'nohup.out'
</pre>
Press Enter to get a clear command prompt.
If you get the error message:
<pre>
[1]+ Exit 255
</pre>
then check that RabbitMQ is still running (see above). Also check
the log files for error messages. If you still can't find out the
problem, try running the script in the foreground:
<pre>
./passconsumer.php
</pre>
The normal operation of this script is to wait silently until it receives a
message from the queue, so if it's working properly and there are no
jobs it won't appear to be doing anything. Exit with <code>^C</code>
(Control+C) and try running it again with nohup.

<li>If applicable, switch the Server Pass website back to normal mode and clear
the banner using
the <?php echo $pass->get_admin_config_link('configuration page'); ?>.

<li>Do a test upload to check that it's working properly.
</ol>

<h2 id="upgrade">Installing a New Version of the Server PASS Backend</h2>

<p>To install a new version of Server PASS:
<ol>
<li>Take the backend offline (see above).

<li>Download <?php echo $pass->getPassServerCliDownloadLink(); ?>.
<li>Copy the file to the server:
<pre>
<?php echo $pass->getSftpInstructions(), PHP_EOL; ?>
put pass-cli-server.tgz
quit
</pre>
<li>Login to the server:
<pre>
<?php echo $pass->getSshInstructions(); ?>
</pre>
<li>Unpack archive:
<pre>
tar zxvf pass-cli-server.tgz
</pre>
This should create the following files:
<pre>
pass-cli-server/bin/pass-cli-server
pass-cli-server/lib/dictionary/passcli-en.xml
pass-cli-server/lib/dictionary/passlib-en.xml
pass-cli-server/lib/pass-cli-lib.jar
pass-cli-server/lib/pass-cli-server.jar
pass-cli-server/lib/passlib.jar
pass-cli-server/lib/resources.xml
pass-cli-server/Dockerfile
</pre>

<li>If any changes need to be made to the URLs in 
<span class="file">pass-cli-server/lib/resources.xml</span> make sure that
the appropriate changes are also made to 
<span class="file"><?php echo $_SERVER['DOCUMENT_ROOT']; ?>/resources.xml</span>.
(This usually won't need to be done unless the remote resources file
has been moved to a new location.)

<li>Change to the <span class="file">pass-cli-server</span> directory:
<pre>
cd pass-cli-server
</pre>
Build the Docker image:
<pre>
docker build --network=host --tag pass:latest .
</pre>
Check the Docker images:
<pre>
docker images
</pre>
This should list pass, rabbitmq and debian. If the list includes
&lt;none&gt; then that's probably an old image that can be deleted:
<pre>
docker image rm <em>image ID</em>
</pre>
<li>Restart the backend (see above).
<li>Upload a test project and make sure that the status changes to
"processed". Download the PDF and check the PASS version number on
the first page.
</ol>

<h2 id="clear">Clearing Old Data</h2>

<p>You may need to periodically delete old data that's no longer
required. <strong>You may want to consider archiving them first.</strong>

<p>Uploads are located in <span class="file"><?php echo $pass->getUploadPath(); ?></span>.
These have read and write access for the "www-data" user (apache),
only read access for the "www-data" group and no access for other.
You can delete the uploaded data from the <?php echo
$pass->get_admin_upload_dirs_link(); ?> page.  Select the unwanted
directories and click on the "Delete Selected" button. You can
change the maximum number of items per page in the 
<?php echo $pass->get_admin_config_link('configuration page'); ?>,
which will allow you to select more in one go.

<p>The log file <span class="file"><?php echo $log_file; ?></span> file, may also need
to be periodically emptied. This has read and write permissions for
the "www-data" group (which "passdocker" belongs to) so this can be
cleared in the same way as the <span class="file">nohup.out</span>
and <span class="file">passdocker.log</span> files (described
above).

<p>The action recorder data can be cleared on the <?php echo $pass->get_admin_view_action_recorder_link(); ?> page.

<p>The processed files are in the 
<span class="file"><?php echo $pass->getCompletedPath(); ?></span> directory.
Only the passdocker user has write permission for these files.
For example, to delete all files uploaded in 2020:
<pre>
sudo su - passdocker
rm -r completed/2020-*
</pre>

<p>Note that just deleting the uploaded and processed files doesn't
remove the submission data from the database.

<p>Users can be individually deleted from the <?php echo
$pass->get_admin_users_link('Users page'); ?> but if you want to
bulk delete users you will need to login to the MySQL
server. Below is an example that only deletes the user
accounts for any student user who created an account over
four years ago but it doesn't delete their corresponding entries in
other tables:
<pre>
DELETE FROM users WHERE role='student' AND account_creation &lt; DATE_SUB(NOW(), INTERVAL 4 YEAR);
</pre>
<p>Remember to specify <code>role='student'</code> otherwise you
will also delete staff and admin users!

<p>The above example doesn't delete the user information from other
tables. That's a bit more complex as it requires joins.
Corresponding rows will also need to be deleted from 
<code>submissions</code>, <code>projectgroup</code>, <code>tokens</code>, 
<code>recovery_codes</code>.

<p><strong>All</strong> submission data can be deleted from the database using:
<pre>
DELETE FROM submissions;
DELETE FROM projectgroup;
</pre>
<p>Other tables that can be periodically cleared:
<pre>
DELETE FROM tokens WHERE expires &lt; DATE_SUB(NOW(), INTERVAL 1 MONTH);
DELETE FROM skip_totp WHERE expires &lt; DATE_SUB(NOW(), INTERVAL 1 MONTH);
DELETE FROM action_recorder WHERE time_stamp &lt; DATE_SUB(NOW(), INTERVAL 1 MONTH);
</pre>
Session data can be deleted from the <?php
 echo $pass->get_admin_sessions_link('session data page'); ?>.

<h2 id="newpages">Adding a New Page to the Website</h2>

<p>The Server PASS website uses object oriented PHP. The main class
is <code>Pass</code> (defined in <span class="file"><?php echo $inc_path; ?>/Pass.php</span>).
There is an example file <span class="file"><?php echo $_SERVER['DOCUMENT_ROOT']; ?>/template.php</span>
that illustrates the use of this class. You basically need to start
with:
<pre>
&lt;?php

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Page Title');
</pre>
If you need to ensure that the user is logged in:
<pre>
$pass-&gt;require_login();// require login
</pre>
This must be used before anything is written to STDOUT as it will
redirect and exit the script if the user isn't logged in or is blocked or
requires verification.
<p>
If you need to ensure that the user is logged in as admin:
<pre>
$pass-&gt;require_admin();// require login as admin
</pre>
<p>This first does <code>require_login</code> to ensure the user is
logged in and then checks the user role.
<p>
If you want to know if the user is either staff or admin (i.e. not a student) use
<pre>
if ($pass-&gt;isUserStaffOrAdmin())
{
   // staff or admin code
}
else
{
   // other
}
</pre>
Or for a particular role, e.g. 'staff':
<pre>
if ($pass-&gt;isUserRole('staff'))
{
   // staff only content here
}
else
{
   // other content here
}
</pre>
<p>Or assign the role to a variable and test that instead:
<pre>
$role = $pass-&gt;getUserRole();
</pre>
The user's numeric ID (the primary key for the <code>users</code>
table) can be obtained with:
<pre>
$pass-&gt;getUserID();
</pre>
This will either return an <code>int</code> (the ID) or
<code>false</code> (not logged in).

The <code>Pass</code> class automatically connects to the
<code>passdb</code> database. To prepare a statement use:
<pre>
$stmt = $pass-&gt;db_prepare($query);
</pre>
This returns either <code>false</code> (if an error occurred) or a <code>mysqli_stmt</code> object.

<h2 id="newcli">Creating a New Command Line PHP Script</h2>

<p>The <code>Pass</code> class described above can also be used in a
PHP command line script. For example:
<pre>
#!/usr/bin/env php
&lt;php

require_once '<?php echo $inc_path; ?>/Pass.php';

$pass = new Pass();
</pre>
Note that in this case no page title is required but again the class
constructor automatically tries to connect to the
Pass database.


<?php
$pass->page_footer();

?>
