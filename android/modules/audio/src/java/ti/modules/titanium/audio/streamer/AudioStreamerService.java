package ti.modules.titanium.audio.streamer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.PowerManager;
import android.webkit.URLUtil;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;

import ti.modules.titanium.filesystem.FileProxy;
import ti.modules.titanium.audio.SoundProxy;


/**
 * A backbround {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
public class AudioStreamerService extends AudioService {
    private static final String TAG = "AudioStreamerService";
    protected static String getCmdPrefix() {
        return "ti.modules.titanium.audio.streamer";
    }
    
    
    protected AudioPlayer createPlayer(AudioService audioService) {
        return new MultiPlayer((AudioStreamerService) audioService);
    }
    
    public class PlayingItem implements BasePlayingItem{
        public String path = null;
        public AssetFileDescriptor assetFileDescriptor = null;
        public FileDescriptor fileDescriptor = null;
        public FileInputStream inputStream = null;
        protected PlayingItem(String path) {
            this.path = path;
        }
        PlayingItem(String path, AssetFileDescriptor assetFileDescriptor) {
            this.path = path;
            this.assetFileDescriptor = assetFileDescriptor;
        }
        PlayingItem(String path, FileInputStream inputStream) {
            this.path = path;
            this.inputStream = inputStream;
        }
        PlayingItem(String path, FileDescriptor fileDescriptor) {
            this.path = path;
            this.fileDescriptor = fileDescriptor;
        }
    }

    protected class MultiPlayer extends AudioPlayer implements
            MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnPreparedListener, OnInfoListener,
            OnBufferingUpdateListener {
        
        


        protected CompatMediaPlayer mCurrentMediaPlayer = new CompatMediaPlayer();

        protected CompatMediaPlayer mNextMediaPlayer;



        protected boolean mIsPaused = false;
        protected boolean mNextIsPreparing = false;

        protected PlayingItem mPlayingItem = null;
        protected PlayingItem mNextPlayingItem = null;

        /**
         * Constructor of <code>MultiPlayer</code>
         */
        public MultiPlayer(final AudioStreamerService service) {
            super(service);
            mCurrentMediaPlayer.setWakeMode(mService.get(),
                    PowerManager.PARTIAL_WAKE_LOCK);
        }

        /**
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @throws Exception 
         */
        public void setDataSource(final String path) throws Exception {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path,
                    mIsPreparing);
            if (mIsInitialized) {
                final String next = null;
                setNextDataSource(next);
            }
        }

        /**
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @throws Exception 
         */
        public void setDataSource(FileDescriptor fd, long offset, long length) throws Exception {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, fd, offset,
                    length);
            if (mIsInitialized) {
                final String next = null;
                setNextDataSource(next);
            }
        }

        /**
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @throws Exception 
         */
        public void setDataSource(FileDescriptor fd) throws Exception {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, fd);
            if (mIsInitialized) {
                final String next = null;
                setNextDataSource(next);
            }
        }

        /**
         * @param player
         *            The {@link MediaPlayer} to use
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @return True if the <code>player</code> has been prepared and is
         *         ready to play, false otherwise
         * @throws Exception 
         */
        private boolean setDataSourceImpl(final MediaPlayer player,
                final String path, boolean remote) throws Exception {
            try {
                player.reset();
                player.setOnCompletionListener(this);
                player.setOnErrorListener(this);
                player.setOnInfoListener(this);
                player.setOnBufferingUpdateListener(this);
                player.setOnPreparedListener(null);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                if (path.startsWith("content://")) {
                    player.setDataSource(mService.get(), Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                if (remote) {
                    player.setOnPreparedListener(this);
                    player.prepareAsync();
                    Log.w(TAG, "prepareAsync from playUrl");
                    if (player == mCurrentMediaPlayer) {
                        setState(State.STATE_STARTING);
                        mService.get().mPlayPending = true;
                    }
                } else {
                    player.prepare();
                }
            } catch (final Exception e) {
                // TODO: notify the user why the file couldn't be opened
                throw e;
//                return false;
            }
            if (player == mCurrentMediaPlayer) {
                onSourceSet();
            }
            return true;
        }
        
        protected void onSourceSet() {
            final Intent intent = new Intent(
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mService.get()
                    .getPackageName());
            mService.get().sendBroadcast(intent);
        }

        /**
         * @param player
         *            The {@link MediaPlayer} to use
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @return True if the <code>player</code> has been prepared and is
         *         ready to play, false otherwise
         */
        private boolean setDataSourceImpl(final MediaPlayer player,
                FileDescriptor fd, long offset, long length) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                player.setDataSource(fd, offset, length);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (final IOException todo) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            } catch (final IllegalArgumentException todo) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            }
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnInfoListener(this);
            player.setOnBufferingUpdateListener(this);
            onSourceSet();
            return true;
        }

        /**
         * @param player
         *            The {@link MediaPlayer} to use
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @return True if the <code>player</code> has been prepared and is
         *         ready to play, false otherwise
         */
        private boolean setDataSourceImpl(final MediaPlayer player,
                FileDescriptor fd) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                player.setDataSource(fd);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (final IOException todo) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            } catch (final IllegalArgumentException todo) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            }
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnInfoListener(this);
            player.setOnBufferingUpdateListener(this);
            onSourceSet();
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
                        url = TiConvert.toString(((HashMap) object).get(TiC.PROPERTY_URL));
                    } else  {
                        url =  TiConvert.toString(object);
                    }
                    boolean isAsset = URLUtil.isAssetUrl(url);
                    if (preparingNext) {
                        mNextIsPreparing = false;
                    } else {
                        mIsPreparing = false;
                    }
                    if (isAsset || url.startsWith("android.resource")) {
                        Context context = TiApplication.getInstance();
                        AssetFileDescriptor afd = null;
                        try {
                            if (isAsset) {
                                String path = url.substring(TiConvert.ASSET_URL
                                        .length());
                                afd = context.getAssets().openFd(path);
                            } else {
                                Uri uri = Uri.parse(url);
                                afd = context.getResources().openRawResourceFd(
                                        TiRHelper.getResource("raw."
                                                + uri.getLastPathSegment()));
                            }
                            // Why mp.setDataSource(afd) doesn't work is a
                            // problem for
                            // another day.
                            // http://groups.google.com/group/android-developers/browse_thread/thread/225c4c150be92416

                            if (preparingNext) {
                                mNextPlayingItem = new PlayingItem(url, afd);
                                setNextDataSource(afd.getFileDescriptor(),
                                        afd.getStartOffset(), afd.getLength());
                            } else {
                                mPlayingItem = new PlayingItem(url, afd);
                                setDataSource(afd.getFileDescriptor(),
                                        afd.getStartOffset(), afd.getLength());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error setting file descriptor: ", e);
                        } finally {
                            if (afd != null) {
                                afd.close();
                            }
                        }
                    } else {
                        Uri uri = Uri.parse(url);
                        if (uri.getScheme().equals(TiC.PROPERTY_FILE)) {
                            if (TiC.ICS_OR_GREATER) {
                                mPlayingItem = new PlayingItem(uri.getPath());
                                setDataSource(mPlayingItem.path);
                            } else {
                                // For 2.2 and below, MediaPlayer uses the
                                // native player
                                // which requires
                                // files to have worldreadable access,
                                // workaround is to
                                // open an input
                                // stream to the file and give that to the
                                // player.
                                FileInputStream fis = null;
                                try {
                                    fis = new FileInputStream(uri.getPath());
                                    mPlayingItem = new PlayingItem(uri.getPath(), fis.getFD());
                                    setDataSource(fis.getFD());
                                } catch (IOException e) {
                                    Log.e(TAG,
                                            "Error setting file descriptor: ",
                                            e);
                                } finally {
                                    if (fis != null) {
                                        fis.close();
                                    }
                                }
                            }
                        } else {
                            if (preparingNext) {
                                mNextIsPreparing = true;
                                mNextPlayingItem = new PlayingItem(url);
                                setNextDataSource(mNextPlayingItem.path);
                            } else {
                                mIsPreparing = true;
                                mPlayingItem = new PlayingItem(url);
                                setDataSource(mPlayingItem.path);
                            }

                        }
                    }

                } catch (Throwable t) {
                    Log.w(TAG, "Issue while initializing : ", t);
                    return false;
                }
                if (!preparingNext) {
                    if (mNextIsPreparing) {
                        setState(State.STATE_STARTING);
                    } else if (mIsInitialized) {
                        setState(State.STATE_INITIALIZED);
                    }
                    if (mIsInitialized && !mIsPreparing) {
                        mService.get().onStartPlaying(mPlayingItem);
                    }
                }
                
                return mIsInitialized;
            }
        }

        /**
         * Set the MediaPlayer to start when this MediaPlayer finishes playback.
         * 
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         * @throws Exception 
         */
        public void setNextDataSource(final String path) throws Exception {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (path == null) {
                return;
            }
            mNextMediaPlayer = new CompatMediaPlayer();
            mNextMediaPlayer.setWakeMode(mService.get(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (!setDataSourceImpl(mNextMediaPlayer, path, mNextIsPreparing)) {
//                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
//            } else {
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        }

        /**
         * Set the MediaPlayer to start when this MediaPlayer finishes playback.
         * 
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         */
        public void setNextDataSource(FileDescriptor fd, long offset,
                long length) {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (fd == null) {
                return;
            }
            mNextMediaPlayer = new CompatMediaPlayer();
            mNextMediaPlayer.setWakeMode(mService.get(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (setDataSourceImpl(mNextMediaPlayer, fd, offset, length)) {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } else {
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        }

        /**
         * Set the MediaPlayer to start when this MediaPlayer finishes playback.
         * 
         * @param path
         *            The path of the file, or the http/rtsp URL of the stream
         *            you want to play
         */
        public void setNextDataSource(FileDescriptor fd) {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (fd == null) {
                return;
            }
            mNextMediaPlayer = new CompatMediaPlayer();
            mNextMediaPlayer.setWakeMode(mService.get(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (setDataSourceImpl(mNextMediaPlayer, fd)) {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } else {
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        }

        /**
         * @return True if the player is ready to go, false otherwise
         */
        public boolean isPlaying() {
            return mCurrentMediaPlayer.isPlaying();
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
            mCurrentMediaPlayer.start();
            mIsPaused = !mCurrentMediaPlayer.isPlaying();
            setState(!mIsPaused ? State.STATE_PLAYING : mService
                            .get().mState);
            startProgressTimer();
        }

        /**
         * Resets the MediaPlayer to its uninitialized state.
         */
        public void stop() {
            mCurrentMediaPlayer.reset();
            mPlayingItem = null;
            
            mIsInitialized = false;
            mIsPaused = false;
            setState(State.STATE_STOPPED);
            stopProgressTimer();
        }

        /**
         * Releases resources associated with this MediaPlayer object.
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        /**
         * Pauses playback. Call start() to resume.
         */
        public void pause() {
            mCurrentMediaPlayer.pause();
            mIsPaused = true;
            setState(State.STATE_PAUSED);
            stopProgressTimer();
        }

        /**
         * Gets the duration of the file.
         * 
         * @return The duration in milliseconds
         */
        public long duration() {
            if (isPrepared()) {
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
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        /**
         * Sets the current playback position.
         * 
         * @param whereto
         *            The offset in milliseconds from the start to seek to
         * @return The offset in milliseconds from the start to seek to
         */
        public long seek(final long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        /**
         * Sets the volume on this player.
         * 
         * @param vol
         *            Left and right volume scalar
         */
        public void setVolume(final float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
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
            return mPlayingItem;
        }

        /**
         * Sets the audio stream type.
         * 
         * @param streamtype
         *            the audio stream type
         */
        public void setAudioStreamType(final int streamtype) {
            mCurrentMediaPlayer.setAudioStreamType(streamtype);
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            String msg = "Unknown media issue.";

            switch (what) {
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                msg = "Stream not interleaved or interleaved improperly.";
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                msg = "Stream does not support seeking";
                break;
            case MediaPlayer.MEDIA_INFO_UNKNOWN:
                msg = "Unknown media issue";
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                msg = "Video is too complex for decoder, video lagging.";
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                mService.get().updateMetadata();;
                break;
            }

            mService.get().onError(TiC.ERROR_CODE_UNKNOWN, msg);
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onError(final MediaPlayer mp, final int what,
                final int extra) {
            if (mp == mNextMediaPlayer) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
                return false;
            }
            
            int code = what;
            if (what == 0) {
                code = -1;
            }
            boolean needsStop = false;
            String msg = "Unknown media error.";
            if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                msg = "Media server died";
                needsStop = true;
            }
            if (extra == -1004) {
                code = 404;
                msg = "File can't be accessed";
                needsStop = true;
            }
            mService.get().onError(code, msg);
            if (needsStop) {
                mIsInitialized = false;
                mPlayingItem = null;
                mService.get().mWakeLock.acquire(30000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
            }
            switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = new CompatMediaPlayer();
                mCurrentMediaPlayer.setWakeMode(mService.get(),
                        PowerManager.PARTIAL_WAKE_LOCK);
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(SERVER_DIED), 2000);
                break;
            default:
                break;
            }
            return needsStop;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCompletion(final MediaPlayer mp) {
            if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = mNextMediaPlayer;
                mPlayingItem = mNextPlayingItem;
                mIsPreparing = mNextIsPreparing;
                mNextMediaPlayer = null;
                mNextPlayingItem = null;
                mNextIsPreparing = false;
                mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
            } else {
                mService.get().mWakeLock.acquire(30000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
            }
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.setOnPreparedListener(null);
            if (mp == mCurrentMediaPlayer) {
                mIsPreparing = false;
                setState(State.STATE_INITIALIZED);
                if (mIsInitialized) {
                    mService.get().onStartPlaying(mPlayingItem);
                }
            } else if (mp == mNextMediaPlayer) {
                mNextIsPreparing = false;
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            }

        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (mp == mCurrentMediaPlayer) {
                mService.get().onBufferingUpdate(-1, percent);
            }
        }
    }

//    @SuppressWarnings("unused")
//    private static final class AudioStreamerServiceStub {
//
//        private final WeakReference<AudioStreamerService> mService;
//
//        private AudioStreamerServiceStub(final AudioStreamerService service) {
//            mService = new WeakReference<AudioStreamerService>(service);
//        }
//
//        public void openFile(final Object object) throws RemoteException {
//            mService.get().openFile(object);
//        }
//
//        public void open(final List<Object> list, final int position)
//                throws RemoteException {
//            mService.get().open(list, position);
//        }
//
//        public void stop() throws RemoteException {
//            mService.get().stop();
//        }
//
//        public void pause() throws RemoteException {
//            mService.get().pause();
//        }
//
//        public void play() throws RemoteException {
//            mService.get().play();
//        }
//
//        public void prev() throws RemoteException {
//            mService.get().prev();
//        }
//
//        public void next() throws RemoteException {
//            mService.get().gotoNext(true);
//        }
//
//        public void enqueue(final List<Object> list, final int action)
//                throws RemoteException {
//            mService.get().enqueue(list, action);
//        }
//
//        public void setQueuePosition(final int index) throws RemoteException {
//            mService.get().setQueuePosition(index);
//        }
//
//        public void setShuffleMode(final int shufflemode)
//                throws RemoteException {
//            mService.get().setShuffleMode(shufflemode);
//        }
//
//        public void setRepeatMode(final int repeatmode) throws RemoteException {
//            mService.get().setRepeatMode(repeatmode);
//        }
//
//        public void moveQueueItem(final int from, final int to)
//                throws RemoteException {
//            mService.get().moveQueueItem(from, to);
//        }
//
//        public void refresh() throws RemoteException {
//            mService.get().refresh();
//        }
//
//        public boolean isPlaying() throws RemoteException {
//            return mService.get().isPlaying();
//        }
//
//        public Object[] getQueue() throws RemoteException {
//            return mService.get().getQueue();
//        }
//
//        public long duration() throws RemoteException {
//            return mService.get().duration();
//        }
//
//        public long position() throws RemoteException {
//            return mService.get().position();
//        }
//
//        public long seek(final long position) throws RemoteException {
//            return mService.get().seek(position);
//        }
//
//        public Object getCurrent() throws RemoteException {
//            return mService.get().getCurrent();
//        }
//
//        public int getQueuePosition() throws RemoteException {
//            return mService.get().getQueuePosition();
//        }
//
//        public int getShuffleMode() throws RemoteException {
//            return mService.get().getShuffleMode();
//        }
//
//        public int getRepeatMode() throws RemoteException {
//            return mService.get().getRepeatMode();
//        }
//
//        public int removeTracks(final int first, final int last)
//                throws RemoteException {
//            return mService.get().removeTracks(first, last);
//        }
//
//        public int removeTrack(final long id) throws RemoteException {
//            return mService.get().removeTrack(id);
//        }
//
//        public int getMediaMountedCount() throws RemoteException {
//            return mService.get().getMediaMountedCount();
//        }
//
//        public int getAudioSessionId() throws RemoteException {
//            return mService.get().getAudioSessionId();
//        }
//
//    }

}
