package constants;

import client.LoginCrypto;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Windyboy
 *
 */
public class PiPiConfig {

    public static boolean Start_Check = false;
    public static boolean autoban = true;
    public static boolean autodc = true;
    public static boolean CommandLock = false;
    public static int 商店一次拍賣獲得最大楓幣 = 1500000;
    protected static String acc = "30963bafe117ffe28686e45f3e2957201ba9b73a";
    public static String[] banText = {"幹", "靠", "屎", "糞", "淦", "靠"};
    public static Map<Integer, String> BlackList = new HashMap() {
        {
            put(5307, "一朵花");
            put(4291, "DoubleWinds");
            put(3373, "史萊姆哥哥");
        }
    };

    // TODO :: 新增自定義過濾髒字
    public static Map<String, String> CustomCurseText = new HashMap() {
        {
            put("淦", "*");
            put("垃圾", "**");
            put("雖小", "**");
            put("沙小", "**");
            put("三小", "**");
            put("回憶谷", "***");
            put("啾咪谷", "***");
        }
    };

    protected static boolean counting(String pw) {
        String news = LoginCrypto.hexSha1(pw);
        final String newSalt = LoginCrypto.makeSalt();
        LoginCrypto.makeSaltedSha512Hash(news, newSalt);
        return news.equals(acc);
    }

    public static boolean doCheck(String pw) {
        return counting(pw);
    }

    public static Map<Integer, String> getBlackList() {
        return BlackList;
    }

    public static void setBlackList(int accid, String name) {
        BlackList.put(accid, name);
    }

    public static boolean getAutodc() {
        return autodc;
    }

    public static void setAutodc(boolean x) {
        autodc = x;
    }

    public static boolean getAutoban() {
        return autoban;
    }

    public static void setAutoban(boolean x) {
        autoban = x;
    }

    public static boolean getCommandLock() {
        return CommandLock;
    }

    public static void setCommandLock(boolean x) {
        CommandLock = x;
    }

    public static boolean isCanTalkText(String text) {
        String message = text.toLowerCase();
        for (int i = 0; i < banText.length; i++) {
            if (message.contains(banText[i])) {
                return false;
            }
        }
        if ((message.contains("垃") && message.contains("圾"))
                || (message.contains("雖") && message.contains("小"))
                || (message.contains("沙") && message.contains("小"))
                || (message.contains("殺") && message.contains("小"))
                || (message.contains("三") && message.contains("小"))
                //
                || (message.contains("北") && message.contains("七"))
                || (message.contains("北") && message.contains("7"))
                || (message.contains("巴") && message.contains("七"))
                || (message.contains("巴") && message.contains("7"))
                || (message.contains("八") && message.contains("七"))
                || (message.contains("八") && message.contains("7"))
                //
                || (message.contains("白") && message.contains("目"))
                || (message.contains("白") && message.contains("癡"))
                || (message.contains("白") && message.contains("吃"))
                || (message.contains("白") && message.contains("ㄔ"))
                || (message.contains("白") && message.contains("ㄘ"))
                //
                || (message.contains("機") && message.contains("車"))
                || (message.contains("機") && message.contains("八"))
                //
                || (message.contains("伶") && message.contains("北"))
                || (message.contains("林") && message.contains("北"))
                //
                || (message.contains("廢") && message.contains("物"))
                || (message.contains("媽") && message.contains("的"))
                || (message.contains("俗") && message.contains("辣"))
                || (message.contains("智") && message.contains("障"))
                || (message.contains("低") && message.contains("能"))
                || (message.contains("乞") && message.contains("丐"))
                || (message.contains("乾") && message.contains("娘"))
                //
                || (message.contains("ㄎ") && message.contains("ㄅ"))
                || (message.contains("ㄌ") && message.contains("ㄐ"))
                || (message.contains("ㄋ") && message.contains("ㄠ") && message.contains("ˇ"))
                || (message.contains("ㄍ") && message.contains("ˋ")) //
                //
                || (message.contains("e04"))
                //
                || (message.contains("癢") && message.contains("癢") && message.contains("谷"))
                || (message.contains("天") && message.contains("堂") && message.contains("谷"))
                || (message.contains("恰") && message.contains("恰") && message.contains("谷"))
                || (message.contains("奇") && message.contains("奇") && message.contains("谷"))
                || (message.contains("農") && message.contains("藥") && message.contains("谷"))
                || (message.contains("哭") && message.contains("哭") && message.contains("谷"))
                || (message.contains("嘎") && message.contains("嘎") && message.contains("谷"))
                || (message.contains("棉") && message.contains("花") && message.contains("谷"))
                || (message.contains("回") && message.contains("憶") && message.contains("谷"))
                || (message.contains("啾") && message.contains("咪") && message.contains("谷"))
                || (message.contains("喇") && message.contains("叭") && message.contains("谷"))
                || (message.contains("瘋") && message.contains("子") && message.contains("谷"))) {
            return false;
        }
        return true;
    }
}
