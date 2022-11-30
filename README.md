# PASS: Prepare Programming Assignments for Submission System
Set of applications to help students prepare their programming
assignments for submission. The end result is a PDF file that
contains their source code pretty-printed with all source code files
in a zip file attached to the PDF. Optionally, may also include
STDOUT/STDERR messages from the compiler and from their application
as well as any result files their application should produce.

## PURPOSE

This system was designed to help students in the School of Computing
Sciences (CMP) at the University of East Anglia (UEA) to prepare
their computer programming assignments for submission. Previously,
students had to hand-in printouts of their source code. Later, this
was changed to uploading their source code as a zip file. Both
methods had drawbacks:

 - With the original hard-copy submission, students were asked to
   order their files in a certain way that made the assignment
   easier to mark. Unfortunately, students would often send all
   their files to the printer in one batch and hand in the printouts
   in the order they came out of the printer, which was usually
   alphabetical order. While it may only take half a minute to
   re-order one submission, this adds up to a lot of wasted minutes
   for someone who has to mark 100+ submissions.

 - University regulations require the student to include their
   registration number on their coursework. The marker requires
   the student's username to log their mark. (Looking up the
   username from the registration number voids anonymity as the
   student's name is revealed in the process.)
   Sometimes one or both pieces of information is missing from
   printouts.

 - With the zip upload, the archive often contained unnecessary
   clutter, such as class files, object files, and IDE project files.
   In a few cases, the archive contained everything except the
   actual source code. Sometimes the student prepared their
   assignment on their own laptop, which turned out to be infected
   with malware that then infected the zip file.

 - Sometimes a connection error occurs when the student tries to
   submit their work, which results in a late submission.
   Conversely, a student submitting late may claim a connection
   error prevented them from submitting in time. Connection errors
   can happen, but it would be useful to have some way of
   determining if their work was ready for submission before the
   deadline.

Regardless of the submission method, there are other issues:

  - Students are provided with a file (such as a CSV file) that
    their application needs to load. These files will typically have
    an awkward case (such as a literal comma in a value) to test
    how well their solution works. Some students who can't work out
    a solution will edit the file to remove the awkward bit. Sometimes
    students hard-code a solution that's specific to the supplied
    test file that won't work for another file. Occasionally the
    file is unknowingly altered, such as when a student's computer
    silently replaced line endings when they downloaded the file.
    The student's solution was then hard-coded for the new line
    endings and didn't work with the original file.

  - The assignment application may be required to write certain
    information to STDOUT, and the student may be required to include
    a copy of this information with their submission. Occasionally
    a student may fake this information, knowing what it *should* be
    even though their source code clearly contains compiler errors.

  - The assignment brief may stipulate conformance to a certain
    standard. Some students use non-conforming code but don't set
    the appropriate compiler flags to guard against it. They then
    complain when they lose marks, even though their application
    "works".

The original Pass application provided a graphical user interface,
which was installed on lab computers, that allowed students to
select their source code files and provided a file selector to save
the PDF in their preferred location. This application is now called
"Pass GUI". Later, it was necessary to provide a command line only
version for assignments that were being developed on devices without
a graphical environment. I split off the main worker code into the
backend library `passlib.jar`. The switch to remote working during
the pandemic led to a version that can run in a Docker image on a
server.

In all cases, the source code is copied to a temporary directory in
which a LaTeX document is also created to generate a PDF of the
project. The source code may optionally be compiled and run to test
it. Note that any files that need to be read or written by the
student's application should use relative paths.

## OVERVIEW

### Course and Assignment Data

Each Pass application has a file called `resources.xml` in the `lib`
directory. This may contain settings specific to the device that
it's installed on. For example, if Pass doesn't pick up the
compiler's location from the operating system's path, you can
explicitly set the path in `lib/resources.xml`:
```xml
 <application
   name="javac"
   uri="file:///usr/java/latest/bin/javac"
 />
```
In Pass GUI, if the PDF is successfully created, a button is
available for the student to automatically open the file in a PDF
viewer. This can be done with Java's `Desktop.open(File)` method,
but this isn't guaranteed to work, so you can explicitly set the
application to view PDF files. For example:
```xml
 <application
   name="pdfviewer"
   uri="file:///usr/bin/okular"
 />
```
If something goes wrong while creating the PDF, there may also be a
button to open the log file. Again, if `Desktop.open(File)` doesn't
work, you can explicitly set the text editor. For example:
```xml
 <application
   name="editor"
   uri="file:///usr/bin/gedit"
 />
```
The main information that needs to be in the `resources.xml` file is
the data about each course that Pass may be used with. This is best
done in a single remote `resources.xml` file. Otherwise, every time
you need to add, edit or delete a course, you will have to edit
every local `lib/resources.xml` file for all Pass installations.
The following is an example `lib/resources.xml` file that sets up
some local paths for the specific Pass installation and specifies a
remote `resources.xml` file that's located at
`http://example.com/pass/resources.xml:`
```xml
<?xml version="1.0"?>
<resources>
  <!-- remote resource file -->
  <courses href="http://example.com/pass/resources.xml" />

  <!-- local settings -->
  <processes timeout="360" />

  <application
    name="javac"
    uri="file:///usr/java/latest/bin/javac"
  />

  <application
    name="pdfviewer"
    uri="file:///usr/bin/okular"
  />
</resources>
```
Each course is specified with a `resource` element that must have
the `name` attribute set to a unique label that identifies the course
and the `href` attribute set to the location of the XML file that
lists the assignments that may be prepared by Pass.

For example, the remote `resources.xml` file may contain:
```xml
<?xml version="1.0"?>
<resources>
 <resource name="CMP-101" href="http://example.com/pass/CMP-101.xml">
  An Introduction to Java
 </resource>

 <resource name="CMP-102" href="http://example.com/pass/CMP-102.xml">
  An Introduction to C and C++
 </resource>
</resources>
```
This has two courses: CMP-101 ("An Introduction to Java") and
CMP-102 ("An Introduction to C and C++"). The assignment data for
CMP-101 can be found in `http://example.com/pass/CMP-101.xml`, and
the assignment data for CMP-102 can be found in
`http://example.com/pass/CMP-102.xml`.

**All URLs must be the exact location.** Redirects (including
redirects from http to https) aren't supported.

Here's an example `CMP-101.xml` file for the hypothetical CMP-101
course:
```xml
<?xml version="1.0"?>
<assignments>

 <assignment name="helloworld">
  <title>Hello World</title>
  <due>2022-11-30 16:30</due>
  <mainfile>HelloWorld.java</mainfile>
 </assignment>

 <assignment name="shop">
  <title>Shop</title>
  <due>2023-03-30 16:30</due>
  <file>Product.java</file>
  <file>Shop.java</file>
  <mainfile>Main.java</mainfile>
  <file>UnknownProductException.java</file>
  <resourcefile src="http://example.com/pass/products.csv" />
  <resultfile type="text/plain" name="receipt.txt" />
 </assignment>

 <assignment name="imageconvert" run="false">
  <title>Graphical Image Converter</title>
  <due>2023-04-20 16:30</due>
  <mainfile>Main.java</mainfile>
  <report>projectreport</report>
  <allowedbinary ext="png" type="image/png" listing="true" />
  <allowedbinary ext="jpeg,jpg" type="image/jpeg" listing="true" />
 </assignment>

</assignments>
```
This provides data for three Java assignments. The first assignment is a
simple Hello World application where the student is required to
submit a file called `HelloWorld.java` that contains the `main`
method. The assignment is identified by the unique label `helloworld`
and has to be submitted by 2022-11-30 16:30.

The second assignment requires that the student submit four files
called: `Product.java`, `Shop.java`, `Main.java` (which has the
`main` method) and `UnknownProductException.java`. The order in
the XML file indicates the order these files should be listed in
the PDF. A student may go beyond the assignment brief and include
additional files, which will be listed afterwards (but they must be
source code not binary).

This second assignment stipulates that the student's application has
to read a file called `products.csv`. The assignment data instructs
Pass to fetch this file from `http://example.com/pass/products.csv`
but this doesn't have to be identical to the `products.csv` file
that the students are given to test their application. For example,
it may have rows switched round or may have slightly different
products. (Note that it's important that the students use relative
paths so that the fetched file can be used.)

The shop assignment also stipulates that the student's application has
to create a plain text file called `receipt.txt`. This needs to be
included in their submission. Pass will search for it after testing
the application and include it verbatim in the PDF.

The third assignment is for a graphical application. Pass is only
designed to run command line applications, so the `run` attribute is
set to `false` to skip the testing step, but Pass will still compile
the source code supplied by the student (unless the `mainfile`
element is omitted). In this case, the student must submit an
accompanying report, which may be called `projectreport.pdf` or
`projectreport.doc` or `projectreport.docx`.  PDF should be
encouraged, where possible, to allow the report to be included in
the PDF created by Pass rather than simply have it as an attachment.
This assignment allows the student to include binary files that form
part of their project, but they are restricted to PNG and JPEG
files. (Note that because Pass doesn't test the application, you
can't use the `resultfile` element.)

There are other elements that may be included with `<assignment>`.
For example, if the student's application needs to read from STDIN,
lines of input can be provided, or if the application must be
supplied command line arguments, these may be provided.
Additionally, you can supply arguments to pass to the compiler (for example,
`javac` or `gcc`) or to the invoker (for example, `java`) or you can
supply your own build script.

### What PASS does

The student runs Pass (or uses the web interface for Server PASS)
and selects the course and the assignment.
They also have to indicate whether or not this is a solo or group
project. They are expected to provide a username (an alphanumeric
identifier, which is their Blackboard ID for CMP) and their student
registration number.

With Pass GUI, the student can select a base directory and Pass will
search for any required files within that path. The student can then
select any additional source code files if they have any. (An
assignment may have no required files, in which case the student can
choose their own file names but they will have to select them all
individually.) The student may select a PDF or Word document if
their submission also requires a report. (The report file's
basename, without the pdf/doc/docx extension can be stipulated in
the assignment data, in which case Pass will search for that as well.)

Pass won't allow the student to select files that have been
identified with the `resourcefile` and `resultfile` elements. This
prevents them from submitting files that have been altered.

Once all the project files have been selected, Pass then creates a
PDF containing all the information. This is implemented as follows:

 1. Pass creates a temporary directory and copies all the files to
    it. (If it's necessary to have sub-paths, they can be copied
    relative to a base directory.)

 2. The current time is saved.

 3. A LaTeX file is created and the preamble is written according to
    various settings. The preamble code includes encrypted data
    that's written to custom PDF metadata (within `\pdfinfo`).

 4. A zip file is created containing all the source code files. The
    code to attach it is written to the LaTeX file (with `\attachfile`
    provided by `attachfile.sty`).

 5. For each selected file:

    - the appropriate `\lstinputlisting` command will be written,
      if the file's identified language is known to be supported
      by `listings.sty`;

    - otherwise a plain text file will be input verbatim;

    - if an allowed binary has been include, this will be attached
      and, if an image, can be included with `\includegraphics`;

    - if a PDF or Word document has been selected, the
      file will be attached (with `\attachfile`). In the case of a PDF
      report, this can also be included with `\includepdf`, if
      supported by `pdfpages.sty` (avoid spaces in the filename).

    If the filename is forbidden, an error occurs. Forbidden filenames
    include files identified with `resourcefile` or `resultfile`,
    `a.out`, or have certain binary extensions (such as `exe`, `o`
    or `class`). GUI versions of Pass should have already flagged
    this when the files were selected.

 6. If the application is compilable (Java, C or C++ assignments)
    and a main file has been identified with the `mainfile` element,
    then the source code will be compiled. All messages from the
    compiler to STDOUT and STDERR will be added verbatim to the
    LaTeX document. This step is skipped for scripting languages.

 7. The student's application is then run. All messages to STDOUT and STDERR
    will be added verbatim to the LaTeX document.

 8. If the assignment data specifies that the application should
    create one or more files, Pass will search for
    these files in the temporary directory.

 9. The LaTeX document is compiled (using either PDFLaTeX or LuaLaTeX).

 10. The PDF can then be saved in the student's preferred location.
     The student must then check the PDF before they submit it using
     the designated submission system.

**It's important for students to understand that Pass doesn't submit
their work.**

Pass will search the LaTeX log file for signs that any binary files
have been erroneously included as source code and will issue a warning if found.
Students also need to understand what
[file encoding](https://dickimaw-books.com/blog/binary-files-text-files-and-file-encodings/)
is.

For example, suppose in the shop assignment their application needs
to write the price and they have the non-ASCII `£` symbol in their code. This
means that their source code isn't an ASCII file. Pass only supports
three encodings: ASCII, Latin-1 and UTF-8. The student must identify
the file encoding of their source code if it contains non-ASCII
characters. Pass will use LuaLaTeX for UTF-8 and PDFLaTeX otherwise.
If the student mis-identifies the file encoding, the resulting PDF
will have odd or missing characters. Pass will search the log files
for any messages that indicate that this might have happened.

The compile and run steps may be omitted through settings supplied in
the assignment data or if the language build sequence isn't
supported by Pass or if no main file is identified. You can provide
your own build script for a specific assignment, which Pass will use
instead of the compile and run steps.

### Encrypted Meta Data

Pass writes some encrypted information in the LaTeX document's PDF
metadata. The information should match unencrypted information
that's also in the document. This isn't an ultra-secure encryption
but a casual way of detecting if the PDF may have been tampered with
after Pass created it. The `pass-checker` tool can be used to
detect discrepancies between the encrypted and unencrypted
information.

The most likely scenario is where a student is unable to complete
the assignment and tries to alter the PDF to remove error messages
or to change the timestamp in order to claim that a late submission
was due to a connection failure.

Pass creates the temporary directory just before it processes the
supplied files. It fetches any files identified with the
`resourcefile` element at the compile step, for compiled languages,
or at the run step, for scripting languages. Pass doesn't allow a
retry in the same instance. If something goes wrong, the student
will have to exit Pass, use their IDE to make the corrections, and
then restart Pass.  The temporary directory is deleted on exit.

This means that it's unlikely the student will be fast enough to
locate the temporary directory and alter any files before Pass
compiles and runs the application. The simplest way of altering the
PDF is to locate the LaTeX source file and edit it while Pass is
still running after it has completed the build process, and then
rerun LaTeX on it, which will alter the file's timestamp and the PDF
modification date.

Some variation between the file's modification date and the PDF
creation date is expected, since the time to compile and run the
application will cause these timestamps to be different. If the
difference between the timestamps is outside of the expected
variation then that's an indication that this may have occurred.
If the creation date is before the encrypted timestamp
(allowing for rounding errors), then the file has been tampered with.

If a student is clever enough to break the encryption then they're
clever enough to do the assignment. If they're prepared to enlist
someone else's help to cheat, then it's more likely that they'll
get the other person to help with writing the assignment code rather
than remove error messages from the PDF file or altering the
timestamp. An experienced marker should be able to tell from the
source code if there are obvious errors, and they can extract the
attached zip file containing the source code to double-check. (The
`pass-checker` application can also verify the zip file's checksum.)

The encrypted information simply provides a guide as to the likelihood
of whether the lack of expected error messages was due to a
bug/incorrect setting in Pass or due to someone altering the PDF and
is also a guide as to the likelihood that the student had completed
the assignment before the deadline. You can process a batch of files
in one go. For example:
```bash
pass-checker *.pdf
```
This can provide a quick way of highlighting PDF files that require
further examination.

**It's important to inform students that if they experience a
connection issue which prevents them from submitting their work in
time, they should not attempt to get Pass to reprocess their files
after the deadline if they managed to successfully create a PDF
before that time.**

With Server Pass, the logging system can be used to determine when
the student's code was queued for processing, and the PDF files are
all stored read-only on the server.

See the [documentation](docs/README.md) for further details.

## BUILD AND INSTALLATION

To run the Pass applications, you will need to have the relevant
software development tools (such as a C or C++ compiler) or
interpreters (such as Perl) installed, and a TeX distribution. The
Pass GUI and Pass Editor applications come with an installer. The
other tools are all command line applications and just need to have
their `bin` directory added to the operating system's path.

If you want to build the Pass applications from the source code
provided here, then files that are named *name*.*ext*`-template` should be
copied to *name*.*ext* (that is, the original name without the
`-template` suffix). The contents of the copied files should be
modified as appropriate. **Don't add the copied files to the
repository.** They contain encryption keys and passwords or other
sensitive information, which should be kept private.

The Pass source code is written in Java. Server Pass has
accompanying PHP files and a Docker file. Server Pass will require
an IT expert to setup and protect the server.

See the [documentation](docs/README.md) for further details.

### Pass Lib

The Pass backend library jar file is called `passlib.jar`. The source code is in
the `pass-lib` subdirectory. This uses the `passlib-`*lang*`.xml`
dictionary file for messages.

### Pass GUI Lib

A library used by the GUI applications Pass GUI and Pass Editor.
The source code is in the `pass-gui-lib` subdirectory. This uses the
`passguilib-`*lang*`.xml` dictionary file for messages. The
library jar file is called `passguilib.jar`.

### Pass GUI

A desktop Pass application with a graphical frontend for use on lab
computers. This was the original Pass application and the jar file
is called `progassignsys.jar`. The source code is in the `pass-gui`
subdirectory. This uses the `progassignsys-`*lang*`.xml` dictionary
file for messages.

See the [documentation](docs/passgui.md) for further details.

### Pass CLI

A command line Pass application intended for processing assignments
for devices that don't have a graphical environment. This has a
library `pass-cli-lib.jar`, which is shared with Server Pass. The
application jar file is ` pass-cli.jar`. This uses the
`passcli-`*lang*`.xml` dictionary file for messages. The source code
is in the `pass-cli` subdirectory.

See the [documentation](docs/passcli.md) for further details.

### Server Pass

This consists of a command line Pass application based on Pass CLI
that is intended for use in a Docker image. It has accompanying PHP
files for the web interface. The server will need to have Docker,
MySql, and RabbitMQ installed.

The source code is in the `pass-cli-server` subdirectory and the
Docker files are in the `docker` subdirectory.

The student has to create an account for the website. (This should
not use Single Sign On as the server should be isolated from the
rest of the University's systems.) The student uploads their source
files and a job is queued. A backend process picks up the job
information and runs Server Pass in a Docker container. The
resulting PDF and transcript are copied out of the container on
completion and an email is sent to the student to notify them that
the process has finished. They can then log into the site and
download the PDF.

See the [Server Pass documentation](docs/server-pass/README.md) for further details.

### Pass Editor

A GUI application with a simple text editor. This is designed for
course tests conducted in the lab. IDEs typically provide
autocomplete and other helpful shortcuts. Unfortunately, this makes
it hard to test the student's knowledge for the common code blocks
that the IDE will autofill.

Pass Editor is like providing a language student a Word processor
that has the spell checker and grammar checker disabled. It also
uses the Pass Lib backend and will create the required files, either
as empty files or will fetch a template, if one is provided.

The backend library can be used to build the student's application
and prepare a PDF for submission.

See the [documentation](docs/passeditor.md) for further details.

### Pass Checker

A command line application that decrypts the custom metadata and
compares it with other information in the PDF file to determine the
likelihood that the PDF file has been altered after Pass created it.
It will flag any late submissions or possible discrepancies.

See the [documentation](docs/compile.md) for further details.

## DISCLAIMER

I wrote this system as a favour for my husband, a computer science
lecturer, in my spare time. I don't have the time to support it.
It's your responsibility to ensure that an IT professional ensures
that appropriate safeguards are put in place on any devices this
software is installed on. The Pass applications all use the software
development tools that are already installed on the device (lab
computer etc). Any harmful code created by a student, either by
accident or malicious intent, can just as easily be compiled and run outside
of Pass in an IDE or with a simple text editor and command prompt.

## CREDITS

The idea for this system was devised by Dr Gavin Cawley.
Russell Smith installed the software, setup a server for Server
Pass, and provided technical advice.
Norbert Preining and the [Island of TeX](https://gitlab.com/islandoftex) community
provided advice on using TeX Live with Docker. Thank you to
Dr Gavin Cawley, Dr Jason Lines, Prof Tony Bagnall, and the students in CMP who
tested the system and provided feedback.

## LICENSE
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

