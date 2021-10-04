/**
 * Provides a class that scrapes NPR.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Responsible for scraping the latest articles from NPR's news section. */
class NPRScraper extends BaseScraper {

  /** Initialize parent class. */
  NPRScraper(Page page) {
    super(page);
  }

  /** Run the scraper and store the result into an attribute. */
  public void run() {
    // Get the articles.
    List<String> articleLinks =
      this.getArticleLinks(
          "https://www.npr.org/sections/news/",
          "h2.title > a"
        );

    // NPR sometimes has "articles" that are more like mini-docs, and not really about notable current events.
    // NPR also sometimes does book reviews. We want neither of these, so let's purge them.
    List<ElementHandle> sectionLinkElements =
      this.page.querySelectorAll("div.slug-wrap > h3.slug > a");
    List<String> sectionLinks = new ArrayList<>();
    for (ElementHandle element : sectionLinkElements) {
      if (sectionLinks.size() == articleLinks.size()) {
        break;
      }
      sectionLinks.add(element.getAttribute("href"));
    }
    for (int i = sectionLinks.size() - 1; i >= 0; --i) {
      String link = sectionLinks.get(i);
      if (link.contains("/series/") || link.contains("/book-reviews/")) {
        articleLinks.remove(i);
      }
    }
    System.out.format(
      "Found %d candidates from NPR's news section%n",
      articleLinks.size()
    );

    // Scrape each article.
    for (String link : articleLinks) {
      System.out.format(
        "Inspecting candidate %d/%d from NPR's news section%n",
        articleLinks.indexOf(link) + 1,
        articleLinks.size()
      );
      this.sleep(5); // Arbitrary wait to avoid burdening NPR's servers.
      this.parseArticle(link);
    }
  }

  /**
   * Parse and store an NPR article.
   * @param link The URL of the article.
   */
  private void parseArticle(String link) {
    // Unlike PBS NewsHour and UPI, NPR often features editorialized leads rather than factual, summary leads.
    // As a result, unfortunately, we're always going to have to extract the whole article for summarization.
    this.page.navigate(link);
    List<ElementHandle> paragraphElements =
      this.page.querySelectorAll("div#storytext > p");
    List<String> paragraphs = new ArrayList<>();
    for (ElementHandle element : paragraphElements) {
      paragraphs.add(element.innerText());
    }

    // Sometimes we have an editor's note in the first paragraph.
    // Since we only care about the article's content, we skip it.
    if (paragraphs.get(0).startsWith("Editor's note")) {
      paragraphs.remove(0);
    }

    // We also sometimes have section headers that aren't part of the main text. Remove them.
    List<ElementHandle> headerElements =
      this.page.querySelectorAll("div#storytext > p > strong");
    List<String> headers = new ArrayList<>();
    for (ElementHandle element : headerElements) {
      headers.add(element.innerText());
    }
    for (int i = paragraphs.size() - 1; i >= 0; --i) {
      if (headers.contains(paragraphs.get(i))) {
        paragraphs.remove(i);
      }
    }

    // Join the paragraphs and store the result.
    String text = String.join(" ", paragraphs);
    this.storeResult(text, this.page.url(), "yes");
  }
}
