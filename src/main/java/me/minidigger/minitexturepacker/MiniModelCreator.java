package me.minidigger.minitexturepacker;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MiniModelCreator {

    private static final String S = File.separator;
    private static final String template = "\t\t{ \"predicate\": {\"custom_model_data\":  %s}, \"model\": \"%s\"},";
    private static final String mappingFileName = "mappings.csv";

    private Path input;
    private Path output;
    private String namespace;
    private String itemToOverride;

    public MiniModelCreator(Path input, Path output, String namespace, String itemToOverride) {
        this.input = input;
        this.output = output;
        this.namespace = namespace;
        this.itemToOverride = itemToOverride;
    }

    public void process() {
        Path modelDir = input.resolve("assets" + S + namespace + S + "models");
        List<Path> models = findModels(modelDir);

        Path mappingFile = input.resolve(mappingFileName);
        Map<String, Integer> mapping = loadMappings(mappingFile);
        processMappings(modelDir, models, mapping);
        writeMappings(mapping, mappingFile);

        Path inputFile = input.resolve("assets" + S + "minecraft" + S + "models" + S + "item" + S + itemToOverride + ".json");
        Path outputFile = output.resolve("assets" + S + "minecraft" + S + "models" + S + "item" + S + itemToOverride + ".json");
        copyItemAndWriteModels(inputFile, outputFile, mapping);

        inputFile = input.resolve("assets" + S + namespace);
        outputFile = output.resolve("assets" + S + namespace);
        copyCustomStuff(inputFile, outputFile);
    }

    private void copyCustomStuff(Path input, Path output) {
        System.out.print("Copying custom models... ");
        try {
            Files.walk(input)
                    .forEach(source -> {
                        try {
                            Files.copy(source, output.resolve(input.relativize(source)));
                        } catch (IOException ex) {
                            System.out.println("Error while coyping file: " + source + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                        }
                    });
            System.out.println("Done!");
        } catch (IOException ex) {
            System.out.println("Error while copying custom models: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void processMappings(Path modelDir, List<Path> models, Map<String, Integer> mappings) {
        models.forEach(model -> {
            Path newPath = modelDir.relativize(model);
            String path = newPath.toString().replace("/", "|").replace("\\", "|");
            if (!mappings.containsKey(path)) {
                System.out.println("Creating new mapping for new model " + newPath + ": " + (mappings.size() + 1));
                mappings.put(path, mappings.size() + 1);
            }
        });
    }

    private void writeMappings(Map<String, Integer> mappings, Path mappingFile) {
        System.out.print("Writing mappings... ");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(mappingFile))) {
            mappings.forEach((path, id) -> writer.println(path.replace(".json", "").replace("|", " ") + "," + id + "," + path));
            System.out.println("Done!");
        } catch (IOException ex) {
            System.out.println("Error while writing mappings: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private Map<String, Integer> loadMappings(Path mappingFile) {
        System.out.print("Loading mappings... ");
        Map<String, Integer> mappings = new TreeMap<>();
        try {
            Files.readAllLines(mappingFile).forEach(line -> {
                String[] s = line.split(",");
//                String name = s[0];
                int id = Integer.parseInt(s[1]);
                String path = s[2];

                mappings.put(path, id);
            });
            System.out.println("Loaded " + mappings.size() + " mappings!");
        } catch (IOException ex) {
            System.out.println("Error while loading mappings: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        return mappings;
    }

    private List<Path> findModels(Path modelDir) {
        System.out.print("Scanning for models... ");
        List<Path> models = new ArrayList<>();
        try {
            // walk thru all models
            Files.walkFileTree(modelDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".json")) {
                        models.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("Found " + models.size() + "!");
        } catch (IOException e) {
            System.out.println("Error while scanning for models in " + modelDir);
            e.printStackTrace();
        }
        return models;
    }

    private void copyItemAndWriteModels(Path inputFile, Path outputFile, Map<String, Integer> mappings) {
        if (!Files.isDirectory(outputFile.getParent())) {
            System.out.print("Creating output dir... ");
            try {
                Files.createDirectories(outputFile.getParent());
                System.out.println("Done!");
            } catch (IOException e) {
                System.out.println("Error while creating dir " + outputFile.getParent() + ": " + e.getClass().getSimpleName() + ":" + e.getMessage());
            }
        }
        System.out.print("Writing item models to template file... ");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            Files.readAllLines(inputFile).forEach(line -> {
                if (line.contains("%mini_model_creator_marker%")) {
                    mappings.forEach((path, id) -> {
                        String properPath = namespace + ":" + path.replace("|", "/").replace(".json", "");
                        writer.println(String.format(template, id, properPath));
                    });

                    writer.println("\t\t{ \"predicate\": {\"custom_model_data\":  13371337}, \"model\": \"dyescape:items/steel_mace\"}"); // dummy to fix trailing semicolon
                } else {
                    writer.println(line);
                }
            });
            System.out.println("Done!");
        } catch (IOException ex) {
            System.out.println("Error while writing item models!");
            ex.printStackTrace();
        }
    }
}
