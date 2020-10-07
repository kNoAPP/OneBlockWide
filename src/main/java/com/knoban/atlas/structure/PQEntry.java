package com.knoban.atlas.structure;

import org.jetbrains.annotations.NotNull;

public class PQEntry<T> implements Comparable<PQEntry> {

    private T value;
    private String extra;
    private Integer priority;

    public PQEntry(T value, Integer priority) {
        this.value = value;
        this.priority = priority;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public Integer getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull PQEntry o) {
        int compare = priority.compareTo(o.priority);
        if(compare != 0)
            return compare;

        if(o.extra == null)
            return 1;

        if(extra == null)
            return -1;

        return extra.compareTo(o.extra);
    }
}
