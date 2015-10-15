package ti.modules.titanium.audio.streamer;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.audio.AudioTrack.WriteException;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import android.content.Context;
import android.media.MediaCodec.CryptoException;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Surface;
import android.widget.MediaController.MediaPlayerControl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It
 * can be prepared with one of a number of {@link RendererBuilder} classes to
 * suit different use cases (e.g. DASH, SmoothStreaming and so on).
 */
public class StreamerExoPlayer implements ExoPlayer.Listener,
        ChunkSampleSource.EventListener, DefaultBandwidthMeter.EventListener,
        MediaCodecVideoTrackRenderer.EventListener,
        MediaCodecAudioTrackRenderer.EventListener,
        TextRenderer,
        StreamingDrmSessionManager.EventListener, MediaPlayerControl {

    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Constructs the necessary components for playback.
         *
         * @param player
         *            The parent player.
         * @param callback
         *            The callback to invoke with the constructed components.
         */
        void buildRenderers(StreamerExoPlayer player,
                RendererBuilderCallback callback);
    }

    /**
     * A callback invoked by a {@link RendererBuilder}.
     */
    public interface RendererBuilderCallback {
        /**
         * Invoked with the results from a {@link RendererBuilder}.
         *
         * @param trackNames
         *            The names of the available tracks, indexed by
         *            {@link StreamerExoPlayer} TYPE_* constants. May be null if
         *            the track names are unknown. An individual element may be
         *            null if the track names are unknown for the corresponding
         *            type.
         * @param multiTrackSources
         *            Sources capable of switching between multiple available
         *            tracks, indexed by {@link StreamerExoPlayer} TYPE_*
         *            constants. May be null if there are no types with multiple
         *            tracks. An individual element may be null if it does not
         *            have multiple tracks.
         * @param renderers
         *            Renderers indexed by {@link StreamerExoPlayer} TYPE_*
         *            constants. An individual element may be null if there do
         *            not exist tracks of the corresponding type.
         */
        void onRenderers(String[][] trackNames,
                MultiTrackChunkSource[] multiTrackSources,
                TrackRenderer[] renderers);

        /**
         * Invoked if a {@link RendererBuilder} encounters an error.
         *
         * @param e
         *            Describes the error.
         */
        void onRenderersError(Exception e);
    }

    /**
     * A listener for core events.
     */
    public interface Listener {
        void onStateChanged(StreamerExoPlayer player, boolean playWhenReady, int playbackState);

        void onError(StreamerExoPlayer player, Exception e);

        void onVideoSizeChanged(StreamerExoPlayer player, int width, int height,
                float pixelWidthHeightRatio);
    }

    /**
     * A listener for internal errors.
     * <p>
     * These errors are not visible to the user, and hence this listener is
     * provided for informational purposes only. Note however that an internal
     * error may cause a fatal error if the player fails to recover. If this
     * happens, {@link Listener#onError(Exception)} will be invoked.
     */
    public interface InternalErrorListener {
        void onRendererInitializationError(StreamerExoPlayer player, Exception e);

        void onAudioTrackInitializationError(
                StreamerExoPlayer player, AudioTrack.InitializationException e);

        void onDecoderInitializationError(StreamerExoPlayer player, DecoderInitializationException e);

        void onCryptoError(StreamerExoPlayer player, CryptoException e);

        void onUpstreamError(StreamerExoPlayer player, int sourceId, IOException e);

        void onConsumptionError(StreamerExoPlayer player, int sourceId, IOException e);

        void onDrmSessionManagerError(StreamerExoPlayer player, Exception e);
        void onAudioTrackWriteError(StreamerExoPlayer player, AudioTrack.WriteException e);
    }

    /**
     * A listener for debugging information.
     */
    public interface InfoListener {
        void onVideoFormatEnabled(StreamerExoPlayer player, String formatId, int trigger, int mediaTimeMs);

        void onAudioFormatEnabled(StreamerExoPlayer player, String formatId, int trigger, int mediaTimeMs);

        void onDroppedFrames(StreamerExoPlayer player, int count, long elapsed);

        void onBandwidthSample(StreamerExoPlayer player, int elapsedMs, long bytes, long bitrateEstimate);

        void onLoadStarted(StreamerExoPlayer player, int sourceId, String formatId, int trigger,
                boolean isInitialization, int mediaStartTimeMs,
                int mediaEndTimeMs, long length);

        void onLoadCompleted(StreamerExoPlayer player, int sourceId, long bytesLoaded);
    }

    /**
     * A listener for receiving notifications of timed text.
     */
    public interface TextListener {
        void onText(StreamerExoPlayer player, String text);
    }

    /**
     * A listener for receiving ID3 metadata parsed from the media stream.
     */
    public interface Id3MetadataListener {
        void onId3Metadata(StreamerExoPlayer player, Map<String, Object> metadata);
    }

    // Constants pulled into this class for convenience.
    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    public static final int DISABLED_TRACK = -1;
    public static final int PRIMARY_TRACK = 0;

    public static final int RENDERER_COUNT = 5;
    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_TIMED_METADATA = 3;
    public static final int TYPE_DEBUG = 4;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final RendererBuilder rendererBuilder;
    private final ExoPlayer player;
    private final PlayerControl playerControl;
    private final Handler mainHandler;
    private final CopyOnWriteArrayList<Listener> listeners;
    private final StringBuilder closedCaptionStringBuilder;

    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private Surface surface;
    private InternalRendererBuilderCallback builderCallback;
    private TrackRenderer videoRenderer;
    private MediaCodecAudioTrackRenderer audioRenderer;

    private MultiTrackChunkSource[] multiTrackSources;
    private String[][] trackNames;
    private int[] selectedTracks;

    private TextListener textListener;
    private Id3MetadataListener id3MetadataListener;
    private InternalErrorListener internalErrorListener;
    private InfoListener infoListener;
    
    private PowerManager.WakeLock wakeLock;

    public StreamerExoPlayer(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        player.addListener(this);
        playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<Listener>();
        lastReportedPlaybackState = STATE_IDLE;
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        selectedTracks = new int[RENDERER_COUNT];
        // Disable text initially.
        selectedTracks[TYPE_TEXT] = DISABLED_TRACK;
        closedCaptionStringBuilder = new StringBuilder();
    }

    public PlayerControl getPlayerControl() {
        return playerControl;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setInternalErrorListener(InternalErrorListener listener) {
        internalErrorListener = listener;
    }

    public void setInfoListener(InfoListener listener) {
        infoListener = listener;
    }

    public void setTextListener(TextListener listener) {
        textListener = listener;
    }

    public void setMetadataListener(Id3MetadataListener listener) {
        id3MetadataListener = listener;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        pushSurfaceAndVideoTrack(false);
    }

    public Surface getSurface() {
        return surface;
    }

    public void blockingClearSurface() {
        surface = null;
        pushSurfaceAndVideoTrack(true);
    }

    public String[] getTracks(int type) {
        return trackNames == null ? null : trackNames[type];
    }

    public int getSelectedTrackIndex(int type) {
        return selectedTracks[type];
    }

    public void selectTrack(int type, int index) {
        if (selectedTracks[type] == index) {
            return;
        }
        selectedTracks[type] = index;
        if (type == TYPE_VIDEO) {
            pushSurfaceAndVideoTrack(false);
        } else {
            pushTrackSelection(type, true);
            if (type == TYPE_TEXT && index == DISABLED_TRACK
                    && textListener != null) {
                textListener.onText(this, null);
            }
        }
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        if (builderCallback != null) {
            builderCallback.cancel();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        maybeReportPlayerState();
        builderCallback = new InternalRendererBuilderCallback();
        rendererBuilder.buildRenderers(this, builderCallback);
    }

    /* package */void onRenderers(String[][] trackNames,
            MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers) {
        builderCallback = null;
        // Normalize the results.
        if (trackNames == null) {
            trackNames = new String[RENDERER_COUNT][];
        }
        if (multiTrackSources == null) {
            multiTrackSources = new MultiTrackChunkSource[RENDERER_COUNT];
        }
        for (int i = 0; i < RENDERER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            } else if (trackNames[i] == null) {
                // We have a renderer so we must have at least one track, but
                // the names are unknown.
                // Initialize the correct number of null track names.
                int trackCount = multiTrackSources[i] == null ? 1
                        : multiTrackSources[i].getTrackCount();
                trackNames[i] = new String[trackCount];
            }
        }
        // Complete preparation.
        this.videoRenderer = renderers[TYPE_VIDEO];
        this.audioRenderer = (MediaCodecAudioTrackRenderer) renderers[TYPE_AUDIO];
        this.trackNames = trackNames;
        this.multiTrackSources = multiTrackSources;
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
        pushSurfaceAndVideoTrack(false);
        pushTrackSelection(TYPE_AUDIO, true);
        pushTrackSelection(TYPE_TEXT, true);
        player.prepare(renderers);
    }

    /* package */void onRenderersError(Exception e) {
        builderCallback = null;
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(this, e);
        }
        for (Listener listener : listeners) {
            listener.onError(this, e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        maybeReportPlayerState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void release() {
        if (builderCallback != null) {
            builderCallback.cancel();
            builderCallback = null;
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        surface = null;
        listeners.clear();
        infoListener = null;
        textListener = null;
        id3MetadataListener = null;
        internalErrorListener = null;
        infoListener = null;
        player.removeListener(this);
        player.release();
    }

    public int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return ExoPlayer.STATE_PREPARING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT
                && rendererBuildingState == RENDERER_BUILDING_STATE_IDLE) {
            // This is an edge case where the renderers are built, but are still
            // being passed to the
            // player's playback thread.
            return ExoPlayer.STATE_PREPARING;
        }
        return playerState;
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    /* package */Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }

    /* package */Handler getMainHandler() {
        return mainHandler;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        maybeReportPlayerState();
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(this, exception);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height,
            float pixelWidthHeightRatio) {
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(this, width, height, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        if (infoListener != null) {
            infoListener.onDroppedFrames(this, count, elapsed);
        }
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes,
            long bitrateEstimate) {
        if (infoListener != null) {
            infoListener.onBandwidthSample(this, elapsedMs, bytes, bitrateEstimate);
        }
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, String formatId,
            int trigger, int mediaTimeMs) {
        if (infoListener == null) {
            return;
        }
        if (sourceId == TYPE_VIDEO) {
            infoListener.onVideoFormatEnabled(this, formatId, trigger, mediaTimeMs);
        } else if (sourceId == TYPE_AUDIO) {
            infoListener.onAudioFormatEnabled(this, formatId, trigger, mediaTimeMs);
        }
    }

    @Override
    public void onDrmSessionManagerError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDrmSessionManagerError(this, e);
        }
    }

    @Override
    public void onDecoderInitializationError(DecoderInitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDecoderInitializationError(this, e);
        }
    }

    @Override
    public void onAudioTrackInitializationError(
            AudioTrack.InitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackInitializationError(this, e);
        }
    }

    @Override
    public void onCryptoError(CryptoException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onCryptoError(this, e);
        }
    }

    @Override
    public void onUpstreamError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onUpstreamError(this, sourceId, e);
        }
    }

    @Override
    public void onConsumptionError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onConsumptionError(this, sourceId, e);
        }
    }

    @Override
    public void onText(String text) {
        processText(text);
    }

    /* package */MetadataTrackRenderer.MetadataRenderer<Map<String, Object>> getId3MetadataRenderer() {
        return new MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>() {
            @Override
            public void onMetadata(Map<String, Object> metadata) {
                if (id3MetadataListener != null) {
                    id3MetadataListener.onId3Metadata(StreamerExoPlayer.this, metadata);
                }
            }
        };
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        // Do nothing.
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        // Do nothing.
    }

    @Override
    public void onLoadStarted(int sourceId, String formatId, int trigger,
            boolean isInitialization, int mediaStartTimeMs, int mediaEndTimeMs,
            long length) {
        if (infoListener != null) {
            infoListener.onLoadStarted(this, sourceId, formatId, trigger,
                    isInitialization, mediaStartTimeMs, mediaEndTimeMs, length);
        }
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(this, sourceId, bytesLoaded);
        }
    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        // Do nothing.
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs,
            int mediaEndTimeMs, long bytesDiscarded) {
        // Do nothing.
    }

    @Override
    public void onDownstreamDiscarded(int sourceId, int mediaStartTimeMs,
            int mediaEndTimeMs, long bytesDiscarded) {
        // Do nothing.
    }

    private void maybeReportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady
                || lastReportedPlaybackState != playbackState) {
            for (Listener listener : listeners) {
                listener.onStateChanged(this, playWhenReady, playbackState);
            }
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }

    private void pushSurfaceAndVideoTrack(boolean blockForSurfacePush) {
        if (rendererBuildingState != RENDERER_BUILDING_STATE_BUILT) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(videoRenderer,
                    MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        } else {
            player.sendMessage(videoRenderer,
                    MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        }
        pushTrackSelection(TYPE_VIDEO, surface != null && surface.isValid());
    }

    private void pushTrackSelection(int type, boolean allowRendererEnable) {
        if (rendererBuildingState != RENDERER_BUILDING_STATE_BUILT) {
            return;
        }

        int trackIndex = selectedTracks[type];
        if (trackIndex == DISABLED_TRACK) {
            player.setRendererEnabled(type, false);
        } else if (multiTrackSources[type] == null) {
            player.setRendererEnabled(type, allowRendererEnable);
        } else {
            boolean playWhenReady = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
            player.setRendererEnabled(type, false);
            player.sendMessage(multiTrackSources[type],
                    MultiTrackChunkSource.MSG_SELECT_TRACK, trackIndex);
            player.setRendererEnabled(type, allowRendererEnable);
            player.setPlayWhenReady(playWhenReady);
        }
    }

    /* package */void processText(String text) {
        if (textListener == null || selectedTracks[TYPE_TEXT] == DISABLED_TRACK) {
            return;
        }
        textListener.onText(this, text);
    }

    private class InternalRendererBuilderCallback implements
            RendererBuilderCallback {

        private boolean canceled;

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRenderers(String[][] trackNames,
                MultiTrackChunkSource[] multiTrackSources,
                TrackRenderer[] renderers) {
            if (!canceled) {
                StreamerExoPlayer.this.onRenderers(trackNames,
                        multiTrackSources, renderers);
            }
        }

        @Override
        public void onRenderersError(Exception e) {
            if (!canceled) {
                StreamerExoPlayer.this.onRenderersError(e);
            }
        }

    }
    
    private void releaseWakeLock() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }
    public void setWakeMode (Context context, int mode) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        releaseWakeLock();
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CAPPIC");
    }

 
    public void stop() {
        player.stop();
    }
    
    public void reset() {
        stop();
        seekTo(0);
    }

    public void setVolume(float vol) {
        if (audioRenderer != null) {
            player.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,
                    vol);
        }
        
    }
    

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    /**
     * This is an unsupported operation.
     * <p>
     * Application of audio effects is dependent on the audio renderer used.
     * When using
     * {@link com.google.android.exoplayer.MediaCodecAudioTrackRenderer},
     * the recommended approach is to extend the class and override
     * {@link com.google.android.exoplayer.MediaCodecAudioTrackRenderer#onAudioSessionId}.
     *
     * @throws UnsupportedOperationException
     *             Always thrown.
     */
    @Override
    public int getAudioSessionId() {
        throw new UnsupportedOperationException();
    }
    public void setAudioSessionId(int sessionId) {
    }

    @Override
    public int getBufferPercentage() {
        return player.getBufferedPercentage();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return (int) player.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public void start() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public void seekTo(int timeMillis) {
        // MediaController arrow keys generate unbounded values.
        player.seekTo(Math.min(Math.max(0, timeMillis), getDuration()));
    }

    @Override
    public void onAudioTrackWriteError(WriteException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackWriteError(this, e);
          }
    }
}
