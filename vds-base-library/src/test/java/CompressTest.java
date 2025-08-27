import com.growingio.android.sdk.snappy.Snappy;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by lishaojie on 05/01/2017.
 */

public class CompressTest {
    @Test
    public void testSnappy() throws Exception {
        String text = "{'s':'d2d555ae-afa1-4815-bfd8-a6f8d3931bc8','tm':1483529031022,'d':'com.zoharo.xiangzhu','p':'AccompanyLookHouseActivity','t':'imp','ptm':1483528973963,'e':[{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/View[2]#alpha','tm':1483529031023},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/RelativeLayout[0]/AppCompatImageView[0]#iv_menu_head','tm':1483529031023},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/RelativeLayout[0]/AppCompatTextView[0]','tm':1483529031023,'v':'选择套餐'},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/RelativeLayout[0]/AppCompatTextView[1]#tv_price','tm':1483529031023,'v':'免费'},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/ScrollView[1]#sl_combo/RadioGroup[0]#ll_combo','tm':1483529031023},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/LinearLayout[2]/AppCompatTextView[0]','tm':1483529031023,'v':'选择时间'},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/LinearLayout[2]/AppCompatTextView[1]','tm':1483529031023,'v':'（为保证服务质量，仅可完成7日内的预约）'},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/RadioGroup[3]#rg_week','tm':1483529031023,'v':'5\\n01月'},{'x':'/MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/FrameLayout[0]/LinearLayout[0]#ll_root/RelativeLayout[1]/LinearLayout[4]#ll_menu/LinearLayout[1]/AppCompatTextView[4]#tv_determine','tm':1483529031023,'v':'确  定'}]}";
        byte[] compressed = Snappy.compress(text.getBytes());
        assertEquals(679, compressed.length);
        assertEquals(text, new String(org.iq80.snappy.Snappy.uncompress(compressed, 0, compressed.length)));
    }
}
