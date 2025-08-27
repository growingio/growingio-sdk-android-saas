package com.growingio.android.sdk.data;

import android.content.Context;
import android.util.Pair;

import com.growingio.android.sdk.base.event.DBInitDiagnose;
import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.base.event.OnCloseBufferEvent;
import com.growingio.android.sdk.base.event.SocketEvent;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.data.db.DBAdapter;
import com.growingio.android.sdk.data.db.MessageUploader;
import com.growingio.android.sdk.data.net.HttpService;
import com.growingio.android.sdk.message.HandleType;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.models.ViewAttrs;
import com.growingio.android.sdk.models.WebEvent;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.GJSONStringer;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.Util;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.ThreadMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * author CliffLeopard
 * time   2018/7/5:下午3:13
 * email  gaoguanling@growingio.com
 */
public class DataSubscriber {

    private final String TAG = "GIO.DataSubscriber";
    private MessageUploader mMessageUploader;
    private Context context;
    private GConfig config;
    private CoreAppState coreAppState;

    private GJSONStringer jsonStringer;


    public DataSubscriber(Context context, GConfig config, CoreAppState coreAppState,
                          DeviceUUIDFactory deviceUUIDFactory, MessageUploader messageUploader) {
        this.context = context;
        this.config = config;
        this.coreAppState = coreAppState;
        this.mMessageUploader = messageUploader;
        jsonStringer = new GJSONStringer();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public synchronized void onDBEvent(DBInitDiagnose dbEvent) {
        DiagnoseLog.initialize(context);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public synchronized void onGIOEvent(VPAEvent event){
        saveMessage(event);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public synchronized void onCloseBuffer(OnCloseBufferEvent event){
        LogUtil.d(TAG, "receive close buffer event, and close buffer");
        jsonStringer.closeBuffer();
    }

    private void saveMessage(VPAEvent vpaEvent) {
        if (!GConfig.sCanHook || !config.isEnabled()) {
            return;
        }
        try {
            boolean instantEvent = !(vpaEvent instanceof ActionEvent) || ((ActionEvent) vpaEvent).isInstant();
            if (vpaEvent instanceof WebEvent) {
                instantEvent &= !(((WebEvent) vpaEvent).toJson().getString("t").equals(ActionEvent.IMP_TYPE_NAME));
                if (!instantEvent && !config.shouldSendImp()) {
                    return;
                }
            }
            DBAdapter dbAdapter = DBAdapter.getsInstance();
            if (dbAdapter == null) {
                DBAdapter.initialize(coreAppState.getGlobalContext());
                dbAdapter = DBAdapter.getsInstance();
            }
            String eventType;
            if (!instantEvent && !config.isCellularImpDisabled() && !config.isThrottled()) {
                instantEvent = true;
            }
            vpaEvent.backgroundWorker();
            if (!instantEvent && config.prepareInstantFilters()) {
                eventType = ActionEvent.IMP_TYPE_NAME; // Only imp events can be non-instant
                Pair<String, String> partEvent = extractInstantEvent(vpaEvent);
                if (partEvent.first != null) {
                    instantEvent = true;
                    dbAdapter.saveEvent(eventType, !config.isCellularImpDisabled(), partEvent.first);
                }
                if (partEvent.second != null && !config.isThrottled()) {
                    dbAdapter.saveEvent(eventType, false, partEvent.second);
                }
            } else {
                JSONObject jsonObject = vpaEvent.toJson();
                eventType = vpaEvent.getType();
                try {
                    patchEsid(vpaEvent, jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dbAdapter.saveEvent(vpaEvent.getType(), instantEvent, jsonStringer.convertToString(jsonObject));
            }
            MessageHandler.handleMessage(HandleType.SAVE_EVENT, vpaEvent);
            if (PendingStatus.isDebuggerEnabled() && !ActionEvent.IMP_TYPE_NAME.equals(eventType)){
                SocketEvent event = new SocketEvent(SocketEvent.EVENT_TYPE.SEND_DEBUGGER, vpaEvent.toJson());
                EventCenter.getInstance().post(event);
            }
            mMessageUploader.newEventSaved(instantEvent, vpaEvent.size());
        } catch (Throwable e) {
            DiagnoseLog.saveLogIfEnabled(e.getClass().getSimpleName());
            LogUtil.d(TAG, e);
        }
    }

    private void patchEsid(VPAEvent vpaEvent, JSONObject jsonObject) {
        int eventSize = vpaEvent.size();
        try {
            if (!vpaEvent.getType().equals(ActionEvent.IMP_TYPE_NAME)) {
                Pair<Integer, Integer> sid = config.getAndAddEsid(vpaEvent.getType(), eventSize);
                if (vpaEvent instanceof ActionEvent) {
                    JSONArray e = jsonObject.getJSONArray("e");
                    for (int i = 0, len = e.length(); i < len; i++) {
                        JSONObject elem = e.getJSONObject(i);
                        elem.put(VPAEvent.GLOBAL_EVENT_SEQUENCE_ID, sid.first + i);
                        elem.put(VPAEvent.EACH_TYPE_EVENT_SEQUENCE_ID, sid.second + i);
                    }
                } else {
                    jsonObject.put(VPAEvent.GLOBAL_EVENT_SEQUENCE_ID, sid.first);
                    jsonObject.put(VPAEvent.EACH_TYPE_EVENT_SEQUENCE_ID, sid.second);
                }
            }
        } catch (JSONException e) {
            LogUtil.d(TAG, e);
        }
    }

    private Pair<String, String> extractInstantEvent(VPAEvent event) {
        HashMap<String, ArrayList<ViewAttrs>> filters = config.getInstantFilters();
        ArrayList<ViewAttrs> globalFilter = filters.get(null);
        if (event instanceof ActionEvent) {
            ArrayList<ViewAttrs> pageFilter = filters.get(event.mPageName);
            int filterCount = (pageFilter != null ? pageFilter.size() : 0) + (globalFilter != null ? globalFilter.size() : 0);
            if (filterCount == 0) {
                return new Pair<>(null, jsonStringer.convertToString(event.toJson()));
            }
            ActionEvent instant = ((ActionEvent) event).copyWithoutElements();
            ActionEvent nonInstant = ((ActionEvent) event).copyWithoutElements();
            for (ActionStruct elem : ((ActionEvent) event).elems) {
                if ((globalFilter != null && Util.isInstant(elem, globalFilter))
                        || (pageFilter != null && Util.isInstant(elem, pageFilter))) {
                    instant.elems.add(elem);
                } else {
                    nonInstant.elems.add(elem);
                }
            }
            return new Pair<String, String>(instant.size() > 0 ? jsonStringer.convertToString(instant.toJson()) : null, nonInstant.size() > 0 ? jsonStringer.convertToString(nonInstant.toJson()) : null);
        } else if (event instanceof WebEvent) {
            WebEvent webEvent = (WebEvent) event;
            JSONObject webObject = webEvent.toJson();
            try {
                String domain = webObject.getString("d");
                String page = webObject.getString("p");
                ArrayList<ViewAttrs> pageFilter = filters.get(page);
                ArrayList<ViewAttrs> wildcardPageFilter = filters.get(event.mPageName + Constants.WEB_PART_SEPARATOR + '*');
                int filterCount = (pageFilter != null ? pageFilter.size() : 0)
                        + (globalFilter != null ? globalFilter.size() : 0)
                        + (wildcardPageFilter != null ? wildcardPageFilter.size() : 0);
                if (filterCount == 0) {
                    return new Pair<String, String>(null, jsonStringer.convertToString(event.toJson()));
                }
                JSONArray elems = webObject.getJSONArray("e");
                JSONArray instant = new JSONArray();
                JSONArray nonInstant = new JSONArray();
                int elemCount = elems.length();
                for (int i = 0; i < elemCount; i++) {
                    JSONObject elem = elems.getJSONObject(i);
                    if ((globalFilter != null && Util.isInstant(elem, globalFilter, domain))
                            || (wildcardPageFilter != null && Util.isInstant(elem, wildcardPageFilter, domain))
                            || (pageFilter != null && Util.isInstant(elem, pageFilter, domain))) {
                        instant.put(elem);
                    } else {
                        nonInstant.put(elem);
                    }
                }
                String instantEvents = instant.length() > 0 ? jsonStringer.convertToString(webObject.put("e", instant)) : null;
                String nonInstantEvents = nonInstant.length() > 0 ? jsonStringer.convertToString(webObject.put("e", nonInstant)) : null;
                return new Pair<String, String>(instantEvents, nonInstantEvents);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new Pair<String, String>(null, null);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onHttpEvent(HttpEvent httpEvent) {
        HttpService.Builder builder = new HttpService.Builder();
        HttpService service = builder.body(httpEvent.getData())
                .headers(httpEvent.getHeaders())
                .ifModifiedSince(httpEvent.getmSinceModified())
                .uri(httpEvent.getUrl())
                .requestMethod(httpEvent.getRequestMethod() == HttpEvent.REQUEST_METHOD.POST ? "POST" : "GET")
                .build();
        Pair<Integer, byte[]> result = service.performRequest();
        HttpCallBack callBack = httpEvent.getCallBack();
        if (callBack != null) {
            callBack.afterRequest(result.first, result.second, service.getLastModified(), service.getResponseHeaders());
        }
    }
}
