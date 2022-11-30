# Server Pass Setup

In addition to the [general assumptions](../README.md), also assume:

  - a server called `cmp-server-01` has been setup specifically for
    Server Pass;

  - the server has had appropriate firewalls set up so that the
    server can only connect to URLs that are sub-paths of
    `http://cmp.example.com/pass/` (where the Pass XML files and
    associated assignment files are) and other appropriate security
    settings are in place, such as limiting access to the server;

  - the server has http server software installed with 
    document root `/var/www/html` and a supplementary directory
    `/var/www/inc` (which both have RW access for group `web-admin` and
    read access for the web user `www-data`) and support for:
    + PHP
    + mysqli
    + json
    + SimpleXML
    + sodium
    + sendmail

  - the web site's URL is `serverpass.cmp.example.com`.

  - the server has MySQL installed;

  - the server has Docker installed;

  - the server has a RabbitMQ Docker image with a container running (that can be
    restarted with `docker start rabbitmq`);

  - the scratch space is used for PASS with:
    + a user `passdocker` (for the backend):
      - belonging to groups `passdocker`, `www-data` and `docker`;
      - with home directory `/scratch/passdocker`
        (permissions `drwxr-xr-x`, user `passdocker` and group `passdocker`);
    + upload directory `/scratch/uploads` with RW access for
      the web server user `www-data` and read access for
      group `www-data`;
    + PHP error log `/scratch/uploads/php_errors.log` with RW access for the
      web server user `www-data` and group `www-data`;
    + TeX Live installed in `/scratch/texlive` (for
      example, TL2022 is in `/scratch/texlive/2022`);

  - Alice has an account on the server with her username `ans` and
    home directory `/home/ans`, she belongs to the following groups:
    + `docker` (which allows her to run `docker` without `sudo`);
    + `www-data` (which allows her to read files that the frontend website
      can read);
    + `web-admin` (which allows her to read and write the frontend website
      pages).

  - Alice can switch to the `passdocker` user with `sudo su - passdocker`.

## Installation

Alice will have to ask her IT support to ensure all the above. She then
may be able to do the rest of the installation by following the
instructions below.

 1. [Setting Up the Frontend](setupfrontend.md)
 2. [Building the PASS Docker Image](buildingimage.md)
 3. [Setting Up the Backend](setupbackend.md)
 4. [Updates](updates.md)


---

 - Previous: [README](README.md)
 - Next: Installation ⏵ [Setting Up the Frontend](setupfrontend.md)
