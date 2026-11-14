## ZPA Forms Extractor

This extractor uses the Oracle Forms JDAPI library to open the Forms modules, therefore an installation of Oracle Forms Builder is required.

# How to build

* Find the JDAPI jar in your Oracle Forms installation (`f60jdapi.jar`, `f90jdapi.jar` or `frmjdapi.jar`) and copy it to the `libs` directory.
* Run `.\gradlew build`. The output will be generated in `build\distributions\zpa-forms-extractor.zip`.

# How to use

To use this application, the native Oracle Forms libraries must be reachable:

* Windows: the PATH environment variable must contain the Oracle Forms Builder path, for example: `PATH=C:/Oracle/Middleware/Oracle_Home/bin;(...)`.
* Linux: set `ORACLE_HOME` and add `$ORACLE_HOME/lib` to `LD_LIBRARY_PATH`.

Copy the JDAPI jar from your Oracle Forms installation (`f60jdapi.jar`, `f90jdapi.jar` or `frmjdapi.jar`) to the `libs` directory of the distribution (next to `bin` and `lib`). The launcher scripts add every jar in `libs` to the classpath.

```
Usage: zpa-forms-extractor [OPTIONS]

Options:
  --input PATH        Oracle Forms module (.fmb, .mmb, .pll, .olb) or
                      directory searched recursively; repeatable
  --output DIRECTORY  output directory
  -h, --help          Show this message and exit
```

Each module is extracted to its own folder below `--output`. A module given directly uses its file name (`--input forms/orders.fmb` → `orders.fmb/`). A module found in a directory keeps its path relative to that directory (`--input forms` finds `forms/sub/orders.fmb` → `sub/orders.fmb/`). A module that fails to extract is reported and skipped, the remaining modules are still extracted, and the exit code is 1. Existing files in `--output` are overwritten but never deleted.

```
<module>/
  metadata.json                                 forms only: alerts, blocks/items (data type, max length), LOVs, parameters
  startup-code.tgg                              menu modules
  program-units/<NAME>.pks|pkb|pcd|fun
  triggers/<TRIGGER>.tgg                        module-level triggers
  blocks/<BLOCK>/<TRIGGER>.tgg                  block triggers
  blocks/<BLOCK>/<ITEM>/<TRIGGER>.tgg           item triggers
  blocks/<BLOCK>/<ITEM>/FORMULA-CALCULATION.tgg item formula, as an assignment
  items/<ITEM>/...                              Object Library items outside a block
  property-classes/<PC>/<TRIGGER>.tgg
  events/<EVENT>/<TRIGGER>.tgg
  record-groups/<RG>.sql
  menus/<MENU>/<MENU_ITEM>.tgg
  menus/<MENU_ITEM>.tgg                         Object Library menu items outside a menu
```

Triggers, menu item code and startup code are wrapped in `BEGIN … END;`, so every `.tgg` file is a complete anonymous block.
