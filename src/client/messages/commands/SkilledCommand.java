package client.messages.commands;

import client.*;
import client.inventory.Equip;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.messages.CommandExecute;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.PiPiConfig;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Windyboy
 */
public class SkilledCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.老實習生;
    }

    public static class WarpT extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            List<MapleCharacter> chrs = new LinkedList<>();
            List<MapleCharacter> chrs_cs = new LinkedList<>();
            String input = splitted[1].toLowerCase();
            MapleCharacter smart_victim = null;
            StringBuilder sb = new StringBuilder();
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    String name = chr.getName().toLowerCase();
                    if (name.contains(input)) {
                        if (smart_victim == null) {
                            smart_victim = chr;
                        }
                        chrs.add(chr);
                    }
                }
            }

            for (MapleCharacter chr : CashShopServer.getPlayerStorage().getAllCharactersThreadSafe()) {
                String name = chr.getName().toLowerCase();
                if (name.contains(input)) {
                    if (smart_victim == null) {
                        smart_victim = chr;
                    }
                    chrs_cs.add(chr);
                }
            }

            if (chrs.size() > 1 || chrs_cs.size() > 1) {
                sb.append("尋找到的玩家共").append(chrs.size() + chrs_cs.size()).append("位 名單如下 : ");
                c.getPlayer().dropMessage(5, sb.toString());
                for (MapleCharacter list : chrs) {
                    c.getPlayer().dropMessage(5, "頻道" + list.getClient().getChannel() + ": " + list.getName() + "(" + list.getId() + ") -- " + list.getMapId() + "(" + list.getMap().getMapName() + ")");
                }
                for (MapleCharacter list : chrs_cs) {
                    c.getPlayer().dropMessage(5, "頻道-10: " + list.getName() + "(" + list.getId() + ") -- " + list.getMapId() + "(商城內)");
                }
                return true;
            } else if (chrs.isEmpty()) {
                c.getPlayer().dropMessage(6, "沒有搜尋到名稱含有 '" + input + "' 的角色");
            } else if (smart_victim != null) {
                c.getPlayer().changeMap(smart_victim.getMap(), smart_victim.getMap().findClosestSpawnpoint(smart_victim.getTruePosition()));
            } else {
                c.getPlayer().dropMessage(6, "角色不存在或是不在線上");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱片段> - 移動到某個地圖或某個玩家所在的地方";
        }
    }

    public static class Ban extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            return "Ban";
        }

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            StringBuilder sb = new StringBuilder(c.getPlayer().getName());
            sb.append(" 封鎖 ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
            boolean offline = false;
            MapleCharacter target = null;
            String name = "";
            String input = "null";
            try {
                name = splitted[1];
                input = splitted[2];
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch >= 1) {
                target = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            } else {
                target = CashShopServer.getPlayerStorage().getCharacterByName(name);
            }
            if (target == null) {
                if (c.getPlayer().OfflineBanByName(name, sb.toString())) {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 成功離線封鎖 " + splitted[1] + ".");
                    offline = true;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 封鎖失敗 " + splitted[1]);
                    return true;
                }
            } else if (Ban(c, target, sb) != 1) {
                return true;
            }
            FileoutputUtil.logToFile("logs/Hack/指令封鎖名單.txt", "\r\n " + FileoutputUtil.NowTime() + " " + c.getPlayer().getName() + " 封鎖了 " + splitted[1] + " 原因: " + sb.toString() + " 是否離線封鎖: " + offline);
            String reason = "null".equals(input) ? "使用違法程式練功" : StringUtil.joinStringFrom(splitted, 2);
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封鎖系統] " + splitted[1] + " 因為" + reason + "而被管理員永久停權。"));

            String msg = "[GM 密語] GM " + c.getPlayer().getName() + "  封鎖了 " + splitted[1] + " 是否離線封鎖 " + offline + " 原因：" + reason;
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家> <原因> - 封鎖玩家";
        }

        public int Ban(MapleClient c, MapleCharacter target, StringBuilder sb) {
            if (c.getPlayer().getGMLevel() >= target.getGMLevel()) {
                sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                if (target.ban(sb.toString(), c.getPlayer().hasGmLevel(5), false, hellban)) {
                    target.getClient().getSession().close();
                    target.getClient().disconnect(true, target.getClient().getChannel() == -10);
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 成功封鎖 " + target.getName() + ".");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 封鎖失敗.");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 無法封鎖GMs...");
                return 0;
            }
        }
    }

    public static class BanID extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            return "Ban";
        }

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            StringBuilder sb = new StringBuilder(c.getPlayer().getName());
            sb.append(" 封鎖 ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
            boolean offline = false;
            boolean ban = false;
            MapleCharacter target;
            int id = 0;
            String input = "null";
            try {
                id = Integer.parseInt(splitted[1]);
                input = splitted[2];
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(id);
            int wl = World.Find.findWorld(id);
            String name = c.getPlayer().getCharacterNameById(id);
            target = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(id);
            if (target == null) {
                target = CashShopServer.getPlayerStorage().getCharacterById(id);
                if (target == null) {
                    if (c.getPlayer().OfflineBanById(id, sb.toString())) {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] 成功離線封鎖 " + name + ".");
                        offline = true;
                    } else {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] 封鎖失敗 " + splitted[1]);
                        return true;
                    }
                } else if (Ban(c, target, sb) != 1) {
                    return true;
                }
            } else {
                if (Ban(c, target, sb) != 1) {
                    return true;
                }
                name = target.getName();
            }

            FileoutputUtil.logToFile("logs/Hack/指令封鎖名單.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " " + c.getPlayer().getName() + " 封鎖了 " + name + " 原因: " + sb.toString() + " 是否離線封鎖: " + offline);
            String reason = "null".equals(input) ? "使用違法程式練功" : StringUtil.joinStringFrom(splitted, 2);
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封鎖系統] " + name + " 因為" + reason + "而被管理員永久停權。"));

            String msg = "[GM 密語] GM " + c.getPlayer().getName() + "  封鎖了 " + name + " 是否離線封鎖 " + offline + " 原因：" + reason;
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家ID> <原因> - 封鎖玩家";
        }

        public int Ban(MapleClient c, MapleCharacter target, StringBuilder sb) {
            if (c.getPlayer().getGMLevel() >= target.getGMLevel()) {
                sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                if (target.ban(sb.toString(), c.getPlayer().hasGmLevel(5), false, hellban)) {
                    target.getClient().getSession().close();
                    target.getClient().disconnect(true, target.getClient().getChannel() == -10);
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 成功封鎖 " + target.getName() + ".");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 封鎖失敗.");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 無法封鎖GMs...");
                return 0;
            }
        }
    }

    public static class CnGM extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getErrorNotice("<GM聊天視窗>" + "頻道" + c.getPlayer().getClient().getChannel() + " [" + c.getPlayer().getName() + "](" + c.getPlayer().getId() + ") : " + StringUtil.joinStringFrom(splitted, 1)));
            return true;
        }

        @Override
        public String getHelp() {
            return "<訊息> - GM聊天";
        }
    }

    public static class MobSize extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int size = c.getPlayer().getMap().mobCount();
            c.getPlayer().dropMessage(5, "當前地圖怪物數量總共有" + size + "隻");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看當前地圖總共的怪物數量";
        }
    }

    public static class Hide extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(9001004).getEffect(1).applyTo(c.getPlayer());
            c.getPlayer().dropMessage(6, "管理員隱藏 = 開啟 \r\n 解除請輸入!unhide");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 隱藏";
        }
    }

    public static class UnHide extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().dispelBuff(9001004);
            c.getPlayer().dropMessage(6, "管理員隱藏 = 關閉 \r\n 開啟請輸入!hide");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 解除隱藏";
        }
    }

    public static class 精靈商人訊息 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter p = c.getPlayer();
            boolean x = p.getmsg_HiredMerchant();
            if (x) {
                p.setmsg_HiredMerchant(false);
            } else {
                p.setmsg_HiredMerchant(true);
            }
            x = p.getmsg_HiredMerchant();
            p.dropMessage("目前精靈商人購買訊息狀態: " + (x ? "開啟 " : " 關閉 ") + "");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟精靈商人購買訊息顯示";
        }
    }

    public static class 玩家私聊 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter p = c.getPlayer();
            boolean x = p.getmsg_Chat();
            if (x) {
                p.setmsg_Chat(false);
            } else {
                p.setmsg_Chat(true);
            }
            x = p.getmsg_Chat();
            p.dropMessage("目前玩家私聊狀態: " + (x ? "開啟 " : "關閉 ") + "");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟玩家訊息顯示";
        }
    }

    public static class Online extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            Integer job = CommandProcessorUtil.getNamedIntArg(splitted, 1, "job");
            Integer job2 = CommandProcessorUtil.getNamedIntArg(splitted, 1, "job2");
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lv");
            Integer level2 = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lv2");
            Integer meso = CommandProcessorUtil.getNamedIntArg(splitted, 1, "meso");
            Integer meso2 = CommandProcessorUtil.getNamedIntArg(splitted, 1, "meso2");
            String name = CommandProcessorUtil.getNamedStringArg(splitted, 1, "name");
            boolean showAllChannel = false;
            boolean level_limit = false;
            boolean job_limit = false;
            boolean meso_limit = false;
            boolean name_limit = false;
            int total = 0;
            int curConnected = c.getChannelServer().getConnectedClients();

            if (job != null && job2 != null) {
                job_limit = true;
                showAllChannel = true;
            }
            if (level != null && level2 != null) {
                level_limit = true;
                showAllChannel = true;
            }
            if (meso != null && meso2 != null) {
                meso_limit = true;
                showAllChannel = true;
            }
            if (name != null) {
                name_limit = true;
                showAllChannel = true;
            }

            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");
            c.getPlayer().dropMessage(6, new StringBuilder().append("頻道: ").append(c.getChannelServer().getChannel()).append(" 線上人數: ").append(curConnected).toString());
            total += curConnected;
            if (!showAllChannel) {
                for (World wlerv : LoginServer.getWorlds()) {
                    for (ChannelServer cserv : wlerv.getChannels()) {
                        if (cserv.getChannel() != c.getChannel()) {
                            continue;
                        }
                        if (!cserv.getPlayerStorage().getAllCharactersThreadSafe().isEmpty()) {
                            c.getPlayer().dropMessage(5, "<頻道" + cserv.getChannel() + ">");
                        }
                        for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                            if (chr == null) {
                                continue;
                            }
                            if (chr.getGMLevel() > c.getPlayer().getGMLevel()) {
                                continue;
                            }
                            StringBuilder ret = new StringBuilder();
                            ret.append("名稱 ");
                            ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                            ret.append(" ID: ");
                            ret.append(StringUtil.getRightPaddedStr(chr.getId() + "", ' ', 5));
                            ret.append(" 等級: ");
                            ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                            ret.append(" 職業: ");
                            ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getJob()), ' ', 4));
                            if (chr.getMap() != null) {
                                ret.append(" 地圖: ");
                                ret.append(chr.getMapId());
                                ret.append(" (");
                                ret.append(chr.getMap().getMapName());
                                ret.append(") ");
                            }
                            c.getPlayer().dropMessage(6, ret.toString());
                        }
                    }
                }
            } else {
                for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                        if (chr == null) {
                            continue;
                        } else if (job_limit && (chr.getJob() < job || chr.getJob() > job2)) {
                            continue;
                        } else if (level_limit && (chr.getLevel() < level || chr.getLevel() > level2)) {
                            continue;
                        } else if (meso_limit && (chr.getMeso() < meso || chr.getMeso() > meso)) {
                            continue;
                        } else if (name_limit && name != null && (chr.getName().toLowerCase().contains(name.toLowerCase()))) {
                            continue;
                        }

                        StringBuilder ret = new StringBuilder();
                        ret.append("名稱 ");
                        ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                        ret.append(" ID: ");
                        ret.append(StringUtil.getRightPaddedStr(chr.getId() + "", ' ', 5));
                        ret.append(" 等級: ");
                        ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                        ret.append(" 職業: ");
                        ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getJob()), ' ', 4));
                        if (meso_limit) {
                            ret.append(" 楓幣: ");
                            ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getMeso()), ' ', 10));
                            c.getPlayer().dropMessage(6, ret.toString());
                        } else if (chr.getMap() != null) {
                            ret.append(" 地圖: ");
                            ret.append(chr.getMapId());
                            c.getPlayer().dropMessage(6, ret.toString());
                        }
                    }
                }
            }
            c.getPlayer().dropMessage(6, new StringBuilder().append("當前頻道總計線上人數: ").append(total).toString());
            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");

            int totalOnline = 0;
            /*伺服器總人數*/
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                totalOnline += cserv.getConnectedClients();
            }

            c.getPlayer().dropMessage(6, new StringBuilder().append("當前伺服器總計線上人數: ").append(totalOnline).append("個").toString());
            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");

            int WorldOnline = 0;
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                WorldOnline += cserv.getConnectedClients();
            }
            c.getPlayer().dropMessage(6, "目前世界人數: " + WorldOnline);

            int onlineInCS = 0;
            if (CashShopServer.getPlayerStorage() != null) {
                onlineInCS += CashShopServer.getPlayerStorage().getConnectedClients();
            }
            c.getPlayer().dropMessage(6, "目前商城人數: " + onlineInCS);
            int allonline = onlineInCS;
            for (World wlerv : LoginServer.getWorlds()) {
                for (ChannelServer cserv : wlerv.getChannels()) {
                    allonline += cserv.getPlayerStorage().getAllCharactersThreadSafe().size();
                }
            }

            c.getPlayer().dropMessage(6, "目前總世界線上人數: " + allonline);
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看線上人數";
        }
    }

    public static class OnlineGM extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            //c.getPlayer().cancelGmLevel();
            c.disconnect(true, false);
            if (c.getPlayer().getGMLevel() < 0)
                return true;

            int channelOnline = 0;
            int totalOnline = 0;
            int GmInChannel = 0;
            List<MapleCharacter> chrs = new LinkedList<>();

            /*當前頻道總GM數*/
            for (MapleCharacter chr : c.getChannelServer().getPlayerStorage().getAllCharactersThreadSafe()) {
                if (chr.getGMLevel() > 0) {
                    channelOnline++;
                }
            }
            /*伺服器總GM數*/
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    if (chr != null && chr.getGMLevel() > 0) {
                        totalOnline++;
                    }
                }
            }
            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    if (chr != null && chr.getGMLevel() > 0) {
                        chrs.add(chr);
                    }
                }
                GmInChannel = chrs.size();
                if (GmInChannel > 0) {
                    c.getPlayer().dropMessage(6, new StringBuilder().append("頻道: ").append(cserv.getChannel()).append(" 線上GM人數: ").append(GmInChannel).toString());
                    for (MapleCharacter chr : chrs) {
                        if (chr != null) {
                            StringBuilder ret = new StringBuilder();
                            ret.append(" GM暱稱 ");
                            ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                            ret.append(" ID: ");
                            ret.append(StringUtil.getRightPaddedStr(chr.getId() + "", ' ', 5));
                            ret.append(" 權限: ");
                            ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getGMLevel()), ' ', 3));
                            c.getPlayer().dropMessage(6, ret.toString());
                        }
                    }
                }
                chrs = new LinkedList<>();
            }
            c.getPlayer().dropMessage(6, new StringBuilder().append("當前頻道總計GM線上人數: ").append(channelOnline).toString());
            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");

            c.getPlayer().dropMessage(6, new StringBuilder().append("當前伺服器GM總計線上人數: ").append(totalOnline).append("個").toString());
            c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看線上人數GM";
        }
    }

    public static class WarpHere extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                int wl = World.Find.findWorld(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "找不到");

                } else {
                    victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(splitted[1]);
                    c.getPlayer().dropMessage(5, "正在把玩家傳到這來");
                    victim.dropMessage(5, "正在傳送到GM那邊");
                    if (victim.getMapId() != c.getPlayer().getMapId()) {
                        final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                        victim.changeMap(mapp, mapp.getPortal(0));
                    }
                    victim.changeChannel(c.getChannel());
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "- 把玩家傳送到這裡";
        }
    }

    public static class WhoIsHere extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            StringBuilder builder = new StringBuilder("在此地圖的玩家: ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return true;
        }

        @Override
        public String getHelp() {
            return "- 查看目前地圖上的玩家";

        }
    }
    public static class AutoLogin extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter p = c.getPlayer();
            p.changeShow(2);
            boolean x = p.getShow(2);
            p.dropMessage("目前登入顯示狀態: " + (x ? "開啟 " : "關閉 ") + "");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟玩家登入訊息顯示";
        }
    }

    public static class AutoRegister extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter p = c.getPlayer();
            p.changeShow(1);
            boolean x = p.getShow(1);
            p.dropMessage("目前註冊顯示狀態: " + (x ? "開啟 " : "關閉 ") + "");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟玩家註冊訊息顯示";
        }
    }

    public static class 吸怪訊息 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter p = c.getPlayer();
            boolean x = p.getmsg_MobVac();
            if (x) {
                p.setmsg_MobVac(false);
            } else {
                p.setmsg_MobVac(true);
            }
            x = p.getmsg_MobVac();
            p.dropMessage("目前吸怪訊息狀態: " + (x ? "開啟 " : "關閉 ") + "");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 開啟玩家吸怪訊息訊息顯示";
        }
    }

    public static class UnHellBan extends UnBan {

        public UnHellBan() {
            hellban = true;
        }

        @Override
        public String getHelp() {
            return "<玩家> - 解鎖玩家";
        }
    }

    public static class UnBan extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            return "UnBan";
        }

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unHellban(splitted[1]);
            } else {
                ret = MapleClient.unban(splitted[1]);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL 錯誤");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 目標玩家不存在");
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 成功解除鎖定");
            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL 錯誤.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 角色不存在.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] IP或Mac已解鎖其中一個.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] IP以及Mac已成功解鎖.");
            }
            if (ret_ == 1 || ret_ == 2) {
                FileoutputUtil.logToFile("logs/Hack/ban/解除封鎖名單.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " " + c.getPlayer().getName() + " 解鎖了 " + splitted[1]
                );
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家> - 解鎖玩家";
        }
    }

    public static class DCID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            int id = Integer.parseInt(splitted[1]);
            int ch = World.Find.findChannel(id);
            int wl = World.Find.findWorld(id);
            if (ch <= 0 && ch != -10) {
                c.getPlayer().dropMessage("該玩家不在線上");
                return true;
            }
            if (ch == -10) {
                victim = CashShopServer.getPlayerStorage().getCharacterById(id);
            } else {
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(id);
            }
            if (victim != null) {
                victim.getClient().disconnect(true, ch == -10);
                victim.getClient().getSession().close();
            } else {
                c.getPlayer().dropMessage("該玩家不在線上");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家ID> - 讓玩家斷線";
        }
    }

    public static class DC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0 && ch != -10) {
                c.getPlayer().dropMessage("該玩家不在線上");
                return true;
            }
            if (ch == -10) {
                victim = CashShopServer.getPlayerStorage().getCharacterByName(name);
            } else {
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            }
            if (victim != null) {
                victim.getClient().disconnect(true, ch == -10);
                victim.getClient().getSession().close();
            } else {
                c.getPlayer().dropMessage("該玩家不在線上");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家> - 讓玩家斷線";
        }
    }

    public static class DC2 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0 && ch != -10) {
                c.getPlayer().dropMessage("該玩家不在線上");
                return true;
            }
            if (ch == -10) {
                victim = CashShopServer.getPlayerStorage().getCharacterByName(name);
            } else {
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            }
            if (victim != null) {
                victim.getClient().sendPacket(MaplePacketCreator.getNPCTalk(99, (byte) 0, "", "00 00", (byte) 0));
            } else {
                c.getPlayer().dropMessage("該玩家不在線上");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家> - 讓玩家-2147斷線";
        }
    }

    public static class FixChar extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            ChannelServer.forceRemovePlayerByCharName(c, splitted[1]);
            c.getPlayer().dropMessage("已經解卡玩家<" + splitted[1] + ">");
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> - 解卡角";
        }
    }

    public static class FixAcc extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            String input = splitted[1];
            int Accountid = 0;
            int times = 0;

            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, input);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return true;
                    }
                    Accountid = rs.getInt(1);
                }
            } catch (Exception ex) {
                Logger.getLogger(PracticerCommand.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE accountid = ?")) {
                ps.setInt(1, Accountid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return false;
                    }
                    times++;
                    try {
                        ChannelServer.forceRemovePlayerByCharName(c, rs.getString("name"));
                    } catch (Exception ex) {
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(PracticerCommand.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
            if (c != null && c.getPlayer() != null) {
                c.getPlayer().dropMessage("已經解卡玩家<" + splitted[1] + ">帳號內的" + times + "個角色");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> - 解帳號卡角";
        }
    }

    public static class Job extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int jobid = 0;
            try {
                jobid = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
                return false;
            }
            c.getPlayer().changeJob(jobid);
            c.getPlayer().dispelDebuffs();
            return true;
        }

        @Override
        public String getHelp() {
            return "<職業代碼> - 更換職業";
        }
    }

    public static class WhereAmI extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage(5, "目前地圖 " + c.getPlayer().getMap().getId() + "座標 (" + String.valueOf(c.getPlayer().getPosition().x) + " , " + String.valueOf(c.getPlayer().getPosition().y) + ")");
            return true;
        }

        @Override
        public String getHelp() {
            return "- 目前地圖";
        }
    }

    public static class BanStatus extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            String name = splitted[1];
            String mac = "";
            String ip = "";
            int acid = 0, Macs = 0;
            boolean Systemban = false;
            boolean ACbanned = false;
            boolean IPbanned = false;
            boolean npc_show = false;
            String reason = null;

            if (splitted.length > 2) {
                if (splitted[2].equalsIgnoreCase("npc")) {
                    npc_show = true;
                }
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ps = con.prepareStatement("select accountid from characters where name = ?");
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        acid = rs.getInt("accountid");
                    }
                }
                ps = con.prepareStatement("select banned, banreason, macs, Sessionip from accounts where id = ?");
                ps.setInt(1, acid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Systemban = rs.getInt("banned") == 2;
                        ACbanned = rs.getInt("banned") == 1 || rs.getInt("banned") == 2;
                        reason = rs.getString("banreason");
                        mac = rs.getString("macs");
                        ip = rs.getString("Sessionip");
                    }
                }
                ps.close();
            } catch (Exception e) {
            }
            if (reason == null || reason.isEmpty()) {
                reason = "無";
            }
            if (c.isBannedIP(ip)) {
                IPbanned = true;
            }

            String msg1 = "玩家[" + name + "] 帳號ID[" + acid + "]是否被封鎖: " + (ACbanned ? "是" : "否") + (Systemban ? "(系統自動封鎖)" : "") + ", 原因: " + reason;
            String msg2 = "目前IP: " + ip + " 是否在封鎖IP名單: " + (IPbanned ? "是" : "否");
            String msg3 = "";
            if (npc_show) {
                if (mac.contains(",")) {
                    for (String SingleMac : mac.split(", ")) {
                        msg3 += "MAC: " + SingleMac + " 在封鎖MAC名單: " + (c.isBannedMac(SingleMac) ? "是" : "否") + "\r\n";
                    }
                } else {
                    msg3 += "MAC: " + mac + " 在封鎖MAC名單: " + (c.isBannedMac(mac) ? "是" : "否") + "\r\n";
                }
                c.getPlayer().dropNPC(msg1 + "\r\n" + msg2 + "\r\n" + msg3);
            } else {
                c.getPlayer().dropMessage(msg1);
                c.getPlayer().dropMessage(msg2);
                if (mac.contains(",")) {
                    for (String SingleMac : mac.split(", ")) {
                        msg3 = "MAC: " + SingleMac + " 在封鎖MAC名單: " + (c.isBannedMac(SingleMac) ? "是" : "否");
                        c.getPlayer().dropMessage(msg3);
                    }
                } else {
                    msg3 = "MAC: " + mac + " 在封鎖MAC名單: " + (c.isBannedMac(mac) ? "是" : "否");
                    c.getPlayer().dropMessage(msg3);
                }
            }

            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> <npc> - 查看玩家是否被封鎖及原因";
        }
    }

    public static class BanMAC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            String mac = splitted[1];
            if (mac.equalsIgnoreCase("00-00-00-00-00-00") || mac.length() != 17) {
                c.getPlayer().dropMessage("封鎖MAC失敗，可能為格式錯誤或是長度錯誤 Ex: 00-00-00-00-00-00 ");
                return true;
            }
            c.getPlayer().dropMessage("封鎖MAC [" + mac + "] 成功");
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
                ps.setString(1, mac);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                System.err.println("Error banning MACs" + e);
                return true;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<MAC> - 封鎖MAC ";
        }
    }

    public static class BanIP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            boolean error = false;
            String IP = splitted[1];
            if (!IP.contains("/") || !IP.contains(".")) {
                c.getPlayer().dropMessage("輸入IP必須包含 '/' 以及 '.' 例如: !banIP /127.0.0.1");
                return true;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ps = con.prepareStatement("INSERT INTO ipbans (ip) VALUES (?)");
                ps.setString(1, IP);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ex) {
                error = true;
            }
            try {
                for (ChannelServer cs : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharactersThreadSafe()) {
                        if (chr.getClient().getSessionIPAddress().equals(IP)) {
                            if (!chr.getClient().isGm()) {
                                chr.getClient().disconnect(true, chr.getClient().getChannel() == -10);
                                chr.getClient().getSession().close();
                            }
                        }
                    }
                }
            } catch (Exception ex) {

            }
            c.getPlayer().dropMessage("封鎖IP [" + IP + "] " + (error ? "成功 " : "失敗"));
            return true;
        }

        @Override
        public String getHelp() {
            return "<IP> - 封鎖IP ";
        }
    }

    public static class OnlineDelay extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            List<MapleCharacter> allchrs = new LinkedList<>();
            boolean allch = false;

            if (splitted.length > 1) {
                if (splitted[1].equalsIgnoreCase("all")) {
                    allch = true;
                }
            }

            if (allch) {
                for (World wl : LoginServer.getWorlds()) {
                    for (ChannelServer cs :wl.getChannels()) {
                        allchrs.addAll(cs.getPlayerStorage().getAllCharactersThreadSafe());
                    }
                }
                ChannelServer.getAllInstances().forEach((cserv) -> {
                    allchrs.addAll(cserv.getPlayerStorage().getAllCharactersThreadSafe());
                });
            } else {
                allchrs.addAll(c.getChannelServer().getPlayerStorage().getAllCharactersThreadSafe());
            }
            StringBuilder allret = new StringBuilder();
            for (MapleCharacter chr : allchrs) {
                if (chr.getClient().getLatency() == 0) {
                    continue;
                }
                StringBuilder ret = new StringBuilder();
                ret.append(" 名稱 ");
                ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                ret.append(" 等級: ");
                ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                ret.append(" 延遲: ");
                ret.append(chr.getClient().getLatency());
                ret.append("ms");
                allret.append(FileoutputUtil.CurrentReadable_Time()).append(" ").append(ret).append("\r\n");
                c.getPlayer().dropMessage(6, ret.toString());
            }
            FileoutputUtil.logToFile("logs/data/Delay.txt", allret.toString());

            return true;
        }

        @Override
        public String getHelp() {
            return "[all] - 查看當前/全部頻道的玩家延遲";
        }
    }

    public static class CItem extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }

            int itemId = 0;
            try {
                itemId = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {

            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " - 物品不存在");
            } else if (!ii.isCash(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " - 物品無法叫出");
            } else if (GameConstants.isEquip(itemId) && ii.isCash(itemId)) {
                IItem item = null;
                byte flag = 0;
                flag |= ItemFlag.LOCK.getValue();
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                    item.setFlag(flag);
                }
                if (item != null) {
                    item.setOwner(c.getPlayer().getName());
                    item.setGMLog(c.getPlayer().getName());
                    MapleInventoryManipulator.addbyItem(c, item);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<道具ID> - 取得點數裝備";
        }
    }

    public static class Level extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int level = c.getPlayer().getLevel();
            try {
                level = Short.parseShort(splitted[1]);
            } catch (Exception ex) {

            }
            c.getPlayer().setLevel((short) level);
            c.getPlayer().updateSingleStat(MapleStat.LEVEL, level);
            c.getPlayer().setExp(0);
            return true;
        }

        @Override
        public String getHelp() {
            return "<等級> - 改變等級";
        }
    }

    public static class 黑單 extends FakeReport {

        @Override
        public String getHelp() {
            return "<玩家名稱> - 將玩家設定為無法回報的黑名單";
        }
    }

    public static class FakeReport extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            String input = splitted[1];
            int ch = World.Find.findChannel(input);
            int wl = World.Find.findWorld(input);
            if (ch <= 0) {
                c.getPlayer().dropMessage("玩家[" + input + "]不在線上");
                return true;
            }
            MapleCharacter target = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(input);
            if (target.isGM()) {
                c.getPlayer().dropMessage(1, "無法黑單GM唷");
                return true;
            }
            int accID = target.getAccountID();
            PiPiConfig.setBlackList(accID, input);
            String msg = "[GM 密語] GM " + c.getPlayer().getName() + " 在回報系統黑單了 " + input;
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            FileoutputUtil.logToFile("logs/data/玩家回報黑單.txt", "\r\n  " + FileoutputUtil.NowTime() + " GM " + c.getPlayer().getName() + " 在回報系統黑單了 " + input);
            return true;
        }

        @Override
        public String getHelp() {
            return "- 將玩家設定為無法回報的黑名單";
        }
    }

    public static class Heal extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getStat().setHp(c.getPlayer().getStat().getCurrentMaxHp());
            c.getPlayer().getStat().setMp(c.getPlayer().getStat().getCurrentMaxMp());
            c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getStat().getCurrentMaxHp());
            c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getStat().getCurrentMaxMp());
            c.getPlayer().dispelDebuffs();
            return true;
        }

        @Override
        public String getHelp() {
            return "- 補滿血魔";
        }
    }

    public static class HealMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    mch.getStat().setHp(mch.getStat().getCurrentMaxHp());
                    mch.updateSingleStat(MapleStat.HP, mch.getStat().getCurrentMaxHp());
                    mch.getStat().setMp(mch.getStat().getCurrentMaxMp());
                    mch.updateSingleStat(MapleStat.MP, mch.getStat().getCurrentMaxMp());
                    mch.dispelDebuffs();
                }
            }
            return true;

        }

        @Override
        public String getHelp() {
            return "- 治癒地圖上所有的人";
        }
    }
}
