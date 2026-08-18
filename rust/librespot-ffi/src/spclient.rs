use librespot_core::{spclient::SpClientResult, SpotifyId, SpotifyUri};
use librespot_protocol::context::Context;

use crate::session::with_session;

pub async fn get_context(uri: &str) -> Result<Context, librespot_core::error::Error> {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("failed to clone session for get_context: {e}");
            return Err(librespot_core::Error::internal("failed to clone session for get_context"));
        }
    };
    let spclient = session.spclient();
    spclient.get_context(uri).await
}

pub async fn get_rootlist() -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("failed to clone session for get_rootlist: {e}");
            return Err(librespot_core::Error::internal("failed to clone session for get_rootlist"));
        }
    };
    let spclient = session.spclient();
    spclient.get_rootlist(0, None).await
}

pub async fn get_radio_for_track(track_uri: &SpotifyUri) -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("failed to clone session for get_radio: {e}");
            return Err(librespot_core::Error::internal("failed to clone session for get_radio"));
        }
    };

    let spclient = session.spclient();
    spclient.get_radio_for_track(track_uri).await
}

pub async fn get_lyrics(track_id: &SpotifyId) -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("failed to clone session for get_lyrics: {e}");
            return Err(librespot_core::Error::internal("failed to clone session for get_lyrics"));
        }
    };

    let spclient = session.spclient();
    spclient.get_lyrics(track_id).await
}

pub fn get_username() -> Result<String, librespot_core::error::Error> {
    match with_session(|s| s.clone()) {
        Ok(s) => Ok(s.username()),
        Err(e) => {
            error!("failed to clone session for get_username: {e}");
            Err(e)
        }
    }
}

fn percent_encode(value: &str) -> String {
    let mut out = String::new();
    for &b in value.as_bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(b as char)
            }
            _ => out.push_str(&format!("%{b:02X}")),
        }
    }
    out
}

pub async fn search_view(query: &str, limit: i32) -> Result<Vec<u8>, librespot_core::error::Error> {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("failed to clone session for search_view: {e}");
            return Err(librespot_core::Error::internal(
                "failed to clone session for search_view",
            ));
        }
    };
    if session.is_invalid() {
        return Err(librespot_core::Error::unauthenticated(
            "playback session is not connected",
        ));
    }

    let encoded = percent_encode(query);
    let country = session.country();
    let endpoints = [
        format!(
            "/searchview/km/v4/search/{encoded}?entityVersion=2&limit={limit}&imageSize=large&catalogue=&country={country}"
        ),
        format!(
            "/searchview/v3/search?q={encoded}&limit={limit}&type=track,artist,album,playlist&country={country}"
        ),
    ];

    let mut last_error = None;
    for endpoint in endpoints {
        match session
            .spclient()
            .request_as_json(&http::Method::GET, &endpoint, None, None)
            .await
        {
            Ok(bytes) if !bytes.is_empty() => return Ok(bytes.to_vec()),
            Ok(_) => {
                warn!("searchview returned empty body for {endpoint}");
            }
            Err(e) => {
                warn!("searchview request failed for {endpoint}: {e}");
                last_error = Some(e);
            }
        }
    }

    Err(last_error.unwrap_or_else(|| {
        librespot_core::Error::unavailable("searchview returned no data")
    }))
}
