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

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import ca.jvsh.jpegtoy.R;
import ca.jvsh.jpegtoy.data.MediaDetails;
import ca.jvsh.jpegtoy.data.MediaItem;
import ca.jvsh.jpegtoy.data.MediaObject;
import ca.jvsh.jpegtoy.data.MediaSet;
import ca.jvsh.jpegtoy.data.Path;
import ca.jvsh.jpegtoy.ui.DetailsHelper;
import ca.jvsh.jpegtoy.ui.GLCanvas;
import ca.jvsh.jpegtoy.ui.GLView;
import ca.jvsh.jpegtoy.ui.MenuExecutor;
import ca.jvsh.jpegtoy.ui.PhotoView;
import ca.jvsh.jpegtoy.ui.PositionRepository;
import ca.jvsh.jpegtoy.ui.SelectionManager;
import ca.jvsh.jpegtoy.ui.SynchronizedHandler;
import ca.jvsh.jpegtoy.ui.UserInteractionListener;
import ca.jvsh.jpegtoy.ui.DetailsHelper.CloseListener;
import ca.jvsh.jpegtoy.ui.DetailsHelper.DetailsSource;
import ca.jvsh.jpegtoy.ui.PositionRepository.Position;
import ca.jvsh.jpegtoy.util.GalleryUtils;





public class PhotoPage extends ActivityState
        implements PhotoView.PhotoTapListener, 
        UserInteractionListener {
    private static final String TAG = "PhotoPage";

    private static final int MSG_HIDE_BARS = 1;

    private static final int HIDE_BARS_TIMEOUT = 3500;

  

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path";
    public static final String KEY_INDEX_HINT = "index-hint";

    private SelectionManager mSelectionManager;

    private PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;
 
    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private MediaSet mMediaSet;
    private Menu mMenu;

    private final Intent mResultIntent = new Intent();
    private int mCurrentIndex = 0;
    private Handler mHandler;
    private boolean mShowBars = true;
    private ActionBar mActionBar;
    private MyMenuVisibilityListener mMenuVisibilityListener;
    private boolean mIsMenuVisible;
    private boolean mIsInteracting;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;
 
    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public MediaItem getCurrentMediaItem();
        public int getCurrentIndex();
        public void setCurrentPhoto(Path path, int indexHint);
    }

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    private final GLView mRootPane = new GLView() {

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer();
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            PositionRepository.getInstance(mActivity).setOffset(0, 0);
           
           
            if (mShowDetails) {
                mDetailsHelper.layout(left, GalleryActionBar.getHeight((Activity) mActivity),
                        right, bottom);
            }
        }
    };

   
    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mActionBar = ((Activity) mActivity).getActionBar();
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);

        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setPhotoTapListener(this);
        mRootPane.addComponent(mPhotoView);
       ((Activity) mActivity).getApplication();

        String setPathString = data.getString(KEY_MEDIA_SET_PATH);
        Path itemPath = Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH));

        if (setPathString != null) {
            mMediaSet = mActivity.getDataManager().getMediaSet(setPathString);
            mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
            mMediaSet = (MediaSet)
                    mActivity.getDataManager().getMediaObject(setPathString);
            if (mMediaSet == null) {
                Log.w(TAG, "failed to restore " + setPathString);
            }
            PhotoDataAdapter pda = new PhotoDataAdapter(
                    mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex);
            mModel = pda;
            mPhotoView.setModel(mModel);

            mResultIntent.putExtra(KEY_INDEX_HINT, mCurrentIndex);
            setStateResult(Activity.RESULT_OK, mResultIntent);

            pda.setDataListener(new PhotoDataAdapter.DataListener() {

                @Override
                public void onPhotoChanged(int index, Path item) {
                   
                    mCurrentIndex = index;
                    mResultIntent.putExtra(KEY_INDEX_HINT, index);
                    if (item != null) {
                        mResultIntent.putExtra(KEY_MEDIA_ITEM_PATH, item.toString());
                        MediaItem photo = mModel.getCurrentMediaItem();
                        if (photo != null) updateCurrentPhoto(photo);
                    } else {
                        mResultIntent.removeExtra(KEY_MEDIA_ITEM_PATH);
                    }
                    setStateResult(Activity.RESULT_OK, mResultIntent);
                }

                @Override
                public void onLoadingFinished() {
                    GalleryUtils.setSpinnerVisibility((Activity) mActivity, false);
                    if (!mModel.isEmpty()) {
                        MediaItem photo = mModel.getCurrentMediaItem();
                        if (photo != null) updateCurrentPhoto(photo);
                    } else if (mIsActive) {
                        mActivity.getStateManager().finishState(PhotoPage.this);
                    }
                }

                @Override
                public void onLoadingStarted() {
                    GalleryUtils.setSpinnerVisibility((Activity) mActivity, true);
                }

				@Override
				public void onPhotoAvailable(long version, boolean fullImage)
				{
					// TODO Auto-generated method stub
					
				}

               
            });
        } else {
            // Get default media set by the URI
            MediaItem mediaItem = (MediaItem)
                    mActivity.getDataManager().getMediaObject(itemPath);
            mModel = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
            mPhotoView.setModel(mModel);
            updateCurrentPhoto(mediaItem);
        }

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };

        // start the opening animation
        mPhotoView.setOpenedItem(itemPath);
    }

  

    private void setTitle(String title) {
        if (title == null) return;
        boolean showTitle = mActivity.getAndroidContext().getResources().getBoolean(
                R.bool.show_action_bar_title);
        if (showTitle)
            mActionBar.setTitle(title);
        else
            mActionBar.setTitle("");
    }

    private void updateCurrentPhoto(MediaItem photo) {
        if (mCurrentPhoto == photo) return;
        mCurrentPhoto = photo;
        if (mCurrentPhoto == null) return;
        updateMenuOperations();
        if (mShowDetails) {
            mDetailsHelper.reloadDetails(mModel.getCurrentIndex());
        }
        setTitle(photo.getName());

   
    }

    private void updateMenuOperations() {
        if (mMenu == null) return;

        if (mCurrentPhoto == null) return;
        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (!GalleryUtils.isEditorAvailable((Context) mActivity, "image/*")) {
            supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        }

        MenuExecutor.updateMenuOperation(mMenu, supportedOperations);
    }

    

    private void showBars() {
        if (mShowBars) return;
        mShowBars = true;
        mActionBar.show();
        WindowManager.LayoutParams params = ((Activity) mActivity).getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE;
        ((Activity) mActivity).getWindow().setAttributes(params);
        
    }

    private void hideBars() {
        if (!mShowBars) return;
        mShowBars = false;
        mActionBar.hide();
        WindowManager.LayoutParams params = ((Activity) mActivity).getWindow().getAttributes();
        params.systemUiVisibility = View. SYSTEM_UI_FLAG_LOW_PROFILE;
        ((Activity) mActivity).getWindow().setAttributes(params);
      
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mIsInteracting) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    @Override
    public void onUserInteraction() {
        showBars();
        refreshHidingMessage();
    }

    public void onUserInteractionTap() {
        if (mShowBars) {
            hideBars();
            mHandler.removeMessages(MSG_HIDE_BARS);
        } else {
            showBars();
            refreshHidingMessage();
        }
    }

    @Override
    public void onUserInteractionBegin() {
        showBars();
        mIsInteracting = true;
        refreshHidingMessage();
    }

    @Override
    public void onUserInteractionEnd() {
        mIsInteracting = false;

        // This function could be called from GL thread (in SlotView.render)
        // and post to the main thread. So, it could be executed while the
        // activity is paused.
        if (mIsActive) refreshHidingMessage();
    }

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else {
            PositionRepository repository = PositionRepository.getInstance(mActivity);
            repository.clear();
            if (mCurrentPhoto != null) {
                Position position = new Position();
                position.x = mRootPane.getWidth() / 2;
                position.y = mRootPane.getHeight() / 2;
                position.z = -1000;
                repository.putPosition(
                        Long.valueOf(System.identityHashCode(mCurrentPhoto.getPath())),
                        position);
            }
            super.onBackPressed();
        }
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        MenuInflater inflater = ((Activity) mActivity).getMenuInflater();
        inflater.inflate(R.menu.photo, menu);
          mMenu = menu;
        mShowBars = true;
        updateMenuOperations();
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        MediaItem current = mModel.getCurrentMediaItem();

        if (current == null) {
            // item is not ready, ignore
            return true;
        }

        int currentIndex = mModel.getCurrentIndex();
      
        int action = item.getItemId();
        switch (action) {
            
            
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails(currentIndex);
                }
                return true;
            }
            
           
            default :
                return false;
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
    }

    private void showDetails(int index) {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.reloadDetails(index);
        mDetailsHelper.show();
    }

    public void onSingleTapUp(int x, int y) {
        MediaItem item = mModel.getCurrentMediaItem();
        if (item == null) {
            // item is not ready, ignore
            return;
        }

      
            onUserInteractionTap();
        
    }



    

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        
        DetailsHelper.pause();
        mPhotoView.pause();
        mModel.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        mMenuExecutor.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
        setContentPane(mRootPane);
        mModel.resume();
        mPhotoView.resume();
        
        if (mMenuVisibilityListener == null) {
            mMenuVisibilityListener = new MyMenuVisibilityListener();
        }
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        onUserInteraction();
    }

    private class MyDetailsSource implements DetailsSource {
        private int mIndex;

        @Override
        public MediaDetails getDetails() {
            return mModel.getCurrentMediaItem().getDetails();
        }

        @Override
        public int size() {
            return mMediaSet != null ? mMediaSet.getMediaItemCount() : 1;
        }

        @Override
        public int findIndex(int indexHint) {
            mIndex = indexHint;
            return indexHint;
        }

        @Override
        public int getIndex() {
            return mIndex;
        }
    }
}
