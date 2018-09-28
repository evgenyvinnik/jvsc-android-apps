/*
 * Copyright (C) 2011 The Android Open Source Project
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

package ca.jvsh.jpegtoy.app;

import ca.jvsh.jpegtoy.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GalleryActionBar implements ActionBar.OnNavigationListener {
    private static final String TAG = "GalleryActionBar";

    public interface ClusterRunner {
        public void doCluster(int id);
    }

    private static class ActionItem {
        public int action;
        public int spinnerTitle;
        public int clusterBy;

        public ActionItem(int action, boolean applied, boolean enabled, int title,
                int clusterBy) {
            this(action, applied, enabled, title, title, clusterBy);
        }

        public ActionItem(int action, boolean applied, boolean enabled, int spinnerTitle,
                int dialogTitle, int clusterBy) {
            this.action = action;
            this.spinnerTitle = spinnerTitle;
            this.clusterBy = clusterBy;
        }
    }

    private static final ActionItem[] sClusterItems = new ActionItem[] {
        new ActionItem(FilterUtils.CLUSTER_BY_ALBUM, true, false, R.string.albums,
                R.string.group_by_album),
       
    };

    private class ClusterAdapter extends BaseAdapter {

        public int getCount() {
            return sClusterItems.length;
        }

        public Object getItem(int position) {
            return sClusterItems[position];
        }

        public long getItemId(int position) {
            return sClusterItems[position].action;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.action_bar_text,
                        parent, false);
            }
            TextView view = (TextView) convertView;
            view.setText(sClusterItems[position].spinnerTitle);
            return convertView;
        }
    }

    private ClusterRunner mClusterRunner;
    private LayoutInflater mInflater;
    private GalleryActivity mActivity;
    private ActionBar mActionBar;
    private int mCurrentIndex;
    private ClusterAdapter mAdapter = new ClusterAdapter();

    public GalleryActionBar(GalleryActivity activity) {
        mActionBar = ((Activity) activity).getActionBar();
        activity.getAndroidContext();
        mActivity = activity;
        mInflater = ((Activity) mActivity).getLayoutInflater();
        mCurrentIndex = 0;
    }

    public static int getHeight(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        return actionBar != null ? actionBar.getHeight() : 0;
    }

    public void setClusterItemEnabled(int id, boolean enabled) {
        for (ActionItem item : sClusterItems) {
            if (item.action == id) {
                return;
            }
        }
    }

    public void setClusterItemVisibility(int id, boolean visible) {
        for (ActionItem item : sClusterItems) {
            if (item.action == id) {
                return;
            }
        }
    }

    public int getClusterTypeAction() {
        return sClusterItems[mCurrentIndex].action;
    }

    public static String getClusterByTypeString(Context context, int type) {
        for (ActionItem item : sClusterItems) {
            if (item.action == type) {
                return context.getString(item.clusterBy);
            }
        }
        return null;
    }

   

    public void showClusterMenu(int action, ClusterRunner runner) {
        Log.v(TAG, "showClusterMenu: runner=" + runner);
        // Don't set cluster runner until action bar is ready.
        mClusterRunner = null;
        mActionBar.setListNavigationCallbacks(mAdapter, this);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        setSelectedAction(action);
        mClusterRunner = runner;
    }

    public void hideClusterMenu() {
        mClusterRunner = null;
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    
    public void setTitle(String title) {
        if (mActionBar != null) mActionBar.setTitle(title);
    }

    public void setTitle(int titleId) {
        if (mActionBar != null) mActionBar.setTitle(titleId);
    }

    public void setSubtitle(String title) {
        if (mActionBar != null) mActionBar.setSubtitle(title);
    }

    public void setNavigationMode(int mode) {
        if (mActionBar != null) mActionBar.setNavigationMode(mode);
    }

    public int getHeight() {
        return mActionBar == null ? 0 : mActionBar.getHeight();
    }

    public boolean setSelectedAction(int type) {
        for (int i = 0, n = sClusterItems.length; i < n; i++) {
            ActionItem item = sClusterItems[i];
            if (item.action == type) {
                mActionBar.setSelectedNavigationItem(i);
                mCurrentIndex = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition != mCurrentIndex && mClusterRunner != null) {
            mActivity.getGLRoot().lockRenderThread();
            try {
                mClusterRunner.doCluster(sClusterItems[itemPosition].action);
            } finally {
                mActivity.getGLRoot().unlockRenderThread();
            }
        }
        return false;
    }
}
