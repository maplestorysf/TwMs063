package handling.world;

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import client.BuddyEntry;
import client.MapleBuffStat;

import client.MapleCharacter;
import client.MapleClient;

import client.MapleCoolDownValueHolder;
import client.MapleDiseaseValueHolder;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.channel.handler.PartyHandler.PartySearchJob;
import handling.login.LoginServer;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import handling.world.guild.MapleGuildSummary;
import java.util.Collection;
import scripting.ReactorScriptManager;
import server.Randomizer;
import server.Timer;
import server.Timer.WorldTimer;
import server.life.MapleMonsterInformationProvider;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import tools.CollectionUtil;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;

public class World {

    public static boolean isShutDown = false;

    public static void init() {
        World.Find.findChannel(0);
        World.Guild.lock.toString();
        World.Alliance.lock.toString();
        World.Family.lock.toString();
        World.Messenger.getMessenger(0);
        World.Party.getParty(0);
    }

    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new HashMap<>();
        int total = 0;
        for (World wl : LoginServer.getWorlds()) {
            for (ChannelServer cs : wl.getChannels()) {
                int curConnected = cs.getConnectedClients();
                ret.put(cs.getChannel(), curConnected);
                total += curConnected;
            }
        }
        ret.put(0, total);
        return ret;
    }

    public static List<CheaterData> getCheaters() {
        List<CheaterData> allCheaters = new ArrayList<>();

        for (World wl : LoginServer.getWorlds()) {
            for (ChannelServer cserv : wl.getChannels()) {
                allCheaters.addAll(cserv.getCheaters());
            }
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 10);
    }

    public static boolean isConnected(String charName) {
        return Find.findChannel(charName) > 0;
    }

    public static void toggleMegaphoneMuteState(int world) {
        for (ChannelServer cs : LoginServer.getWorldStatic(world).getChannels()) {
            cs.toggleMegaphoneMuteState();
        }
    }

    public static void channelChangeData(CharacterTransfer Data, MapleClient c, int characterid, int world, int toChannel) {
        getStorage(world, toChannel).registerPendingPlayer(Data, characterid);
        getStorage(world, toChannel).registerPendingClient(c, characterid);

    }

    public static void clearChannelChangeDataByAccountId(int accountid) {
        try {
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cs : wl.getChannels()) {
                    getStorage(cs.getWorld(), cs.getChannel()).deregisterPendingPlayerByAccountId(accountid);
                }
            }
            getStorage(0, -20).deregisterPendingPlayerByAccountId(accountid);
            getStorage(0, -10).deregisterPendingPlayerByAccountId(accountid);
        } catch (Exception ex) {

        }
    }

    public static boolean isCharacterListConnected(List<String> charName) {
        for (World wl : LoginServer.getWorlds()) {
            for (ChannelServer cs : wl.getChannels()) {
                for (final String c : charName) {
                    if (cs.getPlayerStorage().getCharacterByName(c) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int getMerchantChannel(int accountID) {
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cs : worlds.getChannels()) {
                return cs.getMerchantCh(accountID);
            }
        }
        return -1;
    }

    public static int getMerchantMap(int world, int accountID) {
        for (ChannelServer cs : LoginServer.getWorldStatic(world).getChannels()) {
            return cs.getMerchantMap(accountID);
        }

        return -1;
    }

    public static boolean hasMerchant(int world, int accountID) {
        for (ChannelServer cs : LoginServer.getWorldStatic(world).getChannels()) {
            if (cs.containsMerchant(accountID)) {
                return true;
            }
        }
        return false;
    }

    private List<ChannelServer> channels = new ArrayList<>();

    public static PlayerStorage getStorage(int world, int channel) {
        if (channel == -20) {
            return CashShopServer.getPlayerStorageMTS();
        } else if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(world, channel).getPlayerStorage();
    }

    public ChannelServer getChannel(int channel) {
        return channels.get(channel - 1);
    }

    public static class Party {

        private static Map<Integer, MapleParty> parties = new HashMap<>();
        private static final AtomicInteger runningPartyId = new AtomicInteger();

        static {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = con.prepareStatement("SELECT MAX(party)+2 FROM characters");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    runningPartyId.set(rs.getInt(1));
                }
                ps.close();
            } catch (SQLException e) {

            }
        }

        public static void partyChat(int partyid, String chattext, String namefrom) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                throw new IllegalArgumentException("no party with the specified partyid exists");
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null && !chr.getName().equalsIgnoreCase(namefrom)) { //Extra check just in case
                        chr.getClient().sendPacket(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                    }
                }
            }
        }

        public static void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return; //Don't update, just return. And definitely don't throw a damn exception.
                //throw new IllegalArgumentException("no party with the specified partyid exists");
            }
            switch (operation) {
                case JOIN:
                    party.addMember(target);
                    break;
                case EXPEL:
                case LEAVE:
                    party.removeMember(target);
                    break;
                case DISBAND:
                    disbandParty(partyid);
                    break;
                case SILENT_UPDATE:
                case LOG_ONOFF:
                    party.updateMember(target);
                    break;
                case CHANGE_LEADER:
                case CHANGE_LEADER_DC:
                    party.setLeader(target);
                    break;
                default:
                    throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) {
                        if (operation == PartyOperation.DISBAND) {
                            chr.setParty(null);
                        } else {
                            chr.setParty(party);
                        }
                        chr.getClient().sendPacket(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                    }
                }
            }
            switch (operation) {
                case LEAVE:
                case EXPEL:
                    int ch = Find.findChannel(target.getName());
                    int wl = Find.findWorld(target.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(target.getName());
                        if (chr != null) {
                            chr.getClient().sendPacket(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                            chr.setParty(null);
                        }
                    }
            }
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor) {
            int partyid = runningPartyId.getAndIncrement();
            MapleParty party = new MapleParty(partyid, chrfor);
            parties.put(party.getId(), party);
            return party;
        }

        public static MapleParty getParty(int partyid) {
            return parties.get(partyid);
        }

        public static MapleParty disbandParty(int partyid) {
            return parties.remove(partyid);
        }
    }

    public static class PartySearch {

        private static final Map<MapleCharacter, PartySearchInfo> searches = new HashMap();

        public static class PartySearchInfo {

            private final int minLevel;
            private final int maxLevel;
            private final int memberNum;
            private final int jobMask;

            private PartySearchInfo(int minLevel, int maxLevel, int memberNum, int jobMask) {
                this.minLevel = minLevel;
                this.maxLevel = maxLevel;
                this.memberNum = memberNum;
                this.jobMask = jobMask;
            }

            public boolean isInLevel(int level) {
                return level >= minLevel && level <= maxLevel;
            }

            public int getMemberNum() {
                return memberNum;
            }

            public boolean isAllowJob(int job) {
                return PartySearchJob.checkJob(jobMask, job);
            }
        }

        public static void startSearch(MapleCharacter chr, int minLevel, int maxLevel, int memberNum, int jobMask) {
            if (chr == null || chr.getMap() == null) {
                return;
            }
            stopSearch(chr);

            searches.put(chr, new PartySearchInfo(minLevel, maxLevel, memberNum, jobMask));

            List<MapleCharacter> players = chr.getMap().getCharacters();
            for (MapleCharacter player : players) {
                checkPartySearch(player);
            }
        }

        public static void stopSearch(MapleCharacter chr) {
            if (chr == null) {
                return;
            }
            if (searches.containsKey(chr)) {
                searches.remove(chr);
            }
        }

        public static void checkPartySearch(MapleCharacter chr) {
            if (chr == null || chr.getParty() != null) {
                return;
            }
            for (Map.Entry<MapleCharacter, PartySearchInfo> entry : searches.entrySet()) {
                MapleCharacter player = entry.getKey();
                PartySearchInfo psi = entry.getValue();
                if (player.getParty() != null && psi != null) {
                    if (player.getParty().getMembers().size() >= psi.getMemberNum() || player.getParty().getMembers().size() >= 6) {
                        stopSearch(player);
                        return;
                    }
                    if (player.getMap() == chr.getMap()) {
                        if (psi.isInLevel(chr.getLevel()) && psi.isAllowJob(chr.getJob())) {
                            if (player.getParty().getMembers().size() + 1 >= psi.getMemberNum() || player.getParty().getMembers().size() >= 6) {
                                stopSearch(player);
                            }
                            chr.getClient().sendPacket(MaplePacketCreator.partyInvite(player, true));
                            return;
                        }
                    }
                }
            }
        }
    }

    public static class Buddy {

        public static void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
            for (int characterId : recipientCharacterIds) {
                int ch = Find.findChannel(characterId);
                int wl = Find.findWorld(characterId);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(characterId);
                    if (chr != null && chr.getBuddylist().containsVisible(cidFrom)) {
                        chr.getClient().sendPacket(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                    }
                }
            }
        }

        private static void updateBuddies(int characterId, int channel, Collection<Integer> buddies, boolean offline, int gmLevel, boolean isHidden) {
            for (Integer buddy : buddies) {
                int ch = Find.findChannel(buddy);
                int wl = Find.findWorld(buddy);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(buddy);
                    if (chr != null) {
                        BuddyEntry ble = chr.getBuddylist().get(characterId);
                        if (ble != null && ble.isVisible()) {
                            int mcChannel;
                            if (offline || (isHidden && chr.getGMLevel() < gmLevel)) {
                                ble.setChannel(-1);
                                mcChannel = -1;
                            } else {
                                ble.setChannel(channel);
                                mcChannel = channel - 1;
                            }
                            chr.getBuddylist().put(ble);
                            chr.getClient().sendPacket(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                        }
                    }
                }
            }
        }

        public static void buddyChanged(int cid, int cidFrom, String name, int world, int channel, BuddyOperation operation, String group) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(cid);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    switch (operation) {
                        case ADDED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddyEntry(name, cidFrom, group, world, channel, true));
                                addChar.getClient().sendPacket(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                            }
                            break;
                        case DELETED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddyEntry(name, cidFrom, group, -1, -1, buddylist.get(cidFrom).isVisible()));
                                addChar.getClient().sendPacket(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                            }
                            break;
                    }
                }
            }
        }

        public static BuddyAddResult requestBuddyAdd(String addName, int worldFrom, int channelFrom, int cidFrom, String nameFrom) {
            int ch = Find.findChannel(addName);
            int wl = Find.findWorld(addName);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(addName);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    if (buddylist.isFull()) {
                        return BuddyAddResult.BUDDYLIST_FULL;
                    }
                    if (!buddylist.contains(cidFrom)) {
                        buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, worldFrom, channelFrom);
                    } else {
                        if (buddylist.containsVisible(cidFrom)) {
                            return BuddyAddResult.ALREADY_ON_LIST;
                        }
                    }
                }
            }
            return BuddyAddResult.OK;
        }

        public static void loggedOn(String name, int characterId, int channel, Collection<Integer> buddies, int gmLevel, boolean isHidden) {
            updateBuddies(characterId, channel, buddies, false, gmLevel, isHidden);
        }

        public static void loggedOff(String name, int characterId, int channel, Collection<Integer> buddies, int gmLevel, boolean isHidden) {
            updateBuddies(characterId, channel, buddies, true, gmLevel, isHidden);
        }
    }

    public static class Messenger {

        private static final Map<Integer, MapleMessenger> messengers = new HashMap<>();
        private static final AtomicInteger runningMessengerId = new AtomicInteger();

        static {
            runningMessengerId.set(1);
        }

        public static MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
            int messengerid = runningMessengerId.getAndIncrement();
            MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
            messengers.put(messenger.getId(), messenger);
            return messenger;
        }

        public static void declineChat(String target, String namefrom) {
            int ch = Find.findChannel(target);
            int wl = Find.findWorld(target);
            if (ch > 0) {
                ChannelServer cs = ChannelServer.getInstance(wl, ch);
                MapleCharacter chr = cs.getPlayerStorage().getCharacterByName(target);
                if (chr != null) {
                    MapleMessenger messenger = chr.getMessenger();
                    if (messenger != null) {
                        chr.getClient().sendPacket(MaplePacketCreator.messengerNote(namefrom, 5, 0));
                    }
                }
            }
        }

        public static MapleMessenger getMessenger(int messengerid) {
            return messengers.get(messengerid);
        }

        public static void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            int position = messenger.getPositionByName(target.getName());
            messenger.removeMember(target);

            for (MapleMessengerCharacter mmc : messenger.getMembers()) {
                if (mmc != null) {
                    int ch = Find.findChannel(mmc.getId());
                    int wl = Find.findWorld(mmc.getId());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(mmc.getName());
                        if (chr != null) {
                            chr.getClient().sendPacket(MaplePacketCreator.removeMessengerPlayer(position));
                        }
                    }
                }
            }
        }

        public static void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentRemoveMember(target);
        }

        public static void silentJoinMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentAddMember(target);
        }

        public static void updateMessenger(int messengerid, String namefrom, int fromworld, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            int position = messenger.getPositionByName(namefrom);

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            MapleCharacter from = ChannelServer.getInstance(fromworld, fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                            chr.getClient().sendPacket(MaplePacketCreator.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                        }
                    }
                }
            }
        }

        public static void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromworld, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.addMember(target);
            int position = messenger.getPositionByName(target.getName());
            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null) {
                    int mposition = messenger.getPositionByName(messengerchar.getName());
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            if (!messengerchar.getName().equals(from)) {
                                MapleCharacter fromCh = ChannelServer.getInstance(fromworld, fromchannel).getPlayerStorage().getCharacterByName(from);
                                chr.getClient().sendPacket(MaplePacketCreator.addMessengerPlayer(from, fromCh, position, fromchannel - 1));
                                fromCh.getClient().sendPacket(MaplePacketCreator.addMessengerPlayer(chr.getName(), chr, mposition, messengerchar.getChannel() - 1));
                            } else {
                                chr.getClient().sendPacket(MaplePacketCreator.joinMessenger(mposition));
                            }
                        }
                    }
                }
            }
        }

        public static void messengerChat(int messengerid, String chattext, String namefrom) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {

                            chr.getClient().sendPacket(MaplePacketCreator.messengerChat(chattext));
                        }
                    }
                } //Whisp Monitor Code
                else if (messengerchar != null) {
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                    }
                }
                //
            }
        }

        public static void messengerInvite(String sender, int messengerid, String target, int fromchannel, boolean gm) {

            if (isConnected(target)) {

                int ch = Find.findChannel(target);
                int wl = Find.findWorld(target);
                if (ch > 0) {
                    MapleCharacter from = ChannelServer.getInstance(wl, fromchannel).getPlayerStorage().getCharacterByName(sender);
                    MapleCharacter targeter = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(target);
                    if (from != null) {
                        if (targeter != null && targeter.getMessenger() == null) {
                            if (!targeter.isGM() || gm) {
                                targeter.getClient().sendPacket(MaplePacketCreator.messengerInvite(sender, messengerid));
                                from.getClient().sendPacket(MaplePacketCreator.messengerNote(target, 4, 1));
                            } else {
                                from.getClient().sendPacket(MaplePacketCreator.messengerNote(target, 4, 0));
                            }
                        } else {
                            from.getClient().sendPacket(MaplePacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
                        }
                    }
                }
            }

        }
    }

    public static class Guild {

        private static final Map<Integer, MapleGuild> guilds = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        static {
            System.out.println("【讀取中】 Guilds :::");
            Collection<MapleGuild> allGuilds = MapleGuild.loadAll();
            for (MapleGuild g : allGuilds) {
                if (g.isProper()) {
                    guilds.put(g.getId(), g);
                }
            }
        }

        public static int createGuild(int leaderId, String name) {
            return MapleGuild.createGuild(leaderId, name);
        }

        public static MapleGuild getGuild(int id) {
            MapleGuild ret = null;
            lock.readLock().lock();
            try {
                ret = guilds.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuild(id);
                    if (ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    guilds.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret; //Guild doesn't exist?
        }

        public static MapleGuild getGuildByName(String guildName) {
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (g.getName().equalsIgnoreCase(guildName)) {
                        return g;
                    }
                }
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static MapleGuild getGuild(MapleCharacter mc) {
            return getGuild(mc.getGuildId());
        }

        public static void setGuildMemberOnline(MapleGuildCharacter mc, boolean bOnline, int channel) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.setOnline(mc.getId(), bOnline, channel);
            }
        }

        public static void guildPacket(int gid, byte[] message) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.broadcast(message);
            }
        }

        public static int addGuildMember(MapleGuildCharacter mc) {
            return addGuildMember(mc, true);
        }

        public static int addGuildMember(MapleGuildCharacter mc, boolean show) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                return g.addGuildMember(mc, show);
            }
            return 0;
        }

        public static void leaveGuild(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.leaveGuild(mc);
            }
        }

        public static void guildChat(int gid, String name, int cid, String msg) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.guildChat(name, cid, msg);
            }
        }

        public static void changeRank(int gid, int cid, int newRank) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRank(cid, newRank);
            }
        }

        public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
            MapleGuild g = getGuild(initiator.getGuildId());
            if (g != null) {
                g.expelMember(initiator, name, cid);
            }
        }

        public static void setGuildNotice(int gid, String notice) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildNotice(notice);
            }
        }

        public static void memberLevelJobUpdate(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.memberLevelJobUpdate(mc);
            }
        }

        public static void changeRankTitle(int gid, String[] ranks) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRankTitle(ranks);
            }
        }

        public static void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildEmblem(bg, bgcolor, logo, logocolor);
            }
        }

        public static void disbandGuild(int gid) {
            MapleGuild g = getGuild(gid);
            lock.writeLock().lock();
            try {
                if (g != null) {
                    g.disbandGuild();
                    guilds.remove(gid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void deleteGuildCharacter(int guildid, int charid) {

            //ensure it's loaded on world server
            //setGuildMemberOnline(mc, false, -1);
            MapleGuild g = getGuild(guildid);
            if (g != null) {
                MapleGuildCharacter mc = g.getMGC(charid);
                if (mc != null) {
                    if (mc.getGuildRank() > 1) //not leader
                    {
                        g.leaveGuild(mc);
                    } else {
                        g.disbandGuild();
                    }
                }
            }
        }

        public static boolean increaseGuildCapacity(int gid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.increaseCapacity();
            }
            return false;
        }

        public static void gainGP(int gid, int amount) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount);
            }
        }

        public static int getGP(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getGP();
            }
            return 0;
        }

        public static int getInvitedId(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getInvitedId();
            }
            return 0;
        }

        public static void setInvitedId(final int gid, final int inviteid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setInvitedId(inviteid);
            }
        }

        public static int getGuildLeader(final String guildName) {
            final MapleGuild mga = getGuildByName(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void save() {
            System.out.println("儲存公會資料...");
            lock.writeLock().lock();
            try {
                for (MapleGuild a : guilds.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static List<MapleBBSThread> getBBS(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getBBS();
            }
            return null;
        }

        public static int addBBSThread(final int guildid, final String title, final String text, final int icon, final boolean bNotice, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                return g.addBBSThread(title, text, icon, bNotice, posterID);
            }
            return -1;
        }

        public static final void editBBSThread(final int guildid, final int localthreadid, final String title, final String text, final int icon, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.editBBSThread(localthreadid, title, text, icon, posterID, guildRank);
            }
        }

        public static final void deleteBBSThread(final int guildid, final int localthreadid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSThread(localthreadid, posterID, guildRank);
            }
        }

        public static final void addBBSReply(final int guildid, final int localthreadid, final String text, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.addBBSReply(localthreadid, text, posterID);
            }
        }

        public static final void deleteBBSReply(final int guildid, final int localthreadid, final int replyid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSReply(localthreadid, replyid, posterID, guildRank);
            }
        }

        public static void changeEmblem(int gid, int affectedPlayers, MapleGuildSummary mgs) {
            Broadcast.sendGuildPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1, gid);
            setGuildAndRank(affectedPlayers, -1, -1, -1);	//respawn player
        }

        public static void setGuildAndRank(int cid, int guildid, int rank, int alliancerank) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);
            if (ch == -1) {
                return;
            }
            MapleCharacter mc = getStorage(wl, ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferentGuild;
            if (guildid == -1 && rank == -1) { //just need a respawn
                bDifferentGuild = true;
            } else {
                bDifferentGuild = guildid != mc.getGuildId();
                mc.setGuildId(guildid);
                mc.setGuildRank((byte) rank);
                mc.setAllianceRank((byte) alliancerank);
                mc.saveGuildStatus();
            }
            if (bDifferentGuild && ch > 0) {
                mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
                mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
            }
        }
    }

    public static class Broadcast {

        public static void broadcastSmega(int worldid, byte[] message) {
            for (ChannelServer cs : LoginServer.getInstance().getWorld(worldid).getChannels()) {
                cs.broadcastSmega(message);
            }
        }

        public static void broadcastEtcSmega(int worldid, byte[] message) {
            for (ChannelServer cs : LoginServer.getInstance().getWorld(worldid).getChannels()) {
                cs.broadcastEtcSmega(message);
            }
        }

        public static void broadcastGMMessage(byte[] message, boolean 吸怪) {
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cs : wl.getChannels()) {
                    cs.broadcastGMMessage(message, 吸怪);
                }
            }
        }

        public static void broadcastGMMessage(int wl, byte[] message) {
            for (ChannelServer cs : LoginServer.getWorldStatic(wl).getChannels()) {
                cs.broadcastGMMessage(message);
            }
        }

        public static void broadcastGMMessage(byte[] message) {
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cs : wl.getChannels()) {
                    cs.broadcastGMMessage(message);
                }
            }
        }

        public static void broadcastMessage(int world, byte[] message) {
            for (ChannelServer cs : LoginServer.getWorldStatic(world).getChannels()) {
                cs.broadcastMessage(message);
            }
        }

        public static void broadcastMessage(byte[] message) {
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cs : wl.getChannels()) {
                    cs.broadcastMessage(message);
                }
            }
        }

        public static void sendPacket(List<Integer> targetIds, byte[] packet, int exception) {
            MapleCharacter c;
            for (int i : targetIds) {
                if (i == exception) {
                    continue;
                }
                int ch = Find.findChannel(i);
                int wl = Find.findWorld(i);
                if (ch < 0) {
                    continue;
                }
                c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(i);
                if (c != null) {
                    c.getClient().sendPacket(packet);
                }
            }
        }

        public static void sendGuildPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            int wl = Find.findWorld(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getGuildId() == guildid) {
                c.getClient().sendPacket(packet);
            }
        }

        public static void sendFamilyPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            int wl = Find.findWorld(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getFamilyId() == guildid) {
                c.getClient().sendPacket(packet);
            }
        }
    }

    public static class Find {

        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private static final HashMap<Integer, Integer> idToChannel = new HashMap<>();
        private static final HashMap<Integer, Integer> idToWorld = new HashMap<>();
        private static final HashMap<Integer, Long> DC_List = new HashMap<>();
        //private static final HashMap<String, Integer> nameToChannel = new HashMap<>();

        public static void register(int id, String name, int channel, int world) {
            lock.writeLock().lock();
            try {
                idToChannel.put(id, channel);
                idToWorld.put(id, world);
                //nameToChannel.put(name.toLowerCase(), channel);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void forceDeregister(int id) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
                idToWorld.remove(id);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void forceDeregister(String id) {
            lock.writeLock().lock();
            try {
                //nameToChannel.remove(id.toLowerCase());
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void forceDeregister(int id, String name) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
                idToWorld.remove(id);

                //nameToChannel.remove(name.toLowerCase());
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void registerDisconnect(int id) {
            lock.writeLock().lock();
            try {
                DC_List.put(id, System.currentTimeMillis());
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void forceDeregisterDisconnect(int id) {
            lock.writeLock().lock();
            try {
                DC_List.remove(id);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static int findDisconnect(int id) {
            Integer ret = null;
            lock.readLock().lock();
            try {
                if (DC_List.containsKey(id)) {
                    if ((System.currentTimeMillis() - DC_List.get(id)) / 1000 <= 60) {
                        ret = id;
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null) {
                return ret;
            }
            return -1;
        }

        public static int findChannel(int id) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = idToChannel.get(id);
                ret_ = idToWorld.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static int findChannel(String st) {
            Integer ret = null;
            Integer ret_ = null;
            lock.readLock().lock();
            try {
                MapleCharacter target = null;
                outerloop:
                for (World wl : LoginServer.getWorlds()) {
                    for (ChannelServer ch : wl.getChannels()) {
                        target = ch.getPlayerStorage().getCharacterByName(st);
                        if (target != null) {
                            ret = ch.getChannel();
                            ret_ = ch.getWorld();
                            break outerloop;
                        }
                    }
                }
                // 修復商城內角色無法讀取
                if (target == null) {
                    target = CashShopServer.getPlayerStorage().getCharacterByName(st);
                    if (target != null) {
                        ret = -10;
                    }
                }
                //ret = nameToChannel.get(st.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static MapleCharacter findChr(String st) {
            Integer ret = null;
            Integer ret_ = null;

            MapleCharacter target = null;
            outerloop:
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer ch : wl.getChannels()) {
                    target = ch.getPlayerStorage().getCharacterByName(st);
                    if (target != null) {
                        ret = ch.getChannel();
                        ret_ = ch.getWorld();
                        break outerloop;
                    }
                }
            }

            if (ret_ != null && ret != null) {
                if (ChannelServer.getInstance(ret_, ret) == null) {
                    return CashShopServer.getPlayerStorage().getCharacterByName(st);
                } else {
                    return ChannelServer.getInstance(ret_, ret).getPlayerStorage().getCharacterByName(st);
                }
            }

            return null;
        }

        public static MapleCharacter findChr(int st) {
            Integer ret;
            Integer ret_;

            lock.readLock().lock();
            try {
                ret = idToChannel.get(st);
                ret_ = idToWorld.get(st);
            } finally {
                lock.readLock().unlock();
            }
            if (ret_ != null && ret != null) {
                if (ChannelServer.getInstance(ret_, ret) == null) {
                    return CashShopServer.getPlayerStorage().getCharacterById(st);
                } else {
                    return ChannelServer.getInstance(ret_, ret).getPlayerStorage().getCharacterById(st);
                }
            }
            return null;
        }

        public static int findWorld(String st) {
            Integer ret = null;
            Integer ret_ = null;
            lock.readLock().lock();
            try {
                MapleCharacter target = null;
                outerloop:
                for (World wl : LoginServer.getWorlds()) {
                    for (ChannelServer ch : wl.getChannels()) {
                        target = ch.getPlayerStorage().getCharacterByName(st);
                        if (target != null) {
                            ret = ch.getChannel();
                            ret_ = ch.getWorld();
                            break outerloop;
                        }
                    }
                }
                // 修復商城內角色無法讀取
                if (target == null) {
                    target = CashShopServer.getPlayerStorage().getCharacterByName(st);
                    if (target != null) {
                        ret = -10;
                    }
                }
                //ret = nameToChannel.get(st.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret_;
            }
            return -1;
        }

        public static int findWorld(int id) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = idToChannel.get(id);
                ret_ = idToWorld.get(id); // get the world's channel :)
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret_;
            }
            return -1;
        }

        public static CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, Collection<Integer> characterIds) {
            List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.size());
            for (Integer i : characterIds) {
                Integer channel = findChannel(i);
                if (channel > 0) {
                    foundsChars.add(new CharacterIdChannelPair(i, channel));
                }
            }
            Collections.sort(foundsChars);
            return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
        }
    }

    public static class Alliance {

        private static final Map<Integer, MapleGuildAlliance> alliances = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        static {
            System.out.println("【讀取中】 GuildAlliances :::");
            Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
            for (MapleGuildAlliance g : allGuilds) {
                alliances.put(g.getId(), g);
            }
        }

        public static MapleGuildAlliance getAlliance(final int allianceid) {
            MapleGuildAlliance ret = null;
            lock.readLock().lock();
            try {
                ret = alliances.get(allianceid);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuildAlliance(allianceid);
                    if (ret.getId() <= 0) { //failed to load
                        return null;
                    }
                    alliances.put(allianceid, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static int getAllianceLeader(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void updateAllianceRanks(final int allianceid, final String[] ranks) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setRank(ranks);
            }
        }

        public static void updateAllianceNotice(final int allianceid, final String notice) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setNotice(notice);
            }
        }

        public static boolean canInvite(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getCapacity() > mga.getNoGuilds();
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid);
            }
            return false;
        }

        public static boolean changeAllianceRank(final int allianceid, final int cid, final int change) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.changeAllianceRank(cid, change);
            }
            return false;
        }

        public static boolean changeAllianceCapacity(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setCapacity();
            }
            return false;
        }

        public static boolean disbandAlliance(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.disband();
            }
            return false;
        }

        public static boolean addGuildToAlliance(final int allianceid, final int gid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.addGuild(gid);
            }
            return false;
        }

        public static boolean removeGuildFromAlliance(final int allianceid, final int gid, final boolean expelled) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.removeGuild(gid, expelled);
            }
            return false;
        }

        public static void sendGuild(final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                sendGuild(MaplePacketCreator.getAllianceUpdate(alliance), -1, allianceid);
                sendGuild(MaplePacketCreator.getGuildAlliance(alliance), -1, allianceid);
            }
        }

        public static void sendGuild(final byte[] packet, final int exceptionId, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    int gid = alliance.getGuildId(i);
                    if (gid > 0 && gid != exceptionId) {
                        Guild.guildPacket(gid, packet);
                    }
                }
            }
        }

        public static boolean createAlliance(final String alliancename, final int cid, final int cid2, final int gid, final int gid2) {
            final int allianceid = MapleGuildAlliance.createToDb(cid, alliancename, gid, gid2);
            if (allianceid <= 0) {
                return false;
            }
            final MapleGuild g = Guild.getGuild(gid), g_ = Guild.getGuild(gid2);
            g.setAllianceId(allianceid);
            g_.setAllianceId(allianceid);
            g.changeARank(true);
            g_.changeARank(false);

            final MapleGuildAlliance alliance = getAlliance(allianceid);

            sendGuild(MaplePacketCreator.createGuildAlliance(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.getAllianceInfo(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.getGuildAlliance(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.changeAlliance(alliance, true), -1, allianceid);
            return true;
        }

        public static void allianceChat(final int gid, final String name, final int cid, final String msg) {
            final MapleGuild g = Guild.getGuild(gid);
            if (g != null) {
                final MapleGuildAlliance ga = getAlliance(g.getAllianceId());
                if (ga != null) {
                    for (int i = 0; i < ga.getNoGuilds(); i++) {
                        final MapleGuild g_ = Guild.getGuild(ga.getGuildId(i));
                        if (g_ != null) {
                            g_.allianceChat(name, cid, msg);
                        }
                    }
                }
            }
        }

        public static void setNewAlliance(final int gid, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild guild = Guild.getGuild(gid);
            if (alliance != null && guild != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    if (gid == alliance.getGuildId(i)) {
                        guild.setAllianceId(allianceid);
                        guild.broadcast(MaplePacketCreator.getAllianceInfo(alliance));
                        guild.broadcast(MaplePacketCreator.getGuildAlliance(alliance));
                        guild.broadcast(MaplePacketCreator.changeAlliance(alliance, true));
                        guild.changeARank();
                        guild.writeToDB(false);
                    } else {
                        final MapleGuild g_ = Guild.getGuild(alliance.getGuildId(i));
                        if (g_ != null) {
                            g_.broadcast(MaplePacketCreator.addGuildToAlliance(alliance, guild));
                            g_.broadcast(MaplePacketCreator.changeGuildInAlliance(alliance, guild, true));
                        }
                    }
                }
            }
        }

        public static void setOldAlliance(final int gid, final boolean expelled, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild g_ = Guild.getGuild(gid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    final MapleGuild guild = Guild.getGuild(alliance.getGuildId(i));
                    if (guild == null) {
                        if (gid != alliance.getGuildId(i)) {
                            alliance.removeGuild(gid, false);
                        }
                        continue; //just skip
                    }
                    if (g_ == null || gid == alliance.getGuildId(i)) {
                        guild.changeARank(5);
                        guild.setAllianceId(0);
                        guild.broadcast(MaplePacketCreator.disbandAlliance(allianceid));
                    } else {
                        guild.broadcast(MaplePacketCreator.getErrorNotice("[" + g_.getName() + "] Guild has left the alliance."));
                        guild.broadcast(MaplePacketCreator.changeGuildInAlliance(alliance, g_, false));
                        guild.broadcast(MaplePacketCreator.removeGuildFromAlliance(alliance, g_, expelled));
                    }

                }
            }

            if (gid == -1) {
                lock.writeLock().lock();
                try {
                    alliances.remove(allianceid);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public static List<byte[]> getAllianceInfo(final int allianceid, final boolean start) {
            List<byte[]> ret = new ArrayList<>();
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                if (start) {
                    ret.add(MaplePacketCreator.getAllianceInfo(alliance));
                    ret.add(MaplePacketCreator.getGuildAlliance(alliance));
                }
                ret.add(MaplePacketCreator.getAllianceUpdate(alliance));
            }
            return ret;
        }

        public static void save() {
            System.out.println("儲存聯盟資料...");
            lock.writeLock().lock();
            try {
                for (MapleGuildAlliance a : alliances.values()) {
                    a.saveToDb();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public static class Family {

        private static final Map<Integer, MapleFamily> families = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        static {
            System.out.println("【讀取中】 Families :::");
            Collection<MapleFamily> allGuilds = MapleFamily.loadAll();
            for (MapleFamily g : allGuilds) {
                if (g.isProper()) {
                    families.put(g.getId(), g);
                }
            }
        }

        public static MapleFamily getFamily(int id) {
            MapleFamily ret = null;
            lock.readLock().lock();
            try {
                ret = families.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleFamily(id);
                    if (ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    families.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static void memberFamilyUpdate(MapleFamilyCharacter mfc, MapleCharacter mc) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.memberLevelJobUpdate(mc);
            }
        }

        public static void setFamilyMemberOnline(MapleFamilyCharacter mfc, boolean bOnline, int channel) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.setOnline(mfc.getId(), bOnline, channel);
            }
        }

        public static int setRep(int fid, int cid, int addrep, int oldLevel) {
            MapleFamily f = getFamily(fid);
            if (f != null) {
                return f.setRep(cid, addrep, oldLevel);
            }
            return 0;
        }

        public static void save() {
            System.out.println("儲存家族資料...");
            lock.writeLock().lock();
            try {
                for (MapleFamily a : families.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void setFamily(int familyid, int seniorid, int junior1, int junior2, int currentrep, int totalrep, int cid) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);

            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(wl, ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferent = mc.getFamilyId() != familyid || mc.getSeniorId() != seniorid || mc.getJunior1() != junior1 || mc.getJunior2() != junior2;
            mc.setFamily(familyid, seniorid, junior1, junior2);
            mc.setCurrentRep(currentrep);
            mc.setTotalRep(totalrep);
            if (bDifferent) {
                mc.saveFamilyStatus();
            }
        }

        public static void familyPacket(int gid, byte[] message, int cid) {
            MapleFamily f = getFamily(gid);
            if (f != null) {
                f.broadcast(message, -1, f.getMFC(cid).getPedigree());
            }
        }

        public static void disbandFamily(int gid) {
            MapleFamily g = getFamily(gid);
            lock.writeLock().lock();
            try {
                if (g != null) {
                    g.disbandFamily();
                    families.remove(gid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public static void registerRespawn() {
        WorldTimer.getInstance().register(new Respawn(), 3000); //divisible by 9000 if possible.
        //3000 good or bad? ive no idea >_>
        //buffs can also be done, but eh
    }

    public static class Respawn implements Runnable { //is putting it here a good idea?

        private int numTimes = 0;

        @Override
        public void run() {
            numTimes++;
            for (World world : LoginServer.getWorlds()) {
                for (ChannelServer cserv : world.getChannels()) {
                    Collection<MapleMap> maps = cserv.getMapFactory().getAllMapThreadSafe();
                    for (MapleMap map : maps) { //iterating through each map o_x
                        handleMap(map, numTimes, map.getCharactersSize());
                    }
                    maps = cserv.getMapFactory().getAllInstanceMaps();
                    for (MapleMap map : maps) {
                        handleMap(map, numTimes, map.getCharactersSize());
                    }
                }
            }
            if (numTimes % 4800 == 0) {
                MapleMonsterInformationProvider.getInstance().clearDrops();
                ReactorScriptManager.getInstance().clearDrops();
            }
        }
    }

    public static void handleMap(final MapleMap map, final int numTimes, final int size) {
        if (map.getItemsSize() > 0) {
            for (MapleMapItem item : map.getAllItemsThreadsafe()) {
                if (item.shouldExpire()) {
                    item.expire(map);
                } else if (item.shouldFFA()) {
                    item.setDropType((byte) 2);
                }
            }
        }
        if (map.characterSize() > 0) {
            if (map.canSpawn()) {
                map.respawn(false);
            }
            boolean hurt = map.canHurt();
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                handleCooldowns(chr, numTimes, hurt);
            }
        }
    }

    public static void handleCooldowns(final MapleCharacter chr, final int numTimes, final boolean hurt) { //is putting it here a good idea? expensive?
        final long now = System.currentTimeMillis();
        for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
            if (m.startTime + m.length < now) {
                final int skil = m.skillId;
                chr.removeCooldown(skil);
                chr.getClient().sendPacket(MaplePacketCreator.skillCooldown(skil, 0));
            }
        }
        if (chr.getDiseaseSize() > 0) {
            for (MapleDiseaseValueHolder m : chr.getAllDiseases()) {
                if (m != null && m.startTime + m.length < now) {
                    chr.dispelDebuff(m.disease);
                }
            }
        }
        for (MapleDiseaseValueHolder m : chr.getAllDiseases()) {
            if (m.startTime + m.length < now) {
                chr.dispelDebuff(m.disease);
            }
        }
        if (numTimes % 100 == 0) { //we're parsing through the characters anyway (:
            for (MaplePet pet : chr.getSummonedPets()) {
                if (pet.getSummoned()) {
                    if (pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0) {
                        pet.setLimitedLife(pet.getSecondsLeft() - 1);
                        if (pet.getSecondsLeft() <= 0) {
                            chr.unequipPet(pet, true, true);
                            return;
                        }
                    }
                    int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getPetItemId());
                    if (newFullness <= 5) {
                        pet.setFullness(15);
                        chr.unequipPet(pet, true, true);
                    } else {
                        pet.setFullness(newFullness);
                        chr.getClient().sendPacket(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition())));
                    }
                }
            }
        }
        MapleMount mount = chr.getMount();
        if (numTimes % 100 == 0) { // Mount...Fatigue ? maybe each 3 sec run a time..... 
            if (chr.getBuffSource(MapleBuffStat.MONSTER_RIDING) == GameConstants.isMonsterRiding(mount.getSkillId())) {
                if (mount.Riding) {
                    if (chr != null) {
                        chr.dropMessage(5, "騎寵目前疲勞值:" + (mount.getFatigue() + 1));
                    }
                } else {
                    mount.Riding = true;
                }
                mount.increaseFatigue();
            }
        }

        if (chr.isAlive()) {
            if (chr.getJob() == 131 || chr.getJob() == 132) {
                if (chr.canBlood(now)) {
                    chr.doDragonBlood();
                }
                if (chr.canBerserk()) {
                    chr.doBerserk();
                }
            }
            if (chr.canRecover(now)) {
                chr.doRecovery();
            }
            if (hurt) {
                if (chr.getInventory(MapleInventoryType.EQUIPPED).findById(chr.getMap().getHPDecProtect()) == null) {
                    if (chr.getMapId() == 749040100 && chr.getInventory(MapleInventoryType.CASH).findById(5451000) == null) { //minidungeon
                        chr.addHP(-chr.getMap().getHPDec());
                    } else if (chr.getMapId() != 749040100) {
                        chr.addHP(-chr.getMap().getHPDec());
                    }
                }
            }
        }
    }

    public static void AutoClean(final int mapid) {

        Timer.EventTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                for (World world : LoginServer.getWorlds()) {
                    for (ChannelServer cserv : world.getChannels()) {
                        MapleMap map = cserv.getMapFactory().getMap(mapid);
                        map.killAllMonsters(false);
                        map.removeDrops();
                    }
                }
            }
        }, 10 * 60 * 1000, 10 * 60 * 1000);
    }

    public static void AutoSave(int min) {

        Timer.EventTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                for (World world : LoginServer.getWorlds()) {
                    for (ChannelServer cs : world.getChannels()) {
                        for (MapleCharacter chr : cs.getPlayerStorage().getAllCharactersThreadSafe()) {
                            // ??
                            if (chr != null) {
                                //存檔
                                if (chr.getClient().getLoginState() != 5 && chr.getTrade() == null && chr.getConversation() <= 0 && chr.getPlayerShop() == null && chr.getMap() != null) {
                                    chr.saveToDB(false, false);
                                }
                            }
                        }
                    }
                }
            }
        }, min * 60 * 1000, min * 60 * 1000);
    }

    public static void GainNX(int min) {

        Timer.EventTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                Map<MapleCharacter, Integer> GiveList = new HashMap();
                int quantity = Randomizer.rand(1, 10);
                int quantity1 = Randomizer.rand(1, 10);
                int quantity2 = Randomizer.rand(1, 10);
                int quantity3 = Randomizer.rand(1, 10);
                int quantity4 = Randomizer.rand(1, 10);
                int quantity5 = Randomizer.rand(10, 35);
                int quantity6 = Randomizer.rand(15, 40);
                int quantity7 = Randomizer.rand(15, 45);
                int quantity8 = Randomizer.rand(15, 50);
                int quantity9 = Randomizer.rand(15, 55);
                int quantity10 = Randomizer.rand(15, 60);
                for (World world : LoginServer.getWorlds()) {
                    for (ChannelServer cs : world.getChannels()) {
                        for (MapleCharacter chr : cs.getPlayerStorage().getAllCharactersThreadSafe()) {
                            // ??
                            if (chr == null) {
                                continue;
                            }
                            // 非活著無法領
                            if (!chr.isAlive()) {
                                continue;
                            }
                            int gain = quantity;
                            switch (chr.getVip()) {
                                case 1:
                                    gain = quantity1;
                                    break;
                                case 2:
                                    gain = quantity2;
                                    break;
                                case 3:
                                    gain = quantity3;
                                    break;
                                case 4:
                                    gain = quantity4;
                                    break;
                                case 5:
                                    gain = quantity5;
                                    break;
                                case 6:
                                    gain = quantity6;
                                    break;
                                case 7:
                                    gain = quantity7;
                                    break;
                                case 8:
                                    gain = quantity8;
                                    break;
                                case 9:
                                    gain = quantity9;
                                    break;
                                case 10:
                                    gain = quantity10;
                                    break;
                            }
                            GiveList.put(chr, gain);
                        }
                    }
                }
                if (!GiveList.isEmpty()) {
                    MapleCharacter.setMP(GiveList, true);
                }
            }
        }, min * 60 * 1000, min * 60 * 1000);
    }

    public static void ClearMemory(int min) {
        Timer.EventTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                System.gc();
                System.out.println("系統自動釋放記憶體 ---- " + FileoutputUtil.NowTime());
            }
        }, min * 60 * 1000, min * 60 * 1000);
    }

    public static void scheduleRateDelay(final String type, long delay, final int world) {
        WorldTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                final String rate = type;
                if (rate.equals("經驗")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(world).getChannels()) {
                        cservs.setExExpRate(1);
                    }
                } else if (rate.equals("掉寶")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(world).getChannels()) {
                        cservs.setExDropRate(1);
                    }
                } else if (rate.equals("楓幣")) {
                    for (ChannelServer cservs : LoginServer.getWorldStatic(world).getChannels()) {
                        cservs.setExMesoRate(1);
                    }
                }
                Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice(rate + "雙倍活動已經結束。系統已成功自動切換為正常遊戲模式！"));
            }
        }, delay * 60 * 1000);
    }
    private static final List<Integer> playerSaveLockes = new ArrayList<Integer>();
    private static final ReentrantReadWriteLock saveLock = new ReentrantReadWriteLock();
    private static final List<Integer> playerStorageLockes = new ArrayList<Integer>();
    private static final ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();

    public static final boolean isPlayerSaving(Integer accountid) {
        saveLock.readLock().lock();
        boolean ret = false;
        try {
            ret = playerSaveLockes.contains(accountid);
        } finally {
            saveLock.readLock().unlock();
        }
        return ret;
    }

    public static final void addPlayerSaving(Integer accountid) {
        saveLock.writeLock().lock();
        try {
            playerSaveLockes.add(accountid);
        } finally {
            saveLock.writeLock().unlock();
        }
    }

    public static final void removePlayerSaving(Integer accountid) {
        saveLock.writeLock().lock();
        try {
            playerSaveLockes.remove(accountid);
        } finally {
            saveLock.writeLock().unlock();
        }
    }

    public static final boolean isPlayerStorage(Integer accountid) {
        storageLock.readLock().lock();
        boolean ret = false;
        try {
            ret = playerStorageLockes.contains(accountid);
        } finally {
            storageLock.readLock().unlock();
        }
        return ret;
    }

    public static final void addPlayerStorage(Integer accountid) {
        storageLock.writeLock().lock();
        try {
            playerStorageLockes.add(accountid);
        } finally {
            storageLock.writeLock().unlock();
        }
    }

    public static final void removePlayerStorage(Integer accountid) {
        storageLock.writeLock().lock();
        try {
            playerStorageLockes.remove(accountid);
        } finally {
            storageLock.writeLock().unlock();
        }
    }
    private static PlayerStorage players = new PlayerStorage(-1);
    private int id, flag, expRate, mesoRate, dropRate, cashRate = 3, traitRate = 3, flags = 0, userLimit, chamount;
    private String eventMessage;
    private boolean canCreate;

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public World(int world, int flag, String eventMessage, int exprate, int mesorate, int droprate, int chamount, boolean canCreate) {
        this.id = world;
        this.flag = flag;
        this.eventMessage = eventMessage;
        this.expRate = exprate;
        this.mesoRate = mesorate;
        this.dropRate = droprate;
        this.chamount = chamount;
        this.canCreate = canCreate;
    }

    public int getChAmount() {
        return chamount;
    }

    public int getWorldId() {
        return id;
    }

    public int getExpRate() {
        return expRate;
    }

    public void setExpRate(int exp) {
        this.expRate = exp;
    }

    public int getDropRate() {
        return dropRate;
    }

    public void setDropRate(int drop) {
        this.dropRate = drop;
    }

    public int getMesoRate() {
        return mesoRate;
    }

    public void setMesoRate(int meso) {
        this.mesoRate = meso;
    }

    public void setFlag(byte b) {
        this.flag = b;
    }

    public int getFlag() {
        return flag;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String message) {
        this.eventMessage = message;
    }

    public List<ChannelServer> getChannels() {
        return channels;
    }

    public void addChannel(ChannelServer channel) {
        channels.add(channel);
    }

    public void removeChannel(int channel) {
        channels.remove(channel);
    }

}
