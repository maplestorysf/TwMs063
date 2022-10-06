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
package handling.login;

import client.MapleClient;
import constants.ServerConfig;
import constants.WorldConstants;
import handling.MapleServerHandler;
import handling.channel.ChannelServer;
import java.util.HashMap;
import java.util.Map;

import handling.netty.ServerConnection;
import handling.world.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import server.ServerProperties;
import tools.Pair;
import tools.Quadra;
import tools.StringUtil;
import tools.packet.LoginPacket.Server;

public class LoginServer {

    private static final Map<Integer, Long> SelectCharTime = new WeakHashMap<>();
    private static final Map<Integer, String> LoginKey = new HashMap<>();
    private static final Map<Integer, Long> ChangeChannelTime = new HashMap<>();
    private static final Map<Integer, Long> EnterGameTime = new HashMap<>();
    private static final List<World> worlds = new ArrayList<>();
    private static final List<Map<Integer, String>> channels = new LinkedList<>();
    private static final HashMap<Integer, Quadra<String, String, Integer, String>> loginAuth = new HashMap<>();

    private static Map<Integer, Map<Integer, Integer>> load = new HashMap<>();
    private static LoginServer instance = null;
    private static int usersOn = 0;
    public static int port = 8484, totalChannelAmount = ServerConfig.ChannelPort;
    private static boolean finishedShutdown = true;
    private static AccountStorage clients;
    private static ServerConnection acceptor;

    public static int getChannelAmount() {
        return totalChannelAmount;
    }

    public static void addChannelAmount() {
        totalChannelAmount++;
    }

    public static final void addChannel(final int world, final int channel) {
        load.put(world, new HashMap(channel, 1));
    }

    public static final void removeChannel(final int worldid, int channel) {
        if (load.containsKey(worldid)) {
            load.get(worldid).remove(channel);
        }
        channels.remove(channel);

        World world = worlds.get(worldid);
        if (world != null) {
            world.removeChannel(channel);
        }
    }

    public static final void setup() {
        int firstport, lastport;

        int[] flagg = new int[18];
        int[] expp = new int[18];
        int[] mesoo = new int[18];
        int[] dropp = new int[18];
        int[] chh = new int[18];
        boolean[] createe = new boolean[18];
        int userLimit = WorldConstants.UserLimit;
        String[] eventMessagee = new String[18];
        for (Pair<Integer, Integer> flags : WorldConstants.flags) {
            flagg[flags.left] = flags.right;
        }
        for (Pair<Integer, Integer> loadexp : WorldConstants.expRates) {
            expp[loadexp.left] = loadexp.right;
        }
        for (Pair<Integer, Integer> loadmeso : WorldConstants.mesoRates) {
            mesoo[loadmeso.left] = loadmeso.right;
        }
        for (Pair<Integer, Integer> loaddrop : WorldConstants.dropRates) {
            dropp[loaddrop.left] = loaddrop.right;
        }
        for (Pair<Integer, String> eventmsg : WorldConstants.eventMessages) {
            eventMessagee[eventmsg.left] = eventmsg.right;
        }
        for (Pair<Integer, Integer> loadch : WorldConstants.chAmounts) {
            chh[loadch.left] = loadch.right;
        }
        for (Pair<Integer, Boolean> loadcreate : WorldConstants.canCreates) {
            createe[loadcreate.left] = loadcreate.right;
        }
        for (int i = 0; i < WorldConstants.Worlds; i++) {
            firstport = LoginServer.getChannelAmount();
            World world = new World(i, flagg[i], eventMessagee[i], expp[i], mesoo[i], dropp[i], chh[i], createe[i]);
            worlds.add(world);
            channels.add(new LinkedHashMap<Integer, String>());
            for (int z = 0; z < world.getChAmount(); z++) {
                int channelid = z + 1;
                ChannelServer channel = ChannelServer.newInstance(i, channelid);
                world.addChannel(channel);
                //world.setUserLimit(userLimit);
                channel.init(); // initialize
                channels.get(i).put(channelid, channel.getIP());
            }
            lastport = LoginServer.getChannelAmount();
            String Wn = Server.getById(world.getWorldId()).toString();
            int cs = LoginServer.getInstance().getWorld(world.getWorldId()).getChannels().size();
            int er = LoginServer.getInstance().getWorld(world.getWorldId()).getExpRate();
            int mr = LoginServer.getInstance().getWorld(world.getWorldId()).getMesoRate();
            int dr = LoginServer.getInstance().getWorld(world.getWorldId()).getDropRate();

            StringBuilder sb = new StringBuilder();
            sb.append("世界 : ");
            sb.append(StringUtil.getRightPaddedStr(Wn, ' ', 8));
            sb.append(" 頻道數 :");
            sb.append(cs);
            sb.append(" 經驗 :");
            sb.append(er);
            sb.append(" 金錢 :");
            sb.append(mr);
            sb.append(" 掉落 :");
            sb.append(dr);
            sb.append(" 端口 :");
            sb.append(firstport);
            sb.append(" ~ ");
            sb.append(lastport - 1);
            System.out.println(sb.toString());
        }

        port = Integer.parseInt(ServerProperties.getProperty("server.settings.login.port", "8484"));
        acceptor = new ServerConnection(ServerConfig.IP, port, 0, MapleServerHandler.LOGIN_SERVER);
        acceptor.run();
        System.out.println("\n【登入伺服器】  - 監聽端口: " + port + " \n");
    }

    public static final void shutdown() {
        if (finishedShutdown) {
            System.out.println("【登入伺服器】 已經關閉了...無法執行此動作");
            return;
        }
        System.out.println("【登入伺服器】 關閉中...");
        acceptor.close();
        System.out.println("【登入伺服器】 關閉完畢...");
        finishedShutdown = true; //nothing. lol
    }

    public static final String getServerName() {
        return ServerConfig.SERVER_NAME;
    }

    public static final Map<Integer, Map<Integer, Integer>> getLoad() {
        return load;
    }

    public static void setLoad(final Map<Integer, Map<Integer, Integer>> load_, final int usersOn_) {
        load = load_;
        usersOn = usersOn_;
    }

    public static final int getUsersOn() {
        return usersOn;
    }

    public static final boolean isShutdown() {
        return finishedShutdown;
    }

    public static final void setOn() {
        finishedShutdown = false;
    }

    public static boolean getAutoReg() {
        return ServerConfig.AUTO_REGISTER;
    }

    public static void setAutoReg(boolean x) {
        ServerConfig.AUTO_REGISTER = x;
    }

    public static boolean containClient(MapleClient client) {
        Collection<MapleClient> cls = getClientStorage().getAllClientsThreadSafe();
        for (MapleClient c : cls) {
            if (c == null) {
                continue;
            }
            if (client == c) {
                return true;
            }
        }
        return false;
    }

    public static void forceRemoveClient(MapleClient client, boolean remove) {
        Collection<MapleClient> cls = getClientStorage().getAllClientsThreadSafe();
        for (MapleClient c : cls) {
            if (c == null) {
                continue;
            }
            if (c.getAccID() == client.getAccID() || c == client) {
                if (c != client) {
                    c.unLockDisconnect();
                }
                if (remove) {
                    removeClient(c);
                }
            }
        }
    }

    public static void forceRemoveClient(MapleClient client) {
        forceRemoveClient(client, true);
    }

    public static AccountStorage getClientStorage() {
        if (clients == null) {
            clients = new AccountStorage();
        }
        return clients;
    }

    public static final void addClient(final MapleClient c) {
        getClientStorage().registerAccount(c);
    }

    public static final void removeClient(final MapleClient c) {
        getClientStorage().deregisterAccount(c);
    }

    public static boolean CanLoginKey(MapleClient c, String key) {
        if (LoginKey.get(c.getAccID()) == null) {
            return true;
        }
        if (LoginKey.containsValue(key)) {
            if (LoginKey.get(c.getAccID()).equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean removeLoginKey(MapleClient c) {
        LoginKey.remove(c.getAccID());
        return true;
    }

    public static boolean addLoginKey(MapleClient c, String key) {
        LoginKey.put(c.getAccID(), key);
        return true;
    }

    public static String getLoginKey(MapleClient c) {
        return LoginKey.get(c.getAccID());
    }

    public static boolean CheckSelectChar(int accid) {
        long lastTime = System.currentTimeMillis();
        if (SelectCharTime.containsKey(accid)) {
            long lastSelectCharTime = SelectCharTime.get(accid);
            if (lastSelectCharTime + 3000 > lastTime) {
                return false;
            }
            SelectCharTime.remove(accid);
        } else {
            SelectCharTime.put(accid, lastTime);
        }
        return true;
    }

    public static long getLoginAgainTime(int accid) {
        return ChangeChannelTime.get(accid);
    }

    public static void addLoginAgainTime(int accid) {
        ChangeChannelTime.put(accid, System.currentTimeMillis());
    }

    public static boolean canLoginAgain(int accid) {
        long lastTime = System.currentTimeMillis();
        if (ChangeChannelTime.containsKey(accid)) {
            long lastSelectCharTime = ChangeChannelTime.get(accid);
            if (lastSelectCharTime + 40 * 1000 > lastTime) {
                return false;
            }
        }
        return true;
    }

    public static long getEnterGameAgainTime(int accid) {
        return EnterGameTime.get(accid);
    }

    public static void addEnterGameAgainTime(int accid) {
        EnterGameTime.put(accid, System.currentTimeMillis());
    }

    public static boolean canEnterGameAgain(int accid) {
        long lastTime = System.currentTimeMillis();
        if (EnterGameTime.containsKey(accid)) {
            long lastSelectCharTime = EnterGameTime.get(accid);
            if (lastSelectCharTime + 60 * 1000 > lastTime) {
                return false;
            }
        }
        return true;
    }

    public static LoginServer getInstance() {
        if (instance == null) {
            instance = new LoginServer();
        }
        return instance;
    }

    public ChannelServer getChannel(int world, int channel) {
        return worlds.get(world).getChannel(channel);
    }

    public World getWorld(int id) {
        return worlds.get(id);
    }

    public static World getWorldStatic(int id) {
        return worlds.get(id);
    }

    public static List<World> getWorlds() {
        return worlds;
    }

    public static void putLoginAuth(int chrid, String ip, String tempIp, int channel, String mac) {
        loginAuth.put(chrid, new Quadra<>(ip, tempIp, channel, mac));
    }

    public static Quadra<String, String, Integer, String> getLoginAuth(int chrid) {
        return loginAuth.remove(chrid);
    }
}
