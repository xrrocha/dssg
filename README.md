![dssg](src/test/resources/logo.png)

# Dead-simple Site Generator

`dssg` is a minimalistic site generator imposing no directory structure or complex configuration.

The mantra here is _«just convert my files and get out of the way»_

## Usage

`dssg` recursively copies over an input directory onto an output directory, such that:

- Input files with an unregistered (or no) extension are copied verbatim
- Input files with registered extensions are converted to their target representation
- Conversion occurs only if input files are more recent than their output counterparts
- Orphan files present in the output directory but not in the input directory are deleted

Thus, if you have the following `input-directory` structure:

```
input-directory
├── comment.md               # Markdown
├── css
│   └── style.scss           # SASS
├── img
│   └── diagram.jpg          # No conversion
└── index.ad                 # AsciiDoc
```

then the command:

```bash
$ dssg input-directory output-directory
```

will build `output-directory` so that it contains:

```
output-dir
├── comment.html             # HTML from Markdown
├── css
│   └── style.css            # CSS from SASS
├── img
│   └── diagram.jpg          # Copied verbatim
└── index.html               # HTML from AsciiDoc
```

That's it: no mandated directory structure, no complex configuration files, no BS!

## Simple Configuration

Out of the box, without any configuration, `dssg` supports the following formats:

| Format | Install with |
| ------ | --------------------- |
| AsciiDoc | `npm i -g asciidoctor` |
| Markdown | Linux:       `apt install pandoc` <br>Mac:         `brew install pandoc` <br>Windows: `choco install pandoc` |
| Pug | `npm i -g pug` |
| SASS  | `npm i -g sass` |
| Typescript | `npm i -D @swc/core @swc/cli` |

👉 To use `dssg` it is _**not**_ necessary to install any above dependency that you don't intend to use! Also, you may
choose converters other than the ones listed above; read on.

The built-in `configuration.txt` resource file provisioning the above formats contains:

```
# Input extension(s)  # Output extension  # Command line template
ad,adoc               html                asciidoctor --out-file %o %i
md                    html                pandoc --standalone --output %o %i
pug                   html                sh -c 'pug < %i > %o'
scss                  css                 sass --no-source-map %i %o
ts                    js                  npx swc --out-file %o %i
```

Each converter configuration goes on its own line. Empty lines and lines starting with `#` are ignored

Configuration fields (separated by one or more blanks) are:

1. A comma-separated list of input extensions (no intervening spaces!)
2. The target output extension
3. The rest of line is the converter command template with placeholders:
   - `%i` ➜ the input file name
   - `%o` ➜ the output file name

👉 If the same input extension is specified more than once, the last occurrence wins. This policy enables 
user-provided configuration to override built-in conversions when needed.

You can easily customize these defaults! 

Say you want to use a Pandoc Markdown template called _stencil_ and a new conversion type of your own. Your 
configuration file may look like:

```
# Input extension(s)  # Output extension  # Command line template
md,markdown           html                pandoc --template=stencil -o %o %i
mine                  html                sh -c 'my-own-converter.sh < %i > %o'
```

When specifying a configuration file, the `dssg` command line syntax is, simply:

```bash
$ dssg configuration-file input-directory output-directory
```

## More on Usage

`dssg` comes in three flavors:

- A stand-alone, native executable with no dependencies on the JVM

```bash
$ dssg ...
```

- A Java-only, fat jar requiring a 1.8+ JVM  (but not Scala) in the runtime environment

```bash
$ java -jar dssg-assembly-0.1.0.jar dssg.Main ...
```

- A Scala regular jar requiring both the 1.8+ JVM and 3.0+ Scala in the runtime environment

```bash
$ scala -classpath dssg_3-0.1.0.jar dssg.Main ...
```

## Known Issues

- There's no provision to exclude input files
- There's no provision not to delete orphan output files
- Multi-part extensions are not supported. E.g. `.d.ts` in Typescript descriptor files
- Converters may generate additional files (e.g. `sass`'s sourcemap files.) These files may be deleted on subsequent 
  builds. _This is being addressed_.
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
$ gu install native-image
```

Command `gu` (GraalVM component updater) is located under `$GRAALVM_HOME/bin`.

### 1.2. Build a Fat Jar

Create an all-inclusive fat jar with:

```bash
$ sbt assembly
```

### 1.3. Build a Native Image

👉 This step depends on GraalVM configuration files located under `./configs`

Next, build the native image with:

```bash
$ sbt graalvm-native-image:packageBin
```

This will create an executable file: `./target/graalvm-nartive-image/dssg`

### 1.4. Enjoy!

Once the native image is created, simply run it with:

```bash
$ dssg [configuration-file] input-directory output-directory
```

### 2. Building a Java Fat Jar File

If you want just Java in your installation, not Scala, run:

```bash
$ sbt assembly
```

This will create a fat jar: `./target/scala-3.0.2/dssg-assembly-0.1.0.jar`

Run with

```bash
$ java -jar dssg-assembly-0.1.0.jar dssg.Main [configuration-file] input-directory output-directory
```

### 3. Building a Scala Jar File

Simplest build option, requires Scala in your runtime installation:

```bash
sbt package
```

This will create a regular jar: `./target/scala-3.0.2/dssg_3-0.1.0.jar`

Run with:

```bash
$ scala -classpath dssg_3-0.1.0.jar dssg.Main [configuration-file] input-directory output-directory
```
