use std::time::Duration;

use reqwest::StatusCode;

use crate::{
    spotify::{
        error::SpotifyApiError,
        search::{extract_searchview_uris, extract_web_api_uris},
    },
    types::{
        responses::{ArtistsOrTracksPage, CurrentUserResponse},
    },
};

use super::{check_response_json, SpotifyClient, REQUEST_TIMEOUT, SPOTIFY_API_URL};

const WEB_API_TIMEOUT: Duration = Duration::from_secs(15);

impl SpotifyClient {
    pub async fn search(
        &self,
        query: &str,
        types: &str,
        limit: Option<i32>,
        offset: Option<i32>,
    ) -> Result<Vec<String>, SpotifyApiError> {
        let limit = limit.unwrap_or(10);

        for (source, token) in self.access_tokens().await {
            match self
                .search_web_api(query, types, limit, offset, &token)
                .await
            {
                Ok(uris) if !uris.is_empty() => return Ok(uris),
                Ok(_) => warn!("search via {source} web api returned no results for '{query}'"),
                Err(e) => warn!("search via {source} web api failed for '{query}': {e}"),
            }
        }

        match self.search_via_session(query, types, limit).await {
            Ok(uris) if !uris.is_empty() => return Ok(uris),
            Ok(_) => warn!("searchview returned no results for '{query}'"),
            Err(e) => warn!("searchview failed for '{query}': {e}"),
        }

        if types.split(',').any(|t| t.trim().starts_with("track")) {
            match self.search_via_context(query).await {
                Ok(uris) if !uris.is_empty() => return Ok(uris),
                Ok(_) => warn!("context search returned no results for '{query}'"),
                Err(e) => warn!("context search failed for '{query}': {e}"),
            }
        }

        Ok(Vec::new())
    }

    async fn search_web_api(
        &self,
        query: &str,
        types: &str,
        limit: i32,
        offset: Option<i32>,
        token: &str,
    ) -> Result<Vec<String>, SpotifyApiError> {
        let mut params = vec![
            ("q", query.to_string()),
            ("type", types.to_string()),
            ("limit", limit.to_string()),
        ];
        if let Some(o) = offset {
            params.push(("offset", o.to_string()));
        }

        let res = self
            .client
            .get(format!("{}/v1/search", SPOTIFY_API_URL))
            .query(&params)
            .bearer_auth(token)
            .timeout(WEB_API_TIMEOUT)
            .send()
            .await?;

        if !res.status().is_success() {
            let status = res.status().as_str().to_string();
            let body = res.text().await.unwrap_or_default();
            return Err(SpotifyApiError::Generic(format!(
                "search failed with status {status}: {body}"
            )));
        }

        let text = res.text().await?;
        let parsed: crate::spotify::search::SearchResponse = serde_json::from_str(&text)?;
        Ok(extract_web_api_uris(parsed, Some(types)))
    }

    async fn search_via_session(
        &self,
        query: &str,
        types: &str,
        limit: i32,
    ) -> Result<Vec<String>, SpotifyApiError> {
        let body = crate::spclient::search_view(query, limit).await?;
        match serde_json::from_slice::<crate::spotify::search::SearchViewResponse>(&body) {
            Ok(parsed) => Ok(extract_searchview_uris(parsed, types)),
            Err(e) => {
                let preview = String::from_utf8_lossy(&body);
                let preview: String = preview.chars().take(300).collect();
                Err(SpotifyApiError::Generic(format!(
                    "searchview json parse failed: {e}; body={preview}"
                )))
            }
        }
    }

    async fn search_via_context(&self, query: &str) -> Result<Vec<String>, SpotifyApiError> {
        let encoded = query.replace(' ', "+");
        let uri = format!("spotify:search:{encoded}");
        let context = crate::spclient::get_context(&uri).await?;
        let uris = context
            .pages
            .iter()
            .flat_map(|page| &page.tracks)
            .filter_map(|track| track.uri.clone())
            .collect();
        Ok(uris)
    }

    pub async fn get_current_user(&self) -> Result<CurrentUserResponse, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token.ok_or_else(|| {
            SpotifyApiError::Generic("No account token present!".to_string())
        })?;

        let res = self
            .client
            .get(format!("{}/v1/me", SPOTIFY_API_URL))
            .bearer_auth(token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        if !res.status().is_success() {
            let status = res.status().as_str().to_string();
            let body = res.text().await.unwrap_or_default();
            return Err(SpotifyApiError::Generic(format!(
                "Request failed with status code: {}. Body: {}",
                status, body
            )));
        }

        let text = res.text().await?;

        let data: CurrentUserResponse = serde_json::from_str(&text)?;

        Ok(data)
    }

    pub async fn get_top(
        &self,
        request_type: Option<String>,
        time_range: String,
    ) -> Result<ArtistsOrTracksPage, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token.ok_or_else(|| {
            SpotifyApiError::Generic("No account token present!".to_string())
        })?;

        let request_type = request_type.unwrap_or_else(|| "artists".to_string());

        let url = format!(
            "{}/v1/me/top/{}?time_range={}",
            SPOTIFY_API_URL, request_type, time_range
        );

        let res = self
            .client
            .get(&url)
            .bearer_auth(&token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        if res.status() == StatusCode::UNAUTHORIZED {
            let new_token = self.refresh_token(&token).await?;
            let res = self
                .client
                .get(&url)
                .bearer_auth(new_token.access_token)
                .timeout(REQUEST_TIMEOUT)
                .send()
                .await?;
            let data = check_response_json::<ArtistsOrTracksPage>("get_top", res).await?;
            return Ok(data);
        }

        let data = check_response_json::<ArtistsOrTracksPage>("get_top", res).await?;

        Ok(data)
    }
}
