/*
 * Copyright (C) 2018 The Xiaomi-SDM660 Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.lineageos.settings.device;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;

public class DeviceSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private final String ENABLE_HAL3_KEY = "hal3";
    private final String ENABLE_EIS_KEY = "eis";
    final static String ENABLE_FPACTION_KEY = "fpaction_enabled";
    final static String FP_SHUTTER_KEY = "fp_shutter";
    final static String FPACTION_KEY = "fpaction";
    final static String TORCH_BRIGHTNESS_KEY = "torch_brightness";
    final static String VIBRATION_STRENGTH_KEY = "vibration_strength";
    private final String SPECTRUM_KEY = "spectrum";

    private final String HAL3_SYSTEM_PROPERTY = "persist.camera.HAL3.enabled";
    private final String EIS_SYSTEM_PROPERTY = "persist.camera.eis.enable";
    private final String SPECTRUM_SYSTEM_PROPERTY = "persist.spectrum.profile";

    final static String TORCH_1_BRIGHTNESS_PATH = "/sys/devices/soc/800f000.qcom,spmi/spmi-0/spmi0-03/800f000.qcom,spmi:qcom,pm660l@3:qcom,leds@d300/leds/led:torch_0/max_brightness";
    final static String TORCH_2_BRIGHTNESS_PATH = "/sys/devices/soc/800f000.qcom,spmi/spmi-0/spmi0-03/800f000.qcom,spmi:qcom,pm660l@3:qcom,leds@d300/leds/led:torch_1/max_brightness";
    final static String VIBRATION_STRENGTH_PATH = "/sys/devices/virtual/timed_output/vibrator/vtg_level";

    private final String KEY_CATEGORY_DISPLAY = "display";
    private final String KEY_DEVICE_DOZE = "device_doze";
    private final String KEY_DEVICE_DOZE_PACKAGE_NAME = "org.lineageos.settings.doze";
    private final String KEY_DEVICE_KCAL = "device_kcal";
    private final String KEY_DEVICE_KCAL_PACKAGE_NAME = "org.lineageos.settings.kcal";

    private SwitchPreference mEnableHAL3;
    private SwitchPreference mEnableEIS;
    static SwitchPreference sEnableFpAction;
    static SwitchPreference sFpShutter;
    static ListPreference sFpAction;
    private TorchSeekBarPreference mTorchBrightness;
    private VibrationSeekBarPreference mVibrationStrength;
    private ListPreference mSPECTRUM;

    // value of vtg_min and vtg_max
    final static int MIN_VIBRATION = 116;
    final static int MAX_VIBRATION = 3596;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main, rootKey);

        mEnableHAL3 = (SwitchPreference) findPreference(PREF_ENABLE_HAL3);
        mEnableHAL3.setChecked(FileUtils.getProp(HAL3_SYSTEM_PROPERTY, false));
        mEnableHAL3.setOnPreferenceChangeListener(this);

        mEnableEIS = (SwitchPreference) findPreference(PREF_ENABLE_EIS);
        mEnableEIS.setChecked(FileUtils.getProp(EIS_SYSTEM_PROPERTY, false));
        mEnableEIS.setOnPreferenceChangeListener(this);

        sEnableFpAction = (SwitchPreference) findPreference(ENABLE_FPACTION_KEY);
        sEnableFpAction.setOnPreferenceChangeListener(this);

        sFpShutter = (SwitchPreference) findPreference(FP_SHUTTER_KEY);
        sFpShutter.setOnPreferenceChangeListener(this);

        sFpAction = (ListPreference) findPreference(FPACTION_KEY);
        sFpAction.setSummary(sFpAction.getEntry());
        sFpAction.setOnPreferenceChangeListener(this);

        mTorchBrightness = (TorchSeekBarPreference) findPreference(TORCH_BRIGHTNESS_KEY);
        mTorchBrightness.setEnabled(FileUtils.fileWritable(TORCH_1_BRIGHTNESS_PATH) && FileUtils.fileWritable(TORCH_2_BRIGHTNESS_PATH));
        mTorchBrightness.setOnPreferenceChangeListener(this);

        mVibrationStrength = (VibrationSeekBarPreference) findPreference(PREF_VIBRATION_STRENGTH);
        mVibrationStrength.setEnabled(FileUtils.fileWritable(VIBRATION_STRENGTH_PATH));
        mVibrationStrength.setOnPreferenceChangeListener(this);

        PreferenceCategory displayCategory = (PreferenceCategory) findPreference(CATEGORY_DISPLAY);
        if (isAppNotInstalled(DEVICE_DOZE_PACKAGE_NAME)) {
            displayCategory.removePreference(findPreference(PREF_DEVICE_DOZE));
        }

        if (isAppNotInstalled(DEVICE_KCAL_PACKAGE_NAME)) {
            displayCategory.removePreference(findPreference(PREF_DEVICE_KCAL));
        }

        mSPECTRUM = (ListPreference) findPreference(PREF_SPECTRUM);
        mSPECTRUM.setValue(FileUtils.getStringProp(SPECTRUM_SYSTEM_PROPERTY, "0"));
        mSPECTRUM.setSummary(mSPECTRUM.getEntry());
        mSPECTRUM.setOnPreferenceChangeListener(this);

        boolean enhancerEnabled;
        try {
            enhancerEnabled = DiracService.sDiracUtils.isDiracEnabled();
        } catch (java.lang.NullPointerException e) {
            getContext().startService(new Intent(getContext(), DiracService.class));
            enhancerEnabled = DiracService.sDiracUtils.isDiracEnabled();
        }

        mEnableDirac = (SwitchPreference) findPreference(PREF_ENABLE_DIRAC);
        mEnableDirac.setOnPreferenceChangeListener(this);
        mEnableDirac.setChecked(enhancerEnabled);

        mHeadsetType = (ListPreference) findPreference(PREF_HEADSET);
        mHeadsetType.setOnPreferenceChangeListener(this);
        mHeadsetType.setEnabled(enhancerEnabled);

        mPreset = (ListPreference) findPreference(PREF_PRESET);
        mPreset.setOnPreferenceChangeListener(this);
        mPreset.setEnabled(enhancerEnabled);
    }


    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        switch (key) {
            case ENABLE_HAL3_KEY:
                FileUtils.setProp(HAL3_SYSTEM_PROPERTY, (Boolean) value);
                break;

            case ENABLE_EIS_KEY:
                FileUtils.setProp(EIS_SYSTEM_PROPERTY, (Boolean) value);
                break;

            case ENABLE_FPACTION_KEY:
                sFpAction.setEnabled((Boolean) value);
                break;

            case FPACTION_KEY:
                sFpAction.setValue((String) value);
                sFpAction.setSummary(sFpAction.getEntry());
                break;

            case TORCH_BRIGHTNESS_KEY:
                FileUtils.setValue(TORCH_1_BRIGHTNESS_PATH, (int) value);
                FileUtils.setValue(TORCH_2_BRIGHTNESS_PATH, (int) value);
                break;

            case VIBRATION_STRENGTH_KEY:
                double vibrationValue = (int) value / 100.0 * (MAX_VIBRATION - MIN_VIBRATION) + MIN_VIBRATION;
                FileUtils.setValue(VIBRATION_STRENGTH_PATH, vibrationValue);
                break;

            case SPECTRUM_KEY:
                mSPECTRUM.setValue((String) value);
                mSPECTRUM.setSummary(mSPECTRUM.getEntry());
                FileUtils.setStringProp(SPECTRUM_SYSTEM_PROPERTY, (String) value);
                break;

            case PREF_ENABLE_DIRAC:
                try {
                    DiracService.sDiracUtils.setEnabled((boolean) value);
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setEnabled((boolean) value);
                }
                mHeadsetType.setEnabled((boolean) value);
                mPreset.setEnabled((boolean) value);
                break;

            case PREF_HEADSET:
                try {
                    DiracService.sDiracUtils.setHeadsetType(Integer.parseInt(value.toString()));
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setHeadsetType(Integer.parseInt(value.toString()));
                }
                break;

            case PREF_PRESET:
                try {
                    DiracService.sDiracUtils.setLevel(String.valueOf(value));
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setLevel(String.valueOf(value));
                }
                break;

            default:
                break;
        }
        return true;
    }

    private boolean isAppNotInstalled(String uri) {
        PackageManager packageManager = getContext().getPackageManager();
        try {
            packageManager.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }
}
