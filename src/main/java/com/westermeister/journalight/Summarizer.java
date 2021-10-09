/**
 * Bindings to Python Summarization module.
 * Copyright (c) 2021 Westermeister. All rights reserved.
 */

package com.westermeister.journalight;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Responsible for summarizing text. */
class Summarizer {

  /**
   * Summarizes each string within a list of strings.
   * @param strings The list of strings to be summarized.
   * @return        Summaries for each string.
   */
  List<String> summarize(List<String> strings) {
    List<String> result = new ArrayList<>();
    try {
      Gson gson = new Gson();
      String jsonStrings = gson.toJson(strings);

      ProcessBuilder builder = new ProcessBuilder(
        "python",
        "./lib/Summarizer.py"
      );
      Process process = builder.start();

      OutputStream stdin = process.getOutputStream();
      InputStream stdout = process.getInputStream();

      BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));

      writer.write(jsonStrings);
      writer.flush();
      writer.close();

      Scanner scanner = new Scanner(stdout);
      List<String> output = new ArrayList<>();
      while (scanner.hasNextLine()) {
        output.add(scanner.nextLine());
      }
      String[] jsonOutput = gson.fromJson(output.get(0), String[].class);
      for (String str : jsonOutput) {
        result.add(str);
      }
    } catch (Exception e) {}
    return result;
  }
}
