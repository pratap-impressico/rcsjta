/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.IVideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;
import com.gsma.services.rcs.sharing.video.VideoSharingIntent;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * VideoSharingEventBroadcaster maintains the registering and unregistering of IVideoSharingListener
 * and also performs broadcast events on these listeners upon the trigger of corresponding
 * callbacks.
 */
public class VideoSharingEventBroadcaster implements IVideoSharingEventBroadcaster {

    private final RemoteCallbackList<IVideoSharingListener> mVideoSharingListeners = new RemoteCallbackList<IVideoSharingListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public VideoSharingEventBroadcaster() {
    }

    public void addEventListener(IVideoSharingListener listener) {
        mVideoSharingListeners.register(listener);
    }

    public void removeEventListener(IVideoSharingListener listener) {
        mVideoSharingListeners.unregister(listener);
    }

    public void broadcastStateChanged(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode) {
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        final int N = mVideoSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mVideoSharingListeners.getBroadcastItem(i).onStateChanged(contact, sharingId,
                        rcsState, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mVideoSharingListeners.finishBroadcast();
    }

    public void broadcastInvitation(String sharingId) {
        Intent newInvitation = new Intent(VideoSharingIntent.ACTION_NEW_INVITATION);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(newInvitation);
        IntentUtils.tryToSetReceiverForegroundFlag(newInvitation);
        newInvitation.putExtra(VideoSharingIntent.EXTRA_SHARING_ID, sharingId);
        AndroidFactory.getApplicationContext().sendBroadcast(newInvitation);
    }

    public void broadcastDeleted(ContactId contact, Set<String> sharingIds) {
        List<String> ids = new ArrayList<String>(sharingIds);
        final int N = mVideoSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mVideoSharingListeners.getBroadcastItem(i).onDeleted(contact, ids);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mVideoSharingListeners.finishBroadcast();
    }
}
