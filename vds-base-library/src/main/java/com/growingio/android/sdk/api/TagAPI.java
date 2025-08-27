package com.growingio.android.sdk.api;

import android.text.TextUtils;
import android.util.Log;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.models.Tag;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by xyz on 15/9/11.
 */
public class TagAPI implements HttpCallBack{
    private static final String TAG = "GIO.TagAPI";
    List<Tag> tags = new ArrayList<>();

    void run() {
        HttpEvent event = HttpEvent.createCircleHttpEvent(NetworkConfig.getInstance().getTargetApiEventPoint(), null, true);
        event.setCallBack(this);
        EventCenter.getInstance().post(event);
    }

    @Override
    public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
        if (responseCode == HttpURLConnection.HTTP_OK){
            if (data != null) {
                tags.clear();
                JSONArray msg = new JSONArray();
                try {
                    msg = new JSONArray(new String(data));
                } catch (JSONException e) {
                    Log.d(TAG, "generate tags error", e);
                }
                for (int i = 0; i < msg.length(); i++) {
                    try {
                        Tag tag = new Tag(msg.getJSONObject(i));
                        if (!tag.archived && Constants.PLATFORM_ANDROID.equalsIgnoreCase(tag.platform) && !TextUtils.isEmpty(tag.attrs.domain)) {
                            onReceiveTag(tag);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    void onReceiveTag(Tag tag){
        if (tag.filter != null && tag.filter.xpath != null && tag.filter.xpath.contains(",")){
            String[] xpaths = tag.filter.xpath.split(",");
            List<String> patternServerXPath = new ArrayList<>();
            for (int i = 0; i < xpaths.length; i++){
                String path = xpaths[i];
                if (path.length() != 0 && path.charAt(0) != '*' && path.contains("*")){
                    patternServerXPath.add(path);
                }
            }
            List<String> resultXPaths;
            if (!patternServerXPath.isEmpty()){
                resultXPaths = new ArrayList<>(patternServerXPath);
                for (int i = 0; i < xpaths.length; i++){
                    String xpath = xpaths[i];
                    boolean canBeReplacedByPatterServer = false;
                    for (String patternXpath: patternServerXPath){
                        if (Util.isIdentifyPatternServerXPath(patternXpath, xpath)){
                            // 此Xpath可以由PatternServer的XPath替代
                            canBeReplacedByPatterServer = true;
                            break;
                        }
                    }
                    if (!canBeReplacedByPatterServer){
                        resultXPaths.add(xpath);
                    }
                }
            }else{
                resultXPaths = Arrays.asList(xpaths);
            }

            for (String xpath: resultXPaths){
                Tag oneTag = tag.copyWithoutScreenShot();
                oneTag.filter.xpath = xpath;
                tags.add(oneTag);
            }
        }else{
            tags.add(tag);
        }
    }
}
