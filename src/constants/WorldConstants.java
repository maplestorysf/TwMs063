package constants;

import handling.login.LoginServer;
import java.util.LinkedList;
import java.util.List;
import server.ServerProperties;
import tools.Pair;
import tools.packet.LoginPacket.Server;

/**
 *
 * @author Eric
 *
 * Global World Properties.
 */
public class WorldConstants {

    // 通用設定 : 處理 世界,頻道 的數量 伺服器總人數, 各世界帳號的角色數量
    public static final int defaultserver = Server.雪吉拉.getId(); //預設世界（供NPC腳本使用﹚
    public static int Worlds = 2; // 最大 : 17
    public static int Channels = 5; //各世界頻道總數(最大值為20)
    public static int UserLimit = 1500; //伺服器總人數限制(1M最大可負載約30人,依實際情形增減)
    public static int maxCharacters = 6; //各世界帳號的人物角色數目限制
    public static String SCROLL_MESSAGE = "";
    public static int GLOBAL_FLAG = 1; // 伺服器狀態

    public static String GLOBAL_EVENT_MSGS = "";
    public static int GLOBAL_EXP_RATE = 1; //經驗,預設:1
    public static int GLOBAL_MESO_RATE = 1; //金錢,預設:1
    public static int GLOBAL_DROP_RATE = 1; //掉寶,預設:1
    public static boolean GLOBAL_CAN_CREATE = false; // 創建角色,預設:false

    // 世界通用設定
    public static boolean USE_GLOBAL_CHANNEL_COUNT = true;
    public static boolean USE_GLOBAL_EVENT_MSG = true;
    public static boolean USE_GLOBAL_RATES = true;
    public static boolean USE_GLOBAL_FLAG = true;

    public static boolean ADMIN_ONLY = false;
    public static boolean GMITEMS = false;
    public static boolean CS_ENABLE = true;

    public static boolean isShutDown = false;
    public static boolean Disable_Shop = false;

    public static List<Pair<Integer, Integer>> flags = new LinkedList<>();
    public static List<Pair<Integer, Integer>> chAmounts = new LinkedList<>();
    public static List<Pair<Integer, Integer>> expRates = new LinkedList<>();
    public static List<Pair<Integer, Integer>> mesoRates = new LinkedList<>();
    public static List<Pair<Integer, Integer>> dropRates = new LinkedList<>();
    public static List<Pair<Integer, String>> eventMessages = new LinkedList<>();
    public static List<Pair<Integer, Boolean>> canCreates = new LinkedList<>();

    private static void loadWorldsSettings(List<Pair<Integer, Integer>> list, boolean global, String arg, int def) {

        list.add(new Pair<>(Server.雪吉拉.getId(), (global ? def : ServerProperties.getWorldProperty(arg, def))));
        list.add(new Pair<>(Server.菇菇寶貝.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "2", def))));
        list.add(new Pair<>(Server.星光精靈.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "3", def))));
        list.add(new Pair<>(Server.緞帶肥肥.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "4", def))));
        list.add(new Pair<>(Server.藍寶.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "5", def))));
        list.add(new Pair<>(Server.綠水靈.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "6", def))));
        list.add(new Pair<>(Server.三眼章魚.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "7", def))));
        list.add(new Pair<>(Server.木妖.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "8", def))));
        list.add(new Pair<>(Server.火獨眼獸.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "9", def))));
        list.add(new Pair<>(Server.蝴蝶精.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "10", def))));
        list.add(new Pair<>(Server.巴洛古.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "11", def))));
        list.add(new Pair<>(Server.海怒斯.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "12", def))));
        list.add(new Pair<>(Server.電擊象.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "13", def))));
        list.add(new Pair<>(Server.鯨魚號.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "14", def))));
        list.add(new Pair<>(Server.皮卡啾.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "15", def))));
        list.add(new Pair<>(Server.神獸.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "16", def))));
        list.add(new Pair<>(Server.泰勒熊.getId(), (global ? def : ServerProperties.getWorldProperty(arg + "17", def))));
    }

    public static void init() {

        loadWorldsSettings(chAmounts, USE_GLOBAL_CHANNEL_COUNT, "server.settings.channel.count", Channels);
        loadWorldsSettings(flags, USE_GLOBAL_FLAG, "server.settings.flags", GLOBAL_FLAG);
        loadWorldsSettings(expRates, USE_GLOBAL_RATES, "server.settings.expRate", GLOBAL_EXP_RATE);
        loadWorldsSettings(mesoRates, USE_GLOBAL_RATES, "server.settings.mesoRate", GLOBAL_MESO_RATE);
        loadWorldsSettings(dropRates, USE_GLOBAL_RATES, "server.settings.dropRate", GLOBAL_DROP_RATE);

        {
            String defaults = GLOBAL_EVENT_MSGS;
            String arg = "server.settings.eventMessage";
            // Event messages
            eventMessages.add(new Pair<>(Server.雪吉拉.getId(), ("歡迎來到 #b" + LoginServer.getServerName() + "!#k\r\n" + ServerProperties.getWorldProperty(arg, defaults))));
            eventMessages.add(new Pair<>(Server.菇菇寶貝.getId(), (USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "2", defaults))));
            eventMessages.add(new Pair<>(Server.星光精靈.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "3", defaults)));
            eventMessages.add(new Pair<>(Server.緞帶肥肥.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "4", defaults)));
            eventMessages.add(new Pair<>(Server.藍寶.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "5", defaults)));
            eventMessages.add(new Pair<>(Server.綠水靈.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "6", defaults)));
            eventMessages.add(new Pair<>(Server.三眼章魚.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "7", defaults)));
            eventMessages.add(new Pair<>(Server.木妖.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "8", defaults)));
            eventMessages.add(new Pair<>(Server.火獨眼獸.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "9", defaults)));
            eventMessages.add(new Pair<>(Server.蝴蝶精.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "10", defaults)));
            eventMessages.add(new Pair<>(Server.巴洛古.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "11", defaults)));
            eventMessages.add(new Pair<>(Server.海怒斯.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "12", defaults)));
            eventMessages.add(new Pair<>(Server.電擊象.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "13", defaults)));
            eventMessages.add(new Pair<>(Server.鯨魚號.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "14", defaults)));
            eventMessages.add(new Pair<>(Server.皮卡啾.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "15", defaults)));
            eventMessages.add(new Pair<>(Server.神獸.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "16", defaults)));
            eventMessages.add(new Pair<>(Server.泰勒熊.getId(), USE_GLOBAL_EVENT_MSG ? GLOBAL_EVENT_MSGS : ServerProperties.getWorldProperty(arg + "17", defaults)));
        }
        {
            String arg = "server.settings.canCreate";
            canCreates.add(new Pair<>(Server.雪吉拉.getId(), ServerProperties.getWorldProperty(arg + "1", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.菇菇寶貝.getId(), ServerProperties.getWorldProperty(arg + "2", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.星光精靈.getId(), ServerProperties.getWorldProperty(arg + "3", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.緞帶肥肥.getId(), ServerProperties.getWorldProperty(arg + "4", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.藍寶.getId(), ServerProperties.getWorldProperty(arg + "5", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.綠水靈.getId(), ServerProperties.getWorldProperty(arg + "6", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.三眼章魚.getId(), ServerProperties.getWorldProperty(arg + "7", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.木妖.getId(), ServerProperties.getWorldProperty(arg + "8", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.火獨眼獸.getId(), ServerProperties.getWorldProperty(arg + "9", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.蝴蝶精.getId(), ServerProperties.getWorldProperty(arg + "10", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.巴洛古.getId(), ServerProperties.getWorldProperty(arg + "11", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.海怒斯.getId(), ServerProperties.getWorldProperty(arg + "12", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.電擊象.getId(), ServerProperties.getWorldProperty(arg + "13", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.鯨魚號.getId(), ServerProperties.getWorldProperty(arg + "14", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.皮卡啾.getId(), ServerProperties.getWorldProperty(arg + "15", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.神獸.getId(), ServerProperties.getWorldProperty(arg + "16", GLOBAL_CAN_CREATE)));
            canCreates.add(new Pair<>(Server.泰勒熊.getId(), ServerProperties.getWorldProperty(arg + "17", GLOBAL_CAN_CREATE)));
        }
    }

    public static void loadSetting() {
        Channels = ServerProperties.getProperty("server.settings.channel.count", Channels);
        Worlds = ServerProperties.getProperty("server.settings.worlds.count", Worlds);
        GLOBAL_EXP_RATE = ServerProperties.getProperty("server.settings.expRate", GLOBAL_EXP_RATE);
        GLOBAL_MESO_RATE = ServerProperties.getProperty("server.settings.mesoRate", GLOBAL_MESO_RATE);
        GLOBAL_DROP_RATE = ServerProperties.getProperty("server.settings.dropRate", GLOBAL_DROP_RATE);
        GLOBAL_EVENT_MSGS = ServerProperties.getProperty("server.settings.eventMessage", GLOBAL_EVENT_MSGS);
        GLOBAL_FLAG = ServerProperties.getProperty("server.settings.flag", GLOBAL_FLAG);

        USE_GLOBAL_CHANNEL_COUNT = ServerProperties.getProperty("server.settings.globalchamounts", USE_GLOBAL_CHANNEL_COUNT);
        USE_GLOBAL_EVENT_MSG = ServerProperties.getProperty("server.settings.globaleventmsgs", USE_GLOBAL_EVENT_MSG);
        USE_GLOBAL_RATES = ServerProperties.getProperty("server.settings.globalrates", USE_GLOBAL_RATES);
        USE_GLOBAL_FLAG = ServerProperties.getProperty("server.settings.globalflag", USE_GLOBAL_FLAG);
        SCROLL_MESSAGE = ServerProperties.getProperty("server.settings.serverMessage", SCROLL_MESSAGE);
        ADMIN_ONLY = ServerProperties.getProperty("server.settings.admin", ADMIN_ONLY);
        UserLimit = ServerProperties.getProperty("server.settings.userlimit", UserLimit);
        GMITEMS = ServerProperties.getProperty("server.settings.gmitems", GMITEMS);
        CS_ENABLE = ServerProperties.getProperty("server.settings.cashshop.enable", CS_ENABLE);
        isShutDown = ServerProperties.getProperty("server.isShutdown", isShutDown);
        Disable_Shop = ServerProperties.getProperty("server.Disable_Shop", Disable_Shop);
    }

    static {
        loadSetting();
    }
}
