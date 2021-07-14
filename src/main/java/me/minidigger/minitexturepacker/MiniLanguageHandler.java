package me.minidigger.minitexturepacker;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniLanguageHandler {

    private Path patch;
    private Path output;

    private String assetIndex = "https://launchermeta.mojang.com/v1/packages/e5af543d9b3ce1c063a97842c38e50e29f961f00/1.17.json";

    public MiniLanguageHandler(Path patch, Path output) {
        this.patch = patch;
        this.output = output;
    }

    public void process() {
        System.out.print("Writing lang injections... ");
        Path outputLangFolder = output.resolve("assets/minecraft/lang/");
        JSONObject assets;
        try {
            assets = readJsonFromUrl(assetIndex);
        } catch (IOException e) {
            System.err.println("Can't download assets!");
            e.printStackTrace();
            return;
        }
        AtomicInteger count = new AtomicInteger();
        AtomicInteger existing = new AtomicInteger();
        assets.getJSONObject("objects")
                .keySet()
                .stream()
                .filter(k -> k.startsWith("minecraft/lang/"))
                .map(k -> k.replace("minecraft/lang/", ""))
                .forEach(filename -> {
                    try {
                        Path outputFile = outputLangFolder.resolve(filename);
                        JSONObject json = new JSONObject();
                        if (Files.isRegularFile(outputFile)) {
                            // we already had a file, read that
                            json = readJsonFromPath(outputFile);
                            existing.incrementAndGet();
                        }

                        merge(json);
                        writeJsonToPath(json, outputFile);
                        count.incrementAndGet();
                    } catch (IOException e) {
                        System.err.println("Error while processing lang asset " + filename);
                        e.printStackTrace();
                    }
                });

        System.out.println("Wrote " + count.get() + " lang files, found " + existing.get() + " existing files!");
    }

    private void merge(JSONObject json) throws IOException {
        JSONObject injection = readJsonFromPath(patch.resolve("lang.json"));
        for (String key : injection.keySet()) {
            json.put(key, injection.get(key));
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private JSONObject readJsonFromPath(Path path) throws IOException, JSONException {
        try (BufferedReader rd = Files.newBufferedReader(path)) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private void writeJsonToPath(JSONObject object, Path path) throws IOException, JSONException {
        try (BufferedWriter br = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            object.write(br);
        }
    }
}
