import com.growingio.android.sdk.utils.NetworkUtil;

import org.junit.Assert;
import org.junit.Test;

import java.net.URLEncoder;

/**
 * Created by liangdengke on 2018/4/24.
 */
public class JavaTest {

    @Test
    public void strTrim() {
        String str = "jkjkjk\r";
        Assert.assertEquals("jkjkjk", str.trim());
    }

    @Test
    public void nullEncode() {
        String encode = NetworkUtil.encode(null);
        StringBuilder builder = new StringBuilder();
        builder.append("url=").append(encode);
        System.out.println(builder.toString());
    }

    @Test
    public void scriptTest() {
        String className = "_js";
        String scriptSrc = "https://www.growingio.com";
        String js = "javascript:(function(){try{" +
                "var jsNode = document.getElementById('%s');\n" +
                "if (jsNode==null) {\n" +
                "    var p = document.createElement('script');\n" +
                "    p.src = '%s';\n" +
                "    p.id = '%s';\n" +
                "    document.head.appendChild(p);\n" +
                "}" +
                "}catch(e){}})()";
        String result = String.format(js, className, scriptSrc, className);
        System.out.println(result);
    }
}
