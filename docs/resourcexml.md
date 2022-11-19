# Resource File XML Specification (Courses and Settings)

Each PASS application (except Pass Checker) has its own
`lib/resources.xml` file. This is the application's _local_ resource
file that contains the settings for that particular installation.
The local resource file may reference a _remote_ resource file
that can be shared by multiple PASS applications. Typically, courses
are defined in a remote resource file and any required paths are
defined in the local resource files.

The local and remote resource XML files have the same syntax. Note
that any URLs must be the _exact_ address. Redirects aren’t permitted
(including any redirects from `http:` to `https:`). SSL/TCP requires a
valid certificate in Java’s cacerts file.

The XML file must contain exactly one `resources` element. All other
elements must be inside the body of that element:
```xml
<?xml version="1.0"?>
<resources>

<!-- all other allowed elements in here -->

</resources>
```

Boolean attributes may have the values `true` or `on` (for TRUE) and
`false` or `off` (for FALSE). If an end tag is required and the body
of the element is textual, then leading and trailing white space
will be trimmed.

## Identifying the remote resource file (`courses`)

The remote resource file is identified with the `courses` element.
The end tag may be omitted, but if included the body will be used as
a fallback if the remote file can't be found. (This fallback was
added to allow for a transitional phase when courses were being
migrated to the remote file.)

| Attribute | Description | Default |
| --- | --- | --- |
| `href` | The URL of the remote resource XML file. | _none_ (required) |

For example:
```xml
<courses href="http://cmp.example.com/pass/resources.xml" />
```

## Identifying the remote resource file (`resource`)

The `resource` element identifies a course and its associated
assignment file. The end tag is required. The body is the course
title.

| Attribute | Description | Default |
| --- | --- | --- |
| `name` | The course label. | _none_ (required) |
| `href` | The URL of the assignment XML file. | _none_ (required) |
| `debug` | Boolean value that, if true, indicates the course is only available in debug mode. | `false` |

For example:
```xml
 <resource name="CMP-101" 
  href="http://cmp.example.com/pass/CMP-101.xml">
  Introduction to Java
 </resource>

 <resource name="CMP-102" 
  href="http://cmp.example.com/pass/CMP-102.xml">
  Introduction to C and C++
 </resource>

 <resource name="CMP-123XY" debug="true"
  href="http://cmp.example.com/pass/CMP-123XY.xml">
 Dummy Course for Testing
 </resource>
```

## Specifying Application Paths (`application`)

When the PASS backend processes a project, it needs to run external
processes: PDFLaTeX or LuaLaTeX (depending on encoding) and,
optionally, the compiler and invoker.

Each application that PASS needs to use has a label identifying it,
which may also correspond to the application's filename, and a list
of possible alternative names. PASS will search the `PATH`
environment variable for each possibility. If the application can't
be found (for example, the application isn't on the path or it has
a different filename) then you will need to specify the path to the
application using the `application` element. This should typically go in the _local_
resource file as it will likely be specific to the device the PASS
application has been installed on.

| Attribute | Description | Default |
| --- | --- | --- |
| `name` | The application's label. | _none_ (required) |
| `uri` | The URI of the application. | _none_ (required) |


For example:
```xml
 <application
   name="javac"
   uri="file:///usr/java/latest/bin/javac"
 />
```

The application labels are listed below, along with the filenames
that PASS will try searching for (with `.exe` appended on Windows)
in their order of precedence.

| Label | Filenames |
| --- | --- |
| `bash` | `bash` |
| `g++` | `g++`, `c++`, `gcc-c++` |
| `gcc` | `gcc`, `cc` |
| `java` | Windows: `javaw`, `java`. Other: `java` |
| `javac` | `javac` |
| `lua` | `lua`, `texlua` |
| `lualatex` | `lualatex` |
| `pdflatex` | `pdflatex` |
| `perl` | `perl` |
| `python` | `python` |
| `make` | `make` |


The GUI PASS applications (Pass GUI and Pass Editor) may also run
a PDF viewer, text editor or (Pass Editor only) an image viewer.
These can be opened with `Desktop.open(File)`, but this doesn't
always work. Sometimes the desktop isn't supported, but sometimes
the desktop is supported but the file won't open until after PASS
has exited. Unlike the above, these applications don't have a common
filename, so PASS doesn't try searching for them on the PATH.
Instead, it adopts the following algorithm:

 1. If the corresponding label (`pdfviewer`, `editor` or
    `imageviewer`) has been identified in the resource file
    with the `application` element, use that.
 2. If the environment variable `PDFVIEWER` or `EDITOR` has
    been defined use that for PDF or text files, respectively.
 3. Attempt to use `Desktop.open(File)` if the desktop is supported.

For example:
```xml
 <application
   name="pdfviewer"
   uri="file:///usr/bin/okular"
 />
 
 <application
   name="editor"
   uri="file:///usr/bin/gedit"
 />
 
 <application
   name="imageviewer"
   uri="file:///usr/bin/eog"
 />
```

## Processes (`processes`)

The `processes` element may be used to change the default process
timeout setting. The end tag may be omitted. This element should
only go in the _local_ resource file.

| Attribute | Description | Default |
| --- | --- | --- |
| `timeout` | The timeout in seconds. | _varies_ |

 - Pass GUI will allow the user to set the timeout if it hasn't been
   set in the resource file, otherwise the default is 120 seconds.

 - The Pass Editor default is 120 seconds.

 - Pass CLI has a default of 60 seconds which can be overridden by
   the `--timeout` option, but the timeout in the resources file
   will take precedence, if supplied.

 - Server Pass is a variation of Pass CLI but the `--timeout` switch
   is set according to the `timeout` setting in the configuration table
   used by the frontend, so don't include the `processes` element in
   the resource file used by Server Pass otherwise you'll have to
   rebuild the Docker image every time you need to change the
   timeout value.

## Environment Variables (`env`)

The `env` element may be used to set any environment variables that
should be used when PASS runs any processes. The end tag must be
supplied. The body is the value of the environment variable.

| Attribute | Description | Default |
| --- | --- | --- |
| `name` | The environment variable name. | _none_ (required) |

## Agreement (`agree`)

A boolean attribute that indicates if the user is required to
explicitly agree to check the PDF before submitting it.

| Attribute | Description | Default |
| --- | --- | --- |
| `required` | Boolean indicating whether or not agreement is required. | `true` |

This requirement had to be added because some students were
submitting their PDF without checking it and then later complained
that PASS had got it wrong and the PDF didn't actually represent
their work.
