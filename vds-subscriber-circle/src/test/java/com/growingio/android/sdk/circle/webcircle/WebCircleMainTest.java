package com.growingio.android.sdk.circle.webcircle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangdengke on 2018/11/27.
 */
public class WebCircleMainTest {

    @Test
    public void testTransformCoordinates_Normal() throws Exception{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("x", 10);
        jsonObject.put("y", 11);
        WebCircleMain.transformCoordinates(jsonObject, 10, 9, 1.0);
        assertEquals(0, jsonObject.getInt("x"));
        assertEquals(2, jsonObject.getInt("y"));
    }

    @Test
    public void testTransfromCoordinates_InnerJsonObject() throws Exception{
        JSONObject outerJson = new JSONObject();
        outerJson.put("test", "value");
        JSONObject innerJson = new JSONObject();
        innerJson.put("x", 10);
        innerJson.put("y", 11);
        outerJson.put("inner", innerJson);
        WebCircleMain.transformCoordinates(outerJson, 10, 9, 1.0);
        assertEquals(0, innerJson.getInt("x"));
        assertEquals(2, innerJson.getInt("y"));
    }

    @Test
    public void testScaleFromWeb() throws Exception{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("x", 10);
        jsonObject.put("y", 20);
        jsonObject.put("ew", 40);
        jsonObject.put("eh", 30);
        WebCircleMain.transformCoordinates(jsonObject, 10, 5, 0.5);
        assertEquals(10, jsonObject.get("x"));
        assertEquals(35, jsonObject.get("y"));
        assertEquals(80, jsonObject.get("ew"));
        assertEquals(60, jsonObject.get("eh"));
    }

    @Test
    public void testTransformCoordinates_WithJsonArray() throws Exception{
        JSONObject outerJSON = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray innerJsonArray = new JSONArray();
        JSONObject innerJSON = new JSONObject();
        innerJSON.put("x", 10);
        innerJSON.put("y", 11);
        innerJsonArray.put(innerJSON);
        jsonArray.put(innerJsonArray);
        outerJSON.put("test", jsonArray);
        WebCircleMain.transformCoordinates(outerJSON, 10, 9, 1.0);
        assertEquals(0, innerJSON.getInt("x"));
        assertEquals(2, innerJSON.getInt("y"));
    }

}