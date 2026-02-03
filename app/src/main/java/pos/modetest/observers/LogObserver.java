package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public abstract class LogObserver implements IObserver {
    public static final String TAG = TAG_PREFIX + "LogObs";
    public static final String BUF_DEFAULT = "default";

    @Nullable
    private final String mLogcatBuffer;
    @Nullable
    private final List<String> mExtraArgs;
    @Nullable
    private final List<String> mFilterList;

    private final Thread mReadThread;
    private CancellationSignal mCancel;

    public static final String[] permissions = new String[]{
            Manifest.permission.READ_LOGS
    };

    public LogObserver(@Nullable String buffer, @Nullable List<String> extraArgs, @Nullable List<String> filterList) {
        mLogcatBuffer = buffer;
        mExtraArgs = extraArgs;
        mFilterList = filterList;
        mReadThread = new Thread(this::readLogcat);
        mCancel = null;
    }

    private void readLogcat() {
        ProcessBuilder processBuilder = new ProcessBuilder("logcat");
        if (mLogcatBuffer != null && !Objects.equals(mLogcatBuffer, BUF_DEFAULT)) {
            processBuilder.command().addAll(List.of("-b", mLogcatBuffer));
        }
        if (mExtraArgs != null) {
            processBuilder.command().addAll(mExtraArgs);
        }

        Log.d(TAG, String.format("readLogcat(%s)",
                String.join(" ", processBuilder.command())));

        int exitCode = 0;
        try {
            var process = processBuilder.start();
            var iStream = process.getInputStream();
            var bufferedReader = new BufferedReader(new InputStreamReader(iStream));
            String line;
            while (!(mCancel != null && mCancel.isCanceled())
                    && (line = bufferedReader.readLine()) != null) {
                if (mFilterList == null || mFilterList.stream().anyMatch(line::matches)) {
                    onLogEvent(line);
                }
            }
            process.destroy();
            exitCode = process.exitValue();
        } catch (Exception e) {
            Log.w(TAG, "readLogcat Error : " + e.getMessage(), e);
        }
        Log.d(TAG, "readLogcat - exitCode : " + exitCode);
    }

    public abstract void onLogEvent(String logLine);

    @Override
    public void startObserving() {
        Log.d(TAG, "startObserving");
        mCancel = new CancellationSignal();
        mReadThread.start();
    }

    @Override
    public void stopObserving() {
        Log.d(TAG, "stopObserving");
        mCancel.cancel();
    }
}
