/**
 * Copyright (c) 2018 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package org.jmad.modelpack.gui.panes;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jmad.modelpack.domain.ModelPackage;
import org.jmad.modelpack.domain.ModelPackageVariant;
import org.jmad.modelpack.domain.ModelPackages;
import org.jmad.modelpack.domain.Variant;
import org.jmad.modelpack.service.JMadModelPackageService;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;

import freetimelabs.io.reactorfx.flux.FxFlux;
import freetimelabs.io.reactorfx.schedulers.FxSchedulers;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;

public class ModelPackagesPane extends BorderPane {

    private final JMadModelPackageService packageService;
    private final PackageFilterModel filterModel = new PackageFilterModel();
    private Button refreshButton;
    private TreeItem<PackageLine> root = new TreeItem<>(new PackageLine());

    private SetMultimap<ModelPackage, ModelPackageVariant> map = TreeMultimap
            .create(Comparator.comparing(ModelPackage::name), ModelPackages.packageVariantComparator());

    public ModelPackagesPane(JMadModelPackageService packageService, PackageSelectionModel selectionModel) {
        this.packageService = requireNonNull(packageService, "packageService must not be null");

        TreeTableColumn<PackageLine, String> packageColumn = new TreeTableColumn<>("package");
        packageColumn.setPrefWidth(250);
        packageColumn.setCellValueFactory(param -> param.getValue().getValue().packageNameProperty());

        TreeTableColumn<PackageLine, String> variantColumn = new TreeTableColumn<>("variant");
        variantColumn.setPrefWidth(200);
        variantColumn.setCellValueFactory(param -> param.getValue().getValue().variantProperty());

        TreeTableView<PackageLine> treeTableView = new TreeTableView<>(root);
        treeTableView.setShowRoot(false);
        treeTableView.getColumns().setAll(packageColumn, variantColumn);

        selectionModel.selectedPackageProperty().bind(Bindings.createObjectBinding(() -> {
            TreeItem<PackageLine> treeItem = treeTableView.getSelectionModel().selectedItemProperty().get();
            if (treeItem == null) {
                return null;
            }
            return treeItem.getValue().modelPackageVariant;
        }, treeTableView.getSelectionModel().selectedItemProperty()));

        setCenter(treeTableView);
        setLeft(new PackageFilterPane(filterModel));

        refreshButton = new Button("refresh");
        setBottom(refreshButton);

        update();

        // @formatter:off
        FxFlux.from(refreshButton)
                .subscribeOn(FxSchedulers.fxThread())
                .subscribe(o -> this.update());
        // @formatter:on

        this.filterModel.predicateProperty().addListener((p, oldVal, newVal) -> {
            refresh();
        });
    }

    private void update() {
        // this.refreshButton.setDisable(true);
        this.clear();
        // @formatter:off
        this.packageService.availablePackages()
                .publishOn(FxSchedulers.fxThread())
                .subscribe(l -> add(l));
        // @formatter:on
    }

    private void add(ModelPackageVariant line) {
        this.map.put(line.modelPackage(), line);
        refresh();
    }

    private void refresh() {
        List<TreeItem<PackageLine>> treeItems = treeItemsFor(this.map, filterModel.predicateProperty().get());
        this.root.getChildren().setAll(treeItems);
    }

    private List<TreeItem<PackageLine>> treeItemsFor(SetMultimap<ModelPackage, ModelPackageVariant> map2,
            Predicate<ModelPackageVariant> filter) {
        // @formatter:off
        return map2.keySet().stream().map(k -> {
            Set<ModelPackageVariant> packages = map2.get(k);
            List<PackageLine> itemsForPackage = packages.stream().filter(filter).map(PackageLine::new).collect(Collectors.toList());
            return itemsForPackage;
        }).filter(l -> !l.isEmpty())
          .map(l -> {
            TreeItem<PackageLine> toItem = new TreeItem<>(l.get(0));
            List<TreeItem<PackageLine>> children = l.subList(1, l.size()).stream().map(TreeItem::new).collect(toList());
            toItem.getChildren().setAll(children);
            return toItem;
        })
        .collect(Collectors.toList());
        // @formatter:on
    }

    private void clear() {
        this.map.clear();
        this.root.getChildren().clear();
    }

    private static class PackageLine {

        private final ModelPackageVariant modelPackageVariant;
        private final StringProperty packageName = new SimpleStringProperty();
        private final StringProperty variant = new SimpleStringProperty();

        private PackageLine(ModelPackageVariant variant) {
            this.modelPackageVariant = Objects.requireNonNull(variant, "modelPackageVariant must not be null");
            this.packageName.set(variant.modelPackage().name());
            this.variant.set(stringFor(variant.variant()));
        }

        private static String stringFor(Variant variant) {
            return variant.getClass().getSimpleName().toLowerCase() + ": " + variant.name();
        }

        private PackageLine() {
            this.modelPackageVariant = null;
            /* empty strings */
        }

        private StringProperty packageNameProperty() {
            return this.packageName;
        }

        private StringProperty variantProperty() {
            return this.variant;
        }

    }

}
