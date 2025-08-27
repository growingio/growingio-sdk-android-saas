/*
 * Copyright (C) 2023 Beijing Yishu Technology Co., Ltd.
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
package com.growingio.android.sdk.autotrack.inject;

import com.growingio.android.sdk.autoburry.VdsAgent;

public class FragmentV4Injector {
    private static final String TAG = "FragmentV4Injector";

    private FragmentV4Injector() {
    }

    public static void v4FragmentOnResume(android.support.v4.app.Fragment fragment) {
        VdsAgent.onFragmentResume(fragment);
    }

    public static void v4FragmentOnStart(android.support.v4.app.Fragment fragment) {
    }

    public static void v4FragmentOnPause(android.support.v4.app.Fragment fragment) {
    }

    public static void v4FragmentOnStop(android.support.v4.app.Fragment fragment) {
    }

    public static void v4FragmentSetUserVisibleHint(android.support.v4.app.Fragment fragment, boolean isVisibleToUser) {
        VdsAgent.setFragmentUserVisibleHint(fragment, isVisibleToUser);
    }

    public static void v4FragmentOnHiddenChanged(android.support.v4.app.Fragment fragment, boolean hidden) {
        VdsAgent.onFragmentHiddenChanged(fragment, hidden);
    }

    public static void v4FragmentOnDestroyView(android.support.v4.app.Fragment fragment) {
    }
}
