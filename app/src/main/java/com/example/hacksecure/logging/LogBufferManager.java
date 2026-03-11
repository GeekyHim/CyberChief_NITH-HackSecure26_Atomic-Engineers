package com.example.hacksecure.logging;

import com.example.hacksecure.data.entity.EventLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe in-memory buffer for EventLog objects.
 *
 * Uses ConcurrentLinkedQueue to allow concurrent producers.
 */
public class LogBufferManager {

    private final ConcurrentLinkedQueue<EventLog> buffer = new ConcurrentLinkedQueue<>();

    /**
     * Adds an event to the buffer.
     *
     * @param event the EventLog to add (already deduplicated upstream)
     * @return the approximate current size of the buffer after insertion
     */
    public int addEvent(EventLog event) {
        if (event == null) {
            return buffer.size();
        }
        buffer.add(event);
        return buffer.size();
    }

    /**
     * Drains all available events from the buffer into a List.
     *
     * @return list containing all events that were in the buffer; empty if none.
     */
    public List<EventLog> drainAll() {
        List<EventLog> drained = new ArrayList<>();
        EventLog log;
        while ((log = buffer.poll()) != null) {
            drained.add(log);
        }
        return drained;
    }

    /**
     * @return current approximate size of the buffer.
     */
    public int size() {
        return buffer.size();
    }
}

