/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.vaadin.data.HierarchyData;
import com.vaadin.data.HierarchyData.HierarchyDataBuilder;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.HierarchicalDataCommunicator;
import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.InMemoryHierarchicalDataProvider;
import com.vaadin.shared.ui.treegrid.NodeCollapseRpc;
import com.vaadin.shared.ui.treegrid.TreeGridState;

/**
 * A grid component for displaying hierarchical tabular data.
 *
 * @author Vaadin Ltd
 * @since 8.1
 *
 * @param <T>
 *            the grid bean type
 */
public class TreeGrid<T> extends Grid<T> {

    public TreeGrid() {
        super(new HierarchicalDataCommunicator<>());

        registerRpc(new NodeCollapseRpc() {
            @Override
            public void toggleCollapse(String rowKey, int rowIndex,
                    boolean collapse) {
                if (collapse) {
                    getDataCommunicator().doCollapse(rowKey, rowIndex);
                } else {
                    getDataCommunicator().doExpand(rowKey, rowIndex);
                }
            }
        });
    }

    @Override
    public void setItems(Collection<T> items) {
        setDataProvider(new InMemoryHierarchicalDataProvider<>(
                HierarchyData.<T> builder().addItems(null, items)));
    }

    @Override
    public void setItems(Stream<T> items) {
        setDataProvider(new InMemoryHierarchicalDataProvider<>(
                HierarchyData.<T> builder().addItems(null, items)));
    }

    @Override
    public void setItems(T... items) {
        setDataProvider(new InMemoryHierarchicalDataProvider<>(
                HierarchyData.<T> builder().addItems(null, items)));
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        if (!(dataProvider instanceof HierarchicalDataProvider)) {
            throw new IllegalArgumentException(
                    "TreeGrid only accepts hierarchical data providers");
        }
        super.setDataProvider(dataProvider);
    }

    /**
     * Set the column that displays the hierarchy of this grid's data. By
     * default the hierarchy will be displayed in the first column.
     * <p>
     * Setting a hierarchy column by calling this method also sets the column to
     * be visible and not hidable.
     *
     * @see Column#setId(String)
     *
     * @param id
     *            id of the column to use for displaying hierarchy
     */
    public void setHierarchyColumn(String id) {
        Objects.requireNonNull(id, "id may not be null");
        if (getColumn(id) == null) {
            throw new IllegalArgumentException("No column found for given id");
        }
        getColumn(id).setHidden(false);
        getColumn(id).setHidable(false);
        getState().hierarchyColumnId = getInternalIdForColumn(getColumn(id));
    }

    @Override
    protected TreeGridState getState() {
        return (TreeGridState) super.getState();
    }

    @Override
    protected TreeGridState getState(boolean markAsDirty) {
        return (TreeGridState) super.getState(markAsDirty);
    }

    @Override
    public HierarchicalDataCommunicator<T> getDataCommunicator() {
        return (HierarchicalDataCommunicator<T>) super.getDataCommunicator();
    }

    @Override
    public HierarchicalDataProvider<T, ?> getDataProvider() {
        DataProvider<T, ?> dataProvider = super.getDataProvider();
        // FIXME DataCommunicator by default has a CallbackDataProvider if no
        // DataProvider is set, resulting in a class cast exception if we don't
        // check it here.

        // Once fixed, remove this method from the exclude list in
        // StateGetDoesNotMarkDirtyTest
        if (!(dataProvider instanceof HierarchicalDataProvider)) {
            throw new IllegalStateException("No data provider has been set.");
        }
        return (HierarchicalDataProvider<T, ?>) dataProvider;
    }

    @Override
    protected void readData(Element body,
            List<DeclarativeValueProvider<T>> providers) {
        getSelectionModel().deselectAll();
        List<T> items = new ArrayList<>();
        List<T> selectedItems = new ArrayList<>();
        HierarchyDataBuilder<T> builder = HierarchyData.builder();

        for (Element row : body.children()) {
            T item = deserializeDeclarativeRepresentation(row.attr("item"));
        }

        for (Element row : body.children()) {
            T item = deserializeDeclarativeRepresentation(row.attr("item"));
            items.add(item);
            if (row.hasAttr("selected")) {
                selectedItems.add(item);
            }
            Elements cells = row.children();
            int i = 0;
            for (Element cell : cells) {
                providers.get(i).addValue(item, cell.html());
                i++;
            }
        }

        setItems(items);
        selectedItems.forEach(getSelectionModel()::select);
    }

    private void readDataRecursive(Element item,
            List<DeclarativeValueProvider<T>> providers, List<T> items,
            List<T> selectedItems) {


    }
}
