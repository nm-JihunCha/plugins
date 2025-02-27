// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.urllauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;

import androidx.annotation.Nullable;

import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Launches components for URLs.
 */
class UrlLauncher {
    private static final String TAG = "UrlLauncher";
    private final Context applicationContext;

    @Nullable
    private Activity activity;

    /**
     * Uses the given {@code applicationContext} for launching intents.
     *
     * <p>It may be null initially, but should be set before calling {@link #launch}.
     */
    UrlLauncher(Context applicationContext, @Nullable Activity activity) {
        this.applicationContext = applicationContext;
        this.activity = activity;
    }

    void setActivity(@Nullable Activity activity) {
        this.activity = activity;
    }

    /**
     * Returns whether the given {@code url} resolves into an existing component.
     */
    boolean canLaunch(String url) {
        if (url.contains("intent:")) {
            return true;
        }
        Log.i(TAG, "canLaunch/url - " + url);
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);

        launchIntent.setData(Uri.parse(url));
        launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ComponentName componentName =
                launchIntent.resolveActivity(applicationContext.getPackageManager());

        if (componentName == null) {
            Log.e(TAG, "component name - " + url + " is null");
//            launchIntent.setComponent(new ComponentName("com.wooribank.smart.nplib", ))
            return false;
        } else {
            Log.i(TAG, "component name for " + url + " is " + componentName.toShortString());
            return !"{com.android.fallback/com.android.fallback.Fallback}"
                    .equals(componentName.toShortString());
        }
    }

    /**
     * Attempts to launch the given {@code url}.
     *
     * @param headersBundle    forwarded to the intent as {@code Browser.EXTRA_HEADERS}.
     * @param useWebView       when true, the URL is launched inside of {@link WebViewActivity}.
     * @param enableJavaScript Only used if {@param useWebView} is true. Enables JS in the WebView.
     * @param enableDomStorage Only used if {@param useWebView} is true. Enables DOM storage in the
     * @return {@link LaunchStatus#NO_ACTIVITY} if there's no available {@code applicationContext}.
     * {@link LaunchStatus#ACTIVITY_NOT_FOUND} if there's no activity found to handle {@code
     * launchIntent}. {@link LaunchStatus#OK} otherwise.
     */
    // TODO(nm-JihunCha): wooribank app not work
    LaunchStatus launch(
            String url,
            Bundle headersBundle,
            boolean useWebView,
            boolean enableJavaScript,
            boolean enableDomStorage) {
        if (activity == null) {
            return LaunchStatus.NO_ACTIVITY;
        }
        Log.i(TAG, "launch/url - " + url);

        Intent launchIntent;
        if (url.contains("intent:")) {
            try {
                Log.i(TAG, "intent parsing");
                launchIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                Log.e(TAG, "URISyntaxException - " + e);
                e.printStackTrace();
                return LaunchStatus.ACTIVITY_NOT_FOUND;
            }
        } else if (useWebView) {
            Log.i(TAG, "intent useWebView");
            launchIntent =
                    WebViewActivity.createIntent(
                            activity, url, enableJavaScript, enableDomStorage, headersBundle);
        } else {
            Log.i(TAG, "intent else");
            launchIntent =
                    new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(url))
                            .putExtra(Browser.EXTRA_HEADERS, headersBundle);
        }

        try {
            activity.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException - " + e);

            //해당 앱을 찾을 수 없는 경우 마켓에서 해당 패키지명으로 실행토록 강제 유도
            String packageName = null;
            Pattern pattern = Pattern.compile(";package=([^;]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                packageName = matcher.group(1);
            }

            if (packageName != null && packageName.equals("com.TouchEn.mVaccine.webs")) {
                Log.i(TAG, "packageName - " + packageName);
                String packageUrl = "market://details?id=" + packageName;
                Intent packageIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(packageUrl));
                packageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    activity.startActivity(packageIntent);
                } catch (ActivityNotFoundException ex) {
                    Log.e(TAG, "ActivityNotFoundException - " + ex);
                }
            }

            return LaunchStatus.ACTIVITY_NOT_FOUND;
        }

        return LaunchStatus.OK;
    }

    /**
     * Closes any activities started with {@link #launch} {@code useWebView=true}.
     */
    void closeWebView() {
        applicationContext.sendBroadcast(new Intent(WebViewActivity.ACTION_CLOSE));
    }

    /**
     * Result of a {@link #launch} call.
     */
    enum LaunchStatus {
        /**
         * The intent was well formed.
         */
        OK,
        /**
         * No activity was found to launch.
         */
        NO_ACTIVITY,
        /**
         * No Activity found that can handle given intent.
         */
        ACTIVITY_NOT_FOUND,
    }
}
