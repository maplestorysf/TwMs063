package client.messages.commands;

import client.MapleCharacter;
import client.messages.CommandExecute;
import constants.GameConstants;
import client.MapleClient;
import client.MapleStat;
import client.inventory.IItem;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.PiPiConfig;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import scripting.NPCScriptManager;
import tools.MaplePacketCreator;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import java.util.Arrays;
import tools.StringUtil;
import handling.world.World;
import java.util.Calendar;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleMonsterInformationProvider;
import server.swing.WvsCenter;
import tools.FilePrinter;
import tools.FileoutputUtil;

/**
 *
 * @author Emilyx3
 */

public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.普通玩家;
    }

    public static class Help extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropNPC(""
                    + "\t  #r▇▇▆▅▄▃▂#d萬用指令區#r▂▃▄▅▆▇▇\r\n"
                    + "\t\t#b@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>#k - #r<清除背包道具>#k\r\n"
                    + "\t\t#b@ea#k - #r<解除異常+查看當前狀態>#k\r\n"
                    + "\t\t#b@在線點數/@jcds#k - #r<領取在線點數>#k\r\n"
                    + "\t\t#b@mob#k - #r<查看身邊怪物訊息>#k\r\n"
                    + "\t\t#b@expfix#k - #r<經驗歸零(修復假死)>#k\r\n"
                    + "\t\t#b@CGM <訊息>#k - #r<傳送訊息給GM>#k\r\n"
                    + "\t\t#b@jk_hm #k - #r<清除卡精靈商人>#k\r\n"
                    + "\t\t#b@save#k - #r<存檔>#k\r\n"
                    + "\t\t#b@TSmega#k - #r<開/關所有廣播>#k\r\n"
                    + "\t\t#b@TTSmega#k - #r<開/關綠色廣播>#k\r\n"
                     + "\t\t#b@掉寶查詢#k - #r<查詢怪物掉寶>#k\r\n"
            );
            return true;
        }

        @Override
        public String getHelp() {
            return "- 幫助";
        }
    }
    public static class 掉寶查詢 extends CommandExecute {

        public boolean execute(MapleClient c, String[] splitted) {

            c.removeClickedNPC();
            NPCScriptManager.getInstance().start(c, 9010000, "怪物掉寶查詢");

            return true;
        }

        @Override
        public String getHelp() {
            return "@掉寶查詢 - 查詢怪物掉寶";
        }
    }

    public static class mobdrop extends 掉寶查詢 {

        @Override
        public String getHelp() {
            return "@mobdrop - 查詢怪物掉寶";
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static final int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
            9010017};

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                        return true;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(1, "你的等級必須是10等.");
                    return true;
                }
                if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                    return true;
                }
                if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                    return true;
                }
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return true;
        }
    }

    public static class Save extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            try {
                int res = c.getPlayer().saveToDB(true, false);
                if (res == 1) {
                    c.getPlayer().dropMessage(5, "保存成功！");
                } else {
                    c.getPlayer().dropMessage(5, "保存失敗！");
                }
            } catch (UnsupportedOperationException ex) {

            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 存檔";
        }
    }

    public static class ExpFix extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setExp(0);
            c.getPlayer().updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
            c.getPlayer().dropMessage(5, "經驗修復完成");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 經驗歸零";
        }
    }

    public static class TSmega extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開/關閉所有廣播";
        }
    }

    public static class TTSmega extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setTSmega();
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開/關閉綠色廣播";
        }
    }

    public static class EA extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.sendPacket(MaplePacketCreator.enableActions());
            StringBuilder sc = new StringBuilder();
            c.getPlayer().dropMessage(1, "解卡完畢..");
            c.getPlayer().dropMessage(6, "當前系統時間" + FilePrinter.getLocalDateString() + " 星期" + getDayOfWeek());
            c.getPlayer().dropMessage(6, "經驗值倍率 " + ((Math.round(c.getPlayer().getEXPMod()) * 100) * Math.round(c.getPlayer().getStat().expBuff / 100.0) + (c.getPlayer().getStat().equippedFairy ? c.getPlayer().getFairyExp() : 0)) + "%, 掉寶倍率 " + Math.round(c.getPlayer().getDropMod() * (c.getPlayer().getStat().dropBuff / 100.0) * 100) + "%, 楓幣倍率 " + Math.round((c.getPlayer().getStat().mesoBuff / 100.0) * 100) + "%" );
            c.getPlayer().dropMessage(6, "額外經驗值倍率 " + (c.getChannelServer().getExExpRate()) + "倍, 掉寶倍率 " + (c.getChannelServer().getExDropRate()) + "倍, 楓幣倍率 " + (c.getChannelServer().getExMesoRate()) + "倍");
            c.getPlayer().dropMessage(6, "目前剩餘 " + c.getPlayer().getCSPoints(1) + " GASH " + c.getPlayer().getCSPoints(2) + " 楓葉點數 ");
            c.getPlayer().dropMessage(6, "當前延遲 " + c.getPlayer().getClient().getLatency() + " 毫秒");
            c.getPlayer().dropMessage(6, "已使用:" + c.getPlayer().getHpMpApUsed() + " 張能力重置捲");
            if (c.getPlayer().getLevel() >= 120 && c.getPlayer().getQuestStatus(29400) == 1) {
            c.getPlayer().dropMessage(6, "精明的獵人已經擊殺:" + c.getPlayer().getMobCount() + "隻怪物.");
            }
            //sc.append("\r\n目前銀行存款:#d").append(c.getPlayer().getMesoFromBank());
            c.getPlayer().showInstruction(sc.toString(), 450, 30);

            return true;
        }

        @Override
        public String getHelp() {
            return "- 解卡";
        }

        public static String getDayOfWeek() {
            int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            String dd = String.valueOf(dayOfWeek);
            switch (dayOfWeek) {
                case 0:
                    dd = "日";
                    break;
                case 1:
                    dd = "一";
                    break;
                case 2:
                    dd = "二";
                    break;
                case 3:
                    dd = "三";
                    break;
                case 4:
                    dd = "四";
                    break;
                case 5:
                    dd = "五";
                    break;
                case 6:
                    dd = "六";
                    break;
            }
            return dd;
        }
    }

//    public static class JK extends CommandExecute {
//
//        @Override
//        public boolean execute(MapleClient c, String[] splitted) {
//            for (int i : GameConstants.blockedMaps) {
//                if (c.getPlayer().getMapId() == i) {
//                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                    return true;
//                }
//            }
//            if (c.getPlayer().getLevel() < 10) {
//                c.getPlayer().dropMessage(1, "你的等級必須是10等.");
//                return true;
//            }
//            if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
//                c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                return true;
//            }
//            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
//                c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                return true;
//            }
//            InterServerHandler.EnterCashShop(c, c.getPlayer(), false);
//            return true;
//        }
//
//        @Override
//        public String getHelp() {
//            return "- 重製";
//        }
//    }
    public static class Mob extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleMonster monster = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                monster = (MapleMonster) monstermo;
                if (monster.isAlive()) {
                    c.getPlayer().dropMessage(6, "怪物 " + monster.toString());
                }
            }
            if (monster == null) {
                c.getPlayer().dropMessage(6, "找不到地圖上的怪物");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看怪物狀態";
        }
    }

    public static class CGM extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            boolean autoReply = false;

            if (splitted.length < 2) {
                return false;
            }
            String talk = StringUtil.joinStringFrom(splitted, 1);
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "因為你自己是GM所以無法使用此指令,可以嘗試!cngm <訊息> 來建立GM聊天頻道~");
            } else {
                if (!c.getPlayer().getCheatTracker().GMSpam(100000, 1)) { // 1 minutes.
                    boolean fake = false;
                    boolean showmsg = true;

                    // 管理員收不到，玩家有顯示傳送成功
                    if (PiPiConfig.getBlackList().containsKey(c.getAccID())) {
                        fake = true;
                    }

                    // 管理員收不到，玩家沒顯示傳送成功
                    if (talk.contains("搶") && talk.contains("圖")) {
                        c.getPlayer().dropMessage(1, "搶圖自行解決！！");
                        fake = true;
                        showmsg = false;
                    } else if ((talk.contains("被") && talk.contains("騙")) || (talk.contains("點") && talk.contains("騙"))) {
                        c.getPlayer().dropMessage(1, "被騙請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if ((talk.contains("被") && talk.contains("盜"))) {
                        c.getPlayer().dropMessage(1, "被盜請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("刪") && ((talk.contains("角") || talk.contains("腳")) && talk.contains("錯"))) {
                        c.getPlayer().dropMessage(1, "刪錯角色請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("亂") && (talk.contains("名") && talk.contains("聲"))) {
                        c.getPlayer().dropMessage(1, "請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("密") && talk.contains("咒") && talk.contains("賣")) {
                        c.getPlayer().dropMessage(1, "密咒賣的價格已經更改為1楓幣無誤");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("改") && talk.contains("密") && talk.contains("碼")) {
                        c.getPlayer().dropMessage(1, "目前第二組密碼及密碼無法查詢及更改,");
                        fake = true;
                        showmsg = false;
                    }

                    // 管理員收的到，自動回復
                    if (talk.toUpperCase().contains("VIP") && ((talk.contains("領") || (talk.contains("獲"))) && talk.contains("取"))) {
                        c.getPlayer().dropMessage(1, "VIP將會於儲值後一段時間後自行發放，請耐心等待");
                        autoReply = true;
                    } else if (talk.contains("貢獻") || talk.contains("666") || ((talk.contains("取") || talk.contains("拿") || talk.contains("發") || talk.contains("領")) && ((talk.contains("勳") || talk.contains("徽") || talk.contains("勛")) && talk.contains("章")))) {
                        c.getPlayer().dropMessage(1, "勳章請去點拍賣NPC案領取勳章\r\n如尚未被加入清單請耐心等候GM。");
                        autoReply = true;
                    } else if (((talk.contains("商人") || talk.contains("精靈")) && talk.contains("吃")) || (talk.contains("商店") && talk.contains("補償"))) {
                        c.getPlayer().dropMessage(1, "目前精靈商人裝備和楓幣有機率被吃\r\n如被吃了請務必將當時的情況完整描述給管理員\r\n\r\nPS: 不會補償任何物品");
                        autoReply = true;
                    } else if (talk.contains("檔") && talk.contains("案") && talk.contains("受") && talk.contains("損")) {
                        c.getPlayer().dropMessage(1, "檔案受損請重新解壓縮主程式唷");
                        autoReply = true;
                    } else if ((talk.contains("缺") || talk.contains("少")) && ((talk.contains("技") && talk.contains("能") && talk.contains("點")) || talk.toUpperCase().contains("SP"))) {
                        c.getPlayer().dropMessage(1, "缺少技能點請重練，沒有其他方法了唷");
                        autoReply = true;

                    } else if (talk.contains("母書")) {
                        if (talk.contains("火流星")) {
                            c.getPlayer().dropMessage(1, "技能[火流星] 並沒有母書唷");
                            autoReply = true;
                        }
                    } else if (talk.contains("黑符") && talk.contains("不") && (talk.contains("掉") || talk.contains("噴"))) {
                        MapleMonsterInformationProvider.getInstance().clearDrops();
                        ReactorScriptManager.getInstance().clearDrops();
                        c.getPlayer().dropMessage(1, "黑符掉落機率偏低\r\n請打150場以上沒有噴再回報");
                        autoReply = true;
                    } else if (talk.contains("鎖") && talk.contains("寶")) {
                        c.getPlayer().dropMessage(1, "本伺服器目前並未鎖寶\r\n只有尚未添加的掉寶資料或是掉落機率偏低");
                        autoReply = true;
                    }

                    if (showmsg) {
                        c.sendCGMLog(c, talk);
                        c.getPlayer().dropMessage(6, "訊息已經寄送給GM了!");
                    }

                    if (!fake) {
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[管理員幫幫忙]頻道 " + c.getPlayer().getClient().getChannel() + " 玩家 [" + c.getPlayer().getName() + "] (" + c.getPlayer().getId() + "): " + talk + (autoReply ? " -- (系統已自動回復)" : "")));
                        if (System.getProperty("StartBySwing") != null) {
                            WvsCenter.addChatLog("[管理員幫幫忙] " + c.getPlayer().getName() + ": " + StringUtil.joinStringFrom(splitted, 1) + (autoReply ? " -- (系統已自動回復)" : "") + "\r\n");
                        }
                    }

                    FileoutputUtil.logToFile("logs/data/管理員幫幫忙.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家[" + c.getPlayer().getName() + "] 帳號[" + c.getAccountName() + "]: " + talk + (autoReply ? " -- (系統已自動回復)" : "") + "\r\n");

                } else {
                    c.getPlayer().dropMessage(6, "為了防止對GM刷屏所以每1分鐘只能發一次.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 跟GM回報";
        }
    }

    public static class ClearInv extends 清除道具 {

    }

    public static class 清除道具 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                return false;
            }
            MapleInventory inv;
            MapleInventoryType type;
            String Column = "null";
            int start = -1;
            int end = -1;
            try {
                Column = splitted[1].toUpperCase();
                start = Integer.parseInt(splitted[2]);
                end = Integer.parseInt(splitted[3]);
            } catch (Exception ex) {
            }
            if (start == -1 || end == -1) {
                c.getPlayer().dropMessage("@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            if (start < 1) {
                start = 1;
            }
            if (end > 96) {
                end = 96;
            }

            switch (Column) {
                case "裝備欄":
                case "EQUIP":
                    type = MapleInventoryType.EQUIP;
                    break;
                case "消耗欄":
                case "USE":
                    type = MapleInventoryType.USE;
                    break;
                case "裝飾欄":
                case "SETUP":
                    type = MapleInventoryType.SETUP;
                    break;
                case "其他欄":
                case "ETC":
                    type = MapleInventoryType.ETC;
                    break;
                case "特殊欄":
                case "CASH":
                    type = MapleInventoryType.CASH;
                    break;
                default:
                    type = null;
                    break;
            }
            if (type == null) {
                c.getPlayer().dropMessage("@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            inv = c.getPlayer().getInventory(type);

            StringBuilder sb = new StringBuilder();
            for (int i = start; i <= end; i++) {
                if (inv.getItem((short) i) != null) {
                    IItem item = inv.getItem((short) i);
                    String name = MapleItemInformationProvider.getInstance().getName(item.getItemId());
                    sb.append(name);// 道具名稱
                    sb.append("(");
                    sb.append(item.getItemId());// 道具代碼
                    sb.append(")");
                    sb.append(item.getQuantity());// 道具數量
                    sb.append("個、");
                    MapleInventoryManipulator.removeFromSlot(c, type, (short) i, item.getQuantity(), true);
                }
            }
            FileoutputUtil.logToFile("logs/data/玩家指令.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了指令 " + StringUtil.joinStringFrom(splitted, 0) + " 道具:" + sb.toString());
            c.getPlayer().dropMessage(6, "您已經清除了第 " + start + " 格到 " + end + "格的" + Column + "道具");
            return true;
        }

        @Override
        public String getHelp() {
            return "<裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>";
        }
    }

    public static class jk_hm extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().RemoveHired();
            c.getPlayer().dropMessage("卡精靈商人已經解除");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 卡精靈商人解除";
        }
    }

    public static class jcds extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int gain = c.getPlayer().getMP();
            if (gain <= 0) {
                c.getPlayer().dropMessage("目前沒有任何在線點數唷。");
                return true;
            }
            if (splitted.length < 2) {
                c.getPlayer().dropMessage("目前楓葉點數: " + c.getPlayer().getCSPoints(2));
                c.getPlayer().dropMessage("目前在線點數已經累積: " + gain + " 點，若要領取請輸入 @jcds true");
            } else if ("true".equals(splitted[1])) {
                gain = c.getPlayer().getMP();
                c.getPlayer().modifyCSPoints(2, gain, true);
                c.getPlayer().setMP(0);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().dropMessage("領取了 " + gain + " 點在線點數, 目前楓葉點數: " + c.getPlayer().getCSPoints(2));
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 領取在線點數";
        }
    }

    public static class 在線點數 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int gain = c.getPlayer().getMP();
            if (gain <= 0) {
                c.getPlayer().dropMessage("目前沒有任何在線點數唷。");
                return true;
            }
            if (splitted.length < 2) {
                c.getPlayer().dropMessage("目前楓葉點數: " + c.getPlayer().getCSPoints(2));
                c.getPlayer().dropMessage("目前在線點數已經累積: " + gain + " 點，若要領取請輸入 @在線點數 是");
            } else if ("是".equals(splitted[1])) {
                gain = c.getPlayer().getMP();
                c.getPlayer().modifyCSPoints(2, gain, true);
                c.getPlayer().setMP(0);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().dropMessage("領取了 " + gain + " 點在線點數, 目前楓葉點數: " + c.getPlayer().getCSPoints(2));
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 領取在線點數";
        }
    }

 /*   public static class 提錢 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }

            String input = null;
            long money = 0;

            try {
                input = splitted[1];
                money = Long.parseLong(input);
            } catch (Exception ex) {
                return false;
            }

            if (money <= 0) {
                c.getPlayer().dropMessage(6, "[銀行系統] 不能給負數或是0");
            } else if (c.getPlayer().getMesoFromBank() < money) {
                c.getPlayer().dropMessage(6, "[銀行系統] 銀行內只有" + money + "錢");
            } else if (money > 2100000000) {
                c.getPlayer().dropMessage(6, "[銀行系統] 一次只能提21E以內的錢");
            } else if (money + c.getPlayer().getMeso() > 2100000000 || money + c.getPlayer().getMeso() < 0) {
                c.getPlayer().dropMessage(6, "[銀行系統] 領的錢+身上的錢無法超過21E");
            } else {
                // 身上給錢
                c.getPlayer().gainMeso((int) (money), true);
                // 銀行扣錢 
                c.getPlayer().decMoneytoBank(money);
                c.getPlayer().dropMessage(6, "[銀行系統] 您已經提出 " + money + "錢");
                FileoutputUtil.logToFile("logs/openlog/銀行取出.txt", "取出時間:" + FileoutputUtil.NowTime() + "世界: " + c.getPlayer().getWorld() + " 帳號編號:" + c.getPlayer().getAccountID() + " 角色名稱:" + c.getPlayer().getName() + " 取出金額: " + money + "銀行剩餘:" + c.getPlayer().getMesoFromBank() + "\r\n");
            }

            return true;
        }

        @Override
        public String getHelp() {
            return "<錢> - 提銀行錢到身上";
        }
    }

    public static class 存錢 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }

            String input = null;
            long money = 0;

            try {
                input = splitted[1];
                money = Long.parseLong(input);
            } catch (Exception ex) {
                return false;
            }

            if (money <= 0) {
                c.getPlayer().dropMessage(6, "[銀行系統] 不能給負數或是0");
            } else if (money > 2100000000) {
                c.getPlayer().dropMessage(6, "[銀行系統] 一次只能存21E以內的錢");
            } else if (c.getPlayer().getMeso() < money) {
                c.getPlayer().dropMessage(6, "[銀行系統] 目前您的身上裡沒有" + money + "錢");
            } else {
                // 身上扣錢
                c.getPlayer().gainMeso((int) (-money), true);
                // 銀行存錢 
                c.getPlayer().incMoneytoBank(money);
                c.getPlayer().dropMessage(6, "[銀行系統] 您已經存入 " + money + "錢");
                FileoutputUtil.logToFile("logs/openlog/銀行存入.txt", "存入時間:" + FileoutputUtil.NowTime() + "世界: " + c.getPlayer().getWorld() + "帳號編號:" + c.getPlayer().getAccountID() + " 角色名稱:" + c.getPlayer().getName() + " 存入金額: " + money + "銀行餘額:" + c.getPlayer().getMesoFromBank() + "\r\n");
            }

            return true;
        }

        @Override
        public String getHelp() {
            return "<錢> - 存錢到自己銀行";
        }
    }

    public static class 轉帳 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }

            MapleCharacter victim = null;
            String input = null;
            long money = 0;
            int ch = -1, wl = -1;

            try {
                input = splitted[1];
                money = Long.parseLong(splitted[2]);
                ch = World.Find.findChannel(input);
                wl = World.Find.findWorld(input);
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(input);
            } catch (Exception ex) {
                return false;
            }

            if (money <= 0) {
                c.getPlayer().dropMessage(6, "[銀行系統] 不能給負數或是0");
            } else if (victim == null || victim.getWorld() != c.getPlayer().getWorld()) {
                c.getPlayer().dropMessage(6, "[銀行系統] 對方(" + input + ")不在線上");
            } else if (ch == -10) {
                c.getPlayer().dropMessage(6, "[銀行系統] 對方目前在商城內");
            } else if (victim.getAccountID() == c.getAccID()) {
                c.getPlayer().dropMessage(6, "[銀行系統] 無法給予自己錢。");
            } else if (victim.getGMLevel() > c.getPlayer().getGMLevel()) {
                c.getPlayer().dropMessage(6, "[銀行系統] 無法給予" + victim.getName() + "錢。");
            } else if (c.getPlayer().getMesoFromBank() < money) {
                c.getPlayer().dropMessage(6, "[銀行系統] 目前您的銀行裡沒有" + money + "錢");
            } else {
                // 從自己銀行扣錢
                c.getPlayer().decMoneytoBank(money);
                // 到對方銀行給錢
                victim.incMoneytoBank(money);
                c.getPlayer().dropMessage(6, "[銀行系統] 您已經轉帳給" + " 世界:" + victim.getWorld() + victim.getName() + "的玩家: " + money);
                victim.dropMessage(6, "[銀行系統] 世界" + c.getPlayer().getWorld() + c.getPlayer().getName() + "轉帳給您 " + money);
                FileoutputUtil.logToFile("logs/openlog/銀行系統.txt", "\r\n" + FileoutputUtil.NowTime() + "世界 " + c.getPlayer().getWorld() + " 帳號編號: (" + c.getAccID() + ")" + c.getPlayer().getName() + " 給了世界 " + victim.getWorld() + " 帳號編號: (" + victim.getAccountID() + ")" + victim.getName() + " " + money + "錢\r\n" + "總結:" + c.getPlayer().getName() + " 原本:" + (c.getPlayer().getMesoFromBank() + money) + " 剩餘:" + c.getPlayer().getMesoFromBank() + " " + victim.getName() + " 原本:" + (victim.getMesoFromBank() - money) + " 獲得後:" + victim.getMesoFromBank());
            }

            return true;
        }

        @Override
        public String getHelp() {
            return "<名字> <錢> - 給對方自己銀行的錢";
        }
    }

    public static class 轉鋼珠 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }

            MapleCharacter victim = null;
            String input = null;
            long balls = 0;
            int ch = -1, wl = -1;

            try {
                input = splitted[1];
                balls = Long.parseLong(splitted[2]);
                ch = World.Find.findChannel(input);
                wl = World.Find.findWorld(input);
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(input);
            } catch (Exception ex) {
                return false;
            }

            if (balls <= 0) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 不能給負數或是0");
            } else if (victim == null || victim.getWorld() != c.getPlayer().getWorld()) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 對方(" + input + ")不在線上");
            } else if (ch == -10) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 對方目前在商城內");
            } else if (victim.getAccountID() == c.getAccID()) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 無法給予自己小鋼珠。");
            } else if (victim.getGMLevel() > c.getPlayer().getGMLevel()) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 無法給予" + victim.getName() + "小鋼珠。");
            } else if (c.getPlayer().getBeans() < balls) {
                c.getPlayer().dropMessage(6, "[鋼珠系統] 目前您的身上沒有" + balls + "小鋼珠");
            } else {
                // 從自己身上扣小鋼珠
                c.getPlayer().gainBeans(-(int)balls);
                // 到對方身上給小鋼珠
                victim.gainBeans((int)balls);
                c.getPlayer().dropMessage(6, "[鋼珠系統] 您已經轉鋼珠給" + " 世界:" + victim.getWorld() + victim.getName() + "的玩家: " + balls);
                victim.dropMessage(6, "[鋼珠系統] 世界" + c.getPlayer().getWorld() + c.getPlayer().getName() + "轉鋼珠給您 " + balls);
                FileoutputUtil.logToFile("logs/openlog/鋼珠系統.txt", "\r\n"
                        + FileoutputUtil.NowTime()
                        + "世界 " + c.getPlayer().getWorld() + " 帳號編號: (" + c.getAccID() + ")" + c.getPlayer().getName()
                        + " 給了世界 " + victim.getWorld() + " 帳號編號: (" + victim.getAccountID() + ")" + victim.getName()
                        + " " + balls + "小鋼珠\r\n"
                        + "總結:" + c.getPlayer().getName() + " 原本:" + (c.getPlayer().getBeans() + balls)
                        + " 剩餘:" + c.getPlayer().getBeans() + " "
                        + victim.getName() + " 原本:" + (victim.getBeans() - balls) + " 獲得後:" + victim.getBeans());
            }

            return true;
        }

        @Override
        public String getHelp() {
            return "<名字> <小鋼珠> - 給對方自己身上的小鋼珠";
        }
    }*/
}
