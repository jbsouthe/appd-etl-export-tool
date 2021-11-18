package com.cisco.josouthe.data.analytic;

public class Search {
    public String name, query;
    public int limit = 20000;

    public Search(String name, String query, int limit) {
        this.name=name;
        this.query=query;
        this.limit=limit;
    }

    public String getName() { return name; }
}
