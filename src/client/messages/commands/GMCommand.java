package client.messages.commands;

import client.messages.CommandExecute;
import client.ISkill;
import client.LoginCrypto;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.inventory.ModifyInventory;
import client.inventory.MapleEquipIdOnly;
import client.inventory.MapleEquipOnlyId;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.WorldConstants;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.login.handler.AutoRegister;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.CashItemFactory;
import server.FishingRewardFactory;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.ShutdownServer;
import server.Timer.EventTimer;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.maps.MapleReactorFactory;
import server.maps.MapleReactorStats;
import server.quest.MapleQuest;
//import server.CashItemFactory1;
import tools.ArrayMap;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Quadra;
import tools.StringUtil;


/**
 *
 * @author Emilyx3
 */
public class GMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.?????????;
    }

    public static class ?????????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            int input = 0, Change = 0, sn = 0;
            try {
                input = Integer.parseInt(splitted[1]);
                Change = (input - 1);
                sn = Integer.parseInt(splitted[2]);
            } catch (Exception ex) {
                return false;
            }
            if (input < 1 || input > 5) {
                c.getPlayer().dropMessage("??????????????????1~5?????????");
                return true;
            }
            ServerConstants.hot_sell[Change] = sn;
            c.getPlayer().dropMessage("?????????????????????" + input + "??????????????????SN??? " + sn + " ?????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "<???X???????????????> <????????????SN> - ??????????????????????????????";
        }
    }

    public static class SaveAll extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int p = 0;
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                List<MapleCharacter> chrs = cserv.getPlayerStorage().getAllCharactersThreadSafe();
                for (MapleCharacter chr : chrs) {
                    p++;
                    chr.saveToDB(false, false);
                }
            }
            if (c != null && c.getPlayer() != null) {
                c.getPlayer().dropMessage("[??????] " + p + "?????????????????????????????????.");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????????????????";
        }
    }

    public static class LowHP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getStat().setHp((short) 1);
            c.getPlayer().getStat().setMp((short) 1);
            c.getPlayer().updateSingleStat(MapleStat.HP, 1);
            c.getPlayer().updateSingleStat(MapleStat.MP, 1);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class MyPos extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            Point pos = c.getPlayer().getPosition();
            c.getPlayer().dropMessage(6, "X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + c.getPlayer().getFh() + "| CY:" + pos.y);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class Notice extends CommandExecute {

        private static int getNoticeType(String typestring) {
            switch (typestring) {
                case "n":
                    return 0;
                case "p":
                    return 1;
                case "l":
                    return 2;
                case "nv":
                    return 5;
                case "v":
                    return 5;
                case "b":
                    return 6;
            }
            return -1;
        }

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int joinmod = 1;
            int range = -1;
            if (splitted.length < 2) {
                return false;
            }
            switch (splitted[1]) {
                case "m":
                    range = 0;
                    break;
                case "c":
                    range = 1;
                    break;
                case "w":
                    range = 2;
                    break;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            if (splitted.length < tfrom + 1) {
                return false;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            if (splitted[tfrom].equals("nv")) {
                sb.append("[Notice]");
            } else {
                sb.append("");
            }
            joinmod += tfrom;
            if (splitted.length < joinmod + 1) {
                return false;
            }
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = MaplePacketCreator.broadcastMessage(type, sb.toString());
            switch (range) {
                case 0:
                    c.getPlayer().getMap().broadcastMessage(packet);
                    break;
                case 1:
                    ChannelServer.getInstance(c.getWorld(), c.getChannel()).broadcastPacket(packet);
                    break;
                case 2:
                    World.Broadcast.broadcastMessage(packet);
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<n|p|l|nv|v|b> <m|c|w> <message> - ??????";
        }
    }

    public static class Yellow extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int range = -1;
            switch (splitted[1]) {
                case "m":
                    range = 0;
                    break;
                case "c":
                    range = 1;
                    break;
                case "w":
                    range = 2;
                    break;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = MaplePacketCreator.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            switch (range) {
                case 0:
                    c.getPlayer().getMap().broadcastMessage(packet);
                    break;
                case 1:
                    ChannelServer.getInstance(c.getWorld(), c.getChannel()).broadcastPacket(packet);
                    break;
                case 2:
                    World.Broadcast.broadcastMessage(packet);
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<m|c|w> <message> - ????????????";
        }
    }

    public static class Y extends Yellow {
    }

    public static class NpcNotice extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length <= 2) {
                return false;
            }
            int npcid = Integer.parseInt(splitted[1]);
            String msg = splitted[2];
            MapleNPC npc = MapleLifeFactory.getNPC(npcid);
            if (npc == null || !npc.getName().equals("MISSINGNO")) {
                c.getPlayer().dropMessage(6, "????????? Npc ");
                return true;
            }
            World.Broadcast.broadcastMessage(MaplePacketCreator.getNPCTalk(npcid, (byte) 0, msg, "00 00", (byte) 0));
            return true;
        }

        @Override
        public String getHelp() {
			return "<npcid> <message> - ???NPC?????????";
        }
    }

    public static class OpenNPC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            String script = null;
            int mode = -1;
            int npcid = 0;
            try {
                npcid = Integer.parseInt(splitted[1]);
                mode = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException asd) {
            }
            if (splitted.length == 2 && npcid == 0) {
                script = splitted[1];
            }
            MapleNPC npc = MapleLifeFactory.getNPC(npcid);
            if (script != null) {
                NPCScriptManager.getInstance().start(c, 9900001, -1, script);
            } else {
                if (npc != null && !npc.getName().equalsIgnoreCase("MISSINGNO")) {
                    NPCScriptManager.getInstance().start(c, npcid, mode);
                } else {
                    c.getPlayer().dropMessage(6, "??????NPC");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<NPC??????> - ??????NPC";
        }
    }

    public static class ????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            String after = splitted[1];
            if (after.length() <= 12) {
                c.getPlayer().setName(splitted[1]);
                c.getPlayer().fakeRelog();
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<?????????> - ???????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length != 2) {
                return false;
            }
            com.mysql.jdbc.Connection dcon = (com.mysql.jdbc.Connection) DatabaseConnection.getConnection();
            try {
                com.mysql.jdbc.PreparedStatement ps = (com.mysql.jdbc.PreparedStatement) dcon.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
                ps.setString(1, splitted[1]);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (c.getPlayer().getGuildId() > 0) {
                        try {
                            World.Guild.leaveGuild(c.getPlayer().getMGC());
                        } catch (Exception e) {
                            c.sendPacket(MaplePacketCreator.getErrorNotice("??????????????????????????????????????????????????????"));
                            return false;
                        }
                        c.sendPacket(MaplePacketCreator.showGuildInfo(null));

                        c.getPlayer().setGuildId(0);
                        c.getPlayer().saveGuildStatus();
                    }
                    c.getPlayer().setGuildId(rs.getInt("guildid"));
                    c.getPlayer().setGuildRank((byte) 2); // ?????????
                    try {
                        World.Guild.addGuildMember(c.getPlayer().getMGC(), false);
                    } catch (Exception e) {
                    }
                    c.sendPacket(MaplePacketCreator.showGuildInfo(c.getPlayer()));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.removePlayerFromMap(c.getPlayer().getId()), false);
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.spawnPlayerMapobject(c.getPlayer()), false);
                    c.getPlayer().saveGuildStatus();
                } else {
                    c.getPlayer().dropMessage(6, "????????????????????????");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "	???????????? - ??????????????????";
        }
    }

    public static class ?????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim.setMarriageId(0);
            victim.reloadC();
            victim.dropMessage(5, "???????????????");
            victim.saveToDB(false, false);
            c.getPlayer().dropMessage(6, victim.getName() + "???????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> - ??????";
        }
    }

    public static class CancelBuffs extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().cancelAllBuffs();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????BUFF";
        }
    }

    public static class RemoveNPCs extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().resetNPCs();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????NPC";
        }
    }

    public static class LookNPCs extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllNPCsThreadsafe()) {
                MapleNPC reactor2l = (MapleNPC) reactor1l;
                c.getPlayer().dropMessage(5, "NPC: oID: " + reactor2l.getObjectId() + " npcID: " + reactor2l.getId() + " Position: " + reactor2l.getPosition().toString() + " Name: " + reactor2l.getName());
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????NPC";
        }
    }

    public static class LookReactors extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllReactorsThreadsafe()) {
                MapleReactor reactor2l = (MapleReactor) reactor1l;
                c.getPlayer().dropMessage(5, "Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState() + " Name: " + reactor2l.getName());
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ?????????????????????";
        }
    }

    public static class LookPortals extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MaplePortal portal : c.getPlayer().getMap().getPortals()) {
                c.getPlayer().dropMessage(5, "Portal: ID: " + portal.getId() + " script: " + portal.getScriptName() + " name: " + portal.getName() + " pos: " + portal.getPosition().x + "," + portal.getPosition().y + " target: " + portal.getTargetMapId() + " / " + portal.getTarget());
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ?????????????????????";
        }
    }

    public static class GoTo extends CommandExecute {

        private static final HashMap<String, Integer> gotomaps = new HashMap<>();

        static {
            gotomaps.put("gmmap", 180000000);
            gotomaps.put("southperry", 2000000);
            gotomaps.put("amherst", 1010000);
            gotomaps.put("henesys", 100000000);
            gotomaps.put("ellinia", 101000000);
            gotomaps.put("perion", 102000000);
            gotomaps.put("kerning", 103000000);
            gotomaps.put("lithharbour", 104000000);
            gotomaps.put("sleepywood", 105040300);
            gotomaps.put("florina", 110000000);
            gotomaps.put("orbis", 200000000);
            gotomaps.put("happyville", 209000000);
            gotomaps.put("elnath", 211000000);
            gotomaps.put("ludibrium", 220000000);
            gotomaps.put("aquaroad", 230000000);
            gotomaps.put("leafre", 240000000);
            gotomaps.put("mulung", 250000000);
            gotomaps.put("herbtown", 251000000);
            gotomaps.put("omegasector", 221000000);
            gotomaps.put("koreanfolktown", 222000000);
            gotomaps.put("newleafcity", 600000000);
            gotomaps.put("sharenian", 990000000);
            gotomaps.put("pianus", 230040420);
            gotomaps.put("horntail", 240060200);
            gotomaps.put("chorntail", 240060201);
            gotomaps.put("mushmom", 100000005);
            gotomaps.put("griffey", 240020101);
            gotomaps.put("manon", 240020401);
            gotomaps.put("zakum", 280030000);
            gotomaps.put("czakum", 280030001);
            gotomaps.put("papulatus", 220080001);
            gotomaps.put("showatown", 801000000);
            gotomaps.put("zipangu", 800000000);
            gotomaps.put("ariant", 260000100);
            gotomaps.put("nautilus", 120000000);
            gotomaps.put("boatquay", 541000000);
            gotomaps.put("malaysia", 550000000);
            gotomaps.put("taiwan", 740000000);
            gotomaps.put("thailand", 500000000);
            gotomaps.put("erev", 130000000);
            gotomaps.put("ellinforest", 300000000);
            gotomaps.put("kampung", 551000000);
            gotomaps.put("singapore", 540000000);
            gotomaps.put("amoria", 680000000);
            gotomaps.put("timetemple", 270000000);
            gotomaps.put("pinkbean", 270050100);
            gotomaps.put("peachblossom", 700000000);
            gotomaps.put("fm", 910000000);
            gotomaps.put("freemarket", 910000000);
            gotomaps.put("oxquiz", 109020001);
            gotomaps.put("ola", 109030101);
            gotomaps.put("fitness", 109040000);
            gotomaps.put("snowball", 109060000);
            gotomaps.put("cashmap", 741010200);
            gotomaps.put("golden", 950100000);
            gotomaps.put("phantom", 610010000);
            gotomaps.put("cwk", 610030000);
            gotomaps.put("rien", 140000000);
        }

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "????????????: !goto <????????????>");
            } else if (gotomaps.containsKey(splitted[1])) {
                MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                MaplePortal targetPortal = target.getPortal(0);
                c.getPlayer().changeMap(target, targetPortal);
            } else if (splitted[1].equals("?????????")) {
                c.getPlayer().dropMessage(6, "?????? !goto <?????????>. ?????????????????????:");
                StringBuilder sb = new StringBuilder();
                for (String s : gotomaps.keySet()) {
                    sb.append(s).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
            } else {
                c.getPlayer().dropMessage(6, "????????????????????? - ?????? !goto <?????????>. ???????????????????????????, ???????????? !goto ?????????????????????.");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????> - ???????????????";

        }
    }

    public static class ClearDrops extends RemoveDrops {

    }

    public static class RemoveDrops extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage(5, "????????? " + c.getPlayer().getMap().getNumItems() + " ????????????");
            c.getPlayer().getMap().removeDrops();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ?????????????????????";

        }
    }

    public static class NearestPortal extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MaplePortal portal = c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition());
            c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());

            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????";

        }
    }

    public static class SpawnDebug extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
            return true;
        }

        @Override
        public String getHelp() {
			return "- debug????????????";

        }
    }

    public static class Speak extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

            if (victim == null) {
                c.getPlayer().dropMessage(5, "????????? '" + splitted[1]);
                return false;
            } else {
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> <??????> - ????????????????????????";
        }
    }

    public static class SpeakMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????> - ?????????????????????????????????";
        }

    }

    public static class SpeakChannel extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharactersThreadSafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????> - ?????????????????????????????????";
        }

    }

    public static class SpeakWorld extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    if (victim.getId() != c.getPlayer().getId()) {
                        victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                    }
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????> - ?????????????????????????????????";
        }
    }

    public static class SpeakMega extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter victim = null;
            if (splitted.length >= 2) {
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            }
            try {
                World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getSuperMegaphone(victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true, victim == null ? c.getChannel() : victim.getClient().getChannel()));
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "[????????????] <??????> - ????????????????????????????????????";
        }
    }

    public static class Say extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice(sb.toString()));
            } else {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "?????? - ???????????????";
        }
    }

    public static class Shutdown extends CommandExecute {

        private static Thread t = null;

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage(6, "??????????????????...");
            if (t == null || !t.isAlive()) {
                t = new Thread(server.ShutdownServer.getInstance());
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "???????????????...");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ???????????????";
        }
    }

    public static class ShutdownTime extends CommandExecute {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;
        private static Thread t = null;

        @Override
        public boolean execute(MapleClient c, String splitted[]) {

            if (splitted.length < 2) {
                return false;
            }
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, "??????????????? " + minutesLeft + "????????????????????????????????????????????????????????????.");
            WorldConstants.ADMIN_ONLY = true;
            c.getPlayer().dropMessage(6, "??????????????????????????????");
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        if (!WorldConstants.Disable_Shop) {
                            WorldConstants.Disable_Shop = true;
                            if (c != null && c.getPlayer() != null) {
                                c.getPlayer().dropMessage(6, "??????????????????????????????????????????????????????");
                            }
                        }
                        if ((minutesLeft > 0 && minutesLeft <= 11) && !WorldConstants.isShutDown) {
                            WorldConstants.isShutDown = true;
                            if (c != null && c.getPlayer() != null) {
                                c.getPlayer().dropMessage(6, "???????????????????????????????????????");
                            }
                        }
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().run();
                            t.start();
                            ts.cancel(false);
                            return;
                        }
                        StringBuilder message = new StringBuilder();
                        message.append("[???????????????] ??????????????? ");
                        message.append(minutesLeft);
                        message.append(" ???????????????????????????????????????????????????????????????");
                        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice(message.toString()));
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage(message.toString()));
                        for (World wl : LoginServer.getWorlds()) {
                            for (ChannelServer cs : wl.getChannels()) {
                                cs.setServerMessage("??????????????? " + minutesLeft + " ???????????????");
                            }
                        }
                        System.out.println("??????????????? " + minutesLeft + " ???????????????");
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, new StringBuilder().append("?????????????????????????????? ").append(minutesLeft).append("????????????????????????????????????").toString());
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<?????????> - ???????????????";
        }
    }

    public static class UnbanIP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            byte ret_ = MapleClient.unbanIP(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[unbanip] SQL ??????.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[unbanip] ???????????????.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[unbanip] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[unbanip] IP???Mac?????????????????????.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[unbanip] IP??????Mac???????????????.");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> - ????????????";
        }
    }

    public static class TempBan extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                return false;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            final int reason = Integer.parseInt(splitted[2]);
            final int numDay = Integer.parseInt(splitted[3]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();

            if (victim == null) {
                c.getPlayer().dropMessage(6, "[tempban] ?????????????????????");

            } else {
                victim.tempban("???" + c.getPlayer().getName() + "???????????????", cal, reason, true);
                c.getPlayer().dropMessage(6, "[tempban] " + splitted[1] + " ??????????????????????????? " + df.format(cal.getTime()));
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> - ??????????????????";
        }
    }

    public static class ?????????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            WorldConstants.isShutDown = !WorldConstants.isShutDown;
            c.getPlayer().dropMessage(0, "[??????????????????] " + (WorldConstants.isShutDown ? "??????" : "??????"));
            System.out.println("[??????????????????] " + (WorldConstants.isShutDown ? "??????" : "??????"));
            return true;
        }

        public String getHelp() {
			return " - ??????????????????????????????";
        }
    }

    public static class ?????????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            WorldConstants.Disable_Shop = !WorldConstants.Disable_Shop;
            c.getPlayer().dropMessage(6, "[??????????????????] " + (WorldConstants.Disable_Shop ? "??????" : "??????"));
            System.out.println("[??????????????????] " + (WorldConstants.Disable_Shop ? "??????" : "??????"));
            return true;
        }

        public String getHelp() {
			return " - ??????????????????????????????";
        }
    }

    public static class CopyAll extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter victim;
            if (splitted.length < 2) {
                return false;
            }
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                player.dropMessage("??????????????????");
                return true;
            }
            MapleInventory equipped = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED);
            MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
            List<Short> ids = new LinkedList<>();
            for (IItem item : equipped.list()) {
                ids.add(item.getPosition());
            }
            for (short id : ids) {
                MapleInventoryManipulator.unequip(c, id, equip.getNextFreeSlot());
            }
            c.getPlayer().clearSkills();
            c.getPlayer().setStr(victim.getStr());
            c.getPlayer().setDex(victim.getDex());
            c.getPlayer().setInt(victim.getInt());
            c.getPlayer().setLuk(victim.getLuk());

            c.getPlayer().setMeso(victim.getMeso());
            c.getPlayer().setLevel((short) (victim.getLevel()));
            c.getPlayer().changeJob(victim.getJob());

            c.getPlayer().setHp(victim.getHp());
            c.getPlayer().setMp(victim.getMp());
            c.getPlayer().setMaxHp(victim.getMaxHp());
            c.getPlayer().setMaxMp(victim.getMaxMp());

            String normal = victim.getName();
            String after = (normal + "x2");
            if (after.length() <= 12) {
                c.getPlayer().setName(victim.getName() + "x2");
            }
            c.getPlayer().setRemainingAp(victim.getRemainingAp());
            c.getPlayer().setRemainingSp(victim.getRemainingSp());
            c.getPlayer().addSameSkill(victim);

            c.getPlayer().setFame(victim.getFame());
            c.getPlayer().setHair(victim.getHair());
            c.getPlayer().setFace(victim.getFace());

            c.getPlayer().setSkinColor(victim.getSkinColor() == 0 ? c.getPlayer().getSkinColor() : victim.getSkinColor());

            c.getPlayer().setGender(victim.getGender());

            for (IItem ii : victim.getInventory(MapleInventoryType.EQUIPPED).list()) {
                IItem eq = ii.copy();
                eq.setPosition(eq.getPosition());
                eq.setQuantity((short) 1);
                eq.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                eq.setWorld(c.getPlayer().getWorld());
                c.getPlayer().forceReAddItem_NoUpdate(eq, MapleInventoryType.EQUIPPED);
            }
            c.getPlayer().fakeRelog();
            return true;
        }

        @Override
        public String getHelp() {
			return "???????????? - ????????????";
        }
    }

    public static class CopyInv extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter victim;
            int type = 1;
            if (splitted.length < 2) {
                return false;
            }

            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return false;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                player.dropMessage("??????????????????");
                return true;
            }
            try {
                type = Integer.parseInt(splitted[2]);
            } catch (Exception ex) {
            }
            if (type == 0) {
                for (client.inventory.IItem ii : victim.getInventory(MapleInventoryType.EQUIPPED).list()) {
                    IItem n = ii.copy();
                    n.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                    player.getInventory(MapleInventoryType.EQUIP).addItem(n);
                }
                player.fakeRelog();
            } else {
                MapleInventoryType types;
                switch (type) {
                    case 1:
                        types = MapleInventoryType.EQUIP;
                        break;
                    case 2:
                        types = MapleInventoryType.USE;
                        break;
                    case 3:
                        types = MapleInventoryType.ETC;
                        break;
                    case 4:
                        types = MapleInventoryType.SETUP;
                        break;
                    case 5:
                        types = MapleInventoryType.CASH;
                        break;
                    default:
                        types = null;
                        break;
                }
                if (types == null) {
                    c.getPlayer().dropMessage("????????????");
                    return true;
                }
                int[] equip = new int[97];
                for (int i = 1; i < 97; i++) {
                    if (victim.getInventory(types).getItem((short) i) != null) {
                        equip[i] = i;
                    }
                }
                for (int i = 0; i < equip.length; i++) {
                    if (equip[i] != 0) {
                        IItem n = victim.getInventory(types).getItem((short) equip[i]).copy();
                        n.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                        n.setWorld(c.getPlayer().getWorld());
                        player.getInventory(types).addItem(n);
                        c.sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.ADD, n)));
                    }
                }
            }
            return true;
        }

        public String getHelp() {
			return "???????????? ????????????(0 = ????????? 1=????????? 2=????????? 3=????????? 4=????????? 5=?????????)(???????????????) - ??????????????????";
        }
    }

    public static class Clock extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return true;
        }

        @Override
        public String getHelp() {
			return "<time> ??????";
        }
    }

    public static class Song extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitted[1]));
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class Kill extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                String name = splitted[i];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(name);
                if (ch == -10) {
                    c.getPlayer().dropMessage(6, "??????[" + name + "]???????????????");
                    break;
                } else if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "??????[" + name + "]????????????");
                    break;
                }
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                if (victim != null) {
                    if (player.allowedToTarget(victim)) {
                        victim.getStat().setHp((short) 0);
                        victim.getStat().setMp((short) 0);
                        victim.updateSingleStat(MapleStat.HP, 0);
                        victim.updateSingleStat(MapleStat.MP, 0);
                    }
                } else {
                    c.getPlayer().dropMessage(6, "?????? " + name + " ?????????.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> - ????????????";
        }
    }

    public static class ReloadOps extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            c.getPlayer().dropMessage(6, "??????????????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????OpCode";
        }
    }

    public static class ReloadDrops extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            c.getPlayer().dropMessage(6, "?????????????????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class ReloadPortals extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            PortalScriptManager.getInstance().clearScripts();
            c.getPlayer().dropMessage(6, "???????????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ?????????????????????";
        }
    }

    public static class ReloadShops extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleShopFactory.getInstance().clear();
            c.getPlayer().dropMessage(6, "NPC?????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class ReloadCS extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            CashItemFactory.getInstance().clearItems();
            //CashItemFactory1.getInstance().clearItems();
            c.getPlayer().dropMessage(6, "???????????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????????????????";
        }
    }

    public static class ReloadFishing extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            FishingRewardFactory.getInstance().reloadItems();
            c.getPlayer().dropMessage(6, "?????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????????????????";
        }
    }

    public static class LoadEvent extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            String eventName = splitted[1];
            // ???????????????events??????????????????
            for (ChannelServer cs : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                cs.updateEvents(eventName);
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<?????????> - ??????????????????";

        }
    }

    public static class ReloadEvents extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (ChannelServer instance : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                instance.reloadEvents();
                c.getPlayer().dropMessage("??????" + c.getWorld() + " ?????? " + instance.getChannel() + " ?????????????????????");
            }
            c.getPlayer().dropMessage(6, "?????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????????????????";
        }
    }

    public static class ReloadQuests extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleQuest.clearQuests();
            c.getPlayer().dropMessage(6, "?????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class ReloadOX extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleOxQuizFactory.getInstance().reloadOX();
            c.getPlayer().dropMessage(6, "OX?????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????OX??????";
        }
    }

    public static class ReloadLife extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage("?????????????????????????????? ???????????????:" + MapleMapFactory.loadCustomLife(true, c.getPlayer().getMap()));
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????NPC/??????";
        }
    }

    public static class ReloadAll extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                instance.reloadEvents();
            }
            MapleShopFactory.getInstance().clear();
            PortalScriptManager.getInstance().clearScripts();
            MapleItemInformationProvider.getInstance().loadEtc(true);
            MapleItemInformationProvider.getInstance().loadItems(true);

            CashItemFactory.getInstance().initialize(true);
            MapleMonsterInformationProvider.getInstance().clearDrops();

            MapleGuild.loadAll(); //(this); 
            MapleFamily.loadAll(); //(this); 
            MapleLifeFactory.loadQuestCounts(true);
            MapleQuest.initQuests(true);
            MapleOxQuizFactory.getInstance();
            ReactorScriptManager.getInstance().clearDrops();
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            c.getPlayer().dropMessage(6, "???????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class Skill extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            ISkill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            if (level > skill.getMaxLevel()) {
                level = skill.getMaxLevel();
            }
            c.getPlayer().changeSkillLevel(skill, level, masterlevel);
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> [????????????] [??????????????????] ...  - ????????????";
        }
    }

    public static class GiveSkill extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                return false;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

            ISkill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

            if (level > skill.getMaxLevel()) {
                level = skill.getMaxLevel();
            }
            victim.changeSkillLevel(skill, level, masterlevel);
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> <??????ID> [????????????] [??????????????????] - ????????????";
        }
    }

    public static class MaxSkillsByJob extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().maxSkillsByJob();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class MaxSkills extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().maxSkills();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class ClearSkills extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().clearSkills();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class SP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.sendPacket(MaplePacketCreator.updateSp(c.getPlayer(), false));
            return true;
        }

        @Override
        public String getHelp() {
			return "[??????] - ??????SP";
        }
    }

    public static class AP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().setRemainingAp((short) CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            final List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>();
            c.sendPacket(MaplePacketCreator.updateAp(c.getPlayer(), false));
            return true;
        }

        @Override
        public String getHelp() {
			return "[??????] - ??????AP";
        }
    }

    public static class Shop extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = 0;
            try {
                shopId = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
            }
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            } else {
                c.getPlayer().dropMessage(5, "?????????ID[" + shopId + "]?????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        protected static ScheduledFuture<?> ts = null;

        @Override
        public boolean execute(final MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            if (ts != null) {
                ts.cancel(false);
                c.getPlayer().dropMessage(0, "??????????????????????????????");
            }
            int minutesLeft;
            try {
                minutesLeft = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {
                return false;
            }
            if (minutesLeft > 0) {
                ts = EventTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                                if (mch.getLevel() >= 29 && !mch.isGM()) {
                                    NPCScriptManager.getInstance().start(mch.getClient(), 9010010, "CrucialTime");
                                }
                            }
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("??????????????????????????????30????????????????????????????????????"));
                        ts.cancel(false);
                        ts = null;
                    }
                }, minutesLeft * 60000); // ?????????
                c.getPlayer().dropMessage(0, "???????????????????????????");
            } else {
                c.getPlayer().dropMessage(0, "????????????????????? > 0???");
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????> - ????????????";
        }
    }

    public static class UnlockInv extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            java.util.Map<IItem, MapleInventoryType> eqs = new ArrayMap<>();
            if (splitted.length < 2 || splitted[1].equals("??????")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (IItem item : c.getPlayer().getInventory(type)) {
                        if (isLocked(item)) {
                            eqs.put(item, type);
                        }
                    }
                }
            } else if (splitted[1].equals("???????????????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                }
            } else if (splitted[1].equals("??????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                }
            } else if (splitted[1].equals("??????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.USE);
                    }
                }
            } else if (splitted[1].equals("??????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.SETUP);
                    }
                }
            } else if (splitted[1].equals("??????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.ETC);
                    }
                }
            } else if (splitted[1].equals("??????")) {
                for (IItem item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (isLocked(item)) {
                        eqs.put(item, MapleInventoryType.CASH);
                    }
                }
            } else {
                return false;
            }

            for (Map.Entry<IItem, MapleInventoryType> eq : eqs.entrySet()) {
                c.getPlayer().dropMessage(5, "???????????? " + eq.getKey().getItemName());
                c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
            }
            c.getPlayer().reloadC();
            return true;
        }

        private boolean isLocked(IItem item) {
            if (ItemFlag.LOCK.check(item.getFlag())) {
                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                return true;
            }
            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                return true;
            }
            return false;
        }

        @Override
        public String getHelp() {
			return "<??????/???????????????/??????/??????/??????/??????/??????> - ????????????";
        }
    }

    public static class Letter extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "????????????: ");
                return false;
            }
            int start, nstart;
            if (splitted[1].equalsIgnoreCase("green")) {
                start = 3991026;
                nstart = 3990019;
            } else if (splitted[1].equalsIgnoreCase("red")) {
                start = 3991000;
                nstart = 3990009;
            } else {
                c.getPlayer().dropMessage(6, "???????????????!");
                return true;
            }
            String splitString = StringUtil.joinStringFrom(splitted, 2);
            List<Integer> chars = new ArrayList<>();
            splitString = splitString.toUpperCase();
            // System.out.println(splitString);
            for (int i = 0; i < splitString.length(); i++) {
                char chr = splitString.charAt(i);
                if (chr == ' ') {
                    chars.add(-1);
                } else if ((int) (chr) >= (int) 'A' && (int) (chr) <= (int) 'Z') {
                    chars.add((int) (chr));
                } else if ((int) (chr) >= (int) '0' && (int) (chr) <= (int) ('9')) {
                    chars.add((int) (chr) + 200);
                }
            }
            final int w = 32;
            int dStart = c.getPlayer().getPosition().x - (splitString.length() / 2 * w);
            for (Integer i : chars) {
                if (i == -1) {
                    dStart += w;
                } else if (i < 200) {
                    int val = start + i - (int) ('A');
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                } else if (i >= 200 && i <= 300) {
                    int val = nstart + i - (int) ('0') - 200;
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<color (green/red)> <word> - ??????";
        }

    }

    public static class Marry extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!GameConstants.isEffectRing(itemId)) {
                c.getPlayer().dropMessage(6, "???????????????ID.");
            } else {
                MapleCharacter fff;
                String name = splitted[1];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(name);
                if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "??????????????????");
                    return false;
                }
                fff = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                if (fff == null) {
                    c.getPlayer().dropMessage(6, "??????????????????");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    try {
                        MapleCharacter[] chrz = {fff, c.getPlayer()};
                        for (int i = 0; i < chrz.length; i++) {
                            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId);
                            if (eq == null) {
                                c.getPlayer().dropMessage(6, "???????????????ID.");
                                return true;
                            } else {
                                eq.setUniqueId(ringID[i]);
                                MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                                chrz[i].dropMessage(6, "?????????  " + chrz[i == 0 ? 1 : 0].getName() + " ??????");
                            }
                        }
                        MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                    } catch (SQLException e) {
                    }
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<????????????> <????????????> - ??????";
        }
    }

    public static class KillID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            int id = 0;
            try {
                id = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(id);
            int wl = World.Find.findWorld(id);
            if (ch <= 0) {
                return false;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(id);
            if (victim == null) {
                c.getPlayer().dropMessage(6, "[kill] ??????ID " + id + " ?????????.");
            } else if (player.allowedToTarget(victim)) {
                victim.getStat().setHp((short) 0);
                victim.getStat().setMp((short) 0);
                victim.updateSingleStat(MapleStat.HP, 0);
                victim.updateSingleStat(MapleStat.MP, 0);
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ????????????";
        }
    }

    public static class AutoReg extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            LoginServer.setAutoReg(!LoginServer.getAutoReg());
            c.getPlayer().dropMessage(0, "[autoreg] " + (LoginServer.getAutoReg() ? "??????" : "??????"));
            System.out.println("[autoreg] " + (LoginServer.getAutoReg() ? "??????" : "??????"));
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";
        }
    }

    public static class LoginDoor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            WorldConstants.ADMIN_ONLY = !WorldConstants.ADMIN_ONLY;
            c.getPlayer().dropMessage(0, "[logindoor] " + (WorldConstants.ADMIN_ONLY ? "??????" : "??????"));
            System.out.println("[logindoor] " + (WorldConstants.ADMIN_ONLY ? "??????" : "??????"));
            return true;
        }

        public String getHelp() {
			return "- ???????????????????????????";
        }
    }

    public static class LevelUp extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                c.getPlayer().levelUp();
            } else {
                int up = 0;
                try {
                    up = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                for (int i = 0; i < up; i++) {
                    c.getPlayer().levelUp();
                }
            }
            c.getPlayer().setExp(0);
            c.getPlayer().updateSingleStat(MapleStat.EXP, 0);
//            if (c.getPlayer().getLevel() < 200) {
//                c.getPlayer().gainExp(GameConstants.getExpNeededForLevel(c.getPlayer().getLevel()) + 1, true, false, true);
//            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";
        }
    }

    public static class FakeRelog extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            c.sendPacket(MaplePacketCreator.getCharInfo(player));
            player.getMap().removePlayer(player);
            player.getMap().addPlayer(player);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????????????????";

        }
    }

    /*public static class SpawnReactor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleReactorStats reactorSt = MapleReactorFactory.getReactor(Integer.parseInt(splitted[1]));
          /MapleReactor reactor = new MapleReactor(reactorSt, Integer.parseInt(splitted[1]), c.getWorld());
            reactor.setDelay(-1);
            reactor.setPosition(c.getPlayer().getPosition());
            c.getPlayer().getMap().spawnReactor(reactor);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????Reactor";

        }
    }*/

    public static class HReactor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????Reactor";

        }
    }

    public static class DestroyReactor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            if (splitted[1].equals("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????Reactor";

        }
    }

    public static class ResetReactors extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().resetReactors();
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????????????????Reactor";

        }
    }

    public static class SetReactor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return true;
        }

        @Override
        public String getHelp() {
			return "- ??????Reactor";

        }
    }

    public static class ResetQuest extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ????????????";

        }
    }

    public static class StartQuest extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ????????????";

        }
    }

    public static class CompleteQuest extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ????????????";

        }
    }

    public static class FStartQuest extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ??????????????????";

        }
    }

    public static class FCompleteQuest extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return true;
        }

        @Override
        public String getHelp() {
			return "<??????ID> - ??????????????????";

        }
    }

    public static class FStartOther extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {

            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length >= 4 ? splitted[4] : null);
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";

        }
    }

    public static class FCompleteOther extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
            return true;
        }

        @Override
        public String getHelp() {
			return "- ????????????";

        }
    }

    public static class Log extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            boolean next = false;
            boolean Action;
            String LogType = null;
            String[] Log = {"??????", "??????", "??????", "??????", "????????????", "????????????", "??????", "??????", "??????", "?????????", "?????????"};
            StringBuilder show_log = new StringBuilder();
            for (String s : Log) {
                show_log.append(s);
                show_log.append(" / ");
            }
            if (splitted.length < 3) {
                c.getPlayer().dropMessage("??????Log??????: " + show_log.toString());
                return false;
            }
            final String action = splitted[1].toLowerCase();
            if (action.equals("???") || action.equals("on")) {
                Action = true;
            } else if (action.equals("???") || action.equals("off")) {
                Action = false;
            } else {
                c.getPlayer().dropMessage("?????????????????????????????????????????????????");
                return true;
            }

            for (int i = 0; i < Log.length; i++) {
                if (splitted[2].contains(Log[i])) {
                    next = true;
                    LogType = Log[i];
                    break;
                }
            }
            if (!next) {
                c.getPlayer().dropMessage("??????Log??????: " + show_log.toString());
                return true;
            }

            switch (LogType) {
                case "??????":
                    ServerConfig.LOG_MEGA = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_DAMAGE = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_CHAT = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_CSBUY = Action;
                    break;
                case "????????????":
                    ServerConfig.LOG_MERCHANT = Action;
                    break;
                case "????????????":
                    ServerConfig.LOG_OWNERSTORE = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_CHALKBOARD = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_SCROLL = Action;
                    break;
                case "??????":
                    ServerConfig.LOG_DC = Action;
                    break;
                case "?????????":
                    ServerConfig.LOG_DROPITEM = Action;
                    break;
                case "?????????":
                    ServerConfig.LOG_PICKITEM = Action;
                    break;
            }
            String msg = "[GM ??????] ?????????[" + c.getPlayer().getName() + "] " + splitted[1] + "???" + LogType + "???Log";
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
			return "???/??? Log????????????";
        }

    }

    public static class RemoveItem extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter chr;
            String name = splitted[1];
            int id = Integer.parseInt(splitted[2]);
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(name);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

            if (chr == null) {
                c.getPlayer().dropMessage(6, "?????????????????????");
            } else {
                chr.removeAll(id, false, true);
                c.getPlayer().dropMessage(6, "??????ID??? " + id + " ?????????????????? " + name + " ??????????????????");
            }
            return true;

        }

        @Override
        public String getHelp() {
			return "<????????????> <??????ID> - ???????????????????????????";
        }
    }

    public static class RemoveItemOff extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                int item = Integer.parseInt(splitted[1]);
                String name = splitted[2];
                int id = 0, quantity = 0;
                List<Long> inventoryitemid = new LinkedList();
                boolean isEquip = GameConstants.isEquip(item);

                if (MapleCharacter.getCharacterByName(name) == null) {
                    c.getPlayer().dropMessage(5, "???????????????????????????");
                    return true;
                } else {
                    id = MapleCharacter.getCharacterByName(name).getId();
                }

                PreparedStatement ps = con.prepareStatement("select inventoryitemid, quantity from inventoryitems WHERE itemid = ? and characterid = ?");
                ps.setInt(1, item);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (isEquip) {
                            long Equipid = rs.getLong("inventoryitemid");
                            if (Equipid != 0) {
                                inventoryitemid.add(Equipid);
                            }
                            quantity++;
                        } else {
                            quantity += rs.getInt("quantity");
                        }
                    }
                }
                if (quantity == 0) {
                    c.getPlayer().dropMessage(5, "??????[" + name + "]????????????[" + item + "]????????????");
                    return true;
                }

                if (isEquip) {
                    StringBuilder Sql = new StringBuilder();
                    Sql.append("Delete from inventoryequipment WHERE inventoryitemid = ");
                    for (int i = 0; i < inventoryitemid.size(); i++) {
                        Sql.append(inventoryitemid.get(i));
                        if (i < (inventoryitemid.size() - 1)) {
                            Sql.append(" OR inventoryitemid = ");
                        }
                    }
                    ps = con.prepareStatement(Sql.toString());
                    ps.executeUpdate();
                }

                ps = con.prepareStatement("Delete from inventoryitems WHERE itemid = ? and characterid = ?");
                ps.setInt(1, item);
                ps.setInt(2, id);
                ps.executeUpdate();
                ps.close();

                c.getPlayer().dropMessage(6, "????????? " + name + " ???????????????????????? ID[" + item + "] ??????x" + quantity);
                return true;
            } catch (SQLException e) {
                return true;
            }
        }

        @Override
        public String getHelp() {
			return "<??????ID> <????????????> - ???????????????????????????";
        }
    }

    public static class ??????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleEquipIdOnly Only = MapleEquipIdOnly.getInstance();
            if (Only.isDoing()) {
                c.getPlayer().dropMessage("?????????????????????, ???????????????");
                return true;
            }
            c.getPlayer().dropMessage("?????????????????????....");
            Only.StartChecking();
            c.getPlayer().dropMessage("??????????????????");
            return true;
        }

        @Override
        public String getHelp() {
            return "- ???????????????";
        }
    }

    public static class ??????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleEquipIdOnly Only = MapleEquipIdOnly.getInstance();
            List<Quadra<Integer, Long, Long, Integer>> MapleEquipIdOnlyList = Only.getData();
            if (MapleEquipIdOnlyList.isEmpty()) {
                c.getPlayer().dropMessage("?????????????????????????????????, ????????? !??????????????? ");
                return true;
            } else if (Only.isDoing()) {
                c.getPlayer().dropMessage("?????????????????????, ???????????????");
                return true;
            }
            c.getPlayer().dropMessage(6, "??????????????????...");
            Only.StartCleaning();
            c.getPlayer().dropMessage(6, "????????????????????????");
            return true;
        }

        @Override
        public String getHelp() {
			return "- ???????????????";
        }

        public void HandleOffline(MapleClient c, int chr, long inventoryitemid, long equiponlyid) {
            try {
                String itemname = "null";
                Connection con = DatabaseConnection.getConnection();

                try (PreparedStatement ps = con.prepareStatement("select itemid from inventoryitems WHERE inventoryitemid = ?")) {
                    ps.setLong(1, inventoryitemid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int itemid = rs.getInt("itemid");
                            itemname = MapleItemInformationProvider.getInstance().getName(itemid);
                        } else {
                            c.getPlayer().dropMessage("????????????: ?????????????????????????????????");
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("Delete from inventoryequipment WHERE inventoryitemid = " + inventoryitemid)) {
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("Delete from inventoryitems WHERE inventoryitemid = ?")) {
                    ps.setLong(1, inventoryitemid);
                    ps.executeUpdate();
                }

                String msgtext = "??????ID: " + chr + " ????????????????????????????????????[" + itemname + "]?????????????????????";
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msgtext));
                FileoutputUtil.logToFile("logs/Hack/????????????_?????????.txt", FileoutputUtil.CurrentReadable_Time() + " " + msgtext + " ????????????ID: " + equiponlyid + "\r\n");

            } catch (Exception ex) {
                String output = FileoutputUtil.NowTime();
                FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, ex);
                FileoutputUtil.logToFile(FileoutputUtil.CommandEx_Log, output + " \r\n");
            }
        }
    }

    public static class ????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            String username = "";
            String password = "";
            try {
                username = splitted[1];
                password = splitted[2];
            } catch (Exception ex) {
                return false;
            }
            if (username == null || password == null) {
                c.getPlayer().dropMessage("???????????????????????????");
                return true;
            }
            if (!AutoRegister.getAccountExists(username)) {
                c.getPlayer().dropMessage("??????????????????");
                return true;
            }
            if (username.length() >= 12) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                try (java.sql.PreparedStatement ps = con.prepareStatement("Update accounts set password = ?, salt = null Where name = ? AND gm = 0")) {
                    ps.setString(1, LoginCrypto.hexSha1(password));
                    ps.setString(2, username);
                    ps.execute();
                } catch (SQLException ex) {
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
            c.getPlayer().dropMessage("[????????????]??????: " + username + " ??????: " + password);
            return true;
        }

        @Override
        public String getHelp() {
            return "<??????> <?????????> - ??????????????????";
        }
    }

    public static class ?????????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            String username = "";
            String password = "";
            try {
                username = splitted[1];
                password = splitted[2];
            } catch (Exception ex) {
                return false;
            }
            if (username == null || password == null) {
                c.getPlayer().dropMessage("???????????????????????????");
                return true;
            }
            if (!AutoRegister.getAccountExists(username)) {
                c.getPlayer().dropMessage("??????????????????");
                return true;
            }
            if (username.length() >= 12) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                try (java.sql.PreparedStatement ps = con.prepareStatement("Update accounts set 2ndpassword = ? Where name = ? and gm = 0")) {
                    ps.setString(1, LoginCrypto.hexSha1(password));
                    ps.setString(2, username);
                    ps.execute();
                } catch (SQLException ex) {
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
            c.getPlayer().dropMessage("[?????????????????????]??????: " + username + " ???????????????: " + password);
            return true;
        }

        @Override
        public String getHelp() {
            return "<??????> <??????????????????> - ???????????????????????????";
        }
    }
}
