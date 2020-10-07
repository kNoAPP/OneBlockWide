package com.knoban.atlas.structure;

import org.jetbrains.annotations.Nullable;

/**
 * A simple generic kay-value pair.
 *
 * @param <T> The type of the primary
 * @param <U> The type of the secondary
 * @param <V> The type of the tertiary
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class Trio<T, U, V> {

    private T primary;
    private U secondary;
    private V tertiary;

    /**
     * Creates an empty Trio
     */
    public Trio() {}

    /**
     * Creates a Trio
     * @param primary The primary to store
     * @param secondary The secondary to store
     */
    public Trio(@Nullable T primary, @Nullable U secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    /**
     * @return The primary of this Trio
     */
    @Nullable
    public T getPrimary() {
        return primary;
    }

    /**
     * @param primary The primary to set for this Trio
     */
    public void setPrimary(@Nullable T primary) {
        this.primary = primary;
    }

    /**
     * @return The secondary of this Trio
     */
    @Nullable
    public U getSecondary() {
        return secondary;
    }

    /**
     * @param secondary The secondary to set for this Trio
     */
    public void setSecondary(@Nullable U secondary) {
        this.secondary = secondary;
    }

    /**
     * @return The tertiary of this Trio
     */
    @Nullable
    public V getTertiary() {
        return tertiary;
    }

    /**
     * @param tertiary The tertiary to set for this Trio
     */
    public void setTertiary(@Nullable V tertiary) {
        this.tertiary = tertiary;
    }
}
