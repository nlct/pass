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

 * FAQ page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('FAQ');

$pass->page_header();

?>
<style>code { color: #007208; font-weight: bold; }</style>

<h2>General</h2>
<div>
<button type="button" id="whatispass" class="collapsible prominant">What is PASS?</button>
 <div class="content">
<a href="#whatispass" class="linkanchor"><sup>&#x1F517;</sup></a>
PASS helps to <strong>prepare</strong> your assignment source code into 
 a PDF that contains all the information required for submission. It’s your
 responsibility to check that the PDF accurately represents your work, and
 it’s your responsibility to submit the PDF via the method indicated in your
 assignment brief.
 <p>
 There are several applications that use the PASS library (written in Java) including:
 <ul>
  <li>PASS GUI: this has a graphical user interface and is installed on the lab 
  computers.</li>
  <li>PASS CLI: this is a command line program that can be used as an alternative to 
  PASS GUI on devices that don’t support a graphical interface.</li>
  <li>Server PASS: this is a modified version of PASS CLI designed to run inside 
  a Docker image on a server.</li>
 </ul>
 The PASS library is used by the above applications to perform the following:
 <ol>
 <li>Look up the assignment specifications provided by your lecturer in an XML file.
 This data includes:
 <ul>
  <li>the assignment title;</li>
  <li>a short tag identifying the assignment;</li>
  <li>the submission date;</li>
  <li>whether or not PASS should test your code by compiling (if appropriate)
      and running it;</li>
  <li>(optionally) any source code files that you are required to submit (identifying 
  the main file, if appropriate) and the order in which those files should be listed 
  in the PDF (“required files”);</li>
  <li>(optionally) the base name of any reports (PDF or Word) that should accompany 
  your submission (“reports”);</li>
  <li>(optionally) content to be passed to STDIN for your code to read (“input”);</li>
  <li>(optionally) the URL of any accompanying files that your code needs to read
  (“resource files”);</li>
  <li>(optionally) the names of any files that your code needs to create
  (“result files”);</li>
  <li>(optionally) any flags that must be passed to the compiler;</li>
  <li>(optionally) the URL of a custom build script to be used to compile and run 
  your source code in order to test it (instead of PASS's default compile and run 
  methods).</li>
 </ul>
 </li>
 <li>Copy all your supplied source code files to a temporary directory.
 (That is, the required files and any additional files you have provided.)</li>
 <li>Create a zip file containing all your source code.</li>
 <li>Fetch any accompanying resource files or build script from the URLs 
 provided by your lecturer.</li>
 <li>(Optionally) compile and run your source code (or run the build script)
 and capture any messages written to STDERR and STDOUT.</li>
 <li>Create a PDF containing your <?php echo PassConfig::USERNAME_LABEL; ?> and <?php echo PassConfig::REGNUM_LABEL; ?>, 
 your source code listings, and the results of the testing (compile and run stage). 
 The zip file and any reports or result files are added
 as attachments. This should be all the information required for submitting your work.
 </ol>
 <p>When you upload your project files on this website, they are saved in an upload
 directory and the job details are queued. There is a backend script running on
 this server that pops the job details off the queue and starts up a Docker 
 container that processes your files using the version of Server PASS installed
 in that Docker image. The script logs the exit code and sends you an
 email. You can then download the PDF (if one was successfully created) from
 the “<?php echo $pass->getUploadListTitle(); ?>” page. You should then check the PDF
 and submit it, if it’s correct, or fix any problems and re-upload your project files.
 </div>
</div>

<div>
<button type="button" id="whyfail" class="collapsible prominant">Why does PASS fail when my code works in my IDE?</button>
<div class="content">
<a href="#whyfail" class="linkanchor"><sup>&#x1F517;</sup></a>
There are a number of reasons why this can happen:
<ol>
<li>You haven’t supplied one or more required files. Check that you have named them 
correctly (including case).</li>
<li>You have forgotten to include some additional code files or you didn’t specify 
the correct file type (for example, if you had the selector set to “Plain Text” 
instead of “Java” for a Java project).</li>
<li>You forgot to specify that the relative directory structure must be preserved 
if your project has source code files in multiple subdirectories.
<li>You have used some platform-specific code that works on your device but 
doesn’t work on the device PASS is running on. This includes:
 <ul>
 <li>a hard-coded absolute path (for example, <code>"C:\Documents\foo.csv"</code>);
 <li>a hard-coded directory divider (for example, <code>"files\foo.csv"</code>);
 <li>assuming a current working directory that’s not the same as the application’s directory;
 <li>assuming a specific <a rel="noreferrer" href="https://en.wikipedia.org/wiki/Newline">newline</a> marker.
 </ul>
<li>You may have a subtle bug that only has a noticeable effect under certain 
conditions.</li>
<li>Your application takes a long time to run and the process timed-out.</li>
<li>You haven’t set the appropriate compiler flags in your IDE. For example,
your assignment brief may stipulate that your C code must be ANSI compliant.
In order to ensure this, your lecturer may instruct PASS to add the
<code>-ansi</code> or <code>-std=c89</code> flag to the C compiler. 
If you haven’t also set this flag on your IDE, your project may be using 
non-compliant code but your IDE isn’t alerting you to it, so your code will 
work in your IDE but not with PASS.
</li>
<li>You have used syntax from a newer version of the language that isn’t supported
by the compiler version used by PASS. If you have been instructed to conform
to a particular version (for example, Java&nbsp;11) then you shouldn’t use syntax
introduced in a newer version</li>
</ol>

<p>Remember that you will lose marks if you don’t follow the assignment brief.
PASS isn’t a compiler. It simply spawns a sub-process that runs the appropriate
compiler (for example, <span class="file">gcc</span> or <span
class="file">javac</span>). The compiler flags are set to alert you to problems
that may lose you marks. This means that you have the opportunity to fix your
code before you submit it.

<p>Other things that can make PASS fail:
<ol start="9">
 <li>The assignment provides a resource file (such as a CSV file) that your 
  code should read but your local copy has become corrupted. PASS fetches a fresh
  copy from a remote source. If your code is designed to work on the corrupted
  copy it may fail on the correct version. For example, when you downloaded the
  file and opened it on your device, the line endings may have silently been changed.
  Another possibility is that the remote file that PASS fetches is slightly
  different from the one you were supplied with. For example, the rows of a CSV file
  may be in a different order. This is done to test that your solution works in the
  general case and hasn’t been hard-coded for very specific content. (Remember that
  in real-world applications, data files are often changed as information is added,
  removed or edited.)
</li>
 <li>You tried to send PASS a forbidden file. This includes your local copy of
  a resource file (which PASS expects to download from its remote location) or
  result files (which PASS expects your application to create).</li>
 <li>You tried to send PASS a binary file misidentified as source code or plain text. The 
  only binary files that you can send to PASS are PDF or Word documents
  (for example, as an accompanying report) or specific binary file types
  that your lecturer has instructed PASS to allow. If you get errors in the form 
  “Control character U+<em>hex</em> detected” or “Keyboard character used is undefined”
  then this is the most likely cause. This can also happen if you misidentify
  the file encoding. For example, you indicated that the source code is ASCII
  or UTF-8 when it’s actually Latin&nbsp;1. Check the file encoding settings in your IDE.</li>
</ol>
</div>
</div>

<div>
<button type="button" id="whynosso" class="collapsible prominant">Why can’t I use my <?php echo PassConfig::UNIVERSITY_NAME; ?> credentials to log in?</button>
<div class="content">
<a href="#whynosso" class="linkanchor"><sup>&#x1F517;</sup></a>
There are obvious security risks with running unchecked code on a server. It’s
therefore necessary to isolate this server from the rest of the university,
which means your single sign on (SSO) credentials can’t be used.
</div>
</div>

<div>
<button type="button" id="whynosubmit" class="collapsible prominant">Why doesn’t PASS submit my work for me?</button>
<div class="content">
<a href="#" class="linkanchor"><sup>&#x1F517;</sup></a>
<ol>
 <li>You have to first check that your PDF accurately
represents your work. There may be some issues that you need to
correct before the PDF is ready for submission.</li>
 <li>This server is isolated from the rest of the university for
security reasons (since it’s being used to compile unchecked code).</li>
</ol>
</div>
</div>

<div>
<button type="button" id="email" class="collapsible prominant">How do I change my email address?</button>
<div class="content">
<a href="#email" class="linkanchor"><sup>&#x1F517;</sup></a>
This website determines your email address by appending <code>@<?php echo PassConfig::EMAIL_DOMAIN; ?></code> to
your <?php echo PassConfig::USERNAME_LABEL; ?>. Only users with a valid <?php echo PassConfig::UNIVERSITY_NAME; ?> account (including 
<?php echo PassConfig::USERNAME_LABEL; ?> and associated <?php echo PassConfig::UNIVERSITY_NAME; ?> email address) may use this site. For security reasons,
there’s no option for Server PASS to use a non-<?php echo PassConfig::UNIVERSITY_NAME; ?> email address. If you prefer to have
your emails go to a different address, you can use <?php echo PassConfig::WEBMAIL; ?> to automatically
forward or redirect messages.
</div>
</div>

<?php
if ($pass->isUserStaffOrAdmin())
{
?>
<div>
<button type="button" id="timeout" class="collapsible prominant">Staff: how do I change the timeout value?</button>
<div class="content">
<a href="#timeout" class="linkanchor"><sup>&#x1F517;</sup></a>
For Server PASS, an administrator can change the timeout value in the configuration
page. (Remember that with Server PASS jobs are queued so the longer the timeout
the longer it may take to start the next job if there’s a particularly long
running application.)

<p>For PASS GUI installed on the lab computers, the user can change the timeout
value using the settings menu item. This can be overridden in the 
<span class="file">resources.xml</span> file using the <code>processes</code> tag.
For example:
<pre>
&lt;processes timeout="360" /&gt;
</pre>
If this is set the user won’t be able to change it. If the timeout value is
set in the local <span class="file">resources.xml</span> file, it will only
affect that local installation of PASS.
</div>
</div>
<?php
}
?>

<div>
<button type="button" id="allowedfiles" class="collapsible prominant">What Type of Files Can I Upload?</button>
<div class="content">
<a href="#allowedfiles" class="linkanchor"><sup>&#x1F517;</sup></a>
You can upload text files that contain your source code, but
don’t include any text files (such as CSV files) that you have been provided with that
your project needs to read.

<p>PASS is designed to produce a PDF containing source code listings,
with the source code also provided in a zip attachment. Source code
should be in plain text files with the encoding set to one of: ASCII, 
Latin&nbsp;1 (ISO-8859-1) or UTF-8.

<p>Your assignment specification may require a set of files with
specific names (case-sensitive). If so, make sure that you have followed the
requirements.

<p>You may also include a report as either a PDF (.pdf) or Word
(.doc or .docx) file. This will be attached to the PDF created by
PASS. If the report is a PDF file then it will also be incorporated
into the document (so that it can be viewed without downloading the attachment).
It’s therefore better to supply a PDF rather than a Word file. (You can export
to PDF from Word.)

<p>PASS does not permit any other type of binary file to be uploaded
(such as PNG or JPEG) unless your lecturer has specifically allowed it 
for a particular assignment. If you are unsure of the difference between
binary files, text files and file encodings, see the article <a rel="noreferrer"
href="https://www.dickimaw-books.com/blog/binary-files-text-files-and-file-encodings">Binary
Files, Text Files and File Encodings</a>.
</div>
</div>
<div>

<button type="button" id="encoding" class="collapsible prominant">What’s the
difference between ASCII, Latin&nbsp;1 and UTF-8?</button>
<div class="content">
<a href="#encoding" class="linkanchor"><sup>&#x1F517;</sup></a>
When you upload your project files on this site, there is an
encoding selector which gives you a choice of ASCII, Latin&nbsp;1 or
UTF-8. This should be set to the appropriate encoding. If you set it
incorrectly, some characters may appear incorrect in the PDF 
(or not appear at all) and it will trigger warnings and error messages.

<p>Computers internally store characters (such as <code>a</code> or
<code>$</code>) as numbers (or, more specifically, as binary information). The
encoding specifies the mapping between a number (as a byte or sequence of
bytes) and the corresponding character. For example, the binary value
<code>01001110</code> (78 in decimal) represents the Latin capital
<code>N</code> and <code>01101110</code> (110 in decimal) represents the Latin
small <code>n</code>.  Computer programmers commonly use hexadecimal values.
For example, 0x4E for Latin capital <code>N</code> and 0x6E for Latin small
<code>n</code>.

<p>ASCII (or, more strictly, US-ASCII) provides mappings for 128
characters, ranging from 0x0 (the null character) to 0x7F (the delete
character). <strong>Anything outside this range is invalid.</strong>
The characters from 0x0 to 0x20 and the final character
0x7F are non-printable characters (control codes) that include
the horizontal tab (0x9), line feed (0xA), form feed (0xC), carriage
return (0xD) and the space character (0x20).

<p>The codes from 0x21 to
0x2F represent the following punctuation characters: 
<code>!</code>&nbsp;(exclamation mark), <code>"</code>&nbsp;(straight quote), 
<code>#</code>&nbsp;(hash), <code>$</code>&nbsp;(dollar), <code>%</code>&nbsp;(percent),
<code>&amp;</code>&nbsp;(ampersand), <code>'</code>&nbsp;(straight
apostrophe), <code>(</code>&nbsp;(left parenthesis), <code>)</code>&nbsp;(right
parenthesis), <code>*</code>&nbsp;(asterisk), <code>+</code>&nbsp;(plus),
<code>,</code>&nbsp;(comma), <code>-</code>&nbsp;(hyphen-minus),
<code>.</code>&nbsp;(full stop or period), <code>/</code>&nbsp;(solidus or
forward slash).

<p>The codes from 0x30 to 0x40 start with the decimal
digits: <code>0</code> to <code>9</code> then 
<code>:</code>&nbsp;(colon), <code>;</code>&nbsp;(semi-colon), <code>&lt;</code>&nbsp;(less than),
<code>=</code>&nbsp;(equals), <code>&gt;</code>&nbsp;(greater than),
<code>?</code>&nbsp;(question mark), <code>@</code>&nbsp;(at).

<p>The codes from 0x41 to 0x5A represent the Latin capitals
<code>A</code> to <code>Z</code>. These are followed by
<code>[</code>&nbsp;(left square bracket), <code>\</code>&nbsp;(backslash),
<code>]</code>&nbsp;(right square bracket), <code>^</code>&nbsp;(circumflex),
<code>_</code>&nbsp;(underscore), <code>`</code>&nbsp;(grave or backtick).

<p>The codes from 0x61 to 0x7A represent the Latin lower case
<code>a</code> to <code>z</code>. (So you can obtain the lower case by
simply adding 0x20 to the corresponding capital.) Then follows:
<code>{</code>&nbsp;(left curly bracket), <code>|</code>&nbsp;(vertical line
or pipe), <code>}</code>&nbsp;(right curly bracket) and <code>~</code>&nbsp;(tilde).

<p>Note that ASCII doesn’t include accented characters (such as
<code>é</code>), other currency symbols (such as <code>£</code>), long dashes
(such as <code>—</code>), or “smart quotes”. (Some fonts may render the
straight quote <code>"</code> and straight apostrophe <code>'</code> with a
curl so they have the appearance of smart closing quotes but they are different
characters.)

<p>ASCII is a very limited set of characters but it forms the subset
of many encodings so it’s the most portable. <strong>Only select
ASCII if you’re sure that you have no non-ASCII characters in your
code or written to STDOUT/STDERR.</strong> For example, suppose
your (Java) code contains:
<pre>
System.out.println("\u00A3");
</pre>
then this source code only contains ASCII but a non-ASCII character will be
written to STDOUT. In this case, select UTF-8 not ASCII.

<p>Latin&nbsp;1 (or ISO-8859-1) has mappings ranging from 0x0 to 0xFF.
As with ASCII, every character is represented by a single byte.
The first 128 characters are identical to ASCII. The range 0x80 to
0x9F aren’t printable. The remaining characters from 0xA0 to 0xFF
consists of additional punctuation and symbols (such as <code>£</code>) and
extended Latin characters (such as <code>é</code> and <code>ø</code>).
This doesn’t include characters such as “smart quotes”, long
dashes or emoticons and doesn’t include any non-Latin alphabets.

<p>UTF-8 is a variable-width character encoding using one to four one-byte code units
that identify the Unicode codepoint and covers all Unicode characters. 
The first 128 characters are identical to ASCII,
with each character identified by a single byte. Outside of that range, characters
are identified by multiple bytes.

<p>For example, the hash character <code>#</code> is represented by a single
byte <code>00100011</code> with all three types of encoding 
(ASCII, Latin&nbsp;1 and UTF-8). Whereas the character <code>£</code> can’t be
represented in ASCII, but is represented in Latin&nbsp;1 with the single byte
<code>10100011</code> (0xA3) and is represented in UTF-8 with two bytes
<code>11000010</code> (0xC2) and <code>10100011</code> (0xA3). So if you 
misidentify a UTF-8 file as Latin&nbsp;1, those two bytes will be treated as two
separate characters (<code>&#xC2;</code> and <code>&#xA3;</code>) 
instead of as a single character (<code>£</code>). If you misidentify the file as
ASCII then both bytes are invalid, as they are both outside the valid range 
(&gt;&nbsp;0x7F).

<p>So if you selected ASCII or Latin&nbsp;1 and any non-ASCII character appears
as two or more characters (such as “&#xC2;&#xA3;” instead of “£”) then you
should’ve chosen UTF-8.

<p>UTF-8 is by far the most common encoding on the web and if
you have any non-ASCII characters this is the best encoding to use.
There are many other encodings, such as UTF-16, but these aren’t supported by PASS.
<strong>As a general rule of thumb, choose UTF-8</strong> but make
sure that your IDE is set to UTF-8.

</div>
</div>

<h2>Error Messages</h2>
<div>
<button type="button" id="nomain" class="collapsible prominant">Can't compile: no main file.</button>
<div class="content">
<a href="#nomain" class="linkanchor"><sup>&#x1F517;</sup></a>
A Java project must have the main file identified in the assignment XML file.
Check that you have supplied all the required files and that they match the
required names (case-sensitive).

<?php
if ($pass->isUserStaffOrAdmin())
{
?>
<p>Staff: check that the assignment XML file contains the <code>mainfile</code> tag.
<?php
}
?>
</div>
</div>

<div>
<button type="button" id="nodotjava" class="collapsible prominant">Class names, '<em>filename</em>', are only accepted if annotation processing is explicitly requested</button>
<div class="content">
<a href="#nodotjava" class="linkanchor"><sup>&#x1F517;</sup></a>
You have identified a file (<em>filename</em>) as Java source code, but it doesn’t 
end with the extension <span class="file">.java</span> (case-sensitive).
</div>
</div>
<div>

<button type="button" id="controlchar" class="collapsible prominant">Control character U+<em>hex</em> detected</button>
<div class="content">
<a href="#controlchar" class="linkanchor"><sup>&#x1F517;</sup></a>
You have either supplied a binary file identified as source code/plain text or you have 
identified the wrong file encoding. For example, you have told PASS that the file
is UTF-8 when it’s actually Latin&nbsp;1.

<p>Alternatively, it may mean that your application writes binary content to
STDOUT, which can’t be rendered in the PDF.
</div>
</div>

<div>
<button type="button" id="bom" class="collapsible prominant">Java compiler error: illegal character: '\ufeff'</button>
<div class="content">
<a href="#bom" class="linkanchor"><sup>&#x1F517;</sup></a>
This error indicates that a file contains the Unicode character U+FEFF
(typically right at the start of the file).  The Java compiler doesn’t allow
the <a rel="noreferrer" href="https://en.wikipedia.org/wiki/Byte_order_mark">byte order mark (BOM)</a> even with the <code>-encoding utf8</code> setting.
This character is invisible but must be deleted in order for the source code 
to compile. Unfortunately some text editors automatically silently insert this 
character. The purpose of the BOM is to indicate endianness for UTF-16. 
It has no meaning for UTF-8.
</div>
</div>
<div>

<button type="button" id="inputenc" class="collapsible prominant">Package inputenc Error: Keyboard character used is undefined</button>
<div class="content">
<a href="#inputenc" class="linkanchor"><sup>&#x1F517;</sup></a>
You have either supplied a binary file identified as source code/plain text or you have 
identified the wrong file encoding. For example, you have told PASS that the file
is ASCII when it’s actually UTF-8 or Latin&nbsp;1.

<p>Alternatively, it may mean that you have specified ASCII but your application
writes non-ASCII content to STDOUT.
</div>
</div>

<div>
<button type="button" id="missingchar" class="collapsible prominant">Missing character: There is no <em>character</em> (U+<em>hex</em>) in font [<em>font-identifier</em>]</button>
<div class="content">
<a href="#missingchar" class="linkanchor"><sup>&#x1F517;</sup></a>
You have either supplied a binary file identified as source code/plain text or you have 
identified the wrong file encoding. For example, you have told PASS that the file
is ASCII or UTF-8 when it’s actually Latin&nbsp;1.

<p>Alternatively, it may mean that your application writes binary content to
STDOUT, which can’t be rendered in the PDF.
</div>
</div>

<div>
<button type="button" id="invalidutf8" class="collapsible prominant">String contains an invalid utf-8 sequence.
</button>
<div class="content">
<a href="#invalidutf8" class="linkanchor"><sup>&#x1F517;</sup></a>
You have either supplied a binary file identified as source code/plain text or you have 
identified the wrong file encoding. For example, you have told PASS that the file
is UTF-8 when it’s actually Latin&nbsp;1.

<p>Alternatively, it may mean that your application writes binary content to
STDOUT, which can’t be rendered in the PDF.
</div>
</div>
<div>

<button type="button" id="malformedinput" class="collapsible prominant">java.nio.charset.MalformedInputException</button>
<div class="content">
<a href="#malformedinput" class="linkanchor"><sup>&#x1F517;</sup></a>
This exception is thrown when an input bytes sequence isn’t legal for the
given charset, so it most likely means that you have misidentified the file
encoding or you supplied a binary file as source code/plain text. For example, you have 
told PASS that the file is UTF-8 when it’s actually Latin&nbsp;1.

<p>Alternatively, it may mean that your application writes binary content to
STDOUT, which can’t be rendered in the PDF.
</div>
</div>

<div>
<button type="button" id="duplicate" class="collapsible prominant">ZipException: duplicate entry</button>
<div class="content">
<a href="#duplicate" class="linkanchor"><sup>&#x1F517;</sup></a>
You have accidentally uploaded the same file multiple times (in the same upload job).
Alternatively, you have uploaded multiple files with the same name and haven’t supplied their relative paths.
</div>
</div>

<div>
<button type="button" id="maxoutput" class="collapsible prominant">Output size (<em>number</em> bytes) exceeds maximum setting (<em>number</em> bytes). Truncating with [...]</button>
<div class="content">
<a href="#maxoutput" class="linkanchor"><sup>&#x1F517;</sup></a>
To prevent excessively large PDF files, PASS will truncate content. You may encounter
this if you have a particularly verbose application that writes vast amounts
of information to STDOUT.
</div>
</div>

<div>
<button type="button" id="nofiles" class="collapsible prominant">InvalidSyntaxException: At least one file must be specified.</button>
<div class="content">
<a href="#nofiles" class="linkanchor"><sup>&#x1F517;</sup></a>
You didn’t upload any files.
</div>
</div>

<button type="button" id="assignmentundef" class="collapsible prominant">Can't find assignment '<em>label</em>'</button>
<div class="content">
<a href="#assignmentundef" class="linkanchor"><sup>&#x1F517;</sup></a>
It may be that the assignment XML file has been changed since you logged in.
Try logging out and log back into this site to clear the session data.

<?php
if ($pass->isUserStaffOrAdmin())
{
?>
<p>Staff: admin users can force everyone to login again by clearing the session data.
<?php
}
?>
</div>
</div>

<div>
<button type="button" id="norabbitmq" class="collapsible prominant">RabbitMQ may have gone offline</button>
<div class="content">
<a href="#" class="linkanchor"><sup>&#x1F517;</sup></a>
If the upload page produces the error "Failed to publish message.
RabbitMQ may have gone offline" then please contact your lecturer.
Your files should have successfully been uploaded to the server but
the error means that the queuing system has failed. An administrator
is required to restart the message queue and requeue your submission.

<?php
if ($pass->isUserStaffOrAdmin())
{
?>
<p>Admin: once RabbitMQ has been restarted, go to the "Upload
Directories" page, select the uploaded jobs that failed to be
queued, and click on the "Requeue Selected" button.
<?php
}
?>
</div>
</div>

<?php
if ($pass->isUserStaffOrAdmin())
{
?>
<div>
<button type="button" id="datetimeparse" class="collapsible prominant">DateTimeParseException: Text '<em>content</em>' could not be parsed</button>
<div class="content">
<a href="#datetimeparse" class="linkanchor"><sup>&#x1F517;</sup></a>
The due timestamp should be in the form <code>YYYY-MM-DD hh:mm</code>. Note that
the year must have four digits and the other values must have two digits.
</div>
</div>

<h2>Staff: XML Assignment Files</h2>
<div>
<button type="button" id="xmledit" class="collapsible prominant">I’ve modified the XML file but the changes aren’t showing up on this site</button>
<div class="content">
<a href="#xmledit" class="linkanchor"><sup>&#x1F517;</sup></a>
The course and assignment details are stored in session data so that
the XML files aren’t repeatedly fetched whenever you move from one
page to the next. If you modify the XML files, you will have to
clear the session data to force the data to be refreshed.
This can be done for your own session data by logging out
and then back in again. 

<p>Admin users can force everyone to login again by clearing the session data.
</div>
</div>
<div>

<div>
<button type="button" id="compilerarg" class="collapsible prominant">How do I specify compiler flags?</button>
<div class="content">
<a href="#compilerarg" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>compiler-arg</code> tag for each argument within the 
<code>assignment</code> tag. For example:
<pre>
  &lt;compiler-arg&gt;-std=c89&lt;/compiler-arg&gt;
  &lt;compiler-arg&gt;-pedantic&lt;/compiler-arg&gt;
</pre>
</div>
</div>

<div>
<button type="button" id="resourcefile" class="collapsible prominant">How do I specify a file that the project code must read?</button>
<div class="content">
<a href="#resourcefile" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>resourcefile</code> tag for each file within the 
<code>assignment</code> tag with the <code>src="<em>uri</em>"</code> attribute set to
the location of the file. For example:
<pre>
&lt;resourcefile src="<?php echo $pass->getExampleSrc(); ?>/foo.csv"/&gt;
</pre>
</div>
</div>

<div>
<button type="button" id="resultfile" class="collapsible prominant">How do I specify a file that the project code must create?</button>
<div class="content">
<a href="#resultfile" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>resultfile</code> tag for each file within the 
<code>assignment</code> tag with the <code>name="<em>filename</em>"</code> attribute set to
the file name and the <code>type="<em>mime</em>"</code> attribute set to
the file’s mime type. For example:
<pre>
&lt;resultfile type="image/png" name="image.png" /&gt;
&lt;resultfile type="text/plain" name="output.txt"/&gt;
</pre>
If the application successfully creates the file, it will be added to the PDF
as an attachment. If the mime type is supported, PASS will also display it in the PDF
(verbatim for text files or as an included graphic for an image file).
</div>
</div>

<div>
<button type="button" id="input" class="collapsible prominant">How do I specify content that the project code should read from STDIN?</button>
<div class="content">
<a href="#input" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>input</code> tag for each line of text within the 
<code>assignment</code> tag. For example:
<pre>
   &lt;input&gt;Line 1&lt;/input&gt;
   &lt;input&gt;Line 2&lt;/input&gt;
</pre>
</div>
</div>

<div>
<button type="button" id="report" class="collapsible prominant">How do I specify that a report should be included in the PDF?</button>
<div class="content">
<a href="#report" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>report</code> tag within the <code>assignment</code> tag.
The content should be the base name of the report file, which may have one of the 
following extensions: <span class="file">.pdf</span>, <span class="file">.doc</span>, 
<span class="file">.docx</span>. For example:
<pre>
   &lt;report&gt;project-report&lt;/report&gt;
</pre>
<p>This will expect a file called <span class="file">project-report.pdf</span>,
<span class="file">project-report.doc</span> or 
<span class="file">project-report.docx</span>. (Don’t have spaces in the
filename.)
</div>
</div>

<div>
<button type="button" id="allowedbinary" class="collapsible prominant">How do I allow the students to upload certain binary files?</button>
<div class="content">
<a href="#" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>allowedbinary</code> tag within the <code>assignment</code> tag.
This should have the attribute <code>ext</code> set to the comma-separated list
of allowed file extensions (no leading dot) and <code>type</code> set to the 
corresponding MIME type. The binary files will be attached to the PDF. Images may
also be shown in the PDF. If you want to allow this, use the attribute
<code>listing="true"</code>. This attribute will have no effect 
if the MIME type doesn’t start with <code>image/</code>. The end tag may be omitted.
For example, to allow JPEG and PNG files:
<pre>
  &lt;allowedbinary ext="png" type="image/png" listing="true" /&gt;
  &lt;allowedbinary ext="jpeg,jpg" type="image/jpeg" listing="true" /&gt;
</pre>
</div>
</div>

<div>
<button type="button" id="lineseparator" class="collapsible prominant">How do I make PASS default to Windows line endings for Java applications?</button>
<div class="content">
<a href="#" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>invoker-arg</code> element:
<pre>
  &lt;invoker-arg&gt;-Dline.separator=&amp;#x0D;&amp;#x0A;&lt;/invoker-arg&gt;
</pre>
Note the use of XML entities to specify the carriage return and line feed
characters.
</div>
</div>

<div>
<button type="button" id="norun" class="collapsible prominant">How do I switch off the automatic compile and run test?</button>
<div class="content">
<a href="#norun" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>compile="false"</code> attribute in the <code>assignment</code> tag.
This will automatically switch off the run test as there won’t be an application to run.
If the assignment is for a non-compiled language (such as bash or Perl)
or if you want the code compiled but not run then just use <code>run="false"</code>.
</div>
</div>

<div>
<button type="button" id="build" class="collapsible prominant">How do I replace the automatic compile and run function with a custom build script?</button>
<div class="content">
<a href="#build" class="linkanchor"><sup>&#x1F517;</sup></a>
Use the <code>build="<em>uri</em>"</code> attribute in the <code>assignment</code> tag
where <em>uri</em> is a reference to the build script.
</div>
</div>

<div>
<button type="button" id="xmlspecs" class="collapsible prominant">Where can I find the full specifications for the XML file?</button>
<div class="content">
<a href="#xmlspecs" class="linkanchor"><sup>&#x1F517;</sup></a>
On <a rel="noreferrer" href="https://github.com/nlct/pass/tree/main/docs">GitHub</a>.
</div>
</div>

<div>
<button type="button" id="passchecker" class="collapsible prominant">Is there a way of detecting if the PDF has been modified after PASS created it?</button>
<div class="content">
<a href="#passchecker" class="linkanchor"><sup>&#x1F517;</sup></a>
With Server PASS, you can check the 
<?php echo $pass->get_upload_lists_link('upload logs');?> to find out 
when the project was uploaded and fetch the original PDF.
Alternatively, use the “Export” button to download a tab-separated variable file,
which contains the MD5 sums of the PDF files. This can then be compared against
the MD5 sum of the submitted PDF.

<p>For non-server versions of PASS, try the <span
class="file">pass-checker</span> command line application, which can be
downloaded from <a
href="<?php echo PassConfig::PASS_DOWNLOAD_SITE; ?>/pass-checker.zip"
class="file"><?php echo PassConfig::PASS_DOWNLOAD_SITE; ?>/pass-checker.zip</a>.  PASS saves
encrypted information in some custom PDF metadata, which <span
class="file">pass-checker</span> compares against the unencrypted metadata, and
it will flag any discrepancies. It’s not foolproof and is mainly a check for
the timestamp. 

</div>
</div>

<!--
<div>
<button type="button" id="" class="collapsible prominant">Question</button>
<div class="content">
<a href="#" class="linkanchor"><sup>&#x1F517;</sup></a>
Answer
</div>
</div>
-->

<?php
}

?>
<script>
if (window.location.hash)
{
   var elem = document.getElementById(window.location.hash.substring(1));

   if (elem)
   {
      elem.classList.add("active");

      var content = elem.nextElementSibling;

      content.style.display = "block";
   }
}
<?php
echo $pass->writeCollapsibleJavaScript(false);
?>
</script>
<?php
$pass->page_footer();
?>
