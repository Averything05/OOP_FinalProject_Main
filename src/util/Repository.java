package util;

import exception.InvalidRequestException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic in-memory store that keeps items of any type T and finds them by ID.
 * A LinkedHashMap gives fast lookup by ID while keeping the order in which items were added.
 * The ID of an item is read with the Function given to the constructor (for example Customer::getId).
 *
 * @param <T> the type of item stored
 */
public class Repository<T> {
    private final Map<String, T> items = new LinkedHashMap<>();
    private final Function<T, String> idOf;

    public Repository(Function<T, String> idOf) {
        this.idOf = idOf;
    }

    /** Adds an item. Two items with the same ID are rejected. */
    public void add(T item) {
        String id = idOf.apply(item);
        if (items.containsKey(id)) {
            throw new InvalidRequestException("Duplicate ID: " + id);
        }
        items.put(id, item);
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    /** @return a copy of all items in insertion order */
    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    /** @return all items that match the given condition */
    public List<T> findWhere(Predicate<T> condition) {
        return items.values().stream().filter(condition).collect(Collectors.toList());
    }

    public int size() {
        return items.size();
    }
}
