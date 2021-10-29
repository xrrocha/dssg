# Dead-simple Site Generator

Motto: _Just convert my files and shut the f..k up!_

## Usage

`dssg` recursively copies over an input directory onto an output directory, such that:

- Input files with registered extensions are converted to their target representation in the output directory. For 
  example, markdown files  (extension `md`) are converted to HTML (extension `html`)
- Input files with unregistered (or no) extensions are copied verbatim to the output directory. This includes 
  empty directories
- Files and directories originally present in the output directory but not in the input directory are deleted
- Conversion commands are executed only when input files have changed more recently that any output file counterparts

Thus, if you have the following `input-directory` structure:

```
input-directory
├── comment.md # Look ma: Markdown
├── css
│   └── style.scss # Look ma: SASS
├── img
│   └── photo.jpg
├── index.ad # Look ma: AsciiDoc
└── summary.adoc # Look ma: More AsciiDoc
```

then the command:

```bash
dssg input-directory/ output-dir/
```

will rebuild directory `output-directory` (creating it if needed) so that it contains:

```
output-dir
├── comment.html # Look ma: HTML from Markdown
├── css
│   └── style.css # Look ma: CSS from SASS
├── img
│   └── photo.jpg # Look ma: copied verbatim!
├── index.html # Look ma: HTML from AsciiDoc
└── summary.html # Look ma: HTML from AsciiDoc
```

That's it: no mandated directory structure, no complex configuration files, no BS!

The out-of-the-box configuration supports:

- SASS (npm `sass`)
- AsciiDoc (`asciidoctor`)
- Markdown (Linux `markdown`)

To support other conversions some simple configuration is needed as shown next.

## Simple Configuration

Say you just want to convert Pug and AsciiDoc files. Your configuration file would look like:

```
# File: config.txt
# - Empty lines are ignored
# - Lines starting with '#' are comments
# - Only one configuration per line
# - Fields separated by one or more blanks
# - Field contents:
#   1) Comma-separated list of input extensions (no intervening spaces)
#   2) Output extension
#   3) Rest of Line: converter command line invocation
#      Substitutions:
#      %i -> input file name  (required)
#      %o -> output file name (optional, depending on actual command)
#      Use double percentage sign to escape literal if needed (%%i, %%o)

# Now the real meat!
# Input extension(s)  # Output extension  # Command line template
pug                   html                bash -c 'pug < %i > %o'
ad,adoc               html                asciidoctor -o %o %i
```

Given this configuration file, the command line incantation would be:

```bash
dssg config.txt input-directory output-directory
```

## Release Files

For your convenience, directory `releases` contains ready-made executables:

- Scala jar
- Java fat jar
- Native executable (for `amd64/linux` only)

## Build Options

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
