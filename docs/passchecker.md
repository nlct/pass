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
multiple PDF files are processed at the same time, then
there's a check for any that have identical checksums for the
attached zip files. This can happen by coincidence and only
indicates if the attachments are identical. It doesn't pick up near
identical solutions. PASS doesn't provide any plagiarism detection.
Some learning management systems provide this.

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
 - Notes

Any missing or empty data is denoted with a dash (`―`). This will be
shown in the submission date column for PDFs that weren't created by
Server Pass. Ideally, the notes column should also simply have a
dash. If not, it will have all the notes raised for the given PDF.
Alerts (warning and error messages) are written to STDERR.

## Command Line Options

The following command line options are available:

  - `--version` or `-V` : print version information and exit.
  - `--help` or `-h` : print help and exit.
  - `--debug` : write debugging information.
  - `--out` _filename_ (or `-o`) : write output to _filename_.
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
time for Pass Checker.

---

 - &#x23EE; User Guides ⏵ [Pass CLI](passcli.md)
 - &#x23ED; [Server PASS](server-pass/README.md)
 - &#x23F6; [Documentation Summary](README.md)
