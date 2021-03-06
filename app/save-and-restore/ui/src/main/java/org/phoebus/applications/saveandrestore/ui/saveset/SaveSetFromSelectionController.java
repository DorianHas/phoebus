package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.preferences.PreferencesReader;

import java.net.URL;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class SaveSetFromSelectionController implements Initializable {

    private final SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");
    private final PreferencesReader preferencesReader = (PreferencesReader) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("preferencesReader");

    private final Logger LOGGER = Logger.getLogger(SaveAndRestoreService.class.getName());

    private final String DESCRIPTION_PROPERTY = "description";

    private final SimpleIntegerProperty numSelected = new SimpleIntegerProperty();

    private class TableRowEntry {
        private boolean selected;
        private ConfigPv pv;
    }

    private static final DateTimeFormatter savesetTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML
    private TextField locationTextField;

    @FXML
    private Button browseButton;

    @FXML
    private TextField saveSetName;

    @FXML
    private TextArea description;

    @FXML
    private Label numSelectedLabel;

    @FXML
    private Label numTotalLabel;

    @FXML
    private TableView<TableRowEntry> pvTable;

    @FXML
    private TableColumn<TableRowEntry, Boolean> selectColumn;

    @FXML
    private TableColumn<TableRowEntry, String> pvNameColumn;

    @FXML
    private TableColumn<TableRowEntry, String> readbackPvName;

    @FXML
    private TableColumn<TableRowEntry, Boolean> readOnlyColumn;

    @FXML
    private Button saveButton;

    @FXML
    private Button discardButton;

    private SimpleObjectProperty<Node> targetNode = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        targetNode.addListener((observableValue, node, newNode) -> {
            if (newNode != null) {
                try {
                    if (newNode.getNodeType() == NodeType.CONFIGURATION) {
                        saveSetName.setText(newNode.getName());
                        description.setText(newNode.getProperty("description"));

                        saveSetName.setEditable(false);
                        description.setEditable(true);

                        createLocationText(saveAndRestoreService.getParentNode(newNode.getUniqueId()));
                    } else {
                        saveSetName.setText("");
                        description.setText("");

                        saveSetName.setEditable(true);
                        description.setEditable(true);

                        createLocationText(newNode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        targetNode.set(saveAndRestoreService.getChildNodes(saveAndRestoreService.getRootNode()).get(0));

        browseButton.setOnAction(action -> {
            try {
                SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();

                Stage dialog = new Stage();
                dialog.setTitle("Choose a folder, a saveset, or create one");
                dialog.initModality(Modality.APPLICATION_MODAL);
                if (preferencesReader.getBoolean("splitSaveset")) {
                    dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetSelectorWithSplit.fxml")));
                } else {
                    dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetSelector.fxml")));
                }
                dialog.showAndWait();

                final ISelectedNodeProvider saveSetSelectionController = springFxmlLoader.getLoader().getController();
                final Node selectedNode = saveSetSelectionController.getSelectedNode();
                if (selectedNode != null) {
                    targetNode.set(selectedNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveSetName.setPromptText(savesetTimeFormat.format(Instant.now()));

        description.setPromptText("Saveset created at " + savesetTimeFormat.format(Instant.now()));

        selectColumn.setReorderable(false);
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setCellValueFactory(cell -> {
            final SimpleBooleanProperty selected = new SimpleBooleanProperty(cell.getValue().selected);
            selected.addListener((observable, oldValue, newValue) -> {
                cell.getValue().selected = newValue;
                numSelected.setValue(numSelected.getValue() + (newValue ? 1 : -1));
            });
            return selected;
        });

        pvNameColumn.setReorderable(false);
        pvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        pvNameColumn.setCellValueFactory(cell -> {
            final SimpleStringProperty pvName = new SimpleStringProperty(cell.getValue().pv.getPvName());
            pvName.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setPvName(newValue));
            return pvName;
        });

        readbackPvName.setReorderable(false);
        readbackPvName.setCellFactory(TextFieldTableCell.forTableColumn());
        readbackPvName.setCellValueFactory(cell -> {
            final SimpleStringProperty readbackPvName = new SimpleStringProperty(cell.getValue().pv.getReadbackPvName());
            readbackPvName.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setReadbackPvName(newValue));
            return readbackPvName;
        });

        readOnlyColumn.setReorderable(false);
        readOnlyColumn.setCellFactory(CheckBoxTableCell.forTableColumn(readOnlyColumn));
        readOnlyColumn.setCellValueFactory(cell -> {
            final SimpleBooleanProperty readOnly = new SimpleBooleanProperty(cell.getValue().pv.isReadOnly());
            readOnly.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setReadOnly(newValue));
            return readOnly;
        });

        discardButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText("No information is saved if discarded.\nDo you want to proceed?");
            Optional<ButtonType> response = alert.showAndWait();
            response.ifPresent(type -> {
                if (type == ButtonType.OK) {
                    ((Stage) discardButton.getScene().getWindow()).close();
                }
            });
        });

        numSelected.addListener((observableValue, number, newValue) -> numSelectedLabel.setText(NumberFormat.getIntegerInstance().format(newValue)));
    }

    private void createLocationText(Node node) {
        String labelText = node.getName();

        while (true) {
            try {
                Node parentNode = saveAndRestoreService.getParentNode(node.getUniqueId());

                if (parentNode.getName().equals("Root folder")) {
                    break;
                }

                labelText = parentNode.getName() + " ▶ " + labelText;
                node = parentNode;
            } catch (Exception e) {
                String alertMessage = "Cannot retrieve the parent node of node: " + node.getName() + "(" + node.getUniqueId() + ")";

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOGGER.severe(alertMessage);

                e.printStackTrace();
                break;
            }
        }

        locationTextField.setText(labelText);
    }

    public void setSelection(List<ProcessVariable> pvList) {
        for (ProcessVariable pv : pvList) {
            final TableRowEntry rowEntry = new TableRowEntry();
            rowEntry.selected = true;
            rowEntry.pv = ConfigPv.builder()
                    .pvName(pv.getName())
                    .readbackPvName(null)
                    .readOnly(false)
                    .build();
            pvTable.getItems().add(rowEntry);

            numSelected.set(pvList.size());
            numTotalLabel.setText(NumberFormat.getIntegerInstance().format(pvList.size()));
        }
    }

    @FXML
    private void save(ActionEvent ae) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Duplicate PVs are removed automatically.\nAre you sure to save?");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() != ButtonType.OK) {
            return;
        }

        List<ConfigPv> pvs = new ArrayList<>();
        for (TableRowEntry item : pvTable.getItems()) {
            if (item.selected && !item.pv.getPvName().isEmpty()) {
                pvs.add(item.pv);
            }
        }

        Node selectedNode = targetNode.get();
        if (selectedNode.getNodeType() == NodeType.FOLDER) {
            Node newSaveSetBuild = Node.builder()
                    .nodeType(NodeType.CONFIGURATION)
                    .name(saveSetName.getText().trim().isEmpty() ? saveSetName.getPromptText() : saveSetName.getText().trim())
                    .build();

            Node parentNode = selectedNode;

            try {
                Node newSaveSet = saveAndRestoreService.createNode(parentNode.getUniqueId(), newSaveSetBuild);

                newSaveSet.putProperty(DESCRIPTION_PROPERTY, (description.getText().trim().isEmpty() ? description.getPromptText() : description.getText().trim()));
                saveAndRestoreService.updateSaveSet(newSaveSet, pvs);
            } catch (Exception e) {
                String alertMessage = "Cannot save PVs in parent node: " + parentNode.getName() + "(" + parentNode.getUniqueId() + ")";

                alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOGGER.severe(alertMessage);

                e.printStackTrace();
            }
        } else { // NodeType.CONFIGURATION
            Node parentNode = null;
            try {
                parentNode = saveAndRestoreService.getParentNode(selectedNode.getUniqueId());

                List<ConfigPv> storedPvs = saveAndRestoreService.getConfigPvs(selectedNode.getUniqueId());
                Set<ConfigPv> pvSet = new HashSet<ConfigPv>();
                pvSet.addAll(storedPvs);
                pvSet.addAll(pvs);

                selectedNode.removeProperty(DESCRIPTION_PROPERTY);
                selectedNode.putProperty(DESCRIPTION_PROPERTY, (description.getText().trim().isEmpty() ? description.getPromptText() : description.getText().trim()));

                saveAndRestoreService.updateSaveSet(selectedNode, new ArrayList<>(pvSet));
            } catch (Exception e) {
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Cannot save PVs in parent node: " + parentNode.getName() + "(" + parentNode.getUniqueId() + ")");
                alert.show();

                e.printStackTrace();
            }
        }
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
