# Setting up the Backend

Once you have [setup the frontend](setupfrontend.md) and [built the
PASS Docker image](buildingimage.md), the backend needs to be
setup and started. This has to be done by the `passdocker` user, but first all
the relevant files and directories need to be created and have the
appropriate permissions set.

```bash
sudo su - passdocker
mkdir completed
chmod 750 completed
chgrp www-data completed
touch nohup.out
touch passdocker.log
chmod 640 nohup.out passdocker.log
chgrp www-data nohup.out passdocker.log
```

This should create the following:

  - a directory for completed processes
    `/scratch/passdocker/completed` (permissions `drwxr-x---`,
    user `passdocker` and group `www-data`);
  - a nohup log file `/scratch/passdocker/nohup.out`
    (permissions `-rw-r-----`, user `passdocker` and group
    `www-data`);
  - a PASS backend log file `/scratch/passdocker/passdocker.log`
    (permissions `-rw-r-----`, user `passdocker` and group
    `www-data`);

Assuming that Alice has FTP'd the template file
`passconsumer.php-template` to her home directory `/home/ans/`, she
now needs to copy it over:

```bash
cp /home/ans/passconsumer.php-template passconsumer.php
chmod 770 passconsumer.php
```

Next Alice needs to edit this file and change the `connectConsumer`
function so that it has the correct RabbitMQ username and password.
(Also change the hostname and port, if applicable.)

The backend can now be started:

```bash
nohup ./passconsumer.php &
```

This should produce the message:

> nohup: ignoring input and appending output to 'nohup.out'

Press <kbd>Enter</kbd> to get a clear command prompt. If you get the
message:

> [1]+ Exit 255

then check that RabbitMQ is still running and check the log files
for error messages. If you still can't find out the problem, try
running the script in the foreground:

```bash
./passconsumer.php
```

The normal operation of this script is to wait silently until it
receives a message from the queue, so if it's working properly and
there are no jobs it won't appear to be doing anything. Exit with
<kbd>Ctrl</kbd>+<kbd>C</kbd>) and try running it again with `nohup`.

---

 - Prev: Installation ⏵ [Building the PASS Docker Image](buildingimage.md)
 - Next: Installation ⏵ [Updates](updates.md)

