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
package com.android.quickstep.inputconsumers;

import static com.android.systemui.shared.system.ActivityManagerWrapper.CLOSE_SYSTEM_WINDOWS_REASON_RECENTS;

import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.android.launcher3.Utilities;
import com.android.launcher3.statemanager.BaseState;
import com.android.launcher3.statemanager.StatefulActivity;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.BaseActivityInterface;
import com.android.quickstep.GestureState;
import com.android.quickstep.InputConsumer;
import com.android.quickstep.TaskUtils;
import com.android.systemui.shared.system.InputMonitorCompat;

/**
 * Input consumer for handling touch on the recents/Launcher activity.
 */
public class OverviewInputConsumer<S extends BaseState<S>, T extends StatefulActivity<S>>
        implements InputConsumer {

    private final T mActivity;
    private final BaseActivityInterface<?, T> mActivityInterface;
    private final BaseDragLayer mTarget;
    private final InputMonitorCompat mInputMonitor;

    private final int[] mLocationOnScreen = new int[2];

    private final boolean mStartingInActivityBounds;
    private boolean mTargetHandledTouch;
    private boolean mHasSetTouchModeForFirstDPadEvent;

    public OverviewInputConsumer(GestureState gestureState, T activity,
            @Nullable InputMonitorCompat inputMonitor, boolean startingInActivityBounds) {
        mActivity = activity;
        mInputMonitor = inputMonitor;
        mStartingInActivityBounds = startingInActivityBounds;
        mActivityInterface = gestureState.getActivityInterface();

        mTarget = activity.getDragLayer();
        mTarget.getLocationOnScreen(mLocationOnScreen);
    }

    @Override
    public int getType() {
        return TYPE_OVERVIEW;
    }

    @Override
    public boolean allowInterceptByParent() {
        return !mTargetHandledTouch;
    }

    @Override
    public void onMotionEvent(MotionEvent ev) {
        int flags = ev.getEdgeFlags();
        if (!mStartingInActivityBounds) {
            ev.setEdgeFlags(flags | Utilities.EDGE_NAV_BAR);
        }
        ev.offsetLocation(-mLocationOnScreen[0], -mLocationOnScreen[1]);
        boolean handled = mTarget.proxyTouchEvent(ev, mStartingInActivityBounds);
        ev.offsetLocation(mLocationOnScreen[0], mLocationOnScreen[1]);
        ev.setEdgeFlags(flags);

        if (!mTargetHandledTouch && handled) {
            mTargetHandledTouch = true;
            if (!mStartingInActivityBounds) {
                mActivityInterface.closeOverlay();
                TaskUtils.closeSystemWindowsAsync(CLOSE_SYSTEM_WINDOWS_REASON_RECENTS);
            }
            if (mInputMonitor != null) {
                TestLogging.recordEvent(TestProtocol.SEQUENCE_PILFER, "pilferPointers");
                mInputMonitor.pilferPointers();
            }
        }
        if (mHasSetTouchModeForFirstDPadEvent) {
            mActivity.getRootView().clearFocus();
        }
    }

    @Override
    public void onHoverEvent(MotionEvent ev) {}

    @Override
    public void onKeyEvent(KeyEvent ev) {}
}

