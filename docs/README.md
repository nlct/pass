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
   If the Java compiler isn't needed, then just the Java runtime
   environment. A minimal TeX installation can be used, as long as
   it has the following packages:  listings, attachfile, verbatim,
   upquote, graphicx, hyperref, fontenc, inputenc, lmodern, marvosym, geometry.

## Documentation

 - [Compiling the Source Code](compile.md)
 - Installing
 - User Guides
   + Pass GUI
   + Pass Editor
   + Pass CLI
 - [Server Pass](server-pass/README.md)

