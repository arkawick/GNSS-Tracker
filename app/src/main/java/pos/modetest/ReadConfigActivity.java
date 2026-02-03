package pos.modetest;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import pos.modetest.databinding.ActivityReadConfigBinding;
import pos.modetest.utils.ConfigUtils;

public class ReadConfigActivity extends AppCompatActivity {
    private static final String TAG = TAG_PREFIX + "Config";

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private ActivityReadConfigBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge on A14 and lower
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);

        // Keep screen on and disable auto-rotate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        binding = ActivityReadConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup action bar
        setSupportActionBar(binding.toolbar);

        binding.sectionContent.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsets.Type.navigationBars());
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        var DEFAULT_CONFIG_GROUP = "Default";
        var configTypes = List.of(
                ConfigUtils.ConfigTypes.GPS_DEBUG,
                ConfigUtils.ConfigTypes.SYSPROP,
                ConfigUtils.ConfigTypes.CARRIER_CONFIG,
                ConfigUtils.ConfigTypes.GPS_VENDOR,
                ConfigUtils.ConfigTypes.RESPROP,
                ConfigUtils.ConfigTypes.BUILD_PROP,
                ConfigUtils.ConfigTypes.DUMP_LOC,
                ConfigUtils.ConfigTypes.DUMP_GMS_LMS,
                ConfigUtils.ConfigTypes.DUMP_GMS_LS,
                ConfigUtils.ConfigTypes.DUMP_GMS_E911,
                ConfigUtils.ConfigTypes.DUMP_SUB_MGR,
                ConfigUtils.ConfigTypes.DUMP_BATTERY
        );
        var selectorItems = Stream.concat(Stream.of(DEFAULT_CONFIG_GROUP),
                        configTypes.stream().map(ConfigUtils.TYPE_TITLES::get))
                .toArray(String[]::new);
        var configAdapter = new ArrayAdapter<>(this, R.layout.large_spinner_item, selectorItems);
        configAdapter.setDropDownViewResource(R.layout.large_spinner_dropdown_item);
        binding.configSpinner.setAdapter(configAdapter);
        binding.configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                doUpdateData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                doUpdateData();
            }
        });
        doUpdateData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_read_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh_config) {
            doUpdateData();
        } else {
            Log.e(TAG, "Unexpected menu selected : " + getResources().getResourceEntryName(itemId));
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void doUpdateData() {
        String value = (String) binding.configSpinner.getSelectedItem();
        Log.d(TAG, String.format("doUpdateData(%s)", value));
        if (value == null) {
            return;
        }
        var updateItem = ConfigUtils.TYPE_TITLES.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue(), value))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
        mExecutor.execute(() -> {
            CharSequence content;
            if (updateItem != null) {
                content = ConfigUtils.readConfigByType(getApplicationContext(), updateItem);
            } else {
                content = ConfigUtils.readConfigsByType(getApplicationContext(), ConfigUtils.DEFAULT_CONFIG_TYPES);
            }
            runOnUiThread(() -> binding.sectionContent.setText(content));
        });
    }

}
