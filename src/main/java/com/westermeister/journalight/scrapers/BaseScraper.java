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

/** Responsible for hosting utilities for children scrapers. */
abstract class BaseScraper implements Runnable {
  /** Used like a browser tab to perform the scraping. */
  private final Page page;

  /** Stores the scraped data. */
  private final List<Map<String, String>> result;

  /**
   * Initialize browser tab to be used for scraping, as well as storage.
   * @param page A page object from a Playwright browser instance.
   */
  BaseScraper(Page page) {
    this.page = page;
    this.result = new ArrayList<>();
  }

  /** Run the scraper via a thread and store <= 10 results into the corresponding attribute. */
  public abstract void run();

  /**
   * Get the stored result.
   * @return The scraped data, with each map having keys "text", "url", and "needsSummary".
   *         The first two keys map to the article text and source URL.
   *         The last key maps to either "yes" or "no", depending on whether a summary is needed for the text.
   */
  List<Map<String, String>> output() {
    return this.result;
  }

  /**
   * Move the browser tab to the given URL.
   * @param url   The URL to move to.
   * @param delay Number of seconds to wait before the request.
   */
  void request(String url, int delay) {
    try {
      Thread.sleep(delay * 1000);
    } catch (InterruptedException e) {}
    this.page.navigate(url);
  }

  /**
   * Test whether a selector yields any matches.
   * @param selector The selector to match.
   * @return         True if exists, false otherwise.
   */
  boolean exists(String selector) {
    ElementHandle result = this.page.querySelector(selector);
    return result != null;
  }

  /**
   * Get the innerText of the first selected element.
   * @param selector The selector to match.
   * @return         The innerText.
   */
  String getText(String selector) {
    return this.page.innerText(selector);
  }

  /**
   * Get the innerText values of all elements that match the selector.
   * @param selector The selector to match.
   * @return         The list of innerText values.
   */
  List<String> getTextAll(String selector) {
    List<ElementHandle> elements = this.page.querySelectorAll(selector);
    List<String> texts = new ArrayList<>();
    for (ElementHandle element : elements) {
      texts.add(element.innerText());
    }
    return texts;
  }

  /**
   * Get all the links of selected anchor tags.
   * @param selector A selector for anchor tags.
   * @return         The list of links.
   */
  List<String> getLinkAll(String selector) {
    List<ElementHandle> linkElements = this.page.querySelectorAll(selector);
    List<String> links = new ArrayList<>();
    for (ElementHandle element : linkElements) {
      links.add(element.getAttribute("href"));
    }
    return links;
  }

  /**
   * Return the URL that the page attribute is currently set to.
   * @return The URL.
   */
  String url() {
    return this.page.url();
  }

  /**
   * Remove elements from array until its size is <= the limit.
   * @param array The array to remove elements from.
   * @param limit The limit.
   */
  void capArraySize(List<String> array, int limit) {
    while (array.size() > limit) {
      array.remove(array.size() - 1);
    }
  }

  /**
   * Store a scraped object into the result.
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
