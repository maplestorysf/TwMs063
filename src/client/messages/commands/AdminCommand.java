package client.messages.commands;

import client.messages.CommandExecute;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleDisease;
import client.MapleStat;
import client.anticheat.CheatingOffense;
import client.inventory.Equip;
import client.inventory.IEquip;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.PiPiConfig;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import scripting.EventManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.Timer.EventTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MobSkillFactory;
import server.life.OverrideMonsterStats;
import server.life.PlayerNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.CPUSampler;
import tools.MaplePacketCreator;
import tools.MockIOSession;
import tools.StringUtil;
import tools.packet.MobPacket;
import java.util.concurrent.ScheduledFuture;
import scripting.NPCScriptManager;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.StringTool;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Emilyx3
 */
public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.???????????????;
    }

    public static class ItemList extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            String name = splitted[1];
            MapleClient cs = new MapleClient(null, null, new MockIOSession());
            MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(name), cs, true);
            String msgg = "<" + chhr.getName() + ">\r\n";
            for (IItem ii : chhr.getInventory(MapleInventoryType.EQUIPPED).list()) {
                IEquip eq = (IEquip) ii.copy();
                int id = eq.getItemId();
                int str = eq.getStr();
                int dex = eq.getDex();
                int int_ = eq.getInt();
                int luk = eq.getLuk();
                int hp = eq.getHp();
                int mp = eq.getMp();
                int watk = eq.getWatk();
                int matk = eq.getMatk();
                int wdef = eq.getWdef();
                int mdef = eq.getMdef();
                int lv = eq.getLevel();
                int acc = eq.getAcc();
                int avoid = eq.getAvoid();
                int speed = eq.getSpeed();
                int jump = eq.getJump();
                int upg = eq.getUpgradeSlots();
                String msg = "!ProItem " + id + " " + str + " " + dex + " " + int_ + " " + luk + " " + hp + " " + mp + " " + watk + " " + matk + " " + wdef + " " + mdef + " " + lv + " " + acc + " " + avoid + " " + speed + " " + jump + " " + upg + " -1";
                msgg += msg + "\r\n";
                c.getPlayer().dropMessage(msg);
            }

            FileoutputUtil.logToFile("logs/data/ProItem.txt/", msgg + "\r\n");
            cs.getSession().close();
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> - ????????????????????????";
        }
    }

    public static class GC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            System.gc();
            System.out.println("????????????????????? ---- " + FileoutputUtil.NowTime());
            return true;
        }

        @Override
        public String getHelp() {
            return " - ?????????????????????";
        }
    }

    public static class SavePlayerShops extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                cserv.closeAllMerchant();
            }
            c.getPlayer().dropMessage(6, "????????????????????????.");
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class GiveFame extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                return false;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

            short fame;
            try {
                fame = Short.parseShort(splitted[2]);
            } catch (Exception nfe) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return false;
            }
            if (victim != null && player.allowedToTarget(victim)) {
                victim.addFame(fame);
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
            } else {
                c.getPlayer().dropMessage(6, "[fame] ???????????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> <??????> ...  - ??????";
        }
    }

    public static class GodMode extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            if (player.isInvincible()) {
                player.setInvincible(false);
                player.dropMessage(6, "??????????????????");
            } else {
                player.setInvincible(true);
                player.clearAllCooldowns();
                player.dropMessage(6, "??????????????????.");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????";
        }
    }

    public static class GiveCash extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter player;
            int amount = 0;
            String name = "";
            try {
                amount = Integer.parseInt(splitted[1]);
                name = splitted[2];
            } catch (Exception ex) {
                return false;
            }
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (player == null) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player.modifyCSPoints(1, amount, true);
            player.dropMessage("????????????Gash??????" + amount + "???");
            String msg = "[GM ??????] GM " + c.getPlayer().getName() + " ?????? " + player.getName() + " Gash?????? " + amount + "???";
            // World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            FileoutputUtil.logToFile("logs/data/????????????.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " GM " + c.getPlayer().getName() + " ?????? " + player.getName() + " Gash?????? " + amount + "???");
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> <??????> - ??????Gash??????";
        }
    }

    public static class ????????? extends GiveMaplePoint {

        @Override
        public String getHelp() {
            return " <??????> <??????> - ??????????????????";
        }
    }

    public static class GiveMaplePoint extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter player;
            int amount = 0;
            String name = "";
            try {
                amount = Integer.parseInt(splitted[1]);
                name = splitted[2];
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (player == null) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player.modifyCSPoints(2, amount, true);
            String msg = "[GM ??????] GM " + c.getPlayer().getName() + " ?????? " + player.getName() + " ???????????? " + amount + "???";
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> <??????> - ??????????????????";
        }
    }

    public static class GivePoint extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter player;
            int amount = 0;
            String name = "";
            try {
                amount = Integer.parseInt(splitted[1]);
                name = splitted[2];
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (player == null) {
                c.getPlayer().dropMessage("?????????????????????");
                return true;
            }
            player.setPoints(player.getPoints() + amount);
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> <??????> - ??????Point";
        }
    }

    public static class GiveMeso extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int gain = Integer.parseInt(splitted[2]);
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "????????? '" + name);
            } else {
                victim.gainMeso(gain, false);
                String msg = "[GM ??????] GM " + c.getPlayer().getName() + " ?????? " + victim.getName() + " ?????? " + gain + "???";
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> <??????> - ???????????????";
        }
    }

    public static class GiveBall extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter victim;
            String name = splitted[1];
            int gain = Integer.parseInt(splitted[2]);
            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "??????????????????");
                return true;
            }
            victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "????????? '" + name);
            } else {
                victim.gainBeans(gain);
                String msg = "[GM ??????] GM " + c.getPlayer().getName() + " ?????? " + victim.getName() + gain + " balls";
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> <??????> - ???????????????";
        }
    }

    public static class MesoEveryone extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int gain = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    mch.gainMeso(gain, true);
                }
            }
            String msg = "[GM ??????] GM " + c.getPlayer().getName() + " ?????? ???????????? ?????? " + gain + "???";
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> - ?????????????????????";
        }
    }

    public static class Item extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int itemId = 0;
            try {
                itemId = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                MaplePet pet = MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance());
                if (pet != null) {
                    MapleInventoryManipulator.addById(c, itemId, (short) 1, c.getPlayer().getName(), pet, ii.getPetLife(itemId));
                }
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " - ???????????????");
            } else {
                IItem item;
                byte flag = 0;
                flag |= ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                    item.setFlag(flag);
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                    if (GameConstants.getInventoryType(itemId) != MapleInventoryType.USE) {
                        item.setFlag(flag);
                    }
                }
                item.setWorld(c.getPlayer().getWorld());
                item.setOwner(c.getPlayer().getName());
                item.setGMLog(c.getPlayer().getName());

                MapleInventoryManipulator.addbyItem(c, item);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????ID> - ????????????";
        }
    }

    public static class ServerMsg extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                for (ChannelServer ch : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                    ch.setServerMessage(sb.toString());
                }
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage(sb.toString()));
            } else {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " ?????? - ????????????????????????";
        }
    }

    public static class MobVac extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
                final MapleMonster monster = (MapleMonster) mmo;
                c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, 0, 0, monster.getObjectId(), monster.getPosition(), c.getPlayer().getPosition(), c.getPlayer().getLastRes()));
                monster.setPosition(c.getPlayer().getPosition());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????";
        }
    }

    public static class ItemVac extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            boolean ItemVac = c.getPlayer().getItemVac();
            if (ItemVac == false) {
                c.getPlayer().stopItemVac();
                c.getPlayer().startItemVac();
            } else {
                c.getPlayer().stopItemVac();
            }
            c.getPlayer().dropMessage(6, "????????????????????????:" + (ItemVac == false ? "??????" : "??????"));
            return true;

        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class ?????????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            final EventManager em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
            if (em != null) {
                if (splitted.length < 2) {
                    em.scheduleSystemEvent();
                } else if (splitted.length < 3) {
                    String ss = splitted[1];
                    String s = splitted[2];
                    if (MapleEventType.getByString(s) != null) {
                        em.scheduleSystemEvent(s);
                    } else {
                        em.scheduleRandomEventInChannel(StringTool.parseInt(ss), StringTool.parseInt(s));
                    }
                } else {
                    em.scheduleSystemEventInChannel(splitted[1], StringTool.parseInt(splitted[2]), StringTool.parseInt(splitted[3]));
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " [????????????] [??????????????????] - ?????????/????????????????????????/??????/??????????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        private static ScheduledFuture<?> ts = null;
        private int min = 1, sec = 0;

        @Override
        public boolean execute(final MapleClient c, String splitted[]) {
            if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                MapleEvent.setEvent(c.getChannelServer(), false);
                if (c.getPlayer().getMapId() == 109020001) {
                    sec = 10;
                    c.getPlayer().dropMessage(5, "??????????????????????????????????????????????????????");
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("??????:" + c.getChannel() + "???????????????????????????????????????????????????????????????"));
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(sec));
                } else {
                    sec = 60;
                    c.getPlayer().dropMessage(5, "??????????????????????????????????????????????????????");
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("??????:" + c.getChannel() + "???????????????????????????????????????????????????????????????"));
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(sec));
                }
                ts = EventTimer.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        if (min == 0) {
                            MapleEvent.onStartEvent(c.getPlayer());
                            ts.cancel(false);
                            return;
                        }
                        min--;
                    }
                }, sec * 1000);
                return true;
            } else {
                c.getPlayer().dropMessage(5, "?????????????????? !???????????? ????????????????????????????????????????????????????????????????????????");
                return true;
            }
        }

        @Override
        public String getHelp() {
            return " - ????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            final MapleEventType type = MapleEventType.getByString(splitted[1]);
            if (type == null) {
                final StringBuilder sb = new StringBuilder("????????????????????????: ");
                for (MapleEventType t : MapleEventType.values()) {
                    sb.append(t.name()).append(",");
                }
                c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
            }
            final String msg = MapleEvent.scheduleEvent(type, c.getChannelServer());
            if (msg.length() > 0) {
                c.getPlayer().dropMessage(5, msg);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????";
        }
    }

    public static class LockItem extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "?????????????????????");
            } else {
                int itemid = Integer.parseInt(splitted[2]);
                MapleInventoryType type = GameConstants.getInventoryType(itemid);
                for (IItem item : chr.getInventory(type).listById(itemid)) {
                    item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                    chr.getClient().sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, item)));
                }
                if (type == MapleInventoryType.EQUIP) {
                    type = MapleInventoryType.EQUIPPED;
                    for (IItem item : chr.getInventory(type).listById(itemid)) {
                        item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                        chr.getClient().sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, item)));
                    }
                }
                c.getPlayer().dropMessage(6, "?????? " + splitted[1] + "????????????ID??? " + splitted[2] + " ???????????????????????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> <??????ID> - ???????????????????????????";
        }
    }

    public static class KillMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.getStat().setHp((short) 0);
                    map.getStat().setMp((short) 0);
                    map.updateSingleStat(MapleStat.HP, 0);
                    map.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.cancelAllBuffs();
                    map.dropMessage(5, "????????????????????????BUFF?????????");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????????????????Buff";
        }
    }

    public static class ???????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.unequipAllPets();
                    map.dropMessage(5, "??????????????????????????????");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????????????????????????????";
        }
    }

    public static class ???????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            String name = splitted[1];
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT pets FROM characters WHERE name = ?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    ps.close();
                    rs.close();
                    c.getPlayer().dropMessage("??????[" + name + "] ?????????????????????");
                    return true;
                }
                try (PreparedStatement pss = con.prepareStatement("Update characters set pets = '-1,-1,-1' Where name = '" + name + "'")) {
                    pss.executeUpdate();
                }
                c.getPlayer().dropMessage("??????[" + name + "] ???????????????????????????!");
            } catch (Exception ex) {
                c.getPlayer().dropMessage("?????????????????? " + ex);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> - ?????????????????????????????????";
        }
    }

    public static class Disease extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                //   c.getPlayer().dropMessage(6, "");
                return false;
            }
            int type;
            MapleDisease dis;
            if (splitted[1].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("CURSE")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("SLOW")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else {
                return false;
            }
            dis = MapleDisease.getByMobSkill(type);
            if (splitted.length == 4) {
                MapleCharacter victim;
                String name = splitted[2];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(ch);
                if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "??????????????????");
                    return true;
                }
                victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

                if (victim == null) {
                    c.getPlayer().dropMessage(5, "??????????????????");
                } else {
                    victim.setChair(0);
                    victim.getClient().sendPacket(MaplePacketCreator.cancelChair(-1));
                    victim.getMap().broadcastMessage(victim, MaplePacketCreator.showChair(c.getPlayer().getId(), 0), false);
                    victim.getDiseaseBuff(dis, MobSkillFactory.getMobSkill(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1)));
                }
            } else {
                for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    victim.setChair(0);
                    victim.getClient().sendPacket(MaplePacketCreator.cancelChair(-1));
                    victim.getMap().broadcastMessage(victim, MaplePacketCreator.showChair(c.getPlayer().getId(), 0), false);
                    victim.getDiseaseBuff(dis, MobSkillFactory.getMobSkill(type, CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1)));
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE> [????????????] <????????????> - ????????????????????????";
        }

    }

    public static class SendAllNote extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {

            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharactersThreadSafe()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                }
            } else {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> ??????Note???????????????????????????";
        }
    }

    public static class CloneMe extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().cloneLook();
            return true;
        }

        @Override
        public String getHelp() {
            return " - ???????????????";
        }
    }

    public static class DisposeClones extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().dropMessage(6, c.getPlayer().getCloneSize() + "?????????????????????.");
            c.getPlayer().disposeClones();
            return true;
        }

        @Override
        public String getHelp() {
            return " - ???????????????";
        }
    }

    public static class Monitor extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.getClient().isMonitored()) {
                    target.getClient().setMonitored(false);
                    c.getPlayer().dropMessage(5, "Not monitoring " + target.getName() + " anymore.");
                } else {
                    target.getClient().setMonitored(true);
                    c.getPlayer().dropMessage(5, "Monitoring " + target.getName() + ".");
                }
            } else {
                c.getPlayer().dropMessage(5, "??????????????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> - ??????????????????";
        }
    }

    public static class PermWeather extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (c.getPlayer().getMap().getPermanentWeather() > 0) {
                c.getPlayer().getMap().setPermanentWeather(0);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeMapEffect());
                c.getPlayer().dropMessage(5, "Map weather has been disabled.");
            } else {
                final int weather = CommandProcessorUtil.getOptionalIntArg(splitted, 1, 5120000);
                if (!MapleItemInformationProvider.getInstance().itemExists(weather) || weather / 10000 != 512) {
                    c.getPlayer().dropMessage(5, "Invalid ID.");
                } else {
                    c.getPlayer().getMap().setPermanentWeather(weather);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.startMapEffect("", weather, false));
                    c.getPlayer().dropMessage(5, "Map weather has been enabled.");
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????";

        }
    }

    public static class Threads extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().contains(filter.toLowerCase())) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????Threads??????";
        }
    }

    public static class ShowTrace extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - show trace info";

        }
    }

    public static class ToggleOffense extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }

            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(6, "Offense " + splitted[1] + " not found");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <Offense> - ???????????????CheatOffense";
        }
    }

    public static class ToggleDrop extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().toggleDrops();
            return true;
        }

        @Override
        public String getHelp() {
            return " - ?????????????????????";

        }
    }

    public static class ToggleMegaphone extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            World.toggleMegaphoneMuteState(c.getPlayer().getMap().getWorld());
            c.getPlayer().dropMessage(6, "?????????????????? : " + (c.getChannelServer().getMegaphoneMuteState() ? "???" : "???"));
            return true;
        }

        @Override
        public String getHelp() {
            return " - ?????????????????????";

        }
    }

    public static class ExpRate extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                        cserv.setExpRate(rate);
                    }
                    c.getPlayer().dropMessage(6, "???????????????????????????????????? " + rate + "x");
                } else {
                    c.getChannelServer().setExpRate(rate);
                    c.getPlayer().dropMessage(6, "??????<" + c.getChannel() + ">????????????????????????" + rate + "x");
                }
            } else {
                c.getPlayer().dropMessage(6, "????????????: !exprate <????????????> [all]");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> - ??????????????????";

        }
    }

    public static class DropRate extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                        cserv.setDropRate(rate);
                    }
                    c.getPlayer().dropMessage(6, "????????????????????????????????? " + rate + "x");
                } else {
                    c.getChannelServer().setDropRate(rate);
                    c.getPlayer().dropMessage(6, "?????????<" + c.getChannel() + ">?????????????????? " + rate + "x");
                }
            } else {
                c.getPlayer().dropMessage(6, "????????????: !droprate <????????????> [all]");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> - ??????????????????";

        }
    }

    public static class MesoRate extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                        cserv.setMesoRate(rate);
                    }
                    c.getPlayer().dropMessage(6, "????????????????????????????????? " + rate + "x");
                } else {
                    c.getChannelServer().setMesoRate(rate);
                    c.getPlayer().dropMessage(6, "?????????<" + c.getChannel() + ">?????????????????? " + rate + "x");
                }
            } else {
                c.getPlayer().dropMessage(6, "????????????: !mesorate <????????????> [all]");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????> - ??????????????????";

        }
    }

    public static class DCAll extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int range = -1;
            if (splitted.length < 2) {
                return false;
            }
            String input = null;
            try {
                input = splitted[1];
            } catch (Exception ex) {

            }
            switch (splitted[1]) {
                case "m":
                    range = 0;
                    break;
                case "c":
                    range = 1;
                    break;
                case "w":
                default:
                    range = 2;
                    break;
            }
            if (range == -1) {
                range = 1;
            }
            switch (range) {
                case 0:
                    c.getPlayer().getMap().disconnectAll(c.getPlayer());
                    break;
//                case 1:
//                    c.getChannelServer().getPlayerStorage().disconnectAll(c.getPlayer());
//                    break;
//                case 2:
//                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
//                        cserv.getPlayerStorage().disconnectAll(true);
//                    }
//                    break;
                default:
                    break;
            }
            String show = "";
            switch (range) {
                case 0:
                    show = "??????";
                    break;
                case 1:
                    show = "??????";
                    break;
                case 2:
                    show = "??????";
                    break;
            }
            String msg = "[GM ??????] GM " + c.getPlayer().getName() + "  DC ??? " + show + "??????";
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(msg));
            return true;
        }

        @Override
        public String getHelp() {
            return " [m|c|w] - ??????????????????";

        }
    }

    public static class KillAll extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            boolean withdrop = false;
            if (splitted.length > 1) {
                int mapid = Integer.parseInt(splitted[1]);
                int irange = 9999;
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                    irange = Integer.parseInt(splitted[2]);
                    range = irange * irange;
                }
                if (splitted.length >= 3) {
                    withdrop = splitted[3].equalsIgnoreCase("true");
                }
            }

            MapleMonster mob;
            if (map == null) {
                c.getPlayer().dropMessage("??????[" + splitted[2] + "] ????????????");
                return true;
            }
            List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.killMonster(mob, c.getPlayer(), withdrop, false, (byte) 1);
            }

            c.getPlayer().dropMessage("??????????????? " + monsters.size() + " ??????");

            return true;
        }

        @Override
        public String getHelp() {
            return " [range] [mapid] - ??????????????????";

        }
    }

    public static class KillMonster extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[1])) {
                    mob.damage(c.getPlayer(), mob.getHp(), false);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <mobid> - ???????????????????????????";

        }
    }

    public static class KillMonsterByOID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.killMonster(monster, c.getPlayer(), false, false, (byte) 1);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <moboid> - ???????????????????????????";

        }
    }

    public static class HitMonsterByOID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <moboid> <damage> - ???????????????????????????";

        }
    }

    public static class NPC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            int npcId = 0;
            try {
                npcId = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "?????????????????????" + npcId + "???Npc");

            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <npcid> - ?????????NPC";
        }
    }

    public static class MakePNPC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                return false;
            }
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleCharacter chhr;
                String name = splitted[1];
                int ch = World.Find.findChannel(name);
                int wl = World.Find.findWorld(ch);
                if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "??????????????????");
                    return true;
                }
                chhr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);

                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " is not online");
                } else {
                    int npcId = Integer.parseInt(splitted[2]);
                    MapleNPC npc_c = MapleLifeFactory.getNPC(npcId);
                    if (npc_c == null || npc_c.getName().equals("MISSINGNO")) {
                        c.getPlayer().dropMessage(6, "NPC?????????");
                        return true;
                    }
                    PlayerNPC npc = new PlayerNPC(chhr, npcId, c.getPlayer().getMap(), c.getPlayer());
                    npc.addToServer();
                    c.getPlayer().dropMessage(6, "Done");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());

            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <playername> <npcid> - ????????????NPC";
        }
    }

    public static class MakeOfflineP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleClient cs = new MapleClient(null, null, new MockIOSession());
                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs, false);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " does not exist");

                } else {
                    PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                    npc.addToServer();
                    c.getPlayer().dropMessage(6, "Done");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());

            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <charname> <npcid> - ????????????PNPC";
        }
    }

    public static class DestroyPNPC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            try {
                c.getPlayer().dropMessage(6, "Destroying playerNPC...");
                final MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if (npc instanceof PlayerNPC) {
                    ((PlayerNPC) npc).destroy(true);
                    c.getPlayer().dropMessage(6, "Done");
                } else {
                    c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " [objectid] - ??????PNPC";
        }

    }

    public static class Spawn extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int mid = 0;
            try {
                mid = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
            if (num > 1000) {
                num = 1000;
            }
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer mp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "mp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pmp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pmp");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");
            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "??????: " + e.getMessage());
                return true;
            }

            long newhp;
            int newexp, newmp;
            if (hp != null) {
                newhp = hp;
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (mp != null) {
                newmp = mp;
            } else if (pmp != null) {
                newmp = (int) (onemob.getMobMaxMp() * (pmp / 100));
            } else {
                newmp = onemob.getMobMaxMp();
            }
            if (exp != null) {
                newexp = exp;
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                mob.setOverrideStats(overrideStats);
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????ID> <hp|exp|php||pexp = ?> - ????????????";
        }
    }

    public static class WarpPlayersTo extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            try {
                final MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                final MapleMap from = c.getPlayer().getMap();
                for (MapleCharacter chr : from.getCharactersThreadsafe()) {
                    chr.changeMap(target, target.getPortal(0));
                }
            } catch (Exception e) {
                return false; //assume drunk GM
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <maipid> ????????????????????????????????????";
        }
    }

    public static class WarpAllHere extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            for (ChannelServer CS : LoginServer.getWorldStatic(c.getPlayer().getWorld()).getChannels()) {
                for (MapleCharacter mch : CS.getPlayerStorage().getAllCharactersThreadSafe()) {
                    if (mch.getMapId() != c.getPlayer().getMapId()) {
                        mch.changeMap(c.getPlayer().getMap(), c.getPlayer().getPosition());
                    }
                    if (mch.getClient().getChannel() != c.getPlayer().getClient().getChannel()) {
                        mch.changeChannel(c.getPlayer().getClient().getChannel());
                    }
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " ??????????????????????????????";
        }
    }

    public static class LOLCastle extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length != 2) {
                return false;
            }
            MapleMap target = c.getChannelServer().getEventSM().getEventManager("lolcastle").getInstance("lolcastle" + splitted[1]).getMapFactory().getMap(990000300, false, false);
            c.getPlayer().changeMap(target, target.getPortal(0));

            return true;
        }

        @Override
        public String getHelp() {
            return " level (level = 1-5) - ???????????????";
        }

    }

    public static class StartProfiling extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            c.getPlayer().dropMessage(6, "????????????????????????JVM??????");
            return true;
        }

        @Override
        public String getHelp() {
            return " ????????????JVM??????";
        }
    }

    public static class StopProfiling extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "??????????????????????????????, ??????????????????????????????");
                    return true;
                }
                sampler.stop();
                try (FileWriter fw = new FileWriter(file)) {
                    sampler.save(fw, 1, 10);
                }
                c.getPlayer().dropMessage(6, "JVM????????????????????????????????????????????????" + filename);
            } catch (IOException e) {
                c.getPlayer().dropMessage(6, "?????????JVM?????????????????????" + e);
                System.err.println("?????????JVM?????????????????????" + e);
            }
            sampler.reset();
            return true;
        }

        @Override
        public String getHelp() {
            return " <filename> - ????????????JVM????????????????????????";
        }
    }

    public static class ReloadMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            final int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                    c.getPlayer().dropMessage(5, "????????????????????? " + cserv.getChannel());
                    return true;
                }
            }
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <maipid> - ??????????????????";
        }
    }

    public static class Respawn extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().respawn(true);
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class ResetMap extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().resetFully();
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class PNPC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {

            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                final int xpos = c.getPlayer().getPosition().x;
                final int ypos = c.getPlayer().getPosition().y;
                final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos);
                npc.setRx1(xpos);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    com.mysql.jdbc.Connection con = (com.mysql.jdbc.Connection) DatabaseConnection.getConnection();
                    try (com.mysql.jdbc.PreparedStatement ps = (com.mysql.jdbc.PreparedStatement) con.prepareStatement("INSERT INTO wz_customlife (dataid, f, hide, fh, cy, rx0, rx1, type, x, y, mid, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0); // 1 = right , 0 = left
                        ps.setInt(3, 0); // 1 = hide, 0 = show
                        ps.setInt(4, fh);
                        ps.setInt(5, ypos);
                        ps.setInt(6, xpos);
                        ps.setInt(7, xpos);
                        ps.setString(8, "n");
                        ps.setInt(9, xpos);
                        ps.setInt(10, ypos);
                        ps.setInt(11, c.getPlayer().getMapId());
                        ps.setInt(12, c.getPlayer().getMap().getWorld());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "NPC?????????????????????" + e);
                }
                for (ChannelServer cs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                    cs.getMapFactory().getMap(c.getPlayer().getMapId()).addMapObject(npc);
                    cs.getMapFactory().getMap(c.getPlayer().getMapId()).broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
                }
                c.getPlayer().dropMessage(6, "??????????????????????????????");
            } else {
                c.getPlayer().dropMessage(6, "????????? Npc ");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????NPC";
        }
    }

    public static class PMOB extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }
            int mobid = Integer.parseInt(splitted[1]);

            int mobTime = Integer.parseInt(splitted[2]);
            MapleMonster npc;
            try {
                npc = MapleLifeFactory.getMonster(mobid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(6, "Error: " + e.getMessage());
                return true;
            }
            if (npc != null) {
                final int xpos = c.getPlayer().getPosition().x;
                final int ypos = c.getPlayer().getPosition().y;
                final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false).getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos);
                npc.setRx1(xpos);
                npc.setFh(fh);
                try {
                    Connection con = DatabaseConnection.getConnection();

                    try (PreparedStatement ps = (PreparedStatement) con.prepareStatement("INSERT INTO wz_customlife (dataid, f, hide, fh, cy, rx0, rx1, type, x, y, mid, mobtime, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, mobid);
                        ps.setInt(2, 0); // 1 = right , 0 = left
                        ps.setInt(3, 0); // 1 = hide, 0 = show
                        ps.setInt(4, fh);
                        ps.setInt(5, ypos);
                        ps.setInt(6, xpos);
                        ps.setInt(7, xpos);
                        ps.setString(8, "m");
                        ps.setInt(9, xpos);
                        ps.setInt(10, ypos);
                        ps.setInt(11, c.getPlayer().getMapId());
                        ps.setInt(12, mobTime);
                        ps.setInt(13, c.getPlayer().getMap().getWorld());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "mob?????????????????????" + e);
                    System.err.println(e);
                }
                for (ChannelServer cs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                    cs.getMapFactory().getMap(c.getPlayer().getMapId()).addMonsterSpawn(npc, mobTime, (byte) -1, null, true);
                    cs.getMapFactory().getMap(c.getPlayer().getMapId()).addMaxMobInMap();
                }
                c.getPlayer().dropMessage(6, "??????????????????????????????.");
            } else {
                c.getPlayer().dropMessage(6, "???????????? Mob-Id");
                return true;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> <????????????> - ????????????????????????";
        }
    }

    public static class AutoDC extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            PiPiConfig.setAutodc(!PiPiConfig.getAutodc());
            c.getPlayer().dropMessage("????????????: " + (PiPiConfig.getAutodc() ? "??????" : "??????"));
            System.out.println("????????????: " + (PiPiConfig.getAutodc() ? "??????" : "??????"));
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class AutoBan extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            PiPiConfig.setAutoban(!PiPiConfig.getAutoban());
            c.getPlayer().dropMessage("????????????: " + (PiPiConfig.getAutoban() ? "??????" : "??????"));
            System.out.println("????????????: " + (PiPiConfig.getAutoban() ? "??????" : "??????"));
            return true;
        }

        @Override
        public String getHelp() {
            return " - ??????????????????";
        }
    }

    public static class Search extends ???????????? {

    }

    public static class ???????????? extends CommandExecute {

        public boolean execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().start(c, 9010000, "AdvancedSearch");
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????????????????";
        }
    }

    public static class Packet extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            int packetheader = Integer.parseInt(splitted[1]);
            String packet_in = " 00 00 00 00 00 00 00 00 00 ";
            if (splitted.length > 2) {
                packet_in = StringUtil.joinStringFrom(splitted, 2);
            }

            mplew.writeShort(packetheader);
            mplew.write(HexTool.getByteArrayFromHexString(packet_in));
            mplew.writeZeroBytes(20);
            c.getSession().write(mplew.getPacket());
            c.getPlayer().dropMessage(packetheader + "???????????????[" + packetheader + "] : " + mplew.toString());
            return true;
        }

        public String getHelp() {
            return " <????????????>";
        }
    }

    public static class UpdateMap extends CommandExecute {

        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                return false;
            }
            boolean custMap = splitted.length >= 2;
            int mapid = custMap ? Integer.parseInt(splitted[1]) : player.getMapId();
            MapleMap map = custMap ? player.getClient().getChannelServer().getMapFactory().getMap(mapid) : player.getMap();
            if (player.getClient().getChannelServer().getMapFactory().destroyMap(mapid)) {
                MapleMap newMap = player.getClient().getChannelServer().getMapFactory().getMap(mapid);
                MaplePortal newPor = newMap.getPortal(0);
                LinkedHashSet<MapleCharacter> mcs = new LinkedHashSet<>(map.getCharacters()); // do NOT remove, fixing ConcurrentModificationEx.
                outerLoop:
                for (MapleCharacter m : mcs) {
                    for (int x = 0; x < 5; x++) {
                        try {
                            m.changeMap(newMap, newPor);
                            continue outerLoop;
                        } catch (Throwable t) {
                        }
                    }
                    player.dropMessage("???????????? " + m.getName() + " ??????????????????. ????????????...");
                }
                player.dropMessage("??????????????????.");
                return true;
            }
            player.dropMessage("??????????????????!");
            return true;
        }

        public String getHelp() {
            return " <mapid> - ??????????????????";
        }
    }

    public static class MaxMeso extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Integer.MAX_VALUE - c.getPlayer().getMeso(), true);
            return true;
        }

        @Override
        public String getHelp() {
            return " - ?????????";
        }
    }

    public static class Mesos extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int meso = 0;
            try {
                meso = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
            }
            c.getPlayer().gainMeso(meso, true);
            return true;
        }

        @Override
        public String getHelp() {
            return " <???????????????> - ????????????";
        }
    }

    public static class Balls extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int ball = 0;
            try {
                ball = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {
            }
            c.getPlayer().gainBeans(ball);
            return true;
        }

        @Override
        public String getHelp() {
            return " <???????????????> - ??????balls";
        }
    }

    public static class Drop extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            if (splitted.length < 2) {
                return false;
            }
            int itemId = 0;
            String name = null;
            try {
                itemId = Integer.parseInt(splitted[1]);
                name = splitted[3];
            } catch (Exception ex) {
            }

            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "??????????????????????????????.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " - ???????????????");
            } else {
                IItem toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                toDrop.setOwner(c.getPlayer().getName());
                toDrop.setGMLog(c.getPlayer().getName());
                toDrop.setWorld(c.getPlayer().getMap().getWorld());
                if (name != null) {
                    int ch = World.Find.findChannel(name);
                    int wl = World.Find.findWorld(ch);
                    if (ch > 0) {
                        MapleCharacter victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                        if (victim != null) {
                            victim.getMap().spawnItemDrop(victim, victim, toDrop, victim.getPosition(), true, true);
                        }
                    } else {
                        c.getPlayer().dropMessage("??????: [" + name + "] ???????????????");
                    }
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????ID> - ????????????";
        }
    }

    public static class ProDrop extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }
            int itemId = 0;
            int quantity = 1;
            int Str = 0;
            int Dex = 0;
            int Int = 0;
            int Luk = 0;
            int HP = 0;
            int MP = 0;
            int Watk = 0;
            int Matk = 0;
            int Wdef = 0;
            int Mdef = 0;
            int Scroll = 0;
            int Upg = 0;
            int Acc = 0;
            int Avoid = 0;
            int jump = 0;
            int speed = 0;
            int day = 0;
            try {
                int splitted_count = 1;
                itemId = Integer.parseInt(splitted[splitted_count++]);
                Str = Integer.parseInt(splitted[splitted_count++]);
                Dex = Integer.parseInt(splitted[splitted_count++]);
                Int = Integer.parseInt(splitted[splitted_count++]);
                Luk = Integer.parseInt(splitted[splitted_count++]);
                HP = Integer.parseInt(splitted[splitted_count++]);
                MP = Integer.parseInt(splitted[splitted_count++]);
                Watk = Integer.parseInt(splitted[splitted_count++]);
                Matk = Integer.parseInt(splitted[splitted_count++]);
                Wdef = Integer.parseInt(splitted[splitted_count++]);
                Mdef = Integer.parseInt(splitted[splitted_count++]);
                Upg = Integer.parseInt(splitted[splitted_count++]);
                Acc = Integer.parseInt(splitted[splitted_count++]);
                Avoid = Integer.parseInt(splitted[splitted_count++]);
                speed = Integer.parseInt(splitted[splitted_count++]);
                jump = Integer.parseInt(splitted[splitted_count++]);
                Scroll = Integer.parseInt(splitted[splitted_count++]);
                day = Integer.parseInt(splitted[splitted_count++]);
            } catch (Exception ex) {
                //   ex.printStackTrace();
            }
            boolean Str_check = Str != 0;
            boolean Int_check = Int != 0;
            boolean Dex_check = Dex != 0;
            boolean Luk_check = Luk != 0;
            boolean HP_check = HP != 0;
            boolean MP_check = MP != 0;
            boolean WATK_check = Watk != 0;
            boolean MATK_check = Matk != 0;
            boolean WDEF_check = Wdef != 0;
            boolean MDEF_check = Mdef != 0;
            boolean SCROLL_check = true;
            boolean UPG_check = Upg != 0;
            boolean ACC_check = Acc != 0;
            boolean AVOID_check = Avoid != 0;
            boolean JUMP_check = jump != 0;
            boolean SPEED_check = speed != 0;
            boolean DAY_check = day != 0;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "????????????????????????.");
                return true;
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " ?????????");
                return true;
            }
            IItem toDrop;
            Equip equip;
            if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {// ?????????????????????
                equip = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                equip.setGMLog(c.getPlayer().getName() + " ?????? !Prodrop");
                if (Str_check) {
                    equip.setStr((short) Str);
                }
                if (Luk_check) {
                    equip.setLuk((short) Luk);
                }
                if (Dex_check) {
                    equip.setDex((short) Dex);
                }
                if (Int_check) {
                    equip.setInt((short) Int);
                }
                if (HP_check) {
                    equip.setHp((short) HP);
                }
                if (MP_check) {
                    equip.setMp((short) MP);
                }
                if (WATK_check) {
                    equip.setWatk((short) Watk);
                }
                if (MATK_check) {
                    equip.setMatk((short) Matk);
                }
                if (WDEF_check) {
                    equip.setWdef((short) Wdef);
                }
                if (MDEF_check) {
                    equip.setMdef((short) Mdef);
                }
                if (ACC_check) {
                    equip.setAcc((short) Acc);
                }
                if (AVOID_check) {
                    equip.setAvoid((short) Avoid);
                }
                if (SCROLL_check) {
                    equip.setUpgradeSlots((byte) Scroll);
                }
                if (UPG_check) {
                    equip.setLevel((byte) Upg);
                }
                if (JUMP_check) {
                    equip.setJump((short) jump);
                }
                if (SPEED_check) {
                    equip.setSpeed((short) speed);
                }
                if (DAY_check) {
                    equip.setExpiration(System.currentTimeMillis() + (day * 24 * 60 * 60 * 1000));
                }
                equip.setWorld(c.getPlayer().getMap().getWorld());
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), equip, c.getPlayer().getPosition(), true, true);
            } else {
                toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                toDrop.setGMLog(c.getPlayer().getName() + " ?????? !Prodrop");
                toDrop.setWorld(c.getPlayer().getMap().getWorld());
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> (<??????> <??????> <??????> <??????> <HP> <MP> <??????> <??????> <??????> <??????> <??????+x> <??????> <??????> <??????> <??????> <?????????> <??????>)";
        }
    }

    public static class ProItem extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }
            int itemId = 0;
            int quantity = 1;
            int Str = 0;
            int Dex = 0;
            int Int = 0;
            int Luk = 0;
            int HP = 0;
            int MP = 0;
            int Watk = 0;
            int Matk = 0;
            int Wdef = 0;
            int Mdef = 0;
            int Scroll = 0;
            int Upg = 0;
            int Acc = 0;
            int Avoid = 0;
            int jump = 0;
            int speed = 0;
            int day = 0;
            try {
                int splitted_count = 1;
                itemId = Integer.parseInt(splitted[splitted_count++]);
                Str = Integer.parseInt(splitted[splitted_count++]);
                Dex = Integer.parseInt(splitted[splitted_count++]);
                Int = Integer.parseInt(splitted[splitted_count++]);
                Luk = Integer.parseInt(splitted[splitted_count++]);
                HP = Integer.parseInt(splitted[splitted_count++]);
                MP = Integer.parseInt(splitted[splitted_count++]);
                Watk = Integer.parseInt(splitted[splitted_count++]);
                Matk = Integer.parseInt(splitted[splitted_count++]);
                Wdef = Integer.parseInt(splitted[splitted_count++]);
                Mdef = Integer.parseInt(splitted[splitted_count++]);
                Upg = Integer.parseInt(splitted[splitted_count++]);
                Acc = Integer.parseInt(splitted[splitted_count++]);
                Avoid = Integer.parseInt(splitted[splitted_count++]);
                speed = Integer.parseInt(splitted[splitted_count++]);
                jump = Integer.parseInt(splitted[splitted_count++]);
                Scroll = Integer.parseInt(splitted[splitted_count++]);
                day = Integer.parseInt(splitted[splitted_count++]);
            } catch (Exception ex) {
                //   ex.printStackTrace();
            }
            boolean Str_check = Str != 0;
            boolean Int_check = Int != 0;
            boolean Dex_check = Dex != 0;
            boolean Luk_check = Luk != 0;
            boolean HP_check = HP != 0;
            boolean MP_check = MP != 0;
            boolean WATK_check = Watk != 0;
            boolean MATK_check = Matk != 0;
            boolean WDEF_check = Wdef != 0;
            boolean MDEF_check = Mdef != 0;
            boolean SCROLL_check = true;
            boolean UPG_check = Upg != 0;
            boolean ACC_check = Acc != 0;
            boolean AVOID_check = Avoid != 0;
            boolean JUMP_check = jump != 0;
            boolean SPEED_check = speed != 0;
            boolean DAY_check = day != 0;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "????????????????????????.");
                return true;
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " ?????????");
                return true;
            }
            IItem toDrop;
            Equip equip;
            if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {// ?????????????????????
                equip = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                equip.setGMLog(c.getPlayer().getName() + " ?????? !Proitem");
                if (Str_check) {
                    equip.setStr((short) Str);
                }
                if (Luk_check) {
                    equip.setLuk((short) Luk);
                }
                if (Dex_check) {
                    equip.setDex((short) Dex);
                }
                if (Int_check) {
                    equip.setInt((short) Int);
                }
                if (HP_check) {
                    equip.setHp((short) HP);
                }
                if (MP_check) {
                    equip.setMp((short) MP);
                }
                if (WATK_check) {
                    equip.setWatk((short) Watk);
                }
                if (MATK_check) {
                    equip.setMatk((short) Matk);
                }
                if (WDEF_check) {
                    equip.setWdef((short) Wdef);
                }
                if (MDEF_check) {
                    equip.setMdef((short) Mdef);
                }
                if (ACC_check) {
                    equip.setAcc((short) Acc);
                }
                if (AVOID_check) {
                    equip.setAvoid((short) Avoid);
                }
                if (SCROLL_check) {
                    equip.setUpgradeSlots((byte) Scroll);
                }
                if (UPG_check) {
                    equip.setLevel((byte) Upg);
                }
                if (JUMP_check) {
                    equip.setJump((short) jump);
                }
                if (SPEED_check) {
                    equip.setSpeed((short) speed);
                }
                if (DAY_check) {
                    equip.setExpiration(System.currentTimeMillis() + (day * 24 * 60 * 60 * 1000));
                }
                equip.setWorld(c.getPlayer().getWorld());
                MapleInventoryManipulator.addbyItem(c, equip);
            } else {
                toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                toDrop.setGMLog(c.getPlayer().getName() + " ?????? !ProItem");
                toDrop.setWorld(c.getPlayer().getWorld());
                MapleInventoryManipulator.addbyItem(c, toDrop);
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> (<??????> <??????> <??????> <??????> <HP> <MP> <??????> <??????> <??????> <??????> <??????+x> <??????> <??????> <??????> <??????> <?????????> <??????>)";
        }
    }

    public static class ????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().start(c, 9900001, "GivePoint");

            /*  if (splitted.length < 4) {
                return false;
            }
            String error = null;
            String input = splitted[1];
            String name = splitted[2];
            int nx = 0;
            int gain = 0;
            try {
                switch (input) {
                    case "??????":
                        nx = 1;
                        break;
                    case "??????":
                        nx = 2;
                        break;
                    default:
                        error = "?????????????????????[??????]???[??????] ??????[" + input + "]";
                        break;
                }
                gain = Integer.parseInt(splitted[3]);
            } catch (Exception ex) {
                error = "???????????????????????????????????????2147483647??? " + input + " ?????????: " + ex.toString();
            }
            if (error != null) {
                c.getPlayer().dropMessage(error);
                return true;
            }

            int ch = World.Find.findChannel(name);
            int wl = World.Find.findWorld(ch);
            if (ch <= 0 || wl == -1) {
                c.getPlayer().dropMessage("??????????????????");
                return true;
            }
            try {
                MapleCharacter victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    c.getPlayer().dropMessage("??????????????????");
                } else {
                    c.getPlayer().dropMessage("??????????????????[" + name + "] " + input + " " + gain);
                    FileoutputUtil.logToFile("logs/data/????????????.txt", "\r\n " + FileoutputUtil.NowTime() + " GM " + c.getPlayer().getName() + " ?????? " + victim.getName() + " " + input + " " + gain + "???");
                    victim.modifyCSPoints(nx, gain, true);
                }
            } catch (Exception ex) {
            FileoutputUtil.printError(name, ex, input);
            }*/
            return true;
        }

        @Override
        public String getHelp() {
            return " ??????/?????? <????????????> <??????>";
        }
    }

    public static class GiveAllCash extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int gash = 0, size = 0;
            try {
                gash = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {

            }
            for (ChannelServer cs : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter chr : cs.getPlayerStorage().getAllCharactersThreadSafe()) {
                    size++;
                    chr.modifyCSPoints(1, gash, true);
                    c.getPlayer().dropMessage("[??????] ??????<" + chr.getName() + "> Lv." + chr.getLevel() + " ??????<" + chr.getMap().getMapName() + "> ????????????[" + gash + "] ???????????????[" + (chr.getCSPoints(1)) + "]");
                }
            }

            HashMap<Integer, Integer> acoffline = getOfflineAcc();
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = null;
            ResultSet rs = null;
            boolean f = true;
            try {
                for (final Map.Entry<Integer, Integer> AC : acoffline.entrySet()) {
                    String sql = "UPDATE accounts SET acash = " + (AC.getValue() + gash) + " where id = " + AC.getKey();
                    ps = con.prepareStatement(sql);
                    ps.execute();
                    ps.close();
                    c.getPlayer().dropMessage("[??????] ????????????<" + AC.getKey() + "> ????????????[" + gash + "] ???????????????[" + (AC.getValue() + gash) + "]");
                    size++;
                }
            } catch (Exception ex) {

            }
            c.getPlayer().dropMessage("????????????" + size + "?????????, ?????????" + size * gash + "???");
            FileoutputUtil.logToFile("logs/data/????????????.txt", "\r\n " + FileoutputUtil.CurrentReadable_Time() + " <" + c.getPlayer().getName() + "> ?????????" + splitted[0] + " ????????????" + size + "?????????, ?????????" + size * gash + " GASH???");

            return true;
        }

        private HashMap<Integer, Integer> getOfflineAcc() {
            HashMap<Integer, Integer> AccIdFromDataBase = new HashMap<>();
            try {
                com.mysql.jdbc.Connection con = (com.mysql.jdbc.Connection) DatabaseConnection.getConnection();
                com.mysql.jdbc.PreparedStatement ps = (com.mysql.jdbc.PreparedStatement) con.prepareStatement("SELECT id, acash FROM accounts WHERE loggedin = 0");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    AccIdFromDataBase.put(rs.getInt("id"), rs.getInt("acash"));
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                System.err.println("getOfflineAcc ????????????(DB):" + e);
            }
            return AccIdFromDataBase;
        }

        @Override
        public String getHelp() {
            return " <??????> - ???????????????????????????Gash";
        }

    }

    public static class GiveAllMP extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int mp = 0, size = 0;
            try {
                mp = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {

            }
            for (ChannelServer cs : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                for (MapleCharacter chr : cs.getPlayerStorage().getAllCharactersThreadSafe()) {
                    size++;
                    chr.modifyCSPoints(2, mp, true);
                    c.getPlayer().dropMessage("[??????] ??????<" + chr.getName() + "> Lv." + chr.getLevel() + " ??????<" + chr.getMap().getMapName() + "> ????????????[" + mp + "] ???????????????[" + (chr.getCSPoints(2)) + "]");
                }
            }

            HashMap<Integer, Integer> acoffline = getOfflineAcc();
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = null;
            ResultSet rs = null;
            boolean f = true;
            try {
                for (final Map.Entry<Integer, Integer> AC : acoffline.entrySet()) {
                    String sql = "UPDATE accounts SET mPoints = " + (AC.getValue() + mp) + " where id = " + AC.getKey();
                    ps = con.prepareStatement(sql);
                    ps.execute();
                    ps.close();
                    c.getPlayer().dropMessage("[??????] ????????????<" + AC.getKey() + "> ????????????[" + mp + "] ???????????????[" + (AC.getValue() + mp) + "]");
                    size++;
                }
            } catch (Exception ex) {

            }
            c.getPlayer().dropMessage("????????????" + size + "?????????, ?????????" + size * mp + "???");
            FileoutputUtil.logToFile("logs/data/????????????.txt", "\r\n " + FileoutputUtil.CurrentReadable_Time() + " <" + c.getPlayer().getName() + "> ?????????" + splitted[0] + " ????????????" + size + "?????????, ?????????" + size * mp + " ??????");

            return true;
        }

        private HashMap<Integer, Integer> getOfflineAcc() {
            HashMap<Integer, Integer> AccIdFromDataBase = new HashMap<>();
            try {
                com.mysql.jdbc.Connection con = (com.mysql.jdbc.Connection) DatabaseConnection.getConnection();
                com.mysql.jdbc.PreparedStatement ps = (com.mysql.jdbc.PreparedStatement) con.prepareStatement("SELECT id, mPoints FROM accounts WHERE loggedin = 0");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    AccIdFromDataBase.put(rs.getInt("id"), rs.getInt("mPoints"));
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                System.err.println("getOfflineAcc ????????????(DB):" + e);
            }
            return AccIdFromDataBase;
        }

        @Override
        public String getHelp() {
            return " <??????> - ???????????????????????????????????????";
        }

    }

    public static class ResetMobs extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            c.getPlayer().getMap().killAllMonsters(false);
            return true;
        }

        @Override
        public String getHelp() {
            return " - ???????????????????????????";
        }
    }

    public static class ??????????????? extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition());
            c.getPlayer().dropMessage(-11, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
            return true;
        }

        @Override
        public String getHelp() {
            return " - ????????????????????????";
        }
    }

    public static class BanGuild extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                return false;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                String GuildName = splitted[1];
                String reason = splitted[2];
                int gid = 0;

                List<String> Characternames = new LinkedList();
                List<String> Successed_off = new LinkedList();
                List<String> failed_off = new LinkedList();
                List<String> Successed = new LinkedList();
                List<String> failed = new LinkedList();
                PreparedStatement ps = con.prepareStatement("select guildid from guilds WHERE name = ?");
                ps.setString(1, GuildName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        gid = rs.getInt("guildid");
                    }
                }
                if (gid == 0) {
                    c.getPlayer().dropMessage(5, "??????[" + GuildName + "]?????????");
                    return true;
                }

                ps = con.prepareStatement("select name from characters WHERE guildid = ?");
                ps.setInt(1, gid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Characternames.add(rs.getString("name"));
                    }
                }
                for (int i = 0; i < Characternames.size(); i++) {
                    int ch = World.Find.findChannel(Characternames.get(i));
                    int wl = World.Find.findWorld(ch);
                    String name = Characternames.get(i);
                    MapleCharacter target;
                    if (ch <= 0) {
                        target = MapleCharacter.getCharacterByName(name);
                        if (target != null && target.getGMLevel() == 0) {
                            if (c.getPlayer().OfflineBanByName(name, reason)) {
                                Successed_off.add(name);
                            } else {
                                failed_off.add(name);
                            }
                        }
                    } else {
                        try {
                            target = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(name);
                            if (target != null) {
                                if (target.getGMLevel() == 0) {
                                    if (target.ban(reason, true, false, false)) {
                                        target.getClient().getSession().close();
                                        target.getClient().disconnect(true, target.getClient().getChannel() == -10);
                                        Successed.add(name);
                                    } else {
                                        failed.add(name);
                                    }
                                }
                            }
                        } catch (Exception ex) {

                        }
                    }
                }
                String msg = "??????????????????: ";
                int total = Successed_off.size() + Successed.size();
                for (int i = 0; i < Successed.size(); i++) {
                    msg += Successed.get(i) + ", ";
                }
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                msg = "??????????????????: ";
                for (int i = 0; i < Successed_off.size(); i++) {
                    msg += Successed_off.get(i) + ", ";
                }
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                msg = "??????????????????: ";
                for (int i = 0; i < failed.size(); i++) {
                    msg += failed.get(i) + ", ";
                }
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));

                msg = "??????????????????: ";
                for (int i = 0; i < failed_off.size(); i++) {
                    msg += failed_off.get(i) + ", ";
                }
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] ?????????" + total + "???????????????," + (failed.size() + failed_off.size()) + "??????????????????"));
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[????????????] ??????<" + GuildName + "> ??????????????????????????????????????????????????????"));
                FileoutputUtil.logToFile("Logs/Hack/??????????????????.txt", "\r\n " + FileoutputUtil.CurrentReadable_TimeGMT() + " " + c.getPlayer().getName() + " ???????????????<" + GuildName + "> ??????: " + reason);

            } catch (SQLException e) {
                c.getPlayer().dropMessage(6, "??????????????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> <??????> - ????????????";
        }
    }

    public static class UnbanGuild extends CommandExecute {// TODO: TEST

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                String GuildName = splitted[1];
                int gid = 0;

                List<String> Characternames = new LinkedList();
                List<String> Successed = new LinkedList();
                List<String> failed = new LinkedList();
                PreparedStatement ps = con.prepareStatement("select guildid from guilds WHERE name = ?");
                ps.setString(1, GuildName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        gid = rs.getInt("guildid");
                    }
                }
                if (gid == 0) {
                    c.getPlayer().dropMessage(5, "??????[" + GuildName + "]?????????");
                    return true;
                }

                ps = con.prepareStatement("select name from characters WHERE guildid = ?");
                ps.setInt(1, gid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Characternames.add(rs.getString("name"));
                    }
                }

                for (int i = 0; i < Characternames.size(); i++) {
                    String name = Characternames.get(i);
                    if (MapleClient.Fullyunban(name)) {
                        Successed.add(name);
                    } else {
                        failed.add(name);
                    }
                }

                String msg = "????????????: ";
                for (int i = 0; i < Successed.size(); i++) {
                    msg += Successed.get(i) + ", ";
                }
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                msg = "????????????: ";
                for (int i = 0; i < failed.size(); i++) {
                    msg += failed.get(i) + ", ";
                }

                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + msg));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("-------------------------------------------------------------------------------------"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] ?????????" + Successed.size() + "?????????," + failed.size() + "??????????????????"));
                FileoutputUtil.logToFile("Logs/Hack/??????????????????.txt", "\r\n " + FileoutputUtil.CurrentReadable_TimeGMT() + " " + c.getPlayer().getName() + " ???????????????<" + GuildName + ">");

            } catch (SQLException e) {
                c.getPlayer().dropMessage(6, "??????????????????");
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <????????????> - ????????????";
        }
    }

    public static class SetRate extends CommandExecute {

        public boolean execute(MapleClient c, String splitted[]) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter mc = player;
            if (splitted.length > 3) {
                String input = splitted[1].toLowerCase();
                int arg = Integer.parseInt(splitted[2]);
                int mins = Integer.parseInt(splitted[3]);
                boolean bOk = true;
                if (input.equals("??????") || input.equals("exp")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                        cservs.setExExpRate(arg);
                        cservs.broadcastPacket(MaplePacketCreator.getItemNotice("????????????????????????????????? " + arg + "???????????????????????????.?????????????????????????????????????????????"));
                    }
                } else if (input.equals("??????") || input.equals("drop")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                        cservs.setExDropRate(arg);
                        cservs.broadcastPacket(MaplePacketCreator.getItemNotice("????????????????????????????????? " + arg + "???????????????????????????.?????????????????????????????????????????????"));
                    }
                } else if (input.equals("??????") || input.equals("meso")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                        cservs.setExMesoRate(arg);
                        cservs.broadcastPacket(MaplePacketCreator.getItemNotice("??????????????????????????? " + arg + "???????????????????????????.?????????????????????????????????????????????"));
                    }
                } else {
                    bOk = false;
                }
                if (bOk) {
                    World.scheduleRateDelay(input, mins, c.getWorld());
                } else {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????/exp/??????/drop/??????/meso> <??????> <?????????> - ??????????????????(????????????)";
        }
    }

    public static class SetExRate extends CommandExecute {

        public boolean execute(MapleClient c, String splitted[]) {
            String input = splitted[1].toLowerCase();
            int arg = Integer.parseInt(splitted[2]);
            if (input.equals("??????") || input.equals("exp")) {
                for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                    cservs.setExExpRate(arg);
                    cservs.broadcastPacket(MaplePacketCreator.getItemNotice("????????????????????????????????? " + arg + "??????????????????????????????"));
                }
            } else if (input.equals("??????") || input.equals("drop")) {
                for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                    cservs.setExDropRate(arg);
                    cservs.broadcastPacket(MaplePacketCreator.getItemNotice("????????????????????????????????? " + arg + "??????????????????????????????"));
                }
            } else if (input.equals("??????") || input.equals("meso")) {
                for (ChannelServer cservs : LoginServer.getWorldStatic(c.getWorld()).getChannels()) {
                    cservs.setExMesoRate(arg);
                    cservs.broadcastPacket(MaplePacketCreator.getItemNotice("????????????????????????????????? " + arg + "??????????????????????????????"));
                }
            } else {
                return false;
            }
            return true;
        }

        @Override
        public String getHelp() {
            return " <??????/exp/??????/drop/??????/meso> <??????> - ????????????????????????";
        }
    }
}
