# ZPA Forms Extractor

[![Build](https://github.com/felipebz/zpa-forms-extractor/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/felipebz/zpa-forms-extractor/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarqube.felipebz.com/api/project_badges/measure?project=com.felipebz.zpa%3Azpa-forms-extractor&metric=alert_status)](https://sonarqube.felipebz.com/dashboard?id=com.felipebz.zpa%3Azpa-forms-extractor)

Extracts the PL/SQL code and metadata of Oracle Forms modules (`.fmb`, `.mmb`, `.pll`, `.olb`) into plain files that [ZPA](https://zpa.felipebz.com) and other tools can analyze.

Forms keeps its code inside binary modules, out of reach of linters and static analyzers. This tool opens each module through Oracle's Forms API (JDAPI). It writes every trigger, program unit, menu item and record group query to its own file, as compilable PL/SQL. It also writes a `metadata.json` that describes the form's blocks, items, alerts, LOVs and parameters. With it, ZPA can resolve references such as `:BLOCK.ITEM`.

## Example

```console
$ zpa-forms-extractor --input forms/ --output extracted/
forms/demo/timeout.fmb -> extracted/demo/timeout.fmb
forms/demo/webutil.olb -> extracted/demo/webutil.olb
forms/demo/webutil.pll -> extracted/demo/webutil.pll
forms/demo0007/cd_demo_menu.mmb -> extracted/demo0007/cd_demo_menu.mmb
```

```console
$ find extracted/demo/timeout.fmb -type f | sort
extracted/demo/timeout.fmb/blocks/TEST/DEBUG/WHEN-CHECKBOX-CHANGED.tgg
extracted/demo/timeout.fmb/blocks/TEST/PRESET1/WHEN-BUTTON-PRESSED.tgg
…
extracted/demo/timeout.fmb/metadata.json
extracted/demo/timeout.fmb/program-units/PL_START.pcd
extracted/demo/timeout.fmb/triggers/ON-LOGON.tgg
extracted/demo/timeout.fmb/triggers/WHEN-TIMER-EXPIRED.tgg

$ cat extracted/demo/timeout.fmb/triggers/ON-LOGON.tgg
BEGIN
null;
END;
```

## Requirements

- An **Oracle Forms Builder** installation on the machine that runs the extractor. Both the JDAPI jar (`frmjdapi.jar`) and the native Forms libraries come from it. Tested with Oracle Forms 14.1.2 on Linux.
- **Java 8 or newer.** The JDK bundled with Forms (`$ORACLE_HOME/oracle_common/jdk`) works.

## Installation

1. Download `zpa-forms-extractor-<version>.zip` from the [releases page](https://github.com/felipebz/zpa-forms-extractor/releases) and unzip it.
2. Copy `frmjdapi.jar` from your Forms installation into the distribution's `libs` folder. The jar is proprietary, so it is not bundled.

   ```sh
   cp "$ORACLE_HOME/jlib/frmjdapi.jar" zpa-forms-extractor-<version>/libs/
   ```

3. Make the native Forms libraries reachable.

   | OS | Setting |
   |---|---|
   | Linux | `export ORACLE_HOME=/path/to/oracle LD_LIBRARY_PATH=$ORACLE_HOME/lib` |
   | Windows | Add the Forms `bin` folder to `PATH`, e.g. `C:\Oracle\Middleware\Oracle_Home\bin` |

4. Set the character set (see [Character set](#character-set)). On Linux:

   ```sh
   export NLS_LANG=AMERICAN_AMERICA.WE8ISO8859P1 JAVA_OPTS=-Dfile.encoding=ISO-8859-1
   ```

5. Check the installation:

   ```sh
   zpa-forms-extractor-<version>/bin/zpa-forms-extractor --help
   ```

## Usage

```text
Usage: zpa-forms-extractor [OPTIONS]

Options:
  --input PATH                     Oracle Forms module (.fmb, .mmb, .pll,
                                   .olb) or directory searched recursively;
                                   repeatable
  --output DIRECTORY               output directory
  --metadata-format [full|legacy]  metadata.json format: full (default) or
                                   legacy, for ZPA 4.2 and older (no
                                   parameters, item names only)
  -h, --help                       Show this message and exit
```

- **Inputs:** `--input` takes a module or a directory and can be repeated. Directories are searched recursively for `.fmb`, `.mmb`, `.pll` and `.olb` files.
- **One folder per module:**
  - A module given directly is written to a folder named after its file: `--input forms/orders.fmb` → `<output>/orders.fmb/`.
  - A module found in a directory keeps its path relative to that directory: `--input forms` → `<output>/sub/orders.fmb/` for `forms/sub/orders.fmb`.

  Keeping the extension in the folder name means `webutil.pll` and `webutil.olb` don't collide.
- **Failures:** a module that can't be opened or written is reported on stderr and skipped. The other modules are still extracted, and the exit code is 1.
- **Existing output:** files in `--output` are overwritten but never deleted. Clear the folder before re-extracting a module that has lost code.

## Output

```text
<module>/
  metadata.json                                  forms only
  startup-code.tgg                               menu modules
  program-units/<NAME>.pks|pkb|pcd|fun
  triggers/<TRIGGER>.tgg                         module-level triggers
  blocks/<BLOCK>/<TRIGGER>.tgg                   block triggers
  blocks/<BLOCK>/<ITEM>/<TRIGGER>.tgg            item triggers
  blocks/<BLOCK>/<ITEM>/FORMULA-CALCULATION.tgg  item formula
  items/<ITEM>/…                                 Object Library items outside a block
  property-classes/<CLASS>/<TRIGGER>.tgg
  events/<EVENT>/<TRIGGER>.tgg
  record-groups/<GROUP>.sql                      record group queries
  menus/<MENU>/<MENU_ITEM>.tgg
  menus/<MENU_ITEM>.tgg                          Object Library menu items outside a menu
```

- **Anonymous blocks:** triggers, menu item code and startup code are wrapped in `BEGIN … END;`, so every `.tgg` file is a complete anonymous block.
- **Formulas:** an item formula becomes an assignment, for example `BEGIN :BLK.TOTAL := :BLK.PRICE * :BLK.QTY; END;`.
- **Program units:** written unchanged; the extension follows the unit type.
- **File names:** names that Windows reserves (`CON`, `AUX`, `NUL`…) get a trailing `_`.

### metadata.json

```json
{
  "alerts": ["WEBUTIL_ERROR"],
  "blocks": [
    {
      "name": "TEST",
      "items": [
        { "name": "MAXINACTIVITY", "dataType": "CHAR", "maxLength": 30 },
        { "name": "SLEEPTIME", "dataType": "CHAR", "maxLength": 30 }
      ]
    },
    { "name": "WEBUTIL", "items": [{ "name": "DUMMY" }] }
  ],
  "lovs": [],
  "parameters": [
    { "name": "P_INACTIVITY", "dataType": "NUMBER" },
    { "name": "P_CL_ID", "dataType": "CHAR" }
  ]
}
```

Items that hold no value, such as push buttons, have no data type. Parameters have no `maxLength`, because JDAPI doesn't expose it.

ZPA 4.2 and older reject the `dataType`, `maxLength` and `parameters` fields. For those versions, pass `--metadata-format legacy`. It writes only the alerts, the block and item names, and the LOVs.

## Using the output with ZPA

Analyze a module folder with ZPA and pass its `metadata.json` as the Forms metadata. The [ZPA website](https://zpa.felipebz.com/) covers the ways to run ZPA (SonarQube plugin, command line) and their configuration. With [zpa-cli](https://github.com/felipebz/zpa-cli):

```sh
zpa-cli --sources extracted/demo/timeout.fmb \
        --forms-metadata extracted/demo/timeout.fmb/metadata.json
```

zpa-cli reads `.tgg` and `.sql` files by default, together with the usual PL/SQL extensions.

## Character set

Oracle converts the module text to the character set in `NLS_LANG`. On Linux, when `NLS_LANG` is not set, Oracle uses ASCII and silently drops accents (`ü` becomes `u`, `§` becomes `?`). Set both variables, and match the character set to your modules:

```sh
export NLS_LANG=AMERICAN_AMERICA.WE8ISO8859P1    # character set of the modules
export JAVA_OPTS=-Dfile.encoding=ISO-8859-1      # Java must decode JDAPI's text the same way
```

The output files are always written in UTF-8.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `NoClassDefFoundError: oracle/forms/jdapi/JdapiException` | `frmjdapi.jar` missing | Copy it into the distribution's `libs` folder. |
| `UnsatisfiedLinkError: no frmjapi in java.library.path`, or a missing library such as `libXm.so.4` | Native Forms libraries not found | Set `ORACLE_HOME` and `LD_LIBRARY_PATH` (Linux) or `PATH` (Windows). |
| `_jni_initialize: Failed to create new forms context` | `NLS_LANG` names a character set this Forms installation rejects (seen with `UTF8`, `AL32UTF8` and `WE8MSWIN1252`) | Use a single-byte character set such as `WE8ISO8859P1`. |
| Accents missing, or shown as `?` or `�` | `NLS_LANG` or `file.encoding` not set | See [Character set](#character-set). |
| ZPA logs `Error reading the metadata file` | ZPA 4.2 or older reading the `full` format | Re-extract with `--metadata-format legacy`. |

## Building from source

Building needs the JDAPI jar and JDK 25 to run Gradle. The project targets Java 8; Gradle uses a local JDK 8 or downloads one.

```sh
cp "$ORACLE_HOME/jlib/frmjdapi.jar" libs/
./gradlew distZip          # build/distributions/zpa-forms-extractor-<version>.zip
./gradlew build -x test    # compile, license headers and packaging, without tests
./gradlew build            # also runs the tests: needs the native Forms libraries (see Installation)
```

## Running the tests

Most unit tests depend on Oracle Forms, so they can't run on a plain JDK:

- **Needs Oracle Forms:** `FormsMetadataTest` and `SourceExtractorTest` build Forms modules in memory through JDAPI. They need `libs/frmjdapi.jar` and the native Forms libraries.
- **Pure Kotlin:** `SourceWriterTest` and `ModuleInputsTest` don't use JDAPI.

Run them on a machine with Oracle Forms installed, with the variables from [Installation](#installation) set in the same shell:

```sh
export ORACLE_HOME=/path/to/oracle LD_LIBRARY_PATH=$ORACLE_HOME/lib
./gradlew test --no-daemon
```

Use `--no-daemon`. A Gradle daemon started earlier from another shell doesn't have these variables, and the tests fail loading the native libraries (for example `libXm.so.4`).

For the same reason, CI builds with `./gradlew build -x test` and doesn't run the tests. Its SonarQube analysis therefore has no coverage data.

Some behavior can't be tested this way at all: without a database, JDAPI drops record group queries from modules created in memory. Changes to record groups, or to the output layout in general, should also be checked by extracting real modules, such as the [DOAG Forms demos](https://github.com/Doag/Forms).

## License

[Apache License 2.0](LICENSE)
