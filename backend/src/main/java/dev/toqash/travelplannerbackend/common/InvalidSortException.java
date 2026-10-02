package dev.toqash.travelplannerbackend.common;

import java.util.Set;
import java.util.TreeSet;

public class InvalidSortException extends RuntimeException {
    public InvalidSortException(String property, Set<String> allowed) {
        super("Cannot sort by '" + property + "'. Allowed: " + String.join(", ", new TreeSet<>(allowed)));
    }
}