package constants;

import java.io.File;
import server.ServerProperties;

public class ServerConfig {

    public static boolean LOG_TRADE = true;
    public static boolean LOG_MERCHANT = true;
    public static boolean LOG_OWNERSTORE = true;
    public static boolean LOG_CSBUY = false;
    public static boolean LOG_DAMAGE = false;
    public static boolean LOG_CHAT = false;
    public static boolean LOG_MEGA = false;
    public static boolean LOG_PACKETS = false;
    public static boolean AUTO_REGISTER = true;
    public static boolean LOG_CHALKBOARD = false;
    public static boolean LOG_SCROLL = false;
    public static boolean LOG_DC = true;
    public static boolean LOG_DROPITEM = true;
    public static boolean LOG_PICKITEM = true;
    public static boolean Log_Storage = true;
    public static String SERVER_NAME = "PPMS";
    public static String IP = "127.0.0.1";
    public static int ChannelPort = 7575;
    public static int DEFAULT_CHARSLOT = 3;
    public static boolean ENABLE_OTP = false;
    private static String EVENTS = null;
    public static boolean DEBUG_MODE = false;
    public static boolean checkCopyItem = true;
    public static boolean CUSTOM_ENCRYPTION = false;

    public static String[] getEvents(boolean reLoad) {
        return getEventList(reLoad).split(",");
    }

    public static String getEventList(boolean reLoad) {
        if (EVENTS == null || reLoad) {
            File root = new File("scripts/event");
            File[] files = root.listFiles();
            EVENTS = "";
            for (File file : files) {
                if (!file.isDirectory()) {
                    String[] fileName = file.getName().split("\\.");
                    if (fileName.length > 1 && "js".equals(fileName[fileName.length - 1])) {
                        for (int i = 0; i < fileName.length - 1; i++) {
                            EVENTS += fileName[i];
                        }
                        EVENTS += ",";
                    }
                }
            }
        }
        return EVENTS;
    }

    public static boolean isAutoRegister() {
        return AUTO_REGISTER;
    }

    public static String getVipMedalName(int lv) {
        String medal = "";
        if (SERVER_NAME.equals("辛巴谷")) {
            switch (lv) {
                case 1:
                    medal = " <初級VIP>";
                    break;
                case 2:
                    medal = " <普通VIP>";
                    break;
                case 3:
                    medal = " <進階VIP>";
                    break;
                case 4:
                    medal = " <高級VIP>";
                    break;
                case 5:
                    medal = " <白金VIP>";
                    break;
                case 6:
                    medal = " <鑽石VIP>";
                    break;
                case 7:
                    medal = " <尊貴VIP>";
                    break;
                case 8:
                    medal = " <至尊VIP>";
                    break;
                case 9:
                    medal = " <無極VIP>";
                    break;
                case 10:
                    medal = " <無極至尊VIP>";
                    break;
                default:
                    medal = " <VIP" + medal + ">";
                    break;
            }
        } else if (SERVER_NAME.equals("西西谷")) {
            int newtime = 0;
            medal = " ";
            while (lv - newtime != 0) {
                if (newtime % 2 == 0) {
                    medal += "☆";
                } else {
                    medal += "★";
                }
                newtime++;
            }
        }
        return medal;
    }

    public static void loadSetting() {
        LOG_TRADE = ServerProperties.getProperty("server.settings.tradeLog", LOG_TRADE);
        LOG_SCROLL = ServerProperties.getProperty("server.settings.scrollLog", LOG_SCROLL);
        LOG_DC = ServerProperties.getProperty("server.settings.dclog", LOG_DC);
        LOG_MERCHANT = ServerProperties.getProperty("server.settings.merchantLog", LOG_MERCHANT);
        LOG_OWNERSTORE = ServerProperties.getProperty("server.settings.buystore", LOG_OWNERSTORE);
        LOG_MEGA = ServerProperties.getProperty("server.settings.megaLog", LOG_MEGA);
        LOG_CSBUY = ServerProperties.getProperty("server.settings.csLog", LOG_CSBUY);
        LOG_DAMAGE = ServerProperties.getProperty("server.settings.damLog", LOG_DAMAGE);
        LOG_CHAT = ServerProperties.getProperty("server.settings.chatLog", LOG_CHAT);
        LOG_CHALKBOARD = ServerProperties.getProperty("server.settings.chalkboard", LOG_CHALKBOARD);
        LOG_PACKETS = ServerProperties.getProperty("server.settings.packetLog", LOG_PACKETS);
        LOG_DROPITEM = ServerProperties.getProperty("server.settings.dropitemLog", LOG_DROPITEM);
        LOG_PICKITEM = ServerProperties.getProperty("server.settings.pickitemLog", LOG_PICKITEM);
        AUTO_REGISTER = ServerProperties.getProperty("server.settings.autoRegister", AUTO_REGISTER);
        SERVER_NAME = ServerProperties.getProperty("server.settings.serverName", SERVER_NAME);
        IP = ServerProperties.getProperty("server.settings.ip", IP);
        ChannelPort = ServerProperties.getProperty("server.settings.channel.port", ChannelPort);
        DEBUG_MODE = ServerProperties.getProperty("server.settings.debug", DEBUG_MODE);
        DEFAULT_CHARSLOT = ServerProperties.getProperty("server.settings.charslot", DEFAULT_CHARSLOT);
    }

    static {
        loadSetting();
    }
}
