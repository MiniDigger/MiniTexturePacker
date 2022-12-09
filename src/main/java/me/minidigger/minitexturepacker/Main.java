package me.minidigger.minitexturepacker;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Main {

    @Parameter(names = {"--dir", "-d"}, required = true, description = "The main dir")
    private String mainDirPath;
    @Parameter(names = {"--brighten", "-b"}, required = false, description = "The factor which the original textures should be brightened by")
    private float brightenFactor = 1.0f;
    @Parameter(names = {"--namespace", "-n"}, required = false, description = "The namespace of your custom textures")
    private String namespace = "dyescape";
    @Parameter(names = {"--item", "-i"}, required = false, description = "A list of key=value pairs for item type mappings")
    private List<String> items = List.of("default=diamond_sword", "bow=bow");

    @Parameter(names = "--help", help = true, description = "Displays a help page")
    private boolean help;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();
        commander.parse(args);

        if (main.help) {
            commander.usage();
        } else {
            main.start();
        }
    }

    private void start() {
        Map<String, String> itemsToOverride = items.stream()
                .map(s -> s.split("="))
                .collect(Collectors.toMap(s -> s[0], s-> s[1]));
        Path mainDir = Path.of(mainDirPath);

        new MiniTexturePacker(mainDir.resolve("original"), mainDir.resolve("patch"), mainDir.resolve("output")).patch(brightenFactor);
        new MiniModelCreator(mainDir.resolve("patch"), mainDir.resolve("output"), namespace, itemsToOverride).process();
        new MiniLanguageHandler(mainDir.resolve("patch"), mainDir.resolve("output")).process();
    }
}
