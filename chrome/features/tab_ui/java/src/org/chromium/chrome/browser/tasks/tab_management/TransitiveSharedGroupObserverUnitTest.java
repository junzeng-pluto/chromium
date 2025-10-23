// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.tasks.tab_management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.chromium.components.data_sharing.SharedGroupTestHelper.COLLABORATION_ID1;
import static org.chromium.components.data_sharing.SharedGroupTestHelper.COLLABORATION_ID2;
import static org.chromium.components.data_sharing.SharedGroupTestHelper.GROUP_MEMBER1;
import static org.chromium.components.data_sharing.SharedGroupTestHelper.GROUP_MEMBER2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.chromium.base.Callback;
import org.chromium.base.Token;
import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.components.data_sharing.DataSharingService;
import org.chromium.components.data_sharing.SharedGroupTestHelper;
import org.chromium.components.tab_group_sync.LocalTabGroupId;
import org.chromium.components.tab_group_sync.SavedTabGroup;
import org.chromium.components.tab_group_sync.TabGroupSyncService;

/** Unit tests for {@link TransitiveSharedGroupObserver}. */
@RunWith(BaseRobolectricTestRunner.class)
public class TransitiveSharedGroupObserverUnitTest {
    private static final Token TAB_GROUP_ID_1 = new Token(1L, 2L);
    private static final Token TAB_GROUP_ID_2 = new Token(4L, 3L);
    private static final Token TAB_GROUP_ID_3 = new Token(4L, 6L);

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private DataSharingService mDataSharingService;
    @Mock private TabGroupSyncService mTabGroupSyncService;
    @Mock private Callback<Integer> mOnSharedGroupStateChanged;
    @Mock private Callback<String> mOnSharedGroupCollaborationIdChanged;

    private SharedGroupTestHelper mSharedGroupTestHelper;

    @Before
    public void setUp() {
        mSharedGroupTestHelper = new SharedGroupTestHelper(mDataSharingService);
    }

    @Test
    public void testDestroy_NoTabGroupId() {
        TransitiveSharedGroupObserver observer =
                new TransitiveSharedGroupObserver(mTabGroupSyncService, mDataSharingService);
        verify(mDataSharingService, never()).addObserver(any());

        observer.destroy();
        verify(mDataSharingService, never()).removeObserver(any());
    }

    @Test
    public void testDestroy_WithTabGroupId() {
        TransitiveSharedGroupObserver observer =
                new TransitiveSharedGroupObserver(mTabGroupSyncService, mDataSharingService);
        observer.setTabGroupId(TAB_GROUP_ID_1);
        verify(mDataSharingService).addObserver(any());

        observer.destroy();
        verify(mDataSharingService).removeObserver(any());
    }

    @Test
    public void testChangeTabGroupId_WithData() {
        TransitiveSharedGroupObserver observer =
                new TransitiveSharedGroupObserver(mTabGroupSyncService, mDataSharingService);
        observer.getGroupSharedStateSupplier().addObserver(mOnSharedGroupStateChanged);
        observer.getCollaborationIdSupplier().addObserver(mOnSharedGroupCollaborationIdChanged);

        SavedTabGroup savedTabGroup = new SavedTabGroup();
        savedTabGroup.collaborationId = COLLABORATION_ID1;
        when(mTabGroupSyncService.getGroup(any(LocalTabGroupId.class))).thenReturn(savedTabGroup);
        observer.setTabGroupId(TAB_GROUP_ID_1);
        mSharedGroupTestHelper.respondToReadGroup(COLLABORATION_ID1, GROUP_MEMBER1);

        verify(mOnSharedGroupStateChanged).onResult(GroupSharedState.COLLABORATION_ONLY);
        verify(mOnSharedGroupCollaborationIdChanged).onResult(COLLABORATION_ID1);

        savedTabGroup.collaborationId = COLLABORATION_ID2;
        observer.setTabGroupId(TAB_GROUP_ID_2);
        mSharedGroupTestHelper.respondToReadGroup(COLLABORATION_ID2, GROUP_MEMBER1, GROUP_MEMBER2);

        verify(mOnSharedGroupStateChanged).onResult(GroupSharedState.HAS_OTHER_USERS);
        verify(mOnSharedGroupCollaborationIdChanged).onResult(COLLABORATION_ID2);

        savedTabGroup.collaborationId = null;
        observer.setTabGroupId(TAB_GROUP_ID_3);

        verify(mOnSharedGroupStateChanged).onResult(GroupSharedState.NOT_SHARED);
        verify(mOnSharedGroupCollaborationIdChanged).onResult(null);

        observer.destroy();
    }
}
