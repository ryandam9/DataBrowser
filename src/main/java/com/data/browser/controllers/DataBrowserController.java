package com.data.browser.controllers;

import com.data.browser.AppData;
import com.data.browser.ui.RefreshQueryResultsTask;
import com.data.browser.ui.TreeViewEntry;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.MalformedURLException;
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

        recordsToFetch.setText("10000");

        // Only one entry can be selected at a time
        objectBrowser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // When a table is clicked, its columns should be printed in a List. Each Column is represented as a
        // Check box.
        objectBrowser.getSelectionModel()
                .selectedItemProperty()
                .addListener(new ChangeListener<TreeViewEntry<String>>() {
                    @Override
                    public void changed(
                            ObservableValue<? extends TreeViewEntry<String>> observable,
                            TreeViewEntry<String> old_val,
                            TreeViewEntry<String> new_val) {

                        String db = null;
                        String schema = null;
                        String table = null;

                        TreeViewEntry<String> selectedItem = new_val;

                        String type = selectedItem.getType();
                        String parentItem =  selectedItem.getParentItem();

                        switch (type) {
                            case "root":
                                break;

                            case "database":
                                break;

                            case "schema":
                                db = selectedItem.getParentItem();
                                schema = selectedItem.getValue();

                                // If the schema already has children, don't fetch tables again.
                                if (selectedItem.getChildren().size() > 0)
                                    return;

                                Thread t = new Thread(new FetchTableMetadataTask(db, schema, selectedItem));
                                t.start();
                                break;

                            case "table":
                                db = selectedItem.getParent().getParent().getValue();
                                schema = selectedItem.getParentItem();
                                table = selectedItem.getValue();

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
                                            columnCheckBox.setMnemonicParsing(false);
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
                                break;
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

        // Produces comma separated list of column names to be fetched.
        listProperty.forEach((checkBox -> {
            if (checkBox.isSelected())
                selectPart.append(",")
                        .append(checkBox.getText());
        }));

        if (selectPart.toString().length() == 0) {
            message.setText("Select At least one column");
            return;
        }

        // Remove the first ","
        selectPart.deleteCharAt(0);

        StringBuilder fromPart = new StringBuilder();
        fromPart.append(" FROM ");

        // For SQL Server, add the Database name while fetching data.
        if (AppData.dbSelection.equals(AppData.SQL_SERVER)) {
            fromPart.append(queryDB)
                    .append(".");

            selectPart.insert(0, " TOP " + recordsToFetch.getText() + " ");
        }

        fromPart.append(querySchema)
                .append(".")
                .append(queryTable);

        StringBuilder predicates = new StringBuilder();
        String query;

        if (isUniqueValuesChecked.isSelected())
            query = "SELECT DISTINCT" + " " + selectPart + " " + fromPart;
        else
            query = "SELECT  " + selectPart + " " + fromPart;

        // For Oracle
        if (AppData.dbSelection.equals(AppData.ORACLE)) {
            query = query + " " + "WHERE ROWNUM < " + recordsToFetch.getText() + " ";

            if (valuesToIgnore.getText().length() > 0)
                query += " AND " + valuesToIgnore.getText();
        }

        if (AppData.dbSelection.equals(AppData.SQL_SERVER)) {
            if (valuesToIgnore.getText().length() > 0)
                query += " WHERE " + valuesToIgnore.getText();
        }

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
            Platform.runLater(() -> password.setText(""));

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
        List<String> databases = new ArrayList<>();
        List<String> schemas = new ArrayList<>();

        Map<String, List<String>> dbSchemas = new HashMap<>();

        switch (AppData.dbSelection) {
            case AppData.ORACLE:
                // First get all Database names
                try {
                    databases = OracleMetadata.getAllDatabases(connection);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // For each DB, fetch the Schemas.
                for (String db : databases) {
                    try {
                        // Update application level dictionary for later use
                        AppData.tables.put (db, new HashMap<>());

                        schemas = OracleMetadata.getAllSchemas(connection, db);
                        dbSchemas.put(db, schemas);

                        // Update the application level dictionary with all the Schemas.
                        for(String schema: schemas) {
                            AppData.tables.get(db).put(schema, new HashMap<>());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;

            case AppData.SQL_SERVER:
                // First get all Database names
                try {
                    databases = SqlServerMetadata.getAllDatabases(connection);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // For each DB, fetch the Schemas.
                for (String db : databases) {
                    try {
                        // Update application level dictionary for later use
                        AppData.tables.put (db, new HashMap<>());

                        schemas = SqlServerMetadata.getAllSchemas(connection, db);
                        dbSchemas.put(db, schemas);

                        // Update the application level dictionary with all the Schemas.
                        for(String schema: schemas) {
                            AppData.tables.get(db).put(schema, new HashMap<>());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;
        }

        try {
            // Host name will be the Root of the tree.
            Node serverIcon = new ImageView(new Image(new File("resources/images/" + "server.png").toURI().toURL().toString(), 16, 16, true, true));
            TreeViewEntry rootNode = new TreeViewEntry("root", null, AppData.host, serverIcon);
            rootNode.setExpanded(true);

            List<String> sortedDBs = new ArrayList<>(dbSchemas.keySet());
            Collections.sort(sortedDBs);

            for (String db : sortedDBs) {
                Node databaseIcon = new ImageView(new Image(new File("resources/images/" + "database-green.png").toURI().toURL().toString(), 16, 16, true, true));
                TreeViewEntry dbItem = new TreeViewEntry("database", AppData.host, db, databaseIcon);
                dbItem.setExpanded(true);

                for (String schema : dbSchemas.get(db)) {
                    Node schemaIcon = new ImageView(new Image(new File("resources/images/" + "schema.png").toURI().toURL().toString(), 16, 16, true, true));
                    TreeViewEntry schemaItem = new TreeViewEntry("schema", db, schema, schemaIcon);
                    schemaItem.setExpanded(true);
                    dbItem.getChildren().add(schemaItem);
                }
                rootNode.getChildren().add(dbItem);
            }
            Platform.runLater(() -> objectBrowser.setRoot(rootNode));
        } catch (MalformedURLException ex) {
            logStackTrace(ex);
        }
    }

    class FetchTableMetadataTask extends Task<Integer> {
        private String db;
        private String schema;
        private TreeViewEntry parent;

        private FetchTableMetadataTask(String db, String schema, TreeViewEntry parent) {
            this.db = db;
            this.schema = schema;
            this.parent = parent;
        }

        @Override
        protected Integer call() throws Exception {
            Platform.runLater(() -> progressIndicator.setVisible(true));
            Map<TableDetail, List<ColumnDetail>> tables = new HashMap<>();
            int tableCount = 0;

            switch (AppData.dbSelection) {
                case AppData.ORACLE:
                    tables = OracleMetadata.getAllTables(connection, db, schema);
                    break;

                case AppData.SQL_SERVER:
                    tables = SqlServerMetadata.getAllTables(connection, db, schema);
                    break;
            }

            ObservableList<TreeViewEntry<String>> tablesInThisSchema = FXCollections.observableArrayList();

            // To Sort table names
            List<TableDetail> unorderedTables = new ArrayList<>(tables.keySet());
            Collections.sort(unorderedTables, new Comparator<TableDetail>() {
                                        @Override
                                        public int compare(TableDetail o1, TableDetail o2) {
                                            return o1.getTable().toLowerCase().compareTo(o2.getTable().toLowerCase());
                                        }
                                    });

            for(TableDetail tableDetail: unorderedTables) {
                Node tableIcon = new ImageView(new Image(new File("resources/images/" + "table.png").toURI().toURL().toString(), 16, 16, true, true));
                TreeViewEntry tableItem = new TreeViewEntry("table", schema, tableDetail.getTable(), tableIcon);
                tablesInThisSchema.add(tableItem);
                tableCount++;

                // Update the App level Dictionary
                AppData.tables.get(db).get(schema).put(tableDetail, tables.get(tableDetail));

                logger.debug(AppData.tables.get(db).get(schema).get(tableDetail));
            }

            Platform.runLater(() -> parent.getChildren().addAll(tablesInThisSchema));
            return tableCount;
        }

        @Override
        protected void done() {
            super.done();
            Platform.runLater(() -> progressIndicator.setVisible(false));
        }
    }
}