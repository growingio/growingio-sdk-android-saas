package com.growingio.android.sdk.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @see org.json.JSONStringer, 支持LinkedString
 * Created by liangdengke on 2018/12/18.
 */
public class GJSONStringer {

    /** The output data, containing at most one top-level array or object. */
    private StringBuilder out = new StringBuilder();

    /**
     * Lexical scoping elements within this stringer, necessary to insert the
     * appropriate separator characters (ie. commas and colons) and to detect
     * nesting errors.
     */
    enum Scope {

        /**
         * An array with no elements requires no separators or newlines before
         * it is closed.
         */
        EMPTY_ARRAY,

        /**
         * A array with at least one value requires a comma and newline before
         * the next element.
         */
        NONEMPTY_ARRAY,

        /**
         * An object with no keys or values requires no separators or newlines
         * before it is closed.
         */
        EMPTY_OBJECT,

        /**
         * An object whose most recent element is a key. The next element must
         * be a value.
         */
        DANGLING_KEY,

        /**
         * An object with at least one name/value pair requires a comma and
         * newline before the next element.
         */
        NONEMPTY_OBJECT,

        /**
         * A special bracketless array needed by JSONStringer.join() and
         * JSONObject.quote() only. Not used for JSON encoding.
         */
        NULL,
    }

    /**
     * Unlike the original implementation, this stack isn't limited to 20
     * levels of nesting.
     */
    private final List<Scope> stack = new ArrayList<Scope>();


    public String convertToString(JSONObject jsonObject) {
        out.setLength(0);
        stack.clear();
        try {
            writeToThis(jsonObject);
        } catch (JSONException e) {
            LogUtil.d("GIO.JSON", e.getMessage(), e);
        }
        return out.toString();
    }

    public String convertToString(JSONArray jsonArray) {
        out.setLength(0);
        stack.clear();
        try {
            writeToThis(jsonArray);
        } catch (JSONException e) {
            LogUtil.d("GIO.JSON", e.getMessage(), e);
        }
        return out.toString();
    }

    public void closeBuffer(){
        out = new StringBuilder();
        stack.clear();
    }

    private void writeToThis(JSONObject jsonObject) throws JSONException {
        object();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()){
            String key = keys.next();
            Object value = jsonObject.get(key);
            key(key).value(value);
        }
        endObject();
    }

    private void writeToThis(JSONArray jsonArray) throws JSONException {
        array();
        for (int i = 0; i < jsonArray.length(); i++){
            value(jsonArray.get(i));
        }
        endArray();
    }

    /**
     * Begins encoding a new array. Each call to this method must be paired with
     * a call to {@link #endArray}.
     *
     * @return this stringer.
     */
    private GJSONStringer array() throws JSONException {
        return open(Scope.EMPTY_ARRAY, "[");
    }

    /**
     * Ends encoding the current array.
     *
     * @return this stringer.
     */
    private GJSONStringer endArray() throws JSONException {
        return close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
    }

    /**
     * Begins encoding a new object. Each call to this method must be paired
     * with a call to {@link #endObject}.
     *
     * @return this stringer.
     */
    public GJSONStringer object() throws JSONException {
        return open(Scope.EMPTY_OBJECT, "{");
    }

    /**
     * Ends encoding the current object.
     *
     * @return this stringer.
     */
    private GJSONStringer endObject() throws JSONException {
        return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
    }

    /**
     * Enters a new scope by appending any necessary whitespace and the given
     * bracket.
     */
    GJSONStringer open(Scope empty, String openBracket) throws JSONException {
        if (stack.isEmpty() && out.length() > 0) {
            throw new JSONException("Nesting problem: multiple top-level roots");
        }
        beforeValue();
        stack.add(empty);
        out.append(openBracket);
        return this;
    }

    /**
     * Closes the current scope by appending any necessary whitespace and the
     * given bracket.
     */
    GJSONStringer close(Scope empty, Scope nonempty, String closeBracket) throws JSONException {
        Scope context = peek();
        if (context != nonempty && context != empty) {
            throw new JSONException("Nesting problem");
        }

        stack.remove(stack.size() - 1);
        out.append(closeBracket);
        return this;
    }

    /**
     * Returns the value on the top of the stack.
     */
    private Scope peek() throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        return stack.get(stack.size() - 1);
    }

    /**
     * Replace the value on the top of the stack with the given value.
     */
    private void replaceTop(Scope topOfStack) {
        stack.set(stack.size() - 1, topOfStack);
    }

    /**
     * Encodes {@code value}.
     *
     * @param value a {@link JSONObject}, {@link JSONArray}, String, Boolean,
     *     Integer, Long, Double or null. May not be {@link Double#isNaN() NaNs}
     *     or {@link Double#isInfinite() infinities}.
     * @return this stringer.
     */
    private GJSONStringer value(Object value) throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }

        if (value instanceof JSONArray) {
            writeToThis((JSONArray) value);
            return this;

        } else if (value instanceof JSONObject) {
            writeToThis((JSONObject) value);
            return this;
        }

        beforeValue();

        if (value == null
                || value instanceof Boolean
                || value == JSONObject.NULL) {
            out.append(value);
        } else if (value instanceof Number) {
            out.append(JSONObject.numberToString((Number) value));
        }else if (value instanceof LinkedString){
            stringLinkedString((LinkedString) value);
        }else {
            string(out, value.toString());
        }

        return this;
    }

    /**
     * Encodes {@code value} to this stringer.
     *
     * @return this stringer.
     */
    public GJSONStringer value(boolean value) throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        out.append(value);
        return this;
    }

    /**
     * Encodes {@code value} to this stringer.
     *
     * @param value a finite value. May not be {@link Double#isNaN() NaNs} or
     *     {@link Double#isInfinite() infinities}.
     * @return this stringer.
     */
    public GJSONStringer value(double value) throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        out.append(JSONObject.numberToString(value));
        return this;
    }

    /**
     * Encodes {@code value} to this stringer.
     *
     * @return this stringer.
     */
    public GJSONStringer value(long value) throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        out.append(value);
        return this;
    }

    private void stringLinkedString(LinkedString linkedString){
        out.append("\"");
        LinkedString.LinkedStringIterator iterator = linkedString.iterator();
        while (iterator.hasNext()){
            char next = iterator.next();
            handleChar(out, next);
        }
        out.append("\"");
    }

    private static void string(StringBuilder out, String value) {
        out.append("\"");
        stringWithoutQuotation(out, value);
        out.append("\"");
    }

    public static void stringWithoutQuotation(StringBuilder out, String value){
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            handleChar(out, c);
        }
    }

    private static void handleChar(StringBuilder out, char ch){
        /*
         * From RFC 4627, "All Unicode characters may be placed within the
         * quotation marks except for the characters that must be escaped:
         * quotation mark, reverse solidus, and the control characters
         * (U+0000 through U+001F)."
         */
        switch (ch) {
            case '"':
            case '\\':
            case '/':
                out.append('\\').append(ch);
                break;

            case '\t':
                out.append("\\t");
                break;

            case '\b':
                out.append("\\b");
                break;

            case '\n':
                out.append("\\n");
                break;

            case '\r':
                out.append("\\r");
                break;

            case '\f':
                out.append("\\f");
                break;

            default:
                if (ch <= 0x1F) {
                    out.append(String.format("\\u%04x", (int) ch));
                } else {
                    out.append(ch);
                }
                break;
        }
    }


    /**
     * Encodes the key (property name) to this stringer.
     *
     * @param name the name of the forthcoming value. May not be null.
     * @return this stringer.
     */
    public GJSONStringer key(String name) throws JSONException {
        if (name == null) {
            throw new JSONException("Names must be non-null");
        }
        beforeKey();
        string(out, name);
        return this;
    }

    /**
     * Inserts any necessary separators and whitespace before a name. Also
     * adjusts the stack to expect the key's value.
     */
    private void beforeKey() throws JSONException {
        Scope context = peek();
        if (context == Scope.NONEMPTY_OBJECT) { // first in object
            out.append(',');
        } else if (context != Scope.EMPTY_OBJECT) { // not in an object!
            throw new JSONException("Nesting problem");
        }
        replaceTop(Scope.DANGLING_KEY);
    }

    /**
     * Inserts any necessary separators and whitespace before a literal value,
     * inline array, or inline object. Also adjusts the stack to expect either a
     * closing bracket or another element.
     */
    private void beforeValue() throws JSONException {
        if (stack.isEmpty()) {
            return;
        }

        Scope context = peek();
        if (context == Scope.EMPTY_ARRAY) { // first in array
            replaceTop(Scope.NONEMPTY_ARRAY);
        } else if (context == Scope.NONEMPTY_ARRAY) { // another in array
            out.append(',');
        } else if (context == Scope.DANGLING_KEY) { // value for key
            out.append(":");
            replaceTop(Scope.NONEMPTY_OBJECT);
        } else if (context != Scope.NULL) {
            throw new JSONException("Nesting problem");
        }
    }

    /**
     * Returns the encoded JSON string.
     *
     * <p>If invoked with unterminated arrays or unclosed objects, this method's
     * return value is undefined.
     *
     * <p><strong>Warning:</strong> although it contradicts the general contract
     * of {@link Object#toString}, this method returns null if the stringer
     * contains no data.
     */
    @Override public String toString() {
        return out.length() == 0 ? null : out.toString();
    }
}
