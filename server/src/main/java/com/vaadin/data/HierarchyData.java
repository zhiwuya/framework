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
package com.vaadin.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 
 * 
 * @author Vaadin Ltd
 * @since 8.1
 *
 * @param <T>
 */
public class HierarchyData<T> implements Serializable {

    private static class HierarchyWrapper<T> implements Serializable {
        private T item;
        private T parent;
        private List<T> children;

        public HierarchyWrapper(T item, T parent) {
            this.item = item;
            this.parent = parent;
            children = new ArrayList<>();
        }

        public T getItem() {
            return item;
        }

        public void setItem(T item) {
            this.item = item;
        }

        public T getParent() {
            return parent;
        }

        public void setParent(T parent) {
            this.parent = parent;
        }

        public List<T> getChildren() {
            return children;
        }

        public void setChildren(List<T> children) {
            this.children = children;
        }

        public void addChild(T child) {
            children.add(child);
        }

        public void removeChild(T child) {
            children.remove(child);
        }
    }

    public static class HierarchyDataBuilder<T> implements Serializable {

        private final HierarchyData<T> hierarchyData = new HierarchyData<>();

        public HierarchyDataBuilder<T> addItem(T parent, T item) {
            if (parent != null && !hierarchyData.contains(parent)) {
                throw new IllegalArgumentException(
                        "Parent needs to be added before children. "
                                + "To add root items, call with parent as null");
            }
            if (hierarchyData.contains(item)) {
                throw new IllegalArgumentException(
                        "Cannot add the same item multiple times: " + item);
            }
            hierarchyData.putItem(item, parent);
            return this;
        }

        public HierarchyDataBuilder<T> addItems(T parent, T... items) {
            Arrays.asList(items).stream()
                    .forEach(item -> addItem(parent, item));
            return this;
        }

        public HierarchyDataBuilder<T> addItems(T parent, Collection<T> items) {
            items.stream().forEach(item -> addItem(parent, item));
            return this;
        }

        public HierarchyDataBuilder<T> addItems(T parent, Stream<T> items) {
            items.forEach(item -> addItem(parent, item));
            return this;
        }

        public HierarchyDataBuilder<T> addItems(
                Map<T, Collection<T>> parentToChildrenMap) {
            parentToChildrenMap.entrySet().stream()
                    .forEach(entry -> entry.getValue().stream()
                            .forEach(item -> addItem(entry.getKey(), item)));
            return this;
        }

        public HierarchyData<T> build() {
            return hierarchyData;
        }
    }

    private final Map<T, HierarchyWrapper<T>> itemToWrapperMap;

    private HierarchyData() {
        itemToWrapperMap = new LinkedHashMap<>();
        itemToWrapperMap.put(null, new HierarchyWrapper<>(null, null));
    }

    public List<T> getChildren(T item) {
        if (!contains(item)) {
            throw new IllegalArgumentException(
                    "Item '" + item + "' not in the hierarchy");
        }
        return itemToWrapperMap.get(item).getChildren();
    }

    private boolean contains(T item) {
        return itemToWrapperMap.containsKey(item);
    }

    private void putItem(T item, T parent) {
        HierarchyWrapper<T> wrappedItem = new HierarchyWrapper<>(item, parent);
        if (itemToWrapperMap.containsKey(parent)) {
            itemToWrapperMap.get(parent).addChild(item);
        }
        itemToWrapperMap.put(item, wrappedItem);
    }

    public static <T> HierarchyDataBuilder<T> builder() {
        return new HierarchyDataBuilder<>();
    }
}
