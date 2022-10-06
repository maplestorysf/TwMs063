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
package handling.login.handler;

import java.util.List;
import java.util.Calendar;

import client.inventory.IItem;
import client.inventory.Item;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConfig;
import constants.WorldConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobInfoFlag;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;
import tools.KoreanDateUtil;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;

public class CharLoginHandler {

    private static boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        return c.loginAttempt > 5;
    }

    public static final void handleWelcome(final MapleClient c) {
        c.sendPing();
    }

    public static final void handleLogin(final LittleEndianAccessor slea, final MapleClient c) {
        String account = null;

        String password = null;
        try {
            account = slea.readMapleAsciiString();
            password = slea.readMapleAsciiString();
        } catch (NegativeArraySizeException ex) {
        }

        if (account != null && password != null) {
            String macData = readMacAddress(slea, c);
            c.setMacs(macData);
            c.setLoginMacs(macData);
            c.setAccountName(account);
            final boolean ipBan = c.hasBannedIP();
            final boolean macBan = c.hasBannedMac();
            final boolean ban = ipBan || macBan;
            if (World.isPlayerStorage(c.getAccID())) {
                c.sendPacket(MaplePacketCreator.getPopupMsg("請稍後再登入。"));
                c.sendPacket(LoginPacket.getLoginFailed(1));
                return;
            }
            LoginResponse loginResult = c.login(account, password, ban);
            final Calendar tempbannedTill = c.getTempBanCalendar();
            String errorInfo = null;

            if (loginResult == LoginResponse.LOGIN_SUCCESS && World.isPlayerSaving(c.getAccID())) {
                errorInfo = "您登入的速度過快!\r\n請重新輸入.";
                loginResult = LoginResponse.NOP;
            } else if (loginResult == LoginResponse.LOGIN_SUCCESS && ban && !c.isGm()) {
                //被封鎖IP或MAC的非GM角色成功登入處理
                loginResult = LoginResponse.ACCOUNT_BLOCKED;
                //if (macBan) {
                FileoutputUtil.logToFile("logs/data/" + (macBan ? "MAC" : "IP") + "封鎖_登入帳號.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "]  目前MAC位址:" + macData + " 所有MAC位址: " + c.getMacs() + " IP地址: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號：　" + account + " 密碼：" + password);
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                // MapleCharacter.ban(c.getSessionIPAddress(), c.getSession().remoteAddress().toString().split(":")[0], "Enforcing account ban, account " + account, false, 4, false);
                //}
            } else if (loginResult == LoginResponse.LOGIN_SUCCESS && (c.getGender() == 10 || c.getSecondPassword() == null)) {
                //選擇性别並設置第二組密碼
//            c.updateLoginState(MapleClient.CHOOSE_GENDER, c.getSessionIPAddress());
                c.sendPacket(LoginPacket.getGenderNeeded(c));
                return;
            } else if (loginResult == LoginResponse.NOT_REGISTERED) {
                //帳號不存在
                if (LoginServer.getAutoReg()) {
//                    if (password.equalsIgnoreCase("fixlogged")) {
//                        errorInfo = "這個密碼是解卡密碼，請換其他密碼。";
//                    } else 
                    if (account.length() >= 12) {
                        errorInfo = "您的帳號長度太長了唷!\r\n請重新輸入.";
                    } else if (account.contains(" ")) {
                        errorInfo = "您的帳號包含空白！\r\n請重新輸入帳號。";
                    } else {
                        AutoRegister.createAccount(account, password, c.getSession().remoteAddress().toString(), macData);
                        if (AutoRegister.success && AutoRegister.mac) {
                            errorInfo = "帳號創建成功,請重新登入!";
                        } else if (!AutoRegister.mac) {
                            errorInfo = "無法註冊過多的帳號密碼唷!";
                            AutoRegister.success = false;
                            AutoRegister.mac = true;
                        }
                    }
                    loginResult = LoginResponse.NOP;
                }
            } else if (!LoginServer.canLoginAgain(c.getAccID())) { // 換頻後
                int sec = (int) (((LoginServer.getLoginAgainTime(c.getAccID()) + 50 * 1000) - System.currentTimeMillis()) / 1000);
                c.loginAttempt = 0;
                errorInfo = "遊戲帳號將於" + sec + "秒後可以登入， 請耐心等候。";
                loginResult = LoginResponse.NOP;
            } else if (!LoginServer.canEnterGameAgain(c.getAccID())) { // 選擇角色後
                int sec = (int) (((LoginServer.getEnterGameAgainTime(c.getAccID()) + 60 * 1000) - System.currentTimeMillis()) / 1000);
                c.loginAttempt = 0;
                errorInfo = "遊戲帳號將於" + sec + "秒後可以登入， 請耐心等候。";
                loginResult = LoginResponse.NOP;
            }
            if (ServerConfig.ENABLE_OTP && c.isGm()) {
                if (!c.passwordVerified) {
                    if (loginResult == LoginResponse.LOGIN_SUCCESS) {
                        c.passwordVerified = true;
                        loginResult = LoginResponse.NOP;
                        errorInfo = "這個帳號只能透過安全通道登入。";
                    }
                } else {
                    if (c.verifyOTP(password))
                        loginResult = LoginResponse.LOGIN_SUCCESS;
                    else {
                        loginResult = LoginResponse.NOP;
                        errorInfo = "您使用的通道不夠安全。";
                    }
                }
            }

            if (loginResult != LoginResponse.LOGIN_SUCCESS) {
                if (!loginFailCount(c)) {
                    c.sendPacket(LoginPacket.getLoginFailed(loginResult.getValue()));
                    if (errorInfo != null) {
                        c.getSession().writeAndFlush(MaplePacketCreator.getPopupMsg(errorInfo));
                    }
                } else {
                    c.getSession().close();
                    if (ServerConfig.LOG_DC) {
                        FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n帳號[" + account + "]伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    }
                }
            } else if (tempbannedTill.getTimeInMillis() != 0) {
                if (!loginFailCount(c)) {
                    c.sendPacket(LoginPacket.getTempBan(KoreanDateUtil.getTempBanTimestamp(tempbannedTill.getTimeInMillis()), c.getBanReason()));
                } else {
                    c.getSession().close();
                    if (ServerConfig.LOG_DC) {
                        FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n帳號[" + account + "]伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    }
                }
            } else {
                final String key = RandomString(6);
                c.loginAttempt = 0;
                c.updateMacs(macData);
                c.setClientKey(key);
                FileoutputUtil.logToFile("logs/data/登入帳號.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "]  MAC 地址 : " + c.getLoginMacs() + " IP 地址 : " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號：　" + account + " 密碼：" + password);
                LoginServer.addLoginKey(c, key);
                LoginWorker.registerClient(c);
            }
        }
    }

    public static final void SetGenderRequest(final LittleEndianAccessor slea, final MapleClient c) {
        String username = slea.readMapleAsciiString();
        String password = slea.readMapleAsciiString();
        byte gender = slea.readByte();
        if (gender > 1 || gender < 0) {
            c.getSession().close();
            return;
        }
        if (c.getAccountName().equals(username) && c.getSecondPassword() == null) {
            c.setGender(gender);
            c.setSecondPassword(password);
            c.update2ndPassword();
            c.updateGender();
            c.sendPacket(LoginPacket.getGenderChanged(c));
            c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress());
            FileoutputUtil.logToFile("logs/data/註冊第二組.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "] 帳號：　" + username + " 第二組密碼：" + password + " IP：/" + c.getSessionIPAddress() + " MAC： " + c.getLoginMacs(), false, false);
        } else {
            c.getSession().close();
        }
    }

    public static final void ServerListRequest(final MapleClient c) {
        if (!c.isCanloginpw()) {
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_SERVERLIST, c.getSessionIPAddress());
        LoginServer.forceRemoveClient(c, false);
        for (World iWorld : LoginServer.getWorlds()) {
            c.sendPacket(LoginPacket.getServerList(iWorld.getWorldId(), LoginPacket.Server.getById(iWorld.getWorldId()).toString(), iWorld.getFlag(), iWorld.getEventMessage(), iWorld.getChannels()));
        }
        c.sendPacket(LoginPacket.getEndOfServerList());
    }

    public static final void ServerStatusRequest(final MapleClient c) {
        if (!c.isCanloginpw()) {
            c.getSession().close();
            return;
        }
        LoginServer.forceRemoveClient(c, false);
        ChannelServer.forceRemovePlayerByCharNameFromDataBase(c, c.loadCharacterNamesByAccId(c.getAccID()));
        if (ChannelServer.forceRemovePlayerByCharIDFromDataBase(c, c.loadCharacterIDsByAccId(c.getAccID()), c.getAccID())) {

        } else {

            // 0 = Select world normally
            // 1 = "Since there are many users, you may encounter some..."
            // 2 = "The concurrent users in this world have reached the max"
            final int numPlayer = LoginServer.getUsersOn();
            final int userLimit = WorldConstants.UserLimit;
            if (numPlayer >= userLimit) {
                c.sendPacket(LoginPacket.getServerStatus(2));
            } else if (numPlayer * 2 >= userLimit) {
                c.sendPacket(LoginPacket.getServerStatus(1));
            } else {
                c.sendPacket(LoginPacket.getServerStatus(0));
            }
        }
    }

    public static final void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isCanloginpw()) {
            if (ServerConfig.LOG_DC) {
                FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            c.getSession().close();
            return;
        }
        if (c.getCloseSession()) {
            if (ServerConfig.LOG_DC) {
                FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            return;
        }
        if (c.getLoginState() != MapleClient.LOGIN_SERVERLIST) {
            if (ServerConfig.LOG_DC) {
                FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_CHARLIST, c.getSessionIPAddress());
        ChannelServer.forceRemovePlayerByCharNameFromDataBase(c, c.loadCharacterNamesByAccId(c.getAccID()));
        LoginServer.forceRemoveClient(c, false);
        slea.readByte();
        final int server = slea.readByte();
        final int channel = slea.readByte() + 1;

        c.setWorld(server);
        //System.out.println("Client " + c.getSession().getRemoteAddress().toString().split(":")[0] + " is connecting to server " + server + " channel " + channel + "");
        c.setChannel(channel);
        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null) {
            c.sendPacket(LoginPacket.getCharList(c.getSecondPassword() != null, chars, c.getCharacterSlots()));
        } else {
            if (ServerConfig.LOG_DC) {
                FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            c.getSession().close();
        }
    }

    public static final void checkCharName(final String name, final MapleClient c) {
        c.sendPacket(LoginPacket.charNameResponse(name, !MapleCharacterUtil.canCreateChar(name) || LoginInformationProvider.getInstance().isForbiddenName(name)));
    }

    public static final void handleCreateCharacter(final LittleEndianAccessor slea, final MapleClient c) {
        byte gender, skin;
        short subcategory;
        Map<JobInfoFlag, Integer> infos = new LinkedHashMap();
        final String name = slea.readMapleAsciiString();
        LoginInformationProvider li = LoginInformationProvider.getInstance();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!MapleCharacterUtil.canCreateChar(name) || (li.isForbiddenName(name))) {
            System.out.println("非法創建角色名: " + name);
            return;
        }
        final int job_type = 1; // 0 = 皇家騎士團, 1 = 冒險家, 2 = 狂狼勇士
        JobType job = JobType.getByType(job_type);
        if (!c.getWorldServer().isCanCreate() && !c.isGm()) {
            c.sendPacket(MaplePacketCreator.getPopupMsg("很抱歉\r\n目前此世界不開放創建角色。"));
            c.sendPacket(LoginPacket.getLoginFailed(1));
            return;
        }
        subcategory = 0;
        gender = c.getGender();
        skin = 0;
        // 驗證創建角色的可選項是否正確
        int index = 0;
        for (JobInfoFlag jf : JobInfoFlag.values()) {
            if (jf.check(job.flag)) {
                int value = slea.readInt();
                if (!li.isEligibleItem(gender, index, /*job == JobType.影武者 ? 1 : */ job.id, value)) {
                    System.err.println("創建角色確認道具出錯 - 性別:" + gender + " 職業:" + job.name() + " 類型:" + jf.name() + " 值:" + value);
                    return;
                }
                infos.put(jf, value);
                index++;
            } else {
                infos.put(jf, 0);
            }
        }
        if (gender > 1 || gender < 0) {
            c.getSession().close();
            return;
        }
        short[] stats = new short[4];
        int totalStat = 0;
        for (int i = 0; i < 4; i++) {
            stats[i] = slea.readByte();
            if (stats[i] < 4 || stats[i] > 13) {
                return;
            }
            totalStat += stats[i];
        }
        if (totalStat != 25) {
            return;
        }
        if (slea.available() != 0) {
            System.err.println("創建角色讀取訊息出錯, 有未讀取訊息: " + HexTool.toString(slea.read((int) slea.available())));
            return;
        }

        //讀取創建角色默認配置
        MapleCharacter newchar = MapleCharacter.getDefault(c, job_type, stats);
        newchar.setWorld((byte) (c.getWorld()));
        newchar.setFace(infos.get(JobInfoFlag.臉型));
        newchar.setHair(infos.get(JobInfoFlag.髮型));
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skin);

        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        IItem item;
        //-1 Hat | -2 Face | -3 Eye acc | -4 Ear acc | -5 Topwear 
        //-6 Bottom | -7 Shoes | -8 glove | -9 Cape | -10 Shield | -11 Weapon
        int[] equips = new int[]{
            infos.get(JobInfoFlag.帽子),
            infos.get(JobInfoFlag.衣服),
            infos.get(JobInfoFlag.褲裙),
            infos.get(JobInfoFlag.披風),
            infos.get(JobInfoFlag.鞋子),
            infos.get(JobInfoFlag.手套),
            infos.get(JobInfoFlag.武器),
            infos.get(JobInfoFlag.副手),};
        for (int i : equips) {
            if (i > 0) {
                short[] equipSlot = ItemConstants.getEquipedSlot(i);
                if (equipSlot == null || equipSlot.length < 1) {
                    System.err.println("創建角色新增裝備出錯, 裝備欄位未知, 道具ID" + i);
                    continue;
                }
                item = ii.getEquipById(i);
                item.setPosition(equipSlot[0]);
                item.setGMLog("創建角色獲得, 時間 " + FileoutputUtil.CurrentReadable_Time());
                equip.addFromDB(item);
            }
        }

        newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0), 1);

        if (MapleCharacterUtil.canCreateChar(name) && !LoginInformationProvider.getInstance().isForbiddenName(name)) {
            MapleCharacter.saveNewCharToDB(newchar, job, subcategory);
            c.sendPacket(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.sendPacket(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static final void handleDeleteCharacter(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() < 7) {
            return;
        }
        slea.readByte();

        String _2ndPassword;
        _2ndPassword = slea.readMapleAsciiString();

        final int characterId = slea.readInt();
        if (!c.login_Auth(characterId)) {
            c.sendPacket(LoginPacket.secondPwError((byte) 0x14));
            return;
        }

        boolean pending = false;

        outerloop:
        for (World wl : LoginServer.getWorlds()) {
            for (ChannelServer cserv : wl.getChannels()) {
                if (cserv.getPlayerStorage().getPendingCharacter(characterId, false) != null) {
                    pending = true;
                    break outerloop;
                }
            }
        }

        if (!pending) {
            if (CashShopServer.getPlayerStorage().getPendingCharacter(characterId, false) != null) {
                pending = true;
            }
        }

        byte state = 0;

        if (c.getSecondPassword() != null) {
            if (_2ndPassword == null) {
                c.getSession().close();
                if (ServerConfig.LOG_DC) {
                    FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                }
                return;
            } else if (!c.check2ndPassword(_2ndPassword)) {
                state = 16;
            }
        }
        if (c.getLoginState() != MapleClient.LOGIN_CHARLIST) {
            return;
        }
        if (World.Find.findChr(characterId) != null || pending) {
            c.getSession().close();
            return;
        }
        if (state == 0) {
            state = (byte) c.deleteCharacter(characterId);
        }

        c.sendPacket(LoginPacket.deleteCharResponse(characterId, state));
    }

    public static final void handleSelectCharacter(final LittleEndianAccessor slea, final MapleClient c) {
        if (!LoginServer.CanLoginKey(c, c.getClientKey()) || (LoginServer.getLoginKey(c) == null && !c.getClientKey().isEmpty())) {
            FileoutputUtil.logToFile("Logs/Except/Log_主程式KEY異常.txt", FileoutputUtil.CurrentReadable_Time() + " IP: " + c.getSessionIPAddress() + " 帳號: " + c.getAccountName() + " 主程式Key: " + c.getClientKey() + " 伺服器KEY: " + LoginServer.getLoginKey(c) + " \r\n");
            return;
        }
        if (!LoginServer.containClient(c)) {
            return;
        }
        if (!LoginServer.CheckSelectChar(c.getAccID())) {// 快速登入
            return;
        }
        if (c.getCloseSession()) {// 多重登入
            return;
        }
        if (!c.isCanloginpw()) {// 登入口驗證
            c.getSession().close();
            return;
        }
        if (c.getLoginState() != MapleClient.LOGIN_CHARLIST) {
            c.getSession().close();
            return;
        }
        //LoginServer.addEnterGameAgainTime(c.getAccID());

        if (LoginServer.getLoginKey(c) == null) {
            FileoutputUtil.logToFile("Logs/Except/Log_主程式KEY_Null.txt", FileoutputUtil.CurrentReadable_Time() + " IP: " + c.getSessionIPAddress() + " 帳號: " + c.getAccountName() + " 主程式Key: " + c.getClientKey() + " 伺服器KEY: " + LoginServer.getLoginKey(c) + " \r\n");
        }
        //ChannelServer.forceRemovePlayerByAccId(c, c.getAccID());
        if (World.isPlayerSaving(c.getAccID())) { // 防止倉庫複製
            c.sendPacket(MaplePacketCreator.getPopupMsg("請稍後再試"));
            c.sendPacket(LoginPacket.getLoginFailed(1));
            return;
        }
        final int charId = slea.readInt();
        try {
            PreparedStatement ps = null;
            Connection con = DatabaseConnection.getConnection();
            ResultSet rs;
            ps = con.prepareStatement("select accountid from characters where id = ?");
            ps.setInt(1, charId);
            rs = ps.executeQuery();
            if (!rs.next() || rs.getInt("accountid") != c.getAccID()) {
                ps.close();
                rs.close();
                return;
            }
            ps.close();
            rs.close();
        } catch (Exception ex) {
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        byte[] ip = {127, 0, 0, 1};
        int port = 7575;
        try {
            ip = InetAddress.getByName(ChannelServer.getInstance(c.getWorld(), c.getChannel()).getSocket().split(":")[0]).getAddress();
            port = Integer.parseInt(ChannelServer.getInstance(c.getWorld(), c.getChannel()).getSocket().split(":")[1]);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Logger.getLogger(CharLoginHandler.class.getName()).log(Level.SEVERE, "getIP Error", ex);
        }
        String ips = c.getSessionIPAddress();
        World.clearChannelChangeDataByAccountId(c.getAccID());
        LoginServer.putLoginAuth(charId, ips.substring(ips.indexOf('/') + 1, ips.length()), c.getTempIP(), c.getChannel(), c.getLoginMacs());
        // 避免登入狀態歸0
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        System.setProperty(String.valueOf(c.getAccountName().toLowerCase()), "1");
        c.sendPacket(MaplePacketCreator.getServerIP(ip, port, charId));
        System.setProperty(String.valueOf(charId), "1");
        c.setReceiving(false);

    }

    private static String readMacAddress(final LittleEndianAccessor slea, final MapleClient c) {
        int[] bytes = new int[6];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = slea.readByteAsInt();
        }
        StringBuilder sps = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sps.append(StringUtil.getLeftPaddedStr(Integer.toHexString(bytes[i]).toUpperCase(), '0', 2));
            sps.append("-");
        }
        return sps.toString().substring(0, sps.toString().length() - 1);
    }

    public static String RandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(62);
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }
}
