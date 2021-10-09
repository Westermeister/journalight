/**
 * Provides application entry point.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Responsible for running the program. */
public class App {

  public static void main(String[] args) {
    // Scrape articles.
    Scraper scraper = new Scraper();
    Map<String, List<Map<String, String>>> result = scraper.run();

    // Summarize them if necessary.
    List<String> articles = new ArrayList<>();
    for (String source : result.keySet()) {
      for (int i = 0; i < result.get(source).size(); ++i) {
        if (result.get(source).get(i).get("needsSummary") == "yes") {
          articles.add(result.get(source).get(i).get("text"));
        }
      }
    }
    Summarizer summarizer = new Summarizer();
    List<String> summaries = summarizer.summarize(articles);
    for (String source : result.keySet()) {
      for (int i = 0; i < result.get(source).size(); ++i) {
        if (result.get(source).get(i).get("needsSummary") == "yes") {
          result.get(source).get(i).put("text", summaries.get(0));
          summaries.remove(0);
        }
      }
    }

    System.out.println();

    System.out.println("From PBS:");
    System.out.println();
    for (int i = 0; i < result.get("pbs").size(); ++i) {
      System.out.format(
        "%d. %s%n",
        i + 1,
        result.get("pbs").get(i).get("text")
      );
    }

    System.out.println();

    System.out.println("From NPR:");
    System.out.println();
    for (int i = 0; i < result.get("npr").size(); ++i) {
      System.out.format(
        "%d. %s%n",
        i + 1,
        result.get("npr").get(i).get("text")
      );
    }

    System.out.println();

    System.out.println("From UPI:");
    System.out.println();
    for (int i = 0; i < result.get("upi").size(); ++i) {
      System.out.format(
        "%d. %s%n",
        i + 1,
        result.get("upi").get(i).get("text")
      );
    }

    System.out.println();
    System.exit(0);
  }
}
