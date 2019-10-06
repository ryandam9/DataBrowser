package com.data.browser.controllers;

import com.data.browser.AppData;
import com.data.browser.ui.RefreshQueryResultsTask;
import com.dbutils.common.ColumnDetail;
import com.dbutils.common.DBConnections;
import com.dbutils.common.TableDetail;
import com.dbutils.oracle.OracleMetadata;
import com.dbutils.sqlserver.SqlServerMetadata;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

import static com.data.browser.AppLogger.logger;
import static com.data.browser.Utils.logStackTrace;

public class DataBrowserController implements Initializable {
    // Controls in top horizontal Box
    @FXML
    private ComboBox databaseOptions;

    @FXML
    private TextField user;

    @FXML
    private TextField password;

    @FXML
    private TextField host;

    @FXML
    private TextField service;

    @FXML
    private TextField sid;

    @FXML
    private TextField port;

    @FXML
    private VBox progressBox;

    @FXML
    private Button connectBtn;

    @FXML
    private Label message;

    // UI Controls to produce result
    @FXML
    private TreeView objectBrowser;

    @FXML
    private AnchorPane anchorPane1;

    @FXML
    private VBox columnsBox;
    @FXML
    private TableView queryResult;

    // UI Controls in the bottom horizontal box
    @FXML
    private TextField recordsToFetch;

    @FXML
    private TextArea valuesToIgnore;

    @FXML
    private CheckBox isUniqueValuesChecked;

    @FXML
    private Button selectAllBtn;

    @FXML
    private Button resetBtn;

    @FXML
    private Button cancelQueryBtn;

    @FXML
    private Button fetchDataBtn;

    // These are to check for a Specific table
    @FXML
    private TextField searchDB;

    @FXML
    private TextField searchSchema;

    @FXML
    private TextField searchTable;

    @FXML
    private Button searchBtn;

    private ProgressIndicator progressIndicator;
    private Task<Connection> task;
    private static Connection connection;
    ListProperty<CheckBox> listProperty;

    private String queryDB;
    private String querySchema;
    private String queryTable;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set options to the Combo box
        databaseOptions.getItems().addAll("Select DB", AppData.ORACLE, AppData.SQL_SERVER);
        databaseOptions.getSelectionModel().selectFirst();

        // Create a Progress Indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setVisible(false);
        progressBox.getChildren().add(progressIndicator);

//        VBox.setVgrow(columnsBox, Priority.ALWAYS);
//        columnsBox.prefHeightProperty().bind(anchorPane1.heightProperty());

        // Only one entry can be selected at a time
        objectBrowser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // When a table is clicked, its columns should be printed in a List. Each Column is represented as a
        // Check box.
        objectBrowser.getSelectionModel()
                .selectedItemProperty()
                .addListener(new ChangeListener<TreeItem<String>>() {
                    @Override
                    public void changed(
                            ObservableValue<? extends TreeItem<String>> observable,
                            TreeItem<String> old_val,
                            TreeItem<String> new_val) {

                        TreeItem<String> selectedItem = new_val;
                        String table = selectedItem.getValue();
                        String schema = selectedItem.getParent().getValue();
                        String db = selectedItem.getParent().getParent().getValue();

                        TableDetail searchEntry = new TableDetail(db, schema, table, "BASE TABLE");

                        try {
                            List<ColumnDetail> columns = AppData.tables.get(db).get(schema).get(searchEntry);
                            List<String> cols = new ArrayList<>();

                            columns.forEach(columnDetail -> {
                                cols.add(columnDetail.getColumn());
                            });

                            Collections.sort(cols, new Comparator<String>() {
                                @Override
                                public int compare(String o1, String o2) {
                                    return o1.toLowerCase().compareTo(o2.toLowerCase());
                                }
                            });

                            ObservableList<CheckBox> checkBoxes = FXCollections.observableArrayList();

                            if (columns != null) {
                                for (String c : cols) {
                                    CheckBox columnCheckBox = new CheckBox(c);
                                    checkBoxes.add(columnCheckBox);
                                }
                            }

                            listProperty = new SimpleListProperty<>();
                            listProperty.set(checkBoxes);
                            ListView<CheckBox> listView = new ListView<>();
                            listView.itemsProperty().bind(listProperty);
                            listView.prefHeightProperty().bind(columnsBox.heightProperty());

                            columnsBox.getChildren().clear();
                            columnsBox.getChildren().add(listView);

                            queryDB = db;
                            querySchema = schema;
                            queryTable = table;
                        } catch (Exception ex) {
                        }
                    }
                });
    }

    @FXML
    private void acquireDBConnection(ActionEvent event) {
        // Clear any existing message
        message.setText("");

        if (user.getText() == null || user.getText().length() == 0) {
            message.setText("User ID cannot be null !");
            return;
        }

        if (host.getText() == null || host.getText().length() == 0) {
            message.setText("Host/Server Instance cannot be null !");
            return;
        }

        if (AppData.dbSelection.equals("Oracle")) {
            if (password.getText() == null || password.getText().length() == 0) {
                message.setText("Password cannot be null !");
                return;
            }

            if ((service.getText() == null || service.getText().length() == 0) &&
                    (sid.getText() == null || sid.getText().length() == 0)) {
                message.setText("For Oracle, either Service or SID needs to be specified");
                return;
            }
        }

        AppData.user = user.getText();
        AppData.password = password.getText();
        AppData.host = host.getText();
        AppData.service = service.getText();
        AppData.port = port.getText();

        task = new DBConnectionTask();
        logger.debug("Background thread is getting executed to obtain a DB Connection.");
        new Thread(task).start();
    }

    @FXML
    private void selectAllColumns(ActionEvent event) {
        listProperty.forEach((checkBox -> {
            checkBox.setSelected(true);
        }));
    }

    @FXML
    private void deselectAllColumns(ActionEvent event) {
        listProperty.forEach((checkBox -> {
            checkBox.setSelected(false);
        }));
    }

    @FXML
    private void fetchData(ActionEvent event) {
        StringBuilder selectPart = new StringBuilder();

        listProperty.forEach((checkBox -> {
            if (checkBox.isSelected())
                selectPart.append(",")
                        .append(checkBox.getText());
        }));

        selectPart.deleteCharAt(0);

        StringBuilder fromPart = new StringBuilder();
        fromPart.append(" FROM ");

        // For SQL Server, add the Database name while fetching data.
        if (AppData.dbSelection.equals(AppData.SQL_SERVER))
            fromPart.append(queryDB)
                    .append(".");

        fromPart.append(querySchema)
                .append(".")
                .append(queryTable);

        StringBuilder predicates = new StringBuilder();

        String query;

        if (isUniqueValuesChecked.isSelected())
            query = "SELECT DISTINCT" + " " + selectPart + " " + fromPart;
        else
            query = "SELECT  " + selectPart + " " + fromPart;

        System.out.println(query);

        progressIndicator.setVisible(true);
        message.setText("");

        ResultSet resultSet;
        ObservableList<ObservableList<String>> queryResultData = FXCollections.observableArrayList();

        // Clear existing query results
        if (queryResult.getColumns().size() > 0)
            queryResult.getColumns().clear();

        // Execute the Query in the Background
        Task<Long> task = new RefreshQueryResultsTask(connection, query, queryResult, progressIndicator, message);
        new Thread(task).start();
    }

    @FXML
    private void cancelQuery(ActionEvent event) {
    }

    @FXML
    private void searchForTable(ActionEvent event) {
    }

    @FXML
    public void identifyUserSelection(ActionEvent event) {
        resetFields();
        String userSelection = databaseOptions.getSelectionModel().getSelectedItem().toString();
        AppData.dbSelection = userSelection;

        switch (userSelection) {
            case "Oracle":
                service.setDisable(false);
                password.setDisable(false);
                sid.setDisable(true);
                port.setText("1521");
                break;

            case "SQL Server":
                password.setDisable(true);
                service.setDisable(true);
                sid.setDisable(true);
                port.setText("1433");

                user.setText("ryand");
                host.setText("DESKTOP-0NS7D55\\SQLEXPRESS");
                port.setText("65203");
                break;

            default:
                connectBtn.setDisable(true);
                port.setText("");
        }
    }

    private void resetFields() {
        user.setText("");
        password.setText("");
        host.setText("");
        service.setText("");
        sid.setText("");
        port.setText("");
    }

    /**
     * This is the body of the Background task to get a Database connection.
     */
    class DBConnectionTask extends Task<Connection> {

        @Override
        protected Connection call() {
            Platform.runLater(() -> message.setText(""));
            Platform.runLater(() -> progressIndicator.setVisible(true));
            Connection conn = null;

            try {
                switch (AppData.dbSelection) {
                    case AppData.ORACLE:
                        conn = DBConnections.getOracleConnection(AppData.user, AppData.password, AppData.host, AppData.service, AppData.port);
                        break;

                    case AppData.SQL_SERVER:
                        conn = DBConnections.getSqlServerConnection(AppData.user, AppData.host, AppData.port);
                        break;
                }
            } catch (Exception e) {
                final String errorMessage = "Unable to connect to the DB using " + AppData.user + ";" + e.getMessage();
                Platform.runLater(() -> message.setText(errorMessage));
                logger.debug(errorMessage);
                throw new RuntimeException("Unable to connect to the DB using " + AppData.user);
            }

            Platform.runLater(() -> message.setText("Database Connection acquired ! now, fetching DB metadata !"));
            Platform.runLater(() -> connectBtn.setDisable(true));
            logger.debug("DB Connection acquired.");

            connection = conn;
            fetchDBMetadata();
            return conn;
        }

        /**
         * This gets executed only when the call() method above executes without any issues.
         */
        @Override
        public void succeeded() {
            super.succeeded();
            // connection = task.getValue();
        }

        /**
         * This gets executed after the execution of call() method irrespective of its success/failure. It hides the
         * Progress Indicator.
         */
        @Override
        protected void done() {
            super.done();
            Platform.runLater(() -> progressIndicator.setVisible(false));
        }
    }

    private void fetchDBMetadata() {
        Map<String, Map<String, Map<TableDetail, List<ColumnDetail>>>> tables = new HashMap<>();

        try {
            switch (AppData.dbSelection) {
                case AppData.ORACLE:
                    tables = OracleMetadata.getAllTables(connection);
                    break;

                case AppData.SQL_SERVER:
                    tables = SqlServerMetadata.getAllTables(connection);
                    break;
            }

            AppData.tables = tables;
            List<String> databaseNames = new ArrayList(tables.keySet());

            // Host name will be the Root of the tree.
            Node serverIcon = new ImageView(new Image(new File("resources/images/" + "server.png").toURI().toURL().toString(), 16, 16, true, true));
            TreeItem<String> rootNode = new TreeItem<String>(AppData.host, serverIcon);
            rootNode.setExpanded(true);

            // Add Database names.
            Map<String, Map<String, Map<TableDetail, List<ColumnDetail>>>> finalTables = tables;

            for (String db : databaseNames) {
                Node databaseIcon = new ImageView(new Image(new File("resources/images/" + "database.png").toURI().toURL().toString(), 16, 16, true, true));
                TreeItem<String> dbItem = new TreeItem<>(db, databaseIcon);
                dbItem.setExpanded(true);

                for (String schema : finalTables.get(db).keySet()) {
                    TreeItem<String> schemaItem = new TreeItem<>(schema);
                    schemaItem.setExpanded(false);

                    for (TableDetail table : finalTables.get(db).get(schema).keySet()) {
                        Node tableIcon = new ImageView(new Image(new File("resources/images/" + "table.png").toURI().toURL().toString(), 16, 16, true, true));
                        TreeItem<String> tableItem = new TreeItem<>(table.getTable(), tableIcon);
                        schemaItem.getChildren().add(tableItem);
                    }

                    dbItem.getChildren().add(schemaItem);
                }
                rootNode.getChildren().add(dbItem);
            }

            Platform.runLater(() -> objectBrowser.setRoot(rootNode));
        } catch (Exception ex) {
            logStackTrace(ex);
        }
    }
}