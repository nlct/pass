# Upload Project

Make sure that you have set your student registration number before
uploading a project. If you haven't, the upload page will redirect
you to the [set registration number page](account.md). If you are
part of a group project, ensure that each member of the group has
created an account and set their registration number.

To upload a project, click on the "Upload Project" link in the
navigation bar, which will take you to the course selection.

The default upload web page requires JavaScript. If you don't have
JavaScript enabled, you will move onto the fallback web page when
you click on the first "Next" button. The file upload section also
has a link to the fallback which you can use if you have a problem
with the file upload widgets.
The fallback page does have some JavaScript but can function
without it.

The upload page starts with the same blurb that's shown on the
unauthenticated home page.

> This is a web-script alternative to the desktop PASS (Preparing
> Programming Assignments for Submission System) GUI application.
> This tool helps to _prepare_ computer programming assignments into a
> format suitable for submission. **PASS does not submit your work for
> you.** It's your responsibility to check that the PDF created by
> this tool accurately represents your assignment before you submit
> it via Blackboard. Please read the terms and conditions before
> using this site.
>
> **Note**: the PDF created by PASS doesn't display your name anywhere as
> assignment submissions are anonymous. However, your Blackboard ID
> (Uni of Example username) and registration number are required **in order to log
> your marks.** It's therefore essential that you ensure you have your
> Blackboard ID and registration number correctly entered in your PASS
> account. 

(Admin note: Text such as "Blackboard", "Blackboard ID", and "Uni of
Example username" can be changed to something more applicable by making
the [appropriate modification to `PassConfig.php`](setupfrontend.md).)

## Course Selection

> Course: <kbd>--Please Select-- ⏷</kbd> <kbd>Next ⏵</kbd>

Select your course from the dropdown selector. Click "Next" to continue.

For example, Bob has been paired with Carol for the "Shop"
assignment for the CMP-101 (Introduction to Java) course. They have
worked on the assignment together and Bob has been chosen to upload
their work. He selects the course "CMP-101: Introduction to
Java", then clicks "Next".

## Assignment Selection

> 'CMP-101' Assignment: <kbd>--Please Select-- ⏷</kbd>  
> Number of Students in Project (1 for solo projects): `1`⇳  
> <kbd>⏴ Previous</kbd> <kbd>Next ⏵</kbd>

The next page has a dropdown selector listing all the assignments
for your selected course. Select your assignment title. If you have
been assigned to a group for a group project, enter how many
students are in the group. Leave the value as 1 for a solo project.

For example, Bob selects "Shop" for the assignment and sets the number
of students to 2. Then he clicks on "Next".

## Project Group Members

For group projects (as in the example with Bob and Carol) the
next page is the "Project Group Members" page. This page is skipped
for solo projects if JavaScript is enabled. Without JavaScript, the
fallback upload will call this section "Solo Project" and will only
have the one uneditable row in the table.

> Please ensure that all members of the group have created an
> account and have set their registration number before proceeding.
>
> | | **Blackboard ID** |
> | --- | --- |
> | **Student 1** | vqs23ygl |
> | **Student 2** | `jwh22ird` |
>
> <kbd>⏴ Previous</kbd> <kbd>Next ⏵</kbd>

In the example, Bob specified two students, so the Project Group
Members page has a table with his username on the first row
("Student 1") which can't be edited. The next row ("Student 2") has
a text field for the second student's username. Bob puts Carol's
username (`jwh22ird`) in here.

Note that Carol must also have a Server Pass account with her
registration number set, even though she isn't the one uploading the
group project. If she doesn't have an account, or if Bob misspells
her username, there will be an error when he clicks on the "Next"
button. If the username is correct, but Carol hasn't set her
registration number, there will also be an error.

The next page will vary depending on whether or not you are using
the default upload page that requires JavaScript or the fallback
that can function without JavaScript.

## File Uploads (Default)

The default file uploads page uses JavaScript to make it easier to
upload multiple files.

> File names may only contain alphanumerics (a–z, A–Z, 0–9),
> underscores `_` or hyphens `-` or plus `+` (and a dot for the
> extension). **File names are case-sensitive.** 

Binary files (except PDF or Word) are normally prohibited, but if your
lecturer has instructed PASS to allow certain binary files, they
will be listed here.

The default text if only PDF or Word documents are allowed is:

> The only permitted binary files are PDF or Word documents. Make sure you set the
> correct file type.

The example "Shop" assignment allows PNG or JPEG files, so the text
is:

> The only permitted binary files are PDF or Word documents or files
> with the following extensions: .png, .jpeg, .jpg. Make sure you
> set the correct file type. 

You need to specify the [file encoding](https://dickimaw-books.com/blog/binary-files-text-files-and-file-encodings/), which may
be either UTF-8 (default), ASCII or Latin 1. Make sure that you
select the encoding that matches your source code files.

> File encoding: <kbd>UTF-8⏷</kbd>

This is followed by a link to the FAQ entry "What's the difference between
ASCII, Latin 1 and UTF-8?"

The maximum number of files (including any required files) is
listed. This is a server-side setting.

If your source code is contained in sub-directories and the relative
directory structure needs to be preserved, select the "Project has
sub-directories" checkbox.

> ☐ Project has sub-directories.

If this box is checked, then there's additional information:

> 🗹 Project has sub-directories.
>
> The sub-path relative to the project's base directory should be
> provided in the field near the browse buttons below. Omit the
> final slash. The sub-path must not start with a slash. Each path
> element must consist of only alphanumerics (a-z, A-Z, 0-9) or an
> underscore (`_`) or a hyphen (`-`). Use a single dot (`.`) to indicate
> the project's base directory.

You can select all your source code files in your file
browser/manager (for example, File Explorer on Windows or Nautilus
on Linux) and drag and drop them into the drop area. Alternatively,
you can use the multi-file selector. If any of the selected files
match required filenames, they will be added as appropriate in the
"Required Files" section, otherwise they will be added to the
"Additional Files (Optional)" section.

> ---  
>  
> Either use the upload 📂 file selectors below or drag and drop
> files here in this drop area. Alternatively, you can use this
> multi-file selector: <kbd>📂</kbd>  
>
>  Make sure that you check that any required files have been
>  correctly identified below, where appropriate, and check the
>  language selectors for additional files.
>
> 🗐
> 
> ---  

If your browser doesn't support drag and drop the drop area won't be
available.

Alternatively, you can use the individual file selector next to each
filename in the "Required Files" section (if applicable) and the
"Additional Files (Optional)" section.

The "Required Files" section lists each required file with a file
selector button followed by the text "No file selected" if you
haven't yet selected that file or a tick &#x2705; if you have selected it
correctly or a cross &#x274C; and an error message if there was a problem
with the selected file.

For example, the "Shop" assignment requires the files
`Product.java`, `Shop.java`, `Main.java` and
`UnknownProductException.java` so the "Required Files" section
starts out as:

> Product.java <kbd>📂</kbd> No file selected.  
> Shop.java <kbd>📂</kbd> No file selected.  
> Main.java <kbd>📂</kbd> No file selected.  
> UnknownProductException.java <kbd>📂</kbd> No file selected.  

The Shop assignment has an accompanying file called `products.csv`
that Shop application needs to read. This should not be uploaded,
but it's listed in the "Supplied Project Files" section. This
section will be omitted if there are no supplied project files.

> This project has 1 supplied file which will automatically be fetched by PASS. This file should not be uploaded.
>
> products.csv

The Shop assignment requires the project application to create a
file called `receipt.txt`. This should also not be uploaded, but is
listed in the "Project Result Files" section. This section will be
omitted if there are no required result files.

> This project has 1 result file which should be created by your application. This files should not be uploaded.
>
> receipt.txt

Each additional file is listed in a separate row as:

> Supplementary File _N_: <kbd>&#x1F4C2;</kbd> _filename_ <kbd>Plain Text&#x23F7;</kbd>

where _filename_ is the name of the selected file or "No file
selected" if a file hasn't been selected. The language selector
needs to be set as applicable.

For example, Bob and Carol have also created a file called
`Currency.java`. So Bob selects the four required files and the
extra file in File Explorer and drags and drops them into the drop
area or uses the multi-file selector in the drop area and selects
them all in that.

A confirmation message appears at the bottom of the drop area:

> &#x2705; 5 files added.

If Bob accidentally selected the `products.csv` file, the above
message will be followed by:

> &#x274C; 1 file omitted (forbidden type or filename contains forbidden characters or filename conflicts with supplied project file).

The Required Files section now has:

> Product.java <kbd>📂</kbd> &#x2705;  
> Shop.java <kbd>📂</kbd> &#x2705;  
> Main.java <kbd>📂</kbd> &#x2705;  
> UnknownProductException.java <kbd>📂</kbd> &#x2705;

The Additional Files section has:

> Supplementary File 1: <kbd>&#x1F4C2;</kbd> Currency.java &#x2705; <kbd>Java&#x23F7;</kbd>

If a required file is listed as an additional file after dragging
and dropping it or using the multi-file selector, double-check the
filename (including the case).

Bob and Carol also have image files used by their application that
are in a sub-directory called `icons`. This directory structure
needs to be preserved, so Bob scrolls back up and checks the
"Project has sub-directories" checkbox. This adds a text field for
the sub-directory for each file with the default value `.` (project
base directory):

> `.`/Product.java <kbd>📂</kbd> &#x2705;  
> `.`/Shop.java <kbd>📂</kbd> &#x2705;  
> `.`/Main.java <kbd>📂</kbd> &#x2705;  
> `.`/UnknownProductException.java <kbd>📂</kbd> &#x2705;
>
> Supplementary File 1: <kbd>&#x1F4C2;</kbd> `.`/Currency.java &#x2705; <kbd>Java&#x23F7;</kbd>

Bob goes back to the File Explorer, selects all the image files
(`logo.png` and `sample.png`) and drags and drops them onto the drop
area (or uses the multi-file selector). These are added to the
"Additional Files" section.

> Supplementary File 1: <kbd>&#x1F4C2;</kbd> `.`/Currency.java &#x2705; <kbd>Java&#x23F7;</kbd>  
> Supplementary File 2: <kbd>&#x1F4C2;</kbd> `.`/logo.png &#x2705; <kbd>BINARY&#x23F7;</kbd>  
> Supplementary File 3: <kbd>&#x1F4C2;</kbd> `.`/sample.png &#x2705; <kbd>BINARY&#x23F7;</kbd>

Bob now needs to change the subpath from the default `.` to `icons`
(no trailing slash). If the path consists of multiple elements, use
a forward slash to separate them.

There are four supplementary file fields by default.  If you need
more, enter the number of extra fields in the selector and click on the "Add"
button.

> <kbd>Add</kbd> `1`⇳ more file field(s).

Note that it's not possible to search your device for project files
in the way that Pass GUI can do. Browsers don't allow websites to
access your filing system for your privacy and security. The file
selector only tells the website the base filename not the full file path.

There's no provision to upload an archive of all your files. Only
upload the actual project source code and allowed binaries. Don't
upload version control files or IDE files or binary files that
haven't been listed as allowed.

Once you have selected all your project files, check the agreement
box and click the "Next" button to continue.

> 🗹 We agree that by submitting a PDF generated by PASS we are confirming that we have checked the PDF and that it correctly represents our submission.

(For a solo project the text will start "I agree".)

> <kbd>⏴ Previous</kbd> <kbd>Next ⏵</kbd>

The final line provides a link to the fallback page in the event
that file selectors don't work.

> If you experience a problem with uploading files, try the simpler fallback upload script.

The link will take you to the Project Group Members page of the fallback
script so that you don't have to re-specify the course and assignment.

Otherwise, click on "Next". If there are no problems, you will be
redirected to the [Uploads List page](list-uploads.md).

## File Uploads (Fallback)

The fallback upload splits the file upload information into two
parts.

In the first part, you need to specify the [file encoding](https://dickimaw-books.com/blog/binary-files-text-files-and-file-encodings/), which may
be either UTF-8 (default), ASCII or Latin 1. Make sure that you
select the encoding that matches your source code files.

> File encoding: <kbd>UTF-8⏷</kbd>

This is followed by a link to the FAQ entry "What's the difference between
ASCII, Latin 1 and UTF-8?"

If your source code is contained in sub-directories and the relative
directory structure needs to be preserved, select the "Project has
sub-directories" checkbox.

> ☐ Project has sub-directories.

If your assignment has required filenames, these will be listed.

You can also upload additional files. Binary files (except PDF/Word)
are normally prohibited, but if your lecturer has instructed PASS to
allow certain binary files, they will be listed here. You need to
specify how many additional files that you want to upload. This
needs to be the total number of files minus the required number of
files.

> For this assignment, you are required to provide files called:  
>  - `Product.java`
>  - `Shop.java`
>  - `Main.java`
>  - `UnknownProductException.java`

This is then followed by additional file information.
If only PDF or Word binaries are allowed, the following text is
shown:

> You can upload additional files (source code or accompanying
> PDF/Word reports) if you want. No binary files are permitted
> (aside from PDF/Word).

The Shop example allows PNG and JPEG files, so the text is:

> You can upload additional files (source code or accompanying
> PDF/Word reports) if you want. This assignment allows you to
> upload binary files with the following extensions: .png, .jpeg,
> .jpg.

In both cases, this is followed by:

> How many additional files do you want to upload? `0`⇳

Bob and Carol have an additional source code file called
`Currency.java` and two image files, which are part of their
project. This means that they have 3 additional files.

Bob and Carol's IDE is set to UTF-8 so Bob leaves the file encoding
on the default UTF-8 and enters "3" in the additional files field.

The end of this section lists the maximum number of allowed files
(including the required files).

Click "Next" to move to the next part.

The second part of the fallback file uploads first lists the
required files with the required filename followed by a file
selector. Use this selector to select the corresponding file from
your device.

> Product.java <kbd>Browse...</kbd> No file selected.
> Shop.java <kbd>Browse...</kbd> No file selected.
> Main.java <kbd>Browse...</kbd> No file selected.
> UnknownProductException.java <kbd>Browse...</kbd> No file selected.

As with the main upload page, if the project has supplied files or
required result files, these will also be listed.

If you specified a number of additional files, there will an
"Additional Files (Optional)" section with a row corresponding to
each additional file.

Since Bob specified 3 additional files, there will be three rows:

> Supplementary File 1: <kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>
> Supplementary File 2: <kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>
> Supplementary File 3: <kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>

Bob and Carol's images are in a sub-directory but Bob forgot to
check the "Project has sub-directories" checkbox, so he has to click
on "Previous" to go back and select it. (If Bob has already selected
some files, he will unfortunately have to reselect them all over
again.)

Each file row now has the relative path field with the default `.`
(current directory):

> Product.java `.`/<kbd>Browse...</kbd> No file selected.
> Shop.java `.`/<kbd>Browse...</kbd> No file selected.
> Main.java `.`/<kbd>Browse...</kbd> No file selected.
> UnknownProductException.java `.`/<kbd>Browse...</kbd> No file selected.

> Supplementary File 1: `.`/<kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>
> Supplementary File 2: `.`/<kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>
> Supplementary File 3: `.`/<kbd>Browse...</kbd> <kbd>Plain Text&#x23F7;</kbd>

Use the <kbd>Browse...</kbd> button to select each file.  For the
additional files, change the dropdown selector to the corresponding
language.  (For binaries, select "PDF" for PDF files "Word" for
`.doc` or `.docx` files or "BINARY" for any other allowed binary
format.)

For example, Bob and Carol have a Java file, so Bob needs to select
their additional file `Currency.java` and change the selector to
"Java". They also have to select each image file and change the
sub-path to `icons` for those rows and change the selector to
"BINARY".

Once you have selected all your project files, check the agreement
box and click the "Next" button to continue.

> 🗹 We agree that by submitting a PDF generated by PASS we are confirming that we have checked the PDF and that it correctly represents our submission.

(For a solo project the text will start "I agree".)

> <kbd>⏴ Previous</kbd> <kbd>Next ⏵</kbd>

If there are no problems, you will be redirected to the [Uploads List page](list-uploads.md).

---

 - &#x23EE; [Account Details](account.md)
 - &#x23ED; [List Uploads](list-uploads.md)
 - &#x23F6; [Server Pass Summary](README.md)
