# FigEx
FigEx is a utility tool to export styles and icons from Figma using the Figma REST API.

Features:
- Support for modes and variables in Figma
- Code generation using Jinja2 templating syntax for any code language
- Export of icons as SVG, PDF or Android XML vectors
- Simple configuration

## Installation

1. Download a release from the release list
2. Extract the zip file and place it in your system
3. Add the extracted directory to your system's `$PATH` (macOS)

## How to use (macOS)
1. Create a config file for your project, see the `sample/config.json`
2. Create a Figma personal access token
3. Run ` export FIGMA_TOKEN="your token"`
4. Run `figex -c "path to your config"`

## How to use (Windows)

1. Create a config file for your project, see the `sample/config.json`
2. Create a Figma personal access token
3. Run `set FIGMA_TOKEN="your token"`
4. Run `java -jar "path to figex jar" -c "path to your config"`

## Config file

- `figmaFileKey`: The key for the figma file. You can obtain it from any Figma URL, the section in `>>` and `<<` is the key: `figma.com/file/>>dqsg8P1c2ayjNJPyPYmv4X<<`
- `modeAliases`: Aliases for the modes. FigEx sees modes only as there IDs, e.g. `8124:0` and `8124:1`. You can defined aliases here for your convenience. There is no way to look up the name for a mode from the free Figma API, so you need to figure out what is what.
- `exports`: Defines the exports to be done. There are two kinds of exports:
  - `"type": "values"` is used to export any values like colors, dimensions or text styles
    - `templatePath`: The path to the Jinja2 template. See `samples/AndroidValues.xml.figex` for an example and see below for more details
    - `destinationPath`: The path to where the generated file should be written
    - `defaultMode`: The default mode to be used for the values. If the `defaultMode` is e.g. `test` then `color.test.argb` is the same as `color.argb`
    - `templateVariables`: A map of extra variables for the template. If you define `test` here you can later use `{{ test }}` in your template file
  - `"type": "icons"` is used to export icons and illustrations
    - `format`: One of `svg`, `pdf`, `png`, `webp` or `androidxml`
    - `filter`: A template that should read `true` to include a component in the export
    - `fileNames`: A template defining the file name of the exported component. A `/` will cause a
      directory to be created
    - `destinationPath`: The directory to which the files should be written
    - `clearDestination`: If true, all files in the destination directory will be deleted before exporting the icons
    - `rasterScales`: A list of scale objects defining the sizes for raster graphics exports (`png`
      and `webp`])
      - `scale`: A float defining the scale, `1` being original size
      - `nameSuffix`: A string that is appended to the name generated from `fileNames`, comes before
        the file suffix
      - `namePrefix`: A string tha tis prepended to the name generated from `fileNames`. A `/` will
        cause a directory to be created.
    - `useAndroidRasterScales`: A shorthand to create `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`
      exports of raster graphics. Ignored if `rasterScales` is defined

## Templating
The templating engine uses Jinja2 syntax. You can use loops, if statements and more.

### Templating for icon exports

This templating is used in the `filter` and `fileNames` configurations.

- `figma`: A figma file object
- `date`: The current date
- `fullName`: The full name of the component. If part of a component set, comprised of the name of
  the component set and the component name. The component name otherwise.
- `name`: The name of the component
- `key`: The key of the component
- `id`: The id of the component
- `setName`: The name of the set of which this component is a part of, empty if not part of a set
- `setKey`: The ket of the set of which this component is a part of, empty if not part of a set
- `setId`: The id of the set of which this component is a part of, empty if not part of a set
- `scale`: A scale object representing the current scale for the export

### Templating for values export

This templating is used in the file at the `templatePath` configuration.

- `colors`: A list of Color objects
- `dimens`: A list of dimension objects
- `text_styles`: A list of text style objects
- `figma`: A figma file object
- `date`: The current date

### Templating objects

#### Color
- `name`: A name object
- `a`, `r`, `g`, `b`: The alpha, red, green and blue value 0..1 (for the default mode)
- `a255`, `r255`, `g255`, `b255`: The alpha, red, green and blue value 0..255 (for the default mode)
- `argb`: The argb hex string (for the default mode)
- `modeA`: A nested color object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested color object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config

#### Dimension
- `name`: A name object
- `value`: The value (for the default mode)
- `modeA`: A nested color object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested color object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config

#### Text style
- `name`: A name object
- `font_family`: As defined in Figma (for the default mode)
- `font_post_script_name`: As defined in Figma (for the default mode)
- `font_size`: As defined in Figma (for the default mode)
- `font_weight`: As defined in Figma (for the default mode)
- `letter_spacing`: As defined in Figma (for the default mode)
- `line_height_percent`: As defined in Figma (for the default mode)
- `line_height_percent_font_size`: As defined in Figma (for the default mode)
- `line_height_px`: As defined in Figma (for the default mode)
- `line_height_unit`: As defined in Figma (for the default mode)
- `text_align_horizontal`: As defined in Figma (for the default mode)
- `text_align_vertical`: As defined in Figma (for the default mode)
- `text_auto_resize`: As defined in Figma (for the default mode)
- `modeA`: A nested color object for `modeA`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config
- `modeB`: A nested color object for `modeB`, contains the same values as above. The name depends on your modes and the `modeAliases` in the config

#### Name
Hint: You can also use Jinja filters to modify the name, e.g. `{{ color.name|lowercase|replace("some", "other") }}`

- `original`: The name as defined in Figma
- `snake`: The name in snake case
- `kebab`: The name in kebab case
- `pascal`: The name in pascal case


#### Figma
- `file`: The Figma file name
- `last_modified`: Last modified date of the Figma file
- `version`: The version of the Figma file

#### Scale

- `scale`: The scale as floating point number as configured
- `name_prefix`: The prefix of the filename as configured
- `name_suffix`: The suffix of the filename as configured

## Build the project

- Clone the Git
- `./gradlew clean build` will build the project and create files
  in `figma-exported/build/distributions`