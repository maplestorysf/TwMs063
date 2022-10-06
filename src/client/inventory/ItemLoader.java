/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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
package client.inventory;

import constants.GameConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import database.DatabaseConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.FileoutputUtil;
import tools.Pair;

public enum ItemLoader {

    INVENTORY(0, false, "inventoryitems", "inventoryequipment"),
    STORAGE(1, true, "inventoryitems", "inventoryequipment"),
    CASHSHOP_EXPLORER(2, true, "csitems", "csequipment"),
    CASHSHOP_CYGNUS(3, true, "csitems", "csequipment"),
    CASHSHOP_ARAN(4, true, "csitems", "csequipment"),
    HIRED_MERCHANT(5, true, "csitems", "csequipment"),
    DUEY(6, false, "dueyitems", "dueyequipment"),
    CASHSHOP_EVAN(7, true, "csitems", "csequipment"),
    MTS(8, false, "mtsitems", "mtsequipment"),
    MTS_TRANSFER(9, false, "mtstransfer", "mtstransferequipment"),
    CASHSHOP_DB(10, true, "csitems", "csequipment"),
    CASHSHOP_RESIST(11, true, "csitems", "csequipment"),;
    private final int value;
    private final boolean account;
    private final String table, table_equip;

    private ItemLoader(int value, boolean account, String table, String table_equip) {
        this.value = value;
        this.account = account;
        this.table = table;
        this.table_equip = table_equip;
    }

    public int getValue() {
        return value;
    }

    //does not need connection con to be auto commit
    public Map<Long, Pair<IItem, MapleInventoryType>> loadItems(boolean login, Integer... id) throws SQLException {
        if (!"inventoryitems".equals(table)) {
            List<Integer> lulz = Arrays.asList(id);
            Map<Long, Pair<IItem, MapleInventoryType>> items = loadItems(true, login, id);
            if (items.size() > 0) {
                StringBuilder query = new StringBuilder();
                query.append("DELETE FROM `");
                query.append(table);
                query.append("` WHERE `type` = ? AND (`");
                query.append(account ? "accountid" : "characterid");
                query.append("` = ?");
                if (this == HIRED_MERCHANT) {
                    query.append(" OR `packageid` = ?");
                }
                if (this == STORAGE) {
                    query.append(" AND `world` = ?");
                }
                query.append(")");

                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString());
                ps.setInt(1, value);
                for (int i = 0; i < lulz.size(); i++) {
                    ps.setInt(i + 2, lulz.get(i));
                }
                ps.executeUpdate();
                ps.close();
                List<Pair<IItem, MapleInventoryType>> list = new ArrayList();
                for (Pair<IItem, MapleInventoryType> p : items.values()) {
                    list.add(p);
                }
                saveItems(list, id);
            }
        }
        Map<Long, Pair<IItem, MapleInventoryType>> items = loadItems(false, login, id);
        return items;
    }

    private Map<Long, Pair<IItem, MapleInventoryType>> loadItems(boolean old, boolean login, Integer... id) throws SQLException {
        List<Integer> lulz = Arrays.asList(id);
        Map<Long, Pair<IItem, MapleInventoryType>> items = new LinkedHashMap<>();
        if ((this == HIRED_MERCHANT || this == STORAGE) && lulz.size() != 2) {
            return items;
        }
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `");
        query.append(old ? table : "inventoryitems");
        query.append("` LEFT JOIN `");
        query.append(old ? table_equip : "inventoryequipment");
        query.append("` USING(`inventoryitemid`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid");
        query.append("` = ?");
        if (this == HIRED_MERCHANT) {
            query.append(" AND `packageid` = ?");
        } else if (this == STORAGE) {
            query.append(" AND `world` = ?");
        }

        if (login) {
            query.append(" AND `inventorytype` = ");
            query.append(MapleInventoryType.EQUIPPED.getType());
        }

        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString());
        ps.setInt(1, value);
        for (int i = 0; i < lulz.size(); i++) {
            ps.setInt(i + 2, lulz.get(i));
        }
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));

            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"), rs.getInt("uniqueid"), rs.getByte("flag"));
                if (!login) {
                    equip.setQuantity((short) 1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setInventoryitemId(rs.getLong("inventoryitemid"));
                    equip.setExpiration(rs.getLong("expiredate"));
                    equip.setEquipOnlyId(rs.getLong("equipOnlyId"));
                    equip.setWorld(rs.getInt("World"));
                    equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                    equip.setLevel(rs.getByte("level"));
                    equip.setStr(rs.getShort("str"));
                    equip.setDex(rs.getShort("dex"));
                    equip.setInt(rs.getShort("int"));
                    equip.setLuk(rs.getShort("luk"));
                    equip.setHp(rs.getShort("hp"));
                    equip.setMp(rs.getShort("mp"));
                    equip.setWatk(rs.getShort("watk"));
                    equip.setMatk(rs.getShort("matk"));
                    equip.setWdef(rs.getShort("wdef"));
                    equip.setMdef(rs.getShort("mdef"));
                    equip.setAcc(rs.getShort("acc"));
                    equip.setAvoid(rs.getShort("avoid"));
                    equip.setHands(rs.getShort("hands"));
                    equip.setSpeed(rs.getShort("speed"));
                    equip.setJump(rs.getShort("jump"));
                    equip.setViciousHammer(rs.getByte("ViciousHammer"));
                    equip.setItemEXP(rs.getInt("itemEXP"));
                    equip.setGMLog(rs.getString("GM_Log"));
                    equip.setDurability(rs.getInt("durability"));
                    equip.setEnhance(rs.getByte("enhance"));
                    if (!old) {
                        equip.setIncSkill(rs.getInt("incSkill"));
                    }
                    equip.setHpR(rs.getShort("hpR"));
                    equip.setMpR(rs.getShort("mpR"));
                    equip.setGiftFrom(rs.getString("sender"));
                    if (equip.getUniqueId() > -1) {
                        if (GameConstants.isEffectRing(rs.getInt("itemid"))) {
                            MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(), mit.equals(MapleInventoryType.EQUIPPED));
                            if (ring != null) {
                                equip.setRing(ring);
                            }
                        }
                    }
                    if (equip.hasSetOnlyId()) {
                        equip.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                    }
                }
                items.put(rs.getLong("inventoryitemid"), new Pair<>(equip.copy(), mit));
            } else {
                Item item = new Item(rs.getInt("itemid"), rs.getShort("position"), rs.getShort("quantity"), rs.getByte("flag"));
                item.setUniqueId(rs.getInt("uniqueid"));
                item.setOwner(rs.getString("owner"));
                item.setInventoryitemId(rs.getLong("inventoryitemid"));
                item.setEquipOnlyId(rs.getLong("equipOnlyId"));
                item.setExpiration(rs.getLong("expiredate"));
                item.setWorld(rs.getInt("World"));
                item.setGMLog(rs.getString("GM_Log"));
                item.setGiftFrom(rs.getString("sender"));
                if (GameConstants.isPet(item.getItemId())) {
                    if (item.getUniqueId() > -1) {
                        MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(), item.getPosition());
                        if (pet != null) {
                            item.setPet(pet);
                        } else {
//                            final int new_unique = MapleInventoryIdentifier.getInstance();
//                            item.setUniqueId(new_unique);
//                            item.setPet(MaplePet.createPet(item.getItemId(), new_unique));
                        }
                    } else {
                        //O_O hackish fix
                        final int new_unique = MapleInventoryIdentifier.getInstance();
                        item.setUniqueId(new_unique);
                        item.setPet(MaplePet.createPet(item.getItemId(), new_unique));
                    }
                }
                items.put(rs.getLong("inventoryitemid"), new Pair<>(item.copy(), mit));
            }
        }

        rs.close();
        ps.close();
        return items;
    }

    public void saveItems(List<Pair<IItem, MapleInventoryType>> items, Integer... id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        saveItems(items, con, id);
    }

    public void saveItems(List<Pair<IItem, MapleInventoryType>> items, final Connection con, Integer... id) throws SQLException {
        try {
            List<Integer> lulz = Arrays.asList(id);
            if ((this == HIRED_MERCHANT || this == STORAGE) && lulz.size() != 2) {
                return;
            }
            StringBuilder query = new StringBuilder();
            query.append("DELETE FROM `inventoryitems` WHERE `type` = ? AND (`");
            query.append(account ? "accountid" : "characterid");
            query.append("` = ?");
            if (this == HIRED_MERCHANT) {
                query.append(" OR `packageid` = ?");
            }
            if (this == STORAGE) {
                query.append(" AND `world` = ?");
            }
            query.append(")");

            PreparedStatement ps = con.prepareStatement(query.toString());
            ps.setInt(1, value);
            for (int i = 0; i < lulz.size(); i++) {
                ps.setInt(i + 2, lulz.get(i));
            }
            ps.executeUpdate();
            ps.close();
            if (items == null) {
                return;
            }
            StringBuilder query_2 = new StringBuilder("INSERT INTO `inventoryitems` (");
            query_2.append(account ? "accountid" : "characterid");
            if (this == HIRED_MERCHANT) {
                query_2.append(", packageid");
            }
            if (this == STORAGE) {
                query_2.append(", world");
            }
            boolean add = false;
            query_2.append(", itemid, inventorytype, position, quantity, owner, GM_Log, uniqueid, expiredate, flag, `type`, sender, equipOnlyId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
            if (this == HIRED_MERCHANT) {
                query_2.append(", ?");
            }
            if (this == STORAGE) {
                query_2.append(", ?");
            }
            query_2.append(")");
            ps = con.prepareStatement(query_2.toString(), Statement.RETURN_GENERATED_KEYS);

            String valueStr = "";
            int values = 28;
            for (int i = 0; i < values; i++) {
                if (i == (values - 1)) {
                    valueStr += "?";
                } else {
                    valueStr += "?, ";
                }
            }
            PreparedStatement pse = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, " + valueStr + ")", Statement.RETURN_GENERATED_KEYS);
            final Iterator<Pair<IItem, MapleInventoryType>> iter = items.iterator();
            Pair<IItem, MapleInventoryType> pair;
            while (iter.hasNext()) {
                pair = iter.next();
                IItem item = pair.getLeft();
                MapleInventoryType mit = pair.getRight();

                int i = 1;
                for (int x = 0; x < lulz.size(); x++) {
                    ps.setInt(i, lulz.get(x));
                    i++;
                }
                ps.setInt(i++, item.getItemId());
                ps.setInt(i++, mit.getType());
                ps.setInt(i++, item.getPosition());
                ps.setInt(i++, item.getQuantity());
                ps.setString(i++, item.getOwner());
                ps.setString(i++, item.getGMLog());
                ps.setInt(i++, item.getUniqueId());
                ps.setLong(i++, item.getExpiration());
                ps.setByte(i++, item.getFlag());
                ps.setByte(i++, (byte) value);
                ps.setString(i++, item.getGiftFrom());
                ps.setLong(i++, item.getEquipOnlyId());
                ps.executeUpdate();

                if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                    i = 1;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new RuntimeException("Inserting item failed.");
                        }
                        pse.setLong(i++, rs.getLong(1));
                    }
                    IEquip equip = (IEquip) item;
                    pse.setInt(i++, equip.getUpgradeSlots());
                    pse.setInt(i++, equip.getLevel());
                    pse.setInt(i++, equip.getStr());
                    pse.setInt(i++, equip.getDex());
                    pse.setInt(i++, equip.getInt());
                    pse.setInt(i++, equip.getLuk());
                    pse.setInt(i++, equip.getHp());
                    pse.setInt(i++, equip.getMp());
                    pse.setInt(i++, equip.getWatk());
                    pse.setInt(i++, equip.getMatk());
                    pse.setInt(i++, equip.getWdef());
                    pse.setInt(i++, equip.getMdef());
                    pse.setInt(i++, equip.getAcc());
                    pse.setInt(i++, equip.getAvoid());
                    pse.setInt(i++, equip.getHands());
                    pse.setInt(i++, equip.getSpeed());
                    pse.setInt(i++, equip.getJump());
                    pse.setInt(i++, equip.getViciousHammer());
                    pse.setInt(i++, equip.getItemEXP());
                    pse.setInt(i++, equip.getDurability());
                    pse.setByte(i++, equip.getEnhance());
                    pse.setInt(i++, equip.getPotential1());
                    pse.setInt(i++, equip.getPotential2());
                    pse.setInt(i++, equip.getPotential3());
                    pse.setInt(i++, equip.getIncSkill());
                    pse.setInt(i++, equip.getHpR());
                    pse.setInt(i++, equip.getMpR());
                    pse.executeUpdate();
                }
            }
            pse.close();
            ps.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static boolean isExistsByUniqueid(int uniqueid) {
        for (ItemLoader il : ItemLoader.values()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM `inventoryitems` WHERE `type` = ? AND uniqueid = ?");
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString());
                ps.setInt(1, il.value);
                ps.setInt(2, uniqueid);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {
                    ps.close();
                    rs.close();
                    return true;
                }
                ps.close();
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(ItemLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }
}
