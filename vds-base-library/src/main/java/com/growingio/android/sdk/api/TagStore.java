package com.growingio.android.sdk.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.Tag;
import com.growingio.android.sdk.models.ViewAttrs;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xyz on 15/9/8.
 */

public class TagStore {

    static final String TAG = "GrowingIO.TagStore";

    public static final int CURRENT_PAGE = 0;
    public static final int ALL_PAGE = 1;

    public static TagStore sInstance = new TagStore();

    private final Object mTagsLock = new Object();

    private InitSuccess mInitSuccess;

    public void setInitSuccess(InitSuccess initSuccess) {
        mInitSuccess = initSuccess;
    }

    private boolean mTagsReady = false;
    private boolean mLoading = false;

    public boolean isTagsReady() {
        return mTagsReady;
    }

    public boolean isLoading() {
        return mLoading;
    }

    public static TagStore getInstance() {
        return sInstance;
    }

    public List<Tag> getTags() {
        return mTags;
    }

    private List<Tag> mTags = new ArrayList<Tag>();

    private TagStore() {
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void initial() {
        if (mLoading)
            return;
        mLoading = true;
        new TagAPI(){
            @Override
            public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                super.afterRequest(responseCode, data, mLastModified, mResponseHeaders);
                if (responseCode == HttpURLConnection.HTTP_OK){
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onTagHttpResponse(tags);
                        }
                    });
                }
            }
        }.run();
    }

    private void onTagHttpResponse(List<Tag> tags){
        mTags.clear();
        mTags.addAll(tags);

        TagStore.this.mTagsReady = true;
        TagStore.this.mLoading = false;

        if (mInitSuccess != null) {
            mInitSuccess.initSuccess();
            mInitSuccess = null;
        }
    }

    public static Tag createNewTag(String spn, String eventType, String path, String xpath, int pageMode, String content, String index, String query, String href, ViewAttrs pageFilter) {
        String name;
        ViewAttrs attrs = new ViewAttrs();
        ViewAttrs filter = new ViewAttrs();
        attrs.domain = spn;
        filter.domain = attrs.domain;
        attrs.path = path;
        attrs.query = query;
        if (eventType.equals("elem")) {
            attrs.xpath = xpath;
            attrs.index = index;
            attrs.content = content;
            attrs.href = href;
            filter.xpath = normalizeXPath(xpath);
            filter.content = content;
            filter.index = index;
            filter.href = href;
            if (pageFilter != null) {
                filter.domain = pageFilter.domain;
                filter.path = pageFilter.path;
                filter.query = pageFilter.query;
            } else {
                filter.path = path;
            }
        } else if (eventType.equals(PageEvent.TYPE_NAME)) {
            attrs.content = null;
            filter.path = path;
            filter.xpath = null;
            filter.content = null;
            filter.index = null;
            filter.query = query;
        }
        Tag tag = new Tag();
        tag.eventType = eventType;
        tag.platform = Constants.PLATFORM_ANDROID;
        tag.attrs = attrs;
        tag.filter = filter;
        return tag;
    }

    private static String normalizeXPath(String xpath) {
        if (GConfig.USE_ID) {
            int webPartIndex = xpath.indexOf(Constants.WEB_PART_SEPARATOR);
            String nativeXPath = xpath;
            String webXPath = "";
            if (webPartIndex != -1) {
                nativeXPath = xpath.substring(0, webPartIndex);
                webXPath = xpath.substring(webPartIndex);
            }
            if (GConfig.CIRCLE_USE_ID) {
                int idIndex = nativeXPath.lastIndexOf(Constants.ID_PREFIX);
                if (idIndex != -1) {
                    nativeXPath = nativeXPath.substring(idIndex);
                    return '*' + nativeXPath + webXPath;
                }
            } else {
                nativeXPath = Util.ID_PATTERN_MATCHER.reset(nativeXPath).replaceAll("");
                return nativeXPath + webXPath;
            }
        }
        return xpath;
    }

    public List<Tag> getWebTags() {
        List<Tag> webTags = new ArrayList<Tag>();
        String spn = CoreInitialize.coreAppState().getSPN() + Constants.WEB_PART_SEPARATOR;
        for (Tag tag : mTags) {
            if (tag.attrs.domain.startsWith(spn)) {
                Tag webTag = tag.copyWithoutScreenShot();
                int webPartIndex = spn.length();
                webTag.attrs.domain = webTag.attrs.domain.substring(webPartIndex);
                if (!TextUtils.isEmpty(webTag.filter.domain) && webTag.filter.domain.length() >= webPartIndex) {
                    webTag.filter.domain = webTag.filter.domain.substring(webPartIndex);
                }
                webPartIndex = webTag.attrs.path.indexOf(Constants.WEB_PART_SEPARATOR);
                if (webPartIndex > 0) {
                    webPartIndex += Constants.WEB_PART_SEPARATOR.length();
                    webTag.attrs.path = webTag.attrs.path.substring(webPartIndex);
                    if (!TextUtils.isEmpty(webTag.filter.path) && webTag.filter.path.length() > webPartIndex) {
                        webTag.filter.path = webTag.filter.path.substring(webPartIndex);
                    }
                }
                if (!TextUtils.isEmpty(webTag.attrs.xpath)) {
                    webPartIndex = webTag.attrs.xpath.indexOf(Constants.WEB_PART_SEPARATOR);
                    webPartIndex += Constants.WEB_PART_SEPARATOR.length();
                    if (webPartIndex > 0) {
                        webTag.attrs.xpath = webTag.attrs.xpath.substring(webPartIndex);
                        if (!TextUtils.isEmpty(webTag.filter.xpath) && webTag.filter.xpath.length() > webPartIndex) {
                            webTag.filter.xpath = webTag.filter.xpath.substring(webPartIndex);
                        }
                    }
                }
                webTags.add(webTag);
            }
        }
        return webTags;
    }

    public void addTag(Tag tag) {
        synchronized (mTagsLock) {
            mTags.add(tag);
        }
    }

    public Tag getPageTag(String spn, String path, String query) {
        if (query == null) query = "";
        for (Tag tag : mTags) {
            if (tag.eventType.equals(PageEvent.TYPE_NAME)
                    && Constants.PLATFORM_ANDROID.equalsIgnoreCase(tag.platform)
                    && TextUtils.equals(tag.attrs.domain, spn)
                    && TextUtils.equals(tag.attrs.path, path)
                    && TextUtils.equals(tag.attrs.query, query)) {
                return tag;
            }
        }
        return createNewTag(spn, PageEvent.TYPE_NAME, path, null, CURRENT_PAGE, null, null, query, null, null);
    }

    public void removeTagById(String id) {
        if (TextUtils.isEmpty(id)) return;
        for (Tag tag : mTags) {
            if (TextUtils.equals(id, tag.id)) {
                mTags.remove(tag);
                return;
            }
        }
    }

    public interface InitSuccess {
        void initSuccess();
    }


}
