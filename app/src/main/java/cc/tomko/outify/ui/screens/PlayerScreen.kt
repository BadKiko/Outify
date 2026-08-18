package cc.tomko.outify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.MyIcons
import cc.tomko.outify.core.model.Artist
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.SyncedLyric
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.components.WavyMusicSlider
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import cc.tomko.outify.utils.RomanizationUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val IMAGE_SIZE = 400.dp
private val BOTTOM_ACTIONS_SPACE = 108.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    listState: LazyListState,
    onArtistClick: (Artist) -> Unit,
    onMoreOptions: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val forwardMilliseconds by viewModel.forwardMilliseconds.collectAsState(15_000)
    val positionMs by viewModel.positionMs.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState(initial = null)
    val previousTrack by viewModel.previousTrack.collectAsState(initial = null)
    val nextTrack by viewModel.nextTrack.collectAsState(initial = null)
    val hasPrevious by viewModel.hasPrevious.collectAsState(initial = false)
    val hasNext by viewModel.hasNext.collectAsState(initial = false)
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyric by viewModel.currentLyric.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()
    val isRepeating by viewModel.isRepeating.collectAsState()
    val isFavorite by viewModel.isLiked.collectAsState()
    val romanizeLyrics by viewModel.romanizeLyrics.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
    ) {
        val imageSize = minOf(
            IMAGE_SIZE,
            maxWidth - 48.dp,
            (maxHeight * 0.38f).coerceAtLeast(180.dp),
        )
        val artworkTopSpacer = ((maxHeight - imageSize) / 5f - 24.dp).coerceAtLeast(8.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = BOTTOM_ACTIONS_SPACE,
            )
        ) {
            item { Spacer(Modifier.height(artworkTopSpacer)) }

            item {
                SwipeableNowPlaying(
                    currentTrack = currentTrack,
                    previousTrack = previousTrack,
                    nextTrack = nextTrack,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    imageSize = imageSize,
                    isPlaying = uiState.isPlaying,
                    onArtistClick = onArtistClick,
                    onSkipPrevious = { viewModel.onAction(PlayerAction.Previous) },
                    onSkipNext = { viewModel.onAction(PlayerAction.Next) },
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                TrackProgressBar(
                    durationMs = uiState.totalLengthMs,
                    positionMs = positionMs,
                    isPlaying = uiState.isPlaying,
                    onSeek = { viewModel.onAction(PlayerAction.SeekTo(it)) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                PlaybackControls(
                    isPlaying = uiState.isPlaying,
                    isBuffering = uiState.isBuffering,
                    isShuffling = isShuffling,
                    isRepeating = isRepeating,
                    onPlayPause = { viewModel.onAction(PlayerAction.PlayPause) },
                    onNext = { viewModel.onAction(PlayerAction.Next) },
                    onPrevious = { viewModel.onAction(PlayerAction.Previous) },
                    onShuffle = { viewModel.onAction(PlayerAction.ShuffleToggle) },
                    onRepeat = { viewModel.onAction(PlayerAction.RepeatToggle) },
                    onFastForward = {
                        val position = (positionMs + forwardMilliseconds).coerceAtMost(uiState.totalLengthMs)
                        viewModel.onAction(PlayerAction.SeekTo(position))
                    },
                    onFastRewind = {
                        val position = (positionMs - forwardMilliseconds).coerceAtLeast(0)
                        viewModel.onAction(PlayerAction.SeekTo(position))
                    },
                    showFastForward = forwardMilliseconds > 0
                )
            }

            item {
                Surface(
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .padding(top = 92.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Lyrics(
                            loadLyrics = { viewModel.loadLyrics() },
                            track = currentTrack,
                            lyrics = lyrics,
                            currentLyric = currentLyric,
                            seekTo = { viewModel.onAction(PlayerAction.SeekTo(it)) },
                            romanize = romanizeLyrics,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Top and bottom gradient fades
                        listOf(Alignment.TopCenter, Alignment.BottomCenter).forEach { alignment ->
                            val colors = if (alignment == Alignment.TopCenter)
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    Color.Transparent
                                )
                            else
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(Brush.verticalGradient(colors))
                                    .align(alignment)
                            )
                        }
                    }
                }
            }
        }

        BottomActionsBar(
            isFavorite = isFavorite,
            onFavoriteToggle = { viewModel.toggleFavorite() },
            onMoreOptions = onMoreOptions,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwipeableNowPlaying(
    currentTrack: Track?,
    previousTrack: Track?,
    nextTrack: Track?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    imageSize: Dp,
    isPlaying: Boolean,
    onArtistClick: (Artist) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val hasPreviousLatest = rememberUpdatedState(hasPrevious)
    val hasNextLatest = rememberUpdatedState(hasNext)
    val imageScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.85f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "albumArtScale"
    )

    LaunchedEffect(currentTrack?.uri) {
        if (pagerState.currentPage != 1 || pagerState.targetPage != 1) {
            pagerState.scrollToPage(1)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                when (page) {
                    0 -> if (hasPreviousLatest.value) {
                        onSkipPrevious()
                    } else {
                        pagerState.animateScrollToPage(1)
                    }
                    2 -> if (hasNextLatest.value) {
                        onSkipNext()
                    } else {
                        pagerState.animateScrollToPage(1)
                    }
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(imageSize + 136.dp)
            .clipToBounds(),
        beyondViewportPageCount = 1,
        userScrollEnabled = hasPrevious || hasNext,
    ) { page ->
        val track = when (page) {
            0 -> previousTrack
            2 -> nextTrack
            else -> currentTrack
        }
        val artworkUrl = track?.album?.getCover(CoverSize.LARGE)?.uri
            ?.let { ALBUM_COVER_URL + it }
        val scale = if (page == 1) imageScale else 1f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SmartImage(
                url = artworkUrl,
                imageSize = imageSize,
                monochrome = LocalUiSettings.current.monochromePlayer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
            Spacer(Modifier.height(16.dp))
            TrackInfo(track, onArtistClick)
        }
    }
}

@Composable
private fun TrackInfo(track: Track?, onArtistClick: (Artist) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (track?.explicit ?: false) {
                Icon(
                    imageVector = Icons.Filled.Explicit,
                    contentDescription = "Explicit",
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = track?.name ?: "---",
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            track?.artists?.forEachIndexed { index, artist ->
                Text(
                    text = artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.combinedClickable(
                        onClick = { onArtistClick(artist) },
                        onLongClick = {}
                    )
                )
                if (index < track.artists.lastIndex) {
                    Text(
                        ", ", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomActionsBar(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconToggleButton(
            checked = isFavorite,
            onCheckedChange = { onFavoriteToggle() },
            modifier = Modifier.size(52.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = {
                    (scaleIn(
                        spring(
                            Spring.DampingRatioMediumBouncy,
                            Spring.StiffnessMedium
                        )
                    ) + fadeIn()) togetherWith
                            (scaleOut() + fadeOut())
                },
                label = "favoriteIcon"
            ) { fav ->
                Icon(
                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (fav) "Remove from favorites" else "Add to favorites"
                )
            }
        }

        IconButton(onClick = onMoreOptions, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TrackProgressBar(
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit = {},
) {
    var isDragging by remember { mutableStateOf(false) }
    var targetSliderValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(positionMs, durationMs, isDragging) {
        if (!isDragging && durationMs > 0) {
            targetSliderValue = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        }
    }

    val animatedSliderValue by animateFloatAsState(
        targetValue = targetSliderValue,
        animationSpec = tween(durationMillis = 300),
        label = "sliderAnimation"
    )

    val currentValue = if (isDragging) targetSliderValue else animatedSliderValue

    val displayedMs = (currentValue * durationMs).toLong().coerceIn(0L, durationMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "${formatTime(displayedMs)} / ${formatTime(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )
        WavyMusicSlider(
            value = currentValue,
            onValueChange = {
                isDragging = true
                targetSliderValue = it.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                onSeek((targetSliderValue * durationMs).toLong().coerceIn(0L, durationMs))
                isDragging = false
            },
            isPlaying = isPlaying
        )
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0L)
    return "%02d:%02d".format(s / 60, s % 60)
}

private enum class PlaybackIconState { Buffering, Playing, Paused }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    isShuffling: Boolean,
    isRepeating: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFastForward: () -> Unit,
    onFastRewind: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    showFastForward: Boolean,
) {
    val iconState = when {
        isBuffering -> PlaybackIconState.Buffering
        isPlaying -> PlaybackIconState.Playing
        else -> PlaybackIconState.Paused
    }

    // SpaceEvenly ensures buttons scale gracefully on narrow screens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconToggleButton(
            checked = isShuffling,
            onCheckedChange = { onShuffle() },
            modifier = Modifier.size(42.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            Icon(if(isShuffling)MyIcons.Shuffle else MyIcons.NoShuffle, contentDescription = "Shuffle", modifier = Modifier.size(20.dp))
        }

        IconButton(onClick = onPrevious, modifier = Modifier.size(42.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Previous",
                modifier = Modifier.size(42.dp)
            )
        }

        if(showFastForward) {
            IconButton(onClick = onFastRewind, modifier = Modifier.size(42.dp)) {
                Icon(
                    Icons.Default.FastRewind, contentDescription = "FR 15s",
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        FilledIconButton(
            onClick = onPlayPause,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            modifier = Modifier.size(96.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            AnimatedContent(
                targetState = iconState,
                transitionSpec = {
                    (scaleIn(
                        spring(
                            Spring.DampingRatioMediumBouncy,
                            Spring.StiffnessMediumLow
                        )
                    ) + fadeIn()) togetherWith
                            (scaleOut() + fadeOut())
                },
                label = "playPauseIcon"
            ) { state ->
                when (state) {
                    PlaybackIconState.Buffering -> LoadingIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    PlaybackIconState.Playing -> Icon(
                        Icons.Outlined.Pause, "Pause",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(42.dp)
                    )

                    PlaybackIconState.Paused -> Icon(
                        Icons.Outlined.PlayArrow, "Play",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(42.dp)
                    )
                }
            }
        }

        if(showFastForward) {
            IconButton(onClick = onFastForward, modifier = Modifier.size(42.dp)) {
                Icon(
                    Icons.Default.FastForward, contentDescription = "FF 15s",
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Next",
                modifier = Modifier.size(42.dp)
            )
        }

        FilledTonalIconToggleButton(
            checked = isRepeating,
            onCheckedChange = { onRepeat() },
            modifier = Modifier.size(42.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            Icon(Icons.Outlined.Repeat, contentDescription = "Repeat")
        }
    }
}

@Composable
fun Lyrics(
    loadLyrics: () -> Unit,
    track: Track?,
    lyrics: List<SyncedLyric>,
    currentLyric: SyncedLyric?,
    seekTo: (Long) -> Unit,
    romanize: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeIndex = lyrics.indexOf(currentLyric)

    fun scrollToActive(idx: Int) {
        val target = idx.coerceAtLeast(0)
        val offset = if (idx >= 0) (listState.layoutInfo.viewportEndOffset * 0.38f).toInt() else 0
        scope.launch { listState.animateScrollToItem(target, offset) }
    }

    LaunchedEffect(track?.id) {
        loadLyrics()
        scrollToActive(activeIndex)
    }
    LaunchedEffect(currentLyric?.timeMs, lyrics.hashCode()) {
        scrollToActive(activeIndex)
    }

    if (lyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "♪", style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                )
                Text(
                    "No lyrics available", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics, key = { i, item -> item.timeMs.hashCode() * 31 + i }) { index, line ->
            LyricLine(
                line = line,
                isActive = index == activeIndex,
                distance = if (activeIndex >= 0) abs(index - activeIndex) else Int.MAX_VALUE,
                onClick = { seekTo(line.timeMs) },
                romanize = romanize,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricLine(
    line: SyncedLyric,
    isActive: Boolean,
    distance: Int,
    onClick: () -> Unit,
    romanize: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1.00f
            distance == 1 -> 0.65f
            distance == 2 -> 0.38f
            else -> 0.18f
        },
        animationSpec = tween(280), label = "lyricAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isActive -> 1.00f
            distance == 1 -> 0.96f
            distance == 2 -> 0.93f
            else -> 0.90f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "lyricScale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(250), label = "lyricPillAlpha"
    )
    val verticalPad by animateFloatAsState(
        targetValue = if (isActive) 14f else 5f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "lyricVertPad"
    )

    var tapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (tapped) 1.06f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "lyricTapScale"
    )
    LaunchedEffect(tapped) {
        if (tapped) {
            delay(80); tapped = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha; scaleX = scale * tapScale; scaleY = scale * tapScale
            }
            .padding(horizontal = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = pillAlpha * 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { tapped = true; onClick() }
            .padding(horizontal = 20.dp, vertical = verticalPad.dp),
        contentAlignment = Alignment.Center
    ) {
        val romanizedText = remember(line.text, romanize) {
            if (romanize) {
                val result = RomanizationUtil.romanize(line.text)
                if (result != line.text) result else null
            } else null
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = line.text,
                style = when {
                    isActive -> MaterialTheme.typography.headlineSmall
                    distance == 1 -> MaterialTheme.typography.titleLarge
                    distance == 2 -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.bodyLarge
                },
                fontWeight = when {
                    isActive -> FontWeight.ExtraBold
                    distance == 1 -> FontWeight.SemiBold
                    distance == 2 -> FontWeight.Normal
                    else -> FontWeight.Light
                },
                color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isActive) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (romanizedText != null) {
                Text(
                    text = romanizedText,
                    style = when {
                        isActive -> MaterialTheme.typography.titleSmall
                        distance == 1 -> MaterialTheme.typography.labelLarge
                        distance == 2 -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.labelSmall
                    },
                    fontWeight = FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = if (isActive) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}