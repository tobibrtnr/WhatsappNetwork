package de.tobibrtnr.classes;

/**
 * A simple class to get a ascending ID.
 */
public class NumberID {
    // Current ID
    private static int pointer = 1;

    /**
     * Increments the current ID.
     */
    public static void add() {
        pointer++;
    }

    /**
     * Returns the current ID.
     */
    public static int get() {
        return pointer;
    }

}
