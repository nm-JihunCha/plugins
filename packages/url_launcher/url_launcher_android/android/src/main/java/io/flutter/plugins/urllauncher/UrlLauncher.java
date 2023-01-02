// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.urllauncher;

import static android.content.Intent.CATEGORY_BROWSABLE;

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
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);

        Log.i(TAG, "canLaunchh - " + url);
        if (url.contains("intent:")) {
            Log.i(TAG, "here!");
//            launchIntent.addCategory(Intent.ACTION_VIEW);
//            launchIntent.toUri(Intent.URI_INTENT_SCHEME);

            try {
                Intent j = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                try {
                    Log.i(TAG, "ttt123");
                    activity.startActivity(j);
                    return false;
                } catch (ActivityNotFoundException e) {
                    Log.i(TAG, "ActivityNotFoundException e - " + e);
                    return false;
                }
            } catch (URISyntaxException e) {
                Log.i(TAG, "e - " + e);
                e.printStackTrace();
            }
//            launchIntent.addFlags(Intent.URI_INTENT_SCHEME);
//            (Intent.ACTION_VIEW)
//            Intent i = Intent.parseUri(Uri.parse(url), Intent.URI_INTENT_SCHEME);
//            startActivity(i)
        }
//        else {
//            launchIntent.addCategory(Intent.ACTION_VIEW);
//        }

//        launchIntent.addCategory(Intent.ACTION_VIEW);
//    Intent launchIntent = applicationContext.getPackageManager().getLaunchIntentForPackage("com.healerb.dna");

        launchIntent.setData(Uri.parse(url));
//    launchIntent.addCategory(CATEGORY_BROWSABLE);
        launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ComponentName componentName =
                launchIntent.resolveActivity(applicationContext.getPackageManager());
//            launchIntent.resolveActivity(applicationContext.getPackageManager().getLaunchIntentForPackage("com.package.name"));
//    getPackageManager().getLaunchIntentForPackage("com.package.name");
//    com.healerb.dna

        Log.i(TAG, "canLaunch test ");

//    return true;

        if (componentName == null) {
            Log.i(TAG, "component name for - " + url + " is null");
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
    LaunchStatus launch(
            String url,
            Bundle headersBundle,
            boolean useWebView,
            boolean enableJavaScript,
            boolean enableDomStorage) {
        if (activity == null) {
            return LaunchStatus.NO_ACTIVITY;
        }

        Intent launchIntent;
        if (useWebView) {
            launchIntent =
                    WebViewActivity.createIntent(
                            activity, url, enableJavaScript, enableDomStorage, headersBundle);
        } else {
            launchIntent =
                    new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(url))
                            .putExtra(Browser.EXTRA_HEADERS, headersBundle);
        }

        Log.e(TAG, "launch - " + url);

        try {
            activity.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
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
