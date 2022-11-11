#Preparing Programming Assignments for Submission System (PASS) Post Installation Instructions

After installing PASS, please edit the `lib/resources.xml` file
to provide the details for each course. These details may either be
provided in the local `lib/resources.xml` file using the `resource`
element or the details for each course may be listed in a remote
`resources.xml`. A remote file makes it easier to share the same
course list across multiple installations of PASS.

The remote XML file should be identified with the
`courses` element, which may contain a body as a fallback. The `href`
attribute identifies the URL of the remote resource file.

The remote XML resources file has the same specifications as the
local one. While it is possible to have a mixture of `courses` and
`resource` within a resources XML file, it’s better to have only
`courses` in the local `lib/resources.xml` and `resource` in the
remote resources XML file.

For example, the local `lib/resources.xml` file may contain:
```xml
<?xml version="1.0"?>
<resources>
 <courses href="http://www.example.com/pass/resources.xml" />
</resources>
```
and the remote `resources.xml` file may contain:
```xml
<?xml version="1.0"?>
<resources>
 <resource name="CMP-1234X" 
   href="http://www.example.com/pass/CMP-1234X.xml">
 Introductory Programming
 </resource>
 <resource name="CMP-1234Y" 
   href="http://www.example.com/pass/CMP-1234Y.xml">
 Advanced Programming
 </resource>
</resources>

```
To allow for a transitional period between switching from local to
remote resources, you can supply fallbacks if the remote resource
identified by `<courses>` returns 404 or 301 HTTP error codes. This
is done by placing `<resource` inside `<course ...>...</course>`.
For example:
```xml
  <courses href="http://www.example.com/pass/resources.xml" >
    <resource name="CMP-1234X" 
      href="http://www.example.com/pass/CMP-1234X.xml">
    Introductory Programming
    </resource>
    <resource name="CMP-1234Y" 
      href="http://www.example.com/pass/CMP-1234Y.xml">
    Advanced Programming
    </resource>
  </courses>

```
In this case the local `<resource>` information will be used if
http://www.example.com/pass/resources.xml returns 404 or 301 but
will be ignored otherwise. Any instance of `<resource>` outside of
`<courses>` will behave as normal. For example:
```xml
<?xml version="1.0"?>
<resources>
 <resource debug="true" name="CMP-123XY" 
  href="http://www.example.com/pass/dummy-assignments.xml">
 Dummy Course for Testing
 </resource>

 <courses href="http://www.example.com/pass/resources.xml" >
<!-- fallbacks -->
    <resource name="CMP-1234X" 
      href="http://www.example.com/pass/CMP-1234X.xml">
    Introductory Programming
    </resource>
    <resource name="CMP-1234Y" 
      href="http://www.example.com/pass/CMP-1234Y.xml">
    Advanced Programming
    </resource>
 </courses>
</resources>
```
This provides the dummy course for testing (in debug mode only), a
reference to a remote resource file and two fallbacks in the event
that the remote resource doesn’t exist. (The 301 HTTP code triggers
the same fallback behaviour as 404 because some sites are set up to
automatically redirect unknown URLs.)

A dummy course may be provided that can be used for testing the application.
For example:
```xml
 <resource name="CMP-123XY" 
  href="http://www.example.com/pass/dummy-assignments.xml">
 Dummy Course for Testing
 </resource>
```
The `name` attribute (CMP-123XY) is the course code.
The `href` attribute is the URL of the XML assignment data file
for that course (see below). Both attributes are required.
The text (Dummy Course for Testing) is the course title.

The `debug="true"` attribute may be set which means that the course is only
available in debug mode (that is, when PASS is invoked with the `--debug`
switch). If omitted, `debug="false"` is assumed.

The URL must be the exact address. Redirects aren’t permitted (including 
any redirects from `http:` to `https:`). SSL/TCP requires a valid
certificate in Java’s cacerts file.

If the compiler isn’t on the OS path, you will need to add an
`application` tag to specify the location. For example:
```xml
 <application
   name="javac"
   uri="file:///usr/java/latest/bin/javac"
 />

 <application
   name="java"
   uri="file:///usr/java/latest/bin/java"
 />

```
Currently supported compilers/interpreters: `javac`, `java`, `g++`, `gcc`,
`perl`, `bash` and `lua`. Note that `pdflatex` or `lualatex` are
also required to build the PDF file. (The `attachfile` package
doesn’t work with `xelatex`.)

You can also specify any environment variables that should be set
when PASS runs any processes (this includes compiling, running the
application, and building the PDF with LaTeX).
```xml
<processes>
 <env name="ENV NAME">ENV VALUE</env>
 ...
 <env name="ENV NAME">ENV VALUE</env>
</processes>
```
The value isn't trimmed so be careful of any leading or trailing
spaces. You can optionally use the `timeout` attribute in the
`processes` tag to set the process timeout (in seconds). For
example:
```xml
<processes timeout="120">
 <env name="ENV NAME">ENV VALUE</env>
 ...
 <env name="ENV NAME">ENV VALUE</env>
</processes>
```
This will override any user-specified timeout. If you only want to
set the timeout but not any environment variables you can use a void
element:
```xml
<processes timeout="120" />
```


The `resources.xml` may also include the `<agree />` element, which
has a boolean attribute `required` where the value may either be `true`/`yes`
or `false`/`no`. This setting is on by default, which requires the
user to explicitly agree to the “I agree that by submitting a PDF
generated by PASS I am confirming that I have checked the PDF and
that it correctly represents my submission” statement. The GUI
version of PASS doesn’t allow it to be switched off. In general,
there shouldn’t be any need to use this element.

##Assignment Data Files

The data file for each course is an XML file with the structure:
```xml
<assignments>
  <assignment name="...">
  ...
  </assignment>
  ...
  <assignment name="...">
  ...
  </assignment>
</assignments>
```
Outside of any `<assignment>` block you may use:
```
 <listings>...</listings>
```
to indicate any listings.sty settings. For example:
```
 <listings>basicstyle=\ttfamily\normalsize</listings>
```
You can also adjust the geometry settings with:
```
 <geometry>...</geometry>
```
For example:
```
 <geometry>margin=0.6in</geometry>
```
Both `<listings>` and `<geometry>` are cumulative. That is, if you
specify multiple instances they will be appended to the earlier
settings.

To guard against students submitting code that generates excessive
messages written to STDOUT, you can set a maximum value (in bytes)
with:
```
  <maxoutput>...</maxoutput>
```
where the content should be an integer. If not provided the default
setting is:
```
  <maxoutput>10240</maxoutput>
```
The messages will be truncated to _approximately_ that value.

Known languages are typeset using `\lstinputlisting` but other
verbatim content (plain text files, messages to STDOUT/STDERR) is 
written to a temporary file (rather than using the `verbatim`
environment) and input with `\verbatiminput`. The content is 
preprocessed by PASS to break up long lines and replace TAB characters 
with spaces (since this is something that's easier to do in Java than in TeX).
The default is to break up lines that exceed 80 characters and
use 8 spaces for TAB characters. You can change these values with
the `verbatim` tag, which has the attributes `maxchars` and
`tabcount` to set the maximum characters per line and TAB character count,
respectively. For example:
```
 <verbatim maxchars="85" tabcount="4" />
```
Note that any control characters will be replaced with `[0x`_hex_`]`
within verbatim content (but not in listings, where control
characters aren't expected). Similarly, if the ASCII encoding
setting was selected, non-ASCII characters will be replaced.

Bear in mind that the `listings` package will break lines (if enabled) 
according to the line width, not the character count, so the
`maxchars` setting may need to be adjusted to best fit the `listings`
and `geometry` settings.

If LuaLaTeX is used (UTF-8) then the `fontspec` package will be
loaded. You can provide commands that should be added to the LaTeX
preamble after this package has been loaded using the `fontspec`
tag. For example:
```
<fontspec>
\setromanfont{FreeSerif}
\setsansfont{FreeSans}
\setmonofont{FreeMono}
</fontspec>
```
This tag will be ignored if LuaLaTeX isn't used. This tag has a
cumulative effect. If omitted, `\usepackage{lmodern}` is assumed.

If PDFLaTeX is used (ASCII or Latin-1) then the `fontenc` package
will be loaded. You can provide commands that should be added to the
LaTeX preamble after this package has been loaded using the
`fontenc` tag. For example:
```
<fontenc>\usepackage{noto}</fontenc>
```
This tag will be ignored if PDFLaTeX isn't used. This tag has a
cumulative effect. If omitted, `\usepackage{lmodern}` is assumed.

Both the `fontspec` and `fontenc` tags have an optional attribute
`options`, which can be used to pass options to the relevant
package. The default for `fontspec` is nothing. The default for
`fontenc` is `T1`.

Each assignment is described with:
```
  <assignment name="...">
  ...
  </assignment>
```

The `name` attribute should be a short label identifying the
assignment. For example:
```xml
<assignment name="helloworld">
```
The only permitted characters within the `name` attribute are:
`a`-`z`, `A`-`Z`, `0`-`9`, `.`, `+` and `-`. (The name may be
used to form filenames.)

PASS will automatically try to test the application (after the code
has been compiled if required). If you want to skip this test, set
the `run` attribute to `false`. For example:
```xml
<assignment name="helloworld" run="false">
```
If you want to additionally skip the compiler step, set the
`compile` attribute to `false`. For example:
```xml
<assignment name="helloworld" compile="false">
```
This automatically implements `run="false"`.

The contents of the `<assignment>` tag must include
```xml
<title>Assignment Title</title>
<due>Due Date</due>
<mainfile>filename</mainfile>
```
The due date must be given in the format `YYYY-MM-DD HH:mm`
As from PASS v1.06, the `mainfile` element is optional. If omitted,
PASS won’t attempt to compile or run the application. If the code
should be compiled, you can add options to pass to the compiler
using the `<compiler-arg>` tag. There should be one tag per command
line argument. For example:
```xml
<compiler-arg>-encoding</compiler-arg>
<compiler-arg>UTF-8</compiler-arg>
```
If the assignment application is run using an invoker (such as `java`), you
can use the `<invoker-arg>` tag, which works in an analogous way to
`<compiler-arg>`.

For example, a minimal assignment where the students are 
only required to submit the file `helloworld.c`:
```xml
 <assignment name="helloworld">
  <title>Hello World</title>
  <due>2016-10-02 16:30</due>
  <mainfile>helloworld.c</mainfile>
 </assignment>
```
Additional required files are marked up with the `<file>` 
tag.  For example, if the students are required to submit
`Foo.java`, `Bar.jar` and `FooMain.java`, where the `main`
method is in `FooMain.java`:
```xml
 <assignment name="foobar">
  <title>Foo Bar</title>
  <due>2016-10-02 16:30</due>
  <mainfile>FooMain.java</mainfile>
  <file>Foo.java</file>
  <file>Bar.java</file>
 </assignment>
```
There are two optional attributes to the `<assignment>` tag related
to the code listings: `language` and `variant`. PASS tries to determine the
appropriate language setting for the listings package when
creating the PDF file by testing for known file extensions.
For example, if the extension is `.c` then “C” is assumed.
If it’s unable to determine the language from the extension,
then the `verbatim` environment is used. The `language` tag provides
a fallback to use as a default if this happens.  The value must be
recognised by `listings.sty`.

The `listings` package also recognises some variants for
various languages. This can be supplied in the `variant`
attribute. The value must be recognised by `listings.sty`.
For example:
```xml
 <assignment name="helloarg" language="Lua" variant="5.0">
```
If the project needs to read any files provided by the assignment,
each file should be identified using the `<resourcefile>` tag with the
location of the original file given in the `src` attribute. For
example, if the project needs to load the files `foo.txt` and
`bar.txt` that the students were able to download from
http://www.example.com/projects/foo.txt and
http://www.example.com/projects/bar.txt:
```xml
   <resourcefile src="http://www.example.com/projects/foo.txt" />
   <resourcefile src="http://www.example.com/projects/bar.txt" />
```
The URL must be the exact address. Redirects are not permitted
(which includes redirecting http: to https:). SSL/TCP requires a
valid certificate registered with Java’s cacerts.

This ensures that the original file is used rather than the
student’s downloaded copy. This safeguards against any (intentional
or unintentional) corruption that may have occurred with the
student’s copy. (It also allows for an alternative file to be
provided to test if the student has hard-coded a solution specific to
the given file.)

If the project must generate a file or files, each file name
should be listed in the `<resultfile>` tag with the 
`name` attribute set to the file name and the `type` attribute
set to the mime type for that file. For example, if the project
must create an image file called `image.png` and a text file
called `output.txt`, then:
```xml
   <resultfile type="image/png" name="image.png" />
   <resultfile type="text/plain" name="output.txt" />
```
If the project application requires command line arguments,
use the `<arg>` tag for each argument. For example:
```xml
   <arg>Sample Name</arg>
```
If the project application requires reading information from 
STDIN, use the `<input>` tag to provide each line of test data. 
For example:
```xml
   <input>Sample Name</input>
   <input>Sample Address</input>
```

##How the PASS Application Works

If there’s more than one `<resource>` listed in the
`lib/resources.xml` file, then the student will be prompted for
the course otherwise it will just select the single `<resource>`.

The XML file identified by the `href` attribute in the
`<resource>` tag will then be loaded and the student will be
presented with a list of assignments. The one closest to its due
date (that isn’t overdue) is set to the default, but may be
changed (for example, if the student is late submitting).

The student then needs to select the project directory. PASS
will search that directory and any sub-directories for the
required files. They are given the opportunity to add any
additional files.

Once all the source code files have been found, PASS will
then create a temporary directory and copy those files to it
and create a zip file (using Java’s `java.util.zip` library).

PASS will then create a LaTeX file that loads the `listings`
and `attachfile` packages to create a PDF that contains the zip
file as an attachment and a listing of each source code file
in the order specified in the `<assignment>` tag. (In the above 
“Foo Bar” example, the order will be `FooMain.java`, `Foo.java`
and `Bar.java`.)

PASS will also attempt to compile the code. This feature is
currently only available for Java, C and C++. Any messages
produced by the compiler will be added to the LaTeX document.
This step is skipped for interpreted languages. (Currently
only Perl, Bash and Lua are recognised.)

If the compilation was successful (or skipped for interpreted
scripts), PASS will then attempt to run the application. If any
resource files were specified with the `<resourcefile>` tags, these
will first be downloaded and copied into the temporary directory
before running the application.

PASS will capture any messages to STDOUT and STDERR and add
them to the LaTeX document.

If any files have been specified with the `<resultfile>` tag, these
will be added as an attachment to the document if they are found.
The mime type determines if the file should be additionally be
included as an image (`image/*`) or verbatim (`text/*`) in the
document after the attachment marker.

Once the LaTeX document code has been completed, PASS will then
compile it using `pdflatex` (ASCII or Latin-1) or `lualatex` (UTF-8)
and the student will be presented with a dialog box to save the
resulting PDF file. The student should then view it to check it has
been created correctly.

On exit, the temporary directory will be removed.

##Testing the Installation

You can use the test resource “CMP-123XY” with the XML
file at http://www.dickimaw-books.com/software/pass/dummy-assignments.xml
Please copy it to your own server if you want to use it.

###“Hello World (C)” Example

The `helloworld` assignment is identified as follows:
```xml
 <assignment name="helloworld">
  <title>Hello World</title>
  <due>2016-10-02 16:30</due>
  <mainfile>helloworld.c</mainfile>
 </assignment>
```
This assignment just requires the creation of the file
`helloworld.c`:
```c
#include <stdio.h>

int main()
{
   printf("Hello World!");

   return 0;
}
```

Suppose this file is saved in the directory `~/Projects/HelloWorldC`
then when PASS starts up, select “Hello World” from the dropdown
menu, add a dummy student name and number and click “Next”
to move to the next panel. Use the “Open” button to select
the directory `~/Projects/` and click “Next” to move to the next
panel.

Hopefully PASS will have found the file `helloworld.c` and
listed the full path.

Click “Next” to generate the PDF. If successful the save dialog
box should appear. Once you’ve saved the PDF file, open it
to check the listing and application test.

**Not all PDF viewers can deal with attachments.**

Adobe Reader and Evince can handle attachments.

###“Hello Input” Example

The `helloinput` test assignment is identified as follows:
```xml
 <assignment name="helloinput">
   <title>Hello Input</title>
   <due>2017-05-10 15:00</due>
   <mainfile>hello.pl</mainfile>
   <input>Sample Name</input>
   <input>Sample Colour</input>
 </assignment>
```
This requires two lines of input which are supplied by the
`<input>` tags. Here’s the test code for `hello.pl`:
```perl
#!/usr/bin/perl

print "What's your name? ";

my $name = <STDIN>;

chomp $name;

print "Hello ", $name, "!\n";

print "What's your favourite colour? ";

my $colour = <STDIN>;

chomp $colour;

print "Your favourite colour is '$colour'.\n";

1;
```

###“Hello Arg“ Example

The `helloarg` test assignment is identified as follows:
```xml
 <assignment name="helloarg" variant="5.0">
   <title>Hello Arg</title>
   <due>2017-04-12 15:00</due>
   <mainfile>hello.lua</mainfile>
   <arg>Sample Name</arg>
 </assignment>
```
This requires one command line argument which is supplied
by the `<arg>` tag. Here’s the code for `hello.lua`:
```lua
#!/usr/bin/lua

if #arg < 1
then
  error("Syntax error: argument missing")
end

print("Hello ".. arg[1] .."!\n")
```

###“Make a PNG Image” Example

The `makeimage` test assignment is identified as follows:
```xml
 <assignment name="makeimage">
   <title>Make a PNG Image</title>
   <due>2017-03-04 17:00</due>
   <mainfile>MakeImage.java</mainfile>
   <resultfile type="image/png" name="image.png" />
   <resultfile type="text/plain" name="output.txt" />
 </assignment>
```
This only requires a single source code file, but the
project must create two files: `image.png` and `output.txt`.

Here’s the example Java code:
```java
package makeimage;

import java.io.*;
import java.awt.image.*;
import java.awt.*;
import javax.imageio.ImageIO;

public class MakeImage
{
   private static void test() throws IOException
   {
      BufferedImage image = new BufferedImage(100, 100,
       BufferedImage.TYPE_INT_ARGB);

      Graphics2D g2 = image.createGraphics();

      if (g2 != null)
      {
         g2.setComposite(AlphaComposite.Src);

         g2.setColor(Color.YELLOW);
         g2.fillRect(0, 0, 100, 100);

         g2.setColor(Color.BLUE);

         g2.drawRect(10, 10, 80, 60);

         g2.dispose();
      }

      ImageIO.write(image, "png", new File("image.png"));

      File file = new File("output.txt");

      PrintWriter writer = new PrintWriter(file);

      writer.println("Test output.");

      writer.close();    
   }


   public static void main(String[] args)
   {
      try
      {
         test();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }
}
```

###“Free-Form” Example

The `freeform` test assignment is identified as follows:
```xml
 <assignment name="freeform">
  <title>Free-Form</title>
  <due>2017-04-30 15:00</due>
  <report>reportdoc1</report>
 </assignment>
```
This only requires that the student includes a report file
called `reportdoc1.pdf` or `reportdoc1.doc` or `reportdoc1.docx`.
They may submit other files as well, but there are no
required source code files.
