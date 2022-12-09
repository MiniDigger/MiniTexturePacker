# MiniTexturePacker

Small utility to maintain a set of changes ontop of an existing texture pack and to easily add custom models

## Usage

You want to have 3 folders like this:
```
stuff/
├── original/ # where you can place the original texturepack
│   ├── assets/
│   │   └── minecraft/
│   │       ├── blockstates/
│   │       ├── font/
│   │       ├── etc../
│   │       └── sounds.json
│   ├── pack.mcmeta
│   └── pack.png
├── output/ # where the combined texture pack will end up
│   ├── assets/
│   │   └── minecraft/
│   │       ├── blockstates/
│   │       ├── font/
│   │       ├── etc../
│   │       └── sounds.json
│   ├── pack.mcmeta
│   └── pack.png
└── patch/ # where you can put your changes
    ├── assets/
    │   ├── minecraft/ # overrides of mc stuff go here
    │   │   ├── blockstates/
    │   │   ├── font/
    │   |   ├── models/ # (with your template file, see #custom models)
    │   │   ├── etc../
    │   │   └── sounds.json
    │   └── <custom>/ # your custom models life here
    │       ├── models/
    │       └── textures/
    ├── mappings.csv # id mappings for your custom models
    ├── lang.json # language overrides
    ├── pack.mcmeta
    └── pack.png
```

And then run `java -jar minitexturepacker.jar --dir \path\to\stuff --namespace <your namespace>`  
Run the jar with `--help` for a list of supported arguments.

It currently handles the following things:
* pack.mcmeta + pack.png
* sounds.json
* font
* optifine
* sounds
* texts/splashes.txt
* textures
* blockstates
* models
* language

Unless otherwise noted, it will copy original to output, but first looks into patch for an updates file, then copies all new files from patch to output.

If you have an empty file in patch, it will not copy a file from orig nor from patch. This allows you to revert files from orig back to mc default.

## Custom Models

This tool allows you to easily add new custom models.  
All you need to do is add your models and textures to your custom namespaces and define a template item you want to override, e.g. a diamond sword.
This diamond_sword.json should look like this:
```json
{
	"parent": "item/handheld",
	"textures": {
		"layer0": "item/diamond_sword"
	},
	"overrides": [
		"%mini_model_creator_marker%"
	]
}
```
The tools will then write all custom models into the overrides section.  
It will also create a mappings.csv where you can read which CustomModelData id has been assigned to the model.  
You should never delete that file, as if you regenerate it, the IDs might change, which would be bad if you already use them.

The argument `--item default=diamond_sword` allows you to change which item to patch and even to specify multiple types.

### Bows

Bows are specially handled, to make it easier to deal with their multiple stages. For convince, MiniTexturePacker bundles the bow template. You only need to override it, if you want to override the vanilla bow.  
MiniTexturePacker allows you to specify the pull factor in the file name, no pull factor means not pulling, 0 means start of the pull, 100 means end of the pull.  
So for a nice animation, you most likely will end up with files like this:
```
my_cool_bow.bow.0.json
my_cool_bow.bow.65.json
my_cool_bow.bow.90.json
my_cool_bow.bow.json
```
the resulting predicates would look like this
```json
{ "predicate": { "custom_model_data": 2, "pulling": 0, "pull": 0}, "model": "namespace:item/my_cool_bow.bow"},
{ "predicate": { "custom_model_data": 2, "pulling": 1, "pull": 0.0}, "model": "namespace:item/my_cool_bow.bow.0"},
{ "predicate": { "custom_model_data": 2, "pulling": 1, "pull": 0.9}, "model": "namespace:item/my_cool_bow.bow.90"},
{ "predicate": { "custom_model_data": 2, "pulling": 1, "pull": 0.65}, "model": "namespace:item/my_cool_bow.bow.65"},
```

## Language overrides

This tool allows you to easily override a string in all languages.  
All you need to do is create a new file called lang.json and put your strings in there. 
The tool will then check if you already have a lang file and insert the string there, if not, it will download the missing lang files from mojang.
