package com.vaadin.data.provider;

import java.util.stream.Stream;

import com.vaadin.data.HierarchyData;
import com.vaadin.data.HierarchyData.HierarchyDataBuilder;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;

public class InMemoryHierarchicalDataProvider<T>
        implements HierarchicalDataProvider<T, SerializablePredicate<T>> {

    private final HierarchyData<T> hierarchyData;

    public InMemoryHierarchicalDataProvider(HierarchyData<T> hierarchyData) {
        this.hierarchyData = hierarchyData;
    }

    public InMemoryHierarchicalDataProvider(HierarchyDataBuilder<T> builder) {
        hierarchyData = builder.build();
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public void refreshItem(T item) {
        // NO-OP
    }

    @Override
    public void refreshAll() {
        // NO-OP
    }

    @Override
    public Registration addDataProviderListener(
            DataProviderListener<T> listener) {
        return () -> {
        };
    }

    @Override
    public boolean hasChildren(T item) {
        return !hierarchyData.getChildren(item).isEmpty();
    }

    @Override
    public int getChildCount(
            HierarchicalQuery<T, SerializablePredicate<T>> query) {
        return hierarchyData.getChildren(query.getParent()).size();
    }

    @Override
    public Stream<T> fetchChildren(
            HierarchicalQuery<T, SerializablePredicate<T>> query) {
        Stream<T> childStream = hierarchyData.getChildren(query.getParent())
                .stream();
        return query.getFilter().map(childStream::filter).orElse(childStream)
                .skip(query.getOffset()).limit(query.getLimit());
    }
}