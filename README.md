# MiniTexturePacker

Small utility to maintain a set of changes ontop of an existing texture pack.

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
    │   └── minecraft/
    │       ├── blockstates/
    │       ├── font/
    │       ├── etc../
    │       └── sounds.json
    ├── pack.mcmeta
    └── pack.png
```

And then run `java -jar minitexturepacker.jar \path\to\stuff`

It currently handles the following things:
* pack.mcmeta + pack.png
* sounds.json
* font
* optifine
* sounds
* texts/splashes.txt
* textures

Unless overwise noted, it will copy original to output, but first looks into patch for an updates file, then copies all new files from patch to output.

## TODO
Currently missing support for blockstates and models
