package java_integration.fixtures;

import java.util.*;
import java.util.stream.Collectors;

public class InternalMap<K, V> extends AbstractMap<K, V> {

    private final AbstractSet<Map.Entry<K,V>> entries = new HashSet<>();

    public V put(K key, V value) {
        Optional<Map.Entry<K,V>> entry = entries.stream().filter((e) -> e.getKey().equals(key)).findFirst();
        if (entry.isPresent()) {
            entries.remove(entry.get());
        }

        entries.add(new SimpleEntry(key, value));

        return entry.isPresent() ? entry.get().getValue() : null;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        return entries.stream().filter((e) -> !e.getKey().toString().startsWith("_")).collect(Collectors.toSet());
    }

    public int size() {
        return entries.size();
    }

}