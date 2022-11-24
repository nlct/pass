# Server PASS Documentation

🚧 This is the documentation for Server PASS.

In addition to the [general assumptions](../README.md), also assume:

  - a server called `cmp-server-01` has been setup specifically for
    Server Pass;

  - the server has had appropriate firewalls set up so that the
    server can only connect to URLs that are sub-paths of
    `http://cmp.example.com/pass/` (where the Pass XML files and
    associated assignment files are) and other appropriate security
    settings;

  - the server has http server software installed with support for PHP and
    document root `/var/www/html` and supplementary directory
    `/var/www/inc` (which both have RW access for group `web-admin` and
    read access for the web user `www-data`);

  - the server has MySQL installed;

  - the server has Docker installed;

  - the server has RabbitMQ Docker image running;

  - the server has TeX Live installed in `/scratch/texlive` (for
    example, TL2022 is in `/scratch/texlive/2022`);

  - the scratch space is used for PASS:
    + a user `passdocker` with home `/scratch/passdocker`
      (permissions `drwxr-xr-x`, user `passdocker` and group `passdocker`);
    + upload directory `/scratch/uploads` with RW access for
      the web server user `www-data` and read access for
      group `www-data`;
    + PHP error log `/scratch/uploads/php_errors.log` with read
      access for group `www-data` as well as RW access for the
      web server user `www-data`;

  - Alice has an account on the server with her username `ans` and
    home directory `/home/ans`, she belongs to the following groups:
    + `docker` (which allows her to run `docker` without `sudo`)
    + `www-data`
    + `web-admin`

  - Alice can switch to the `passdocker` user with `sudo su - passdocker`.

## Installation

 - [Setting Up the Frontend](setupfrontend.md)
 - [Building the PASS Docker Image](buildingimage.md)
 - [Setting Up the Backend](setupbackend.md)
 - [Updates](updates.md)


## Website Documentation

### General Web Pages

 - [List Uploads](list-uploads.md)

### Admin Pages

Users with "admin" status can access the Admin pages.

 - [Configuration](admin-config.md)
 - [Users](admin-users.md)
 - [Upload Directories](list-uploads.md)
