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

public class FragmentInjector {
    private static final String TAG = "FragmentInjector";

    private FragmentInjector() {
    }

    public static void systemFragmentOnResume(android.app.Fragment fragment) {
        VdsAgent.onFragmentResume(fragment);
    }

    public static void systemFragmentOnStart(android.app.Fragment fragment) {
    }

    public static void systemFragmentOnPause(android.app.Fragment fragment) {
    }

    public static void systemFragmentOnStop(android.app.Fragment fragment) {
    }

    public static void systemFragmentSetUserVisibleHint(android.app.Fragment fragment, boolean isVisibleToUser) {
        VdsAgent.setFragmentUserVisibleHint(fragment, isVisibleToUser);
    }

    public static void systemFragmentOnHiddenChanged(android.app.Fragment fragment, boolean hidden) {
        VdsAgent.onFragmentHiddenChanged(fragment, hidden);
    }

    public static void systemFragmentOnDestroyView(android.app.Fragment fragment) {

    }

    public static void androidxFragmentOnResume(androidx.fragment.app.Fragment fragment) {
        VdsAgent.onFragmentResume(fragment);
    }

    public static void androidxFragmentOnStart(androidx.fragment.app.Fragment fragment) {
    }

    public static void androidxFragmentOnPause(androidx.fragment.app.Fragment fragment) {
    }

    public static void androidxFragmentOnStop(androidx.fragment.app.Fragment fragment) {
    }

    /**
     * 新版本的AndroidX Fragment setUserVisibleHint 将通过 FragmentTransaction setMaxLifecycle 来控制生命周期实现
     */
    @Deprecated
    public static void androidxFragmentSetUserVisibleHint(androidx.fragment.app.Fragment fragment, boolean isVisibleToUser) {
        VdsAgent.setFragmentUserVisibleHint(fragment, isVisibleToUser);
    }

    public static void androidxFragmentOnHiddenChanged(androidx.fragment.app.Fragment fragment, boolean hidden) {
        VdsAgent.onFragmentHiddenChanged(fragment, hidden);
    }

    public static void androidxFragmentOnDestroyView(androidx.fragment.app.Fragment fragment) {
    }

}
