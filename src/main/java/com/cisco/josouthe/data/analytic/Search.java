package com.cisco.josouthe.data.analytic;

import java.util.ArrayList;
import java.util.List;

public class Search {
    public String searchName, searchDescription, searchType, searchMode, viewMode, visualization;
    public long limit = 20000;
    public List<String> adqlQueries;

    public Search() {} //for gson conversion

    public Search(String name, String query, long limit, String visualization) {
        this.searchName =name;
        this.adqlQueries = new ArrayList<>();
        this.adqlQueries.add(query);
        this.limit=limit;
        this.visualization=visualization;
    }

    public String getName() { return searchName; }
    public String getQuery() {
        if( adqlQueries != null && !adqlQueries.isEmpty() ) return adqlQueries.get(0);
        return "";
    }
}
