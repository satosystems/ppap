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

Extract the [latest of ppap-&lt;version&gt;.zip](https://github.com/satosystems/ppap/releases) and save it to a location with your PATH.

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
$ echo password=foobar > ~/.ppaprc
$ echo ~/.ppaprc
password=foobar
$ ppap foo
Created: foo.2.zip
$ unzip -t foo.2.zip
$ unzip -t foo.2.zip
Archive:  foo.2.zip
[foo.2.zip] foo password: # input `foobar'
    testing: foo                      OK
No errors detected in compressed data of foo.2.zip.
$
```

## 😀 Contributing

Contributions are welcome.

### 📝 Bug Reports & Feature Requests

Please use the [issue tracker](https://github.com/satosystems/ppap/issues) to report any bugs or features requests.

### 🍳 Developing

To begin developing, do this:

```shell-session
$ git clone git@github.com:satosystems/ppap.git
$ cd ppap
$ ./gradlew zip
...
$ ls .work # created script and jar file
ppap			ppap-v1.0.0.jar
$ ls out $ created zip file for release
ppap-v1.0.0.zip
$
```

[IntelliJ IDEA](https://www.jetbrains.com/idea/download/) can make your development easier.

## 🪶 License

ppap is under [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
