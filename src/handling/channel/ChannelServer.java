/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConfig;
import constants.WorldConstants;
import handling.cashshop.CashShopServer;
import handling.login.LoginServer;
import handling.netty.ServerConnection;
import handling.world.CharacterTransfer;
import handling.world.CheaterData;
import handling.world.World;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventScriptManager;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.maps.MapleMapFactory;
import server.shops.HiredMerchant;
import tools.MaplePacketCreator;
import server.life.PlayerNPC;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import server.ServerProperties;
import server.events.MapleCoconut;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.events.MapleFitness;
import server.events.MapleOla;
import server.events.MapleOxQuiz;
import server.events.MapleSnowball;
import server.events.MapleJewel;
import server.maps.MapleMapObject;
import server.shops.MaplePlayerShop;
import tools.CollectionUtil;
import tools.ConcurrentEnumMap;

public class ChannelServer implements Serializable {

    public static long serverStartTime;
    private int port = 7575;
    private static final short DEFAULT_PORT = 7575;
    private final int world, channel;
    private int running_MerchantID = 0;
    private int running_PlayerShopID = 0;
    private String socket;
    private boolean shutdown = false, finishedShutdown = false, MegaphoneMuteState = false;
    private PlayerStorage players;
    private ServerConnection acceptor;
    private final MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private static final Map<Integer, ChannelServer> instances = new HashMap<>();
    private final Map<MapleSquadType, MapleSquad> mapleSquads = new ConcurrentEnumMap<>(MapleSquadType.class);
    private final Map<Integer, HiredMerchant> merchants = new HashMap<>();
    private final Map<Integer, MaplePlayerShop> playershops = new HashMap<>();
    private final Map<Integer, PlayerNPC> playerNPCs = new HashMap<>();
    private final ReentrantReadWriteLock merchLock = new ReentrantReadWriteLock(); //merchant
    private final ReentrantReadWriteLock squadLock = new ReentrantReadWriteLock(); //squad
    private int eventmap = -1;
    private final Map<MapleEventType, MapleEvent> events = new EnumMap<>(MapleEventType.class);
    private int extend_Exp = 1, extend_Drop = 1, extend_Meso = 1;

    private ChannelServer(final int world, final int channel) {
        this.world = world;
        this.channel = channel;
        setChannel(channel);
        // System.out.println("world: " + world + " ch : " + channel);
        mapFactory = new MapleMapFactory(world, channel);
    }

    public static Set<Integer> getAllChannels() {
        return new HashSet<>(instances.keySet());
    }

    public final void loadEvents() {
        if (!events.isEmpty()) {
            return;
        }
        events.put(MapleEventType.打瓶蓋, new MapleCoconut(world, channel, MapleEventType.打瓶蓋.mapids));
        events.put(MapleEventType.打果子, new MapleCoconut(world, channel, MapleEventType.打果子.mapids));
        events.put(MapleEventType.終極忍耐, new MapleFitness(world, channel, MapleEventType.終極忍耐.mapids));
        events.put(MapleEventType.爬繩子, new MapleOla(world, channel, MapleEventType.爬繩子.mapids));
        events.put(MapleEventType.是非題大考驗, new MapleOxQuiz(world, channel, MapleEventType.是非題大考驗.mapids));
        events.put(MapleEventType.滾雪球, new MapleSnowball(world, channel, MapleEventType.滾雪球.mapids));
        events.put(MapleEventType.尋寶, new MapleJewel(world, channel, MapleEventType.尋寶.mapids));
        //  events.put(MapleEventType.你的生死, new Mapledie(channel, MapleEventType.你的生死.mapids));
    }

    public final void setup() {
        setChannel(channel); //instances.put
        try {
            eventSM = new EventScriptManager(this, ServerProperties.getProperty("server.settings.events").split(","));
            port = ((ServerProperties.getProperty("server.settings.channel.port", DEFAULT_PORT) + channel) - 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        socket = ServerConfig.IP + ":" + port;

        players = new PlayerStorage(channel);
        loadEvents();
        acceptor = new ServerConnection(ServerConfig.IP, port, 0, channel);
        acceptor.run();
        System.out.println("【頻道" + getChannel() + "】  - 監聽端口: " + port + "");
        eventSM.init();
    }

    public final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        broadcastPacket(MaplePacketCreator.serverNotice("【頻道" + getChannel() + "】 這個頻道正在關閉中."));
        shutdown = true;

//        System.out.println("【頻道" + getChannel() + "】 儲存商人資料...");
//
//        closeAllMerchant();
        System.out.println("【頻道" + getChannel() + "】 儲存角色資料...");

        //    getPlayerStorage().disconnectAll();
        System.out.println("【頻道" + getChannel() + "】 解除端口綁定中...");

        try {
            if (acceptor != null) {
                acceptor.close();
                System.out.println("【頻道" + getChannel() + "】 解除端口成功");
            }
        } catch (Exception e) {
            System.out.println("【頻道" + getChannel() + "】 解除端口失敗");
        }

        instances.remove(channel);
        LoginServer.removeChannel(world, channel);
        setFinishShutdown();

    }

    public final boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public final MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public final void addPlayer(final MapleCharacter chr) {
        getPlayerStorage().registerPlayer(chr);
        chr.getClient().sendPacket(MaplePacketCreator.serverMessage(getServerMessage()));
    }

    public final PlayerStorage getPlayerStorage() {
        if (players == null) { //wth
            players = new PlayerStorage(channel); //wthhhh
        }
        return players;
    }

    public final void removePlayer(final MapleCharacter chr) {
        getPlayerStorage().deregisterPlayer(chr);
    }

    public final void removePlayer(final int idz, final String namez) {
        getPlayerStorage().deregisterPlayer(idz, namez);

    }

    public final String getServerMessage() {
        return WorldConstants.SCROLL_MESSAGE;
    }

    public final void setServerMessage(final String newMessage) {
        WorldConstants.SCROLL_MESSAGE = newMessage;
    }

    public final void broadcastPacket(final byte[] data) {
        getPlayerStorage().broadcastPacket(data);
    }

    public final void broadcastSmegaPacket(final byte[] data) {
        getPlayerStorage().broadcastSmegaPacket(data);
    }
    
    public final void broadcastEtcSmegaPacket(final byte[] data) {
        getPlayerStorage().broadcastEtcSmegaPacket(data);
    }
    
    public final void broadcastGMPacket(final byte[] data) {
        getPlayerStorage().broadcastGMPacket(data);
    }

    public final void broadcastGMPacket(final byte[] data, boolean 吸怪) {
        getPlayerStorage().broadcastGMPacket(data, 吸怪);
    }

    public final int getExpRate() {
        return LoginServer.getWorldStatic(world).getExpRate() * extend_Exp;
    }

    public final void setExpRate(final int expRate) {
        LoginServer.getWorldStatic(world).setExpRate(expRate);
    }

    public final int getExExpRate() {
        return extend_Exp;
    }

    public final void setExExpRate(final int expRate) {
        extend_Exp = expRate;
    }

    public final int getMesoRate() {
        return LoginServer.getWorldStatic(world).getMesoRate() * extend_Meso;
    }

    public final void setMesoRate(final int mesoRate) {
        LoginServer.getWorldStatic(world).setMesoRate(mesoRate);
    }

    public final int getExMesoRate() {
        return extend_Meso;
    }

    public final void setExMesoRate(final int mesoRate) {
        extend_Meso = mesoRate;
    }

    public final int getDropRate() {
        return LoginServer.getWorldStatic(world).getDropRate() * extend_Drop;
    }

    public final void setDropRate(final int dropRate) {
        LoginServer.getWorldStatic(world).setDropRate(dropRate);
    }

    public final int getExDropRate() {
        return extend_Drop;
    }

    public final void setExDropRate(final int dropRate) {
        extend_Drop = dropRate;
    }

    public final String getIP() {
        return ServerConfig.IP;
    }

    public final int getChannel() {
        return channel;
    }

    public final int getWorld() {
        return world;
    }

    public final void setChannel(final int channel) {
        instances.put(channel, this);
        LoginServer.addChannel(world, channel);
    }

    public static final Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public final String getSocket() {
        return socket;
    }

    public final boolean isShutdown() {
        return shutdown;
    }

    public final int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public final EventScriptManager getEventSM() {
        return eventSM;
    }

    public final void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerProperties.getProperty("server.settings.events").split(","));
        eventSM.init();
    }

    public Map<MapleSquadType, MapleSquad> getAllSquads() {
        return Collections.unmodifiableMap(mapleSquads);
    }

    public final MapleSquad getMapleSquad(final String type) {
        return getMapleSquad(MapleSquadType.valueOf(type.toLowerCase()));
    }

    public final MapleSquad getMapleSquad(final MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public final boolean addMapleSquad(final MapleSquad squad, final String type) {
        final MapleSquadType types = MapleSquadType.valueOf(type.toLowerCase());
        if (types != null && !mapleSquads.containsKey(types)) {
            mapleSquads.put(types, squad);
            squad.scheduleRemoval();
            return true;
        }
        return false;
    }

    public final boolean removeMapleSquad(final MapleSquadType types) {
        if (types != null && mapleSquads.containsKey(types)) {
            mapleSquads.remove(types);
            return true;
        }
        return false;
    }

    public final int closeAllPlayerShop() {
        int ret = 0;
        merchLock.writeLock().lock();
        try {
            final Iterator<Map.Entry<Integer, MaplePlayerShop>> playershops_ = playershops.entrySet().iterator();
            while (playershops_.hasNext()) {
                MaplePlayerShop hm = playershops_.next().getValue();
                hm.closeShop(true, false);
                hm.getMap().removeMapObject(hm);
                playershops_.remove();
                ret++;
            }
        } finally {
            merchLock.writeLock().unlock();
        }
        return ret;
    }

    public final int closeAllMerchant() {
        int ret = 0;
        merchLock.writeLock().lock();
        try {
            final Iterator<Map.Entry<Integer, HiredMerchant>> merchants_ = merchants.entrySet().iterator();
            while (merchants_.hasNext()) {
                HiredMerchant hm = merchants_.next().getValue();
                hm.closeShop(true, false);
                //HiredMerchantSave.QueueShopForSave(hm);
                hm.getMap().removeMapObject(hm);
                merchants_.remove();
                ret++;
            }
        } finally {
            merchLock.writeLock().unlock();
        }
        return ret;
    }

    public final int addPlayerShop(final MaplePlayerShop PlayerShop) {
        merchLock.writeLock().lock();

        int runningmer = 0;
        try {
            runningmer = running_PlayerShopID;
            playershops.put(running_PlayerShopID, PlayerShop);
            running_PlayerShopID++;
        } finally {
            merchLock.writeLock().unlock();
        }
        return runningmer;
    }

    public final int addMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();

        int runningmer = 0;
        try {
            runningmer = running_MerchantID;
            merchants.put(running_MerchantID, hMerchant);
            running_MerchantID++;
        } finally {
            merchLock.writeLock().unlock();
        }
        return runningmer;
    }

    public final int getMerchantCh(final int accid) {
        int ch = -1;

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hired = ((HiredMerchant) itr.next());
                if (hired.getOwnerAccId() == accid) {
                    ch = getChannel();
                    break;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return ch;
    }

    public final void removeMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();

        try {
            merchants.remove(hMerchant.getStoreId());
        } finally {
            merchLock.writeLock().unlock();
        }
    }

    public final boolean containsMerchant(final int accid) {
        boolean contains = false;

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                if (((HiredMerchant) itr.next()).getOwnerAccId() == accid) {
                    contains = true;
                    break;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return contains;
    }

    public final List<HiredMerchant> searchMerchant(final int itemSearch) {
        final List<HiredMerchant> list = new LinkedList<>();
        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.searchItem(itemSearch).size() > 0) {
                    list.add(hm);
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return list;
    }

    public final void toggleMegaphoneMuteState() {
        this.MegaphoneMuteState = !this.MegaphoneMuteState;
    }

    public final boolean getMegaphoneMuteState() {
        return MegaphoneMuteState;
    }

    public int getEvent() {
        return eventmap;
    }

    public final void setEvent(final int ze) {
        this.eventmap = ze;
    }

    public MapleEvent getEvent(final MapleEventType t) {
        if (!events.containsKey(t)) {
            return null;
        }
        return events.get(t);
    }

    public Map<MapleEventType, MapleEvent> getAllEvent() {
        return events;
    }

    public final Collection<PlayerNPC> getAllPlayerNPC() {
        return playerNPCs.values();
    }

    public final PlayerNPC getPlayerNPC(final int id) {
        return playerNPCs.get(id);
    }

    public final void addPlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.containsKey(npc.getId())) {
            removePlayerNPC(npc);
        }
        playerNPCs.put(npc.getId(), npc);
        getMapFactory().getMap(npc.getMapId()).addMapObject(npc);
    }

    public final void removePlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.containsKey(npc.getId())) {
            playerNPCs.remove(npc.getId());
            getMapFactory().getMap(npc.getMapId()).removeMapObject(npc);
        }
    }

    public final String getServerName() {
        return ServerConfig.SERVER_NAME;
    }

    public final void setServerName(final String sn) {
        ServerConfig.SERVER_NAME = sn;
    }

    public final int getPort() {
        return port;
    }

    public final void setPrepareShutdown() {
        this.shutdown = true;
        System.out.println("【頻道" + getChannel() + "】 準備關閉.");
    }

    public final void setFinishShutdown() {
        this.finishedShutdown = true;
        System.out.println("【頻道" + getChannel() + "】 已經關閉完成.");
    }

    public final boolean isAdminOnly() {
        return WorldConstants.ADMIN_ONLY;
    }

    public static Map<Integer, Map<Integer, Integer>> getChannelLoad() {
        Map<Integer, Map<Integer, Integer>> ret_ = new HashMap<>();
        for (World wl : LoginServer.getWorlds()) {
            Map<Integer, Integer> ret = new HashMap<>();
            for (ChannelServer cs : wl.getChannels()) {
                ret.put(cs.getChannel(), cs.getConnectedClients());
            }
            ret_.put(wl.getWorldId(), ret);
        }
        return ret_;
    }

    public int getConnectedClients() {
        return getPlayerStorage().getConnectedClients();
    }

    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = getPlayerStorage().getCheaters();

        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 20);
    }

    public void broadcastMessage(byte[] message) {
        broadcastPacket(message);
    }

    public void broadcastSmega(byte[] message) {
        broadcastSmegaPacket(message);
    }
    
    public void broadcastEtcSmega(byte[] message) {
        broadcastEtcSmegaPacket(message);
    }

    public void broadcastGMMessage(byte[] message, boolean 吸怪) {
        broadcastGMPacket(message, 吸怪);
    }

    public void broadcastGMMessage(byte[] message) {
        broadcastGMPacket(message);
    }

    public void saveAll() {
        int ppl = 0;
        List<MapleCharacter> all = this.players.getAllCharactersThreadSafe();
        for (MapleCharacter chr : all) {
            try {
                int res = chr.saveToDB(false, false);
                if (res == 1) {
                    ++ppl;
                } else {
                    System.out.println("[自動存檔] 角色:" + chr.getName() + " 儲存失敗.");
                }

            } catch (Exception e) {

            }
        }

    }

    public boolean CanGMItem() {
        return WorldConstants.GMITEMS;
    }

    public final int getMerchantMap(final int accid) {
        int map = -1;

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hired = ((HiredMerchant) itr.next());
                if (hired.getOwnerAccId() == accid) {
                    map = hired.getMap().getId();
                    break;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return map;
    }

    public final static int getChannelCount() {
        return instances.size();
    }

    public static void forceRemovePlayerByAccId(MapleClient client, int accid) {
        for (ChannelServer ch : LoginServer.getWorldStatic(client.getWorld()).getChannels()) {
            Collection<MapleCharacter> chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter c : chrs) {
                if (c.getAccountID() == accid) {
                    try {
                        if (c.getClient() != null) {
                            if (c.getClient() != client) {
                                c.getClient().unLockDisconnect();
                            }
                        }
                    } catch (Exception ex) {
                    }
                    chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
                    if (chrs.contains(c)) {
                        ch.removePlayer(c);
                    }
                }
            }
        }
        try {
            Collection<MapleCharacter> chrs = CashShopServer.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter c : chrs) {
                if (c.getAccountID() == accid) {
                    try {
                        //   FileoutputUtil.logToFile("logs/Hack/洗道具.txt", "\r\n" + FileoutputUtil.NowTime() + " MAC: " + client.getMacs() + " IP: " + client.getSessionIPAddress() + " 帳號: " + accid + " 角色: " + c.getName(), false, false);
                        if (c.getClient() != null) {
                            if (c.getClient() != client) {
                                c.getClient().unLockDisconnect();
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (Exception ex) {

        }
    }

    public static void forceRemovePlayerByCharName(MapleClient client, String Name) {
        for (ChannelServer ch : LoginServer.getWorldStatic(client.getWorld()).getChannels()) {
            Collection<MapleCharacter> chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter c : chrs) {
                if (c.getName().equalsIgnoreCase(Name)) {
                    try {
                        if (c.getClient() != null) {
                            if (c.getClient() != client) {
                                c.getClient().unLockDisconnect();
                            }
                        }
                    } catch (Exception ex) {
                    }
                    chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
                    if (chrs.contains(c)) {
                        ch.removePlayer(c);
                    }
                    c.getMap().removePlayer(c);
                }
            }
        }
    }

    public static boolean forceRemovePlayerByCharIDFromDataBase(MapleClient client, List<Integer> Cid, int accid) {
        boolean remove = false;
        for (ChannelServer ch : LoginServer.getWorldStatic(client.getWorld()).getChannels()) {
            for (final int cid : Cid) {
                CharacterTransfer CharT = ch.getPlayerStorage().getPendingCharacter(cid);
                if (CharT != null) {
                    MapleClient c = ch.getPlayerStorage().getPendingClient(cid);
                    try {
                        if (c != null) {
                            if (c != client) {
                                //System.out.println("(ChannelServer)頻道<" + ch.getChannel() + ">角色複製: " + cid + " 帳號id: " + accid);
                                //FileoutputUtil.logToFile("logs/Hack/角色複製.txt", FileoutputUtil.CurrentReadable_Time() + " 玩家id: " + cid + " 帳號id: " + accid + " 頻道: " + ch.getChannel() + " 角色複製 (ChannelServer)");
                                // client.unLockDisconnect();
                                remove = true;
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }

        for (final int cid : Cid) {
            CharacterTransfer CharT = CashShopServer.getPlayerStorage().getPendingCharacter(cid);
            if (CharT != null) {
                MapleClient c = CashShopServer.getPlayerStorage().getPendingClient(cid);
                try {
                    if (c != null) {
                        if (c != client) {
                            //System.out.println("(ChannelServer)商城角色複製: " + cid + " 帳號id: " + accid);
                            //FileoutputUtil.logToFile("logs/Hack/角色複製.txt", FileoutputUtil.CurrentReadable_Time() + " 玩家id: " + cid + " 帳號id: " + accid + " 商城角色複製 (ChannelServer)");
                            // client.unLockDisconnect();
                            remove = true;
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }
        if (remove) {
            World.Find.registerDisconnect(accid);
        }

        return World.Find.findDisconnect(accid) > 0;

    }

    public static void forceRemovePlayerByCharNameFromDataBase(MapleClient client, List<String> Name) {
        for (ChannelServer ch : LoginServer.getWorldStatic(client.getWorld()).getChannels()) {
            for (final String name : Name) {
                if (ch.getPlayerStorage().getCharacterByName(name) != null) {
                    MapleCharacter c = ch.getPlayerStorage().getCharacterByName(name);
                    try {
                        if (c.getClient() != null) {
                            if (c.getClient() != client) {
                                c.getClient().unLockDisconnect();
                            }
                        }
                    } catch (Exception ex) {
                    }
                    if (ch.getPlayerStorage().getAllCharactersThreadSafe().contains(c)) {
                        ch.removePlayer(c);
                    }
                    c.getMap().removePlayer(c);
                }
            }
        }

        for (final String name : Name) {
            if (CashShopServer.getPlayerStorage().getCharacterByName(name) != null) {
                MapleCharacter c = CashShopServer.getPlayerStorage().getCharacterByName(name);
                try {
                    if (c.getClient() != null) {
                        if (c.getClient() != client) {
                            c.getClient().unLockDisconnect();
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }

    }

    public static final Set<Integer> getChannels() {
        return new HashSet<>(instances.keySet());
    }

    public static ChannelServer newInstance(final int world, final int channel) {
        return new ChannelServer(world, channel);
    }

    public static ChannelServer getInstance(int world, int channel) {
        return LoginServer.getInstance().getChannel(world, channel);
    }

    public final void updateEvents(String event) {
        if (event != null) {// 讀取新的活動並初始化
            eventSM.addEventManager(this, event);
        }
    }

    public final void init() {
        serverStartTime = System.currentTimeMillis();
        setChannel(channel);
        eventSM = new EventScriptManager(this, ServerProperties.getProperty("server.settings.events").split(","));
        // port = (short) (((GameSetConstants.Channelport + channel) - 1));
        //port += (world * 100);
        port = (short) LoginServer.getChannelAmount();
        socket = ServerConfig.IP + ":" + port;

        //  this.ip = ServerConfig.IP + ":" + port;
        loadEvents();
        acceptor = new ServerConnection(ServerConfig.IP, port, world, channel);
        acceptor.run();
        eventSM.init();
        LoginServer.addChannelAmount();
    }

}
