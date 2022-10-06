package client.messages.commands;

import client.MapleCharacter;
import constants.ServerConfig;
import constants.WorldConstants;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.World;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.CashItemFactory;
import server.FishingRewardFactory;
import server.MapleShopFactory;
import server.ShutdownServer;
import server.Timer;
import tools.MaplePacketCreator;
import tools.StringUtil;
import client.messages.*;
import handling.cashshop.CashShopServer;
import handling.login.LoginServer;
import server.life.MapleMonsterInformationProvider;

/**
 *
 * @author Flower
 */
public class ConsoleCommand {

    public static class Info extends ConsoleCommandExecute {

        @Override
        public int execute(String[] paramArrayOfString) {

            RecvPacketOpcode.reloadValues();
            SendPacketOpcode.reloadValues();

            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            Runtime runtime = Runtime.getRuntime();

            NumberFormat format = NumberFormat.getInstance();

            StringBuilder sb = new StringBuilder();
            Long maxMemory = runtime.maxMemory();
            Long allocatedMemory = runtime.totalMemory();
            Long freeMemory = runtime.freeMemory();
            System.out.println("------------------ 系統資訊 ------------------");
            System.out.println("線程數 :" + ((Integer) threadSet.size()).toString());
            System.out.println("SQL連接數 :" + ((Integer) DatabaseConnection.getConnectionsCount()).toString());
            System.out.println("記憶體最大限制 :" + maxMemory.toString());
            System.out.println("已申請記憶體 :" + allocatedMemory.toString());
            System.out.println("尚未使用記憶體 :" + freeMemory.toString());
            return 1;
        }

    }

    public static class Shutdown extends ConsoleCommandExecute {

        private static Thread t = null;

        @Override
        public int execute(String[] splitted) {
            System.out.println("執行關閉作業");
            System.out.println("伺服器關閉中...");
            if (t == null || t.isAlive()) {
                try {
                    t = new Thread(server.ShutdownServer.getInstance());
                    t.start();
                } catch (Exception ex) {
                    System.out.println("[關閉伺服器錯誤]");
                }
            } else {
                System.out.println("已在執行中...");
            }
            return 1;
        }
    }

    public static class ShutdownTime extends ConsoleCommandExecute {

        private int minutesLeft = 0;
        private static Thread t = null;
        private static ScheduledFuture<?> ts = null;

        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                minutesLeft = Integer.parseInt(splitted[1]);
                World.isShutDown = true;
                System.out.println("已開啟限制玩家。");
                if (ts == null && (t == null || !t.isAlive())) {
                    t = new Thread(server.ShutdownServer.getInstance());
                    ts = Timer.EventTimer.getInstance().register(new Runnable() {
                        @Override
                        public void run() {
                            if (minutesLeft == 0) {
                                ShutdownServer.getInstance().run();
                                t.start();
                                ts.cancel(false);
                                return;
                            }
                            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[楓之谷公告] 伺服器將在 " + minutesLeft + "分鐘後關閉，請做好安全措施後並且盡快登出。"));
                            System.out.println("本伺服器將在 " + minutesLeft + "分鐘後關閉.");
                            minutesLeft--;
                        }
                    }, 60000);
                } else {
                    System.out.println("好吧真拿你沒辦法..伺服器關閉時間修改...請等待關閉完畢..請勿強制關閉服務器..否則後果自負!");
                }
            } else {
                System.out.println("使用規則: shutdowntime <關閉時間>");
                return 0;
            }
            return 1;
        }
    }

    public static class ExpRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted[2].equalsIgnoreCase("all")) {
                    for (World wl : LoginServer.getWorlds()) {
                        for (ChannelServer cserv : wl.getChannels()) {
                            cserv.setExpRate(rate);
                        }
                    }
                } else {
                    int world = Integer.parseInt(splitted[2]);
                    int channel = Integer.parseInt(splitted[3]);
                    ChannelServer.getInstance(world, channel).setExpRate(rate);
                }
                System.out.println("經驗備率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: exprate <number> [<world> <channel>/all]");
            }
            return 1;
        }
    }

    public static class DropRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (World wl : LoginServer.getWorlds()) {
                        for (ChannelServer cserv : wl.getChannels()) {
                            cserv.setDropRate(rate);
                        }
                    }
                } else {
                    int world = Integer.parseInt(splitted[2]);
                    int channel = Integer.parseInt(splitted[3]);
                    ChannelServer.getInstance(world, channel).setDropRate(rate);
                }
                System.out.println("掉落備率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: droprate <number> [<world> <channel>/all]");
            }
            return 1;
        }
    }

    public static class MesoRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted[2].equalsIgnoreCase("all")) {
                    for (World wl : LoginServer.getWorlds()) {
                        for (ChannelServer cserv : wl.getChannels()) {
                            cserv.setMesoRate(rate);
                        }
                    }
                } else {
                    int world = Integer.parseInt(splitted[2]);
                    int channel = Integer.parseInt(splitted[3]);
                    ChannelServer.getInstance(world, channel).setMesoRate(rate);
                }
                System.out.println("金錢備率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: mesorate <number> [<world> <channel>/all]");
            }
            return 1;
        }
    }

    public static class Saveall extends ConsoleCommandExecute {

        private int p = 0;

        @Override
        public int execute(String[] splitted) {
            for (World wlerv : LoginServer.getWorlds()) {
                for (ChannelServer cserv : wlerv.getChannels()) {
                    List<MapleCharacter> chrs = cserv.getPlayerStorage().getAllCharactersThreadSafe();
                    for (MapleCharacter chr : chrs) {
                        p++;
                        chr.saveToDB(false, true);

                    }
                }
            }
            System.out.println("[保存] " + p + "個玩家數據保存到數據中.");
            return 1;
        }
    }

    public static class AutoReg extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            ServerConfig.AUTO_REGISTER = !ServerConfig.AUTO_REGISTER;
            System.out.println("自動註冊狀態: " + (ServerConfig.AUTO_REGISTER ? "開啟" : "關閉"));
            return 1;
        }
    }

    public static class cashshop extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            WorldConstants.CS_ENABLE = !WorldConstants.CS_ENABLE;
            System.out.println("商城狀態: " + (WorldConstants.CS_ENABLE ? "開啟" : "關閉"));
            return 1;
        }
    }

    public static class logindoor extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            WorldConstants.ADMIN_ONLY = !WorldConstants.ADMIN_ONLY;
            System.out.println("管理員登入模式狀態: " + (WorldConstants.ADMIN_ONLY ? "開啟" : "關閉"));
            return 1;
        }
    }

    public static class serverMsg extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                for (World wlerv : LoginServer.getWorlds()) {
                    for (ChannelServer ch : wlerv.getChannels()) {
                        ch.setServerMessage(sb.toString());
                    }
                }
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage(sb.toString()));
            } else {
                System.out.println("指令規則: !serverMsg <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class ReloadMap extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            try {
                final int mapId = Integer.parseInt(splitted[1]);

                for (World wl : LoginServer.getWorlds()) {
                    for (ChannelServer cserv : wl.getChannels()) {
                        if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                            System.out.println("該地圖還有人唷");
                            return 0;
                        }
                    }
                }
                for (World wl : LoginServer.getWorlds()) {
                    for (ChannelServer cserv : wl.getChannels()) {
                        if (cserv.getMapFactory().isMapLoaded(mapId)) {
                            cserv.getMapFactory().removeMap(mapId);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[指令用法] reloadMap <地圖ID>");
            }
            return 1;
        }
    }

    public static class help extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            System.out.println("╭〝☆指令列表〞★╮");
            System.out.println("-------------------------");
            System.out.println("exprate  經驗倍率");
            System.out.println("droprate 掉寶倍率");
            System.out.println("mesorate 金錢倍率");
            System.out.println("-------------------------");
            System.out.println("shutdown 關閉伺服器");
            System.out.println("shotdowntime <時間> 倒數關閉服務器");
            System.out.println("reloadchannel 重新載入頻道");
            System.out.println("reloadmap 重新載入地圖");
            System.out.println("Info 查看伺服器狀況");
            System.out.println("AutoReg 自動註冊開關");
            System.out.println("logindoor 管理員登入模式開關");
            System.out.println("cashshop 商城開關");
            System.out.println("-------------------------");
            System.out.println("ReloadEvents 重新載入副本事件");
            System.out.println("ReloadFishing 重新載入釣魚獎勵");
            System.out.println("ReloadCS 重新載入購物商城物品");
            System.out.println("ReloadShops 重新載入商店設定");
            System.out.println("ReloadPortals 重新載入門口腳本");
            System.out.println("ReloadDrops 重新載入掉落物品");
            System.out.println("ReloadOps 重新載入封包包頭");
            System.out.println("-------------------------");
            System.out.println("online 線上玩家");
            System.out.println("say 伺服器說話");
            System.out.println("saveall 全服存檔");
            System.out.println("-------------------------");
            System.out.println("╰〝★指令列表〞╯");
            return 1;
        }
    }

    public static class Online extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            int total = 0;
            for (World wlerv : LoginServer.getWorlds()) {
                for (ChannelServer ch : wlerv.getChannels()) {
                    System.out.println("----------------------------------------------------------");
                    System.out.println(new StringBuilder().append("頻道: ").append(ch.getChannel()).append(" 線上人數: ").append(ch.getConnectedClients()).toString());
                    total += ch.getConnectedClients();
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {

                        if (chr != null) {
                            StringBuilder ret = new StringBuilder();
                            ret.append(" 角色暱稱 ");
                            ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                            ret.append(" ID: ");
                            ret.append(chr.getId());
                            ret.append(" 等級: ");
                            ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                            ret.append(" 職業: ");
                            ret.append(chr.getJob());
                            if (chr.getMap() != null) {
                                ret.append(" 地圖: ");
                                ret.append(chr.getMapId()).append(" - ").append(chr.getMap().getMapName());
                                System.out.println(ret.toString());
                            }
                        }
                    }
                }
                /*伺服器總人數*/
                System.out.println(new StringBuilder().append("當前世界總計線上人數: ").append(total).append("個").toString());
                System.out.println("-------------------------------------------------------------------------------------");
            }

            int onlineInCS = 0;
            if (CashShopServer.getPlayerStorage() != null) {
                onlineInCS += CashShopServer.getPlayerStorage().getConnectedClients();
            }
            System.out.println(new StringBuilder().append("目前商城人數: ").append(onlineInCS));
            int allonline = onlineInCS;

            for (World wlerv : LoginServer.getWorlds()) {
                for (ChannelServer cserv : wlerv.getChannels()) {
                    allonline += cserv.getPlayerStorage().getAllCharactersThreadSafe().size();
                }
            }

            System.out.println(new StringBuilder().append("目前總世界線上人數: ").append(allonline));

            System.out.println("-------------------------------------------------------------------------------------");

            return 1;
        }
    }

    public static class Say extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[伺服器公告] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice(sb.toString()));
            } else {
                System.out.println("指令規則: say <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class ReloadOps extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            return 1;
        }

    }

    public static class ReloadDrops extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }

    }

    public static class ReloadPortals extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class ReloadShops extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }

    }

    public static class ReloadCS extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            CashItemFactory.getInstance().clearItems();
            return 1;
        }

    }

    public static class ReloadFishing extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            FishingRewardFactory.getInstance().reloadItems();
            return 1;
        }

    }

    public static class ReloadEvents extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cserv : wl.getChannels()) {
                    cserv.reloadEvents();
                }
            }
            return 1;
        }

    }

}
