# GradleTemplateProcessor
A mustache template processor gradle plugin.\
It is using the trimou template engine and json as the data source.

For a practical example, you can take a look at my [commons library](https://github.com/Lenni0451/commons/tree/main/commons-functional/src/main/templates).

## Installation
To use this plugin, you need to add my maven server to your plugin repositories:
```groovy
pluginManagement {
    repositories {
        maven {
            name = "lenni0451 releases"
            url = "https://maven.lenni0451.net/releases"
        }
    }
}
```

After adding the repository, you can add the plugin to your project:
```groovy
plugins {
    id "net.lenni0451.template-processor" version "x.x.x"
}
```

## Configuration
The plugin provides an extension to configure the template processor:
```groovy
templateProcessor {
    templateDir = project.file("src/main/templates") //The directory where the templates are located
    outputDir = layout.buildDirectory.dir("generated/templates/main/java") //The directory where the generated files should be places
    markAsSource(project.sourceSets.main) //Mark the output directory as a source directory for the given source set
    //markAsSource(...) //Multiple source sets ca be used
}
```
If the template directory does not exist, the plugin will skip the processing.\
The output directory will be automatically created if it does not exist.

## Usage
### Template config
To use the template processor, you first need to create a config `.json` file in the template directory.\
Here is an example with all available options:
```json
{
    "parent": "parent.json",
    "template": "template.mustache",
    "target": "net/lenni0451/commons/arrays/ArrayUtils.java",
    "globals": {
        "test": true
    },
    "variants": [
        {
            "variant": "Var1"
        },
        {
            "variant": "Var2"
        }
    ],
    "variables": [
        {
            "type": "byte",
            "name": "b",
            "suffix": ""
        },
        {
            "type": "int",
            "name": "i",
            "suffix": ""
        },
        {
            "type": "long",
            "name": "l",
            "suffix": "L"
        }
    ]
}
```
| Key       | Description                                                                                              |
| --------- | -------------------------------------------------------------------------------------------------------- |
| parent    | The parent template file. If this is set, all keys from the parent will be inherited if not overwritten. |
| template  | The trimou/mustache template file.                                                                       |
| target    | The target file where the generated content should be placed.                                            |
| globals   | Global variables that can be used in the template.                                                       |
| variants  | Variables that can be used to generate multiple files.                                                   |
| variables | Variables that can be used in the template.                                                              |

The parent and template file paths are relative to the location of the config file.\
The target file path is relative to the output directory.

The target file name can contain variables from the `globals` and `variants` section.

### Template
The template file is a normal mustache file.\
Here is an example:
```mustache
public class Test{{variant}} {

    public static void main(String[] args) {
        {{#test}}
        //This is a test
        {{/test}}
        {{#each this}}
        {{type}} {{name}} = 0{{suffix}};
        {{/each}}
    }

}
```
For more information about the mustache syntax, you can visit the [official website](https://mustache.github.io/mustache.5.html).\
Trimou provides additional features that can be found in the [official documentation](http://trimou.org/doc/latest.html).

### Available helpers
The plugin provides some helper functions that can be used in the template:
| Function   | Description                                         | Usage                                                    | Output           |
| ---------- | --------------------------------------------------- | -------------------------------------------------------- | ---------------- |
| lower      | Converts the input to lower case                    | `{{lower "Test"}}`                                       | `test`           |
| upper      | Converts the input to upper case                    | `{{upper "Test"}}`                                       | `TEST`           |
| capitalize | Capitalizes the first letter of the input           | `{{capitalize "test"}}`                                  | `Test`           |
| var        | Allows to declare and use variables in the template | `{{#var "test"}}This is a test{{/var}}` `{{var "test"}}` | `This is a test` |
