/*
 * Copyright (C) 2014 Sean J. Barbeau (sjbarbeau@gmail.com), University of South Florida
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
package org.onebusaway.android.util;

import org.onebusaway.android.BuildConfig;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.io.elements.ObaRegion;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Base64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onebusaway.android.util.GetRestrictionsReceiver.EMBEDDED_SOCIAL_KEY;

public class EmbeddedSocialUtils {
    public static final String ROUTE_DISCUSSION = "route_%d_%s";
    public static final String STOP_DISCUSSION = "stop_%d_%s";
    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[\\\\#%+/?\u0000-\u001F\u007F-\u009F]");

    public static String createRouteDiscussionTitle(long regionId, String routeId) {
        String title = String.format(ROUTE_DISCUSSION, regionId, routeId);
        return sanitizeDiscussionTitle(title);
    }

    public static String createStopDiscussionTitle(long regionId, String stopId) {
        String title = String.format(STOP_DISCUSSION, regionId, stopId);
        return sanitizeDiscussionTitle(title);
    }

    private static String sanitizeDiscussionTitle(String discussionTitle) {
        StringBuffer safeDiscussionTitle = new StringBuffer();
        Matcher matcher = DISALLOWED_CHARS.matcher(discussionTitle);
        while (matcher.find()) {
            String found = matcher.group();
            byte[] encoding = Base64.encode(found.getBytes(), Base64.NO_WRAP);
            String converted = new String(encoding);
            converted = converted.replace('/', '_');
            matcher.appendReplacement(safeDiscussionTitle, converted);
        }
        matcher.appendTail(safeDiscussionTitle);

        return safeDiscussionTitle.toString();
    }

    /**
     * Returns true if social features are restricted
     */
    private static boolean isSocialRestricted(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // min sdk version for user restrictions
            Bundle restrictionsBundle = ((UserManager)context.getSystemService(Context.USER_SERVICE))
                    .getApplicationRestrictions(context.getPackageName());
            if (restrictionsBundle == null) {
                restrictionsBundle = new Bundle();
            }

            if (restrictionsBundle.containsKey(EMBEDDED_SOCIAL_KEY)) {
                return !restrictionsBundle.getBoolean(EMBEDDED_SOCIAL_KEY);
            }
        }

        return false;
    }

    /**
     * Returns true if the build version meets min sdk level for Embedded Social
     */
    public static boolean isBuildVersionSupportedBySocial() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }

    /**
     * Returns true if the Embedded Social api key is non-empty
     */
    public static boolean isSocialApiKeyDefined() {
        return !BuildConfig.EMBEDDED_SOCIAL_API_KEY.isEmpty();
    }

    /**
     * Returns true if social features are enabled
     */
    public static boolean isSocialEnabled(Context context) {
        if (!isSocialApiKeyDefined()) {
            return false;
        }

        if (isBuildVersionSupportedBySocial()) {
            ObaRegion currentRegion = Application.get().getCurrentRegion();
            if (currentRegion != null && currentRegion.getSupportsEmbeddedSocial()) {
                return !isSocialRestricted(context);
            }
        }
        return false;
    }
}