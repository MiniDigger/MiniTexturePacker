package me.minidigger.minitexturepacker;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import javax.imageio.ImageIO;

public class MiniTexturePacker {

    public static final String S = File.separator;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar minitexturepacker /path/to/dir <factor>");
            return;
        }
        Path mainDir = Path.of(args[0]);
        float brightenFactor = Float.parseFloat(args[1]);

        new MiniTexturePacker(mainDir.resolve("original"), mainDir.resolve("patch"), mainDir.resolve("output")).patch(brightenFactor);
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
        copyFolder("textures", brightenFactor);
        copyFolder("sounds", 1);
        copySoundsJson();
        copyFolder("optifine", 1);
        copyFolder("models", 1);
        copyFolder("blockstates", 1);
        copySplashes();
        copyFont();
        // TODO blockstates and models
    }

    private void cleanOutput() {
        System.out.print("Cleaning output... ");
        try {
            deleteDir(output);
            Files.createDirectories(output);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("Error while cleaning output: " + e.getClass().getName() + ": " + e.getMessage());
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

    private void copyFont() {
        System.out.print("Copying font... ");
        String name = "assets" + S + "minecraft" + S + "font" + S + "glyph_sizes.bin";
        Path splashes = patch.resolve(name);
        if (Files.isRegularFile(splashes)) {
            try {
                Files.createDirectories(output.resolve("assets" + S + "minecraft" + S + "font"));
            } catch (IOException e) {
                System.out.println("Error creating font folder: " + e.getClass().getName() + ": " + e.getMessage());
            }
            try {
                Files.copy(splashes, output.resolve(name));
                System.out.println("Done!");
            } catch (IOException e) {
                System.out.println("Error while copying font: " + e.getClass().getName() + ": " + e.getMessage());
            }
        } else {
            System.out.println("No font found");
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

    // util shit

    private void copyFolder(String name, float brightenFactor) {
        System.out.print("Creating " + name + " folder... ");
        if (brightenFactor != 1) {
            System.out.print("Also brighten it with factor " + brightenFactor + "... ");
        }
        String folderName = "assets" + S + "minecraft" + S + "" + name;
        try {
            Files.createDirectories(output.resolve(folderName));
        } catch (IOException e) {
            System.out.println("Error while creating " + name + " folder: " + e.getClass().getName() + ": " + e.getMessage());
            return;
        }
        System.out.println("Done");
        System.out.print("Copying " + name + "... ");
        try {
            // copy over files from original, using patch files if they are there
            Files.list(original.resolve(folderName)).parallel().forEach(folder -> {
                if (Files.isDirectory(folder)) {
                    copyFolder(folderName, folder, brightenFactor);
                } else if (Files.isRegularFile(folder)) {
                    copyFitting(folder.getFileName().toString(), original.resolve(folderName), patch.resolve(folderName), output.resolve(folderName), brightenFactor);
                }
            });
            // copy over fully custom files
            Path patchFolder = patch.resolve(folderName);
            if (Files.isDirectory(patchFolder)) {
                Files.list(patchFolder).parallel().forEach(folder -> {
                    String patchFolderName = folder.getFileName().toString();
                    Path outputFolder = output.resolve(folderName).resolve(patchFolderName);
                    if (!Files.isDirectory(outputFolder)) {
                        try {
                            Files.createDirectories(outputFolder);
                        } catch (IOException e) {
                            System.out.println("Error while creating " + outputFolder + " folder: " + e.getClass().getName() + ": " + e.getMessage());
                        }
                    }
                    copyNewFiles(folder, outputFolder, brightenFactor);
                });
            }
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("Error while listing files " + original.resolve(folderName) + ": " + e.getClass().getName() + ": " + e.getMessage());
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
            if (Files.exists(newPatch)) {
                Files.list(newPatch).parallel()
                        .forEach(file -> {
                            String fileName = file.getFileName().toString();
                            if (Files.isDirectory(file)) {
                                // if its a dir, we need to go one step deeper
                                copyNewFiles(newPatch.resolve(file), newOutput.resolve(file), brightenFactor);
                            } else if (Files.isRegularFile(file)) {
                                // if its a file we can just copy, assuming its actually a new file
                                if (!Files.isRegularFile(newOutput.resolve(fileName))) {
                                    try {
                                        copyAndBrighten(newPatch.resolve(fileName), newOutput.resolve(fileName), brightenFactor);
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
        }
        if (!Files.isRegularFile(file)) {
            System.err.println("Couldn't copy file " + name + " since it neither exist in patch nor in original");
            return;
        }

        try {
            copyAndBrighten(file, output.resolve(name), brightenFactor);
        } catch (IOException e) {
            System.err.println("Error while copying " + name + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void deleteDir(Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println(exc.toString());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyAndBrighten(Path input, Path output, float factor) throws IOException {
        if (factor != 1 && input.toString().endsWith(".png") && input.toString().contains("/blocks/")) {
            BufferedImage in = ImageIO.read(input.toFile());
            if (in == null) {
                System.out.println("Couldnt brighten " + input.toString() + ", just copy OG");
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            BufferedImage out = brighten(in, factor, input.toString());
            if (out == null) {
                return;
            }
            ImageIO.write(out, "png", output.toFile());
        } else {
            Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private BufferedImage brighten(BufferedImage image, float factor, String debugName) {
        try {
            float[] factors = new float[]{
                    factor, factor, factor, 1f
            };
            float[] offsets = new float[]{
                    0.0f, 0.0f, 0.0f, 0.0f
            };
            RescaleOp op = new RescaleOp(factors, offsets, null);
            return op.filter(image, null);
        } catch (IllegalArgumentException ex) {
            try {
                float[] factors = new float[]{
                        1.4f, 1.4f, 1.4f
                };
                float[] offsets = new float[]{
                        0.0f, 0.0f, 0.0f
                };
                RescaleOp op = new RescaleOp(factors, offsets, null);
                return op.filter(image, null);
            } catch (IllegalArgumentException ex2) {
                System.out.println("Couldnt brighten " + debugName + ": " + ex2.getMessage());
                return null;
            }
        }
    }
}
