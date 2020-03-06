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

import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.ICONS_FOR_PREVIEW;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ANDROID_THEME;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_ANDROID;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_QS;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_SHAPE;
import static com.android.customization.model.ResourceConstants.PATH_SIZE;
import static com.android.customization.model.ResourceConstants.QS_STYLE_FLAG_USE_MASK;
import static com.android.customization.model.ResourceConstants.QS_STYLE_FLAG_USE_COLORS;
import static com.android.customization.model.ResourceConstants.QS_STYLE_BACKGROUND_NAME;
import static com.android.customization.model.ResourceConstants.QS_STYLE_COLOR_FOREGROUND_ACTIVE_NAME;
import static com.android.customization.model.ResourceConstants.QS_STYLE_COLOR_BACKGROUND_ACTIVE_NAME;
import static com.android.customization.model.ResourceConstants.SYSUI_PACKAGE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.PathParser;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.ThemeComponentOption.QsStyleOptionsProvider;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThemeComponentOptionProvider} that reads {@link QsStyleOptionsProvider}s from
 * icon overlays.
 */
public class QsStyleOptionsProvider extends ThemeComponentOptionProvider<QsStyleOptionsProvider> {

    private static final String TAG = "QsStyleOptionsProvider";
    private final CustomThemeManager mCustomThemeManager;
    private final String mDefaultThemePackage;

    public QsStyleOptionsProvider(Context context, OverlayManagerCompat manager,
            CustomThemeManager customThemeManager) {
        super(context, manager, OVERLAY_CATEGORY_ICON_QS);
        mCustomThemeManager = customThemeManager;
        List<String> themePackages = manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_ANDROID_THEME, UserHandle.myUserId(), ANDROID_PACKAGE);
        mDefaultThemePackage = themePackages.isEmpty() ? null : themePackages.get(0);
    }

    @Override
    protected void loadOptions() {
        List<Drawable> previewIcons = new ArrayList<>();
        String iconPackage =
                mCustomThemeManager.getOverlayPackages().get(OVERLAY_CATEGORY_ICON_ANDROID);
        if (TextUtils.isEmpty(iconPackage)) {
            iconPackage = ANDROID_PACKAGE;
        }
        for (String iconName : ICONS_FOR_PREVIEW) {
            try {
                previewIcons.add(loadIconPreviewDrawable(iconName, iconPackage));
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load icon in %s for qs style preview, will skip it",
                        iconPackage), e);
            }
        }

        String qsStylePackage = mCustomThemeManager.getOverlayPackages().get(OVERLAY_CATEGORY_SHAPE);
        if (TextUtils.isEmpty(qsStylePackage)) {
            qsStylePackage = ANDROID_PACKAGE;
        }
        Drawable qsStyle = loadShape(qsStylePackage);

        addDefault(previewIcons, qsStyle);
        for (String overlayPackage : mOverlayPackages) {
            try {
                Resources overlayRes = getOverlayResources(overlayPackage);
                boolean forceMask = overlayRes.getBoolean(
                        overlayRes.getIdentifier(QS_STYLE_FLAG_USE_MASK, "bool", overlayPackage));
                if (!forceMask) {
                	qsStyle = overlayRes.getDrawable(
                        overlayRes.getIdentifier(QS_STYLE_BACKGROUND_NAME, "drawable", overlayPackage),
                        null);
                }

                PackageManager pm = mContext.getPackageManager();
                String label = pm.getApplicationInfo(overlayPackage, 0).loadLabel(pm).toString();
                QsStyleOption option = new QsStyleOption(overlayPackage, label, qsStyle, resolveActiveBgColor(overlayPackage), resolveActiveForegroundColor(overlayPackage));
                option.setPreviewIcons(previewIcons);
                option.setQsBackground(qsStyle);
                mOptions.add(option);
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load QS Style overlay %s, will skip it",
                        overlayPackage), e);
            }
        }
    }

    private Drawable loadIconPreviewDrawable(String drawableName, String packageName)
            throws NameNotFoundException, NotFoundException {
        final Resources resources = ANDROID_PACKAGE.equals(packageName)
                ? Resources.getSystem()
                : mContext.getPackageManager().getResourcesForApplication(packageName);
        return resources.getDrawable(
                resources.getIdentifier(drawableName, "drawable", packageName), null);
    }



    private void addDefault(List<Drawable> previewIcons, Drawable shape) {
    	Drawable qsBg = shape;
        Resources sysUIRes = mContext.getPackageManager().getResourcesForApplication(SYSUI_PACKAGE);
        try {
            Resources r = getOverlayResources(mDefaultThemePackage);

        	boolean forceMask = r.getBoolean(
            		r.getIdentifier(QS_STYLE_FLAG_USE_MASK, "bool", mDefaultThemePackage));
            if (!forceMask) {
            	qsBg = r.getDrawable(
                    r.getIdentifier(QS_STYLE_BACKGROUND_NAME, "drawable", mDefaultThemePackage),
                    null);
            }
        } catch (NotFoundException | NameNotFoundException e) {
            Log.d(TAG, "Didn't find default color, will use system option", e);
        	boolean forceMask = sysUIRes.getBoolean(
            		sysUIRes.getIdentifier(QS_STYLE_FLAG_USE_MASK, "bool", SYSUI_PACKAGE));
            if (!forceMask) {
            	qsBg = sysUIRes.getDrawable(
                    sysUIRes.getIdentifier(QS_STYLE_BACKGROUND_NAME, "drawable", SYSUI_PACKAGE),
                    null);
            }
		}
        ColorOption option = new QsStyleOption(null, mContext.getString(R.string.default_theme_title), 
        				qsBg, resolveActiveBgColor(overlayPackage), resolveActiveForegroundColor(overlayPackage));
        option.setPreviewIcons(previewIcons);
        option.setQsBackground(qsBg);
        mOptions.add(option);
    }

    private Drawable loadShape(String packageName) {
        String path = null;
        try {
            Resources r = getOverlayResources(packageName);

            path = ResourceConstants.getIconMask(r, packageName);
        } catch (NameNotFoundException e) {
            Log.d(TAG, String.format("Couldn't load shape icon for %s, skipping.", packageName), e);
        }
        ShapeDrawable shapeDrawable = null;
        if (!TextUtils.isEmpty(path)) {
            PathShape shape = new PathShape(PathParser.createPathFromPathData(path),
                    PATH_SIZE, PATH_SIZE);
            shapeDrawable = new ShapeDrawable(shape);
            shapeDrawable.setIntrinsicHeight((int) PATH_SIZE);
            shapeDrawable.setIntrinsicWidth((int) PATH_SIZE);
        }
        return shapeDrawable;
    }

    private int resolveActiveBgColor(String overlayPackage) {
    	try {
    		Resources overlayRes = getOverlayResources(overlayPackage);
	        boolean customColor = overlayRes.getBoolean(
	            overlayRes.getIdentifier(QS_STYLE_FLAG_USE_COLORS, "bool", overlayPackage));
	    	if (customColor) {
	            return overlayRes.getColor(
	                    overlayRes.getIdentifier(QS_STYLE_COLOR_BACKGROUND_ACTIVE_NAME, "color", overlayPackage), null);
	    	} else {
	        	return mCustomThemeManager.resolveAccentColor(mContext.getResources());
	    	}
        } catch (NotFoundException | NameNotFoundException e) {
            Log.d(TAG, "Didn't find any background color, will use system option", e);
        }
        return mCustomThemeManager.resolveAccentColor(mContext.getResources());
    }

    private int resolveActiveForegroundColor(String overlayPackage) {
        Configuration configuration = mContext.getResources().getConfiguration();
        boolean nightMode = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? true : false;
        Resources system = Resources.getSystem();
    	try {
    		Resources overlayRes = getOverlayResources(overlayPackage);
	        boolean customColor = overlayRes.getBoolean(
	            overlayRes.getIdentifier(QS_STYLE_FLAG_USE_COLORS, "bool", overlayPackage));
	    	if (customColor) {
	            return overlayRes.getColor(
	                    overlayRes.getIdentifier(QS_STYLE_COLOR_FOREGROUND_ACTIVE_NAME, "color", overlayPackage), null);
	    	}
	        return mCustomThemeManager.resolveAccentColor(mContext.getResources());
        } catch (NotFoundException | NameNotFoundException e) {
            Log.d(TAG, "Didn't find any foreground color, will use system option", e);
        }
            return system.getColor(
                    system.getIdentifier(nightMode ? STYLE_BACKGROUND_COLOR_DARK_NAME : STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", ANDROID_PACKAGE), null);
    }
}
