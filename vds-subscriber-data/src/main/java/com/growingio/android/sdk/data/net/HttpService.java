package com.growingio.android.sdk.data.net;

import android.annotation.SuppressLint;
import android.net.TrafficStats;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.data.DiagnoseLog;
import com.growingio.android.sdk.utils.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * Author: xyz
 * <br/>
 * Date: 2015/3/31
 * <br/>
 * Description: 控制SDK中所有的网络请求
 */

public class HttpService {

    private static final String TAG = "GIO.HttpService";
    private static final String GZIP_ENCODING = "gzip";

    private static final int THREAD_STATS_TAG = "GIO_HTTP".hashCode();
    private static final AsyncTimeout sAsyncTimeout = new AsyncTimeout();

    private String mUrl;
    private String mRequestMethod;
    private Map<String, String> mHeaders;
    private byte[] mData;
    private long mIfModifiedSince;
    private long mLastModified;
    private Map<String, List<String>> mResponseHeaders;
    public static SSLSocketFactory sSystemDefaultFactory;


    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    private HttpService(String url, String requestMethod, Map<String, String> headers, byte[] data, long sinceModified) {
        mUrl = url;
        mRequestMethod = requestMethod;
        mHeaders = headers;
        mData = data;
        mIfModifiedSince = sinceModified;
    }

    /**
     * 对原本的网络请求进行了一层封装, 以方便传入 {@link DNSService.HostInformation}
     */
    @WorkerThread
    public Pair<Integer, byte[]> performRequest() {
        return performRequest(null);
    }

    /**
     * 进行网络请求
     */
    private Pair<Integer, byte[]> performRequest(DNSService.HostInformation hostInformation) {
        Pair<Integer, byte[]> ret = new Pair<Integer, byte[]>(0, new byte[0]);

        // FIXME: 16/1/6 hide retry before the retry to many times bug fixed
        OutputStream outputStream = null;
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        boolean postRequest = mRequestMethod.equals("POST");
        boolean success = false;
        String error = null;
        URL url = null;
        AsyncTimeout.Timeout totalTimeout = null;

        try {
            url = new URL(mUrl);

            if (hostInformation == null) {
                hostInformation = DNSService.getInstance().getHostInformationByHostName(url.getHost(), true);
                if (replaceHostNameWithIP(hostInformation)) {
                    url = new URL(mUrl);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
            }
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(60000);
            totalTimeout = new AsyncTimeout.Timeout(urlConnection, 65_000);
            if (mIfModifiedSince > 0) {
                urlConnection.setIfModifiedSince(mIfModifiedSince);
            }
            if (urlConnection instanceof HttpsURLConnection && sSystemDefaultFactory != null) {
                completeHttpsRequestOption((HttpsURLConnection) urlConnection, hostInformation);
            }

            //            urlConnection.setRequestProperty("Cookie", "grwng_uid=" + getAppState().getDeviceId());
            if (mHeaders != null) {
                for (String key : mHeaders.keySet()) {
                    urlConnection.setRequestProperty(key, mHeaders.get(key));
                }
            }
            if ((mData != null && mData.length > 0) || mRequestMethod.equals("POST")) {
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                outputStream = urlConnection.getOutputStream();
                // total timeout中出去connect timeout
                sAsyncTimeout.enter(totalTimeout);
                if (mData != null) {
                    outputStream.write(mData);
                }
                outputStream.flush();
                outputStream.close();
                outputStream = null;
            } else {
                urlConnection.setRequestMethod(mRequestMethod);
                urlConnection.setRequestProperty("Accept-Encoding", GZIP_ENCODING);
                sAsyncTimeout.enter(totalTimeout);
            }
            int retCode = urlConnection.getResponseCode();
            boolean isError = retCode >= 400;
            mLastModified = urlConnection.getLastModified();
            mResponseHeaders = urlConnection.getHeaderFields();
            inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
            if (GZIP_ENCODING.equalsIgnoreCase(urlConnection.getHeaderField("Content-Encoding"))) {
                inputStream = new GZIPInputStream(urlConnection.getInputStream());
            }
            ret = new Pair<Integer, byte[]>(retCode, slurp(inputStream));
            inputStream.close();
            success = ret.first == HttpURLConnection.HTTP_OK;
        } catch (Throwable e) {
            // 如果内存不存在HttpDNS解析结果且当前为DNS解析异常则请求HttpDNS
            if (url != null && hostInformation == null && e instanceof UnknownHostException) {
                hostInformation = DNSService.getInstance().getHostInformationByHostName(url.getHost(), false);
                if (replaceHostNameWithIP(hostInformation)) {
                    DiagnoseLog.saveLogIfEnabled("hd");
                    return performRequest(hostInformation);
                }
            } else {
                // 如果内存存在HttpDNS解析结果且当前通过IP地址请求无法获取正确结果则认为HttpDNS解析的IP已失效。
                if (hostInformation != null && e instanceof SocketTimeoutException) {
                    DNSService.getInstance().removeHostInformation(hostInformation);
                }
            }

            error = getExceptionMessage(e);
        } finally {
            if (totalTimeout != null) {
                sAsyncTimeout.exit(totalTimeout);
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                TrafficStats.clearThreadStatsTag();
            }
        }

        if (GConfig.DEBUG) {
            Log.w(TAG, "performRequest: code " + ret.first + " url:" + mUrl + ", error: " + error + " ,response:" + new String(ret.second));
        }

        if (postRequest && !success) {
            if (error == null) {
                error = String.valueOf(ret.first);
            }
            DiagnoseLog.saveLogIfEnabled(error);
        }
        return ret;
    }


    /**
     * 如果url的域名有对应的HttpDNS解析结果则将url中的域名替换为ip地址
     */
    private boolean replaceHostNameWithIP(DNSService.HostInformation hostInformation) {
        if (hostInformation != null) {
            String ip = hostInformation.getIp();
            // 请求HttpDNS可能出现请求成功，但是ip列表为空的情况，这个时候为避免反复进行HttpDNS冲击sever会设置ttl为一定时间,同时意味着虽然获取hostInformation,但依旧无法根据IP直接进行网络请求。
            if (!TextUtils.isEmpty(ip)) {
                mUrl = mUrl.replaceFirst(hostInformation.getHostName(), ip);
                return true;
            }
        }
        return false;
    }

    /**
     * 补全https请求中需要额外增加的选项
     */
    private void completeHttpsRequestOption(final HttpsURLConnection urlConnection, final DNSService.HostInformation hostInformation) {
        if (sSystemDefaultFactory != null) {
            urlConnection.setSSLSocketFactory(new GIOSSLSocketFilterFactory(sSystemDefaultFactory));
        }
        if (hostInformation != null) {
            final String hostName = hostInformation.getHostName();
            urlConnection.setRequestProperty("Host", hostName);
            urlConnection.setHostnameVerifier(new HostnameVerifier() {
                /**
                 * 使用HTTPDNS后URL里设置的hostname不是远程的主机名(如:m.taobao.com)，与证书颁发的域不匹配，
                 * Android HttpsURLConnection提供了回调接口让用户来处理这种定制化场景。
                 * 在确认HTTPDNS返回的源站IP与Session携带的IP信息一致后，您可以在回调方法中将待验证域名替换为原来的真实域名进行验证。
                 */
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    boolean verifySuccess = HttpsURLConnection.getDefaultHostnameVerifier().verify(hostName, session);
                    // 如果验证失效则通知DNSService出现证书验证错误
                    if (!verifySuccess) {
                        DNSService.getInstance().verifyFail(hostInformation);
                    }
                    return verifySuccess;
                }
            });
        }
    }

    /**
     * 将异常信息转为对应的后台诊断接口需要的格式并返回
     */
    private String getExceptionMessage(Throwable e) {
        String error;

        if (e instanceof UnknownHostException) {
            error = "uh";
        } else if (e instanceof SocketTimeoutException) {
            error = "timeout";
        } else if (e instanceof SSLException) {
            error = "ssl";
        } else if (e instanceof IOException) {
            error = "io";
        } else if (e instanceof ArrayIndexOutOfBoundsException) {
            error = "aioob";
            LogUtil.e(TAG, "performRequest: bad response");
        } else {
            error = "other";
            LogUtil.e(TAG, "performRequest: unknown exception");
            e.printStackTrace();
        }

        return error;
    }

    /**
     * 读取网络请求返回的数据并返回读取的数据
     */
    private static byte[] slurp(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public long getLastModified() {
        return mLastModified;
    }

    public static class Builder {
        private String mNestedUri;
        private String mNestedRequestMethod = "GET";
        private Map<String, String> mNestedHeaders = new HashMap<String, String>();
        private byte[] mNestedData = new byte[0];
        private long mSinceModified = 0;

        public Builder uri(String uri) {
            mNestedUri = uri;
            return this;
        }

        public Builder requestMethod(String requestMethod) {
            mNestedRequestMethod = requestMethod;
            return this;
        }

        public Builder ifModifiedSince(long date) {
            mSinceModified = date;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            mNestedHeaders = headers;
            return this;
        }

        public Builder body(byte[] data) {
            mNestedData = data;
            return this;
        }

        public HttpService build() {
            return new HttpService(mNestedUri, mNestedRequestMethod, mNestedHeaders, mNestedData, mSinceModified);
        }
    }

    private static class GIOSSLSocketFilterFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        private static volatile Boolean sQuestionOkHttp = null;
        private static Field sSocketField, sSocketImplField, sFdField, sDescriptorField;

        public GIOSSLSocketFilterFactory(SSLSocketFactory factory) {
            delegate = factory;
        }


        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return checkFdSetSize(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return checkFdSetSize(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return checkFdSetSize(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return checkFdSetSize(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return checkFdSetSize(delegate.createSocket(address, port, localAddress, localPort));
        }


        private Socket checkFdSetSize(Socket socketWrapper) throws GIOHttpException {
            // 见 Android: f1e55cdd8107685a8705e377e6d95859dbd28582 (直接google diff号即可),
            // Android 7.0修复
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return socketWrapper;
            }
            // 实现应该是HttpURLConnectionImpl.java
            if (sQuestionOkHttp == null) {
                synchronized (HttpService.class) {
                    if (sSocketImplField == null) {
                        try {
                            @SuppressLint("PrivateApi")
                            Class wrapperClazz = Class.forName("com.android.org.conscrypt.OpenSSLSocketImplWrapper");
                            sSocketField = wrapperClazz.getDeclaredField("socket");
                            sSocketField.setAccessible(true);

                            sSocketImplField = Socket.class.getDeclaredField("impl");
                            sSocketImplField.setAccessible(true);

                            Class socketImplClazz = Class.forName("java.net.SocketImpl");
                            sFdField = socketImplClazz.getDeclaredField("fd");
                            sFdField.setAccessible(true);

                            sDescriptorField = FileDescriptor.class.getDeclaredField("descriptor");
                            sDescriptorField.setAccessible(true);

                            sQuestionOkHttp = true;
                        } catch (Throwable t) {
                            LogUtil.e(TAG, "detect okhttp version failed: " + t.getMessage(), t);
                            sQuestionOkHttp = false;
                        }
                    }
                }
            }
            if (!sQuestionOkHttp) {
                return socketWrapper;
            }
            try {
                FileDescriptor fdObj = (FileDescriptor) sFdField.get(
                        sSocketImplField.get(sSocketField.get(socketWrapper)));
                int fd = (int) sDescriptorField.get(fdObj);
                if (fd >= 1024) {
                    throw new GIOHttpException("current https socketWrapper's fd > 1024, wrong state, throw HttpException, and fd=" + fd);
                }
            } catch (Throwable t) {
                if (t instanceof GIOHttpException) {
                    throw (GIOHttpException) t;
                }
                LogUtil.e(TAG, t.getMessage(), t);
            }
            return socketWrapper;
        }
    }

    private static class GIOHttpException extends IOException {
        GIOHttpException(String message) {
            super(message);
        }
    }
}
