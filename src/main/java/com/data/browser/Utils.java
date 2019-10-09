package com.data.browser;

import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.data.browser.AppLogger.logger;

public class Utils {
    /**
     * Log an Exception
     *
     * @param e Exception Object
     */
    public static void logStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        logger.debug(sStackTrace);
    }

    public static void autoResizeColumns(TableView<?> table) {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getColumns().stream().forEach((column) -> {
            Text t = new Text(column.getText());
            double max = t.getLayoutBounds().getWidth();
            for (int i = 0; i < table.getItems().size(); i++) {
                if (column.getCellData(i) != null) {
                    t = new Text(column.getCellData(i).toString());
                    double calcwidth = t.getLayoutBounds().getWidth();
                    if (calcwidth > max) {
                        max = calcwidth;
                    }
                }
            }
            //set the new max-width with some extra space
            column.setPrefWidth(max + 20.0d);
        });
    }
}