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
    ├── pack.mcmeta
    └── pack.png
```

And then run `java -jar minitexturepacker.jar \path\to\stuff <brighten factor>`

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

Unless overwise noted, it will copy original to output, but first looks into patch for an updates file, then copies all new files from patch to output.

## Custom Models

This tools allows you to easily add new custom models.  
All you need to do is add your models and textures to your custom namespaces and define a template item you want to override, eg. a diamond sword.
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
