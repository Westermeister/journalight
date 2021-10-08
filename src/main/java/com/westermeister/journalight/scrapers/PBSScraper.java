/**
 * Provides a class that scrapes PBS.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.microsoft.playwright.*;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Responsible for scraping the latest articles from PBS NewsHour. */
class PBSScraper extends BaseScraper {

  /** Initialize parent class. */
  PBSScraper(Page page) {
    super(page);
  }

  /** Implement abstract base class method. */
  public void run() {
    // Get the articles.
    this.request("https://www.pbs.org/newshour/latest", 0);
    List<String> articleLinks = this.getLinkAll("a.card-timeline__title");
    this.capArraySize(articleLinks, 10);
    System.out.format(
      "Found %d candidates from PBS NewsHour%n",
      articleLinks.size()
    );

    // Scrape each article.
    for (String link : articleLinks) {
      System.out.format(
        "Inspecting candidate %d/%d from PBS NewsHour%n",
        articleLinks.indexOf(link) + 1,
        articleLinks.size()
      );
      this.request(link, 2);

      // PBS NewsHour actually has two types of "articles".
      // The first type is actually a "summary + transcript" of a broadcast. We refer to these as "transcripts".
      // The second is an actual, normal article. We refer to these as "publications".
      if (this.exists("#transcript")) {
        this.parseTranscript();
      } else {
        this.parsePublication();
      }
    }
  }

  /** Parse and store a PBS NewsHour transcript. */
  private void parseTranscript() {
    // We're only interested in getting the summary leads of the transcripts.
    String intro = this.getText("div#transcript p");

    // Sometimes, PBS has interviews with people over things that aren't "actual" news.
    // e.g. mini-docs, books, television series, etc.
    // Journalight doesn't consider these to be newsworthy, so we abort if we detect it.
    if (
      intro.contains("new book") ||
      intro.contains("new report") ||
      intro.contains("special report") ||
      intro.contains("series")
    ) {
      return;
    }

    // Before we scrape transcripts, we need to see what type of transcript it is:
    // 1. A "news wrap" transcript, which is a special transcript detailing news highlights.
    // 2. An "interview" transcript, which is just that: an interview.
    // We need to handle these different types of transcripts separately.
    String text;
    String title = this.getText("title");
    if (title.startsWith("News Wrap")) {
      // e.g. replaces "In our news wrap Friday" with "This Friday" for brevity.
      text = intro.replace("In our news wrap", "This");
    } else {
      text = this.removeLastSentence(intro);
      // But wait: what if by removing the last sentence, there's no longer any text?
      // This is a special case. It basically means that the transcript's lead
      // was actually mostly an introduction and doesn't contain a meaningful news
      // summary. So, there being nothing of interest, we simply move on.
      if (text.isEmpty()) {
        return;
      }
    }

    // Now that we have the text, let's store our result.
    this.storeResult(text, this.url(), "no");
  }

  /**
   * Removes the last sentence of a piece of text.
   * @param text The text to analyze.
   * @return     The text, but with the last sentence removed.
   */
  private String removeLastSentence(String text) {
    List<String> sentences = new ArrayList<>();
    BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.US);
    boundary.setText(text);
    int start = boundary.first();
    for (
      int end = boundary.next();
      end != BreakIterator.DONE;
      start = end, end = boundary.next()
    ) {
      sentences.add(text.substring(start, end));
    }
    sentences.remove(sentences.size() - 1);
    StringBuilder result = new StringBuilder();
    for (String sentence : sentences) {
      result.append(sentence);
    }
    return result.toString();
  }

  /** Parse and store a PBS NewsHour publication. */
  private void parsePublication() {
    // Some publications have a summary lead, which can be detected by an "em dash" unicode character.
    // We can take this summary and store it directly.
    String firstParagraph = this.getText("div.body-text > p");
    if (firstParagraph.contains("\u2014")) {
      String text = firstParagraph.substring(
        firstParagraph.indexOf("\u2014") + 2
      );
      this.storeResult(text, this.url(), "no");
      return;
    }

    // Otherwise, we're unfortunately going to have to extract the whole article.
    List<String> paragraphs = this.getTextAll("div.body-text > p");

    // Some of the paragraphs are useless links encouraging readers to "read more" or "watch" some video.
    // Let's get rid of those.
    for (int i = paragraphs.size() - 1; i >= 0; --i) {
      String paragraph = paragraphs.get(i);
      if (paragraph.startsWith("READ MORE") || paragraph.startsWith("Watch")) {
        paragraphs.remove(i);
      }
    }

    // Join the paragraphs and store the result.
    String text = String.join(" ", paragraphs);
    this.storeResult(text, this.url(), "yes");
  }
}
