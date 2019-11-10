package com.data.browser.ui;

import com.data.browser.AppData;
import com.dbutils.common.DBConnections;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static com.data.browser.Utils.logStackTrace;

/**
 * It is a JavaFX Background task to execute a Query and refresh the TableView with the results.
 */
public class RefreshQueryResultsTask extends Task<Long> {
    private final Connection connection;
    private final String query;
    private final AnchorPane tableViewAnchorPane;
    private final ProgressIndicator progressIndicator;
    private final Label statusMessage;
    private final Button fetchDataBtn;

    private Long recordCount = 0L;
    private TableView tableView;

    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final ClipboardContent content = new ClipboardContent();

    public RefreshQueryResultsTask(Connection connection,
                                   String query,
                                   AnchorPane tableViewAnchorPane,
                                   ProgressIndicator progressIndicator,
                                   Label statusMessage,
                                   Button fetchDataBtn) {
        this.connection = connection;
        this.query = query;
        this.tableViewAnchorPane = tableViewAnchorPane;
        this.progressIndicator = progressIndicator;
        this.statusMessage = statusMessage;
        this.fetchDataBtn = fetchDataBtn;

        // Create a Table View and setup its properties
        createTableView();
    }

    @Override
    protected Long call() throws Exception {
        ResultSet resultSet = null;
        ObservableList<ObservableList<Object>> queryResultData = FXCollections.observableArrayList();
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
                Platform.runLater(() -> tableView.getColumns().addAll(col));
            }

            // Fetch Data from Result Set
            while (resultSet.next()) {
                ObservableList<Object> row = FXCollections.observableArrayList();
                recordCount++;

                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    row.add(resultSet.getObject(i));
                }
                queryResultData.add(row);
            }

            if (recordCount > 0)
                tableView.setItems(queryResultData);
        } catch (Exception e) {
            logStackTrace(e);
            System.out.println(e);
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

        if (recordCount > 0) {
            // Set Table View with data
            Platform.runLater(() -> {
                tableViewAnchorPane.getChildren().add(tableView);
            });
        }
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

    private void createTableView() {
        tableView = new TableView();
        AnchorPane.setTopAnchor(tableView, 5.0);
        AnchorPane.setRightAnchor(tableView, 5.0);
        AnchorPane.setBottomAnchor(tableView, 5.0);
        AnchorPane.setLeftAnchor(tableView, 5.0);
        tableView.setStyle("-fx-background-insets: 0 ;");

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().setCellSelectionEnabled(true);

        ObservableList<TablePosition> selectedCells = tableView.getSelectionModel().getSelectedCells();

        selectedCells.addListener((ListChangeListener.Change<? extends TablePosition> change) -> {
            StringBuilder selectedCellsData = new StringBuilder();
            Object object;

            if (selectedCells.size() > 0) {
                for (int i = 0; i < selectedCells.size(); i++) {
                    TablePosition selectedCell = selectedCells.get(i);
                    TableColumn column = selectedCell.getTableColumn();
                    int rowIndex = selectedCell.getRow();

                    if (column.getCellObservableValue(rowIndex) != null) {
                        object = column.getCellObservableValue(rowIndex).getValue();
                        selectedCellsData
                                .append(",")
                                .append(object);
                    }
                }

                if (selectedCellsData.length() > 0)
                    content.putString(selectedCellsData.deleteCharAt(0).toString());

                clipboard.setContent(content);
            }
        });
    }
}