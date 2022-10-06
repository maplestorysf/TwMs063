package handling.login.handler;

import client.LoginCrypto;
import client.MapleCharacter;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import tools.FileoutputUtil;

public class AutoRegister {

    private static final int ACCOUNTS_PER_MAC = 2;
    public static boolean autoRegister = LoginServer.getAutoReg();
    public static boolean success = false;
    public static boolean mac = true;

    public static boolean getAccountExists(String login) {
        boolean accountExists = false;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                accountExists = true;
            }
        } catch (SQLException ex) {
            System.err.println("[getAccountExists]" + ex);
        }
        return accountExists;
    }

    public static void createAccount(String login, String pwd, String eip, String macData) {
        String sockAddr = eip;
        Connection con;

        try {
            con = DatabaseConnection.getConnection();
        } catch (Exception ex) {
            System.err.println("[createAccount]" + ex);
            return;
        }

        try {
            ResultSet rs;
            try (PreparedStatement ipc = con.prepareStatement("SELECT Macs FROM accounts WHERE macs = ?")) {
                ipc.setString(1, macData);
                rs = ipc.executeQuery();
                if (rs.first() == false || rs.last() == true && rs.getRow() < ACCOUNTS_PER_MAC) {
                    try {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, lastmac, SessionIP) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                            Calendar c = Calendar.getInstance();
                            int year = c.get(Calendar.YEAR);
                            int month = c.get(Calendar.MONTH) + 1;
                            int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
                            ps.setString(1, login);
                            ps.setString(2, LoginCrypto.hexSha1(pwd));
                            ps.setString(3, "TMS113@mail.com");
                            ps.setString(4, year + "-" + month + "-" + dayOfMonth);//Created day
                            ps.setString(5, macData);
                            ps.setString(6, macData);
                            ps.setString(7, "/" + sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                            ps.executeUpdate();
                        }
                        success = true;
                    } catch (SQLException ex) {
                        System.err.println("createAccount" + ex);
                        return;
                    }
                }
                if (rs.getRow() >= ACCOUNTS_PER_MAC) {
                    mac = false;
                } else {
                    mac = true;
                    FileoutputUtil.logToFile("logs/data/註冊帳號.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "] 帳號：　" + login + " 密碼：" + pwd + " IP：/" + sockAddr.substring(1, sockAddr.lastIndexOf(':')) + " MAC： " + macData + " 註冊成功 : " + (mac ? "成功" : "失敗"), false, false);

                    try {
                        for (World wl : LoginServer.getWorlds()) {
                            for (ChannelServer cserv : wl.getChannels()) {
                                for (MapleCharacter chr_ : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                                    if (chr_.getShow(1)) {
                                        chr_.dropMessage("[GM密語] 帳號： " + login + " 密碼：" + pwd + " IP：/" + sockAddr.substring(1, sockAddr.lastIndexOf(':')) + " MAC： " + macData + " 註冊成功 : 是");
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
            rs.close();
        } catch (SQLException ex) {
            System.err.println("[createAccount]" + ex);
        }
    }
}
