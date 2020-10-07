package com.knoban.atlas.structure;

import org.jetbrains.annotations.Nullable;

/**
 * A simple generic kay-value pair.
 *
 * @param <T> The type of the key
 * @param <U> The type of the value
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class Pair<T, U> {

    private T key;
    private U value;

    /**
     * Creates an empty key-value pair
     */
    public Pair() {}

    /**
     * Creates a key-value pair
     * @param key The key to store
     * @param value The value to store
     */
    public Pair(@Nullable T key, @Nullable U value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return The key of this pair
     */
    @Nullable
    public T getKey() {
        return key;
    }

    /**
     * @param key The key to set for this pair
     */
    public void setKey(@Nullable T key) {
        this.key = key;
    }

    /**
     * @return The value of this pair
     */
    @Nullable
    public U getValue() {
        return value;
    }

    /**
     * @param value The value to set for this pair
     */
    public void setValue(@Nullable U value) {
        this.value = value;
    }
}
