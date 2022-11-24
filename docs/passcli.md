# Pass CLI User Guide

The Pass CLI application is a command line alternative to Pass GUI.
Instead of using a graphical interface to supply the information to
the PASS backend, the information is supplied via the command line
or in a file that can be read with the `--from-file` switch.

The `pass-cli/bin` directory should be added to the system `PATH` or
use the full pathname, as applicable. The `bin` directory has two
scripts:

  - `pass-cli` : a bash script for Linux or Mac;
  - `pass-cli.bat` : a DOS batch file that can be used on Windows.

Both files simply run the `pass-cli.jar` file. If neither script is
suitable, you can run the JAR file directly:

```bash
java -jar /path/to/pass-cli/lib/pass-cli.jar
```

where `/path/to/pass-cli` is the path to the Pass CLI directory.

Below, `<...>` indicates content you need to supply, `[...]`
indicates optional content, `+` indicates one or more of whatever
precedes it, and <kbd>↹</kbd> indicates a Tab character.

Long switches with values may be specified as _switch_=_value_ or
_switch_ _value_ (for example, `--file=HelloWorld.java` or
`--file HelloWorld.java`).

## General Options

  - `--version` or `-V` : print version information and exit.
  - `--help` or `-h` : print help and exit.
  - `--silent` or `-q` : equivalent to `--message 0` (silent).
  - `--debug` : equivalent to `--message 5` (debug).
  - `--allow-debug-courses` : allows debug courses without changing
    verbosity (default).
  - `--noallow-debug-courses` :  disallows debug courses without changing
    verbosity.
  - `--encoding <name>` : the encoding for the transcript and
    `--from-file` files.
  - `--transcript <file>` or `-l <file>` : write messages to `<file>`.
  - `--directory <dir>` or `-d <dir>` : project files are specified relative
    to `<dir>`. (To maintain the relative path structure, use
    `--base-path`.)
  - `--job-id <n>` : only available with Server Pass, this sets the
    job ID which is used in messages to help identify which job the
    message came from.

`--from-file <file>` or `-F <dir>`

Reads settings from `<file>`. The remaining settings listed below
can all be placed in this file instead of on the command line.
Each setting should be on a separate line in the form:

```
<Setting-name>: <value>
```

where `Setting-name` is like the long switch but without the leading
`--` and with an initial capital. For example, if the file
`example.txt` contains:

<pre>
Student: vqs23ygl<kbd>↹</kbd>327509401
Student: jwh22ird<kbd>↹</kbd>423901355
Course: CMP-101
Assignment: helloworldjava
File: ../tests/HelloWorldJava/HelloWorld.java
Agree: true
Pdf-result: ../tests/results/helloworldjava-vqs23ygl.pdf
</pre>

Then

```
pass-cli --from-file example.txt
```

is equivalent to

```
pass-cli --student vqs23ygl 327509401 --student jwh22ird 423901355 --course CMP-101 --assignment helloworldjava --file ../tests/HelloWorldJava/HelloWorld.java --agree --pdf-result ../tests/results/helloworldjava-vqs23ygl.pdf
```

The file passed to `--from-file` is referred to as the "From File"
below.

## Project Settings

### Project File

Command line: 

`--file <path> [<language>]` or `-f <path> [<language>]`

"From File" syntax:

<pre>
File: &lt;path&gt;[<kbd>↹</kbd>&lt;language&gt;]
</pre>

A project file and (optionally) its associated language label (if
it can't be determined from the file extension). Each file needs to
be specified with a separate `--file` switch or `File:` line.

### Base Path

Command line: 

`--base-path <path>` or `-b <path>`

"From File" syntax:

<pre>
Base-path: &lt;path&gt;
</pre>

Use filenames relative to `<path>` when copying them to the
temporary directory created by the PASS Lib backend.

### Project File Encoding

Command line: 

`--project-encoding <name>` or `-e <name>`

"From File" syntax:

<pre>
Project-encoding: &lt;name&gt;
</pre>

Sets the encoding for the project files (case-insensitive). The
`<name>` may be one of:

  - `ASCII` or `US-ASCII`
  - `Latin-1` or `Latin1` or `Latin 1`
  - `UTF-8` or `UTF8` (default)

### Course

Command line: 

`--course <code>` or `-e <code>`

"From File" syntax:

<pre>
Course: &lt;code&gt;
</pre>

The course code as given in the `resources.xml` file (e.g. CMP-101).

### Assignment

Command line: 

`--assignment <label>` or `-a <label>`

"From File" syntax:

<pre>
Assignment: &lt;label&gt;
</pre>

The assignment label as given in the assignment XML file.

### Student Details

There are two ways of specifying a student. The first way specifies
both the username and registration number together.

Command line: 

`--student <username> <regnum>`

"From File" syntax:

<pre>
Student: &lt;username&gt;<kbd>↹</kbd>&lt;regnum&gt;
</pre>

For group projects, use one `--student` switch or `Student:` line
per student.

The second method is to specify all the usernames in one option and
all the registration numbers in another.

Command line: 

`--user-id <username>[,<username>]+` or `-u <username>[,<username>]+`  
`--student-number <regnum>[,<regnum>]+` or `-n <regnum>[,<regnum>]+`  

"From File" syntax:

<pre>
User-id: &lt;username&gt;[,&lt;username&gt;]+
Student-number: &lt;regnum&gt;[,&lt;regnum&gt;]+
</pre>

Both lists must be the same length and in the same order.

### Agreement

The PDF has a line on the first page below the title that starts
with a checkbox, and the following text ("I" for solo projects and
"we" for group projects):

> I/We agree that by submitting a PDF generated by PASS I am/we are confirming that I/we have checked the PDF and that it correctly represents my/our submission.

The agreement setting indicates whether or not this box should be
ticked.

Command line: 

`--agree` or `-Y` : tick the checkbox  
`--no-agree` or `-N` : don't tick the checkbox (default)

"From File" syntax:

<pre>
Agree: &lt;value&gt;
</pre>

where `<value>` is `true` for `--agree` and `false` for `--no-agree`.

Note that agreement is required by default or PASS will reject the
process.

### PDF File

Command line: 

`--pdf-result <path>` or `-r <path>`

"From File" syntax:

<pre>
Pdf-result: &lt;path&gt;
</pre>

Save the PDF created by PASS to `<path>`.

### Timeout

Command line: 

`--timeout <seconds>`

"From File" syntax:

<pre>
Timeout: &lt;seconds&gt;
</pre>

Timeout for processes run by the PASS backend.

### Messages

Command line: 

`--messages <id>` or `-m <id>`

"From File" syntax:

<pre>
Messages: &lt;id&gt;
</pre>

Sets the verbosity level. The value `<id>` may either be an integer
or a label:

| Numeric | Label |
| --- | --- |
| 0 | `silent` |
| 1 | `errors` |
| 2 | `errors and warnings` |
| 3 | `errors warnings and info` |
| 4 | `verbose` |
| 5 | `debug` |


### Submission Timestamp

This setting is only available with Server Pass. The value is the
time the project files were uploaded on the Server Pass web frontend
upload page.

---

 - Prev: User Guides ⏵ [Pass Editor](passeditor.md)
 - Next: [Server PASS](server-pass/README.md)
