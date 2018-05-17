package org.jmad.modelpack.gui.util;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;
import java.util.function.Consumer;

public final class FxUtils {

    public static void setFontWeight(TitledPane definitionPane, FontWeight weight) {
        Font currentFont = definitionPane.getFont();
        Font boldFont = Font.font(currentFont.getFamily(), weight, currentFont.getSize());
        definitionPane.setFont(boldFont);
    }


    public static void glueToAnchorPane(Node node) {
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
    }

    public static void setPercentageWith(TreeTableView<?> table, Map<TreeTableColumn<?, ?>, Double> percentages) {
        if(!table.getColumns().containsAll(percentages.keySet())) {
            throw new IllegalArgumentException("The percentages map must include all the columns of the table");
        }

        if(percentages.values().stream().mapToDouble(d -> d).sum() != 1.0) {
            throw new IllegalArgumentException("The sum of the percentages MUST be 1.0");
        }

        int widthMarginToPreventHorizontalScrollbar = table.getColumns().size() + 1;
        table.widthProperty().addListener(onChange(width -> percentages.forEach((column, percentage) -> {
            double processedWidth = width.doubleValue() - widthMarginToPreventHorizontalScrollbar;
            double columnWidth = Math.floor(processedWidth * percentage);
            column.setPrefWidth(columnWidth);
        })));
    }

    public static <T> ChangeListener<T> onChange(Consumer<T> consumer) {
        return (obs, ov, nv) -> consumer.accept(nv);
    }

    private FxUtils() {
        /* static things*/
    }
}
