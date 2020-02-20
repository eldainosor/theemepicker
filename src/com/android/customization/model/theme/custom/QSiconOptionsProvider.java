/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License.
 */
package com.android.customization.model.theme.custom;

import static com.android.customization.model.ResourceConstants.SYSUI_PACKAGE;
import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.QSICONS_FOR_PREVIEW;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_QS;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.ThemeComponentOption.QSiconOption;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThemeComponentOptionProvider} that reads {@link QSiconOption}s from
 * icon overlays.
 */
public class QSiconOptionsProvider extends ThemeComponentOptionProvider<QSiconOption> {

    private static final String TAG = "QSiconOptionsProvider";

    public QSiconOptionsProvider(Context context, OverlayManagerCompat manager) {
        super(context, manager, OVERLAY_CATEGORY_ICON_QS);
    }

    @Override
    protected void loadOptions() {
        addDefault();

        Map<String, QSiconOption> optionsByPrefix = new HashMap<>();
        for (String overlayPackage : mOverlayPackages) {
            QSiconOption option = addOrUpdateOption(optionsByPrefix, overlayPackage,
                    OVERLAY_CATEGORY_ICON_QS);
            for (String iconName : QSICONS_FOR_PREVIEW) {
                try{
                    option.addIcon(loadIconPreviewDrawable(iconName, overlayPackage));
                } catch (NotFoundException | NameNotFoundException e) {
                    Log.w(TAG, String.format("Couldn't load icon %s overlay details for %s, will skip it",
                        iconName, overlayPackage), e);
                }
            }
        }

        for (QSiconOption option : optionsByPrefix.values()) {
            if (option.isValid(mContext)) {
                mOptions.add(option);
                option.setLabel(mContext.getString(R.string.qsicon_component_label, mOptions.size()));
            }
        }
    }

    private QSiconOption addOrUpdateOption(Map<String, QSiconOption> optionsByPrefix,
            String overlayPackage, String category) {
        String prefix = overlayPackage.substring(0, overlayPackage.lastIndexOf("."));
        QSiconOption option;
        if (!optionsByPrefix.containsKey(prefix)) {
            option = new QSiconOption();
            optionsByPrefix.put(prefix, option);
        } else {
            option = optionsByPrefix.get(prefix);
        }
        option.addOverlayPackage(category, overlayPackage);
        return option;
    }

    private Drawable loadIconPreviewDrawable(String drawableName, String packageName)
            throws NameNotFoundException, NotFoundException {
        final Resources resources = ANDROID_PACKAGE.equals(packageName)
                ? Resources.getSystem()
                : mContext.getPackageManager().getResourcesForApplication(packageName);
        return resources.getDrawable(
                resources.getIdentifier(drawableName, "drawable", packageName), null);
    }

    private void addDefault() {
        QSiconOption option = new QSiconOption();
        option.setLabel(mContext.getString(R.string.default_theme_title));
        for (String iconName : QSICONS_FOR_PREVIEW) {
            try {
                option.addIcon(loadIconPreviewDrawable(iconName, SYSUI_PACKAGE));
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Didn't find SystemUi package icon %s, will skip option", iconName), e);
            }
        }
        option.addOverlayPackage(OVERLAY_CATEGORY_ICON_QS, null);
        mOptions.add(option);
    }

}
