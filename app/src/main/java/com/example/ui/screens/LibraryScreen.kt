package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.FolderItem
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.model.TrackSortOrder
import com.example.ui.components.TrackArtworkView
import com.example.ui.theme.PureRed
import com.example.ui.viewmodel.AudioPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: AudioPlayerViewModel) {
    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val allTracksList by viewModel.allTracks.collectAsStateWithLifecycle()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.allArtists.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()

    val liveCurrentTrack by viewModel.liveCurrentTrack.collectAsStateWithLifecycle()
    val lyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val activeLyricIndex by viewModel.activeLyricIndex.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val folderItems by viewModel.folderItems.collectAsStateWithLifecycle()
    val currentFolderPath by viewModel.currentFolderPath.collectAsStateWithLifecycle()

    // Preferences & Settings
    val currentTheme by viewModel.currentThemeMode.collectAsStateWithLifecycle()
    val currentSortOrder by viewModel.currentSortOrder.collectAsStateWithLifecycle()
    val crossfadeSec by viewModel.crossfadeSec.collectAsStateWithLifecycle()
    val isGaplessEnabled by viewModel.isGaplessEnabled.collectAsStateWithLifecycle()

    // Multi-Select
    val isMultiSelect by viewModel.isMultiSelectActive.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()

    // UI sheet & dialog visibility
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val showEqualizer by viewModel.showEqualizerDialog.collectAsStateWithLifecycle()
    val showSleepTimer by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
    val showQueue by viewModel.showQueueSheet.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettingsSheet.collectAsStateWithLifecycle()
    val showCreatePlaylist by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val trackForPlaylist by viewModel.trackForPlaylistSelection.collectAsStateWithLifecycle()
    val trackForTagEdit by viewModel.trackForTagEditing.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylistDetail.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.playlistDetailTracks.collectAsStateWithLifecycle()
    val showDeleteConfirm by viewModel.showDeleteConfirmDialog.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val tabs = listOf("Tracks", "Albums", "Artists", "Playlists", "Folders")

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (!isMultiSelect) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_audiowave_logo),
                                        contentDescription = "Audiowave Logo",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Audiowave",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isSearchActive = !isSearchActive },
                                    modifier = Modifier.size(44.dp).testTag("search_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.showEqualizerDialog.value = true },
                                    modifier = Modifier.size(44.dp).testTag("top_equalizer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = "Equalizer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.showSettingsSheet.value = true },
                                    modifier = Modifier.size(44.dp).testTag("top_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedIds.size} Selected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select All", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSearchActive && !isMultiSelect) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search title, artist, or album...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("search_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Horizontally scrollable category tabs
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = tabTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag("tab_$tabTitle")
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (isMultiSelect && selectedIds.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("multi_select_action_bar"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.playSelectedNext() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("multi_play_next_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Next", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.trackForPlaylistSelection.value = tracks.firstOrNull { selectedIds.contains(it.id) }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("multi_add_playlist_button")
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Playlist", fontSize = 12.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.showDeleteConfirmDialog.value = true },
                                    modifier = Modifier.testTag("multi_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = PureRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mini-Player Docked above nav bar with isolated progress flow
                    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                    val activeTrack = liveCurrentTrack ?: playbackState.currentTrack
                    if (activeTrack != null && !isMultiSelect) {
                        MiniPlayer(
                            track = activeTrack,
                            isPlaying = playbackState.isPlaying,
                            durationMs = playbackState.durationMs.takeIf { it > 0L } ?: activeTrack.durationMs,
                            currentPositionFlow = viewModel.currentPositionMs,
                            onExpand = { viewModel.isNowPlayingExpanded.value = true },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onSkipPrevious = { viewModel.skipPrevious() }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> TracksTab(
                        tracks = tracks,
                        currentTrackId = liveCurrentTrack?.id,
                        isMultiSelect = isMultiSelect,
                        selectedIds = selectedIds,
                        currentSortOrder = currentSortOrder,
                        onSetSortOrder = { viewModel.setSortOrder(it) },
                        onTrackClick = { track ->
                            if (isMultiSelect) {
                                viewModel.toggleTrackSelection(track.id)
                            } else {
                                viewModel.playTrack(track, allTracksList)
                            }
                        },
                        onTrackLongClick = { track ->
                            if (!isMultiSelect) {
                                viewModel.startMultiSelect(track.id)
                            } else {
                                viewModel.toggleTrackSelection(track.id)
                            }
                        },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onAddToPlaylist = { track -> viewModel.trackForPlaylistSelection.value = track },
                        onEditTags = { track -> viewModel.trackForTagEditing.value = track }
                    )
                    1 -> AlbumsTab(
                        albums = albums,
                        onAlbumClick = { album ->
                            val albumTracks = allTracksList.filter { it.album == album.title }
                            if (albumTracks.isNotEmpty()) {
                                viewModel.playTracks(albumTracks, 0)
                            }
                        }
                    )
                    2 -> ArtistsTab(
                        artists = artists,
                        onArtistClick = { artist ->
                            val artistTracks = allTracksList.filter { it.artist == artist.name }
                            if (artistTracks.isNotEmpty()) {
                                viewModel.playTracks(artistTracks, 0)
                            }
                        }
                    )
                    3 -> PlaylistsTab(
                        playlists = playlists,
                        favoriteTracksCount = favoriteTracks.size,
                        mostPlayedCount = mostPlayedTracks.size,
                        onOpenLikedSongs = {
                            viewModel.openPlaylistDetail(
                                Playlist(id = -1L, name = "Liked Songs", trackCount = favoriteTracks.size, isSystem = true)
                            )
                        },
                        onOpenMostPlayed = {
                            viewModel.openPlaylistDetail(
                                Playlist(id = -2L, name = "Most Played", trackCount = mostPlayedTracks.size, isSystem = true)
                            )
                        },
                        onPlaylistClick = { playlist -> viewModel.openPlaylistDetail(playlist) },
                        onCreatePlaylist = { viewModel.showCreatePlaylistDialog.value = true },
                        onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist.id) }
                    )
                    4 -> FoldersTab(
                        folderPath = currentFolderPath,
                        items = folderItems,
                        onNavigateFolder = { path -> viewModel.loadFolder(path) },
                        onNavigateUp = { viewModel.navigateFolderUp() },
                        onPlayTrack = { track -> viewModel.playTrack(track, listOf(track)) },
                        onPlayAll = { viewModel.playAllInFolder() }
                    )
                }
            }
        }

        // Full Screen Now Playing Sheet Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
            NowPlayingSheet(
                playbackState = playbackState,
                currentPositionFlow = viewModel.currentPositionMs,
                currentTrack = liveCurrentTrack,
                isFavorite = favoriteTrackIds.contains(liveCurrentTrack?.id ?: -1L),
                lyrics = lyrics,
                activeLyricIndex = activeLyricIndex,
                onDismiss = { viewModel.isNowPlayingExpanded.value = false },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onSetSpeed = { speed -> viewModel.setSpeed(speed) },
                onOpenEqualizer = { viewModel.showEqualizerDialog.value = true },
                onOpenSleepTimer = { viewModel.showSleepTimerDialog.value = true },
                onOpenQueue = { viewModel.showQueueSheet.value = true },
                onAddToPlaylist = { track -> viewModel.trackForPlaylistSelection.value = track },
                onToggleFavorite = { track -> viewModel.toggleFavorite(track) }
            )
        }

        if (showEqualizer) {
            EqualizerSheet(
                equalizerState = equalizerState,
                onToggleEnable = { viewModel.toggleEqualizer(it) },
                onSetBandLevel = { band, level -> viewModel.setEqualizerBand(band, level) },
                onApplyPreset = { preset -> viewModel.applyEqualizerPreset(preset) },
                onSetBassBoost = { strength -> viewModel.setBassBoost(strength) },
                onDismiss = { viewModel.showEqualizerDialog.value = false }
            )
        }

        if (showSettings) {
            val pauseOnHeadset by viewModel.pauseOnHeadsetDisconnect.collectAsStateWithLifecycle()
            val duckOnNotification by viewModel.duckVolumeOnNotification.collectAsStateWithLifecycle()
            val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

            SettingsSheet(
                currentTheme = currentTheme,
                crossfadeSec = crossfadeSec,
                isGaplessEnabled = isGaplessEnabled,
                pauseOnHeadsetDisconnect = pauseOnHeadset,
                duckVolumeOnNotification = duckOnNotification,
                keepScreenOn = keepScreenOn,
                onSelectTheme = { viewModel.setThemeMode(it) },
                onSetCrossfade = { viewModel.setCrossfade(it) },
                onToggleGapless = { viewModel.setGapless(it) },
                onTogglePauseOnHeadsetDisconnect = { viewModel.setPauseOnHeadsetDisconnect(it) },
                onToggleDuckVolumeOnNotification = { viewModel.setDuckVolumeOnNotification(it) },
                onToggleKeepScreenOn = { viewModel.setKeepScreenOn(it) },
                onRescanLibrary = {
                    viewModel.refreshLibrary()
                    viewModel.showSettingsSheet.value = false
                },
                onResetDemoTracks = {
                    viewModel.resetDemoTracks()
                    viewModel.showSettingsSheet.value = false
                },
                onDismiss = { viewModel.showSettingsSheet.value = false }
            )
        }

        if (showSleepTimer) {
            val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
            SleepTimerDialog(
                currentRemainingSec = playbackState.sleepTimerRemainingSec,
                isEndOfTrack = playbackState.isSleepTimerEndOfTrack,
                onSetTimer = { min -> viewModel.setSleepTimer(min) },
                onSetTimerEndOfTrack = { viewModel.setSleepTimerEndOfTrack() },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.showSleepTimerDialog.value = false }
            )
        }

        if (showQueue) {
            QueueSheet(
                queue = currentQueue,
                currentTrack = liveCurrentTrack,
                onTrackSelect = { track -> viewModel.playTrack(track, currentQueue) },
                onRemoveItem = { index -> viewModel.removeQueueItem(index) },
                onMoveItem = { from, to -> viewModel.reorderQueue(from, to) },
                onDismiss = { viewModel.showQueueSheet.value = false }
            )
        }

        if (showCreatePlaylist) {
            CreatePlaylistDialog(
                onConfirm = { name ->
                    viewModel.createPlaylist(name)
                    viewModel.showCreatePlaylistDialog.value = false
                },
                onDismiss = { viewModel.showCreatePlaylistDialog.value = false }
            )
        }

        trackForPlaylist?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                selectedCount = if (isMultiSelect) selectedIds.size else 1,
                onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                onSelectPlaylist = { playlistId ->
                    if (isMultiSelect) {
                        viewModel.addSelectedToPlaylist(playlistId)
                    } else {
                        viewModel.addTrackToPlaylist(playlistId, track.id)
                    }
                },
                onDismiss = { viewModel.trackForPlaylistSelection.value = null }
            )
        }

        trackForTagEdit?.let { track ->
            TagEditorDialog(
                track = track,
                onSave = { title, artist, album, year, trackNum ->
                    viewModel.saveTrackTags(track, title, artist, album, year, trackNum)
                },
                onDismiss = { viewModel.trackForTagEditing.value = null }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.showDeleteConfirmDialog.value = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Delete Tracks?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove ${selectedIds.size} selected tracks from your library?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteSelectedTracks() },
                        colors = ButtonDefaults.buttonColors(containerColor = PureRed, contentColor = Color.White)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDeleteConfirmDialog.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        selectedPlaylist?.let { playlist ->
            PlaylistDetailView(
                playlist = playlist,
                tracks = playlistTracks,
                onPlayAll = { if (playlistTracks.isNotEmpty()) viewModel.playTracks(playlistTracks, 0) },
                onTrackClick = { t -> viewModel.playTrack(t, playlistTracks) },
                onRemoveTrack = { t ->
                    if (!playlist.isSystem) {
                        viewModel.removeTrackFromPlaylist(playlist.id, t.id)
                    } else if (playlist.id == -1L) {
                        viewModel.toggleFavorite(t)
                    }
                },
                onExportM3u = { viewModel.exportPlaylistToM3u(playlist) },
                onBack = { viewModel.closePlaylistDetail() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksTab(
    tracks: List<Track>,
    currentTrackId: Long?,
    isMultiSelect: Boolean,
    selectedIds: Set<Long>,
    currentSortOrder: TrackSortOrder,
    onSetSortOrder: (TrackSortOrder) -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onEditTags: (Track) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                FilterChip(
                    selected = true,
                    onClick = { sortMenuExpanded = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sort: ${currentSortOrder.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sort_chip")
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    TrackSortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    fontWeight = if (order == currentSortOrder) FontWeight.Bold else FontWeight.Normal,
                                    color = if (order == currentSortOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSetSortOrder(order)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "${tracks.size} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No audio tracks found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            var activeMenuTrack by remember { mutableStateOf<Track?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize().testTag("tracks_list")
                ) {
                    items(
                        items = tracks,
                        key = { it.id },
                        contentType = { "track_row" }
                    ) { track ->
                        val isCurrent = track.id == currentTrackId
                        val isSelected = selectedIds.contains(track.id)

                        TrackItemRow(
                            track = track,
                            isCurrent = isCurrent,
                            isSelected = isSelected,
                            isMultiSelect = isMultiSelect,
                            onTrackClick = { onTrackClick(track) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onOpenMenu = { activeMenuTrack = track }
                        )
                    }
                }

                if (activeMenuTrack != null) {
                    val selTrack = activeMenuTrack!!
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { activeMenuTrack = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = {
                                activeMenuTrack = null
                                onTrackLongClick(selTrack)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                activeMenuTrack = null
                                onAddToPlaylist(selTrack)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Tags") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                activeMenuTrack = null
                                onEditTags(selTrack)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItemRow(
    track: Track,
    isCurrent: Boolean,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onTrackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitleText = remember(track.artist, track.durationMs) {
        "${track.artist} • ${formatDuration(track.durationMs)}"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onTrackClick)
            .testTag("track_row_${track.id}"),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTrackClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkView(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    placeholderIconSize = 28.dp,
                    targetSizePx = 138,
                    contentDescription = track.title
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isMultiSelect) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) PureRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumsTab(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    if (albums.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("albums_grid")
        ) {
            items(
                items = albums,
                key = { "${it.id}_${it.title}" },
                contentType = { "album_card" }
            ) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) }
                        .testTag("album_card_${album.title}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.trackCount} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    if (artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize().testTag("artists_list")
        ) {
            items(
                items = artists,
                key = { it.name },
                contentType = { "artist_row" }
            ) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("artist_row_${artist.name}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${artist.albumCount} albums • ${artist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    favoriteTracksCount: Int,
    mostPlayedCount: Int,
    onOpenLikedSongs: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    val customPlaylists = remember(playlists) { playlists.filter { !it.isSystem } }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = Modifier.fillMaxSize().testTag("playlists_list")
    ) {
        item(key = "hero_liked_songs") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onOpenLikedSongs() }
                    .testTag("liked_songs_playlist_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PureRed, Color(0xFFB71C1C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liked Songs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$favoriteTracksCount tracks favorited",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PureRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Liked",
                            tint = PureRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item(key = "hero_most_played") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenMostPlayed() }
                    .testTag("most_played_playlist_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Most Played",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Most Played",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$mostPlayedCount frequently played tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Most Played",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item(key = "create_playlist_action") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreatePlaylist() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("create_playlist_button"),
                color = Color.Transparent
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Create New Playlist",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        items(
            items = customPlaylists,
            key = { it.id },
            contentType = { "playlist_row" }
        ) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("playlist_row_${playlist.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${playlist.trackCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onDeletePlaylist(playlist) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FoldersTab(
    folderPath: String,
    items: List<FolderItem>,
    onNavigateFolder: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().testTag("folders_tab")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(36.dp).testTag("folder_navigate_up")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = folderPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPlayAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("play_all_folder_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Folder is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = items,
                    key = { it.path },
                    contentType = { if (it.isDirectory) "folder_dir" else "folder_file" }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.isDirectory) {
                                    onNavigateFolder(item.path)
                                } else if (item.track != null) {
                                    onPlayTrack(item.track)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("folder_item_${item.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = if (item.isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.isDirectory) {
                                Text(
                                    text = "${item.trackCount} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    onPlayAll: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onRemoveTrack: (Track) -> Unit,
    onExportM3u: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("playlist_detail_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tracks.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    if (!playlist.isSystem) {
                        IconButton(
                            onClick = onExportM3u,
                            modifier = Modifier.testTag("export_m3u_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Export M3U",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This playlist has no tracks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = tracks,
                        key = { it.id },
                        contentType = { "playlist_track_row" }
                    ) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackClick(track) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                TrackArtworkView(
                                    track = track,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIconSize = 24.dp,
                                    targetSizePx = 120,
                                    contentDescription = track.title
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemoveTrack(track) }) {
                                Icon(
                                    imageVector = if (playlist.id == -1L) Icons.Default.Favorite else Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = if (playlist.id == -1L) PureRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "New Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth().testTag("new_playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_create_playlist")
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    selectedCount: Int = 1,
    onCreatePlaylist: (String) -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val headerText = if (selectedCount > 1) {
                    "Select a playlist for $selectedCount selected tracks:"
                } else {
                    "Select a playlist for \"${track.title}\":"
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isCreatingNew) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_to_new_playlist_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCreatingNew = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName.trim())
                                    newPlaylistName = ""
                                    isCreatingNew = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Create & Save")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_create_playlist_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Playlist", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val customPlaylists = playlists.filter { !it.isSystem }
                if (customPlaylists.isEmpty() && !isCreatingNew) {
                    Text(
                        text = "No custom playlists found. Create one above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(
                            items = customPlaylists,
                            key = { it.id }
                        ) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylist(p.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${p.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}