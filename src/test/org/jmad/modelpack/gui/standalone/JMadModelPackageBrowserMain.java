/**
 * Copyright (c) 2018 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package org.jmad.modelpack.gui.standalone;

import static javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE;

import java.util.Optional;

import org.jmad.modelpack.gui.panes.JMadModelDefinitionSelectionPane;
import org.jmad.modelpack.gui.panes.ModelPackagesPane;
import org.jmad.modelpack.gui.panes.ModelRepositoryPane;
import org.jmad.modelpack.gui.panes.PackageSelectionModel;
import org.jmad.modelpack.gui.panes.SelectedModelConfiguration;
import org.jmad.modelpack.service.JMadModelPackageService;
import org.jmad.modelpack.service.ModelPackageRepositoryManager;
import org.jmad.modelpack.service.conf.JMadModelPackageServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import cern.accsoft.steering.jmad.domain.machine.RangeDefinition;
import cern.accsoft.steering.jmad.model.JMadModelStartupConfiguration;
import cern.accsoft.steering.jmad.modeldefs.domain.JMadModelDefinition;
import cern.accsoft.steering.jmad.modeldefs.domain.OpticsDefinition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

@Configuration
@Import(value = JMadModelPackageServiceConfiguration.class)
public class JMadModelPackageBrowserMain extends Application{

    private static final Logger LOGGER = LoggerFactory.getLogger(JMadModelPackageBrowserMain.class);
    private static final String MAIN_NODE_NAME = "main_fx_node";

    @Bean
    public Node packageBrowser(JMadModelPackageService packageService, PackageSelectionModel packageSelectionModel) {
        return new ModelPackagesPane(packageService, packageSelectionModel);
    }

    @Bean
    public Node fullSelectionPane(Node packageBrowser, PackageSelectionModel jmadModelDefinitionSelectionModel) {
        JMadModelDefinitionSelectionPane selectionPane = new JMadModelDefinitionSelectionPane(
                jmadModelDefinitionSelectionModel);
        HBox pane = new HBox(packageBrowser, selectionPane);
        pane.setFillHeight(true);
        pane.setAlignment(Pos.TOP_CENTER);
        return pane;
    }

    @Bean
    public PackageSelectionModel packageSelectionModel(JMadModelPackageService packageService) {
        return new PackageSelectionModel(packageService);
    }

    @Bean
    public ModelRepositoryPane repoListView(ModelPackageRepositoryManager manager) {
        return new ModelRepositoryPane(manager);
    }

    @Bean
    public Dialog<SelectedModelConfiguration> modelDefinitionSelectionDialog(Node fullSelectionPane,
            ModelRepositoryPane repoListView, PackageSelectionModel selectionModel) {
        Dialog<SelectedModelConfiguration> dialog = new Dialog<>();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(UNAVAILABLE);
        tabPane.getTabs().add(new Tab("models", fullSelectionPane));
        tabPane.getTabs().add(new Tab("repos", repoListView));

        dialog.getDialogPane().setContent(tabPane);

        ButtonType buttonTypeOk = new ButtonType("Ok", ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        dialog.setResultConverter(new Callback<ButtonType, SelectedModelConfiguration>() {
            @Override
            public SelectedModelConfiguration call(ButtonType b) {
                if (b == buttonTypeOk) {
                    JMadModelDefinition modelDefinition = selectionModel.selectedModelDefinitionProperty().get();
                    if (modelDefinition == null) {
                        return null;
                    }
                    
                    OpticsDefinition opticsDefinition = selectionModel.selectedOpticsProperty().get();
                    RangeDefinition rangeDefinition = selectionModel.selectedRangeProperty().get();

                    JMadModelStartupConfiguration startupConfiguration = new JMadModelStartupConfiguration();
                    startupConfiguration.setInitialOpticsDefinition(opticsDefinition);
                    startupConfiguration.setInitialRangeDefinition(rangeDefinition);

                    return new SelectedModelConfiguration(modelDefinition, startupConfiguration);
                } else {
                    return null;
                }
            }
        });

        return dialog;
    }

    @Bean(MAIN_NODE_NAME)
    public BorderPane view(Dialog<SelectedModelConfiguration> modelDefinitionSelectionDialog) {
        BorderPane pane = new BorderPane();
        Button button = new Button("select model");
        button.setOnAction((evt) -> {
            modelDefinitionSelectionDialog.setWidth(1000);
            modelDefinitionSelectionDialog.setHeight(700);
            modelDefinitionSelectionDialog.setResizable(true);
            Optional<SelectedModelConfiguration> result = modelDefinitionSelectionDialog.showAndWait();
            if (result.isPresent()) {
                LOGGER.info("Selected model configuration: {}", result.get());
            } else {
                LOGGER.info("No model configuration selected.");
            }

        });
        pane.setCenter(button);
        return pane;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(JMadModelPackageBrowserMain.class);

        Parent mainNode = ctx.getBean(MAIN_NODE_NAME, Parent.class);

        Scene scene = new Scene(mainNode);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(JMadModelPackageBrowserMain.class);
    }
}