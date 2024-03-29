# Pass Checker User Guide

The Pass Checker command line application can be used to check the
PDF files submitted by students to determine if they were created by
PASS and if there are signs that they may have been modified after
PASS created them. Usage:
```
pass-checker [options] <PDF-file>+
```

The PASS assignment processor writes custom fields in the PDF
metadata. Pass Checker decrypts this information and compares it to
the corresponding unencrypted information that's also available in
the PDF file. The output is written in tab-separated form (to
STDOUT, by default).

There's no support for encrypting the entire PDF file. Students need
to be able to read the file in order to check that it correctly
represents their work before they submit it. There's no support for
making the file read-only as markers may need to annotate their
copy.

The standard PDF metadata fields can be displayed using the
Properties setting in your PDF viewer or with a command
line application such as `pdfinfo`. Example output from `pdfinfo`:
```
Title:           Hello World C
Subject:         CMP-123XY Assignment Submission
Keywords:
Author:          ans
Creator:         LaTeX with hyperref
Producer:        LuaTeX-1.15.0
CreationDate:    Fri Oct  2 15:00:55 2020 BST
ModDate:         Wed Nov 16 17:14:15 2022 GMT
Custom Metadata: yes
Metadata Stream: no
Tagged:          no
UserProperties:  no
Suspects:        no
Form:            none
JavaScript:      no
Pages:           3
Encrypted:       no
Encrypted:       no
Page size:       595.276 x 841.89 pts (A4)
Page rot:        0
File size:       44552 bytes
Optimized:       no
PDF version:     1.5
```

PASS sets the title, subject, author, and creation date. The
modification date, creator and producer information is set by
the `hyperref` package or the LaTeX kernel.

The custom metadata set by PASS consists of:

 - the checksum of the attached zip file (which should match the
   actual checksum of the attached zip file);
 - the creation date (which should approximately match the PDF
   CreationDate field);
 - the PASS application that created the PDF (name and version);
 - the assignment due date;
 - the author name (which should match the PDF Author field);
 - (Server Pass only) the timestamp that the source code files were
   uploaded.

With Server Pass, you can also obtain the submission date from the
staff tools on the frontend as well as a checksum of the original
PDF, which can be compared with the checksum of the submitted PDF.
You can use Pass Checker with `--job` to match the submitted PDF
files with their corresponding job ID.

## Validation

For each PDF file, Pass Checker parses the PDF and performs the
following.

 - Reads the non-encrypted creation date, modification date and
   author from the document information.

 - Reads and decrypts the custom metadata. If any encrypted fields
   are missing or have an invalid format, a note is added.

 - Reads the PDF annotations and checks if there is an attachment
   on the first page with the MIME type "application/zip". If there is:
    - the checksum for the attached file is calculated and, if it
      doesn't match the encrypted checksum, a note is added and an
      alert is issued;
    - the actual file size is compared against the declared file size
      and, if they don't match, an alert is issued.
   If the attachment is missing or has the wrong MIME type or if there
   are multiple attachments on the first page, an alert is issued.

 - If the encrypted author information doesn't match the
   corresponding non-encrypted author, a note is added.

 - If the non-encrypted creation date or modification date is
   missing from the standard PDF information, a note is added.

 - If the decrypted date doesn't match the non-encrypted
   creation date, a note is added.

 - If the modification date is less than or equal to the creation
   date, a note is added. (The modification date will always be
   after the creation date, as that's the timestamp when LaTeX
   creates the document, which is sometime after PASS starts to
   write the LaTeX source code.)

 - If the modification date is more than the specified time
   difference tolerance after the creation date, a note is added.

 - If the creation date (or Server Pass submission date, if set) is
   later than the due date, a note is added.

Finally, if the `--flag-identical-checksums` setting is on and
multiple PDF files are processed at the same time, then there's a
check for any that have identical checksums for the attached zip
files. This is unlikely to actually happen, even if the archives have
identical content as the timestamps within the archive are likely to
be different. It is also possible to have a false positive as two
files can coincidentally have identical checksums.

Pass Checker doesn't detect near identical solutions or any other
form of plagiarism detection. Some learning management systems
provide this.

## Output

The output from Pass Checker is written to STDOUT by default but an
output file can be specified with the `--out` (or `-o`) option. The
output format is TAB separated with the following fields:

 - PDF Filename
 - Author (the PDF author)
 - Author Check (this should be identical to the Author field)
 - Date Check (the date PASS created the PDF)
 - Creation Date (the PDF creation date, which should be identical
   to the date check)
 - Mod Date (the PDF modification date, which should be slightly
   later than the date check)
 - PASS Version (the version of PASS that created the PDF)
 - Application (the PASS application that created the PDF)
 - Submission Date (Server PASS Only)
 - Job ID (only present if `--job` switch used)
 - Notes

Any missing or empty data is denoted with a dash (`―`), except for
the Notes column, which will simply be empty if no issues are raised.
Alerts (warning and error messages) are written to STDERR.

## Command Line Options

The following command line options are available:

  - `--version` or `-V` : print version information and exit.
  - `--help` or `-h` : print help and exit.
  - `--debug` : write debugging information.
  - `--out` _filename_ (or `-o`) : write output to _filename_.
  - `--job` _filename_ (or `-j`) : (cumulative action) read Server Pass submission data from  _filename_ (which can be exported via the [Uploads](server-pass/list-uploads.md) page).
  - `--max-time-diff` _seconds_ : maximum difference for
    modification timestamps.
  - `--flag-identical-checksums` (or `-c`) : flag coincident zip
    checksums.
  - `--noflag-identical-checksums` (or `-k`) : don't flag coincident zip
    checksums (default).

Note that you need to take into account how long it's likely to take
PASS to write the LaTeX source code, fetch any resource files, and
compile and run the application (where applicable) when determining
the appropriate timestamp tolerance.

The `--flag-identical-checksums` setting will cause a longer run
time for Pass Checker, and it's not guaranteed to find identical
archives (as demonstrated in the example below). It's therefore not
on by default.

## Examples

The [`tests/pass-checker`](../tests/pass-checker/README.md) directory contains PDF files simulating
student submissions for the dummy course and a simulated Server Pass export file [`uploads.tsv`](../tests/pass-checker/uploads.tsv). After you have
[compiled](compile.md) Pass Checker with `make`, you can then run
`make test`, which will run Pass Checker on the test files and save
the results to a file called `results.tsv` in the same test
directory.

The file `subdirs-abc01xyz.pdf` represents a late submission for
student `abc01xyz`. The Notes column contains "Late Submission".

The file `helloworldgui-jwh22ird.pdf` was created by Pass GUI with
the [Bob example](README.md) username `vqs23ygl` and registration number 327509401.
While Pass GUI was still running, I edited the LaTeX source and
performed a global search and replace to switch Bob's details with
Carol's (username `jwh22ird` and registration number 423901355). I
changed the date from 5th Dec 2022 to 1st Dec 2022 for both the PDF
creation date and the date on the title page. I also changed the
name of the zip file from `helloworldgui-vqs23ygl` to
`helloworldgui-jwh22ird.zip`. I then reran LuaLaTeX twice and saved
the PDF file as `helloworldgui-jwh22ird.pdf`. The Notes column for
this file contains:

> Mismatched author.  
> Mismatched creation date.  
> Modification date \> creation date + 10 seconds.  
> Late submission.

Note that the zip attachment for `helloworldgui-jwh22ird.pdf` has a
different checksum to the zip attachment for
`helloworldgui-vqs23ygl.pdf`. Although both archives have identical
files, the files have different timestamps and the PNG files are listed
in a different order. This means that even with the
`--flag-identical-checksums` setting, they won't be flagged as
identical. For this example, I simply renamed the zip file that PASS
created, so the encrypted checksum still matches.

The file `helloworldbash-vqs23ygl.pdf` was created by Pass GUI but I
then modified the zip file in the temporary directory while Pass GUI
was still running and reran LaTeX to create the modified PDF. The
original zip file contains `helloworldbash/helloworld`. I edited the
echo line in the `helloworld` script to `echo "Hello World (Edited)!"`
and updated the zip file. This means that the zip file is now
240 bytes, which is larger than the original zip file (which was 197 bytes).
This modification has also changed the zip file's checksum.
The time taken to make the modification exceeds the allowed time
difference of 10 seconds between the creation date and the modification date.

This results in one or two warnings written to STDERR, depending on
whether or not `--flag-identical-checksums` is used. With this
option on, there are two warnings:

> Warning: helloworldbash-vqs23ygl.pdf: Embedded attachment 'helloworldbash-vqs23ygl.zip' claims to be size 197 but 240 bytes found    
> Warning: helloworldbash-vqs23ygl.pdf: Mismatched zip checksum.

With the default `--noflag-identical-checksums`, only one warning
occurs:

> Warning: helloworldbash-vqs23ygl.pdf: Embedded attachment 'helloworldbash-vqs23ygl.zip' claims to be size 197 but more than 197 bytes found

In this case, the checksum isn't calculated if the size doesn't
match (any remaining bytes aren't read, which is why the actual size
isn't shown in the warning). The size mismatch is a sufficient alert
that something is wrong regardless of the checksum.

The size is written in plain text in the LaTeX file, so I could have
also altered this to 240, in which case there wouldn't be a warning
about the size mismatch, but the checksum will be calculated and
won't match the encrypted checksum.

The Notes column for this file contains (`--flag-identical-checksums`):

> Mismatched zip checksum.  
> Modification date \> creation date + 10 seconds.

or (`--noflag-identical-checksums`)

> zip checksum not calculated.  
> Modification date \> creation date + 10 seconds.

This is the only example file that writes an alert to STDERR,
because it's a serious concern when the zip file doesn't match the
size or checksum calculated by PASS after it created the file.

---

 - &#x23EE; User Guides ⏵ [Pass CLI](passcli.md)
 - &#x23ED; [Server PASS](server-pass/README.md)
 - &#x23F6; [Documentation Summary](README.md)
