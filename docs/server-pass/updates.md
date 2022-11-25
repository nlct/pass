# Updating the Backend

If you have to install a new version of Server PASS you will need to
take the backend offline. Find the process ID:

```bash
ps aux | grep passconsumer
```

If this only shows the `grep` command then the backend is already
down otherwise kill the process using the process ID _N_ obtained from
the previous command (if it's currently running):

<pre>
kill -9 _N_
</pre>

Check for any stray containers that haven't been deleted:

```bash
docker ps -a
```

(This is only likely to happen if the `passconsumer.php` script was
interrupted before it was able to delete a container it had
started.) 

Copy over `pass-cli-server.tgz` and create the new Docker image as
per the [setting up the backend instructions](buildingimage.md).
Restart the backend.

```bash
sudo su - passdocker
nohup ./passconsumer.php &
exit
```

(You may want to empty the log files first, but if you delete them,
make sure you create them again with the correct permissions.)

---

 - Prev: Installation ⏵ [Setting Up the Backend](setupbackend.md)

