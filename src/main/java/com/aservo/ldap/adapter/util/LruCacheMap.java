/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aservo.ldap.adapter.util;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provides an in-memory LRU cache map.
 * If the cache maximum size is reached, the least recently used entry is removed. If the cache maximum size is
 * reached and the access time is equal to the least recently used entries, the oldest entry is removed. All outdated
 * entries by maximum age will be removed on access.
 *
 * @param <K> the type parameter for keys
 * @param <V> the type parameter for values
 */
public final class LruCacheMap<K, V>
        implements Map<K, V> {

    private final FixedSizeMap cacheEntries;
    private final PriorityQueue<PriorityQueueEntry> priorityEntries;
    private final int maxSize;
    private final Duration maxAge;

    /**
     * Instantiates a new LRU cache map.
     *
     * @param maxSize the max size
     * @param maxAge  the max age
     */
    public LruCacheMap(int maxSize, Duration maxAge) {

        cacheEntries = new FixedSizeMap();
        priorityEntries = new PriorityQueue<>(maxSize);

        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    /**
     * Gets maximum cache size.
     *
     * @return the max size
     */
    public int getMaxSize() {

        return maxSize;
    }

    /**
     * Gets maximum entry age.
     *
     * @return the max age
     */
    public Duration getMaxAge() {

        return maxAge;
    }

    private void clean() {

        // remove all outdated entries

        while (!priorityEntries.isEmpty()) {

            if (maxAge.compareTo(Duration.between(priorityEntries.peek().getCreatedAt(), Instant.now())) < 0) {

                PriorityQueueEntry entry = priorityEntries.poll();

                if (entry == null)
                    throw new IllegalStateException("A side effect happened during parallel execution.");

                cacheEntries.remove(entry.getKey());

            } else
                break;
        }
    }

    @Override
    public int size() {

        clean();

        return cacheEntries.size();
    }

    @Override
    public boolean isEmpty() {

        clean();

        return cacheEntries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {

        clean();

        return cacheEntries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {

        clean();

        return cacheEntries.containsValue(value);
    }

    @Override
    public V get(Object key) {

        clean();

        return cacheEntries.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {

        clean();

        priorityEntries.add(new PriorityQueueEntry(key));

        return cacheEntries.put(key, value);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> map) {

        clean();

        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {

            priorityEntries.add(new PriorityQueueEntry(entry.getKey()));
            cacheEntries.put(entry.getKey(), entry.getValue());
        }
    }

    @NotNull
    @Override
    public Set<K> keySet() {

        clean();

        return cacheEntries.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {

        clean();

        return cacheEntries.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {

        clean();

        return cacheEntries.entrySet();
    }

    @Override
    public V remove(Object key) {

        clean();

        priorityEntries.removeIf(x -> x.getKey().equals(key));

        return cacheEntries.remove(key);
    }

    @Override
    public void clear() {

        priorityEntries.clear();
        cacheEntries.clear();
    }

    private class FixedSizeMap
            extends LinkedHashMap<K, V> {

        /**
         * Instantiates a new map with fixed size.
         */
        public FixedSizeMap() {

            super(maxSize + 2, 1, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> entry) {

            return size() > maxSize;
        }
    }

    private class PriorityQueueEntry
            implements Comparable<PriorityQueueEntry> {

        private final K key;
        private final Instant createdAt;

        /**
         * Instantiates a new priority queue entry.
         *
         * @param key the key
         */
        public PriorityQueueEntry(K key) {

            this.key = key;
            this.createdAt = Instant.now();
        }

        @Override
        public int compareTo(@NotNull PriorityQueueEntry entry) {

            if (this.createdAt.isBefore(entry.createdAt))
                return -1;

            if (this.createdAt.isAfter(entry.createdAt))
                return 1;

            return 0;
        }

        /**
         * Gets the key of an entry.
         *
         * @return the key
         */
        public K getKey() {

            return key;
        }

        /**
         * Gets creation time of an entry.
         *
         * @return the created at
         */
        public Instant getCreatedAt() {

            return createdAt;
        }
    }
}
