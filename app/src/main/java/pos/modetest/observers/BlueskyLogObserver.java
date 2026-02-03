package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlueskyLogObserver extends LogObserver implements IObserver {
    public static final String TAG = TAG_PREFIX + "LogObsBS";
    private static final String RGX_LMS = ".*LocationManagerService:.*";
    private static final String RGX_ENV_BEARING = ".*hasEnvironmentBearing.*";
    private static final String RGX_BLUESKY = ".*(Bluesky(Manager|Registrant)|GCoreFlp):.*";

    public static final String[] permissions = LogObserver.permissions;

    private final Listener mListener;
    private final Executor mExecutor;

    public BlueskyLogObserver(Listener listener) {
        super(BUF_DEFAULT,
                List.of("-s",
                        "LocationManagerService:*",
                        "LocSvc_ApiV02:*",
                        "BlueskyManager:*",
                        "BlueskyRegistrant:*",
                        "GCoreFlp:*"
                ),
                List.of(
                        RGX_LMS,
                        RGX_ENV_BEARING,
                        RGX_BLUESKY
                )
        );
        mListener = listener;
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onLogEvent(String logLine) {
        if (logLine.matches(RGX_LMS)) {
            mExecutor.execute(() -> mListener.onLocationManagerServiceLogEvent(logLine));
        } else if (logLine.matches(RGX_ENV_BEARING)) {
            mExecutor.execute(() -> mListener.onEnvBearingLogEvent(logLine));
        } else if (logLine.matches(RGX_BLUESKY)) {
            mExecutor.execute(() -> mListener.onBlueskyLogEvent(logLine));
        }
    }

    public interface Listener {
        void onLocationManagerServiceLogEvent(String logLine);
        void onEnvBearingLogEvent(String logLine);
        void onBlueskyLogEvent(String logLine);
    }
}
