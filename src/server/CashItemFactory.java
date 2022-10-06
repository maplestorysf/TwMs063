/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import database.DatabaseConnection;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 *
 * @author user
 */
public class CashItemFactory {

    private final static CashItemFactory instance = new CashItemFactory();
    private final Map<Integer, List<Integer>> openBox = new HashMap();
    private final Map<Integer, CashItem> itemStats = new HashMap<>();
    private final Map<Integer, List<Integer>> itemPackage = new HashMap<>();
    private final Map<Integer, CashModItem> itemMods = new HashMap<>();
    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider("Etc.wz");

    public static final CashItemFactory getInstance() {
        return instance;
    }

    public void initialize(boolean reload) {
        if (reload) {
            itemStats.clear();
            itemPackage.clear();
            itemMods.clear();
            openBox.clear();
        }
        if (!itemStats.isEmpty() || !itemPackage.isEmpty() || !itemMods.isEmpty() || !openBox.isEmpty()) {
            return;
        }

        System.out.println("【讀取中】 購物商城物品:::");

        final List<MapleData> cccc = data.getData("Commodity.img").getChildren();
        for (MapleData field : cccc) {
            final int SN = MapleDataTool.getIntConvert("SN", field, 0);

            final CashItem stats = new CashItem(SN,
                    MapleDataTool.getIntConvert("ItemId", field, 0),
                    MapleDataTool.getIntConvert("Count", field, 1),
                    MapleDataTool.getIntConvert("Price", field, 0),
                    MapleDataTool.getIntConvert("Period", field, 0),
                    MapleDataTool.getIntConvert("Gender", field, 2),
                    MapleDataTool.getIntConvert("Class", field, -1),
                    MapleDataTool.getIntConvert("OnSale", field, 0) > 0 && MapleDataTool.getIntConvert("Price", field, 0) > 0);
            if (SN > 0) {
                itemStats.put(SN, stats);
            }
        }

        refreshAllModInfo();

        final MapleData b = data.getData("CashPackage.img");
        for (MapleData c : b.getChildren()) {
            if (c.getChildByPath("SN") == null) {
                continue;
            }
            final List<Integer> packageItems = new ArrayList<>();
            for (MapleData d : c.getChildByPath("SN").getChildren()) {
                packageItems.add(MapleDataTool.getIntConvert(d));
            }
            itemPackage.put(Integer.parseInt(c.getName()), packageItems);
        }
    }

    private void refreshAllModInfo() {
        itemMods.clear();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_items");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CashModItem ret = new CashModItem(
                        rs.getInt("SN"),
                        rs.getString("Note"),
                        rs.getInt("ItemId"),
                        rs.getInt("Count"),
                        rs.getInt("Price"),
                        rs.getInt("Period"),
                        rs.getInt("Gender"),
                        rs.getInt("Class"),
                        rs.getInt("OnSale") > 0
                );
                if (ret.getId() == 0 && itemStats != null && itemStats.containsKey(ret.getSN())) {
                    ret.setId(itemStats.get(ret.getSN()).getId());
                }
                itemMods.put(ret.getSN(), ret);
                if (ret.isOnSale()) {
                    final CashItem cc = itemStats.get(ret.getSN());
                    ret.initFlags(cc);
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final List<CashItem> getAllItems() {
        return new ArrayList<>(itemStats.values());
    }

    public final List<CashItem> getHideAllDefaultItems() {
        List<CashItem> allItem = new LinkedList();
        for (CashItem ci : itemStats.values()) {
            CashModItem csMod = getModItem(ci.getSN());
            if ((csMod != null && !csMod.isOnSale()) || ci.isOnSale()) {
                allItem.add(ci);
            }
        }
        return allItem;
    }

    public final List<CashItem> getHideItems() {
        List<CashItem> allItem = new LinkedList();
        for (CashItem ci : itemStats.values()) {
            CashModItem csMod = getModItem(ci.getSN());
            if ((csMod != null && !csMod.isOnSale())) {
                allItem.add(ci);
            }
        }
        return allItem;
    }

    public final List<CashModItem> getAllModItems() {
        List<CashModItem> allItem = new LinkedList();
        for (CashModItem csMod : getAllModInfo()) {
            if (csMod.isOnSale()) {
                allItem.add(csMod);
            }
        }
        return allItem;
    }

    public final CashItem getSimpleItem(int sn) {
        return itemStats.get(sn);
    }

    public final CashItem getItem(int sn) {
        final CashItem stats = itemStats.get(Integer.valueOf(sn));
        final CashModItem z = getModItem(sn);
        if (z != null && z.isOnSale()) {
            return stats; //null doesnt matter
        }
        if (stats == null || !stats.isOnSale() || (z != null && !z.isOnSale())) {
            return null;
        }
        return null;
    }

    public final List<Integer> getPackageItems(int itemId) {
        return itemPackage.get(itemId);
    }

    public final CashModItem getModItem(int sn) {
        return itemMods.get(sn);
    }

    public final Collection<CashModItem> getAllModInfo() {
        return itemMods.values();
    }

    public final Map<Integer, List<Integer>> getRandomItemInfo() {
        return this.openBox;
    }

    public final int getItemSN(int itemid) {
        for (Map.Entry<Integer, CashModItem> ci : itemMods.entrySet()) {
            if (ci.getValue().getId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        for (Map.Entry<Integer, CashItem> ci : itemStats.entrySet()) {
            if (ci.getValue().getId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        return 0;
    }

    public final Set<Integer> getAllItemSNs() {
        return itemStats.keySet();
    }

    public void addModItem(CashModItem cModItem) {
        if (cModItem.getId() == 0) {
            if (itemStats == null || !itemStats.containsKey(cModItem.getSN())) {
                return;
            }
            cModItem.setId(itemStats.get(cModItem.getSN()).getId());
        }
        if (!itemMods.containsKey(cModItem.getSN())) {
            itemMods.put(cModItem.getSN(), cModItem);
            cModItem.initFlags(itemStats.get(cModItem.getSN()) != null ? itemStats.get(cModItem.getSN()) : null);
            PreparedStatement ps = null;
            Connection con = DatabaseConnection.getConnection();
            try {
                ps = con.prepareStatement("INSERT INTO cashshop_items (SN, Note, ItemId, Count, Price, Period, Gender, Class, OnSale) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, cModItem.getSN());
                ps.setString(2, cModItem.getNote());
                ps.setInt(3, cModItem.getId());
                ps.setInt(4, cModItem.getCount());
                ps.setInt(5, cModItem.getPrice());
                ps.setInt(6, cModItem.getPeriod());
                ps.setInt(7, cModItem.getGender());
                ps.setInt(8, cModItem.getFlage());
                ps.setInt(9, cModItem.isOnSale() ? 1 : 0);
                ps.execute();
                ps.close();
            } catch (SQLException ex) {
            }
        }
    }

    public void updateModItem(CashModItem cModItem) {
        if (cModItem.getId() == 0) {
            if (itemStats == null || !itemStats.containsKey(cModItem.getSN())) {
                return;
            }
            cModItem.setId(itemStats.get(cModItem.getSN()).getId());
        }
        itemMods.put(cModItem.getSN(), cModItem);
        cModItem.initFlags(itemStats.get(cModItem.getSN()) != null ? itemStats.get(cModItem.getSN()) : null);
        PreparedStatement ps = null;
        Connection con = DatabaseConnection.getConnection();
        try {
            ps = con.prepareStatement("UPDATE cashshop_items SET Note = ?, ItemId = ?, Count = ?, Price = ?, Period = ?, Gender = ?, Class = ?, OnSale = ? WHERE SN = ?");
            ps.setString(1, cModItem.getNote());
            ps.setInt(2, cModItem.getId());
            ps.setInt(3, cModItem.getCount());
            ps.setInt(4, cModItem.getPrice());
            ps.setInt(5, cModItem.getPeriod());
            ps.setInt(6, cModItem.getGender());
            ps.setInt(7, cModItem.getFlage());
            ps.setInt(8, cModItem.isOnSale() ? 1 : 0);
            ps.setInt(9, cModItem.getSN());
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
        }
    }

    public void deleteModItem(int sn) {
        itemMods.remove(sn);
        PreparedStatement ps = null;
        Connection con = DatabaseConnection.getConnection();
        try {
            ps = con.prepareStatement("DELETE FROM cashshop_items WHERE SN = ?");
            ps.setInt(1, sn);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
        }
    }

    public void clearItems() {
        refreshAllModInfo();
    }
}
