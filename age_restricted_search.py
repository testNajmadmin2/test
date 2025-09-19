"""Utility for finding age-restricted YouTube videos for a query.

This script mirrors the minimal example from the user request: it performs a
search using the YouTube Data API, then filters the resulting videos so only
those with ``contentDetails.contentRating.ytRating == "ytAgeRestricted"`` are
returned.  The API key defaults to the value supplied in the prompt but can be
overridden via the ``YOUTUBE_API_KEY`` environment variable or the
``--api-key`` command line flag.  The query is provided via ``--query`` and
defaults to ``"KHITANAN "`` (note the trailing space, as in the prompt).

Usage::

    python age_restricted_search.py --query "some search" [--api-key KEY]

The script prints matching video titles and URLs to stdout, one per line.
"""

from __future__ import annotations

import argparse
import os
from typing import List, Sequence, Tuple

import requests


DEFAULT_API_KEY = "AIzaSyDVvPQ7c830nznOc2U6BoaJETlzCoWP88o"
DEFAULT_QUERY = "KHITANAN "


def search_videos(api_key: str, query: str, *, max_results: int = 50) -> List[str]:
    """Return a list of video IDs resulting from a YouTube search."""

    params = {
        "key": api_key,
        "q": query,
        "type": "video",
        "maxResults": max_results,
    }
    response = requests.get("https://www.googleapis.com/youtube/v3/search", params=params)
    response.raise_for_status()
    payload = response.json()
    items = payload.get("items", [])
    return [item["id"]["videoId"] for item in items if "id" in item and "videoId" in item["id"]]


def filter_age_restricted(api_key: str, video_ids: Sequence[str]) -> List[Tuple[str, str]]:
    """Filter *video_ids* to those that are age restricted."""

    if not video_ids:
        return []

    params = {
        "key": api_key,
        "id": ",".join(video_ids),
        "part": "contentDetails,snippet",
    }
    response = requests.get("https://www.googleapis.com/youtube/v3/videos", params=params)
    response.raise_for_status()
    payload = response.json()

    matches = []
    for item in payload.get("items", []):
        content_details = item.get("contentDetails", {})
        rating = content_details.get("contentRating", {}).get("ytRating")
        if rating == "ytAgeRestricted":
            title = item.get("snippet", {}).get("title", "(untitled)")
            video_id = item.get("id")
            if video_id:
                matches.append((title, f"https://www.youtube.com/watch?v={video_id}"))
    return matches


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--api-key",
        default=os.getenv("YOUTUBE_API_KEY", DEFAULT_API_KEY),
        help="YouTube Data API key (default: value from YOUTUBE_API_KEY env var or provided key)",
    )
    parser.add_argument(
        "--query",
        default=DEFAULT_QUERY,
        help="Query string to search for (default: %(default)r)",
    )
    parser.add_argument(
        "--max-results",
        type=int,
        default=50,
        help="Maximum number of search results to retrieve (default: %(default)d)",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    video_ids = search_videos(args.api_key, args.query, max_results=args.max_results)
    matches = filter_age_restricted(args.api_key, video_ids)

    for title, url in matches:
        print(f"{title} {url}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
