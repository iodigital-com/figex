# FigEx - Figma Exporter
FigEx is a utility tool to export styles and icons from Figma using the Figma REST API.

Features:
- Support for modes and variables in Figma
- Code generation using Jinja2 templating syntax for any code language
- Export of icons as SVG, PDF, PNG, WEBP or Android XML vectors
- Simple configuration with many options

`config.json` is a simple configuration, telling FigEx what to put where:
```
{
  "type": "values",
  "templatePath": "../samples/AndroidValues.xml.figex",
  "destinationPath": "~/Downloads/AndroidValuesModeB.xml",
  "defaultMode": "modeB",
  "templateVariables": {
    "templateVarDemo": "\uD83D\uDE80\uD83D\uDE80\uD83D\uDE80"
  }
},
{
  "type": "icons",
  "format": "androidxml",
  "filter": "{% if fullName|startsWith('icon-', true) %} true {% else %} false {% endif %}",
  "fileNames" : "{{ fullName|replaceSpecialChars('_')|lowercase }}",
  "destinationPath": "~/Downloads/icons",
  "clearDestination": true
  }
```

You can defined template files with Jinja-tokens to generate source code files in any language:
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    {{ }}
    {%- for color in colors -%}
    <color name="{{ color.name.snake }}">#{{ color.argb }}</color>
    {% endfor %}
</resources>
```

## Getting started

See the files in the `samples` for a few examples! You can download the `samples` folder to get
started. Go to the Figma [Variables playground example](https://www.figma.com/community/file/1234936397107899445) and select "Open in Figma".
The file will open in your workspace and the URL will look like this:

```
https://www.figma.com/design/{{figmaFileKey}}/Variables-playground-(Community)?node-id=41-11&t=SPTJno70ETNtkk5D-0
```

Copy the `{{figmaFileKey}}` section and replace the file key in `samples/config.json`. Now select your profile picture in the top
left of Figma and select "Settings", then scroll down to "Personal access tokens" and create a new one. Now run figex.
This will create a `samples_output` folder next to the `samples` folder. Enjoy!

### Option A: Using the gradle plugin
FigEx can be used as part of your gradle build system. Add the plugin to the root build.gradle.kts` file:

```kotlin
plugins {
    id("com.iodigital.figex") version "{latest-version}"
}

figex {
  figmaToken = "{Figma Token}"
  configFile = file("path/to/config.json")
}
```

Now you can run `./gradlew exportFigma` to export all your Figma resources!


### Option B: Using shell
FigEx can be run standalone from the shell.

1. Make sure Java is installed on your machine, run `java --version` to confirm
2. Download a `figex.zip` from the [release list](https://github.com/iodigital-com/figex/releases)
3. Extract the zip file and place it in your system
4. Create a config file for your project, see the `sample/config.json`
6. Create a Figma personal access token

#### macOS / Linux
```shell
 export FIGMA_TOKEN="Figma token"
 figex -c "path to your config"
```

#### Windows
```shell
 set FIGMA_TOKEN="your token"
 java -jar "path to figex jar" -c "path to your config"
```

### Option C: Using the core library
You can make use of the FigEx core library in any Java/Kotlin project by including it in your `build.gradle` file:

```kotlin
repositories {
    mavenCentral()
}

implementation("com.iodigital:figex-core:{latest-version}")
```

In your code you can now use FigEx:

```kotlin
FigEx.exportBlocking(
    configFile = "path/to/config.json",
    figmaToken = "{Figma Token}",
)
```


## Setup in Figma
Nothing to do here really! All you need is the file key from the URL. You can then use this in the configuration (see below).

We use FigEx to export a "style library" file which is not containing the actual visual designs but only colors, text styles, icons and dimensions. The design files then reference this style library.
This is a small sample file: https://www.figma.com/design/0VIabis5OosbFC3Q1tYXnT


## Modes in Figma
Modes in Figma allow you to have multiple values for the same variable. This can be used for light and dark modes, different brand styles or languages. 
FigEx does see the modes you configured in Figma, but not the names you gave these modes. Initially, the modes will show up as their id, e.g. `834:0`.
In the logs, FigEx will list modes that you did not give a name to yet and also some sample values. Using the values, you can identify the modes and then
add a `modeAliases` entry in the `config.json` file. Afterwards, FigEx will represent the modes by their alias instead of their id.

## Config file

See the example config in the `samples` directory.

- `figmaFileKey`: The key for the figma file. You can obtain it from any Figma URL, the section in `>>` and `<<` is the key: `figma.com/file/>>dqsg8P1c2ayjNJPyPYmv4X<<`
- `modeAliases`: Aliases for the modes. FigEx sees modes only as their IDs, e.g. `8124:0` and `8124:1`. You can defined aliases here for your convenience. There is no way to look up the name for a mode from the free Figma API, so you need to figure out what is what.
- `exports`: Defines the exports to be done. There are two kinds of exports:
  - `"type": "values"` is used to export any values like colors, dimensions or text styles
    - `templatePath`: The path to the Jinja2 template. See `samples/AndroidValues.xml.figex` for an example and see below for more details
    - `destinationPath`: The path to where the generated file should be written
    - `defaultMode`: The default mode to be used for the values. If the `defaultMode` is e.g. `test` then `color.test.argb` is the same as `color.argb`
    - `templateVariables`: A map of extra variables for the template. If you define `test` here you can later use `{{ test }}` in your template file
    - `filter`: A template that should read `true` to include a value in the export
  - `"type": "icons"` is used to export icons and illustrations
    - `format`: One of `svg`, `pdf`, `png`, `webp` or `androidxml`
    - `filter`: A template that should read `true` to include a component in the export
    - `fileNames`: A template defining the file name of the exported component. A `/` will cause a
      directory to be created
    - `destinationPath`: The directory to which the files should be written
    - `clearDestination`: If `true`, all files in the destination directory will be deleted before exporting the icons
    - `rasterScales`: A list of scale objects defining the sizes for raster graphics exports (`png`
      and `webp`)
      - `scale`: A float defining the scale, `1` being original size
      - `nameSuffix`: A string that is appended to the name generated from `fileNames`, comes before
        the file suffix
      - `namePrefix`: A string tha tis prepended to the name generated from `fileNames`. A `/` will
        cause a directory to be created.
    - `useAndroidRasterScales`: A shorthand to create `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`
      exports of raster graphics. Ignored if `rasterScales` is defined
    - `companionFileName`  The path to where the generated companion file should be written
    - `companionFileTemplatePath` The path to the Jinja2 template. See `samples/Contents.json.figex` for an example and see below for more details
      Ignored if `useXcodeAssetCompanionFile` is true
    - `useXcodeAssetCompanionFile`: A shorthand to create xcode assets `Contents.json` companion files, using the default template.

## Templating
The templating engine uses Jinja syntax. You can use loops, if statements and more. FigEx's templating is build with [jinjava](https://github.com/HubSpot/jinjava) which is also the base of HubSpot's [HubL templating system](https://developers.hubspot.com/docs/cms/hubl). This means the syntax for if-statements and loops also applies to FigEx, same goes for the filters available. Of course, HubSpot specific variables and functions are not available.

### Templating for icon exports

This templating is used in the `filter` and `fileNames` configurations.

- `figma`: A `Figma` object
- `date`: The current date
- `full_name`: The full name of the component. If part of a component set, comprised of the name of
  the component set and the component name. The component name otherwise.
- `name`: The name of the component
- `normalized_name`: A name object for the `full_name`
- `key`: The key of the component
- `id`: The id of the component
- `set_name`: The name of the set of which this component is a part of, empty if not part of a set
- `set_key`: The ket of the set of which this component is a part of, empty if not part of a set
- `set_id`: The id of the set of which this component is a part of, empty if not part of a set
- `scale`: A scale object representing the current scale for the export

### Templating for companion file export

This templating is used in the file at the `companionFileTemplatePath` configuration, and expands upon the `Templating for icon exports`

- `file_name`: The full filename passed in the file at the `companionFileName`
- `file_name_relative`: the relative file name passed in the file at `companionFileName`

### Templating for values export

This templating is used in the file at the `templatePath` configuration.

- `colors`: A list of `Color` objects
- `floats`: A list of `Float` objects
- `strings`: A list of `String` objects
- `icons`: A list of `Icon` objects as for the icon export. Useful to generate code accessors to the icons.
- `booleans`: A list of `Boolean` objects
- `text_styles`: A list of `TextStyle` objects
- `figma`: A `Figma` object
- `date`: The current date

### Templating objects

#### Color
- `name`: A name object
- `a`, `r`, `g`, `b`: The alpha, red, green and blue value 0..1 (for the default mode)
- `a255`, `r255`, `g255`, `b255`: The alpha, red, green and blue value 0..255 (for the default mode)
- `argb`: The argb hex string (for the default mode)
- `modeA`: A nested `Color` object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested `Color` object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modes`: A list of nested `Color` objects for each mode in which the `name` field represents the name of the mode, not the color

#### Float
- `name`: A name object
- `value`: The value (for the default mode)
- `modeA`: A nested `Float` object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested `Float` object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modes`: A list of nested `Float` objects for each mode in which the `name` field represents the name of the mode, not the float
- 
#### String
- `name`: A name object
- `value`: A string
- `modeA`: A nested `String` object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested `String` object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modes`: A list of nested `String` objects for each mode in which the `name` field represents the name of the mode, not the string

#### Boolean
- `name`: A name object
- `value`: A boolean string
- `modeA`: A nested `Boolean` object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested `Boolean` object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modes`: A list of nested `Boolean` objects for each mode in which the `name` field represents the name of the mode, not the boolean
- 
#### TextStyle
- `name`: A name object
- `font_family`: As defined in Figma (for the default mode)
- `font_style`: As defined in Figma (for the default mode) (note: might not be consistent with `font_weight`)
- `font_size`: As defined in Figma (for the default mode)
- `font_weight`: As defined in Figma (for the default mode) (note: might not be consistent with `font_style`)
- `letter_spacing`: As defined in Figma (for the default mode)
- `line_height_percent`: As defined in Figma (for the default mode)
- `line_height_percent_font_size`: As defined in Figma (for the default mode)
- `line_height_px`: As defined in Figma (for the default mode)
- `line_height_unit`: As defined in Figma (for the default mode)
- `text_align_horizontal`: As defined in Figma (for the default mode)
- `text_align_vertical`: As defined in Figma (for the default mode)
- `text_auto_resize`: As defined in Figma (for the default mode)
- `modeA`: A nested `TextStyle` object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested `TextStyle` object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modes`: A list of nested `TextStyle` objects for each mode in which the `name` field represents the name of the mode, not the style

#### Name
Hint: You can also use Jinja filters to modify the name, e.g. `{{ color.name|lowercase|replace("some", "other") }}`

- `original`: The name as defined in Figma
- `snake`: The name in snake case
- `kebab`: The name in kebab case
- `pascal`: The name in pascal case
- `camel`: The name in camel case

#### Figma
- `file`: The Figma file name
- `last_modified`: Last modified date of the Figma file
- `version`: The version of the Figma file
- `file_key`: The file key from config
- `file_url`: The URL of the file to open in the browser

#### Scale
- `scale`: The scale as floating point number as configured
- `name_prefix`: The prefix of the filename as configured
- `name_suffix`: The suffix of the filename as configured

## Build the project
- Clone the Git
- To test, open in Android Studio and
  - Create a `.figmatoken` file containing your token in `figex-cli/.figmatoken`
  - Run the `Run sample` configuration
- `./gradlew clean build` will build the project and create files in `figma-exported/build/distributions`
