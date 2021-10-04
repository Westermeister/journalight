/**
 * Provides a base class for other scrapers.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Responsible for hosting a basic framework and utilities for children classes. */
abstract class BaseScraper implements Runnable {
  /** Used like a browser tab to perform the scraping. */
  final Page page;

  /** Stores the scraped data. */
  final List<Map<String, String>> result;

  /**
   * Initialize browser tab to be used for scraping, as well as storage.
   * @param page A page object from a Playwright browser instance.
   */
  BaseScraper(Page page) {
    this.page = page;
    this.result = new ArrayList<>();
  }

  public abstract void run();

  /**
   * Get the saved result.
   * @return The scraped data, with each map having keys "text", "url", and "needsSummary".
   *         The first two keys map to the article text and source URL.
   *         The last key maps to either "yes" or "no", depending on whether a summary is needed for the text.
   */
  List<Map<String, String>> output() {
    return this.result;
  }

  /**
   * Grabs up to 10 links from a page with a list of articles.
   * @param index    URL of the index to parse.
   * @param selector The CSS selector to use for anchor tags.
   * @return         The list of links.
   */
  List<String> getArticleLinks(String index, String selector) {
    this.page.navigate(index);
    List<ElementHandle> articleLinkElements =
      this.page.querySelectorAll(selector);
    List<String> articleLinks = new ArrayList<>();
    for (ElementHandle element : articleLinkElements) {
      articleLinks.add(element.getAttribute("href"));
    }
    while (articleLinks.size() > 10) {
      articleLinks.remove(articleLinks.size() - 1);
    }
    return articleLinks;
  }

  /**
   * Sleep for the given number of seconds.
   * @param seconds The number of seconds to sleep.
   */
  void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException e) {}
  }

  /**
   * Stores a scraped object into the result.
   * @param text         The scraped text.
   * @param url          The source URL of the text.
   * @param needsSummary Either "yes" or "no".
   */
  void storeResult(String text, String url, String needsSummary) {
    Map<String, String> resultItem = new HashMap<>();
    resultItem.put("text", text);
    resultItem.put("url", url);
    resultItem.put("needsSummary", needsSummary);
    this.result.add(resultItem);
  }
}
