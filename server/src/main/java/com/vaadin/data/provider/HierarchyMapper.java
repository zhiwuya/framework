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
package com.vaadin.data.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Vaadin Ltd
 * @since
 */
public class HierarchyMapper implements Serializable {

    public static class TreeLevelQuery {
        final TreeNode node;
        final int startIndex;
        final int size;
        final int depth;
        final List<TreeNode> subTrees;

        public TreeLevelQuery(TreeNode node, int startIndex, int size,
                int depth, List<TreeNode> subTrees) {
            this.node = node;
            this.startIndex = startIndex;
            this.size = size;
            this.depth = depth;
            this.subTrees = subTrees;
        }
    }

    public static class TreeNode implements Serializable, Comparable<TreeNode> {

        private final String parentKey;
        private int startIndex;
        private int size;
        private int endIndex;

        public TreeNode(String parentKey, int startIndex, int size) {
            this.parentKey = parentKey;
            this.startIndex = startIndex;
            endIndex = startIndex + size - 1;
            this.size = size;
        }

        public TreeNode(int startIndex) {
            parentKey = "INVALID";
            this.startIndex = startIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public String getParentKey() {
            return parentKey;
        }

        public void push(int offset) {
            startIndex += offset;
            endIndex += offset;
        }

        public void pushEnd(int offset) {
            endIndex += offset;
        }

        @Override
        public int compareTo(TreeNode other) {
            return Integer.valueOf(startIndex).compareTo(other.startIndex);
        }
    }

    private final TreeSet<TreeNode> nodes = new TreeSet<>();

    public void reset(int rootDepthSize) {
        nodes.clear();
        nodes.add(new TreeNode(null, 0, rootDepthSize));
    }

    public int getTreeSize() {
        AtomicInteger count = new AtomicInteger(0);
        nodes.forEach(node -> count.getAndAdd(node.size));
        return count.get();
    }

    public boolean isCollapsed(String itemKey) {
        return !getNodeForKey(itemKey).isPresent();
    }

    public int getDepth(String expandedNodeKey) {
        Optional<TreeNode> node = getNodeForKey(expandedNodeKey);
        if (!node.isPresent()) {
            throw new IllegalArgumentException("No node with given key "
                    + expandedNodeKey + " was expanded.");
        }
        long depth = nodes.headSet(node.get(), false).stream().filter(
                higherNode -> higherNode.startIndex < node.get().startIndex
                        && higherNode.endIndex > node.get().endIndex)
                .count();

        return (int) depth;
    }

    protected Optional<TreeNode> getNodeForKey(String expandedNodeKey) {
        return nodes.stream()
                .filter(node -> Objects.equals(node.parentKey, expandedNodeKey))
                .findAny();
    }

    public void expand(String expanedRowKey, int expandedRowIndex,
            int expandedNodeSize) {
        TreeNode newNode = new TreeNode(expanedRowKey, expandedRowIndex + 1,
                expandedNodeSize);

        boolean added = nodes.add(newNode);
        if (!added) {
            throw new IllegalStateException("Node in index " + expandedRowIndex
                    + " was expanded already.");
        }

        // push end indexes for parent nodes
        List<TreeNode> updated = nodes.headSet(newNode, false).stream()
                .filter(node -> node.endIndex > expandedRowIndex)
                .collect(Collectors.toList());
        nodes.removeAll(updated);
        updated.stream().forEach(node -> node.pushEnd(expandedNodeSize));
        nodes.addAll(updated);

        // push start and end indexes for later nodes
        updated = nodes.tailSet(newNode, false).stream()
                .collect(Collectors.toList());
        nodes.removeAll(updated);
        updated.stream().forEach(node -> node.push(expandedNodeSize));
        nodes.addAll(updated);
    }

    public int collapse(int collapsedRowIndex) {
        TreeNode collapsedNode = nodes
                .ceiling(new TreeNode(collapsedRowIndex + 1));
        if (collapsedNode == null
                || collapsedNode.startIndex != collapsedRowIndex + 1) {
            throw new IllegalArgumentException(
                    "Could not find expanded node for index "
                            + collapsedRowIndex + ", node was not collapsed");
        }

        // remove complete subtree
        nodes.tailSet(collapsedNode, false)
                .removeIf(node -> node.startIndex < collapsedNode.endIndex);

        // adjust parent end indexes
        final int removedSubTreeSize = collapsedNode.endIndex
                - collapsedNode.startIndex + 1;
        List<TreeNode> updated = nodes.headSet(collapsedNode, false).stream()
                .filter(node -> node.endIndex > collapsedRowIndex)
                .collect(Collectors.toList());
        nodes.removeAll(updated);
        updated.stream().forEach(node -> node.pushEnd(-1 * removedSubTreeSize));
        nodes.addAll(updated);

        nodes.remove(collapsedNode);

        return removedSubTreeSize;
    }

    public Stream<TreeLevelQuery> splitRangeToLevelQueries(final int firstRow,
            final int lastRow) {
        return nodes.stream()
                // filter to parts intersecting with the range
                .filter(node -> node.startIndex <= lastRow
                        && firstRow <= node.endIndex)
                // split into queries per level with level based indexing
                .map(node -> {
                    AtomicInteger start = new AtomicInteger(
                            Math.max(node.startIndex, firstRow));
                    int end = Math.min(node.endIndex, lastRow);
                    AtomicInteger size = new AtomicInteger(
                            end - start.get() + 1);
                    List<TreeNode> subTrees = new ArrayList<>();

                    // calculate how subtrees effect indexing and size
                    int depth = getDepth(node.parentKey);
                    nodes.tailSet(node, false).stream()
                            // find subtrees intersecting the range
                            .filter(subTree -> start.get() <= subTree.endIndex
                                    && subTree.startIndex <= end)
                            // filter to direct subtrees
                            .filter(subTree -> getDepth(
                                    subTree.parentKey) == (depth + 1))
                            .forEachOrdered(childTree -> {
                                // if child is before currently requested start,
                                // adjust start to after subTree and decrease
                                // size
                                if (childTree.startIndex < start.get()) {
                                    int before = start
                                            .getAndSet(childTree.endIndex + 1);
                                    size.addAndGet(before - start.get());
                                } else {
                                    // reduce subtree size from size
                                    size.addAndGet(-1 * (childTree.endIndex
                                            - childTree.startIndex + 1));
                                    subTrees.add(childTree);
                                }
                            });
                    return new TreeLevelQuery(node,
                            start.get() - node.startIndex, size.get(), depth,
                            subTrees);
                }).filter(query -> query.size > 0);
    }

    public <T> void mergeLevelQueryResultIntoRange(
            BiConsumer<T, Integer> rangePositionCallback, TreeLevelQuery query,
            List<T> results) {
        AtomicInteger nextPossibleIndex = new AtomicInteger(
                query.node.startIndex + query.startIndex);
        for (T item : results) {
            // search for any intersecting subtrees and push index if necessary
            query.subTrees.stream().filter(
                    subTree -> subTree.startIndex <= nextPossibleIndex.get()
                            && nextPossibleIndex.get() <= subTree.endIndex)
                    .findAny().ifPresent(intersecting -> {
                        nextPossibleIndex.addAndGet(intersecting.endIndex
                                - intersecting.startIndex + 1);
                        query.subTrees.remove(intersecting);
                    });
            rangePositionCallback.accept(item,
                    nextPossibleIndex.getAndIncrement());
        }
    }

}
