package constants;

public class MapConstants {

    public static boolean isBlockFM(final int mapid) {
        int header = mapid / 100000;
        if (isEventMap(mapid)) {
            return true;
        }
        if (header == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100)) {
            return true;
        }
        if (mapid / 10000 == 92502) {
            return true;
        }
        if (header == 7090) {
            return true;
        }
        if (header == 1090) {
            return true;
        }
        switch (mapid) {
            case 702060000:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMapleLand(int mapid) {
        return mapid < 1010004;
    }

    public static boolean isStartingEventMap(final int mapid) {
        switch (mapid) {
            case 109010000:
            case 109020001:
            case 109030001:
            case 109030101:
            case 109030201:
            case 109030301:
            case 109030401:
            case 109040000:
            case 109060001:
            case 109060002:
            case 109060003:
            case 109060004:
            case 109060005:
            case 109060006:
            case 109080000:
            case 109080001:
            case 109080002:
            case 109080003:
                return true;
        }
        return false;
    }

    public static boolean isEventMap(final int mapid) {
        return (mapid >= 109010000 && mapid < 109050000) || (mapid > 109050001 && mapid < 109090000) || (mapid >= 809040000 && mapid <= 809040100);
    }

    public static boolean isBossMap(int mapid) {
        if (mapid / 10000 == 92502) {// 武陵道場
            return true;
        }
        switch (mapid) {
            case 105100300: // 巴洛古
            case 220080001: // 鐘王
            case 230040420: // 海怒斯
            case 240060000: // 龍王前置
            case 240060100: // 龍王前置
            case 240060200: // 龍王
            case 270050100: // 皮卡啾
            case 280030000: // 炎魔
            case 551030200: // 夢幻主題公園
            case 740000000: // PQ
            case 741020101: // 黑輪王
            case 741020102: // 黑輪王
            case 749050301: // 洽吉
            case 802000211: // 日本台場BOSS
            case 802000611: // 日本台場BOSS
            case 922010900: // 時空的裂縫
            case 925020200: // 武陵
            case 930000600: // 劇毒森林
                return true;
        }
        return false;
    }

    public static boolean isCakeMap(final int mapId) {
        return mapId >= 749020000 && mapId <= 749020800;
    }

    public static boolean is猴子森林(final int mapid) {
        switch (mapid) {
            case 100040101:
            case 100040102:
            case 100040103:
            case 100040104:
            case 107000401:
            case 107000402:
            case 107000403:
            case 191000000:
                return true;
        }
        return false;
    }

    public static boolean is靈藥幻境(final int mapid) {
        switch (mapid) {
            case 251010000:
            case 251010100:
            case 251010101:
                return true;
        }
        return false;
    }

    public static boolean is赫爾奧斯塔(final int mapid) {
        switch (mapid) {
            case 222020100:
            case 222020200:
            case 222020300:
                return true;
        }
        return false;
    }

    public static boolean is愛奧斯塔(final int mapid) {
        switch (mapid) {
            case 221020100:
            case 221020200:
            case 221020400:
            case 221020300:
            case 221023700:
            case 221023800:
            case 221023900:
            case 221024000:
            case 221024100:
            case 221024200:
                return true;
        }
        return false;
    }

    public static boolean is維多利亞港地域(final int mapid) {
        switch (mapid) {
            case 104000100:
            case 104000200:
            case 104000300:
            case 104000400:
                return true;
        }
        return false;
    }

    public static boolean is雲彩公園(final int mapid) {
        switch (mapid) {
            case 200020000:
            case 200030000:
            case 200040000:
            case 200040001:
            case 200050000:
            case 200060000:
            case 200070000:
            case 200080000:
                return true;
        }
        return false;
    }
}
