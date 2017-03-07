package com.vaadin.tests.components.treegrid;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HierarchyData;
import com.vaadin.data.HierarchyData.HierarchyDataBuilder;
import com.vaadin.data.provider.InMemoryHierarchicalDataProvider;
import com.vaadin.tests.components.AbstractComponentTest;
import com.vaadin.ui.TreeGrid;

@Theme("valo")
@Widgetset("com.vaadin.DefaultWidgetSet")
public class TreeGridBasicFeatures extends AbstractComponentTest<TreeGrid> {

    private TreeGrid<HierarchicalTestBean> grid;
    private InMemoryHierarchicalDataProvider<HierarchicalTestBean> inMemoryDataProvider;

    @Override
    public TreeGrid getComponent() {
        return grid;
    }

    @Override
    protected Class<TreeGrid> getTestClass() {
        return TreeGrid.class;
    }

    @Override
    protected void initializeComponents() {
        initializeInMemoryDataProvider();
        grid = new TreeGrid<>();
        grid.setSizeFull();
        grid.addColumn(HierarchicalTestBean::toString).setCaption("String")
                .setId("string");
        grid.addColumn(HierarchicalTestBean::getDepth).setCaption("Depth")
                .setId("depth");
        grid.addColumn(HierarchicalTestBean::getIndex)
                .setCaption("Index on this depth").setId("index");
        grid.setHierarchyColumn("string");
        grid.setDataProvider(inMemoryDataProvider);
        // grid.setDataProvider(new LazyHierarchicalDataProvider(100, 3));

        grid.setId("testComponent");
        addTestComponent(grid);
    }

    @Override
    protected void createActions() {
        super.createActions();

        createHierarchyColumnSelect();
    }

    private void initializeInMemoryDataProvider() {
        HierarchyDataBuilder<HierarchicalTestBean> builder = HierarchyData
                .builder();

        List<Integer> ints = Arrays.asList(1, 2, 3);

        ints.stream().forEach(index -> {
            HierarchicalTestBean parentBean = new HierarchicalTestBean(0,
                    index);
            builder.addItem(null, parentBean);
            ints.stream()
                    .forEach(childIndex -> builder.addItem(
                            parentBean,
                            new HierarchicalTestBean(1, childIndex)));
        });

        inMemoryDataProvider = new InMemoryHierarchicalDataProvider<>(
                builder.build());
    }

    private void createHierarchyColumnSelect() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        grid.getColumns().stream()
                .forEach(column -> options.put(column.getId(), column.getId()));

        createSelectAction("Set hierarchy column", CATEGORY_FEATURES, options,
                grid.getColumns().get(0).getId(),
                (treeGrid, value, data) -> treeGrid.setHierarchyColumn(value));
    }

    static class HierarchicalTestBean {

        private final int depth;
        private final int index;

        public HierarchicalTestBean(int level, int index) {
            depth = level;
            this.index = index;
        }

        public int getDepth() {
            return depth;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return depth + " | " + index;
        }
    }
}
