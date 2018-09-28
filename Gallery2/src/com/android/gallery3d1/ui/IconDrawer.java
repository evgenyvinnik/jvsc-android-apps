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
package com.android.gallery3d1.ui;

import com.android.gallery3d1.R;
import android.content.Context;

public abstract class IconDrawer extends SelectionDrawer {
    private final ResourceTexture mLocalSetIcon;


    private final NinePatchTexture mFramePressed;
    private final NinePatchTexture mFrameSelected;
    private final NinePatchTexture mDarkStrip;
    private final NinePatchTexture mPanoramaBorder;

    private final int mIconSize;

    public static class IconDimension {
        int x;
        int y;
        int width;
        int height;
    }

    public IconDrawer(Context context) {
        mLocalSetIcon = new ResourceTexture(context, R.drawable.frame_overlay_gallery_folder);
        mPanoramaBorder = new NinePatchTexture(context, R.drawable.ic_pan_thumb);
        mFramePressed = new NinePatchTexture(context, R.drawable.grid_pressed);
        mFrameSelected = new NinePatchTexture(context, R.drawable.grid_selected);
        mDarkStrip = new NinePatchTexture(context, R.drawable.dark_strip);
        mIconSize = context.getResources().getDimensionPixelSize(
                R.dimen.albumset_icon_size);
    }

    @Override
    public void prepareDrawing() {
    }

    protected IconDimension drawIcon(GLCanvas canvas, int width, int height,
            int dataSourceType) {
        ResourceTexture icon = getIcon(dataSourceType);

        if (icon != null) {
            IconDimension id = getIconDimension(icon, width, height);
            icon.draw(canvas, id.x, id.y, id.width, id.height);
            return id;
        }
        return null;
    }

    protected ResourceTexture getIcon(int dataSourceType) {
        ResourceTexture icon = null;
        switch (dataSourceType) {
            case DATASOURCE_TYPE_LOCAL:
                icon = mLocalSetIcon;
                break;
            default:
                break;
        }

        return icon;
    }

    protected IconDimension getIconDimension(ResourceTexture icon, int width,
            int height) {
        IconDimension id = new IconDimension();
        float scale = (float) mIconSize / icon.getWidth();
        id.width = Math.round(scale * icon.getWidth());
        id.height = Math.round(scale * icon.getHeight());
        id.x = -width / 2;
        id.y = (height + 1) / 2 - id.height;
        return id;
    }

    protected void drawMediaTypeOverlay(GLCanvas canvas, int mediaType,
            boolean isPanorama, int x, int y, int width, int height) {

        if (isPanorama) {
            drawPanoramaBorder(canvas, x, y, width, height);
        }
    }



    protected void drawPanoramaBorder(GLCanvas canvas, int x, int y,
            int width, int height) {
        float scale = (float) width / mPanoramaBorder.getWidth();
        int w = Math.round(scale * mPanoramaBorder.getWidth());
        int h = Math.round(scale * mPanoramaBorder.getHeight());
        // draw at the top
        mPanoramaBorder.draw(canvas, x, y, w, h);
        // draw at the bottom
        mPanoramaBorder.draw(canvas, x, y + width - h, w, h);
    }

    protected void drawLabelBackground(GLCanvas canvas, int width, int height,
            int drawLabelBackground) {
        int x = -width / 2;
        int y = (height + 1) / 2 - drawLabelBackground;
        drawFrame(canvas, mDarkStrip, x, y, width, drawLabelBackground);
    }

    protected void drawPressedFrame(GLCanvas canvas, int x, int y, int width,
            int height) {
        drawFrame(canvas, mFramePressed, x, y, width, height);
    }

    protected void drawSelectedFrame(GLCanvas canvas, int x, int y, int width,
            int height) {
        drawFrame(canvas, mFrameSelected, x, y, width, height);
    }

    @Override
    public void drawFocus(GLCanvas canvas, int width, int height) {
    }
}
