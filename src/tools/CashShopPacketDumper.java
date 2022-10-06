package tools;

import database.DatabaseConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

public class CashShopPacketDumper {

    private static boolean initItemSN = false;
    private static final HashMap<Integer, Integer> snitems = new HashMap<>();

    public static void main(String[] args) throws IOException {
        String packet;
        Properties props = new Properties();

        try {
            try (FileInputStream is = new FileInputStream("商城封包.txt")) {
                props.load(is);
            }
        } catch (IOException ex) {
            FileoutputUtil.outputFileError("logs/Except/CashShopPacketDumper.log", ex);
        }
        packet = props.getProperty("pack");
        LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream((byte[]) (byte[]) HexTool.getByteArrayFromHexString(packet)));
        String msg = "";
        int times = slea.readShort();
        for (int i = 0; i < times; i++) {
            String itemName;
            int itemid = 0;
            short count = 0;
            int discountPrice = 0;
            byte unk_1 = 0;
            byte priority = -1;
            short period = 0;
            int meso = 0;
            int sn = slea.readInt();
            int flags = slea.readInt();
            byte unk_2 = 0;
            byte gender = 0;
            byte showup = 0;
            byte mark = 0;
            byte unk_3 = 0;
            int packages = 0;
            if ((flags & 0x1) != 0) {
                itemid = slea.readInt();
            }
            if ((flags & 0x2) != 0) {
                count = slea.readShort();
            }
            if ((flags & 0x4) != 0) {
                discountPrice = slea.readInt();
            }
            if ((flags & 0x8) != 0) {
                unk_1 = (byte) (slea.readByte() + 1);
            }
            if ((flags & 0x10) != 0) {
                priority = (byte) (slea.readByte());
            }
            if ((flags & 0x20) != 0) {
                period = slea.readShort();
            }
            if ((flags & 0x40) != 0) {
                slea.readInt();
            }
            if ((flags & 0x80) != 0) {
                meso = slea.readInt();
            }
            if ((flags & 0x100) != 0) {
                unk_2 = (byte) (slea.readByte() + 1);
            }
            if ((flags & 0x200) != 0) {
                gender = slea.readByte();
            }
            if ((flags & 0x400) != 0) {
                showup = slea.readByte();
            }
            if ((flags & 0x800) != 0) {
                mark = slea.readByte();
            }
            if ((flags & 0x1000) != 0) {
                unk_3 = (byte) (slea.readByte() + 1);
            }
            if ((flags & 0x2000) != 0) {
                slea.readShort();
            }
            if ((flags & 0x4000) != 0) {
                slea.readShort();
            }
            if ((flags & 0x8000) != 0) {
                slea.readShort();
            }
            if ((flags & 0x10000) != 0) {
                packages = 1;
                int time = slea.readByte();
                while (time > 0) {
                    slea.readInt();
                    time--;
                }
            }
            if (itemid > 0) {
                itemName = MapleItemInformationProvider.getInstance().getName(itemid);
            } else {
                if (!initItemSN) {
                    InitSN();
                }
                int item = getItemIdBySn(sn);
                itemName = MapleItemInformationProvider.getInstance().getName(item);
            }
            if (itemName == null) {
                itemName = "";
            }
            addItem(sn, itemName, discountPrice, mark, showup, itemid, priority, packages, period, gender, count, meso, unk_1, unk_2, unk_3);
            msg += "NAME:" + itemName + " SN: " + sn + " FLAGS: " + flags + " ITEM: " + itemid + " count: " + count + " discountPrice: " + discountPrice + " unk_1: " + unk_1 + " priority: " + priority + " period: " + period + " meso: " + meso + " gender: " + gender + " showup: " + showup + " mark: " + mark + " unk_3: " + unk_3 + "\r\n";
            System.out.println("NAME:" + itemName + " SN: " + sn + " FLAGS: " + flags + " ITEM: " + itemid + " count: " + count + " discountPrice: " + discountPrice + " unk_1: " + unk_1 + " priority: " + priority + " period: " + period + " meso: " + meso + " gender: " + gender + " showup: " + showup + " mark: " + mark + " unk_3: " + unk_3);
        }
        msg += "-----------------" + times + " in total";

        System.out.println("-----------------" + times + "個");

        if (slea.available() > 0) {
            msg += "\r\nnow: " + slea.toString();
        }

        FileoutputUtil.log("CashShop.txt", msg);
    }

    public static void addItem(int serial, String itemName, int discount_price, int mark, int showup, int itemid, int priority, int packages, int period, int gender, int count, int meso, int unk_1, int unk_2, int unk_3) {
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "insert into cashshop_modified_items (serial, name, discount_price, mark, showup, itemid, priority, package, period, gender, count, meso, unk_1, unk_2, unk_3, extra_flags) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(sql)) {
                int index = 0;
                ps.setInt(++index, serial);
                ps.setString(++index, itemName);
                ps.setInt(++index, discount_price);
                ps.setInt(++index, mark);
                ps.setInt(++index, showup);//sn
                ps.setInt(++index, itemid);
                ps.setInt(++index, priority);
                ps.setInt(++index, packages);
                ps.setInt(++index, period);
                ps.setInt(++index, gender);
                ps.setInt(++index, count);
                ps.setInt(++index, meso);
                ps.setInt(++index, unk_1);
                ps.setInt(++index, unk_2);
                ps.setInt(++index, unk_3);
                ps.executeUpdate();
            }
        } catch (SQLException sqlex) {
            System.err.println(sqlex);
        }
    }

    private static void InitSN() {
        if (!initItemSN) {
            MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Etc.wz"));
            for (MapleData field : data.getData("Commodity.img").getChildren()) {
                final int itemId = MapleDataTool.getIntConvert("ItemId", field, 0);
                final int SN = MapleDataTool.getIntConvert("SN", field, 0);
                if (SN > 0) {
                    snitems.put(SN, itemId);
                }
            }
            initItemSN = true;
        }
    }

    private static int getItemIdBySn(int sn) {
        int item = -1;
        if (snitems.containsKey(sn)) {
            item = snitems.get(sn);
        }
        return item;
    }

}
