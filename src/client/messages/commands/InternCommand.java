package client.messages.commands;

import client.messages.CommandExecute;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CheaterData;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.TreeMap;
import server.MaplePortal;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.FilePrinter;
import tools.StringUtil;

public class InternCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.巡邏者;
    }

    public static class 發勳章 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }

            String playername = splitted[1];
            int playerid = MapleCharacter.getCharacterIdByName(playername);
            if (playerid == -1) {
                c.getPlayer().dropMessage("玩家[" + playername + "]不存在於資料庫內。");
                return true;
            }
            MapleCharacter victim = MapleCharacter.getCharacterById(playerid);
            if (victim == null){
                c.getPlayer().dropMessage("玩家[" + playername + "]null，ＷＴＦ？");
                return true;
            }
            int accountid = victim.getAccountID();
            int world =victim.getWorld();
            int id = createPlayer(accountid, playerid, playername, world);
            if (id < 0) {
                switch (id) {
                    case -3:
                        c.getPlayer().dropMessage("帳號ID[" + accountid + "]已經在領取勳章清單內。");
                        break;
                    case -2:
                        c.getPlayer().dropMessage("玩家[" + playername + "]已經在領取勳章清單內。");
                        break;
                    case -1:
                        c.getPlayer().dropMessage("發生未知的錯誤。");
                        break;
                    default:
                        c.getPlayer().dropMessage("發生未知的未知的錯誤, id:" + id + " accountid: " + accountid + " playerid: " + playerid + " playername: " + playername);
                        break;
                }
                return true;
            } else {
                c.getPlayer().dropMessage("玩家[" + playername + "]已經成功添加到領取勳章清單內(編號" + id + ")。");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> - 將勳章領取名單添加玩家";
        }

        public int createPlayer(int accountid, int id, String name, int world) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM rcmedals WHERE name = ?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {// 角色已經存在於勳章表單
                    rs.close();
                    ps.close();
                    return -2;
                }
                ps.close();
                rs.close();


                ps = con.prepareStatement("SELECT accountid FROM rcmedals WHERE accountid = ? and world  = ?");

                ps.setInt(1, accountid);
                ps.setInt(2, world);
                rs = ps.executeQuery();
                if (rs.first()) {// 帳號已經存在於勳章表單
                    rs.close();
                    ps.close();
                    return -3;
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("INSERT INTO rcmedals (`accountid`,`id`, `name`, `amount`, `world`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, accountid);
                ps.setInt(2, id);
                ps.setString(3, name);
                ps.setInt(4, 1);
                ps.setInt(5, world);
                ps.execute();
                rs = ps.getGeneratedKeys();
                int ret = 0;
                if (rs.next()) {
                    ret = rs.getInt(1);
                }
                rs.close();
                ps.close();
                return ret;
            } catch (Exception ex) {
                FilePrinter.printError("InternCommand.txt", ex, "createPlayer(" + name + ")");

            }
            return -1;
        }
    }
 public static class 發分享 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }

            String playername = splitted[1];
            int playerid = MapleCharacter.getCharacterIdByName(playername);
            if (playerid == -1) {
                c.getPlayer().dropMessage("玩家[" + playername + "]不存在於資料庫內。");
                return true;
            }
            MapleCharacter victim = MapleCharacter.getCharacterById(playerid);
            if (victim == null){
                c.getPlayer().dropMessage("玩家[" + playername + "]null，ＷＴＦ？");
                return true;
            }
            int accountid = victim.getAccountID();
            int world =victim.getWorld();
            int id = createPlayer(accountid, playerid, playername, world);
            if (id < 0) {
                switch (id) {
                    case -3:
                        c.getPlayer().dropMessage("帳號ID[" + accountid + "]已經在領取勳章清單內。");
                        break;
                    case -2:
                        c.getPlayer().dropMessage("玩家[" + playername + "]已經在領取勳章清單內。");
                        break;
                    case -1:
                        c.getPlayer().dropMessage("發生未知的錯誤。");
                        break;
                    default:
                        c.getPlayer().dropMessage("發生未知的未知的錯誤, id:" + id + " accountid: " + accountid + " playerid: " + playerid + " playername: " + playername);
                        break;
                }
                return true;
            } else {
                c.getPlayer().dropMessage("玩家[" + playername + "]已經成功添加到領取勳章清單內(編號" + id + ")。");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> - 將勳章領取名單添加玩家";
        }

        public int createPlayer(int accountid, int id, String name, int world) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM fbmedals WHERE name = ?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {// 角色已經存在於勳章表單
                    rs.close();
                    ps.close();
                    return -2;
                }
                ps.close();
                rs.close();


                ps = con.prepareStatement("SELECT accountid FROM fbmedals WHERE accountid = ? and world  = ?");

                ps.setInt(1, accountid);
                ps.setInt(2, world);
                rs = ps.executeQuery();
                if (rs.first()) {// 帳號已經存在於勳章表單
                    rs.close();
                    ps.close();
                    return -3;
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("INSERT INTO fbmedals (`accountid`,`id`, `name`, `amount`, `world`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, accountid);
                ps.setInt(2, id);
                ps.setString(3, name);
                ps.setInt(4, 1);
                ps.setInt(5, world);
                ps.execute();
                rs = ps.getGeneratedKeys();
                int ret = 0;
                if (rs.next()) {
                    ret = rs.getInt(1);
                }
                rs.close();
                ps.close();
                return ret;
            } catch (Exception ex) {
                FilePrinter.printError("fb分享獎勵.txt", ex, "createPlayer(" + name + ")");

            }
            return -1;
        }
    }
    public static class Mute extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            String name = "";
            try {
                name = splitted[1];
            } catch (Exception ex) {
            }
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch > 0) {
                MapleCharacter victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                victim.canTalk(!victim.getCanTalk());
                c.getPlayer().dropMessage("玩家[" + victim.getName() + "] 目前已經" + (victim.getCanTalk() ? "可以說話" : "閉嘴"));
            } else if (ch == -10) {
                c.getPlayer().dropMessage("玩家[" + splitted[1] + "] 目前正在購物商城");
            } else {
                try {
                    boolean canTalk = true;
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT canTalk FROM characters WHERE name = ?");
                    ps.setString(1, name);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        ps.close();
                        rs.close();
                        c.getPlayer().dropMessage("玩家[" + name + "] 不存在於資料庫");
                        return true;
                    } else {
                        canTalk = rs.getInt("canTalk") == 1;
                    }

                    int tochange = (canTalk ? 0 : 1);
                    try (PreparedStatement pss = con.prepareStatement("Update characters set canTalk = " + tochange + " Where name = " + name)) {
                        pss.executeUpdate();
                    }
                    c.getPlayer().dropMessage("玩家[" + name + "] 目前已經" + (tochange == 1 ? "可以說話" : "閉嘴"));
                } catch (Exception ex) {
                    c.getPlayer().dropMessage("發生異常情況 " + ex);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "玩家名稱 - 讓玩家閉嘴或可以說話";
        }
    }

    public static class MuteID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int id = 0;
            try {
                id = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
            }
            int ch = World.Find.findChannel(id);
            int wl = World.Find.findWorld(id);
            if (ch > 0) {
                MapleCharacter victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(id);
                victim.canTalk(!victim.getCanTalk());
                c.getPlayer().dropMessage("玩家[" + victim.getName() + "] 目前已經" + (victim.getCanTalk() ? "可以說話" : "閉嘴"));
            } else if (ch == -10) {
                c.getPlayer().dropMessage("玩家[" + splitted[1] + "] 目前正在購物商城");
            } else {
                try {
                    boolean canTalk = true;
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT canTalk FROM characters WHERE id = ?");
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        ps.close();
                        rs.close();
                        c.getPlayer().dropMessage("玩家編號[" + id + "] 不存在於資料庫");
                        return true;
                    } else {
                        canTalk = rs.getInt("canTalk") == 1;
                    }

                    int tochange = (canTalk ? 0 : 1);
                    try (PreparedStatement pss = con.prepareStatement("Update characters set canTalk = " + tochange + " Where id = " + id)) {
                        pss.executeUpdate();
                    }
                    c.getPlayer().dropMessage("玩家[" + id + "] 目前已經" + (tochange == 1 ? "可以說話" : "閉嘴"));
                } catch (Exception ex) {
                    c.getPlayer().dropMessage("發生異常情況 " + ex);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "玩家編號 - 讓玩家閉嘴或可以說話";
        }
    }

    public static class 禁言名單 extends MuteList {

    }

    public static class MuteList extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            java.util.Map<Integer, String> CharactersInfo = new TreeMap();
            Connection con = DatabaseConnection.getConnection();
            StringBuilder ret = new StringBuilder();
            String show = "";
            try {
                try (PreparedStatement ps = con.prepareStatement("select id, name from characters WHERE canTalk = 0")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            CharactersInfo.put(rs.getInt("id"), rs.getString("name"));
                        }
                    }
                }
            } catch (Exception ex) {
                c.getPlayer().dropMessage("發生異常情況 " + ex);
            }
            if (CharactersInfo.isEmpty()) {
                c.getPlayer().dropMessage("查詢禁言名單為空");
            } else {
                for (java.util.Map.Entry<Integer, String> entry : CharactersInfo.entrySet()) {
                    ret = new StringBuilder();
                    ret.append(" 角色暱稱 ");
                    ret.append(StringUtil.getRightPaddedStr(entry.getValue(), ' ', 15));
                    ret.append(" 角色編號: ");
                    ret.append(StringUtil.getRightPaddedStr(entry.getKey() + "", ' ', 5));
                    ret.append("\r\n");
                    show += ret.toString();
                }
                c.getPlayer().dropNPC(show);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看禁言名單";
        }
    }

    public static class MuteMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                chr.canTalk(!chr.getCanTalk());
                StringBuilder ret = new StringBuilder();
                ret.append(" 角色暱稱 ");
                ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                ret.append(" ID: ");
                ret.append(StringUtil.getRightPaddedStr(chr.getId() + "", ' ', 5));
                ret.append(" 目前已經: ");
                ret.append(chr.getCanTalk() ? "可以說話" : "閉嘴");
                c.getPlayer().dropMessage(ret.toString());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 讓地圖玩家閉嘴或可以說話";
        }
    }

    public static class CCGM extends 查CGM紀錄 {

        @Override
        public String getHelp() {
            return "<玩家ID> - 可以查看玩家CGM說過的話";
        }
    }

    public static class 查CGM紀錄 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int id = 0;
            try {
                id = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
            }
            MapleCharacter victim = MapleCharacter.getCharacterById(id);
            java.util.Map<String, String> cgminfo = new TreeMap();
            Connection con = DatabaseConnection.getConnection();
            StringBuilder ret = new StringBuilder();
            String show = "";
            try {
                try (PreparedStatement ps = con.prepareStatement("select message,cgmtime from cgmlogs WHERE cid = " + id)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            cgminfo.put(rs.getString("message"), rs.getString("cgmtime"));
                        }
                    }
                }
            } catch (Exception ex) {
                c.getPlayer().dropMessage("發生異常情況 " + ex);
            }
            if (cgminfo.isEmpty()) {
                c.getPlayer().dropMessage("此玩家沒有使用過CGM");
            } else {
                show += ("角色名稱: " + victim.getName() + " 角色編號: " + id + "\r\n\r\n");
                for (java.util.Map.Entry<String, String> entry : cgminfo.entrySet()) {
                    ret = new StringBuilder();
                    ret.append("留言: ");
                    ret.append(StringUtil.getRightPaddedStr(entry.getKey() + "", ' ', 500));
                    ret.append("紀錄時間: ");
                    ret.append(StringUtil.getRightPaddedStr(entry.getValue() + "", ' ', 0));
                    ret.append("\r\n");
                    show += ret.toString();
                }
                c.getPlayer().dropNPC(show);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家ID> - 可以查看玩家CGM說過的話";
        }
    }

    public static class CC extends ChangeChannel {

        public String getHelp() {
            return "<頻道> - 更換頻道";
        }
    }

    public static class ChangeChannel extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int cc = Integer.parseInt(splitted[1]);
            if (c.getChannel() != cc) {
                c.getPlayer().changeChannel(cc);
            } else {
                c.getPlayer().dropMessage(5, "請輸入正確的頻道。");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<頻道> - 更換頻道";
        }
    }

    public static class SpyBuff extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            } else {
                String name = splitted[1];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(name);
                MapleCharacter player = null;
                if (ch == -10) {
                    c.getPlayer().dropMessage("目前玩家[" + name + "]正在商城中");
                    return true;
                } else if (ch > 0) {
                    player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                }

                if (player != null) {
                    int Watk = player.getBuffSource(MapleBuffStat.WATK);
                    int Matk = player.getBuffSource(MapleBuffStat.MATK);
                    int MapleWarrior = player.getBuffSource(MapleBuffStat.MAPLE_WARRIOR);
                    int SharpEyes = player.getBuffSource(MapleBuffStat.SHARP_EYES);
                    int Speed = player.getBuffSource(MapleBuffStat.SPEED);
                    int Jump = player.getBuffSource(MapleBuffStat.JUMP);
                    int AttackSpeed = player.getBuffSource(MapleBuffStat.BOOSTER);
                    int SpeedInfusion = player.getBuffSource(MapleBuffStat.SPEED_INFUSION);
                    int unlimitmp = player.getBuffSource(MapleBuffStat.INFINITY);
                    int shadow = player.getBuffSource(MapleBuffStat.SHADOWPARTNER);
                    int magicGuard = player.getBuffSource(MapleBuffStat.MAGIC_GUARD);
                    int exp = player.getBuffSource(MapleBuffStat.EXPRATE);
                    int meso = player.getBuffSource(MapleBuffStat.MESO_RATE);
                    int drop = player.getBuffSource(MapleBuffStat.DROP_RATE);
                    c.getPlayer().dropMessage("玩家[" + name + "] 目前補助技能如下");
                    if (Watk != -1) {
                        c.getPlayer().dropMessage("物理攻擊技能: " + Watk + " (" + SkillFactory.getSkillName(Watk) + ")");
                    }
                    if (Matk != -1) {
                        c.getPlayer().dropMessage("魔法攻擊技能: " + Matk + " (" + SkillFactory.getSkillName(Matk) + ")");
                    }
                    if (MapleWarrior != -1) {
                        c.getPlayer().dropMessage("楓葉祝福技能: " + MapleWarrior + " (" + SkillFactory.getSkillName(MapleWarrior) + ")");
                    }
                    if (SharpEyes != -1) {
                        c.getPlayer().dropMessage("會心之眼技能: " + SharpEyes + " (" + SkillFactory.getSkillName(SharpEyes) + ")");
                    }
                    if (Speed != -1) {
                        c.getPlayer().dropMessage("移動速度技能: " + Speed + " (" + SkillFactory.getSkillName(Speed) + ")");
                    }
                    if (Jump != -1) {
                        c.getPlayer().dropMessage("跳越高度技能: " + Jump + " (" + SkillFactory.getSkillName(Jump) + ")");
                    }
                    if (AttackSpeed != -1) {
                        c.getPlayer().dropMessage("攻擊速度技能: " + AttackSpeed + " (" + SkillFactory.getSkillName(AttackSpeed) + ")");
                    }
                    if (SpeedInfusion != -1) {
                        c.getPlayer().dropMessage("最終極速技能: " + SpeedInfusion + " (" + SkillFactory.getSkillName(SpeedInfusion) + ")");
                    }
                    if (unlimitmp != -1) {
                        c.getPlayer().dropMessage("魔無限技能: " + unlimitmp + " (" + SkillFactory.getSkillName(unlimitmp) + ")");
                    }
                    if (shadow != -1) {
                        c.getPlayer().dropMessage("影分身技能: " + shadow + " (" + SkillFactory.getSkillName(shadow) + ")");
                    }
                    if (magicGuard != -1) {
                        c.getPlayer().dropMessage("魔心防禦技能: " + magicGuard + " (" + SkillFactory.getSkillName(magicGuard) + ")");
                    }
                    if (exp != -1) {
                        c.getPlayer().dropMessage("經驗倍率技能: " + exp + " (" + SkillFactory.getSkillName(exp) + ")");
                    }
                    if (drop != -1) {
                        c.getPlayer().dropMessage("掉落倍率技能: " + drop + " (" + SkillFactory.getSkillName(drop) + ")");
                    }
                    if (meso != -1) {
                        c.getPlayer().dropMessage("楓幣倍率技能: " + meso + " (" + SkillFactory.getSkillName(meso) + ")");
                    }
                    c.getPlayer().dropMessage("玩家[" + name + "] 補助技能如上");
                } else {
                    c.getPlayer().dropMessage(5, "找不到此玩家.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名字> - 查看玩家補助技能";
        }

    }

    public static class 角色訊息 extends Spy {
    }

    public static class Spy extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            } else {
                String name = splitted[1];
                MapleCharacter victim = MapleCharacter.getCharacterByName(name);
                int ch = World.Find.findChannel(name);
                if (victim != null) {
                    if (victim.getGMLevel() > c.getPlayer().getGMLevel()) {
                        c.getPlayer().dropMessage(5, "你沒有足夠的權限");
                        return true;
                    }
                    int mesoInStorage = 0;
                    try {
                        if (victim.getStorage() != null) {
                            mesoInStorage = victim.getStorage().getMeso();
                        }
                    } catch (Exception ex) {
                    }
                    c.getPlayer().dropMessage(5, "此玩家狀態:");
                    c.getPlayer().dropMessage(5, "角色伺服器:" + victim.getWorld());
                    c.getPlayer().dropMessage(5, "玩家名稱: " + victim.getName() + " 玩家編號: " + victim.getId() + " 帳號: " + victim.getClient().getAccountName() + " 帳號ID: " + victim.getAccountID());
                    c.getPlayer().dropMessage(5, "玩家VIP: " + victim.getVip() + " 玩家贊助: " + victim.getDonate() + " 玩家權限: " + victim.getGMLevel() + " 等級: " + victim.getLevel() + " 職業: " + victim.getJob() + " 名聲: " + victim.getFame());
                    c.getPlayer().dropMessage(5, "地圖: " + victim.getMapId() + " - " + victim.getMap().getMapName());
                    c.getPlayer().dropMessage(5, "目前HP: " + victim.getStat().getHp() + " 目前MP: " + victim.getStat().getMp());
                    c.getPlayer().dropMessage(5, "最大HP: " + victim.getStat().getCurrentMaxHp() + " 最大MP: " + victim.getStat().getCurrentMaxMp());
                    c.getPlayer().dropMessage(5, "力量: " + victim.getStat().getStr() + "  ||  敏捷: " + victim.getStat().getDex() + "  ||  智力: " + victim.getStat().getInt() + "  ||  幸運: " + victim.getStat().getLuk());
                    c.getPlayer().dropMessage(5, "物理攻擊: " + victim.getStat().getTotalWatk() + "  ||  魔法攻擊: " + victim.getStat().getTotalMagic());
                    //       c.getPlayer().dropMessage(5, "物理攻擊: " + victim.getStat().getTotalWatk() + "  ||  魔法攻擊: " + victim.getStat().getTotalMagic());
                    c.getPlayer().dropMessage(5, "經驗倍率: " + victim.getStat().expBuff + " 金錢倍率: " + victim.getStat().mesoBuff + " 掉寶倍率: " + victim.getStat().dropBuff);
                    c.getPlayer().dropMessage(5, "GASH: " + victim.getOfflineAcash(victim) + " 楓葉點數: " + victim.getCSPoints(2) + " 楓幣: " + victim.getMeso() + " 倉庫楓幣 " + mesoInStorage);
                    if (ch <= 0 && CashShopServer.getPlayerStorage().getCharacterByName(name) == null) {
                        c.getPlayer().dropMessage(5, "該角色為離線狀態");
                    } else {
                        if (ch <= 0) {
                            victim = CashShopServer.getPlayerStorage().getCharacterByName(name);
                        }
                        c.getPlayer().dropMessage(5, "IP:" + victim.getClient().getSessionIPAddress() + " 目前MAC:" + victim.getNowMacs() + " 所有MAC:" + victim.getClient().getMacs());
                        c.getPlayer().dropMessage(5, "對伺服器延遲: " + victim.getClient().getLatency());
                    }
                } else {
                    c.getPlayer().dropMessage(5, "找不到此玩家.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名字> - 觀察玩家";
        }
    }

    public static class SpyID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            } else {
                int id = 0;
                try {
                    id = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {
                }
                MapleCharacter victim = MapleCharacter.getCharacterById(id);
                int ch = World.Find.findChannel(id);
                if (victim != null) {
                    if (victim.getGMLevel() > c.getPlayer().getGMLevel()) {
                        c.getPlayer().dropMessage(5, "你不能查看比你高權限的人!");
                    } else {
                        int mesoInStorage = 0;
                        try {
                            if (victim.getStorage() != null) {
                                mesoInStorage = victim.getStorage().getMeso();
                            }
                        } catch (Exception ex) {
                        }
                        c.getPlayer().dropMessage(5, "此玩家狀態:");
                        c.getPlayer().dropMessage(5, "角色伺服器:" + victim.getWorld());
                        c.getPlayer().dropMessage(5, "玩家名稱: " + victim.getName() + " 玩家編號: " + victim.getId() + " 帳號: " + victim.getClient().getAccountName() + " 帳號ID: " + victim.getAccountID());
                        c.getPlayer().dropMessage(5, "玩家VIP: " + victim.getVip() + " 玩家權限: " + victim.getGMLevel() + " 等級: " + victim.getLevel() + " 職業: " + victim.getJob() + " 名聲: " + victim.getFame());
                        c.getPlayer().dropMessage(5, "地圖: " + victim.getMapId() + " - " + victim.getMap().getMapName());
                        c.getPlayer().dropMessage(5, "目前HP: " + victim.getStat().getHp() + " 目前MP: " + victim.getStat().getMp());
                        c.getPlayer().dropMessage(5, "最大HP: " + victim.getStat().getCurrentMaxHp() + " 最大MP: " + victim.getStat().getCurrentMaxMp());
                        c.getPlayer().dropMessage(5, "力量: " + victim.getStat().getStr() + "  ||  敏捷: " + victim.getStat().getDex() + "  ||  智力: " + victim.getStat().getInt() + "  ||  幸運: " + victim.getStat().getLuk());
                        c.getPlayer().dropMessage(5, "物理攻擊: " + victim.getStat().getTotalWatk() + "  ||  魔法攻擊: " + victim.getStat().getTotalMagic());
                        //       c.getPlayer().dropMessage(5, "物理攻擊: " + victim.getStat().getTotalWatk() + "  ||  魔法攻擊: " + victim.getStat().getTotalMagic());
                        c.getPlayer().dropMessage(5, "經驗倍率: " + victim.getStat().expBuff + " 金錢倍率: " + victim.getStat().mesoBuff + " 掉寶倍率: " + victim.getStat().dropBuff);
                        c.getPlayer().dropMessage(5, "GASH: " + victim.getOfflineAcash(victim) + " 楓葉點數: " + victim.getCSPoints(2) + " 楓幣: " + victim.getMeso() + " 倉庫楓幣 " + mesoInStorage);
                        if (ch <= 0 && CashShopServer.getPlayerStorage().getCharacterById(id) == null) {
                            c.getPlayer().dropMessage(5, "該角色為離線狀態");
                        } else {
                            if (ch <= 0) {
                                victim = CashShopServer.getPlayerStorage().getCharacterById(id);
                            }
                            c.getPlayer().dropMessage(5, "IP:" + victim.getClient().getSessionIPAddress() + " 目前MAC:" + victim.getNowMacs() + " 所有MAC:" + victim.getClient().getMacs());
                            c.getPlayer().dropMessage(5, "對伺服器延遲: " + victim.getClient().getLatency());
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(5, "找不到此玩家.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名字> - 觀察玩家";
        }
    }

    public static class Map extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            try {
                MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                if (target == null) {
                    c.getPlayer().dropMessage(5, "地圖不存在.");
                    return true;
                }
                MaplePortal targetPortal = null;
                if (splitted.length > 2) {
                    try {
                        targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                    } catch (IndexOutOfBoundsException e) {
                        // noop, assume the gm didn't know how many portals there are
                        c.getPlayer().dropMessage(5, "傳送點錯誤.");
                    } catch (NumberFormatException a) {
                        // noop, assume that the gm is drunk
                    }
                }
                if (targetPortal == null) {
                    targetPortal = target.getPortal(0);
                }
                c.getPlayer().changeMap(target, targetPortal);
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<mapid|charname> [portal] - 傳送到某地圖/人";
        }
    }

    public static class WarpMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            try {
                final MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                if (target == null) {
                    c.getPlayer().dropMessage(6, "地圖不存在。");
                    return false;
                }
                final MapleMap from = c.getPlayer().getMap();
                for (MapleCharacter chr : from.getCharactersThreadsafe()) {
                    chr.changeMap(target, target.getPortal(0));
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<地圖代碼> - 把地圖上的人全部傳到那張地圖";
        }
    }

    public static class AttDebug extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setAttackDebugMessage(!c.getPlayer().getAttackDebugMessage());
            c.getPlayer().dropMessage(6, "目前攻擊速度調試訊息 : " + c.getPlayer().getAttackDebugMessage());
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟Debug訊息";
        }
    }

    public static class WDebug extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setWalkDebugMessage(!c.getPlayer().getWalkDebugMessage());
            c.getPlayer().dropMessage(6, "目前走路調試訊息 : " + c.getPlayer().getWalkDebugMessage());
            return true;
        }

        @Override
        public String getHelp() {
            return " - 開啟/關閉走路調試訊息";
        }

    }

    public static class Debug extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().setDebugMessage(!c.getPlayer().getDebugMessage());
            c.getPlayer().dropMessage("DeBug訊息已經" + (c.getPlayer().getDebugMessage() ? "開啟" : "關閉"));
            return true;
        }

        public String getHelp() {
            return "- 開啟Debug訊息";
        }
    }

    public static class Quest extends 任務 {

        @Override
        public String getHelp() {
            return "- 查看任務狀態";
        }
    }

    public static class 任務 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int type = 5;
            java.util.Map<MapleQuest, MapleQuestStatus> quests = c.getPlayer().getQuest_Map();
            for (MapleQuestStatus q : quests.values()) {
                if (q.getStatus() != 2 && !q.getQuest().getName().isEmpty()) {
                    String qname = q.getQuest().getName();
                    String qstatus = q.getStatus() == 1 ? "進行中" : "未知 " + q.getStatus();
                    String qfinsh = q.getQuest().canComplete(c.getPlayer(), null) ? "是" : "否";
                    String akillmob = q.hasMobKills() ? "是" : "否";
                    String aSkill = q.getQuest().getSkillID() > 0 ? "是" : "否";
                    c.getPlayer().dropMessage(type, "任務[" + qname + "] 狀態[" + qstatus + "] 可完成[" + qfinsh + "] 需要打怪[" + akillmob + "] 指定技能[" + aSkill + "]");
                }
            }
            return true;
        }

        public String getHelp() {
            return "- 查看當前任務狀態";
        }
    }

    public static class CharInfo extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {

            if (splitted.length < 2) {
                return false;
            }
            final StringBuilder builder = new StringBuilder();
            MapleCharacter other;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "玩家必須上線");
                return true;
            }
            other = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

            if (other == null) {
                builder.append("角色不存在");
                c.getPlayer().dropMessage(6, builder.toString());
            } else {
                if (other.getClient().getLastPing() <= 0) {
                    other.getClient().sendPing();
                }
                builder.append(MapleClient.getLogMessage(other, ""));
                builder.append(" 在 ").append(other.getPosition().x);
                builder.append(" /").append(other.getPosition().y);

                builder.append(" || 血量 : ");
                builder.append(other.getStat().getHp());
                builder.append(" /");
                builder.append(other.getStat().getCurrentMaxHp());

                builder.append(" || 魔量 : ");
                builder.append(other.getStat().getMp());
                builder.append(" /");
                builder.append(other.getStat().getCurrentMaxMp());

                builder.append(" || 物理攻擊力 : ");
                builder.append(other.getStat().getTotalWatk());
                builder.append(" || 魔法攻擊力 : ");
                builder.append(other.getStat().getTotalMagic());
                builder.append(" || 最高攻擊 : ");
                builder.append(other.getStat().getCurrentMaxBaseDamage());
                builder.append(" || 攻擊%數 : ");
                builder.append(other.getStat().dam_r);
                builder.append(" || BOSS攻擊%數 : ");
                builder.append(other.getStat().bossdam_r);

                builder.append(" || 力量 : ");
                builder.append(other.getStat().getStr());
                builder.append(" || 敏捷 : ");
                builder.append(other.getStat().getDex());
                builder.append(" || 智力 : ");
                builder.append(other.getStat().getInt());
                builder.append(" || 幸運 : ");
                builder.append(other.getStat().getLuk());

                builder.append(" || 全部力量 : ");
                builder.append(other.getStat().getTotalStr());
                builder.append(" || 全部敏捷 : ");
                builder.append(other.getStat().getTotalDex());
                builder.append(" || 全部智力 : ");
                builder.append(other.getStat().getTotalInt());
                builder.append(" || 全部幸運 : ");
                builder.append(other.getStat().getTotalLuk());

                builder.append(" || 經驗值 : ");
                builder.append(other.getExp());

                builder.append(" || 組隊狀態 : ");
                builder.append(other.getParty() != null);

                builder.append(" || 交易狀態: ");
                builder.append(other.getTrade() != null);
                builder.append(" || Latency: ");
                builder.append(other.getClient().getLatency());
                builder.append(" || 最後PING: ");
                builder.append(other.getClient().getLastPing());
                builder.append(" || 最後PONG: ");
                builder.append(other.getClient().getLastPong());
                builder.append(" || IP: ");
                builder.append(other.getClient().getSessionIPAddress());
                other.getClient().DebugMessage(builder);

                c.getPlayer().dropMessage(6, builder.toString());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<角色名稱> - 查看角色狀態";

        }
    }

    public static class Cheaters extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            List<CheaterData> cheaters = World.getCheaters();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看作弊角色";

        }
    }

    public static class CheckItem extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                return false;
            } else {
                int item = Integer.parseInt(splitted[2]);
                MapleCharacter chr;
                String name = splitted[1];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(name);
                if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "玩家必須上線");
                    return true;
                }
                chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

                int itemamount = chr.getItemQuantity(item, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(6, chr.getName() + " 有 " + itemamount + " (" + item + ").");
                } else {
                    c.getPlayer().dropMessage(6, chr.getName() + " 並沒有 (" + item + ")");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<playername> <itemid> - 檢查物品";
        }
    }

    public static class CheckGash extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter chrs;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "玩家必須上線");
                return true;
            }
            chrs = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (chrs == null) {
                c.getPlayer().dropMessage(5, "找不到該角色");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " 有 " + chrs.getCSPoints(1) + " 點數.");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> - 檢查點數";
        }
    }

    public static class Connected extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            java.util.Map<Integer, Integer> connected = World.getConnected();
            StringBuilder conStr = new StringBuilder("已連接的客戶端: ");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    conStr.append(", ");
                } else {
                    first = false;
                }
                if (i == 0) {
                    conStr.append("所有: ");
                    conStr.append(connected.get(i));
                } else {
                    conStr.append("頻道 ");
                    conStr.append(i);
                    conStr.append(": ");
                    conStr.append(connected.get(i));
                }
            }
            c.getPlayer().dropMessage(6, conStr.toString());
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看已連線的客戶端";
        }
    }

    public static class MapOn extends CommandExecute {

        public boolean execute(MapleClient c, String[] splitted) {
            int mapid = 0;
            String input = null;
            MapleMap map = null;
            if (splitted.length < 2) {
                return false;
            }
            try {
                input = splitted[1];
                mapid = Integer.parseInt(input);
            } catch (Exception ex) {
            }
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                cserv.getMapFactory().HealMap(mapid);
            }
            return true;

        }

        @Override
        public String getHelp() {
            return "- 開放地圖";
        }
    }

    public static class MapOff extends CommandExecute {

        public boolean execute(MapleClient c, String[] splitted) {
            int mapid = 0;
            String input = null;
            MapleMap map = null;
            if (splitted.length < 2) {
                return false;
            }
            try {
                input = splitted[1];
                mapid = Integer.parseInt(input);
            } catch (Exception ex) {
            }
            if (c.getChannelServer().getMapFactory().getMap(mapid) == null) {
                c.getPlayer().dropMessage("地圖不存在");
                return true;
            }
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                cserv.getMapFactory().destroyMap(mapid, true);
            }
            return true;

        }

        public String getHelp() {
            return "- 關閉地圖";
        }
    }

    public static class NPCOff extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int npcid = -1;
            try {
                npcid = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            if (npcid == -1) {
                c.getPlayer().dropMessage("代碼為" + npcid + "的Npc不存在");
                return true;
            }
            GameConstants.addBlockedNpc(npcid);
            c.getPlayer().dropMessage("代碼為" + npcid + "的Npc已經關閉");

            return true;
        }

        @Override
        public String getHelp() {
            return "<Npc代碼> - 關閉Npc";
        }
    }

    public static class NPCOn extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int npcid = -1;
            try {
                npcid = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            if (npcid == -1) {
                c.getPlayer().dropMessage("代碼為" + npcid + "的Npc不存在");
                return true;
            }
            GameConstants.removeBlockedNpc(npcid);
            c.getPlayer().dropMessage("代碼為" + npcid + "的Npc已經開放");

            return true;
        }

        @Override
        public String getHelp() {
            return "<Npc代碼> - 開放Npc";
        }
    }
}
