package utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author DANISH AHMED on 12/9/2018
 */
public class Utils {
    public static List<String> getFilesInDirectory(String directory) {
        List<String> filesInDirectory = new LinkedList<>();
        Path path = Paths.get(directory);
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        filesInDirectory.add(filePath.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            filesInDirectory.add(path.getFileName().toString());
        }
        return filesInDirectory;
    }

    public static String[] getLabelSplit(String label) {
        try {
            if (!label.contains("."))
                return label.split(" ");

            String[] split = label.split(" ");
            if (split.length == 1)
                return split;

            List<String> splitLabel = new ArrayList<>();
            for (int i = 0; i < split.length; i++) {
                String[] indexLabel = (extractLabelParts(split, i, split[i]));
                String labelPart = indexLabel[1].trim().replaceFirst(split[i] + " ", "");
                splitLabel.add(labelPart);
                i = Integer.parseInt(indexLabel[0]);

                if ((i == split.length - 1)) {
                    if (!split[i].contains("."))
                        splitLabel.add(split[i]);
                }
            }
            return splitLabel.stream().toArray(String[]::new);
        } catch (Exception e) {
            System.out.println("Label split exception for:\t" + label);
            e.printStackTrace();
            return label.split(" ");
        }
    }

    public static String[] extractLabelParts(String[] labelSplit, int index, String labelPart) {
        if (!labelSplit[index].contains(".") || index == labelSplit.length - 1) {
            String[] indexLabel = new String[2];
            indexLabel[0] = Integer.toString(index);
            indexLabel[1] = labelPart;

            return indexLabel;
        }
        // contains "."
        return extractLabelParts(labelSplit, index + 1, labelPart + " " + labelSplit[index]);
    }
}
