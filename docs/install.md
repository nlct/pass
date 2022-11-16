# Installing

After [compiling and testing](compile.md) all the source code,
the various command line applications can be prepared for installation:
```bash
make -C pass-cli ../dist/pass-cli.zip
make -C pass-cli-server ../dist/pass-cli-server.tgz
make -C pass-checker ../dist/pass-checker.zip
```

These all create archives in the `dist` directory, which can simply
be copied to the appropriate device:

  - `pass-cli.zip`: this can be unpacked on any small devices
    available for computer programming projects that have
    software development tools but don't have a desktop environment.
    Add the `bin` directory to the system path. This contains a bash
    script that invokes the `pass-cli.jar` file in the `lib`
    directory. If bash isn't available on the device, replace this
    with another appropriate script. The device will need
    Java and TeX installed, see <README.md>.

  - `pass-cli-server.tgz`: this should be unpacked on the server
    that will be used for Server Pass. It then needs to be
    incorporated into a Docker image. See the [Server Pass](server-pass)
    documentation for further details.

  - `pass-checker.zip`: this can be distributed to lecturers who
    have instructed students to use Pass to prepare their
    programming assignments for submission. Unpack the archive to
    an appropriate location and add the `bin` directory to the
    system path. As with Pass CLI, this contains a bash
    script that invokes the `pass-cli.jar` file in the `lib`
    directory. If bash isn't available on the device, replace this
    with another appropriate script.

The GUI applications use [IzPack](http://izpack.org/) to create an installer.
If you don't already have IzPack, you can download it from
<http://izpack.org/downloads/>. The Pass GUI and Pass Editor
Makefiles assume that IzPack has been installed in the directory
`/usr/local/IzPack`. If it has been installed elsewhere, you'll have
to modify the `IZPACKDIR` variable. Then run:
```bash
make -C pass-gui ../dist/pass-installer.jar
make -C pass-editor ../dist/pass-editor-installer.jar
```
These jar files are the installers for Pass GUI and Pass Editor.
Copy them to the lab computers that have the software development
tools, and run the appropriate installer. These computers will
also need TeX installed, see <README.md>.
