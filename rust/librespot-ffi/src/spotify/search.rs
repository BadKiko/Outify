// Helper for deconstructing the search json

use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct SearchSection {
    items: Vec<Option<UriItem>>,
}

#[derive(Deserialize, Debug)]
pub struct UriItem {
    uri: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct SearchResponse {
    pub tracks: Option<SearchSection>,
    pub artists: Option<SearchSection>,
    pub albums: Option<SearchSection>,
    pub playlists: Option<SearchSection>,
    pub shows: Option<SearchSection>,
    pub episodes: Option<SearchSection>,
    pub audiobooks: Option<SearchSection>,
}

pub(crate) fn extract_all_uris(res: SearchResponse) -> Vec<String> {
    extract_web_api_uris(res, None)
}

pub(crate) fn extract_web_api_uris(res: SearchResponse, types: Option<&str>) -> Vec<String> {
    let want = types.map(wanted_types);
    let mut uris = Vec::new();
    push_web_section(&mut uris, res.tracks, "track", &want);
    push_web_section(&mut uris, res.artists, "artist", &want);
    push_web_section(&mut uris, res.albums, "album", &want);
    push_web_section(&mut uris, res.playlists, "playlist", &want);
    push_web_section(&mut uris, res.shows, "show", &want);
    push_web_section(&mut uris, res.episodes, "episode", &want);
    push_web_section(&mut uris, res.audiobooks, "audiobook", &want);
    uris
}

fn push_web_section(
    out: &mut Vec<String>,
    section: Option<SearchSection>,
    kind: &str,
    want: &Option<Vec<String>>,
) {
    if let Some(want) = want {
        if !want.iter().any(|t| t == kind) {
            return;
        }
    }
    if let Some(section) = section {
        out.extend(
            section
                .items
                .into_iter()
                .flatten()
                .filter_map(|item| item.uri),
        );
    }
}

#[derive(Debug, Deserialize, Default)]
pub struct SearchViewResponse {
    #[serde(default)]
    pub results: Option<SearchViewSections>,
    #[serde(default)]
    pub tracks: Option<SearchViewSection>,
    #[serde(default)]
    pub artists: Option<SearchViewSection>,
    #[serde(default)]
    pub albums: Option<SearchViewSection>,
    #[serde(default)]
    pub playlists: Option<SearchViewSection>,
    #[serde(default)]
    pub shows: Option<SearchViewSection>,
    #[serde(default)]
    pub episodes: Option<SearchViewSection>,
}

#[derive(Debug, Deserialize, Default)]
pub struct SearchViewSections {
    #[serde(default)]
    pub tracks: Option<SearchViewSection>,
    #[serde(default)]
    pub artists: Option<SearchViewSection>,
    #[serde(default)]
    pub albums: Option<SearchViewSection>,
    #[serde(default)]
    pub playlists: Option<SearchViewSection>,
    #[serde(default)]
    pub shows: Option<SearchViewSection>,
    #[serde(default)]
    pub episodes: Option<SearchViewSection>,
}

#[derive(Debug, Deserialize, Default)]
pub struct SearchViewSection {
    #[serde(default)]
    pub hits: Vec<SearchViewHit>,
    #[serde(default)]
    pub items: Vec<SearchViewHit>,
}

#[derive(Debug, Deserialize, Default)]
pub struct SearchViewHit {
    pub uri: Option<String>,
}

pub(crate) fn extract_searchview_uris(res: SearchViewResponse, types: &str) -> Vec<String> {
    let want = wanted_types(types);
    let nested = res.results.unwrap_or_default();
    let mut uris = Vec::new();
    push_view_section(&mut uris, res.tracks.or(nested.tracks), "track", &want);
    push_view_section(&mut uris, res.artists.or(nested.artists), "artist", &want);
    push_view_section(&mut uris, res.albums.or(nested.albums), "album", &want);
    push_view_section(&mut uris, res.playlists.or(nested.playlists), "playlist", &want);
    push_view_section(&mut uris, res.shows.or(nested.shows), "show", &want);
    push_view_section(&mut uris, res.episodes.or(nested.episodes), "episode", &want);
    uris
}

fn push_view_section(
    out: &mut Vec<String>,
    section: Option<SearchViewSection>,
    kind: &str,
    want: &[String],
) {
    if !want.iter().any(|t| t == kind) {
        return;
    }
    if let Some(section) = section {
        out.extend(
            section
                .hits
                .into_iter()
                .chain(section.items)
                .filter_map(|item| item.uri),
        );
    }
}

fn wanted_types(types: &str) -> Vec<String> {
    types
        .split(',')
        .map(|t| t.trim().trim_end_matches('s').to_ascii_lowercase())
        .filter(|t| !t.is_empty())
        .collect()
}

