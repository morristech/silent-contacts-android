/*
Copyright © 2013-2014, Silent Circle, LLC.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the names of its contributors may 
      be used to endorse or promote products derived from this software 
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * This  implementation is edited version of original Android sources.
 */

/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.silentcircle.contacts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A FrameLayout whose contents are kept beneath an {@link AlphaTouchInterceptorOverlay}.
 * If necessary, you can specify your own alpha-layer and manually manage its z-order.
 */
public class FrameLayoutWithOverlay extends FrameLayout {
    private final AlphaTouchInterceptorOverlay mOverlay;

    public FrameLayoutWithOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Programmatically create touch-interceptor View.
        mOverlay = new AlphaTouchInterceptorOverlay(context);

        addView(mOverlay);
    }

    /** After adding the View, bring the overlay to the front to ensure it's always on top. */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        mOverlay.bringToFront();
    }

    /**
     * Delegate to overlay:  set the View that it will use as its alpha-layer.
     * If none is set, the overlay will use its own alpha layer.  Only
     * necessary to set this if some child views need to appear above the
     * alpha-layer.
     */
    protected void setAlphaLayer(View layer) {
        mOverlay.setAlphaLayer(layer);
    }

    /** Delegate to overlay: set the alpha value on the alpha layer. */
    public void setAlphaLayerValue(float alpha) {
        mOverlay.setAlphaLayerValue(alpha);
    }

    /** Delegate to overlay. */
    public void setOverlayOnClickListener(OnClickListener listener) {
        mOverlay.setOverlayOnClickListener(listener);
    }

    /** Delegate to overlay. */
    public void setOverlayClickable(boolean clickable) {
        mOverlay.setOverlayClickable(clickable);
    }
}