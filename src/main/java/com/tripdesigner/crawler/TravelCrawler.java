package com.tripdesigner.crawler;

import java.util.List;

public interface TravelCrawler {

    String getSourceName();

    List<String> crawl(String destination, int limit);
}