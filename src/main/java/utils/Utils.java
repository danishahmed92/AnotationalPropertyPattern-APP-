package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DANISH AHMED on 12/9/2018
 */
public class Utils {
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
