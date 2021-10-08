/**
 * Provides a class that scrapes NPR.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.util.ArrayList;
import java.util.List;

/** Responsible for scraping the latest articles from NPR's news section. */
class NPRScraper extends BaseScraper {

  /** Initialize parent class. */
  NPRScraper(Page page) {
    super(page);
  }

  /** Implement abstract base class method. */
  public void run() {
    // Get the articles.
    this.request("https://www.npr.org/sections/news/", 0);
    List<String> articleLinks = this.getLinkAll("h2.title > a");
    this.capArraySize(articleLinks, 10);

    // NPR sometimes has "articles" that are more like mini-docs, and not really about notable current events.
    // NPR also sometimes does book reviews. We want neither of these, so let's purge them.
    List<String> sectionLinks = this.getLinkAll("div.slug-wrap > h3.slug > a");
    this.capArraySize(sectionLinks, articleLinks.size());
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
      this.request(link, 5);
      this.parseArticle();
    }
  }

  /** Parse and store an NPR article. */
  private void parseArticle() {
    // Unlike PBS NewsHour and UPI, NPR often features editorialized leads rather than factual, summary leads.
    // As a result, unfortunately, we're always going to have to extract the whole article for summarization.
    List<String> paragraphs = this.getTextAll("div#storytext > p");

    // Sometimes we have an editor's note in the first paragraph.
    // Since we only care about the article's content, we skip it.
    if (paragraphs.get(0).startsWith("Editor's note")) {
      paragraphs.remove(0);
    }

    // We also sometimes have section headers that aren't part of the main text. Remove them.
    List<String> headers = this.getTextAll("div#storytext > p > strong");
    for (int i = paragraphs.size() - 1; i >= 0; --i) {
      if (headers.contains(paragraphs.get(i))) {
        paragraphs.remove(i);
      }
    }

    // Join the paragraphs and store the result.
    String text = String.join(" ", paragraphs);
    this.storeResult(text, this.url(), "yes");
  }
}
