# Pass GUI User Guide

If Pass GUI was installed with the [`pass-installer.jar` installer](install.md)
then it can be run from the Start menu. Note that this will run
Pass GUI in non-debug mode. If the only course in the `resources.xml` file
has `debug="true"` then no courses will be available, which will
generate the error "No course data provided".

Pass GUI can also be invoked from the command line:
```bash
java -jar /path/to/PASS/lib/progassignsys.jar
```
where `/path/to/PASS` is the path to the Pass GUI installation.
Alternative, for Linux or Mac you can use the bash script in the
`bin` directory:
```bash
/path/to/PASS/bin/progassignsys
```
Add the `--debug` switch to make the test course available.

---

 - Prev: XML File Specifications ⏵ [Assignment Files](assignmentxml.md)
 - Next: User Guides ⏵ [Pass Editor](passeditor.md)
