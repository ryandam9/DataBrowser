package com.data.browser.ui;

import com.data.browser.AppData;
import com.data.browser.Utils;
import com.dbutils.common.DBConnections;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static com.data.browser.Utils.logStackTrace;

public class RefreshQueryResultsTask extends Task<Long> {
    private final TableView resultsView;
    private final Connection connection;
    private final String query;
    private final ProgressIndicator progressIndicator;
    private final Label statusMessage;
    private final Button fetchDataBtn;
    private Long recordCount = 0L;

    public RefreshQueryResultsTask(Connection connection, String query, TableView resultsView, ProgressIndicator progressIndicator,
                                   Label statusMessage,
                                   Button fetchDataBtn) {
        this.connection = connection;
        this.query = query;
        this.resultsView = resultsView;
        this.progressIndicator = progressIndicator;
        this.statusMessage = statusMessage;
        this.fetchDataBtn = fetchDataBtn;
    }

    @Override
    protected Long call() throws Exception {
        ResultSet resultSet = null;
        ObservableList<ObservableList<String>> queryResultData = FXCollections.observableArrayList();
        Long recordCount = 0L;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            AppData.sqlStatement = preparedStatement;
            resultSet = DBConnections.execReadOnlyQuery(connection, preparedStatement);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            // Setup Header
            for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                final int j = i;
                TableColumn col = new TableColumn(resultSet.getMetaData().getColumnName(i + 1).toUpperCase());
//                col.setPrefWidth(Control.USE_COMPUTED_SIZE);

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
            Platform.runLater(() -> Utils.autoResizeColumns(resultsView));
        } catch (Exception e) {
            logStackTrace(e);
            Platform.runLater(() -> statusMessage.setText(e.getMessage()));
            throw e;
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
        Platform.runLater(() -> fetchDataBtn.setDisable(false));
    }

    @Override
    protected void failed() {
        super.failed();
    }
}