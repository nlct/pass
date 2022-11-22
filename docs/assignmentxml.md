# Assignment XML Specifications

Each course has its own XML file identified in the resource file
with the [`resource` element](resourcexml.md).

Note that any URLs must be the _exact_ address. Redirects aren’t permitted
(including any redirects from `http:` to `https:`). SSL/TCP requires a
valid certificate in Java’s cacerts file.
   
The XML file must contain exactly one `assignments` element. All other
elements must be inside the body of that element:
```xml
<?xml version="1.0"?>
<assignments>

<!-- all other allowed elements in here -->

</assignments>
```

Boolean attributes may have the values `true` or `on` (for TRUE) and
`false` or `off` (for FALSE). If an end tag is required and the body
of the element is textual, then leading and trailing white space
will be trimmed, unless indicated otherwise.

## Assignment Data (`assignment`)

Each assignment is identified with the `assignment` element. The end
tag is required. The body of the element should contain all the
assignment-specific elements.

| Attribute | Description | Default |
| --- | --- | --- |
| `name` | The assignment label. | _none_ (required) |
| `language` | The language label. | deduced from the extension of the main file, if provided |
| `variant` | The language variant (according to `listings.sty`). | _none_ |
| `run` | Boolean that indicates whether or not PASS should try running the student's application, if applicable. | `true` (unless `build` is set) |
| `compile` | Boolean that indicates whether or not PASS should try compiling the student's code, if applicable. | `true` (unless `build` is set) |
| `build` | The URL of a script to use to compile and test the project instead of PASS's default algorithm. | _none_ |
| `nopdfbuild` | (Pass Editor only) The URL of the build script for the quick "no PDF" build. | value of `build` |
| `nopdfrun` | (Pass Editor only) Boolean that indicates whether or not PASS should try running the student's application for the quick "no PDF" build. | value of `run` |
| `relpath` | Boolean that indicates the student needs to identify a base path. | `false` |

Each file supplied by the student needs to have its listing language
identified.  There are four special labels for non-code files:
`PDF` (`.pdf` files), `Word` (`.doc` or `.docx` files),
`Plain Text` (any plain text files without a supported language
label), and `BINARY` (an allowed binary file). These shouldn't be
used in the `language` attribute, but the students can use them to identify files
in their project (via a dropdown box where a graphical interface is
available). The other labels
correspond to labels recognised by `listings.sty`. See the
[`AssignmentData.LISTING_LANGUAGES`](../pass-lib/java/AssignmentData.java)
static variable for the full list.

Listings:

 1. PDF files will be attached with `\attachfile` and included with `\includepdf`
    (provided there are no spaces in the filename).
 2. Word files will be attached with `\attachfile` and not displayed.
 3. Binary files will be included in the project zip file, if they are an
    allowed binary. If the associated `allowedbinary` element
    has a MIME type starting with `image/` and the `showlisting`
    attribute set to true, then the image file will also be included
    with `\includegraphics`. Binary files that are identified as a
    result file will be attached with `\attachfile` and similarly
    included in the document with `\includegraphics`, if applicable.
 4. Files with a language label that corresponds to a known
    `listings.sty` language, will be included with
    `\lstinputlisting`. If the `variant` attribute is used, it
    will be included in the language identifier. (See the
    `listings` element for settings.)
 5. Plain text files will be included with `\verbatiminput`
    (see the `verbatim` element for settings).

When PASS tries to add the LaTeX code to display the file contents
in the PDF document, it uses the language label associated with the file
to determine the appropriate LaTeX command. Each PASS application
has a different method of supplying the language label for each
project file.

GUI applications (and the Server PASS web interface) have a
drop-down menu next to the file name that the student can change as
appropriate. The default label will be determined by the file
extension, if available, otherwise the value of the `language`
attribute will be used.

Pass CLI can have the language label specified along with the
filename. If omitted, the label will be determined by the file
extension, if available, otherwise the value of the `language`
attribute will be used.

### Assignment Label (`name`)

The assignment label (provided by the `name` attribute) may only
contain the characters: `a`–`z`, `A`–`Z`, `0`–`9`, `.`, `+` and `-`.
It's used to form the basename for certain files.

### Assignment Language Label (`language`)

The assignment language label identifies the programming language for source
code files. This can be deduced from the file extension of the
`mainfile` element, but if there are no required files or if the 
main file doesn't have an extension, then you will need to set
the `language` attribute to the language label that will be supplied
to `listings.sty` otherwise the files without extensions will be
shown using `\verbatiminput`.

For example:
```xml
 <assignment name="helloworldbash" language="bash">
   <title>Hello World (Bash)</title>
   <due>2023-04-10 15:00</due>
   <mainfile>helloworld</mainfile>
 </assignment>
```

### Compiling and Running the Application (`compile`, `run` and `build`)

If a main file is identified, that file's associated language label
determines how PASS should test the student's application if
the `compile` and `run` attributes are true and the `build`
attribute hasn't been set, where supported.

It's best not to run GUI applications (but they can still be
compiled) as it won't be possible to finish writing the LaTeX code
until the user exits the sub-process, which may not occur to them.
(Pass Checker will also flag the PDF as suspicious as it will likely
cause too long a time difference between the creation date and
modification date.)

PASS can only capture content written to STDERR and STDOUT. It
doesn't take screenshots. However, it's useful for a student to be
able to run their application in the quick build (no PDF) setting of
Pass Editor, in which case use `run="false" nopdfrun="true"`
or provide different `build` and `nopdfbuild` scripts.

Supported languages (if no build script provided):

 - Java: if `compile` is true, the source code will be compiled
   using the application associated with the `javac` label.
   If `run` is also true and the compile step was successful, the application
   will then be run using the invoker application associated with
   the `java` label.

 - C: if `compile` is true, the source code will be compiled
   using the application associated with the `gcc` label.
   If a Makefile has been included, the application associated
   with the `make` label will be run instead. With `make`, the
   executable `a.out` is expected to be made. Otherwise, the
   executable file will have the filename given by the assignment
   label (with `.exe` appended on Windows). If `run` is also
   true and the compile step was successful, the executable file
   will be run.

 - C++: as for C but the compiler is the application associated
   with the label `g++`.

 - Perl: the `compile` attribute is ignored. If `run` is true,
   the Perl script will be run using the invoker application
   associated with the `perl` label.

 - Lua: the `compile` attribute is ignored. If `run` is true,
   the Lua script will be run using the invoker application
   associated with the `lua` label.

 - Bash: the `compile` attribute is ignored. If `run` is true,
   the Bash script will be run using the invoker application
   associated with the `bash` label.

Other languages will need a build script.

If a build script is provided then the `compile` and `run` attributes
should be omitted. The build script will be invoked as follows:

 - If the build script extension is `bat` or `com` then the script
   will be run by invoking its filename.

 - If the build script has the extension `lua`, it will be run
   using the invoker identified by the `lua` label.

 - If the build script has the extension `pl`, it will be run
   using the invoker identified by the `perl` label.

 - If the build script has the extension `sh`, it will be run
   using the invoker identified by the `sh` label.

 - If the build script has the extension `php`, it will be run
   using the invoker identified by the `php` label.

 - If the build script has the extension `py`, it will be run
   using the invoker identified by the `python` label.

 - If the build script has the extension `mk` or `make` or if the
   filename is `Makefile`, it will be run
   using the invoker identified by the `make` label.

 - If the build script starts with `#!` then it will be run using
   the information supplied in that line.

 - Otherwise, PASS will attempt to set the build script's executable
   bit and, if allowed, will invoke the build script by its filename.

If no build script is provided and `compile=false` and `run=false`
then the PDF produced by PASS will simply provide code listings
with an attached zip file containing the source code and (if
applicable) an attached report.

## Assignment Settings

These settings apply to a specific assignment. The elements must be
inside the applicable `assignment` element. For example:
```xml
 <assignment name="helloworld">
  <title>Hello World C</title>
  <due>2016-10-02 16:30</due>
  <compiler-arg>-ansi</compiler-arg>
  <file>other.c</file>
  <file>other.h</file>
  <mainfile>helloworld.c</mainfile>
 </assignment>
```

### Assignment Title (`title`)

The assignment title must be specified with the `title` element.
The end tag is required. The content is the assignment title.
For example:
```xml
<title>Hello World</title>
```

### Due Date (`due`)

The due date must be specified with the `due` element.
The end tag is required. The content is the due by date (the date
the student has to submit the assignment) in the format
_YYYY_`-`_MM_`-`_DD_` `_hh_`:`_mm_ where _YYYY_ is the four digit
year, _MM_ is the two digit month, _DD_ is the two digit day,
_hh_ is the two digit (24hr) hour and _mm_ is the two digit minute.
For example:
```xml
<due>2021-03-30 16:30</due>
```

The due date is used by GUI applications to select the assignment
closest to its due date as the default. The due date is also added
to the encrypted metadata for Pass Checker so that it doesn't have
to fetch the information from the XML file, but also so that there
is a record of the due date presented to the student when they used
PASS, in the event there may be some possibility that that XML file
may have changed since the student prepared their PDF.

### Required Files (`file` and `mainfile`)

A required file is a file with a specific filename that the student
must provided. The main file is either the file with the `main`
method/function (Java, C and C++) or the script that needs to be invoked in
order to test the application. In the case of scripts, if it's not
possible to determine the language from the script filename then the
`language` attribute will need to be set for the parent `assignment`
element.

Required files are identified with the `file` element, except for
the main file, which is identified with the `mainfile` element.
Only one `mainfile` element is allowed. An assignment may have no
required files, in which case `mainfile` and `file` should be omitted.

If no main file is provided, either set `run="false"` or supply your
own build script.

The required files should be listed in the order they should be
listed in the PDF. For example:
```xml
 <assignment name="shop">
  <title>Shop</title>
  <due>2023-03-30 16:30</due> 
  <file>Product.java</file>
  <file>Shop.java</file> 
  <mainfile>Main.java</mainfile>
  <file>UnknownProductException.java</file>
 </assignment>
```
This will list the files in the PDF in the order: `Product.java`,
`Shop.java`, `Main.java` and `UnknownProductException.java`.
If the default `compile="true"` and `run="true"` settings are on,
PASS will compile these files with the Java compiler and then run
the application in the Java runtime environment with the designated `Main` class.

The `file` and `main` file elements have the same syntax. The end
tag is required and the content is the filename. This should not be
an absolute path, but may be a relative path if a directory
structure is required (in which case, set the `relpath` attribute in
the parent `assignment` element). For example:
```xml
 <assignment name="shop" relpath="true">
  <title>Shop</title>
  <due>2023-03-30 16:30</due> 
  <file>Product.java</file>
  <file>Shop.java</file> 
  <mainfile>Main.java</mainfile>
  <file>exceptions/UnknownProductException.java</file>
  <file>exceptions/InvalidQuantityException.java</file>
 </assignment>
```

| Attribute | Description | Default |
| --- | --- | --- |
| `template` | (Pass Editor only) the URL of a template file. | _none_ |

A template file may be supplied for use with Pass Editor. It's
ignored by the other PASS applications. If set, Pass Editor will
fetch the template file, which provides the student with a starting
point. For example, the template may have syntax errors or bugs that the student
needs to identify and fix.

### Compiler Arguments (`compiler-arg`)

If the `compile` attribute is true (and the language is Java, C or C++)
then you can supply arguments for the compiler with the
`compiler-arg` element. The end tag is required. The content is the
argument to add. Any leading or trailing space will be trimmed, but interior space
will be considered part of the argument. All characters will be
treated literally (so, for example, you can't reference environment variables) but
use XML entities for awkward characters.

For example, for a C project that must conform to ANSI standards:
```xml
<compiler-arg>-ansi</compiler-arg>
```

Use one `compiler-arg` element per argument. For example, for a Java
project that needs to be compatible with Java 8:
```xml
<compiler-arg>-release</compiler-arg>
<compiler-arg>8</compiler-arg>
```

The following arguments will always be applied before the `compiler-arg` 
arguments:

 - Java Compiler: `-Xlint:unchecked -Xlint:deprecation -encoding `_encoding_
   (where _encoding_ is the file encoding of the source files identified when running
   the PASS application).

 - C and C++ Compilers: `-Wall -o `_outputfile_ (where _outputfile_ is the
   name of the executable file).

The following arguments will always be applied after the
`compiler-arg` arguments:

 - Java Compiler: `-d `_classdir_ _javafiles_ (where _classdir_ is the temporary
    directory created by PASS to store the class files and
    _javafiles_ is the list of Java files supplied by the student).

 - C and C++ Compilers: the list of C/C++ files.

This element is ignored for scripts or if an accompanying Makefile
(C/C++ only) is supplied or if the `build` attribute is set.

### Invoker Arguments (`invoker-arg`)

If the `run` attribute is true for Java or scripts then you can
supply arguments for the invoker with the `invoker-arg` element. The
end tag is required. The content is the argument to add.

**No spaces are trimmed**. All space will be considered part of the
argument. All characters will be treated literally (so, for example, you can't
reference environment variables) but use XML entities for awkward
characters.

For example, to set the newline separator to CR+LF for a Java
project (regardless of the platform PASS is running on):
```xml
<invoker-arg>-Dline.separator=&#x0D;&#x0A;</invoker-arg>
```

Note that arguments that need to be passed to the student's
application should be identified with the `arg` element instead.

 - Java applications are invoked with the application identified
   by the label `java` followed by any supplied `invoker-arg`
   and then by the main class name and then any arguments identified
   with any supplied `arg`.

 - Perl scripts are invoked with the application identified by
   the label `perl` followed by `-Wall` and then any supplied
   `invoker-arg` and then by the main file and then any arguments
   identified with any supplied `arg`.

 - Lua scripts are invoked with the application identified by
   the label `lua` followed by any supplied
   `invoker-arg` and then by the main file and then any arguments
   identified with any supplied `arg`.

 - Bash scripts are invoked with the application identified by
   the label `bash` followed by any supplied
   `invoker-arg` and then by the main file and then any arguments
   identified with any supplied `arg`.

All other languages ignore `invoker-arg`. This element is also
ignored if the `build` attribute is set.

### Project Application Arguments (`arg`)

If the `run` attribute is true, any arguments that must be supplied
to the student's application should be specified with the `arg`
element. The end tag is required. The content is the argument to add.

**No space is trimmed**. All space will be considered part of the
argument. All characters will be treated literally (so, for example, you can't
reference environment variables) but use XML entities for awkward
characters.

For example, the following requires a Lua script called `hello.lua`
that will be passed one argument:
```xml
<assignment name="helloarg">
  <title>Hello Arg Lua</title>
  <due>2017-04-12 15:00</due>
  <mainfile>hello.lua</mainfile>
  <arg>Sample Name</arg>
</assignment>
```
This will essentially be invoked as:
```bash
lua hello.lua "Sample Name"
```

This element is ignored if the `build` attribute is set.

### Project Application STDIN (`input`)

If the `run` attribute is true, the content of any `input` elements
will be supplied as lines of STDIN. The end tag is required.
The content is a line of input to add to STDIN.
**No space is trimmed.** (It was trimmed previously but isn't as from v1.3.1.)

The line separator will be PASS's line separator which is the
setting in the Java runtime environment that the PASS application
was invoked in (not any line separator specified in `invoker-arg`).

This element is also used with build scripts.

For example:
```xml
 <assignment name="helloinput">
   <title>Hello Input Perl</title>
   <due>2017-05-10 15:00</due>
   <mainfile>hello.pl</mainfile>
   <input>Sample Name</input>
   <input>Sample Colour</input>
 </assignment>
```
This essentially simulates:
```bash
perl -Wall hello.pl
Sample Name
Sample Colour
```

### Reports (`report`)

If the student must submit a PDF or Word report along with their project code,
the base name of the report file can be specified with the `report`
element. The end tag must be supplied. Leading and trailing spaces
will be trimmed. Avoid interior spaces as `\includepdf` doesn't
support filenames with spaces.

For example, the following indicates that students must supply a
file called `projectreport.pdf` or `projectreport.doc` or
`project-report.docx`:
```xml
<report>project-report</report>
```
Note that this stipulates the file is _required_. PASS will still
allow other PDF or Word files to be included, but will complain if
a required file is missing.

The report will be attached to the PDF. Only PDF files can be
included in the document (in addition to being attached) provided
they don't have spaces in the filename.

### Binary Files (`allowedbinary`)

PASS normally doesn't allow students to include binary files, with
the exception of PDF or Word documents. However, if the student is
expected to include binary files that aren't obtained by compiling
the code, then you can stipulate what type of binary files are
permitted.

Allowed binary files are identified with the `allowedbinary`
element. The end tag is optional. If provided, the content should be
a description of the file type. If omitted, the description will be
obtained from the localisation file (`passlib-`_locale_`.xml`) if
the entry with the key `file.`_mimetype_ is defined.

| Attribute | Description | Default |
| --- | --- | --- |
| `ext` | Comma-separated list of allowed extensions without a leading dot. | _none_ (required) |
| `type` | The MIME type. | _none_ (required) |
| `listing` | Show in the PDF, if supported. | `true` |
| `case` | Boolean that indicates the file extension is case-sensitive. | `true` | 

Note that any PDF or Word files will be considered reports.

All binary files selected by the student will be listed with their file size
in the PDF document in their own section after the source code listings.  If
the `listing` attribute is true, PASS will also try to include the
file in the document. Note that PASS can only do this if the MIME
type starts with `image/` (which can be inserted with
`\includegraphics`, if supported by the graphics driver).  If you
want to allow other types of binary files, you will need to set the
`listing` attribute to `false` or `off`. (The binary files will be
included in the zip file attached on page 1 of the document.)

For example, the following assignment is for a GUI application where
the students have to create image files for the icons. They are
allowed to submit either PNG files or JPEG files:
```xml
 <assignment name="helloworldgui" run="false" >
  <title>Hello World GUI</title>
  <due>2022-12-02 16:30</due>
  <mainfile>HelloWorldGUI.java</mainfile>
  <allowedbinary ext="png" type="image/png" />
  <allowedbinary ext="jpeg,jpg" type="image/jpeg" />
 </assignment>
```

### Project Resource Files (`resourcefile`)

If you want to supply the students with a file (or files) that their
assignment application must read, then you can instruct PASS to
fetch the file from a remote location so that it uses a fresh copy
rather than the student's local copy. This is done with the
`resourcefile` element. The end tag may be omitted.

**Note** this relies on the student's application not using any
directory path when referencing the file. That is, the file is expected to be in
the same directory as the invoked application's current working
directory and should not have an absolute path.

If the student uses an absolute path in their source code to
reference this file then the student's application run by PASS will
read their local copy rather than the fresh copy fetched by PASS.
This will fail completely with Server PASS as the absolute path
won't be accessible from within the Docker image.

If the student uses a relative path where the file's parent isn't
the application's directory, then their application will fail to
find the file when PASS tries to run it. If you need the file to be
in a different relative directory, then you will have to supply a
build script that copies the file fetched by PASS into the required
place.

| Attribute | Description | Default |
| --- | --- | --- |
| `src` | The URL of the file's location. | _none_ (required) |
| `type` | The file's MIME type. | `text/plain` |

For example, Alice instructs the students to create a Java
application that reads a file called `films.csv` that contains a
list of film titles, sorts them in alphabetical order, and then
writes the list to STDOUT. She provides them with a `films.csv`
file that they can use to test their application:
```
Title,Release Date
"The Good, the Bad and the Ugly",1966-12-23
A Fistful of Dollars,1964-09-12
```

However, she instructs PASS to fetch a different `films.csv` file
from `http://cmp.example.com/pass/CMP-123XY/films.csv`, which has
different content:
```
Title,Release Date
"The Chronicles of Narnia: Prince Caspian",2008-05-07
"The Chronicles of Narnia: The Lion, the Witch, and the Wardrobe",2005-12-07
```
This provides a way of testing if the student's application has
hard-coded a solution for the test file or if it can work for a more
general case.

This assignment can be specified in the XML file as:
```xml
 <assignment name="filmlist">
  <title>Film List</title>
  <due>2024-02-01 16:30</due>
  <mainfile>FilmList.java</mainfile>
  <file>Film.java</file>
  <resourcefile src="http://cmp.example.com/pass/CMP-123XY/films.csv"/>
 </assignment>
```

PASS won't allow students to include any identified `resourcefile` with their source
code.

Note that the MIME type is optional in this case and is only
applicable with Pass Editor. The file isn't included in the PDF.

### Project Output Files (`resultfile`)

If the assignment application must create one or more specific
files, you can identify them with the `resultfile` element.
The end tag is optional. These files are expected to be written in the same
directory as the application is invoked from.

If found, PASS will include the files in the PDF as attachments.
PASS won't allow students to include these files with their source
code.

| Attribute | Description | Default |
| --- | --- | --- |
| `name` | The filename. | _none_ (required) |
| `type` | The file's MIME type. | _none_ (required) |
| `listing` | Show in the PDF, if supported. | `true` |

The `type` and `listing` attributes are as for `allowedbinary`.

## Course Options

The options below apply to all assignments for the course.
The elements must not be inside any `assignment` elements.

### Maximum Output (`maxoutput`)

Occasionally a student has produced an application that writes so
much content to STDOUT that the LaTeX process times out before it can
complete the huge document. Even if the timeout setting is
increased, such a large PDF is undesirable. This led to the
introduction of a maximum number of characters saved from
STDERR and STDOUT. If the content exceeds this amount, it will be
truncated with an ellipsis in square brackets and a warning message
will be issued.

The default maximum is 10240 characters. This may be changed with
the `maxoutput` element. The end tag must be provided. The content
should be the new maximum value.

For example:
```xml
<maxoutput>5000</maxoutput>
```

### Options for `fontspec.sty` (`fontspec`)

If LuaLaTeX is used to create the PDF (UTF-8 encoding), then the
`fontspec` package will be loaded. Any options or additional
packages can be supplied with the `fontspec` element. This element
will be ignored if PDFLaTeX is used.

| Attribute | Description | Default |
| --- | --- | --- |
| `options` | Package options. | _none_ |

The content of the `fontspec` element should be LaTeX code to insert after
`\usepackage{fontspec}`. Any options that should be passed to the
`fontspec` package should be supplied with the `options` attribute.

If you use the `fontspec` element, you may also want to use
the `fontenc` element to provide alternative settings for PDFLaTeX.

For example:
```xml
 <fontspec>
 \setromanfont{FreeSerif}
 \setsansfont{FreeSans}
 \setmonofont{FreeMono}
 </fontspec>
```

### Options for `fontenc.sty` (`fontenc`)

If PDFLaTeX is used to create the PDF (ASCII or Latin 1 encodings), then the
`fontenc` package will be loaded. Any options or additional
packages can be supplied with the `fontenc` element. This element
will be ignored if LuaLaTeX is used.

| Attribute | Description | Default |
| --- | --- | --- |
| `options` | Package options. | `T1` |

The content of the `fontenc` element should be LaTeX code to insert after
`\usepackage{fontenc}`. Any options that should be passed to the
`fontenc` package should be supplied with the `options` attribute.

If you use the `fontenc` element, you may also want to use
the `fontspec` element to provide alternative settings for LuaLaTeX.

For example:
```xml
 <fontenc options="T1">
 \usepackage{dejavu}
 </fontenc>
```

### Options for `listings.sty` (`listings`)

The `listings` element can be used to add additional options for the
[`listings` package](https://ctan.org/pkg/listings). The end tag is
required. This element must not be inside any `assignment` elements.

The content is _added_ to
the current set of options. This element has a cumulative effect.

For example:
```xml
 <listings>basicstyle=\ttfamily\normalsize</listings>
 <listings>showlines=false</listings>
```
This is equivalent to:
```xml
 <listings>basicstyle=\ttfamily\normalsize,showlines=false</listings>
```

The default options are:
```
basicstyle=\ttfamily,
numbers=left,
numberstyle=\tiny,
stepnumber=2,
showstringspaces=false,
breaklines
```
Any instance of the `listings` element will append options to this list.

See the [`listings` documentation](http://mirrors.ctan.org/macros/latex/contrib/listings/listings.pdf) for available options.

## Options for `geometry.sty` (`geometry`)

The `geometry` element can be used to add additional options for the
[`geometry` package](https://ctan.org/pkg/geometry). The end tag is
required. This element must not be inside any `assignment` elements.

If no `geometry` elements are provided, the page geometry will be
set to the paper size obtained from
`AssignmentProcessConfig.getGeometryPaperSize()` and the option
`margin=1in`. If any `geometry` elements are provided then the paper
size will again be obtained from
`AssignmentProcessConfig.getGeometryPaperSize()` but the `margin`
won't be set. Instead, the options provided with the `geometry`
elements will be used.

For example:
```xml
 <geometry>margin=0.6in</geometry>
```

### Verbatim Setting (`verbatim`)

The verbatim settings are applied to all assignments in the course
file and are identified with the `verbatim` element. This element
must not be inside any `assignment` elements.

| Attribute | Description | Default |
| --- | --- | --- |
| `maxchars` | The maximum number of characters per line. | 80 |
| `tabcount` | The number of spaces a TAB character should cover. | 8 |

For example:
```xml
 <verbatim maxchars="85" tabcount="4" />
```

The verbatim content is pre-processed by PASS to implement line
wrap, TAB substitution, and invalid or control character substitution.

---

 - Prev: XML File Specifications ⏵ [Resource Files (Courses and Settings)](resourcexml.md)
 - Next: User Guides ⏵ [Pass GUI](passgui.md)
