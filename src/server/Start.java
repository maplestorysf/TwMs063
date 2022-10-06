package server;

import client.inventory.MapleEquipIdOnly;
import client.SkillFactory;
import constants.PiPiConfig;
import constants.ServerConfig;
import constants.WorldConstants;
import handling.channel.MapleGuildRanking;
import handling.login.LoginServer;
import handling.cashshop.CashShopServer;
import handling.login.LoginInformationProvider;
import handling.world.World;
import java.sql.SQLException;
import database.DatabaseConnection;
import handling.world.SkillCollector;
import handling.world.family.MapleFamilyBuff;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import client.messages.*;
import constants.ServerConstants;

public class Start {

    private static Thread Copyserver = null;

    public final static void main(final String args[]) {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
//        if (!PiPiConfig.Start_Check) {
//            String input = null;
//            try {
//                input = (new BufferedReader(new InputStreamReader(System.in)).readLine());
//            } catch (Exception ex) {
//
//            }
//            if (!PiPiConfig.doCheck(input)) {
//                System.exit(0);
//                return;
//            }
//        }
        System.setProperty("file.encoding", "utf-8");
        System.setProperty("path", "");
        System.out.println("【台版楓之谷模擬器】");
        System.out.println("【版本】 v" + ServerConstants.MAPLE_VERSION);
        resetAllLoginState();

        if (WorldConstants.ADMIN_ONLY) {
            System.out.println("【管理員模式】開啟");
        } else {
            System.out.println("【管理員模式】關閉");
        }

        if (ServerConfig.AUTO_REGISTER) {
            System.out.println("【自動註冊】開啟");
        } else {
            System.out.println("【自動註冊】關閉");
        }

        if (!WorldConstants.GMITEMS) {
            System.out.println("【允許玩家使用管理員物品】開啟");
        } else {
            System.out.println("【允許玩家使用管理員物品】關閉");
        }

        /* 載入設定 */
        ServerConfig.loadSetting();
        WorldConstants.init();
        World.init();
        /* 載入計時器 */
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        MobTimer.getInstance().start();
        CloneTimer.getInstance().start();
        autoEventTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        /* 讀取WZ內禁止使用的名稱 */
        LoginInformationProvider.getInstance();
        /* 讀取釣魚 */
        FishingRewardFactory.getInstance();
        /* 載入任務*/
        MapleQuest.initQuests(false);
        MapleLifeFactory.loadQuestCounts(false);
        MapleOxQuizFactory.getInstance().initialize();
        /* 載入物品資訊 */
        MapleItemInformationProvider.getInstance().loadEtc(false);
        MapleItemInformationProvider.getInstance().loadItems(false);
        PredictCardFactory.getInstance().initialize();
        CashItemFactory.getInstance().initialize(false);
        /* if (ServerConfig.EnableTwoCSServer) {
            CashItemFactory1.getInstance().initialize(false);
        }*/
 /* 載入隨機獎勵 */
        RandomRewards.getInstance();
        /* 載入技能資訊 */
        SkillFactory.LoadSkillInformaion(false);
        SkillCollector.getInstance().init();
        /* 載入怪物技能 */
        MapleCarnivalFactory.getInstance();
        /* 載入排行 */
        MapleGuildRanking.getInstance().getGuildRank();
        MapleGuildRanking.getInstance().getJobRank(1);
        MapleGuildRanking.getInstance().getJobRank(2);
        MapleGuildRanking.getInstance().getJobRank(3);
        MapleGuildRanking.getInstance().getJobRank(4);
        MapleGuildRanking.getInstance().getJobRank(5);
        MapleGuildRanking.getInstance().getJobRank(6);
        /* 載入家族Buff */
        MapleFamilyBuff.getBuffEntry();
        /* 載入登入伺服器 */
        LoginServer.setup();
        /* 載入頻道伺服器 */
//        ChannelServer.startAllChannels();
        /* 載入拍賣伺服器 */
        //MTSStorage.load();
        /* 載入商城伺服器 */
        CashShopServer.setup();
        /* 載入自動封鎖系統 */
        CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
        /* 載入關閉伺服器線程 */
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownServer.getInstance()));
        /* 載入速度排行 */
        SpeedRunner.getInstance().loadSpeedRuns();
        /* 處理怪物重生、CD、寵物、坐騎 */
        World.registerRespawn();
        /* 設定finishedShutdown為false */
        LoginServer.setOn();
        /* 載入自訂義NPC、怪物*/
        MapleMapFactory.loadCustomLife();
        /* 載入自訂義功能 */
        World.GainNX(60);// 每六十分鐘自動給點數
        //World.AutoSave(5);// 每五分鐘自動存檔
        //   World.ClearMemory(5 * 60);// 每小時清理記憶體
        //   WorldTimer.getInstance().register(CloseSQLConnections, 60 * 60 * 1000);// 定時清理MySql連接數
        if (WorldConstants.isShutDown) {
            System.out.println("【禁止玩家使用:啟動 如果要開放請GM上線打:!禁止玩家使用】");
        }
        if (WorldConstants.Disable_Shop) {
            System.out.println("【禁止使用個人商店、精靈商人:啟動 如果要開放請GM上線打:!禁止使用商店】");
        }
        System.out.println("【伺服器開啟完畢】");
        if (args != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (!ShutdownServer.end) {
                try {
                    System.out.print(">>");
                    String cmd = br.readLine();
                    if (cmd.equals("")) {
                        continue;
                    }
                    ConsoleCommandProcessor.processCommand(cmd);
                } catch (IOException ex) {

                } catch (Exception ex) {

                }
            }
        }

        /* 唯一道具 */
        Copyserver = new Thread() {
            @Override
            public void run() {
                Timer.WorldTimer.getInstance().register(new MapleEquipIdOnly.run(), 12 * 60 * 60 * 1000);
            }
        };
        Copyserver.start();

        //MapleEquipIdOnly.getInstance();
    }

    private static void resetAllLoginState() {
        String name = null;
        int id = 0, vip = 0, size = 0;

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET pets = '-1,-1,-1'")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }

        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM characters WHERE gm = 100"); ResultSet rs = ps.executeQuery()) {
            rs.beforeFirst();
            while (rs.next()) {
                size = rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }
        if (size > 1) {
            System.out.println("警告：資料表內ＧＭ權限異常 ");
        }

        try {
            try (PreparedStatement ps = con.prepareStatement("select id, name, vip FROM accounts where vip > 10"); ResultSet rs = ps.executeQuery()) {
                rs.beforeFirst();
                while (rs.next()) {
                    name = rs.getString("name");
                    vip = rs.getInt("vip");
                    id = rs.getInt("id");
                    System.err.println("VIP權限異常: 帳號[" + name + "], 編號[" + id + "], VIP[" + vip + "]");
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }

    }
}
