# LIBPQ4S

LIBPQ4S is a Scala wrapper of the corresponding C library.


# Current status of the project

This is the first release of this library. 
LIBPQ4S is targeting for now the Scala Native platform.
The long-term goal is hwoever to provide similar features on the JVM (when the JNI implementation is done).

Please, also note that this library is not yet released on maven central, so to use it, you will have to clone this repository and publishLocal from sbt.

# Getting started
<!-- [![Maven Central](https://img.shields.io/maven-central/v/com.github.david-bouyssie/sqlite4s_native0.3_2.11/0.1.0)](https://mvnrepository.com/artifact/com.github.david-bouyssie/sqlite4s_native0.3_2.11/0.1.0) -->

If you are already familiar with Scala Native you can jump right in by adding the following dependency in your `sbt` build file.

```scala
libraryDependencies += "com.github.david-bouyssie" %%% "libpq4s" % "x.y.z"
```

To use in `sbt`, replace `x.y.z` with the latest version number (currently 0.1.0-SNAPSHOT).

<!-- To use in `sbt`, replace `x.y.z` with the version from Maven Central badge above.
     All available versions can be seen at the [Maven Repository](https://mvnrepository.com/artifact/com.github.david-bouyssie/sqlite4s). -->

If you are not familiar with Scala Native, please follow the relative [Getting Started](https://scala-native.readthedocs.io/en/latest/user/setup.html) instructions.

This library is using libpq and libuv C libraries, so you need to install them on you system as follows:

* Linux/Ubuntu

```
$ sudo apt-get install -y libpq5
$ sudo apt-get install -y libuv1
```

* macOS

```
$ brew install libpq
$ brew install libuv
```

* Other OSes need to have these libraries available on the system.
An alternative could consist in creating a project sub-directory called for instance "native-lib" and to put the LMDB shared library in this directory.
Then you would also have to change the build.sbt file and add the following settings:
```
nativeLinkingOptions ++= Seq("-L" ++ baseDirectory.value.getAbsolutePath() ++ "/native-lib")
```

# How to generate LibPQ bindings

The following code section can be useful to generate bindings for Scala Native and for JNI.

## How to generate Scala Native bindings?

```
# Retrieve the CLI version of Scala Native bindgen (https://github.com/scala-native/scala-native-bindgen)
mkdir snbindings
cd snbindings
wget https://github.com/scala-native/scala-native-bindgen/releases/download/v0.3.1/scala-native-bindgen-linux

# Copy some required header files
cp /usr/include/postgresql/*.h .
cp /path/to/stdarg.h .
cp /path/to/stddef.h .
cp /path/to/stdio.h .
cp /path/to/libio.h .
cp /path/to/_G_config.h .

# Fix header files to point to local files instead of system ones

# Execute scala-native-bindgen-linux command
# See: https://scala-native.github.io/scala-native-bindgen/cli.html
./scala-native-bindgen-linux --name pq --link pq --package com.github.libpq4s.bindings libpq-fe.h -- > pqbindings.scala
```

## How to generate and use bindings for the JVM?

### Generating JNI bindings using SWIG

```
# Install LibPQ
sudo apt-get install -y libpq-dev

# Install Swig from package
sudo apt-get install swig

# Or install Swig from source
# See: http://www.linuxfromscratch.org/blfs/view/svn/general/swig.html
sudo apt-get install libpcre3-dev
wget https://downloads.sourceforge.net/swig/swig-4.0.1.tar.gz
tar -xzf swig-4.0.1.tar.gz
cd swig-4.0.1/
./configure --prefix=/usr --without-maximum-compile-warnings && make
sudo make install \
 mkdir /usr/share/doc/swig-4.0.1 \
 install -v -m755 -d /usr/share/doc/swig-4.0.1 \
 cp -v -R Doc/* /usr/share/doc/swig-4.0.1

# Prepare a SWIG interface file "pqswig.i" describing the C functions to wrap, by including "libpq-fe.h".
# About libpq-fe.h: this file contains definitions for structures and externs for functions used by frontend postgres applications.

cat <<EOF >> pqswig.i
%module pq

%{
#include "/usr/include/postgresql/libpq-fe.h"
%}

%include "carrays.i"
%array_functions(int, intArray);

%include "various.i"
%apply char **STRING_ARRAY { char ** };

%include "/usr/include/postgresql/libpq-fe.h"
EOF

# Generate Swig bindings
# Remarks:
# - the current directory will contain a pqswig_wrapper.c file. 
# - in the indicated path (./bindings, change it following your needs) you'll find the Java wrapper classes.

mkdir bindings
swig -java \
  -package com.github.libpq4s.bindings \
  -outdir ./bindings \
  pqswig.i
```


### Compiling the swigged library for Linux

```
# Compile the pqswig_wrapper.c file

gcc -O3 -fpic -c pqswig_wrap.c -I /usr/lib/jvm/java-8-openjdk-amd64/include/ -I /usr/lib/jvm/java-8-openjdk-amd64/include/linux

# Build the dynamic library
ld -G pqswig_wrap.o -o libpqswig.so -lpq

# Copy (or link) the libpqswig.o file into a directory of the java.library.path. For example, copy it into the /usr/lib directory:
# cp libpqswig.so /usr/lib/
```

### Compiling the swigged library for Windows

```
sudo apt-get install mingw-w64
sudo apt install bison
git clone https://github.com/postgres/postgres.git
cd postgres
./configure --host=x86_64-w64-mingw32 --without-zlib --prefix=/home/ubuntu/libpq/
make -C src/common
make -C src/interfaces/libpq

x86_64-w64-mingw32-gcc -fpic -Wall -Wextra -L"/usr/x86_64-w64-mingw32/lib/" -L"/usr/lib/x86_64-linux-gnu/" \
  -I /usr/lib/jvm/java-8-openjdk-amd64/include/ -I /usr/lib/jvm/java-8-openjdk-amd64/include/linux \
  pqswig_wrap.c

x86_64-w64-mingw32-gcc -fpic -Wall -Wextra -L"/usr/x86_64-w64-mingw32/lib/" -L"/usr/lib/x86_64-linux-gnu/" \
  -I /usr/lib/jvm/java-8-openjdk-amd64/include/ -I /usr/lib/jvm/java-8-openjdk-amd64/include/linux \
  -L/home/ubuntu/libpq/postgres/src/interfaces/libpq/ -llibpq -I/home/ubuntu/libpq/postgres/src/interfaces/libpq/ \
  -shared -o libpqswig.dll pqswig_wrap.c
```

# How to extract the doxygen documentation?

```
# Install doxygen
sudo apt-get install doxygen

# Generate default config file
mkdir doxygen && cd doxygen
doxygen ‑g

# Copy src files and patch comments to follow doxygen JavaDoc style
mkdir src
cp ../postgres/src/interfaces/libpq/*.c ./src/
cd src
sed -i -e 's/\/\*/\/**/' *.c

# Customize config file
# PROJECT_NAME           = "LIBPQ"
# OUTPUT_DIRECTORY       = "./"
# INPUT                  = "./src/"
# GENERATE_HTML          = NO
# GENERATE_LATEX         = NO
# GENERATE_XML           = YES
# CLASS_DIAGRAMS         = NO

# Run doxygen
doxygen Doxyfile

# Convert doxygen xml to swig doc
cd xml
wget https://raw.githubusercontent.com/m7thon/doxy2swig/master/doxy2swig.py
python3 doxy2swig.py index.xml pq-swig-doc.i
```

# Webography (for development purpose)

## Libpq

### API and examples
- https://doxygen.postgresql.org/testlibpq_8c_source.html
- https://www.postgresql.org/docs/12/libpq.html
- http://zetcode.com/db/postgresqlc/
- https://www.postgresql.org/docs/12/libpq-example.html
- https://github.com/postgres/postgres/tree/master/src/test/examples
- https://gist.github.com/ictlyh/12fe787ec265b33fd7e4b0bd08bc27cb
- https://gist.github.com/Meekohi/11291680
- https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/C/h2o/src
- https://stackoverflow.com/questions/43922809/libpq-how-to-pass-bulk-data

### Postgres wire protocol
- https://www.postgresql.org/docs/current/protocol.html
- https://github.com/pgjdbc/pgjdbc/tree/master/pgjdbc/src/main/java/org/postgresql/core
- https://github.com/dwclark/postgresql/tree/master/protocol/src/main/java/db/postgresql/protocol/v3
- https://github.com/crate/crate/blob/master/server/src/main/java/io/crate/protocols/postgres/
- https://github.com/finagle/finagle-postgres/tree/master/src/main/scala/com/twitter/finagle/postgres
- https://github.com/traneio/ndbc/tree/master/ndbc-postgres/src/main/java/io/trane/ndbc/postgres/proto
- https://github.com/r2dbc/r2dbc-postgresql/tree/master/src/main/java/io/r2dbc/postgresql
- https://github.com/mauricio/postgresql-async
- https://github.com/alaisi/postgres-async-driver
- https://github.com/tpolecat/skunk/tree/master/modules/core/src/main/scala

### Libpq and batch mode
- https://www.2ndquadrant.com/en/blog/postgresql-latency-pipelining-batching/
- https://bytes.com/topic/postgresql/answers/173321-another-command-already-progress-error-fwd
- https://github.com/2ndQuadrant/postgres/tree/dev/libpq-async-batch/
- https://github.com/royaltm/ruby-em-pg-client/issues/13
- http://www.programmersought.com/article/88611472425/
- https://commitfest.postgresql.org/14/1024/
- https://www.postgresql.org/message-id/CAMsr+YFgDUiJ37DEfPRk8WDBuZ58psdAYJd8iNFSaGxtw=wU3g@mail.gmail.com
- https://www.mail-archive.com/pgsql-hackers@postgresql.org/msg322454.html
- https://github.com/jasync-sql/jasync-sql/wiki/PostgreSQL-Types

### JNI bindings

#### Swig
- http://benfante.blogspot.com/2013/02/using-postgresql-in-java-without-jdbc.html
- https://github.com/benfante/libpq-wrapper
- http://www.swig.org/Doc4.0/Java.html
- http://www.swig.org/Doc4.0/Java.html#Java_converting_java_string_arrays
- https://github.com/swig/swig/blob/master/Lib/java/various.i
- https://stackoverflow.com/questions/12304806/making-swig-understand-char-for-using-it-in-java
- https://stackoverflow.com/questions/46846754/swig-char-as-a-pointer-to-a-char

#### Cross-compilation links
- https://wiki.postgresql.org/wiki/Building_With_MinGW#Cross_Compiling
- http://hectorhon.blogspot.com/2018/08/building-libpq-on-msys2-mingw-64-bit.html
- https://stackoverflow.com/questions/2033997/how-to-compile-for-windows-on-linux-with-gcc-g/47061145#47061145
- https://superuser.com/questions/1438675/how-to-link-a-64bit-dll-file-on-linux-using-mingw
- https://www.transmissionzero.co.uk/computing/building-dlls-with-mingw/

#### Doxygen
- https://stackoverflow.com/questions/37780325/generating-doxygen-comments-for-swig-generated-c-sharp-that-wraps-c
- https://developer.ibm.com/articles/au-learningdoxygen/
- http://www.doxygen.nl/manual/docblocks.html
- http://www.doxygen.nl/manual/config.html#cfg_javadoc_autobrief
- http://www.doxygen.nl/manual/output.html
- http://www.doxygen.nl/manual/customize.html#xmlgenerator
- https://gist.github.com/ugovaretto/261bd1d16d9a0d2e76ee

#### Swig & doxgyen
- https://stackoverflow.com/questions/12413957/export-comments-in-swig-wrapper
- http://www.swig.org/Doc4.0/Java.html#Java_javadoc_comments
- https://github.com/m7thon/doxy2swig
- https://github.com/artclarke/xuggle-swig

## Libuv

### libuv and polling
- https://stackoverflow.com/questions/19335408/why-does-libpq-use-polling-rather-than-notification-for-data-fetch
- https://stackoverflow.com/questions/54558801/how-to-execute-an-async-query-with-libpq
- http://man7.org/linux/man-pages/man7/epoll.7.html
- https://github.com/alex-eri/libuvpg
- https://github.com/rootmos/libpquv
- https://gist.github.com/kassane/b43cde40818c35be0a23104e1f978a26
- https://github.com/TechEmpower/FrameworkBenchmarks/blob/master/frameworks/C/h2o/src/database.c#L297
- https://github.com/scala-native/scala-native-loop/blob/a0873e56da1fee2290c8de473ee6284b49c0c66f/client/curl.scala#L167

### libuv and JNI
- http://blog.jonasbandi.net/2014/03/running-nodejs-applications-on-jvm-with.html
- https://stackoverflow.com/questions/22289753/avatar-js-and-project-avatar
- https://github.com/stefanjauker/avatar-js
- https://github.com/stefanjauker/libuv-java
