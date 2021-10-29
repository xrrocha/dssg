# Dead-simple Site Generator

Just convert my files and shut the f..k up!

_I_ dictate the directory structure and everything else.

## Build Options

For convenience, directory `releases` contains ready-made executables (native images are `amd64/linux` only.)

The brave may build this utility as:

- A fast native image (recommended)
- A Java-only fat jar file (no Scala at runtime)
- A simple Scala jar file (Scala required at runtime)

### 1. Building a native image

This is a bit more involved, but it's worth the effort as it gives you a faster, portable, native image requiring no Java or Scala at runtime!

### 1.1. Install GraalVM

You must install [GraalVM](https://www.graalvm.org) first, set it as your `JAVA_HOME` and add it to your `PATH`.

👉 GraalVM is required for building only, _not_ at runtime!

You can [download GraalVM](https://www.graalvm.org/downloads/) and install it by hand or, more simply, install it using
[SDKMan](https://sdkman.io).

### 1.2. Install GraalVM Native Image Support

After GraalVM is installed and selected, install its _native image support_ component. This is a once-in-a-lifetime 
operation:

```bash
gu install native-image
```

Command `gu` (GraalVM component updater) is located under `$GRAALVM_HOME/bin`.

### 1.2. Build a Fat Jar

Create an all-inclusive fat jar with:

```bash
sbt assembly
```

### 1.3. Build a Native Image

Next, build the native image with:

```bash
sbt graalvm-native-image:packageBin
```

This will create an executable file: `./target/graalvm-nartive-image/dssg`

### 1.4. Enjoy!

Once the native image is created, simply run it with:

```bash
dssg  <inputDir> <outputDir>
```

### 2. Building a Java Fat Jar File

If you want just Java in your installation, not Scala, run:

```bash
sbt assembly
```

This will create a fat jar: `./target/scala-3.0.2/dssg-assembly-1.0.jar`

Run with

```bash
java -jar dssg-assembly-1.0.jar dssg.Main <inputDir> <outputDir>
```

### 3. Building a Scala Jar File

Simplest build option, requires Scala in your runtime installation:

```bash
sbt package
```

This will create a regular jar: `./target/scala-3.0.2/dssg_3-1.0.jar`

Run with:

```bash
scala -classpath dssg_3-1.0.jar dssg.Main <inputDir> <outputDir>
```
