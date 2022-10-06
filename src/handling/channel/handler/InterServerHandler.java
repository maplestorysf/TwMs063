package handling.channel.handler;

import java.util.List;

import client.BuddyEntry;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleStat;
import client.SkillFactory;
import constants.MapConstants;
import constants.ServerConfig;
import constants.WorldConstants;
import constants.skills.SkillType;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.CharacterIdChannelPair;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.guild.MapleGuild;
import java.sql.Timestamp;
import java.util.Collection;
import server.Randomizer;
import server.maps.FieldLimitType;
import tools.FilePrinter;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Quadra;
import tools.data.LittleEndianAccessor;
import tools.packet.FamilyPacket;
import tools.packet.MTSCSPacket;

public class InterServerHandler {

    public static final void EnterCashShop(final MapleClient c, final MapleCharacter chr, final boolean mts) {
        if (c.getCloseSession()) {
            return;
        }
        int num = Randomizer.rand(1, 5);
        if (World.isShutDown && !chr.isGM()) {
            c.sendPacket(MaplePacketCreator.serverBlocked(2));
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (!WorldConstants.CS_ENABLE && !chr.isGM() || mts) {
            c.sendPacket(MaplePacketCreator.serverBlocked(2));
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getTrade() != null || !chr.isAlive() || chr.getEventInstance() != null || c.getChannelServer() == null) {
            c.sendPacket(MaplePacketCreator.serverBlocked(2));
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        if (c.getPlayer().inMapleLand() || c.getPlayer().getMapId() == 220080001 || mts) {
            c.getSession().writeAndFlush(tools.MaplePacketCreator.enableActions());
            c.getPlayer().dropMessage(5, "目前地圖無法使用購物商城。");
            return;
        } else if (c.getLoginState() != MapleClient.LOGIN_LOGGEDIN) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        byte[] packet = MTSCSPacket.warpCS(c);
//        if (packet.length >= 65535) {
//            c.getPlayer().dropMessage(5, "請將背包的東西減少後再進入商城");
//            c.sendPacket(MaplePacketCreator.enableActions());
//            return;
//        }
        try {
            int res = chr.saveToDB(false, false);
            if (res == 1) {
                chr.dropMessage(5, "角色保存成功！");
            }
        } catch (Exception ex) {
        }
        final ChannelServer ch = ChannelServer.getInstance(c.getWorld(), c.getChannel());
        if (chr != null && ch
                != null) {
            chr.changeRemoval();
            if (chr.getMessenger() != null) {
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
            }
            PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
            PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
            PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
            World.channelChangeData(new CharacterTransfer(chr), c, chr.getId(), chr.getWorld(), mts ? -20 : -10);
            ch.removePlayer(chr);
            c.updateLoginState(MapleClient.CASH_SHOP_TRANSITION, c.getSessionIPAddress());

            chr.getMap().removePlayer(chr);
            c.sendPacket(MaplePacketCreator.getChannelChange(CashShopServer.getIP().split(":")[0], Integer.parseInt(CashShopServer.getIP().split(":")[1])));
            c.getPlayer().expirationTask(true, false);
            c.setPlayer(null);
            c.setReceiving(false);
        }
    }

    /**
     * 註解by宗達 2016.01.24.
     *
     * @param playerid - 玩家ID
     * @param c - 客戶端 Client
     */
    public static final void LoggedIn(final int playerid, final MapleClient c) {
        if (c.getCloseSession()) {
            return;
        }
        c.loadAccountidByPlayerid(playerid);
        if (World.Find.findDisconnect(c.getAccID()) > 0) {
            System.out.println("(Loggedin) 頻道<" + c.getChannel() + "> 角色複製: " + playerid + " 帳號id: " + c.getAccID());
            FileoutputUtil.logToFile("logs/Hack/角色複製.txt", FileoutputUtil.CurrentReadable_Time() + " 玩家id: " + playerid + " 頻道: " + c.getChannel() + " 帳號id: " + c.getAccID() + " 角色複製 (Loggedin)");
            World.Find.forceDeregisterDisconnect(c.getAccID());
            c.getSession().close();
            return;
        } else if (!MapleCharacterUtil.isExistCharacterInDataBase(playerid)) {
            c.getSession().close();
            return;
        }
        final ChannelServer channelServer = c.getChannelServer();
        MapleCharacter player;

        CharacterTransfer transfer = null;//channelServer.getPlayerStorage().getPendingCharacter(playerid)

        outterLoop:
        for (World wl : LoginServer.getWorlds()) {
            for (ChannelServer cserv : wl.getChannels()) {
                transfer = cserv.getPlayerStorage().getPendingCharacter(playerid);
                if (transfer != null) {
                    break outterLoop;
                }
            }
        }
        final int state = c.getLoginState();

        if (state != MapleClient.LOGIN_SERVER_TRANSITION && transfer == null) {
            c.getSession().close();
            return;
        }
        if (state == MapleClient.LOGIN_SERVER_TRANSITION && transfer != null) {
            c.getSession().close();
            return;
        }
        if (state != MapleClient.LOGIN_SERVER_TRANSITION && state != MapleClient.CHANGE_CHANNEL && state != MapleClient.CASH_SHOP_TRANSITION_LEAVE && state != MapleClient.MAPLE_TRADE_TRANSITION_LEAVE) {
            c.getSession().close();
            return;
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        if (c.getLastLogin() + 5 * 1000 < currentTime.getTime()) {
            c.setReceiving(false);
            c.getSession().close();
            return;
        }
        if (transfer == null) { // Player isn't in storage, probably isn't CC
            //    System.out.println(c.getAccountName() + " " + System.getProperty(c.getAccountName()));
            if (System.getProperty(String.valueOf(playerid)) == null || !System.getProperty(String.valueOf(playerid)).equals("1")) {
                c.getSession().close();
                return;
            } else if (System.getProperty(c.getAccountName().toLowerCase()) == null || !System.getProperty(c.getAccountName().toLowerCase()).equals("1")) {
                c.getSession().close();
                return;
            } else {
                System.setProperty(String.valueOf(playerid), String.valueOf(0));
                System.setProperty(String.valueOf(c.getAccountName().toLowerCase()), String.valueOf(0));
            }
            Quadra<String, String, Integer, String> ip = LoginServer.getLoginAuth(playerid);
            String s = c.getSessionIPAddress();
            if (ip == null || (!s.substring(s.indexOf('/') + 1, s.length()).equals(ip.first)/* && !c.getMac().equals(macData)*/)) {
                if (ip != null) {
                    LoginServer.putLoginAuth(playerid, ip.first, ip.second, ip.third, ip.forth);
                } else {
                    c.getSession().close();
                    return;
                }
            }
            c.setTempIP(ip.second);
            c.setChannel(ip.third);
        }

        if (transfer == null) {
            LoginServer.removeClient(c);
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }

        // 儲存中的帳號不載入
        if (World.isPlayerSaving(c.getAccID())) {
            FileoutputUtil.logToFile("logs/data/儲存中載入.txt", FileoutputUtil.CurrentReadable_Time() + " 角色: " + player.getName() + "(" + player.getId() + ") 帳號: " + c.getAccountName() + "(" + player.getAccountID() + ") \r\n ");
            c.getSession().close();
            return;
        }

        //對在線上角色做斷線
        LoginServer.forceRemoveClient(c, false);
        ChannelServer.forceRemovePlayerByAccId(c, c.getAccID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            if (ServerConfig.LOG_DC) {
                FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n帳號[" + c.getAccountName() + "]伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            return;
        }

        //設置用戶端角色
        c.setPlayer(player);
        //設置用戶端賬號ID
        c.setAccID(player.getAccountID());
        c.setWorld((player.getWorld()));
        c.loadAccountData(player.getAccountID());

        //更新登入狀態
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        channelServer.addPlayer(player);
        c.sendPacket(MaplePacketCreator.getCharInfo(player));

        //暫存能力值解除
//        c.sendPacket(MaplePacketCreator.temporaryStats_Reset());
        try {
            short nhp = player.getStat().hp;
            short nmp = player.getStat().mp;
            // BUFF技能
            player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
            player.getStat().hp = nhp;
            player.getStat().mp = nmp;
            player.updateSingleStat(MapleStat.HP, nhp);
            player.updateSingleStat(MapleStat.MP, nmp);
            // 冷卻時間
            player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()), true);
            // 疾病狀態
            player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));

            // 伺服器管理員上線預設無敵
//            if (player.isAdmin() && !player.isInvincible()) {
//                player.dropMessage(6, "無敵已經開啟.");
//                player.setInvincible(true);
//            }
            // 管理員上線預設隱藏
            if (player.isGM()) {
                SkillFactory.getSkill(SkillType.GM.終極隱藏).getEffect(1).applyTo(player);
            }

            // 開啟好友列表
            final Collection<Integer> buddyIds = player.getBuddylist().getBuddiesIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds, player.getGMLevel(), player.isHidden());
            if (player.getParty() != null) {
                //channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
                World.Party.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
            }
            /* 讀取好友 */
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final BuddyEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.sendPacket(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

            // Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getWorld(), c.getChannel());
            }

            // 開始公會及聯盟
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.sendPacket(MaplePacketCreator.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.sendPacket(pack);
                            }
                        }
                    }
                } else {
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            } else {
                c.sendPacket(MaplePacketCreator.勳章(player));
            }
            // 家族
//            if (player.getFamilyId() > 0) {
//                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
//            }
//            c.sendPacket(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FilePrinter.printError(FilePrinter.LoginError, e);
        }

        // 把角色添加進地圖
        player.getMap().addPlayer(player);

//        c.sendPacket(FamilyPacket.getFamilyData());
        // 技能組合
        player.sendMacros();
        // 顯示訊息
        player.showNote();
        // 更新組隊成員HP
        player.updatePartyMemberHP();
        // 精靈墜飾計時
        player.startFairySchedule(false, true);
        // 修復消失技能
        player.baseSkills();
        // 鍵盤設置
        c.sendPacket(MaplePacketCreator.getKeymap(player.getKeyLayout()));

        // 任務狀態
        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.sendPacket(MaplePacketCreator.updateQuestMobKills(status));
            }
        }

        // 好友
        final BuddyEntry pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddyEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getCharacterId(), pendingBuddyRequest.getGroup(), -1, -1, false));
            c.sendPacket(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getCharacterId(), pendingBuddyRequest.getName()));
        }

        // 黑騎士技能 
        if (player.canBerserk()) {
            player.doBerserk();
        }
        // 複製人
        player.spawnClones();
        // 寵物
        player.spawnSavedPets();
        // 重新計算Stat
        //player.getStat().recalcLocalStats();

        if (transfer != null) {
            if (player.getJob() == 0 || player.getJob() == 1000 || player.getJob() == 2000) {
                try {
                    for (ChannelServer cserv : LoginServer.getWorldStatic(player.getWorld()).getChannels()) {
                        for (MapleCharacter chr_ : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                            if (chr_.getShow(2)) {
                                chr_.dropMessage("[GM密語] 帳號:" + player.getClient().getAccountName() + "(" + player.getAccountID() + ") 角色:" + player.getName() + "(" + player.getId() + ") 職業: " + player.getJob() + " 地圖: " + player.getMap().getMapName() + "(" + player.getMapId() + ") 進入頻道 " + player.getClient().getChannel());
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }
        } else {
            //     LoginServer.removeLoginKey(c);
        }

    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final boolean inCS = CashShopServer.getPlayerStorage().getCharacterById(chr.getId()) != null;
        final boolean inMTS = CashShopServer.getPlayerStorageMTS().getCharacterById(chr.getId()) != null;

        if (c.getCloseSession()) {
            return;
        }
        if (chr.hasBlockedInventory(true) || chr.getEventInstance() != null || chr.getMap() == null || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit()) || inCS || inMTS) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        final int loginstate = chr.getClient().getLoginState();
        if (loginstate != MapleClient.LOGIN_LOGGEDIN) {
            chr.getClient().getSession().close();
            return;
        }
        chr.changeChannel(slea.readByte() + 1);

        // 換頻道更新VIP等級
        c.loadVip(chr.getAccountID());
    }

}
