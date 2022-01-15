package me.minidigger.minitexturepacker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MiniTexturePacker {

    private static final String S = File.separator;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar minitexturepacker /path/to/dir <factor> [namespace] [itemToOverride]");
            return;
        }
        Path mainDir = Path.of(args[0]);
        float brightenFactor = Float.parseFloat(args[1]);
        String namespace = args.length >= 3 ? args[2] : "dyescape";
        String itemToOverride = args.length >= 4 ? args[3] : "diamond_sword";

        new MiniTexturePacker(mainDir.resolve("original"), mainDir.resolve("patch"), mainDir.resolve("output")).patch(brightenFactor);
        new MiniModelCreator(mainDir.resolve("patch"), mainDir.resolve("output"), namespace, itemToOverride).process();
        new MiniLanguageHandler(mainDir.resolve("patch"), mainDir.resolve("output")).process();
    }

    private final Path original;
    private final Path patch;
    private final Path output;

    public MiniTexturePacker(Path original, Path patch, Path output) {
        this.original = original;
        this.patch = patch;
        this.output = output;
    }

    public void patch(float brightenFactor) {
        cleanOutput();
        copyMeta();
        copyFolder("minecraft", "textures", brightenFactor);
        copyFolder("minecraft", "sounds", 1);
        copySoundsJson();
        copyFolder("minecraft", "optifine", 1);
        copyFolder("minecraft", "models", 1);
        copyFolder("minecraft", "blockstates", 1);
        copyFolder("minecraft", "font", 1);
        copySplashes();
        copyFolder("minecraft", "lang", 1);
        copyOtherNamespaces();
    }

    private void cleanOutput() {
        System.out.print("Cleaning output... ");
        try {
            Util.deleteDir(output);
        } catch (IOException e) {
            System.out.print("Error while cleaning output: " + e.getClass().getName() + ": " + e.getMessage());
        }
        try {
            Files.createDirectories(output);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("Error while creating output: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void copyMeta() {
        System.out.print("Copying meta... ");
        for (String fileName : new String[]{"pack.mcmeta", "pack.png"}) {
            copyFitting(fileName, original, patch, output, 1);
        }
        System.out.println("Done!");
    }

    private void copySplashes() {
        System.out.print("Copying splashes... ");
        String name = "assets" + S + "minecraft" + S + "texts" + S + "splashes.txt";
        Path splashes = patch.resolve(name);
        if (Files.isRegularFile(splashes)) {
            try {
                Files.createDirectories(output.resolve("assets" + S + "minecraft" + S + "texts"));
            } catch (IOException e) {
                System.out.println("Error creating texts folder: " + e.getClass().getName() + ": " + e.getMessage());
            }
            try {
                Files.copy(splashes, output.resolve(name));
                System.out.println("Done!");
            } catch (IOException e) {
                System.out.println("Error while copying splashes: " + e.getClass().getName() + ": " + e.getMessage());
            }
        } else {
            System.out.println("No splashes found");
        }
    }

    private void copySoundsJson() {
        System.out.print("Copying sounds.json... ");
        String name = "assets" + S + "minecraft" + S + "sounds.json";
        Path splashes = patch.resolve(name);
        if (Files.isRegularFile(splashes)) {
            try {
                Files.copy(splashes, output.resolve(name));
                System.out.println("Done!");
            } catch (IOException e) {
                System.out.println("Error while copying sounds.json: " + e.getClass().getName() + ": " + e.getMessage());
            }
        } else {
            System.out.println("No sounds.json found");
        }
    }

    private void copyOtherNamespaces() {
        System.out.println("Copying other namespaces... ");
        try {
            Files.list(patch.resolve("assets"))
                    .filter(f -> !f.getFileName().toString().equals("minecraft"))
                    .filter(f -> !f.getFileName().toString().startsWith("."))
                    .forEach(f -> {
                        copyFolder(f.getFileName().toString(), "", 1);
                    });
            System.out.println("ALL DONE!");
        } catch (IOException e) {
            System.out.println("Error while copying sounds.json: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    // util shit

    private void copyFolder(String namespace, String name, float brightenFactor) {
        String nameThing = (name.length() > 0 ? name : namespace);
        System.out.print("Creating " + nameThing + " folder... ");
        if (brightenFactor != 1) {
            System.out.print("Also brighten it with factor " + brightenFactor + "... ");
        }
        String folderName = "assets" + S + namespace + S + "" + name;
        try {
            Files.createDirectories(output.resolve(folderName));
        } catch (IOException e) {
            System.out.println("Error while creating " + nameThing + " folder: " + e.getClass().getName() + ": " + e.getMessage());
            return;
        }
        System.out.println("Done");
        System.out.print("Copying " + nameThing + "... ");
        Path ogFolder = original.resolve(folderName);
        try {
            // copy over files from original, using patch files if they are there
            if (Files.isDirectory(ogFolder)) {
                Files.list(ogFolder).parallel().forEach(folder -> {
                    if (Files.isDirectory(folder)) {
                        copyFolder(folderName, folder, brightenFactor);
                    } else if (Files.isRegularFile(folder)) {
                        copyFitting(folder.getFileName().toString(), ogFolder, patch.resolve(folderName), output.resolve(folderName), brightenFactor);
                    }
                });
            }
            // copy over fully custom files
            Path patchFolder = patch.resolve(folderName);
            if (Files.isDirectory(patchFolder)) {
                Path outputFolder = output.resolve(folderName);
                if (!Files.isDirectory(outputFolder)) {
                    try {
                        Files.createDirectories(outputFolder);
                    } catch (IOException e) {
                        System.out.println("Error while creating " + outputFolder + " folder: " + e.getClass().getName() + ": " + e.getMessage());
                    }
                }
                copyNewFiles(patchFolder, outputFolder, brightenFactor);
            }
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("Error while listing files " + ogFolder + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void copyFolder(String folderPath, Path subFolder, float brightenFactor) {
        // prepare output
        String subFolderName = subFolder.getFileName().toString();
        Path newOriginal = original.resolve(folderPath).resolve(subFolderName);
        Path newPatch = patch.resolve(folderPath).resolve(subFolderName);
        Path newOutput = output.resolve(folderPath).resolve(subFolderName);
        try {
            Files.createDirectories(newOutput);
        } catch (IOException e) {
            System.out.println("Error while creating output files " + newOutput + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
        // first iterate the files in orig
        try {
            Files.list(newOriginal).parallel().forEach(file -> {
                if (Files.isDirectory(file)) {
                    copyFolder(folderPath + "" + S + "" + subFolderName, file, brightenFactor);
                } else if (Files.isRegularFile(file)) {
                    copyFitting(file.getFileName().toString(), newOriginal, newPatch, newOutput, brightenFactor);
                }
            });
        } catch (IOException e) {
            System.out.println("Error while listing files in orig " + newOriginal + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
        // then copy new files from patch
        copyNewFiles(newPatch, newOutput, brightenFactor);
    }

    private void copyNewFiles(Path newPatch, Path newOutput, float brightenFactor) {
        try {
            if (!Files.exists(newOutput)) {
                Files.createDirectories(newOutput);
            }

            if (Files.exists(newPatch) && Files.isDirectory(newPatch)) {
                Files.list(newPatch).parallel()
                        .forEach(file -> {
                            String fileName = file.getFileName().toString();
                            if (Files.isDirectory(file)) {
                                // if its a dir, we need to go one step deeper
                                copyNewFiles(newPatch.resolve(fileName), newOutput.resolve(fileName), brightenFactor);
                            } else if (Files.isRegularFile(file)) {
                                // if its a file we can just copy, assuming its actually a new file
                                if (!Files.isRegularFile(newOutput.resolve(fileName))) {
                                    try {
                                        // 0 file in patch? -> skip
                                        if (Files.size(file) == 0) {
                                            return;
                                        }

                                        Util.copyAndBrighten(newPatch.resolve(fileName), newOutput.resolve(fileName), brightenFactor);
                                    } catch (IOException e) {
                                        System.out.println("Error while coyping file " + newPatch.resolve(fileName) + " : " + e.getClass().getName() + ": " + e.getMessage());
                                    }
                                }
                            }
                        });
            }
        } catch (IOException e) {
            System.out.println("Error while listing files in patch " + newPatch + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void copyFitting(String name, Path original, Path patch, Path output, float brightenFactor) {
        Path file = patch.resolve(name);
        if (!Files.isRegularFile(file)) {
            file = original.resolve(name);
        } else {
            try {
                // 0 file in patch? -> skip
                if (Files.size(file) == 0) {
                    return;
                }
            } catch (IOException e) {
                System.err.println("Can't read file " + name + " in patch to figure out its size!");
                return;
            }
        }
        if (!Files.isRegularFile(file)) {
            System.err.println("Couldn't copy file " + name + " since it neither exist in patch nor in original");
            return;
        }

        try {
            Util.copyAndBrighten(file, output.resolve(name), brightenFactor);
        } catch (IOException e) {
            System.err.println("Error while copying " + name + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

}
