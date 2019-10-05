package com.data.browser.ui;

import com.dbutils.common.DBConnections;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static com.data.browser.Utils.logStackTrace;

public class RefreshQueryResultsTask extends Task<Long> {
    private final TableView resultsView;
    private final Connection connection;
    private final String query;
    private final ProgressIndicator progressIndicator;
    private final Label statusMessage;
    private Long recordCount = 0L;

    public RefreshQueryResultsTask(Connection connection, String query, TableView resultsView, ProgressIndicator progressIndicator,
                                   Label statusMessage) {
        this.connection = connection;
        this.query = query;
        this.resultsView = resultsView;
        this.progressIndicator = progressIndicator;
        this.statusMessage = statusMessage;
    }

    @Override
    protected Long call() throws Exception {
        ResultSet resultSet = null;
        ObservableList<ObservableList<String>> queryResultData = FXCollections.observableArrayList();
        Long recordCount = 0L;

        try {
            resultSet = DBConnections.execReadOnlyQuery(connection, query);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            // Setup Header
            for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                final int j = i;
                TableColumn col = new TableColumn(resultSet.getMetaData().getColumnName(i + 1));

                col.setCellValueFactory(
                        new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                            public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                                try {
                                    return new SimpleStringProperty(param.getValue().get(j).toString());
                                } catch (Exception e) {
                                    return null;
                                }
                            }
                        });
                Platform.runLater(() -> resultsView.getColumns().addAll(col));
            }

            // Fetch Data from Result Set
            while (resultSet.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                recordCount++;

                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    row.add(resultSet.getString(i));
                }
                queryResultData.add(row);
            }

            // Set Table View with data
            Platform.runLater(() -> resultsView.setItems(queryResultData));
        } catch (Exception e) {
            logStackTrace(e);
        }

        this.recordCount = recordCount;
        return recordCount;
    }

    @Override
    public void succeeded() {
        super.succeeded();
        Platform.runLater(() -> statusMessage.setText("Records fetched from DB: " + this.recordCount));
    }

    @Override
    protected void done() {
        super.done();
        Platform.runLater(() -> progressIndicator.setVisible(false));
    }
}