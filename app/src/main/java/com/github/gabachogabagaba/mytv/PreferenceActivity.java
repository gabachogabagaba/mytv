package com.github.gabachogabagaba.mytv;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceActivity  extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ParentPreferenceFragment())
                .commit();
    }

    public static class ParentPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference, rootKey);
            makeSingleLine("streamer_ip_address");
            makeSingleLine("streamer_port");
            makeSingleLine("lirc_ip_address");
            makeSingleLine("lirc_port");
            makeSingleLine("lirc_remote_name");
            makeSingleLine("lirc_power_button_name");
            makeSingleLine("latency_target");

//            changeInputType("streamer_ip_address", InputType.TYPE_CLASS_PHONE);
            changeInputType("streamer_port", InputType.TYPE_CLASS_NUMBER);
//            changeInputType("lirc_ip_address", InputType.TYPE_CLASS_PHONE);
            changeInputType("lirc_port", InputType.TYPE_CLASS_NUMBER);
            changeInputType("latency_target", InputType.TYPE_CLASS_NUMBER);
        }



        private void makeSingleLine(String key) {
            EditTextPreference author_name_pref = findPreference(key);
            if (author_name_pref != null) {
                author_name_pref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(EditText editText) {
                        editText.setSingleLine();
                    }
                });
            }
        }
        private void changeInputType(String key, int input_type) {
            EditTextPreference author_name_pref = findPreference(key);
            if (author_name_pref != null) {
                author_name_pref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(EditText editText) {
                        editText.setInputType(input_type);
                    }
                });
            }
        }
    }

}

