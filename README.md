# 🕺 ppap

[PPAP](https://ja.wikipedia.org/wiki/PPAP_(%E3%82%BB%E3%82%AD%E3%83%A5%E3%83%AA%E3%83%86%E3%82%A3)) is send a zip file with `P`assword by email, send a `P`assword by email, `A`ngouka (encryption) and `P`rotocol acronym for this.
It's a silly practice, but one that often needs to be followed.
I sincerely hope that this practice will disappear soon.

Targeted at Mac users.

## 🚀 Features

- Compresses the specified file or folder and sets the password
- The password will be read from `$HOME/.ppaprc`
- If `$HOME/.ppaprc` is not found, the password will be generated automatically
- ZIP file entries should be encoded in Shift_JIS, because most recipients are Japanese Windows users

## 🎉 Installation

Since this program is written in Java, please prepare the Java execution environment in any way you wish.

### 🍺 Install via Homebrew (Recommended)

```shell-session
$ brew install satosystems/tap/ppap
...
$
```

### 🍳 Install via self build

Show [Developing](#-developing).

### Install via GitHub.

Extract the [latest of ppap-&lt;version&gt;.tar.gz](https://github.com/satosystems/ppap/releases) and save it to a location with your PATH.

## 🤔 Usage

```shell-session
$ mkdir temp
$ cd temp
$ touch foo
$ ppap foo
Created: foo.zip
Password is: `xm4_&@'{M+$8Q*j
$ ppap foo
Created: foo.1.zip
Password is: ,5H,((7%(=Te'A5K
$ mkdir bar
$ touch bar/baz
$ ppap foo bar
$ ppap foo bar
Created: archive.zip
Password is: LYVzE!~EY5]l?t.2
$ ppap -p bar
Enter password:
Verify password:
Created: bar.zip
$ echo password=foobar > ~/.ppaprc
$ echo ~/.ppaprc
password=foobar
$ ppap foo
Created: foo.2.zip
$ unzip -t foo.2.zip
$ unzip -t foo.2.zip
Archive:  foo.2.zip
[foo.2.zip] foo password:
    testing: foo                      OK
No errors detected in compressed data of foo.2.zip.
$ ppap -v
v1.1.1
$ ppap -h
usage: ppap [-h] [-p] [-v] [FILES/DIRS]

This is a CLI program that creates a ZIP file with a password.
 -h,--help       Show this help.
 -p,--password   Input password interactively.
 -v,--version    Show version.

Please report issues at https://github.com/satosystems/ppap/issues
$
```

## ⚙️ .ppaprc

```properties
charset=utf-8
password=foobar
ignore=^(\.DS_Store|\..+\.swp)$
```

- charset: Character set name of ZIP entry. Optional. Default value is `windows-31j`. Do not set it If you are not sure.
- password: ZIP password you always use. Optional.
- ignore: The name of the file you want to ignore, using a regular expression. If there is more than one file name, use a regular expression to represent more than one file name. Optional.

## 😀 Contributing

Contributions are welcome.

### 📝 Bug Reports & Feature Requests

Please use the [issue tracker](https://github.com/satosystems/ppap/issues) to report any bugs or features requests.

### 🍳 Developing

To begin developing, do this:

```shell-session
$ git clone git@github.com:satosystems/ppap.git
$ cd ppap
$ ./gradlew tar
...
$ ls .work # created script and jar file
ppap			ppap-v1.1.1.jar
$ ls out # created zip file for release
ppap-v1.1.1.tar.gz
$
```

[IntelliJ IDEA](https://www.jetbrains.com/idea/download/) can make your development easier.

## 🪶 License

ppap is under [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
