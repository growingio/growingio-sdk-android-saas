package com.growingio.android.sdk.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by liangdengke on 2018/6/26.
 */
public class JsonUtilTest {

    @Test
    public void copyJson() throws JSONException {
        String jsonValue = "{\"String\":\"String\",\"Int\":10,\"Double\":0.333,\"Array\":[1,2,3,4],\"Object\":{\"Obj1\":1,\"Obj2\":\"String\"}}";
        JSONObject jsonObject = new JSONObject(jsonValue);
        JSONObject copy = JsonUtil.copyJson(jsonObject, true);
        assertNotSame(copy, jsonObject);
        assertEquals("String", copy.getString("String"));
        assertEquals(10, copy.getInt("Int"));
        assertEquals(0.333, copy.getDouble("Double"), 0.00001);
        assertTrue(copy.get("Array") instanceof JSONArray);
        assertTrue(copy.get("Object") instanceof JSONObject);
        System.out.println("The copy Json: " + copy.toString());
    }

    @Test
    public void testJsonEqual() throws Exception{
        JSONObject equal1 = new JSONObject();
        assertTrue(JsonUtil.equal(equal1, equal1));
        assertTrue(JsonUtil.equal((JSONObject) null, null));
        assertFalse(JsonUtil.equal(null,equal1));

        equal1.put("name1", "name2");
        equal1.put("nested", equal1);
        assertTrue(JsonUtil.equal(equal1, equal1));

        JSONObject notEql = JsonUtil.copyJson(equal1, true);
        assertTrue(JsonUtil.equal(equal1, notEql));
        notEql.put("name1", "not same");
        assertFalse(JsonUtil.equal(equal1, notEql));

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("a");
        jsonArray.put("c");
        jsonArray.put("b");
        jsonArray.put(new JSONObject());
        jsonObject.put("array", jsonArray);

        JSONObject jsonObject2 = new JSONObject();
        JSONArray jsonArray2 = new JSONArray();
        jsonArray2.put("a");
        jsonArray2.put("c");
        jsonArray2.put("b");
        jsonArray2.put(new JSONObject());
        jsonObject2.put("array", jsonArray2);

        assertTrue(JsonUtil.equal(jsonObject, jsonObject2));
        jsonArray2.put("c");
        assertFalse(JsonUtil.equal(jsonObject, jsonObject2));
    }
}