/**
 * Tests for the web scrapers.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Tests for the Scraper class. */
public class ScraperTest {

  /** Run all tests within a single function to avoid rescraping. */
  @Test
  public void testAll() {
    Scraper scraper = new Scraper();
    Map<String, List<Map<String, String>>> result = scraper.run();

    // Do we have the keys we expect?
    assertTrue(result.size() == 3);
    assertTrue(result.containsKey("pbs"));
    assertTrue(result.containsKey("npr"));
    assertTrue(result.containsKey("upi"));

    // Did we get the expected amount of articles for each source?
    assertTrue(result.get("pbs").size() <= 10);
    assertTrue(result.get("npr").size() <= 10);
    assertTrue(result.get("upi").size() <= 10);

    // Ensure that each scraped item is formatted correctly.
    for (String source : result.keySet()) {
      for (Map<String, String> item : result.get(source)) {
        assertTrue(item.containsKey("text"));
        assertTrue(item.get("text").length() > 0);
        assertTrue(item.containsKey("url"));
        assertTrue(item.get("url").length() > 0);
        assertTrue(item.containsKey("needsSummary"));
        assertTrue(item.get("needsSummary").length() > 0);
        assertTrue(
          item.get("needsSummary") == "yes" || item.get("needsSummary") == "no"
        );
      }
    }
  }
}
