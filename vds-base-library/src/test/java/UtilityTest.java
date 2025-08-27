import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewAttrs;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by lishaojie on 16/7/31.
 */

public class UtilityTest {

    @Before
    public void setup(){
        GConfig.USE_ID = true;
        GConfig.CIRCLE_USE_ID = true;
    }
    @Test
    public void xpathMatcherTest() throws Exception {
        assertTrue(Util.isIdentifyXPath("*#btn_login", "/MainWindow/FrameLayout[1]#container/RelativeLayout[0]/Button[1]#btn_login"));
        assertFalse(Util.isIdentifyXPath("*#container", "/MainWindow/FrameLayout[1]#container/RelativeLayout[0]/Button[1]#btn_login"));
        assertTrue(Util.isIdentifyXPath("*#title/TextView[1]", "/MainWindow/FitSystemWindow[0]#content/LinearLayout[1]#title/TextView[1]"));
        assertFalse(Util.isIdentifyXPath("*#title", "/MainWindow/FitSystemWindow[0]#content/LinearLayout[1]#title/TextView[1]"));
        assertTrue(Util.isIdentifyXPath("*#container::/div/div.bg/div/span.top", "/MainWindow/WebView[1]#container::/div/div.bg/div/span.top"));
        assertFalse(Util.isIdentifyXPath("*#container::/div/div.bg/div/span#title.top", "/MainWindow/WebView[1]#container::/div/div.bg/div/span.top"));
    }

    @Test
    public void instantFilterTest() throws Exception {
        ArrayList<ViewAttrs> testFilter = parseTagFilter("[{'x':'*#container'}," +
                "{'x':'*#loginBtn','v':'Login'}," +
                "{'x':'*#itemImage','idx':3}," +
                "{'x':'*#itemTitle','idx':3,'v':'Shadow'}]"
        );
        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/FrameLayout[0]/RelativeLayout[1]#container'}"), testFilter));
        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/FrameLayout[0]/RelativeLayout[1]#container','v':'some text'}"), testFilter));
        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/FrameLayout[0]/RelativeLayout[1]#container','idx':1}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/FrameLayout[0]/RelativeLayout[1]#not_container'}"), testFilter));

        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/Button[1]#loginBtn','v':'Login'}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/Button[1]#loginBtn','v':'NotLogin'}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/Button[1]#loginBtn'}"), testFilter));

        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/ImageView[3]#itemImage','idx':3}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/ImageView[3]#itemImage','idx':5}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/ImageView[3]#itemImage'}"), testFilter));

        assertTrue(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/TextView[1]#itemTitle','idx':3,'v':'Shadow'}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/TextView[1]#itemTitle','idx':1,'v':'Shadow'}"), testFilter));
        assertFalse(Util.isInstant(parseActionStruct("{'x':'/MainWindow/LinearLayout[0]/TextView[1]#itemTitle','idx':3,'v':'NotShadow'}"), testFilter));
    }

    /**
     * 用22个UUID测试采样函数的稳定性（提高采样率后被采样本状态不变）
     */
    @Test
    public void testSampling() throws Exception {
        double sampleRate = 0.03;
        assertFalse(Util.isInSampling("029ff18a-060b-4644-91e3-e7179b920ee0", sampleRate));
        assertFalse(Util.isInSampling("02b9ca85-070c-42f0-980c-ff8f5b96b0a1", sampleRate));
        assertFalse(Util.isInSampling("049a59b9-cbaf-496c-8cee-4a369af94f50", sampleRate));
        assertFalse(Util.isInSampling("064e8de6-a7b5-4b9f-a3f1-a8a3cc3809e0", sampleRate));
        assertFalse(Util.isInSampling("08d67b70-f6db-44f8-8c68-b34418aeb8a2", sampleRate));
        assertFalse(Util.isInSampling("0b83cab3-ceb5-4fff-99d3-9a7ada4964a6", sampleRate));
        assertFalse(Util.isInSampling("0eaa3811-a442-4ce1-8634-bf7eee4c1f07", sampleRate));
        assertFalse(Util.isInSampling("1007c546-ef45-482b-8de9-e961f297ccdc", sampleRate));
        assertFalse(Util.isInSampling("316ad8f7-d4ba-4356-8811-75d00b7eb344", sampleRate));
        assertFalse(Util.isInSampling("6333813f-cb38-4067-878b-12051895be7e", sampleRate));
        assertFalse(Util.isInSampling("657245d6-f770-4114-8e09-898dc9730b7e", sampleRate));
        assertFalse(Util.isInSampling("a097de49-b016-40c3-8efe-a72520e92e27", sampleRate));
        assertFalse(Util.isInSampling("b670849e-7f90-4a6a-bc9f-dd65985c1480", sampleRate));
        assertFalse(Util.isInSampling("c06f8d91-4702-492c-8d30-14605e49d559", sampleRate));
        assertFalse(Util.isInSampling("d16e23ed-2444-4f3d-a378-a1fe97529bcd", sampleRate));
        assertFalse(Util.isInSampling("e26eb028-605a-410a-baa4-fa2761d912ae", sampleRate));
        assertFalse(Util.isInSampling("f3eaf591-0fec-4439-a7c1-10bf2ce8368b", sampleRate));

        assertTrue(Util.isInSampling("1a5dbbc6-a055-4e52-8cc1-17fc8335c475", sampleRate));
        assertTrue(Util.isInSampling("461f6d87-ed9f-4147-b7ad-93e49717e0f1", sampleRate));
        assertTrue(Util.isInSampling("5b8171e0-b0e9-4e74-9028-f3898b0ecfd7", sampleRate));
        assertTrue(Util.isInSampling("78497a7c-f35f-4ac1-9290-df2f50b50bf7", sampleRate));
        assertTrue(Util.isInSampling("ada68d38-f842-4e6f-8e4c-4c94e3aa223b", sampleRate));

        sampleRate = 0.09;
        assertTrue(Util.isInSampling("1a5dbbc6-a055-4e52-8cc1-17fc8335c475", sampleRate));
        assertTrue(Util.isInSampling("461f6d87-ed9f-4147-b7ad-93e49717e0f1", sampleRate));
        assertTrue(Util.isInSampling("5b8171e0-b0e9-4e74-9028-f3898b0ecfd7", sampleRate));
        assertTrue(Util.isInSampling("78497a7c-f35f-4ac1-9290-df2f50b50bf7", sampleRate));
        assertTrue(Util.isInSampling("ada68d38-f842-4e6f-8e4c-4c94e3aa223b", sampleRate));

        boolean testBigSample = false;
        if (!testBigSample) {
            // 因为下面的测试是基于大量随机数计算，有一定几率会失败，所以加上条件判断，需要时手动执行即可
            return;
        }

        int coreCount = Runtime.getRuntime().availableProcessors();
        final CountDownLatch threadCount = new CountDownLatch(Runtime.getRuntime().availableProcessors());
        final AtomicInteger inSampleCount = new AtomicInteger(0);
        final int sampleSize = 1000 * 10000;
        final int threadSampleSize = sampleSize / coreCount;
        sampleRate = 0.2;
        while (coreCount-- > 0) {
            final double finalSampleRate = sampleRate;
            new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < threadSampleSize; i++) {
                        UUID id = UUID.randomUUID();
                        if (Util.isInSampling(id.toString(), finalSampleRate)) inSampleCount.addAndGet(1);
                    }
                    threadCount.countDown();
                }
            }.start();
        }
        threadCount.await();
        int diff = (int) Math.abs(sampleSize * sampleRate - inSampleCount.get());
        int upperLimit = (int) (sampleSize * sampleRate * 0.01);
        System.out.println("sample diff: " + diff);
        if (diff < upperLimit) {
            diff = 0;
        }
        assertEquals("In Sample Count Diff Tolerance Limit " + upperLimit, 0, diff);
    }

   /* @Test
    public void testGetHostInformationInCache() {
        DNSService dnsService = new DNSService();

        ArrayList<DNSService.HostInformation> cachedHostInformation = new ArrayList<DNSService.HostInformation>();
        DNSService.HostInformation hostInformation = new DNSService.HostInformation();
        hostInformation.setHostName("api.growingio.com");
        cachedHostInformation.add(hostInformation);
        assertNotNull(dnsService.getHostInformationInCache("api.growingio.com", cachedHostInformation));
        assertNull(dnsService.getHostInformationInCache("test.growingio.com", cachedHostInformation));

        cachedHostInformation = new ArrayList<DNSService.HostInformation>();
        hostInformation = new DNSService.HostInformation();
        hostInformation.setHostName("api.growingio.com");
        cachedHostInformation.add(hostInformation);
        hostInformation = new DNSService.HostInformation();
        hostInformation.setHostName("test.growingio.com");
        cachedHostInformation.add(hostInformation);
        assertNotNull(dnsService.getHostInformationInCache("api.growingio.com", cachedHostInformation));
        assertNotNull(dnsService.getHostInformationInCache("test.growingio.com", cachedHostInformation));

        cachedHostInformation = new ArrayList<DNSService.HostInformation>();
        assertNull(dnsService.getHostInformationInCache("api.growingio.com", cachedHostInformation));
        assertNull(dnsService.getHostInformationInCache("test.growingio.com", cachedHostInformation));
    }*/

    @Test
    public void testShouldSetLocation() {
        long fiveMinute = 5 * 60 * 1000;

        assertFalse(Util.shouldSetLocation(0.0, 0.0, 0.0, 0.0, 1, 0));
        assertFalse(Util.shouldSetLocation(0.0, 0.0, 0.0, 0.0, 1 + fiveMinute, 0));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, 0.0001, 0.0, 1 + fiveMinute, 0));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, 0.0, 0.0000001, 1 + fiveMinute, 0));

        assertFalse(Util.shouldSetLocation(0.0, 0.0, 0.04999999, 0.0, 0, 1));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, 0.05000001, 0.0, 0, 1));
        assertFalse(Util.shouldSetLocation(0.0, 0.0, 0.02499999, 0.025, 0, 1));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, 0.02500001, 0.025, 0, 1));
        assertFalse(Util.shouldSetLocation(0.0, 0.0, 0.02499999, 0.025, 0, 1));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, 0.02500001, 0.025, 0, 1));
        assertFalse(Util.shouldSetLocation(0.0, 0.0, -0.02499999, 0.025, 0, 1));
        assertTrue(Util.shouldSetLocation(0.0, 0.0, -0.02500001, 0.025, 0, 1));
        assertFalse(Util.shouldSetLocation(-0.02499999, 0.025, 0.0, 0.0, 0, 1));
        assertTrue(Util.shouldSetLocation(-0.02500001, 0.0, 0.0, 0.025, 0, 1));
    }

    private ActionStruct parseActionStruct(String json) throws JSONException {
        return parseActionStruct(new JSONObject(json));
    }

    private ActionStruct parseActionStruct(JSONObject object) {
        ActionStruct struct = new ActionStruct();
        struct.xpath = LinkedString.fromString(object.optString("x", null));
        struct.content = object.optString("v", null);
        struct.index = object.optInt("idx", -1);
        return struct;
    }

    private ViewAttrs parseViewAttrs(JSONObject object) {
        ViewAttrs attrs = new ViewAttrs();
        attrs.xpath = object.optString("x", null);
        attrs.content = object.optString("v", null);
        try {
            attrs.index = String.valueOf(object.getInt("idx"));
        } catch (JSONException e) {
        }
        return attrs;
    }

    private ArrayList<ViewAttrs> parseTagFilter(String tags) {
        ArrayList<ViewAttrs> filter = new ArrayList<ViewAttrs>();
        try {
            JSONArray array = new JSONArray(tags);
            for (int i = 0; i < array.length(); i++) {
                filter.add(parseViewAttrs(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return filter;
    }
}
