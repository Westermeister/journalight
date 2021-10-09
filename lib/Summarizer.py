"""Provides Summarizer class and stdio interface."""

from dotenv import load_dotenv

load_dotenv(".env")

import os

# We want to silence any unneeded messages from the transformers library.
os.environ["TRANSFORMERS_VERBOSITY"] = "error"

import json
import logging
import sys
import time

from transformers import pipeline


class Summarizer:

    """Can convert long text (up to about 4000 chars) into tweet-sized summaries."""

    def __init__(self):
        """Initialize summarization model and logger."""
        self._model = pipeline("summarization")
        logging.basicConfig(
            format="%(asctime)s.%(msecs)03dZ [Summarizer] %(levelname)s: %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S",
            level=logging.DEBUG,
            filename="combined.log",
        )
        logging.Formatter.converter = time.gmtime
        if "NODE_ENV" in os.environ and os.environ["NODE_ENV"] == "production":
            logging.disable(logging.DEBUG)

    def __call__(self, json_str):
        """
        Parameters:
            json_str (str): JSON string that is an array of strings to be summarized.
        Returns:
            (str): The input, except each string is a summarized version.
        """
        try:
            data = json.loads(json_str)
            retval = []
            for text in data:
                summary = self._summarize(text)
                retval.append(summary)
            retval = json.dumps(retval)
            return retval
        except Exception as e:
            logging.error(str(e))

    def _summarize(self, text):
        """
        Summarizes text.

        Parameters:
            text (str): The text to summarize.
        Returns:
            (str): The summary.
        """
        # The length restrictions will give us a summary about the size of a tweet.
        summary_obj = self._model(text[0:3999], min_length=20, max_length=280)
        summary = summary_obj[0]["summary_text"]
        # The generated summaries tend to have some minor errors, which we fix.
        # Remove leading and trailing whitespace.
        summary = summary.strip()
        # Fix misplaced periods.
        summary = summary.replace(" .", ".")
        # Fix ending quote not having a period.
        if summary[-1] == '"':
            summary = summary[:-1] + "." + summary[-1]
        return summary


if __name__ == "__main__":
    json_str = sys.stdin.readline()
    sys.stdout.write(Summarizer()(json_str))
    sys.stdout.flush()
