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
package tools;

import client.MapleCharacter;
import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class FileoutputUtil {

    // Logging output file
    private static final SimpleDateFormat sdfT = new SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒");
    public static final String Acc_Stuck = "logs/Except/Log_AccountStuck.txt",
            Login_Error = "logs\\Log_Login_Error.txt",
            IP_Log = "logs\\Log_AccountIP.txt",
            GMCommand_Log = "logs\\Log_GMCommand.txt",
            Script_Bug = "logs/Except/腳本漏洞.txt",
            // Zakum_Log = "Log_Zakum.rtf",
            //Horntail_Log = "Log_Horntail.rtf",
            UnknownPacket_Log = "logs\\數據包_未知.txt",
            Packet_Log = "logs\\數據包收發\\Log.txt",
            Pinkbean_Log = "logs\\Log_Pinkbean.rtf",
            ScriptEx_Log = "logs\\Log_Script_Except.txt",
            PacketEx_Log = "logs/Except/Log_Packet_Except.txt", // I cba looking for every error, adding this back in.
            CodeEx_Log = "logs/Except/Log_Code_Except.txt",
            ArrayEx_Log = "logs/Except/Log_Packet_ArrayExcept.txt",
            Donator_Log = "logs\\Shadier_Merchant.txt",
            Hacker_Log = "logs\\Log_Hacker.txt",
            Movement_Log = "logs\\Log_Movement.txt",
            Client_Error_2 = "Logs/Client/用戶端_報錯_非38.txt",
            Client_Error = "Logs/Client/用戶端_報錯.txt",
            CommandEx_Log = "logs/Except/Log_Command_Except.txt" //PQ_Log = "Log_PQ.rtf"
            ;
    // End
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdf_ = new SimpleDateFormat("yyyy-MM-dd");
    private static final String FILE_PATH = "logs/";// + sdf.format(Calendar.getInstance().getTime()) + "/"
    private static final String ERROR = "error/";

    static {
        sdfGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static boolean readtxt(String txt, String existMsg) {
        /* String filePath = test.class.getResource("").getPath().replace("file:", "")
         + "/test.txt"; // 文件和該類在同個目錄下*/
        File file = new File(txt);
        if (!file.exists()) {
            try {
                file.createNewFile(); // 創建新文件  
            } catch (IOException ex) {
                ex.printStackTrace();
                // Logger.getLogger(FileoutputUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String filePath = txt;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StringUtil.codeString(filePath))); // 指定讀取文件的編碼格式，以免出現中文亂碼
            String str;
            while ((str = reader.readLine()) != null) {
                //  System.out.println(str);
                return str.contains(existMsg);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void print(final String name, final String s) {
        print(name, s, true);
    }

    public static void print(final String name, final String s, boolean line) {
        logToFile(FILE_PATH + name, s + (line ? "\r\n---------------------------------\r\n" : null));
    }

    public static void printError(final String name, final Throwable t, final String info) {
        printError(name, info + "\r\n" + getString(t));
    }

    public static void logToFile_PortalScript_Bug(final MapleCharacter chr, final String msg) {
        logToFile(Script_Bug, CurrentReadable_Time() + " 玩家[" + chr.getName() + "] 傳送腳本[" + chr.getPortalNow() + "]" + msg, false);
    }

    public static void logToFile_ReactorScript_Bug(final MapleCharacter chr, final String msg) {
        logToFile(Script_Bug, CurrentReadable_Time() + " 玩家[" + chr.getName() + "] 反應物腳本[" + chr.getReactorNow() + "]" + msg, false);
    }

    public static void logToFile_NpcScript_Bug(final MapleCharacter chr, final String msg) {
        logToFile(Script_Bug, CurrentReadable_Time() + " 玩家[" + chr.getName() + "] NPC腳本[" + chr.getNpcNow() + "]" + msg, false);
    }

    public static void printError(final String name, final String s) {
        logToFile(FILE_PATH + ERROR + sdf_.format(Calendar.getInstance().getTime()) + "/" + name, s + "\r\n---------------------------------\r\n");
    }

    public static void outputFileError(final String file, final Throwable t, boolean size) {
        log(file, getString(t), size);
    }

    public static void outputFileError(final String file, final Throwable t) {
        log(file, getString(t));
    }

    public static void log(final String file, final String msg, boolean size) {
        logToFile(file, "\r\n------------------------ " + CurrentReadable_Time() + " ------------------------\r\n" + msg, false, size);
    }

    public static void log(final String file, final String msg) {
        logToFile(file, "\r\n------------------------ " + CurrentReadable_Time() + " ------------------------\r\n" + msg);
    }

    public static void logToFile(final String file, final String[] msgs) {
        for (int i = 0; i < msgs.length; i++) {
            logToFile(file, msgs[i], false);
            if (i < msgs.length - 1) {
                logToFile(file, "\r\n", false);
            }
        }
    }

    public static void logToFile(final String file, final String msg) {
        logToFile(file, msg, false);
    }

    public static void logToFileIfNotExists(final String file, final String msg) {
        logToFile(file, msg, true);
    }

    public static void logToFile(final String file, final String msg, boolean notExists) {
        logToFile(file, msg, notExists, true);
    }

    /**
     *
     * @param file - 檔案名稱(包含目錄)
     * @param oldmsg - 要記錄的訊息
     * @param notExists - 檔案是否存在
     * @param size - 是否單文件限制大小
     */
    public static void logToFile(final String file, final String oldmsg, boolean notExists, boolean size) {
        String msg = oldmsg;
        if (!oldmsg.contains("\r\n")) {
            msg = "\r\n" + oldmsg;
        }
        FileOutputStream out = null;
        try {
            File outputFile = new File(file);
            if (outputFile.exists() && outputFile.isFile() && outputFile.length() >= 1024000 && size) {
                String sub = file.substring(0, file.indexOf('/', file.indexOf("/") + 1) + 1) + "old/" + file.substring(file.indexOf('/', file.indexOf("/") + 1) + 1, file.length() - 4);
                String time = sdfT.format(Calendar.getInstance().getTime());
                String sub2 = file.substring(file.length() - 4, file.length());
                String output = sub + "_" + time + sub2;
                if (new File(output).getParentFile() != null) {
                    new File(output).getParentFile().mkdirs();
                }
                outputFile.renameTo(new File(output));
                outputFile = new File(file);
            }
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            if (!out.toString().contains(msg) || !notExists) {
                OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
                osw.write(msg);
                osw.flush();
            }
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static String getChineseData() {
        return sdfT.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_Date() {
        return sdf_.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_Time() {
        return sdf.format(Calendar.getInstance().getTime());
    }

    public static String NowTime() {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");//可以方便地修改日期格式    
        String hehe = dateFormat.format(now);
        return hehe;
    }

    public static String CurrentReadable_TimeGMT() {
        return sdfGMT.format(new Date());
    }

    public static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }

    public static void logToFile_chr(MapleCharacter chr, final String file, final String msg) {
        logToFile(file, "\r\n" + FileoutputUtil.NowTime() + " 帳號 " + chr.getClient().getAccountName() + " 名稱 " + chr.getName() + " (" + chr.getId() + ") 等級 " + chr.getLevel() + " 地圖 " + chr.getMapId() + msg, false);
    }
}
