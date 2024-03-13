package com.eu.habbo.habbohotel.modtool;

import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class CfhCategory {
    private final int id;
    private final String name;
    private final TIntObjectMap<CfhTopic> topics;

    public CfhCategory(int id, String name) {
        this.id = id;
        this.name = name;
        this.topics = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    }

    public void addTopic(CfhTopic topic) {
        this.topics.put(topic.id, topic);
    }

    public TIntObjectMap<CfhTopic> getTopics() {
        return this.topics;
    }

    public String getName() {
        return this.name;
    }
}