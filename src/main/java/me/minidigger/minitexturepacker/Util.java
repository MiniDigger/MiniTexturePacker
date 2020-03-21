package me.minidigger.minitexturepacker;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import javax.imageio.ImageIO;

public class Util {
    static void deleteDir(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static void copyAndBrighten(Path input, Path output, float factor) throws IOException {
        // this is hardcoded to blocks but I dont give a shit right now
        if (factor != 1 && input.toString().endsWith(".png") && input.toString().contains("blocks")) {
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

    private static BufferedImage brighten(BufferedImage image, float factor, String debugName) {
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
