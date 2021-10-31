![dssg](src/test/resources/logo.png)

# Dead-simple Site Generator

`dssg` is a minimalistic site generator imposing no directory structure or complex configuration. Its motto? _Just
convert my files and get out of the way_.

## Usage

`dssg` recursively copies over an input directory onto an output directory, such that:

- Input files with registered extensions are converted to their target representation in the output directory. For
  example, markdown files  (extension `md`) are converted to HTML (extension `html`)
- Input files with an unregistered (or no) extension are copied verbatim to the output directory. This includes empty
  directories
- Conversion commands are executed only when input files have changed more recently that any output file counterparts
- Files and directories originally present in the output directory but not in the input directory are deleted by default (unless `--no-delete` is in effect, see below)

Thus, if you have the following `input-directory` structure:

```
input-directory
├── comment.md               # Markdown
├── css
│   └── style.scss           # SASS
├── img
│   └── diagram.jpg          # No conversion
├── index.ad                 # AsciiDoc
└── summary.adoc             # More AsciiDoc
```

then the command:

```bash
dssg input-directory output-directory
```

will [re]build `output-directory` (creating it if needed) so that it contains:

```
output-dir
├── comment.html             # HTML from Markdown
├── css
│   └── style.css            # CSS from SASS
├── img
│   └── diagram.jpg          # Copied verbatim
├── index.html               # HTML from AsciiDoc
└── summary.html             # HTML from AsciiDoc
```

That's it: no mandated directory structure, no complex configuration files, no BS!

## Skipping Orphan Output File Deletion

If you want to retain output files not corresponding to any input file then pass the `--no-delete` (aka `-n`) flag 
on command invocation:

```bash
dssg --no-delete input-directory output-directory
```

## Simple Configuration

Out of the box, `dssg` supports the following formats:

| Format | Internal Command Line | Install with |
| ------ | --------------------- | ------------ |
| SASS  | `sass %i %o`  | `npm i -g sass` |
| Typescript | `npx swc -o %o %i` | `npm i -D @swc/core @swc/cli` |
| AsciiDoc | `asciidoctor -o %o %i` | `npm i -g asciidoctor` |
| Markdown | `pandoc -s -o %o %i` | Linux: `apt install pandoc` <br>Mac: `brew install pandoc` <br>Windows: `choco install pandoc` |

👉 To use `dssg` it is _**not**_ necessary to install any above dependency that you don't intend to use! Also, you may
choose converters other than the ones listed above; read on.

Say you want a different Markdown processor, a customized SASS conversion, and a new conversion of your own. Your configuration file may look like:

```
# Input extension(s)  # Output extension  # Command line template
md,markdown           html                alt-markdown %i %o
scss                  html                sass --source-map %i %o
own                   html                sh -c 'my-very-own.sh < %o > %i'
```

The configuration file syntax is:

- Empty lines and lines starting with '#' are ignored
- Each converter configuration goes on its own line
- Configuration fields are separated by one or more blanks:
  1) A comma-separated list of input extensions (no intervening spaces!)
  2) The target output extension
  3) The rest of line is the converter command template with substitutions:
     - `%i` ➜ the input file name (required)
     - `%o` ➜ the output file name (optional, depending on converter)
     - Use double percentage sign to escape literals if needed (`%%i`, `%%o`)

If the same input extension is specified than once, the last occurrence wins. This policy enables user-provided 
configuration to override built-in conversions when needed.

When specifying a configuration file, the `dssg` command line syntax is, simply:

```bash
dssg [-n | --no-delete] configuration-file input-directory output-directory
```

## More on Usage

`dssg` comes in three flavors:

- A stand-alone, native executable with no dependencies on the JVM

```bash
$ dssg ...
```

- A Java-only, fat jar requiring a 1.8+ JVM  (but not Scala) in the runtime environment

```bash
$ java -jar dssg-assembly-1.0.jar dssg.Main ...
```

- A Scala regular jar requiring both the 1.8+ JVM and 3.0+ Scala in the runtime environment

```bash
$ scala -classpath dssg_3-1.0.jar dssg.Main ...
```

## Known Issues

- Multi-part extensions are not supported. E.g. `.d.ts` in Typescript descriptor files
- Converters might generate other files in addition to the output one (e.g. `sass`'s map files). These may be deleted on
subsequent rebuilds if you don't specify `--no-delete`
- There is currently no provision to exclude input files

___

## Build Options

The brave may build this utility as:

- A fast native image (recommended)
- A Java-only fat jar file (no Scala at runtime)
- A simple Scala jar file (Scala required at runtime)

### 1. Building a native image

This is a bit more involved, but it's worth the effort as it gives you a faster, portable, native image requiring no
Java or Scala installation at runtime!

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
dssg [-n | --no-delete] [configuration-file] input-directory output-directory
```

### 2. Building a Java Fat Jar File

If you want just Java in your installation, not Scala, run:

```bash
sbt assembly
```

This will create a fat jar: `./target/scala-3.0.2/dssg-assembly-1.0.jar`

Run with

```bash
java -jar dssg-assembly-1.0.jar dssg.Main [-n | --no-delete] [configuration-file] input-directory output-directory
```

### 3. Building a Scala Jar File

Simplest build option, requires Scala in your runtime installation:

```bash
sbt package
```

This will create a regular jar: `./target/scala-3.0.2/dssg_3-1.0.jar`

Run with:

```bash
scala -classpath dssg_3-1.0.jar dssg.Main [-n | --no-delete] [configuration-file] input-directory output-directory
```
