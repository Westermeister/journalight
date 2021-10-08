/**
 * Provides a class that scrapes UPI.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.util.ArrayList;
import java.util.List;

/** Responsible for scraping the latest articles from UPI's top news index. */
class UPIScraper extends BaseScraper {

  /** Initialize parent class. */
  UPIScraper(Page page) {
    super(page);
  }

  /** Implement abstract base class method. */
  public void run() {
    // Luckily, many of the articles in UPI's top news index include both the title AND a summary lead.
    // Thus, we can extract many summaries with just one request for the index.
    this.request("https://www.upi.com/Top_News/", 0);
    System.out.println("Scraping summaries from UPI's top news index");
    this.scrapeIndex();

    // While we're still at the index, we can get a few other top articles.
    // These ones don't include summaries with the index, unfortunately.
    // We gotta deal with them the old-fashioned way: by navigating to them directly.
    List<String> articleLinks = this.getLinkAll("a.col-md-4.col-sm-4");
    // Remove "On this day" articles that are about history, not current events.
    for (int i = articleLinks.size() - 1; i >= 0; --i) {
      if (articleLinks.get(i).contains("On-This-Day")) {
        articleLinks.remove(i);
      }
    }
    // Now scrape each article.
    for (String link : articleLinks) {
      System.out.format(
        "Inspecting extra UPI article %d/%d%n",
        articleLinks.indexOf(link) + 1,
        articleLinks.size()
      );
      this.request(link, 5);
      this.scrapeArticle();
    }
  }

  /** Parse and store summary leads from UPI's top news index. */
  private void scrapeIndex() {
    List<String> summaries = this.getTextAll("div.content");
    List<String> summaryLinks = this.getLinkAll("a.row");

    // We want 10 articles; the other 3 aren't covered by this method.
    this.capArraySize(summaries, 7);
    this.capArraySize(summaryLinks, 7);

    // There MAY be a string ") --" within a summary.
    // The sequence separates some info from a summary lead e.g. "(UPI) -- Breaking news, blah blah"
    for (int i = 0; i < summaries.size(); ++i) {
      if (summaries.get(i).contains(") --")) {
        int index = summaries.get(i).indexOf(") --");
        summaries.set(i, summaries.get(i).substring(index + 5));
      }
    }

    // UPI sometimes has "On this day" articles that are about history, not current events.
    // We'll be able to tell this by looking at the URL.
    for (int i = summaries.size() - 1; i >= 0; --i) {
      if (summaryLinks.get(i).contains("On-This-Day")) {
        summaries.remove(i);
        summaryLinks.remove(i);
      }
    }

    // Now let's save our results.
    for (int i = 0; i < summaries.size(); ++i) {
      this.storeResult(summaries.get(i), summaryLinks.get(i), "no");
    }
  }

  /** Parse and store summary leads from UPI articles. */
  private void scrapeArticle() {
    String rawSummaryLead = this.getText("article > p");
    String summary = rawSummaryLead.substring(
      rawSummaryLead.indexOf("-- ") + 3
    );
    this.storeResult(summary, this.url(), "no");
  }
}
