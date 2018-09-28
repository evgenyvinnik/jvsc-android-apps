/*
 * Copyright (C) 2010 The Android Open Source Project
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

import ca.jvsh.jpegtoy.data.DataManager;
import ca.jvsh.jpegtoy.data.ImageCacheService;
import ca.jvsh.jpegtoy.util.GalleryUtils;
import ca.jvsh.jpegtoy.util.ThreadPool;


import android.app.Application;
import android.content.Context;

public class GalleryAppImpl extends Application implements GalleryApp {

    private ImageCacheService mImageCacheService;
    private DataManager mDataManager;
    private ThreadPool mThreadPool;
 
    @Override
    public void onCreate() {
        super.onCreate();
        GalleryUtils.initialize(this);
    }

    public Context getAndroidContext() {
        return this;
    }

    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
        }
        return mDataManager;
    }

    public synchronized ImageCacheService getImageCacheService() {
        if (mImageCacheService == null) {
            mImageCacheService = new ImageCacheService(getAndroidContext());
        }
        return mImageCacheService;
    }

    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }
}
