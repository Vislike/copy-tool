# copy-tool
The goal of this tool is to copy files over an unstable network connection.
It will try, try and try again until either you abort it or it has copied all files.

It is inspired by robocopy's restartable mode. But for me, that mode made robocopy very slow and I also got some corrupt files in the end.
This tool tries to avoid that by immediately closing all file handlers at the first sign of a problem, and then reopening them to try again. I also made it discard the last two buffers of copied data, just like the old FTP days, even though I found no case there it actually made any difference in my testing, but still kept it it since it did not hurt either.

The setup this tool was developed for:
WIFI to 4G modem/router.
VPN over 4G connection.
Windows share (smb) mounted over VPN.

This means that if something goes down, then the files will no longer be visible to your system, and tools like robocopy would simply stop retrying since it can no longer see any files.
This tool first index all files at the start, so it knows what files it needs to copy and simply retries indefinitely until it either succeeds or you manually abort. So if you loose 4G connection and VPN gets disconnected, so your smb shares are no longer mounted, then it will print errors until 4G connection is restored and VPN reconnected, and then it happily continue from where it left of.

Basic Usage:
```
ct *src* *dst* -d
```
Where **src** can be either a file or directory, and **dst** should always be a directory.
If **src** is a directory then it will operate in recursive mode.
**-d** means dry run, it will only list files, not actually copy them. To perform the copy simply remove **-d**.

Or to see the help:
```
ct -h
```

When checking **src** for files **dst** is also analyzed and it will use the two basic file stats size and modified time to determine if two files are the same. By default all existing files is skipped but this can be changed with the overwrite option (**-o**), then it just overwrites files it can determine has changed, all files that look the same (date and size) is always skipped.
