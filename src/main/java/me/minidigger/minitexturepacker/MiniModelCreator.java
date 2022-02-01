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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniModelCreator {

    record Mapping(String name, String path, int id, String type) {
        public String csv() {
            return name() + "," + id() + "," + path() + "," + type();
        }
    }

    private static final String S = File.separator;
    private static final String template = "\t\t{ \"predicate\": {\"custom_model_data\":  %s}, \"model\": \"%s\"},";
    private static final String mappingFileName = "mappings.csv";

    private final Path input;
    private final Path output;
    private final String namespace;
    private final Map<String, String> itemsToOverride;

    public MiniModelCreator(Path input, Path output, String namespace, Map<String, String> itemsToOverride) {
        this.input = input;
        this.output = output;
        this.namespace = namespace;
        this.itemsToOverride = itemsToOverride;
    }

    public void process() {
        Path modelDir = input.resolve("assets" + S + namespace + S + "models");
        List<Path> models = findModels(modelDir);

        Path mappingFile = input.resolve(mappingFileName);
        Map<String, Mapping> mapping = loadMappings(mappingFile);
        AtomicInteger nextId = new AtomicInteger(findNextId(mapping));
        deduplicateMappings(mapping, nextId);
        processMappings(modelDir, models, mapping, nextId);
        writeMappings(mapping, mappingFile);

        for (var entry : itemsToOverride.entrySet()) {
            copyItemAndWriteModels(mapping, entry.getKey(), entry.getValue());
        }
    }

    private void processMappings(Path modelDir, List<Path> models, Map<String, Mapping> mappings, AtomicInteger nextId) {
        models.forEach(model -> {
            Path newPath = modelDir.relativize(model);
            String path = newPath.toString().replace("/", "|").replace("\\", "|");
            if (!mappings.containsKey(path)) {
                int id = nextId.incrementAndGet();
                String type = "default";
                String filename = newPath.getFileName().toString().replace(".json", "");
                int index = filename.indexOf(".");
                if (index != -1) {
                    type = filename.substring(index + 1);
                }
                String name = path.replace(".json", "").replace("|", " ").replace("." + type, "");
                System.out.println("Creating new mapping for new model " + name + " with mapping " + newPath + ": " + id + " using type " + type);
                mappings.put(path, new Mapping(name,path, id, type));
            }
        });
    }

    private void writeMappings(Map<String, Mapping> mappings, Path mappingFile) {
        System.out.print("Writing mappings... ");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(mappingFile))) {
            mappings.forEach((path, mapping) -> writer.println(mapping.csv()));
            System.out.println("Done!");
        } catch (IOException ex) {
            System.out.println("Error while writing mappings: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private Map<String, Mapping> loadMappings(Path mappingFile) {
        System.out.print("Loading mappings... ");
        Map<String, Mapping> mappings = new TreeMap<>();
        try {
            Files.readAllLines(mappingFile).forEach(line -> {
                String[] s = line.split(",");
                String name = s[0];
                int id = Integer.parseInt(s[1]);
                String path = s[2];
                String type = s.length > 3 ? s[3] : "default";

                mappings.put(path, new Mapping(name, path, id, type));
            });
            System.out.println("Loaded " + mappings.size() + " mappings!");
        } catch (IOException ex) {
            System.out.println("Error while loading mappings: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        return mappings;
    }

    private void deduplicateMappings(Map<String, Mapping> mappings, AtomicInteger nextId) {
        Set<Integer> ids = new HashSet<>();
        for (String key : new HashSet<>(mappings.keySet())) {
            Mapping old = mappings.get(key);
            int id = old.id();
            if (ids.contains(id)) {
                int newId = nextId.incrementAndGet();
                System.out.println("found duplicate id for " + id + ", assigning new id " + newId + " (" + key +")...");
                mappings.remove(key);
                mappings.put(key, new Mapping(old.name(), old.path(), newId, old.type()));
            } else {
                ids.add(id);
            }
        }
    }

    private int findNextId(Map<String, Mapping> mappings) {
        return mappings.values().stream().mapToInt(Mapping::id).max().orElse(1);
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

    private void copyItemAndWriteModels(Map<String, Mapping> mappings, String type, String itemToOverride) {
        Path inputFile = input.resolve("assets" + S + "minecraft" + S + "models" + S + "item" + S + itemToOverride + ".json");
        Path outputFile = output.resolve("assets" + S + "minecraft" + S + "models" + S + "item" + S + itemToOverride + ".json");
        if (!Files.isDirectory(outputFile.getParent())) {
            System.out.print("Creating output dir... ");
            try {
                Files.createDirectories(outputFile.getParent());
                System.out.println("Done!");
            } catch (IOException e) {
                System.out.println("Error while creating dir " + outputFile.getParent() + ": " + e.getClass().getSimpleName() + ":" + e.getMessage());
            }
        }
        System.out.print("Writing item models of type " + type + " to template file " + itemToOverride + ".json... ");
        AtomicBoolean foundMarker = new AtomicBoolean(false);
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            Files.readAllLines(inputFile).forEach(line -> {
                if (line.contains("%mini_model_creator_marker%")) {
                    foundMarker.set(true);
                    mappings.entrySet().stream().filter(e -> e.getValue().type().equals(type)).sorted(Comparator.comparing(e -> e.getValue().id())).forEach((entry) -> {
                        String properPath = namespace + ":" + entry.getKey().replace("|", "/").replace(".json", "");
                        writer.println(String.format(template, entry.getValue().id, properPath));
                    });

                    writer.println("\t\t{ \"predicate\": {\"custom_model_data\":  13371337}, \"model\": \"dyescape:items/steel_mace\"}"); // dummy to fix trailing semicolon
                } else {
                    writer.println(line);
                }
            });
            if (!foundMarker.get()) {
                System.out.println("Didn't find %mini_model_creator_marker%!");
            } else {
                System.out.println("Done!");
            }
        } catch (IOException ex) {
            System.out.println("Error while writing item models!");
            ex.printStackTrace();
        }
    }
}
