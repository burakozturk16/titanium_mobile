package ti.modules.titanium.audio.streamer;

import android.app.Service;
import android.media.MediaCodec;
import android.media.MediaCodec.CryptoException;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.filesystem.FileProxy;
import ti.modules.titanium.audio.SoundProxy;
import ti.modules.titanium.audio.streamer.StreamerExoPlayer.InfoListener;
import ti.modules.titanium.audio.streamer.StreamerExoPlayer.InternalErrorListener;
import ti.modules.titanium.audio.streamer.StreamerExoPlayer.RendererBuilder;
import ti.modules.titanium.audio.streamer.StreamerExoPlayer.RendererBuilderCallback;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack.InitializationException;
import com.google.android.exoplayer.audio.AudioTrack.WriteException;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource.InvalidResponseCodeException;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

/**
 * A backbround {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
@SuppressWarnings("deprecation")
public class AudioStreamerExoService extends AudioService {
    private static final String TAG = "AudioStreamerExoService";
    
    protected static String getCmdPrefix() {
        return "ti.modules.titanium.audio.streamer";
    }
    
    
    @Override
    protected AudioPlayer createPlayer(AudioService audioService) {
        return new MultiPlayer((AudioStreamerExoService) audioService);
    }

    private class ExoRendererBuilder implements RendererBuilder,
            ManifestCallback<HlsPlaylist> {

        private final String userAgent = TiApplication.getInstance()
                .getUserAgent();
        private final String url;
        private boolean isHLS = false;

        private StreamerExoPlayer player;
        private RendererBuilderCallback callback;

        public ExoRendererBuilder(String url) {
            this.url = url;
             isHLS = !url.endsWith("mp3");
        }

        @Override
        public void buildRenderers(StreamerExoPlayer player,
                RendererBuilderCallback callback) {
            this.player = player;
            this.callback = callback;

            if (isHLS) {
                HlsPlaylistParser parser = new HlsPlaylistParser();
                ManifestFetcher<HlsPlaylist> playlistFetcher =
                        new ManifestFetcher<HlsPlaylist>(url, new DefaultHttpDataSource(userAgent, null), parser);
                playlistFetcher.singleLoad(player.getMainHandler().getLooper(),
                        this);
            } else {
                // Build the video and audio renderers.
                
                DefaultSampleSource sampleSource =
                        new DefaultSampleSource(new FrameworkSampleExtractor(AudioStreamerExoService.this, Uri.parse(url), null), 2);
                    MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                        null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, player.getMainHandler(),
                        player, 50);
                    MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                        null, true, player.getMainHandler(), player);

                    MetadataTrackRenderer<Map<String, Object>> id3Renderer = new MetadataTrackRenderer<Map<String, Object>>(
                            sampleSource, new Id3Parser(),
                            player.getId3MetadataRenderer(), player
                                    .getMainHandler().getLooper());
                    
                    // Invoke the callback.
                    TrackRenderer[] renderers = new TrackRenderer[StreamerExoPlayer.RENDERER_COUNT];
                    renderers[StreamerExoPlayer.TYPE_VIDEO] = videoRenderer;
                    renderers[StreamerExoPlayer.TYPE_AUDIO] = audioRenderer;
                    renderers[StreamerExoPlayer.TYPE_TIMED_METADATA] = id3Renderer;
//                    renderers[StreamerExoPlayer.TYPE_DEBUG] = debugRenderer;
                    callback.onRenderers(null, null, renderers);
            }

        }
        
        @Override
        public void onSingleManifest(HlsPlaylist manifest) {
            
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

            DataSource dataSource = new UriDataSource(userAgent, bandwidthMeter);
            HlsChunkSource chunkSource = new HlsChunkSource(dataSource, url, manifest, bandwidthMeter, null,
                HlsChunkSource.ADAPTIVE_MODE_SPLICE);
            HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, true, 3);
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, player.getMainHandler(), player, 50);
            MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

            MetadataTrackRenderer<Map<String, Object>> id3Renderer =
                new MetadataTrackRenderer<Map<String, Object>>(sampleSource, new Id3Parser(),
                    player.getId3MetadataRenderer(), player.getMainHandler().getLooper());

//            Eia608TrackRenderer closedCaptionRenderer = new Eia608TrackRenderer(sampleSource, player,
//                player.getMainHandler().getLooper());

            TrackRenderer[] renderers = new TrackRenderer[StreamerExoPlayer.RENDERER_COUNT];
            renderers[StreamerExoPlayer.TYPE_VIDEO] = videoRenderer;
            renderers[StreamerExoPlayer.TYPE_AUDIO] = audioRenderer;
            renderers[StreamerExoPlayer.TYPE_TIMED_METADATA] = id3Renderer;
//            renderers[DemoPlayer.TYPE_TEXT] = closedCaptionRenderer;
            callback.onRenderers(null, null, renderers);
            player.setVolume(mVolume);
        }

        @Override
        public void onSingleManifestError(IOException e) {
            callback.onRenderersError(e);
        }

    }
    
    private class PlayingItem implements BasePlayingItem {
        public String path = null;

        PlayingItem(String path) {
            this.path = path;
        }
    }

    private final class MultiPlayer extends AudioPlayer implements
            StreamerExoPlayer.Id3MetadataListener, StreamerExoPlayer.Listener,
            InfoListener, InternalErrorListener {

        private StreamerExoPlayer mCurrentMediaPlayer = null;
        private StreamerExoPlayer mNextMediaPlayer;
        private float mVolume = 1.0f;
        
        private int state = -1;

        private boolean mIsPaused = false;
        private boolean mNextIsPreparing = false;

        private PlayingItem mPlayingFile = null;
        private PlayingItem mNextPlayingFile = null;
        protected Timer mProgressTimer;

        /**
         * Constructor of <code>MultiPlayer</code>
         */
        public MultiPlayer(final AudioStreamerExoService service) {
            super(service);
        }

        private void releasePlayer(StreamerExoPlayer player) {
            if (player != null) {
                player.release();
            }
        }

        private boolean setDataSource(final String path) {
            if (mCurrentMediaPlayer != null) {
                releasePlayer(mCurrentMediaPlayer);
                mCurrentMediaPlayer = null;
            }
            if (mNextMediaPlayer != null) {
                releasePlayer(mNextMediaPlayer);
                mNextMediaPlayer = null;
            }
            if (path == null) {
                return false;
            }
            mCurrentMediaPlayer = new StreamerExoPlayer(
                    ((AudioStreamerExoService)getService()).new ExoRendererBuilder(path));
            mCurrentMediaPlayer.setPlayWhenReady(true);
            mCurrentMediaPlayer.addListener(this);
            mCurrentMediaPlayer.setMetadataListener(this);
            mCurrentMediaPlayer.setInfoListener(this);
            mCurrentMediaPlayer.setInternalErrorListener(this);
            mCurrentMediaPlayer.prepare();
            return true;
        }

        private boolean setNextDataSource(final String path) {
            if (mNextMediaPlayer != null) {
                releasePlayer(mNextMediaPlayer);
                mNextMediaPlayer = null;
            }
            
            if (path == null) {
                return false; 
            }
            mNextMediaPlayer = new StreamerExoPlayer(
                    ((AudioStreamerExoService)getService()).new ExoRendererBuilder(path));
            mNextMediaPlayer.setPlayWhenReady(false);
            mNextMediaPlayer.addListener(this);
            mNextMediaPlayer.setInfoListener(this);
            mNextMediaPlayer.setInternalErrorListener(this);
            mNextMediaPlayer.prepare();

            return true;
        }

        public boolean openFile(final Object object, final boolean preparingNext) {
            synchronized (this) {
                if (object == null) {
                    return false;
                }

                try {
                    String url = null;
                    if (object instanceof FileProxy) {
                        url = ((FileProxy) object).getNativePath();
                    } else if (object instanceof SoundProxy) {
                        url = ((SoundProxy) object).getUrl();
                    } else if (object instanceof HashMap) {
                        url = TiConvert.toString(((HashMap) object)
                                .get(TiC.PROPERTY_URL));
                    } else {
                        url = TiConvert.toString(object);
                    }
                    if (preparingNext) {
                        mNextIsPreparing = false;
                    } else {
                        mIsPreparing = false;
                    }
                    if (preparingNext) {
                        mNextIsPreparing = true;
                        mNextPlayingFile = new PlayingItem(url);
                        setNextDataSource(mNextPlayingFile.path);
                    } else {
                        mIsPreparing = true;
                        mPlayingFile = new PlayingItem(url);
                        mIsInitialized = setDataSource(mPlayingFile.path);
                    }

                    // }
                    // }

                } catch (Throwable t) {
                    Log.w(TAG, "Issue while initializing : ", t);
                    return false;
                }
                if (!preparingNext) {
                    if (mNextIsPreparing) {
                        mService.get().setState(State.STATE_STARTING);
                    } else if (mIsInitialized) {
                        mService.get().setState(State.STATE_INITIALIZED);
                    }
                    if (mIsInitialized && !mIsPreparing) {
                        mService.get().onStartPlaying(mPlayingFile);
                    }
                }

                return mIsInitialized;
            }
        }
        /**
         * @return True if the player is ready to go, false otherwise
         */
        public boolean isPlaying() {
            if (mCurrentMediaPlayer != null) {
                return mCurrentMediaPlayer.isPlaying();
            }
            return false;
        }

        public boolean isValid() {
            return isPrepared();
        }

        /**
         * @return True if the player is ready to go, false otherwise
         */
        public boolean isPaused() {
            return mIsPaused;
        }

        /**
         * Starts or resumes playback.
         */
        public void start() {
            if (mCurrentMediaPlayer == null)
                return;
            mCurrentMediaPlayer.start();
            mIsPaused = !mCurrentMediaPlayer.isPlaying();
            mService.get().setState(
                    !mIsPaused ? State.STATE_PLAYING : mService.get().mState);
            startProgressTimer();
        }

        /**
         * Resets the MediaPlayer to its uninitialized state.
         */
        public void stop() {
            if (mCurrentMediaPlayer != null) {
                mCurrentMediaPlayer.reset();
            }
            
            mPlayingFile = null;
            mIsInitialized = false;
            mIsPaused = false;
            mService.get().setState(State.STATE_STOPPED);
            stopProgressTimer();
        }

        /**
         * Releases resources associated with this MediaPlayer object.
         */
        public void release() {
            stop();
            releasePlayer(mCurrentMediaPlayer);
        }

        /**
         * Pauses playback. Call start() to resume.
         */
        public void pause() {
            if (mCurrentMediaPlayer == null) {
                return;
            }
            mCurrentMediaPlayer.pause();
            mIsPaused = true;
            mService.get().setState(State.STATE_PAUSED);
            stopProgressTimer();
        }

        /**
         * Gets the duration of the file.
         * 
         * @return The duration in milliseconds
         */
        public long duration() {
            if (mCurrentMediaPlayer != null) {
                return mCurrentMediaPlayer.getDuration();
            }
            return -1;
        }

        /**
         * Gets the current playback position.
         * 
         * @return The current position in milliseconds
         */
        public long position() {
            if (mCurrentMediaPlayer != null) {
                return mCurrentMediaPlayer.getCurrentPosition();
            }
            return 0;
        }

        /**
         * Sets the current playback position.
         * 
         * @param whereto
         *            The offset in milliseconds from the start to seek to
         * @return The offset in milliseconds from the start to seek to
         */
        public long seek(final long whereto) {
            if (mCurrentMediaPlayer != null) {
                mCurrentMediaPlayer.seekTo((int) whereto);
            }
            return whereto;
        }

        /**
         * Sets the volume on this player.
         * 
         * @param vol
         *            Left and right volume scalar
         */
        public void setVolume(final float vol) {
            mVolume = vol;
            if (mCurrentMediaPlayer != null) {
                mCurrentMediaPlayer.setVolume(vol);
            }
        }

        /**
         * Sets the audio session ID.
         * 
         * @param sessionId
         *            The audio session ID
         */
        public void setAudioSessionId(final int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        /**
         * Returns the audio session ID.
         * 
         * @return The current audio session ID.
         */
        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }

        public PlayingItem getPlayingFile() {
            return mPlayingFile;
        }
        
        @Override
        public void onLoadCompleted(StreamerExoPlayer player, int sourceId,
                long bytesLoaded) {
            if (player == mCurrentMediaPlayer) {
                mIsPreparing = false;
                mService.get().setState(State.STATE_INITIALIZED);
                mService.get().notifyChange(cmds.PLAYSTATE_CHANGED);
                if (mIsInitialized) {
                    mService.get().onStartPlaying(mPlayingFile);
                }
            } else if (player == mNextMediaPlayer) {
                mNextIsPreparing = false;
            }

        }

        @Override
        public void onRendererInitializationError(StreamerExoPlayer player,
                Exception e) {
//            onError(player, e);
        }

        @Override
        public void onAudioTrackInitializationError(StreamerExoPlayer player,
                InitializationException e) {
//            onError(player, e);
        }

        @Override
        public void onAudioTrackWriteError(StreamerExoPlayer player,
                WriteException e) {
//          onError(player, e);
        }

        @Override
        public void onDecoderInitializationError(StreamerExoPlayer player,
                DecoderInitializationException e) {
//            onError(player, e);
        }

        @Override
        public void onCryptoError(StreamerExoPlayer player, CryptoException e) {
//            onError(player, e);
        }

        @Override
        public void onUpstreamError(StreamerExoPlayer player, int sourceId,
                IOException e) {
//            onError(player, e);
        }

        @Override
        public void onConsumptionError(StreamerExoPlayer player, int sourceId,
                IOException e) {
//            onError(player, e);
        }

        @Override
        public void onDrmSessionManagerError(StreamerExoPlayer player,
                Exception e) {
//            onError(player, e);
        }

        @Override
        public void onVideoFormatEnabled(StreamerExoPlayer player,
                String formatId, int trigger, int mediaTimeMs) {
        }

        @Override
        public void onAudioFormatEnabled(StreamerExoPlayer player,
                String formatId, int trigger, int mediaTimeMs) {
        }

        @Override
        public void onDroppedFrames(StreamerExoPlayer player, int count,
                long elapsed) {
        }

        @Override
        public void onBandwidthSample(StreamerExoPlayer player, int elapsedMs,
                long bytes, long bitrateEstimate) {
        }

        @Override
        public void onLoadStarted(StreamerExoPlayer player, int sourceId,
                String formatId, int trigger, boolean isInitialization,
                int mediaStartTimeMs, int mediaEndTimeMs, long length) {

        }
        
        

        @Override
        public void onStateChanged(StreamerExoPlayer player,
                boolean playWhenReady, int playbackState) {
            if (playbackState == StreamerExoPlayer.STATE_BUFFERING) {
                mService.get().onBufferingUpdate(player.getBufferedPosition(), player.getBufferPercentage());
            }
            else if (player == mCurrentMediaPlayer && state != playbackState) {
                state = playbackState;
                if (playbackState == StreamerExoPlayer.STATE_ENDED) {
                    if (!mService.get().mForcedTrackToEnd) {
                        if (mNextMediaPlayer != null) {
                            releasePlayer(mCurrentMediaPlayer);
                            mCurrentMediaPlayer = mNextMediaPlayer;
                            if (mCurrentMediaPlayer != null) {
                                mCurrentMediaPlayer.setMetadataListener(this);
                                mCurrentMediaPlayer.setPlayWhenReady(true);
                            }
                            mPlayingFile = mNextPlayingFile;
                            mIsPreparing = mNextIsPreparing;
                            mNextMediaPlayer = null;
                            mNextPlayingFile = null;
                            mNextIsPreparing = false;
                            mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                        } else {
                            mService.get().mWakeLock.acquire(30000);
                            mHandler.sendEmptyMessage(TRACK_ENDED);
                            mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                        }
                    }
                } else if (playbackState == StreamerExoPlayer.STATE_READY) {
                    mIsPreparing = false;
                    if (mIsInitialized) {
                        if (player.isPlaying()) {
                            mService.get().onStartPlaying(mPlayingFile);
                            // playback started, simulate state
                            start();
                        }
                        else {
                            mService.get().setState(State.STATE_INITIALIZED);
                        }
                    }
                }
            } else {
                if (playbackState == StreamerExoPlayer.STATE_READY) {
                    if (player == mNextMediaPlayer) {
                        mNextIsPreparing = false;
                    }
                } else if (playbackState == StreamerExoPlayer.STATE_ENDED) {
                    if (player == mNextMediaPlayer) {
                        mNextMediaPlayer = null;
                    }
                    if (player == mCurrentMediaPlayer) {
                        mCurrentMediaPlayer = null;
                    }
                    releasePlayer(player);
                }
            }
        }

        @Override
        public void onError(StreamerExoPlayer player, Exception e) {
            Log.e(TAG, "onError " + e.getLocalizedMessage());
            state = ExoPlayer.STATE_IDLE;
            int code = -1;
            boolean needsStop = false;
            String msg = "Unknown media error.";
            int what = 0;
//            if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
//                msg = "Media server died";
//                needsStop = true;
//            }
            if (e instanceof FileNotFoundException) {
                what  = MediaPlayer.MEDIA_ERROR_IO;
                code = 404;
                msg = "File can't be accessed: " + mPlayingFile.path;
                needsStop = true;
            } else if (e instanceof InvalidResponseCodeException)  {
                what  = MediaPlayer.MEDIA_ERROR_IO;
                code = ((InvalidResponseCodeException) e).responseCode;
                msg = e.getLocalizedMessage();
                needsStop = true;
            }
            mService.get().onError(code, msg);
            if (needsStop) {
                mIsInitialized = false;
                mPlayingFile = null;
//                mService.get().closeCursor();
            }
            switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
//                releasePlayer(mCurrentMediaPlayer);
//                mCurrentMediaPlayer = null;
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(SERVER_DIED), 2000);
                break;
            default:
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(TRACK_ENDED), 200);
                break;
            }
        }

        @Override
        public void onVideoSizeChanged(StreamerExoPlayer player, int width,
                int height, float pixelWidthHeightRatio) {
        }

        @Override
        public void onId3Metadata(StreamerExoPlayer player,
                Map<String, Object> metadata) {
            mService.get().updateMetadata(metadata);
        }

        public int getBufferPercentage() {
            if (mCurrentMediaPlayer != null) {
                return mCurrentMediaPlayer.getBufferPercentage();
            }
            return 0;
        }
        public long getBufferPosition() {
            if (mCurrentMediaPlayer != null) {
                return mCurrentMediaPlayer.getBufferedPosition();
            }
            return 0;
        }

    }

}
