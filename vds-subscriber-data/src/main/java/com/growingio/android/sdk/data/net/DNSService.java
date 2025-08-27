package com.growingio.android.sdk.data.net;

import android.content.Context;
import android.os.Build;
import android.support.annotation.WorkerThread;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.PersistUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Author: tongyuzheng
 * <br/>
 * Date: 2017/2/16
 * <br/>
 * Description: 用于对需要进行HttpDNS的域名进行解析
 */

public class DNSService {
    private static final String TAG = "GIO.DNSService";
    private static final int MAX_VERIFY_FAIL_NUMBER = 3;
    private static final long EMPTY_RESULT_HOST_TTL = 60;


    private boolean enable;
    private int verifyFailNumber;

    private final HttpDNSTask httpDNSTask;
    private final ArrayList<HostInformation> cachedHostInformation;

    private static final Object instanceLocker = new Object();

    private static DNSService instance;

    public DNSService() {
        enable = true;
        verifyFailNumber = 0;
        cachedHostInformation = new ArrayList<HostInformation>();
        httpDNSTask = new HttpDNSTask();
    }

    public static DNSService getInstance() {
        synchronized (instanceLocker) {
            if (instance == null) {
                instance = new DNSService();
                instance.initCachedHostInformation();
            }
            return instance;
        }
    }

    /**
     * 读取存储的HostInformation, 用于处理同一设备应用被多次打开和关闭导致HttpDNS接口被频繁请求的情况, 暂时不支持多个集成SDK的应用共享一份存储的HostInformation, 因为需要额外申请写外文件权限。
     */
    private void initCachedHostInformation() {
        String hostInformationData = PersistUtil.fetchHostInformationData();

        if (hostInformationData != null) {
            try {
                JSONArray jsonArray = new JSONArray(hostInformationData);
                int jsonArrayLength = jsonArray.length();
                HostInformation hostInformation;

                for (int i = 0; i < jsonArrayLength; i++) {
                    hostInformation = new HostInformation();
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    hostInformation.ttl = jsonObject.getLong("ttl");
                    hostInformation.queryTime = jsonObject.getLong("queryTime");
                    hostInformation.hostName = jsonObject.getString("hostName");
                    hostInformation.ip = jsonObject.getString("ip");
                    cachedHostInformation.add(hostInformation);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 当cachedHostInformation中的数据出现变动时更新存储的HostInformation
     */
    private void updateSavedHostInformation() {
        JSONArray jsonArray = new JSONArray();
        try {
            for (HostInformation hostInformation : cachedHostInformation) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ttl", hostInformation.ttl);
                jsonObject.put("queryTime", hostInformation.queryTime);
                jsonObject.put("hostName", hostInformation.hostName);
                jsonObject.put("ip", hostInformation.ip);
                jsonArray.put(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String hostInformationData = jsonArray.toString();
        PersistUtil.saveHostInformationData(hostInformationData);
    }

    /**
     * 检测系统是否已经设置代理，针对于设置代理的处理情况请参考: https://help.aliyun.com/document_detail/30139.html#代理情况下的使用
     */
    private boolean detectIfProxyExist() {
        String proxyHost = null;
        int proxyPort = -1;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                proxyHost = System.getProperty("http.proxyHost");
                String port = System.getProperty("http.proxyPort");
                proxyPort = Integer.parseInt(port != null ? port : "-1");
            } else {
                Context context = CoreInitialize.coreAppState().getGlobalContext();
                proxyHost = android.net.Proxy.getHost(context);
                proxyPort = android.net.Proxy.getPort(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return proxyHost != null && proxyPort != -1;
    }

    /**
     * 用于判定域名应该进行HttpDNS解析
     *
     * return : true 需要进行 DNS 解析； false 不需要
     */
    public boolean shouldHttpDNS(String hostName) {
        String removeHttpsEndPoint = NetworkConfig.getInstance().apiEndPoint().substring(Constants.HTTP_PROTOCOL_PREFIX.length());
        return removeHttpsEndPoint.indexOf(hostName) == 0;
    }

    /**
     * HttpDNS获取的HostInformation会被缓存, 此方法根据hostName查看是否有对应缓存的HostInformation, 如果有则返回对应的HostInformation
     */
    public HostInformation getHostInformationInCache(String hostName, ArrayList<HostInformation> cachedHostInformation) {
        for (HostInformation hostInformation : cachedHostInformation) {
            if (hostInformation.hostName.equals(hostName)) {
                return hostInformation;
            }
        }
        return null;
    }

    /**
     * 通过hostName获取 {@link HostInformation}
     */
    @WorkerThread
    HostInformation getHostInformationByHostName(String hostName, boolean onlyInCache) {
        if (!enable) {
            LogUtil.e(TAG, "DNSService disable");
            return null;
        }
        if (detectIfProxyExist()) {
            LogUtil.d(TAG, "ProxyExist");
            return null;
        }
        if (!shouldHttpDNS(hostName)) {
            return null;
        }
        HostInformation hostInformation = getHostInformationInCache(hostName, cachedHostInformation);
        if (hostInformation != null && !hostInformation.isExpired()) {
            LogUtil.d(TAG, "Available hostInformation: ", hostInformation);
            return hostInformation;
        }
        if (GConfig.ISOP()) {
            return null;
        } else {
            removeHostInformation(hostInformation);
            if (onlyInCache) {
                LogUtil.d(TAG, "onlyInCache");
                return null;
            }
            httpDNSTask.resetQueryHost(hostName);
            return httpDNSTask.query();
        }
    }

    /**
     * 如果设备通过IP进行Https验证失败超过三次,则认为硬件或软件设置导致不支持通过IP进行Https验证,为避免无必要的HttpDNS请求,当前进程禁止主动请求HttpDNS
     */
    void verifyFail(HostInformation hostInformation) {
        removeHostInformation(hostInformation);
        if (++verifyFailNumber >= MAX_VERIFY_FAIL_NUMBER) {
            enable = false;
        }
    }

    /**
     * 移除对应的缓存的HostInformation
     */
    void removeHostInformation(HostInformation hostInformation) {
        if (hostInformation == null) {
            return;
        }
        cachedHostInformation.remove(hostInformation);
        updateSavedHostInformation();
    }

    /**
     * 将请求获取的HostInformation放置缓存, 并更新本地存储
     */
    private void addHostInformation(HostInformation hostInformation) {
        cachedHostInformation.add(hostInformation);
        updateSavedHostInformation();
    }

    /**
     * Author: tongyuzheng
     * <br/>
     * Date: 2017/2/21
     * <br/>
     * Description: 实际进行HttpDNS请求的类
     */

    private class HttpDNSTask {
        private String hostName;
        private int retryNumber;
        private final static int MAX_RETRY_NUMBER = 1;
        // SDK指定默认的TTL时间为一天
        private static final long DEFAULT_DNS_TTL = 86400;
        private static final String HTTP_DNS_SERVER_IP = "203.107.1.1";
        private static final String ACCOUNT_ID = "144428";

        void resetQueryHost(String hostToQuery) {
            this.hostName = hostToQuery;
            this.retryNumber = MAX_RETRY_NUMBER;
        }

        public HostInformation query() {
            String queryUrl = Constants.HTTPS_PROTOCOL_PREFIX + HTTP_DNS_SERVER_IP + "/" + ACCOUNT_ID + "/d?host=" + hostName;
            InputStream inputStream = null;
            HttpURLConnection urlConnection = null;

            LogUtil.d(TAG, "HttpDNS queryUrl: ", queryUrl);

            try {
                urlConnection = (HttpURLConnection) new URL(queryUrl).openConnection();
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                int retCode = urlConnection.getResponseCode();
                if (retCode == HttpURLConnection.HTTP_OK) {
                    inputStream = urlConnection.getInputStream();
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = streamReader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    String host = json.getString("host");
                    JSONArray ips = json.getJSONArray("ips");
                    if (host != null && host.equals(hostName)) {
                        long ttl = DEFAULT_DNS_TTL;
                        String ip = (ips == null) ? null : ips.getString(0);
                        if (ip == null) {
                            // 如果请求HttpDNS有结果返回，但是ip列表为空，那默认没有ip就是解析结果，并设置ttl为一定时间,避免反复进行HttpDNS冲击sever
                            ttl = EMPTY_RESULT_HOST_TTL;
                        }
                        HostInformation hostInformation = new HostInformation();
                        hostInformation.setHostName(hostName);
                        hostInformation.setTtl(ttl);
                        hostInformation.setIp(ip);
                        addHostInformation(hostInformation);
                        return hostInformation;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            if (retryNumber > 0) {
                retryNumber--;
                return query();
            }

            return null;
        }
    }


    /**
     * Author: tongyuzheng
     * <br/>
     * Date: 2017/2/20
     * <br/>
     * Description: 存储HttpDNS解析后的数据
     */
    public static class HostInformation {
        private long ttl;
        private long queryTime;
        private String hostName;
        private String ip;

        public HostInformation() {
            this.queryTime = System.currentTimeMillis() / 1000;
        }

        boolean isExpired() {
            return queryTime + ttl < System.currentTimeMillis() / 1000;
        }

        void setTtl(long ttl) {
            this.ttl = ttl;
        }

        String getIp() {
            return ip;
        }

        void setIp(String ip) {
            this.ip = ip;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        String getHostName() {
            return hostName;
        }

        @Override
        public String toString() {
            return "HostInformation [hostName=" + hostName + ", ip=" + ip + ", ttl=" + ttl + ", queryTime=" + queryTime + "]";
        }
    }
}
