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
import java.util.Map;

/** Responsible for scraping the latest articles from PBS NewsHour. */
class PBSScraper extends BaseScraper {

  /** Initialize parent class. */
  PBSScraper(Page page) {
    super(page);
  }

  /** Run the scraper and store the result into an attribute. */
  public void run() {
    // Get the articles.
    List<String> articleLinks =
      this.getArticleLinks(
          "https://www.pbs.org/newshour/latest",
          "a.card-timeline__title"
        );
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

      // Respect 2-second "Crawl-Delay" in robots.txt.
      this.sleep(2);
      this.page.navigate(link);

      // PBS NewsHour actually has two types of "articles".
      // The first type is actually a "summary + transcript" of a broadcast. We refer to these as "transcripts".
      // The second is an actual, normal article. We refer to these as "publications".
      ElementHandle transcript = this.page.querySelector("#transcript");

      // These calls will take control of the "page" attribute and store their results into the "result" attribute.
      if (transcript != null) {
        parseTranscript();
      } else {
        parsePublication();
      }
    }
  }

  /** Parse and store a PBS NewsHour transcript. */
  private void parseTranscript() {
    // We're only interested in getting the summary leads of the transcripts.
    String intro = this.page.innerText("div#transcript p");

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
    String title = this.page.innerText("title");
    if (title.startsWith("News Wrap")) {
      // e.g. replaces "In our news wrap Friday" with "This Friday" for brevity.
      text = intro.replace("In our news wrap", "This");
    } else {
      text = removeLastSentence(intro);
      // But wait: what if by removing the last sentence, there's no longer any text?
      // This is a special case. It basically means that the transcript's lead
      // was actually mostly an introduction and doesn't contain a meaningful news
      // summary. So, there being nothing of interest, we simply move on.
      if (text.isEmpty()) {
        return;
      }
    }

    // Now that we have the text, let's store our result.
    this.storeResult(text, this.page.url(), "no");
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
    String firstParagraph = page.innerText("div.body-text > p");
    if (firstParagraph.contains("\u2014")) {
      String text = firstParagraph.substring(
        firstParagraph.indexOf("\u2014") + 2
      );
      this.storeResult(text, this.page.url(), "no");
      return;
    }

    // Otherwise, we're unfortunately going to have to extract the whole article.
    List<ElementHandle> paragraphElements = page.querySelectorAll(
      "div.body-text > p"
    );
    List<String> paragraphs = new ArrayList<>();
    for (ElementHandle element : paragraphElements) {
      paragraphs.add(element.innerText());
    }

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
    this.storeResult(text, this.page.url(), "yes");
  }
}
