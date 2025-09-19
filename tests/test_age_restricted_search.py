import io
import sys
import types
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

if "requests" not in sys.modules:
    requests_stub = types.ModuleType("requests")
    requests_stub.get = None
    sys.modules["requests"] = requests_stub

import age_restricted_search as ars


def _mock_response(json_data):
    response = mock.Mock()
    response.raise_for_status.return_value = None
    response.json.return_value = json_data
    return response


def test_search_videos_returns_only_items_with_video_ids():
    json_payload = {
        "items": [
            {"id": {"videoId": "abc"}},
            {"id": {"kind": "youtube#video"}},
            {"not_id": {}},
            {"id": {"videoId": "def"}},
        ]
    }
    mock_resp = _mock_response(json_payload)
    with mock.patch.object(ars.requests, "get", return_value=mock_resp) as mock_get:
        video_ids = ars.search_videos("api-key", "query", max_results=25)

    assert video_ids == ["abc", "def"]
    mock_get.assert_called_once_with(
        "https://www.googleapis.com/youtube/v3/search",
        params={
            "key": "api-key",
            "q": "query",
            "type": "video",
            "maxResults": 25,
        },
    )


def test_filter_age_restricted_returns_titles_and_urls():
    json_payload = {
        "items": [
            {
                "id": "vid1",
                "contentDetails": {"contentRating": {"ytRating": "ytAgeRestricted"}},
                "snippet": {"title": "First"},
            },
            {
                "id": "vid2",
                "contentDetails": {"contentRating": {"ytRating": "ytSomethingElse"}},
                "snippet": {"title": "Second"},
            },
            {
                "id": "vid3",
                "contentDetails": {},
                "snippet": {"title": "Third"},
            },
            {
                "id": "vid4",
                "contentDetails": {"contentRating": {"ytRating": "ytAgeRestricted"}},
                "snippet": {},
            },
        ]
    }
    mock_resp = _mock_response(json_payload)
    with mock.patch.object(ars.requests, "get", return_value=mock_resp) as mock_get:
        matches = ars.filter_age_restricted("api-key", ["vid1", "vid2", "vid3", "vid4"])

    assert matches == [
        ("First", "https://www.youtube.com/watch?v=vid1"),
        ("(untitled)", "https://www.youtube.com/watch?v=vid4"),
    ]
    mock_get.assert_called_once_with(
        "https://www.googleapis.com/youtube/v3/videos",
        params={
            "key": "api-key",
            "id": "vid1,vid2,vid3,vid4",
            "part": "contentDetails,snippet",
        },
    )


def test_filter_age_restricted_skips_network_call_when_no_ids():
    with mock.patch.object(ars.requests, "get") as mock_get:
        matches = ars.filter_age_restricted("api-key", [])

    assert matches == []
    mock_get.assert_not_called()


def test_main_prints_each_match_on_its_own_line():
    with mock.patch.object(ars, "search_videos", return_value=["one", "two"]) as mock_search, \
        mock.patch.object(
            ars,
            "filter_age_restricted",
            return_value=[("Title 1", "url1"), ("Title 2", "url2")],
        ) as mock_filter:
        buffer = io.StringIO()
        with mock.patch("sys.stdout", buffer):
            exit_code = ars.main(["--api-key", "token", "--query", "term", "--max-results", "10"])

    assert exit_code == 0
    assert buffer.getvalue() == "Title 1 url1\nTitle 2 url2\n"
    mock_search.assert_called_once_with("token", "term", max_results=10)
    mock_filter.assert_called_once_with("token", ["one", "two"])
