/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import database.DatabaseConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import tools.FileoutputUtil;

/**
 *
 * @author pungin
 */
public class MapleGachapon {

    private static Map<Integer, String> gashaponName = new HashMap();
    private static final List<MapleGachaponItem> items = new LinkedList();

    public static void loadItems() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM gachaponitems");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new MapleGachaponItem(
                        rs.getInt("itemId"),
                        rs.getInt("quantity"),
                        rs.getInt("remainingQuantity"),
                        rs.getInt("minimum_quantity"),
                        rs.getInt("maximum_quantity"),
                        rs.getInt("chance"),
                        rs.getInt("smegaType"),
                        rs.getInt("gachaponType"))
                );
            }
            ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM gashapons");
            rs = ps.executeQuery();
            while (rs.next()) {
                gashaponName.put(rs.getInt("id"), rs.getString("name"));
            }
            ps.close();

        } catch (Exception ex) {
            FileoutputUtil.outputFileError("MapleGachapon.txt", ex);
        }
    }

    public static List<MapleGachaponItem> getItems() {
        if (items.isEmpty()) {
            loadItems();
        }
        List<MapleGachaponItem> list = new LinkedList();
        list.addAll(items);
        return list;
    }

    public static List<MapleGachaponItem> getItemsByType(int gachaponType) {
        List<MapleGachaponItem> item = getItems();
        List<MapleGachaponItem> list = new LinkedList();
        for (MapleGachaponItem it : item) {
            if (it.getGachaponType() == -1 || it.getGachaponType() == gachaponType) {
                list.add(it);
            }
        }
        return list;
    }

    public static MapleGachaponItem randomItem(int gachaponType) {
        List<MapleGachaponItem> itemList = getItemsByType(gachaponType);
        if (itemList.isEmpty()) {
            return null;
        }
        List<MapleGachaponItem> list = new LinkedList();
        int chance = Randomizer.nextInt(10000);
        for (MapleGachaponItem item : itemList) {
            if (item.getChance() >= chance
                    && (item.getRemainingQuantity() >= item.getMinQuantity()
                    || item.getQuantity() == 0)) {
                list.add(item);
            }
        }
        if (list.isEmpty()) {
            return randomItem(gachaponType);
        }
        return list.get(Randomizer.nextInt(list.size()));
    }

    public static int gainItem(MapleGachaponItem item) {
        int quantity = item.getMinQuantity();
        if (quantity > 1 && item.getMaxQuantity() > item.getMinQuantity()) {
            quantity += Randomizer.nextInt(item.getMaxQuantity() - item.getMinQuantity());
        }
        if (item.getQuantity() != 0) {
            if (quantity > item.getRemainingQuantity()) {
                quantity = item.getRemainingQuantity();
            }
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE gachaponitems SET `remainingQuantity` = ? WHERE itemId = ? AND quantity = ? AND remainingQuantity = ? AND minimum_quantity = ? AND maximum_quantity = ? AND chance = ? AND smegaType = ? AND gachaponType = ?");
                ps.setInt(1, item.getRemainingQuantity() - quantity);
                ps.setInt(2, item.getItemId());
                ps.setInt(3, item.getQuantity());
                ps.setInt(4, item.getRemainingQuantity());
                ps.setInt(5, item.getMinQuantity());
                ps.setInt(6, item.getMaxQuantity());
                ps.setInt(7, item.getChance());
                ps.setInt(8, item.getSmegaType());
                ps.setInt(9, item.getGachaponType());
                ps.execute();
                ps.close();
            } catch (SQLException ex) {
            }
            item.setRemainingQuantity(item.getRemainingQuantity() - quantity);
        }
        return quantity;
    }

    public static String getGachaponName(int type) {
        return gashaponName.get(type);
    }

}
