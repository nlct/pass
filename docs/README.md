# PASS Documentation

## General Assumptions

 - The university's domain is `example.com`

 - The computing school/department has a sub-domain
   `cmp.example.com`

 - A directory on this sub-domain is available for lecturers to 
   write the XML files needed by the various PASS applications:

   `http://cmp.example.com/pass/`

 - People:

   + Alice Nemo Smith is a lecturer. Her university username is
   `ans`. She doesn't have a student registration number because
   she's not a student.

   + Bob is a student. His university username is `vqs23ygl`
   and his student registration number is 327509401.

   + Carol is a student. Her university username is `jwh22ird`
   and her student registration number is 423901355.

 - Department devices:

   + Lab computers running an operating system with a desktop.
   For example, Windows, MacOS, or Linux with Gnome/Kde/Cinnamon
   etc.
   + Small devices for computer programming projects that have
   software development tools but don't have a desktop environment.

   These devices will need Java and a TeX installation as well.
   If the Java compiler isn't needed, then just install the Java runtime
   environment.

   For TeX Live, follow the [TUG instructions](https://www.tug.org/texlive/acquire-netinstall.html). If storage space is an issue and you don't
   need TeX for anything else on the device, you can select the
   "basic" scheme when installing TeX Live and opt out of installing
   the documentation and source. The following packages are required
   by PASS: `listings`, `attachfile`, `verbatim`, `upquote`, `graphicx`, `hyperref`,
   `fontenc`, `inputenc`, `lmodern`, `marvosym`, `geometry`. Most, but not all, of these
   should be installed with the basic scheme. The remainder can be
   installed with the TeX package manager (for example,
   `sudo tlmgr install attachfile upquote marvosym`). PASS should also
   work with MikTeX, but I haven't tested it.

## Documentation

 - [Compiling the Source Code](compile.md)
 - [Installing](install.md)
 - XML File Specifications
   + [Resource Files (Courses and Settings)](resourcexml.md)
   + [Assignment Files](assignmentxml.md)
 - User Guides
   + Pass GUI
   + Pass Editor
   + Pass CLI
 - [Server Pass](server-pass/README.md)

