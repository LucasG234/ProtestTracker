package com.lucasg234.protesttracker.mainactivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * Custom TreeSet which stores objects so they can be efficiently retrieved by Object Id
 */
public class MapSearchableSet<K> implements Set<K> {

    public interface Accessor<K> extends Comparator<K> {
        String accessId(K object);
    }

    // Nodes used within Tree structure
    private class Node {
        K object;
        Node left;
        Node right;
        Node parent;

        Node(K object) {
            this.object = object;
        }
    }

    private Accessor<K> mAccessor;
    private Node mHead;
    private int mSize;

    public MapSearchableSet(Accessor<K> accessor) {
        this.mAccessor = accessor;
        this.mHead = null;
        this.mSize = 0;
    }

    public K getObjectById(String objectId) {
        Iterator<K> iter = this.iterator();
        while (iter.hasNext()) {
            K object = iter.next();
            String id = mAccessor.accessId(object);
            if (id != null && id.equals(objectId))
                return object;
        }
        return null;
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return traverseToNode((K) o) != null;
    }

    @NonNull
    @Override
    public Iterator<K> iterator() {
        return new SearchIterator();
    }

    // Currently unimplemented
    @NonNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    // Currently unimplemented
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] ts) {
        return null;
    }

    @Override
    public boolean add(K object) {
        if (mHead == null) {
            mHead = new Node(object);
            mSize++;
            return true;
        }
        Node currNode = mHead;
        do {
            // Do not add duplicates into the set
            if (object.equals(currNode.object)) {
                return false;
            } else if (mAccessor.compare(object, currNode.object) < 0) {
                // Left case
                if (currNode.left == null) {
                    Node newLeft = new Node(object);
                    newLeft.parent = currNode;
                    currNode.left = newLeft;
                    mSize++;
                    return true;
                }
                currNode = currNode.left;
            } else {
                // Right case
                if (currNode.right == null) {
                    Node newRight = new Node(object);
                    newRight.parent = currNode;
                    currNode.right = newRight;
                    mSize++;
                    return true;
                }
                currNode = currNode.right;
            }
        } while (currNode != null);
        // Loop exited only in error cases
        return false;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        Node node = traverseToNode((K) o);
        if (node == null) {
            // Case where the object is not in the Tree
            return false;
        } else {
            Node toReplace;

            if (node.left == null && node.right == null) {
                // In case with no children, simply delete the node
                toReplace = null;
            } else if (node.left == null || node.right == null) {
                // In case with one child, replace this node with that the child
                toReplace = node.left == null ? node.right : node.left;
                toReplace.parent = node.parent;
            } else {
                // In case with two children, replace this node with the largest node in the left subtree
                toReplace = node.left;
                while (toReplace.right != null) {
                    toReplace = toReplace.right;
                }
                toReplace.parent.right = toReplace.left;
                toReplace.left = node.left;
                toReplace.right = node.right;
                toReplace.parent = node.parent;
            }

            if (node.parent == null) {
                mHead = toReplace;
            } else {
                if (node == node.parent.left) {
                    node.parent.left = toReplace;
                } else {
                    node.parent.right = toReplace;
                }
            }
            return true;
        }
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends K> collection) {
        boolean changed = false;
        for (K k : collection) {
            changed = changed || add(k);
        }
        return changed;
    }

    // Naive implementation
    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        clear();
        addAll((Collection<? extends K>) collection);
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        boolean changed = false;
        for (Object o : collection) {
            changed = changed || remove(o);
        }
        return changed;
    }

    @Override
    public void clear() {
        mHead = null;
        mSize = 0;
    }

    // Helper method which traverse to the node containing the given object
    // Returns null if it does not exist
    private Node traverseToNode(K object) {
        if (object == null) {
            return null;
        }

        Node currNode = mHead;
        while (currNode != null) {
            if (object.equals(currNode.object)) {
                return currNode;
            } else if (mAccessor.compare(object, currNode.object) < 0) {
                currNode = currNode.left;
            } else {
                currNode = currNode.right;
            }
        }
        // Exit means that null was found where the node would be
        return null;
    }

    private class SearchIterator implements Iterator<K> {
        private Stack<Node> mStack;

        public SearchIterator() {
            mStack = new Stack<>();
            Node currNode = mHead;

            while (currNode != null) {
                mStack.push(currNode);
                currNode = currNode.left;
            }
        }

        @Override
        public boolean hasNext() {
            return !mStack.isEmpty();
        }

        @Override
        public K next() {
            Node currNode = mStack.pop();
            K object = currNode.object;
            if (currNode.right != null) {
                currNode = currNode.right;
                while (currNode != null) {
                    mStack.push(currNode);
                    currNode = currNode.left;
                }
            }
            return object;
        }
    }

}
