/**
 * Provides a class for scraping multiple news sites.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Responsible for running and combining output from all news scrapers. */
class Scraper {

  /**
   * Run every available news scraper and combine their data into a map.
   * @return The scraped data with keys mapping to each respective news site's scraped data.
   */
  Map<String, List<Map<String, String>>> run() {
    Map<String, List<Map<String, String>>> result = new HashMap<>();

    // Playwright Java doesn't support multithreading.
    // Therefore, we have to declare a Playwright instance for each scraper.
    // Serial scraping is NOT an option - it's significantly slower.

    // Declare PBS scraper's thread.
    Playwright pbsPlaywright = Playwright.create();
    PBSScraper pbsScraper = new PBSScraper(
      pbsPlaywright.chromium().launch().newPage()
    );
    Thread pbsScraperThread = new Thread(pbsScraper);

    // Declare NPR scraper's thread.
    Playwright nprPlaywright = Playwright.create();
    NPRScraper nprScraper = new NPRScraper(
      nprPlaywright.chromium().launch().newPage()
    );
    Thread nprScraperThread = new Thread(nprScraper);

    // Run the threads.
    pbsScraperThread.start();
    nprScraperThread.start();

    // Cleanup time!
    try {
      pbsScraperThread.join();
      nprScraperThread.join();
    } catch (InterruptedException e) {
      System.err.println("Interrupt occurred!");
      e.printStackTrace();
    }
    pbsPlaywright.close();
    nprPlaywright.close();

    // Combine and return the results.
    result.put("pbs", pbsScraper.output());
    result.put("npr", nprScraper.output());
    return result;
  }
}
