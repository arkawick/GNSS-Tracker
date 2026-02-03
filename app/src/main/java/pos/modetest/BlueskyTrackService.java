package pos.modetest;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pos.modetest.observers.BlueskyLogObserver;
import pos.modetest.utils.HelperUtils;

public class BlueskyTrackService extends Service {
    private static final String TAG = TAG_PREFIX + "BlueskyTrack";
    private static final String CHANNEL_ID = "service";
    private static final int NOTIFICATION_ID = TAG.hashCode();
    private static final int MIN_UPDATE_INTERVAL = 1000; // ms
    private static final int MAX_LOG_BUFF_SIZE = 10000; // lines

    private final List<String> mAllLogsBuffer = new ArrayList<>();
    private final List<String> mEnvLogsBuffer = new ArrayList<>();
    private final List<String> mBlueskyLogsBuffer = new ArrayList<>();

    private int mAllLogsCount;
    private int mEnvLogsCount;
    private int mBlueskyLogsCount;

    private boolean mIsRunning = false;
    private int mNotificationLastUpdateCount = 0;

    private Handler mHandler;
    private Runnable mNotifyUpdater;
    private BlueskyLogObserver blueskyLogObserver;

    private Listener mListener;
    private final IBinder mBinder = new LocalBinder();

    private final BlueskyLogObserver.Listener blueskyLogObserverlistener
            = new BlueskyLogObserver.Listener() {
        @Override
        public void onLocationManagerServiceLogEvent(String logLine) {
            addAndCirculateLogs(mAllLogsBuffer, MAX_LOG_BUFF_SIZE, logLine);
            mAllLogsCount++;
        }

        @Override
        public void onEnvBearingLogEvent(String logLine) {
            addAndCirculateLogs(mAllLogsBuffer, MAX_LOG_BUFF_SIZE, logLine);
            addAndCirculateLogs(mEnvLogsBuffer, MAX_LOG_BUFF_SIZE, logLine);
            mAllLogsCount++;
            mEnvLogsCount++;
        }

        @Override
        public void onBlueskyLogEvent(String logLine) {
            addAndCirculateLogs(mAllLogsBuffer, MAX_LOG_BUFF_SIZE, logLine);
            addAndCirculateLogs(mBlueskyLogsBuffer, MAX_LOG_BUFF_SIZE, logLine);
            mAllLogsCount++;
            mBlueskyLogsCount++;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(getMainLooper());
        blueskyLogObserver = new BlueskyLogObserver(blueskyLogObserverlistener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, String.format("onStartCommand(%d) isRunning:%s", startId, mIsRunning));
        if (!mIsRunning) {
            boolean permCheck = HelperUtils.checkPermissions(getApplicationContext(),
                    BlueskyLogObserver.permissions);
            Log.d(TAG, "onStartCommand(" + startId + ") doStart perms:" + permCheck);
            if (permCheck) {
                doStart();
            } else {
                Toast.makeText(this, "Please grant android.permission.READ_LOGS by adb",
                                Toast.LENGTH_SHORT).show();
            }
        } else {
            String action = intent.getAction();
            Log.v(TAG, String.format("onStartCommand(%d) action: %s", startId, action));
            if (action != null) {
                if (action.equals(Intent.ACTION_DELETE)) {
                    doStop();
                } else {
                    Log.e(TAG, "Unexpected action: " + action);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void doStart() {
        Log.d(TAG, "doStart");

        mIsRunning = true;
        mAllLogsCount = 0;
        mEnvLogsCount = 0;
        mBlueskyLogsCount = 0;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Services",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }
        Notification notification = createNotification();
        nm.notify(NOTIFICATION_ID, notification);

        mHandler.postDelayed(mNotifyUpdater = () -> {
            mHandler.postDelayed(mNotifyUpdater, MIN_UPDATE_INTERVAL);
            updateNotification();
        }, MIN_UPDATE_INTERVAL);

        blueskyLogObserver.startObserving();

        startForeground(NOTIFICATION_ID, notification);
    }

    public static List<String> getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return List.of(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            return List.of();
        }
    }

    private void doStop() {
        Log.d(TAG, "doStop");

        mIsRunning = false;

        blueskyLogObserver.stopObserving();
        mHandler.removeCallbacks(mNotifyUpdater);
        mNotifyUpdater = null;

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void addAndCirculateLogs(List<String> buffer, int maxSize, String item) {
        buffer.add(item);
        if (buffer.size() > maxSize) {
            buffer.subList(maxSize / 10, buffer.size());
        }
    }

    private Notification createNotification() {
        Intent stopIntent = new Intent(this, BlueskyTrackService.class);
        stopIntent.setAction(Intent.ACTION_DELETE);
        PendingIntent stopPi = PendingIntent.getForegroundService(this, 0,
                stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_fg)
                .setContentTitle("Bluesky Logs tracker")
                .setSubText(String.valueOf(mAllLogsCount))
                .setContentText("Env Bearing Logs : " + mEnvLogsCount
                        + "\nBluesky Logs : " + mBlueskyLogsCount)
                .setDeleteIntent(stopPi)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm =
                getSystemService(NotificationManager.class);
        if (mNotificationLastUpdateCount < mAllLogsBuffer.size()) {
            if (mListener != null) {
                mListener.onLogsUpdate(mAllLogsBuffer
                        .subList(mNotificationLastUpdateCount, mAllLogsBuffer.size()));
            }
            nm.notify(NOTIFICATION_ID, createNotification());
            mNotificationLastUpdateCount = mAllLogsBuffer.size();
            Log.v(TAG, "Updating Notification");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        List<String> getAllLogs() {
            return mAllLogsBuffer;
        }

        List<String> getEnvLogs() {
            return mEnvLogsBuffer;
        }

        List<String> getBlueskyLogs() {
            return mBlueskyLogsBuffer;
        }

        public void setUpdateListener(Listener listener) {
            mListener = listener;
        }
    }

    public interface Listener {
        void onLogsUpdate(List<String> newLogs);
    }
}
