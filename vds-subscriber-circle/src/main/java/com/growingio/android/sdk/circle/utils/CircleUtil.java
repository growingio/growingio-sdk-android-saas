package com.growingio.android.sdk.circle.utils;

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;

import com.growingio.android.sdk.autoburry.VdsJsHelper;
import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by liangdengke on 2019/1/2.
 */
public class CircleUtil {

    private static final String TAG = "GIO.CircleUtil";

    /**
     * 将ViewNode序列化为Impress信息， 发给圈选
     *
     *     以下字段会进行 URLEncoder.encode
     *     grObj
     *     grBannerContent
     *     content
     *     grContent
     *
     * @see ViewNode copy自
     */
    public static JSONObject getImpressObj(ViewNode viewNode){
        View view = viewNode.mView;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("xpath", viewNode.mParentXPath);
            if (viewNode.mLastListPos > -1) {
                jsonObject.put("index", String.valueOf(viewNode.mLastListPos));
            }
            if (viewNode.mParentXPath != null && viewNode.mParentXPath.length() != 0){
                jsonObject.put("patternXPath", viewNode.mPatternXPath);
            }
            Rect rect = new Rect();
            viewNode.getVisibleRect(view, rect, viewNode.mFullScreen);
            double scaledX = ScreenshotHelper.getScaledFactor();
            Util.getVisibleRectOnScreen(view, rect, viewNode.mFullScreen);
            if (viewNode.mClipRect != null) {
                if (!rect.intersect(viewNode.mClipRect)) {
                    return null;
                }
            }
            jsonObject.put("left", rect.left * scaledX);
            jsonObject.put("top", rect.top * scaledX);
            jsonObject.put("width", rect.width() * scaledX);
            jsonObject.put("height", rect.height() * scaledX);
            jsonObject.put("isTrackingEditText", (viewNode.mView instanceof EditText) || viewNode.hybridIsTrackingEditText);
            boolean isViewClickable = Util.isViewClickable(view);
            jsonObject.put("isClickable", isViewClickable);
            boolean isWebView = false;
            if (view instanceof WebView || ClassExistHelper.instanceOfX5WebView(view)){
                VdsJsHelper helper = (VdsJsHelper) view.getTag(AbstractGrowingIO.GROWING_WEB_BRIDGE_KEY);
                if (helper != null){
                    isWebView = helper.isReturnedData();
                }
            }
            jsonObject.put("isWebView", isWebView);
            jsonObject.put("isContainer", isViewClickable || (!viewNode.mInClickableGroup && !TextUtils.isEmpty(viewNode.mViewContent)));
            String nodeType = isViewClickable ? "button" : "text";
            if (viewNode.mWebElementInfo != null) {
                nodeType = viewNode.mWebElementInfo.mNodeType;
                jsonObject.put("href", viewNode.mWebElementInfo.mHref);
                jsonObject.put("query", viewNode.mWebElementInfo.mQuery);
            }
            jsonObject.put("nodeType", nodeType);
            if (viewNode.mClickableParentXPath != null && viewNode.mClickableParentXPath.length() != 0) {
                jsonObject.put("parentXPath", viewNode.mClickableParentXPath);
            }
            if (!TextUtils.isEmpty(viewNode.mViewContent)) {
                String encodedViewContent = URLEncoder.encode(viewNode.mViewContent, "UTF-8");
                encodedViewContent = encodedViewContent.replaceAll("\\+", "%20");
                jsonObject.put("content", encodedViewContent);
            }
            Object grContent = view.getTag(GrowingIO.GROWING_CONTENT_KEY);
            if (grContent != null) {
                if (grContent instanceof String) {
                    String grText = (String) grContent;
                    if (!TextUtils.isEmpty(grText)) {
                        String encodedGrText = URLEncoder.encode(grText, "UTF-8");
                        encodedGrText = encodedGrText.replaceAll("\\+", "%20");
                        jsonObject.put("grContent", encodedGrText);
                    }
                } else {
                    jsonObject.put("grContent", grContent);
                }
            }
            jsonObject.put("grImage", view instanceof ImageView);
            if (!TextUtils.isEmpty(viewNode.mBannerText)) {
                String encodedBannerText = URLEncoder.encode(viewNode.mBannerText, "UTF-8");
                encodedBannerText = encodedBannerText.replaceAll("\\+", "%20");
                jsonObject.put("grBannerContent", encodedBannerText);
            }
            jsonObject.put("grIgnored", Util.isIgnoredView(view));
            if (!TextUtils.isEmpty(viewNode.mInheritableGrowingInfo)) {
                String encodedGrowingInfo = URLEncoder.encode(viewNode.mInheritableGrowingInfo, "UTF-8");
                encodedGrowingInfo = encodedGrowingInfo.replaceAll("\\+", "%20");
                jsonObject.put("grObj", encodedGrowingInfo);
            }
            jsonObject.put("isContentEncoded", true);
        } catch (JSONException e) {
            LogUtil.d(TAG, "generate impress view error", e);
            return null;
        } catch (UnsupportedEncodingException codeExcp) {
            LogUtil.d(TAG, "generate impress encode error", codeExcp);
            return null;
        }

        return jsonObject;
    }
}
