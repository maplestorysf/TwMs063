package handling.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import constants.GameConstants;
import client.inventory.ItemLoader;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import database.DatabaseConnection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import scripting.NPCScriptManager;
import server.MapleDueyActions;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;

public class DueyHandler {


    /*
     * 19 = Successful
     * 18 = One-of-a-kind Item is already in Reciever's delivery
     * 17 = The Character is unable to recieve the parcel
     * 15 = Same account
     * 14 = Name does not exist
     */
    public static final void DueyOperation(final LittleEndianAccessor slea, final MapleClient c) {

        final byte operation = slea.readByte();

        switch (operation) {
            case 1: { // 第二組處理
                final String _2ndpassword = slea.readMapleAsciiString();
                if (c.getSecondPassword() != null) {
                    if (_2ndpassword == null) { // 確認是否封包掛
                        c.getPlayer().dropMessage(1, "請輸入密碼。");
                        c.getPlayer().setConversation(0);
                        c.removeClickedNPC();
                        NPCScriptManager.getInstance().dispose(c);
                        return;
                    } else {
                        if (!c.check2ndPassword(_2ndpassword)) { // 錯誤密碼
                            c.getPlayer().dropMessage(1, "密碼錯誤。");
                            c.getPlayer().setConversation(0);
                            c.removeClickedNPC();
                            NPCScriptManager.getInstance().dispose(c);
                            return;
                        }
                        //  int unk = slea.readInt(); // Theres an int here, value = 1
                        //  9 = error
                        final int conv = c.getPlayer().getConversation();

                        if (conv == 2) { // Duey
                            c.sendPacket(MaplePacketCreator.sendDuey((byte) 10, loadItems(c.getPlayer())));
                        }
                        break;
                    }
                }
            }
            case 3: { // 寄送
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final byte inventId = slea.readByte();
                final short itemPos = slea.readShort();
                final short amount = slea.readShort();
                final int mesos = slea.readInt();
                final String recipient = slea.readMapleAsciiString();
                boolean quickdelivery = slea.readByte() > 0;
                String message = "";
                if (quickdelivery) {
                    message = slea.readMapleAsciiString();
                }

                final int finalcost = mesos + GameConstants.getTaxAmount(mesos) + (quickdelivery ? 0 : 5000);

                if (mesos >= 0 && mesos <= 100000000 && c.getPlayer().getMeso() >= finalcost) {
                    final int accid = MapleCharacterUtil.getIdByName(recipient);
                    if (accid != -1) {
                        if (accid != c.getAccID()) {
                            boolean recipientOn = false;
                            MapleClient rClient = null;

                            if (inventId > 0) {
                                final MapleInventoryType inv = MapleInventoryType.getByType(inventId);
                                final IItem item = c.getPlayer().getInventory(inv).getItem((byte) itemPos);
                                if (item == null) {
                                    c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                    return;
                                }
                                final byte flag = item.getFlag();
                                if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
                                    c.sendPacket(MaplePacketCreator.enableActions());
                                    return;
                                }
                                if (c.getPlayer().getItemQuantity(item.getItemId(), false) >= amount) {
                                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                    if (!ii.isDropRestricted(item.getItemId()) && !ii.isAccountShared(item.getItemId())) {
                                        if (addItemToDB(item, amount, mesos, c.getPlayer().getName(), message, accid, recipientOn)) {
                                            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, item.getQuantity(), true);
                                            } else {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, amount, true, false);
                                            }
                                            if (quickdelivery) {
                                                MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330000, 1, true, false);
                                            }
                                            c.getPlayer().gainMeso(-finalcost, false);
                                            c.sendPacket(MaplePacketCreator.sendDuey((byte) 19, null)); // Successfull
                                        } else {
                                            c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessful
                                        }
                                    } else {
                                        c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                    }
                                } else {
                                    c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                }
                            } else if (addMesoToDB(mesos, c.getPlayer().getName(), message, accid, recipientOn)) {
                                c.getPlayer().gainMeso(-finalcost, false);
                                if (quickdelivery) {
                                    MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330000, 1, true, false);
                                }
                                c.sendPacket(MaplePacketCreator.sendDuey((byte) 19, null)); // Successfull
                            } else {
                                c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                            }
                            if (recipientOn && rClient != null) {
                                rClient.sendPacket(MaplePacketCreator.sendDuey((byte) 19, null));
                            }
                        } else {
                            c.sendPacket(MaplePacketCreator.sendDuey((byte) 15, null)); // Same acc error
                        }
                    } else {
                        c.sendPacket(MaplePacketCreator.sendDuey((byte) 14, null)); // Name does not exist
                    }
                } else {
                    c.sendPacket(MaplePacketCreator.sendDuey((byte) 12, null)); // Not enough mesos
                }
                break;
            }
            case 5: { // 領收包裹
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = slea.readInt();
                //System.out.println("Item attempted : " + packageid);
                final MapleDueyActions dp = loadSingleItem(packageid, c.getPlayer().getId());
                if (dp == null) {
                    return;
                }
                if (dp.getItem() != null && !MapleInventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
                    c.sendPacket(MaplePacketCreator.sendDuey((byte) 16, null)); // Not enough Space
                    return;
                } else if (dp.getMesos() < 0 || (dp.getMesos() + c.getPlayer().getMeso()) < 0) {
                    c.sendPacket(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                    return;
                }
                removeItemFromDB(packageid, c.getPlayer().getId()); // Remove first
                //System.out.println("Item removed : " + packageid);
                if (dp.getItem() != null) {
                    MapleInventoryManipulator.addFromDrop(c, dp.getItem(), false);
                }
                if (dp.getMesos() != 0) {
                    c.getPlayer().gainMeso(dp.getMesos(), false);
                }
                c.sendPacket(MaplePacketCreator.removeItemFromDuey(false, packageid));
                break;
            }
            case 6: { // 刪除包裹
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = slea.readInt();
                removeItemFromDB(packageid, c.getPlayer().getId());
                c.sendPacket(MaplePacketCreator.removeItemFromDuey(true, packageid));
                break;
            }
            case 8: { // 關閉快遞介面
                c.getPlayer().setConversation(0);
                c.removeClickedNPC();
                NPCScriptManager.getInstance().dispose(c);
                break;
            }
            default: {
                System.out.println("尚未處理的快遞操作碼 : " + slea.toString());
                break;
            }
        }

    }

    private static boolean addMesoToDB(final int mesos, final String sName, final String message, final int recipientID, final boolean isOn) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type, message) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, recipientID);
                ps.setString(2, sName);
                ps.setInt(3, mesos);
                ps.setLong(4, System.currentTimeMillis() + (20 * 24 * 60 * 60 * 1000));
                ps.setInt(5, isOn ? 0 : 1);
                ps.setInt(6, 3);
                ps.setString(7, message);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "addMesoToDB");
            return false;
        }
    }

    private static boolean addItemToDB(final IItem item, final int quantity, final int mesos, final String sName, final String message, final int recipientID, final boolean isOn) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type, message) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipientID);
                ps.setString(2, sName);
                ps.setInt(3, mesos);
                ps.setLong(4, System.currentTimeMillis() + (20 * 24 * 60 * 60 * 1000));
                ps.setInt(5, isOn ? 0 : 1);
                ps.setInt(6, item.getType());
                ps.setString(7, message);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        IItem itemx = item.copy();
                        itemx.setQuantity((short) quantity);
                        ItemLoader.DUEY.saveItems(Collections.singletonList(new Pair<>(itemx, GameConstants.getInventoryType(item.getItemId()))), rs.getInt(1));
                    }
                }
            }

            return true;
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "addItemToDB");
            return false;
        }
    }

    public static final List<MapleDueyActions> loadItems(final MapleCharacter chr) {
        List<MapleDueyActions> packages = new LinkedList<>();
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE RecieverId = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final int packageid = rs.getInt("packageid");
                MapleDueyActions dueypack = getItemByPID(packageid);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                dueypack.setMsg(rs.getString("message"));
                if (dueypack.getSentTime() > System.currentTimeMillis()) {
                    packages.add(dueypack);
                } else {
                    removeItemFromDB(packageid, chr.getId());
                }
            }
            rs.close();
            ps.close();
            return packages;
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "loadItems");
            return null;
        }
    }

    public static final MapleDueyActions loadSingleItem(final int packageid, final int charid) {
        List<MapleDueyActions> packages = new LinkedList<>();
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE PackageId = ? and RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(packageid);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                if (dueypack.getSentTime() > System.currentTimeMillis()) {
                    packages.add(dueypack);
                } else {
                    removeItemFromDB(packageid, charid);
                }
                rs.close();
                ps.close();
                return dueypack;
            } else {
                rs.close();
                ps.close();
                return null;
            }
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "loadSingleItem");
            return null;
        }
    }

    public static final void reciveMsg(final MapleClient c, final int recipientId) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?")) {
                ps.setInt(1, recipientId);
                ps.executeUpdate();
            }
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "reciveMsg");
        }
    }

    private static void removeItemFromDB(final int packageid, final int charid) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ? and RecieverId = ?")) {
                ps.setInt(1, packageid);
                ps.setInt(2, charid);
                ps.executeUpdate();
            }
        } catch (SQLException se) {
            FilePrinter.printError("DueyHandler.txt", se, "removeItemFromDB");
        }
    }

    private static MapleDueyActions getItemByPID(final int packageid) {
        try {
            Map<Long, Pair<IItem, MapleInventoryType>> iter = ItemLoader.DUEY.loadItems(false, packageid);
            if (iter != null && iter.size() > 0) {
                for (Pair<IItem, MapleInventoryType> i : iter.values()) {
                    return new MapleDueyActions(packageid, i.getLeft());
                }
            }
        } catch (Exception se) {
            FilePrinter.printError("DueyHandler.txt", se, "getItemByPID");
        }
        return new MapleDueyActions(packageid);
    }
}
