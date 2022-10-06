package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import client.inventory.Equip;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import constants.GameConstants;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleWeaponType;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import provider.WzXML.MapleDataType;
import server.StructSetItem.SetItem;
import tools.Pair;
import tools.Triple;

public class MapleItemInformationProvider {

    private final static MapleItemInformationProvider instance = new MapleItemInformationProvider();
    protected final MapleDataProvider etcData = MapleDataProviderFactory.getDataProvider("Etc.wz");
    protected final MapleDataProvider itemData = MapleDataProviderFactory.getDataProvider("Item.wz");
    protected final MapleDataProvider equipData = MapleDataProviderFactory.getDataProvider("Character.wz");
    protected final MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider("String.wz");
    protected final Map<Integer, ItemInformation> dataCache = new TreeMap<>((v1, v2) -> v1.compareTo(v2));
    protected final Map<Integer, Integer> mobIds = new TreeMap<>((v1, v2) -> v1.compareTo(v2));
    protected final Map<Integer, Triple<Integer, List<Integer>, List<Integer>>> monsterBookSets = new TreeMap<>((v1, v2) -> v1.compareTo(v2));
    protected Map<Integer, Boolean> onEquipUntradableCache = new HashMap<>();
    protected final Map<Integer, List<StructPotentialItem>> potentialCache = new HashMap<>();
    protected final Map<Integer, MapleStatEffect> itemEffects = new HashMap<>();
    protected final Map<Integer, Map<String, Byte>> itemMakeStatsCache = new HashMap<>();
    protected final Map<Integer, Equip> equipCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, Integer>> chairRecovery = new HashMap();
    protected final Map<Integer, Short> petFlagInfo = new HashMap<>();
    protected final Map<Integer, Integer> petLimitLifeInfo = new HashMap<>();
    protected final Map<Integer, Integer> petLifeInfo = new HashMap<>();
    protected final Map<Integer, Map<String, Integer>> SkillStatsCache = new HashMap<>();
    protected final Map<Integer, Byte> consumeOnPickupCache = new HashMap<>();
    protected final Map<Integer, List<Integer>> petsCanConsumeCache = new HashMap<>();
    protected final Map<Integer, List<Pair<Integer, Integer>>> summonMobCache = new HashMap<>();
    protected final Map<Integer, StructSetItem> setItems = new HashMap<>();

    protected MapleItemInformationProvider() {
        System.out.println("【讀取中】 MapleItemInformationProvider :::");
    }

    public final void loadEtc(boolean reload) {
        if (reload) {
            setItems.clear();
            potentialCache.clear();
        }
        if (!setItems.isEmpty() || !potentialCache.isEmpty()) {
            return;
        }

//        final MapleData setsData = etcData.getData("SetItemInfo.img");
//        StructSetItem itemz;
//        SetItem itez;
//        for (MapleData dat : setsData) {
//            itemz = new StructSetItem();
//            itemz.setItemID = Integer.parseInt(dat.getName());
//            itemz.completeCount = (byte) MapleDataTool.getIntConvert("completeCount", dat, 0);
//            for (MapleData level : dat.getChildByPath("ItemID")) {
//                if (level.getType() != MapleDataType.INT) {
//                    for (MapleData leve : level) {
//                        if (!leve.getName().equals("representName") && !leve.getName().equals("typeName")) {
//                            itemz.itemIDs.add(MapleDataTool.getString(level));
//                        }
//                    }
//                } else {
//                    itemz.itemIDs.add(Integer.toString(MapleDataTool.getInt(level)));
//                }
//            }
//            for (MapleData level : dat.getChildByPath("Effect")) {
//                itez = new SetItem();
//                itez.incPDD = MapleDataTool.getIntConvert("incPDD", level, 0);
//                itez.incMDD = MapleDataTool.getIntConvert("incMDD", level, 0);
//                itez.incSTR = MapleDataTool.getIntConvert("incSTR", level, 0);
//                itez.incDEX = MapleDataTool.getIntConvert("incDEX", level, 0);
//                itez.incINT = MapleDataTool.getIntConvert("incINT", level, 0);
//                itez.incLUK = MapleDataTool.getIntConvert("incLUK", level, 0);
//                itez.incACC = MapleDataTool.getIntConvert("incACC", level, 0);
//                itez.incPAD = MapleDataTool.getIntConvert("incPAD", level, 0);
//                itez.incMAD = MapleDataTool.getIntConvert("incMAD", level, 0);
//                itez.incSpeed = MapleDataTool.getIntConvert("incSpeed", level, 0);
//                itez.incMHP = MapleDataTool.getIntConvert("incMHP", level, 0);
//                itez.incMMP = MapleDataTool.getIntConvert("incMMP", level, 0);
//                itemz.items.put(Integer.parseInt(level.getName()), itez);
//            }
//            setItems.put(itemz.setItemID, itemz);
//        }
        /*        final MapleData potsData = itemData.getData("ItemOption.img");
         StructPotentialItem item;
         List<StructPotentialItem> items;
         for (MapleData dat : potsData) {
         items = new LinkedList<StructPotentialItem>();
         for (MapleData level : dat.getChildByPath("level")) {
         item = new StructPotentialItem();
         item.optionType = MapleDataTool.getIntConvert("info/optionType", dat, 0);
         item.reqLevel = MapleDataTool.getIntConvert("info/reqLevel", dat, 0);
         item.face = MapleDataTool.getString("face", level, "");
         item.boss = MapleDataTool.getIntConvert("boss", level, 0) > 0;
         item.potentialID = Short.parseShort(dat.getName());
         item.attackType = (short) MapleDataTool.getIntConvert("attackType", level, 0);
         item.incMHP = (short) MapleDataTool.getIntConvert("incMHP", level, 0);
         item.incMMP = (short) MapleDataTool.getIntConvert("incMMP", level, 0);

         item.incSTR = (byte) MapleDataTool.getIntConvert("incSTR", level, 0);
         item.incDEX = (byte) MapleDataTool.getIntConvert("incDEX", level, 0);
         item.incINT = (byte) MapleDataTool.getIntConvert("incINT", level, 0);
         item.incLUK = (byte) MapleDataTool.getIntConvert("incLUK", level, 0);
         item.incACC = (byte) MapleDataTool.getIntConvert("incACC", level, 0);
         item.incEVA = (byte) MapleDataTool.getIntConvert("incEVA", level, 0);
         item.incSpeed = (byte) MapleDataTool.getIntConvert("incSpeed", level, 0);
         item.incJump = (byte) MapleDataTool.getIntConvert("incJump", level, 0);
         item.incPAD = (byte) MapleDataTool.getIntConvert("incPAD", level, 0);
         item.incMAD = (byte) MapleDataTool.getIntConvert("incMAD", level, 0);
         item.incPDD = (byte) MapleDataTool.getIntConvert("incPDD", level, 0);
         item.incMDD = (byte) MapleDataTool.getIntConvert("incMDD", level, 0);
         item.prop = (byte) MapleDataTool.getIntConvert("prop", level, 0);
         item.time = (byte) MapleDataTool.getIntConvert("time", level, 0);
         item.incSTRr = (byte) MapleDataTool.getIntConvert("incSTRr", level, 0);
         item.incDEXr = (byte) MapleDataTool.getIntConvert("incDEXr", level, 0);
         item.incINTr = (byte) MapleDataTool.getIntConvert("incINTr", level, 0);
         item.incLUKr = (byte) MapleDataTool.getIntConvert("incLUKr", level, 0);
         item.incMHPr = (byte) MapleDataTool.getIntConvert("incMHPr", level, 0);
         item.incMMPr = (byte) MapleDataTool.getIntConvert("incMMPr", level, 0);
         item.incACCr = (byte) MapleDataTool.getIntConvert("incACCr", level, 0);
         item.incEVAr = (byte) MapleDataTool.getIntConvert("incEVAr", level, 0);
         item.incPADr = (byte) MapleDataTool.getIntConvert("incPADr", level, 0);
         item.incMADr = (byte) MapleDataTool.getIntConvert("incMADr", level, 0);
         item.incPDDr = (byte) MapleDataTool.getIntConvert("incPDDr", level, 0);
         item.incMDDr = (byte) MapleDataTool.getIntConvert("incMDDr", level, 0);
         item.incCr = (byte) MapleDataTool.getIntConvert("incCr", level, 0);
         item.incDAMr = (byte) MapleDataTool.getIntConvert("incDAMr", level, 0);
         item.RecoveryHP = (byte) MapleDataTool.getIntConvert("RecoveryHP", level, 0);
         item.RecoveryMP = (byte) MapleDataTool.getIntConvert("RecoveryMP", level, 0);
         item.HP = (byte) MapleDataTool.getIntConvert("HP", level, 0);
         item.MP = (byte) MapleDataTool.getIntConvert("MP", level, 0);
         item.level = (byte) MapleDataTool.getIntConvert("level", level, 0);
         item.ignoreTargetDEF = (byte) MapleDataTool.getIntConvert("ignoreTargetDEF", level, 0);
         item.ignoreDAM = (byte) MapleDataTool.getIntConvert("ignoreDAM", level, 0);
         item.DAMreflect = (byte) MapleDataTool.getIntConvert("DAMreflect", level, 0);
         item.mpconReduce = (byte) MapleDataTool.getIntConvert("mpconReduce", level, 0);
         item.mpRestore = (byte) MapleDataTool.getIntConvert("mpRestore", level, 0);
         item.incMesoProp = (byte) MapleDataTool.getIntConvert("incMesoProp", level, 0);
         item.incRewardProp = (byte) MapleDataTool.getIntConvert("incRewardProp", level, 0);
         item.incAllskill = (byte) MapleDataTool.getIntConvert("incAllskill", level, 0);
         item.ignoreDAMr = (byte) MapleDataTool.getIntConvert("ignoreDAMr", level, 0);
         item.RecoveryUP = (byte) MapleDataTool.getIntConvert("RecoveryUP", level, 0);
         switch (item.potentialID) {
         case 31001:
         case 31002:
         case 31003:
         case 31004:
         item.skillID = (short) (item.potentialID - 23001);
         break;
         default:
         item.skillID = 0;
         break;
         }
         items.add(item);
         }
         potentialCache.put(Integer.parseInt(dat.getName()), items);
         }*/
    }

    public void loadItems(boolean reload) {
        if (reload) {
            dataCache.clear();
        }
        if (!dataCache.isEmpty()) {
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();

            // Load Item Data
            PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_itemdata");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                initItemInformation(rs);
            }
            rs.close();
            ps.close();

            // Load Item Equipment Data
            ps = con.prepareStatement("SELECT * FROM wz_itemequipdata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemEquipData(rs);
            }
            rs.close();
            ps.close();

            // Load Item Addition Data
            ps = con.prepareStatement("SELECT * FROM wz_itemadddata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemAddData(rs);
            }
            rs.close();
            ps.close();

            // Load Item Reward Data
            ps = con.prepareStatement("SELECT * FROM wz_itemrewarddata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemRewardData(rs);
            }
            rs.close();
            ps.close();

            // Finalize all Equipments
            for (Entry<Integer, ItemInformation> entry : dataCache.entrySet()) {
                if (GameConstants.getInventoryType(entry.getKey()) == MapleInventoryType.EQUIP) {
                    finalizeEquipData(entry.getValue());
                }
            }
        } catch (SQLException ex) {
        }
        //System.out.println(dataCache.size() + " items loaded.");
    }

    public final List<StructPotentialItem> getPotentialInfo(final int potId) {
        return potentialCache.get(potId);
    }

    public final Map<Integer, List<StructPotentialItem>> getAllPotentialInfo() {
        return potentialCache;
    }

    public static final MapleItemInformationProvider getInstance() {
        return instance;
    }

    public Pair<Integer, Integer> getChairRecovery(int itemId) {
        if (itemId / 10000 != 301) {
            return null;
        }
        if (chairRecovery.containsKey(itemId)) {
            return (Pair) chairRecovery.get(itemId);
        }
        int recoveryHP = MapleDataTool.getIntConvert("info/recoveryHP", getItemData(itemId), 0);
        int recoveryMP = MapleDataTool.getIntConvert("info/recoveryMP", getItemData(itemId), 0);
        Pair ret = new Pair(recoveryHP, recoveryMP);
        chairRecovery.put(itemId, ret);
        return ret;
    }

    public final Collection<ItemInformation> getAllItems() {
        return dataCache.values();
    }

    public final boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case 雙手斧:
            case 雙手棍:
            case 弓:
            case 拳套:
            case 弩:
            case 槍:
            case 矛:
            case 雙手劍:
            case 火槍:
            case 指虎:
                return true;
            default:
                return false;
        }
    }

    public boolean isUntradeableOnEquip(int itemId) {
        if (onEquipUntradableCache.containsKey(itemId)) {
            return onEquipUntradableCache.get(itemId);
        }
        boolean untradableOnEquip = MapleDataTool.getIntConvert("info/equipTradeBlock", getItemData(itemId), 0) > 0;
        onEquipUntradableCache.put(itemId, untradableOnEquip);
        return untradableOnEquip;
    }

    public MapleWeaponType getWeaponType(int itemId) {
        int cat = (itemId / 10000) % 100;
        MapleWeaponType[] type = {MapleWeaponType.單手劍, MapleWeaponType.單手斧, MapleWeaponType.單手棍, MapleWeaponType.短劍, MapleWeaponType.沒有武器, MapleWeaponType.沒有武器, MapleWeaponType.沒有武器, MapleWeaponType.長杖, MapleWeaponType.短杖, MapleWeaponType.沒有武器, MapleWeaponType.雙手劍, MapleWeaponType.雙手斧, MapleWeaponType.雙手棍, MapleWeaponType.矛, MapleWeaponType.槍, MapleWeaponType.弓, MapleWeaponType.弩, MapleWeaponType.拳套, MapleWeaponType.指虎, MapleWeaponType.火槍};
        if (cat < 30 || cat > 49) {
            return MapleWeaponType.沒有武器;
        }
        return type[cat - 30];
    }

    protected final MapleData getItemData(final int itemId) {
        MapleData ret = null;
        final String idStr = "0" + String.valueOf(itemId);
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            // we should have .img files here beginning with the first 4 IID
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) {
                        return null;
                    }
                    ret = ret.getChildByPath(idStr);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return itemData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = equipData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return equipData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return ret;
    }

    /**
     * returns the maximum of items in one slot
     *
     * @param c
     * @param itemId
     * @return
     */
    public final short getSlotMax(final MapleClient c, final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.slotMax;
    }

    public final int getWholePrice(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return -1;
        }
        return i.wholePrice;
    }

    public final double getPrice(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return -1.0;
        }
        return i.price;
    }

    public final Map<String, Byte> getItemMakeStats(final int itemId) {
        if (itemMakeStatsCache.containsKey(itemId)) {
            return itemMakeStatsCache.get(itemId);
        }
        if (itemId / 10000 != 425) {
            return null;
        }
        final Map<String, Byte> ret = new LinkedHashMap<>();
        final MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        final MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        ret.put("incPAD", (byte) MapleDataTool.getInt("incPAD", info, 0)); // WATK
        ret.put("incMAD", (byte) MapleDataTool.getInt("incMAD", info, 0)); // MATK
        ret.put("incACC", (byte) MapleDataTool.getInt("incACC", info, 0)); // ACC
        ret.put("incEVA", (byte) MapleDataTool.getInt("incEVA", info, 0)); // AVOID
        ret.put("incSpeed", (byte) MapleDataTool.getInt("incSpeed", info, 0)); // SPEED
        ret.put("incJump", (byte) MapleDataTool.getInt("incJump", info, 0)); // JUMP
        ret.put("incMaxHP", (byte) MapleDataTool.getInt("incMaxHP", info, 0)); // HP
        ret.put("incMaxMP", (byte) MapleDataTool.getInt("incMaxMP", info, 0)); // MP
        ret.put("incSTR", (byte) MapleDataTool.getInt("incSTR", info, 0)); // STR
        ret.put("incINT", (byte) MapleDataTool.getInt("incINT", info, 0)); // INT
        ret.put("incLUK", (byte) MapleDataTool.getInt("incLUK", info, 0)); // LUK
        ret.put("incDEX", (byte) MapleDataTool.getInt("incDEX", info, 0)); // DEX
//	ret.put("incReqLevel", MapleDataTool.getInt("incReqLevel", info, 0)); // IDK!
        ret.put("randOption", (byte) MapleDataTool.getInt("randOption", info, 0)); // Black Crystal Wa/MA
        ret.put("randStat", (byte) MapleDataTool.getInt("randStat", info, 0)); // Dark Crystal - Str/Dex/int/Luk

        itemMakeStatsCache.put(itemId, ret);
        return ret;
    }

    private int rand(int min, int max) {
        return Math.abs((int) Randomizer.rand(min, max));
    }

    public Equip levelUpEquip(Equip equip, Map<String, Integer> sta) {
        Equip nEquip = (Equip) equip.copy();
        //is this all the stats?
        try {
            for (Entry<String, Integer> stat : sta.entrySet()) {
                switch (stat.getKey()) {
                    case "STRMin":
                        nEquip.setStr((short) (nEquip.getStr() + rand(stat.getValue(), sta.get("STRMax"))));
                        break;
                    case "DEXMin":
                        nEquip.setDex((short) (nEquip.getDex() + rand(stat.getValue(), sta.get("DEXMax"))));
                        break;
                    case "INTMin":
                        nEquip.setInt((short) (nEquip.getInt() + rand(stat.getValue(), sta.get("INTMax"))));
                        break;
                    case "LUKMin":
                        nEquip.setLuk((short) (nEquip.getLuk() + rand(stat.getValue(), sta.get("LUKMax"))));
                        break;
                    case "PADMin":
                        nEquip.setWatk((short) (nEquip.getWatk() + rand(stat.getValue(), sta.get("PADMax"))));
                        break;
                    case "PDDMin":
                        nEquip.setWdef((short) (nEquip.getWdef() + rand(stat.getValue(), sta.get("PDDMax"))));
                        break;
                    case "MADMin":
                        nEquip.setMatk((short) (nEquip.getMatk() + rand(stat.getValue(), sta.get("MADMax"))));
                        break;
                    case "MDDMin":
                        nEquip.setMdef((short) (nEquip.getMdef() + rand(stat.getValue(), sta.get("MDDMax"))));
                        break;
                    case "ACCMin":
                        nEquip.setAcc((short) (nEquip.getAcc() + rand(stat.getValue(), sta.get("ACCMax"))));
                        break;
                    case "EVAMin":
                        nEquip.setAvoid((short) (nEquip.getAvoid() + rand(stat.getValue(), sta.get("EVAMax"))));
                        break;
                    case "SpeedMin":
                        nEquip.setSpeed((short) (nEquip.getSpeed() + rand(stat.getValue(), sta.get("SpeedMax"))));
                        break;
                    case "JumpMin":
                        nEquip.setJump((short) (nEquip.getJump() + rand(stat.getValue(), sta.get("JumpMax"))));
                        break;
                    case "MHPMin":
                        nEquip.setHp((short) (nEquip.getHp() + rand(stat.getValue(), sta.get("MHPMax"))));
                        break;
                    case "MMPMin":
                        nEquip.setMp((short) (nEquip.getMp() + rand(stat.getValue(), sta.get("MMPMax"))));
                        break;
                    case "MaxHPMin":
                        nEquip.setHp((short) (nEquip.getHp() + rand(stat.getValue(), sta.get("MaxHPMax"))));
                        break;
                    case "MaxMPMin":
                        nEquip.setMp((short) (nEquip.getMp() + rand(stat.getValue(), sta.get("MaxMPMax"))));
                        break;
                }
            }
        } catch (NullPointerException e) {

        }
        return nEquip;
    }

    public final Map<Integer, Map<String, Integer>> getEquipIncrements(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null || i.equipIncs == null) {
            return new LinkedHashMap<>();
        }
        return i.equipIncs;
    }

    public final List<Integer> getEquipSkills(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.incSkill;
    }

    public final Map<String, Integer> getEquipStats(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.equipStats;
//        if (equipStatsCache.containsKey(itemId)) {
//            return equipStatsCache.get(itemId);
//        }
//        final Map<String, Integer> ret = new LinkedHashMap<>();
//        final MapleData item = getItemData(itemId);
//        if (item == null) {
//            return null;
//        }
//        final MapleData info = item.getChildByPath("info");
//        if (info == null) {
//            return null;
//        }
//        for (final MapleData data : info.getChildren()) {
//            if (data.getName().startsWith("inc")) {
//                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
//            }
//        }
//        ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
//        ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
//        ret.put("reqJob", MapleDataTool.getInt("reqJob", info, 0));
//        ret.put("reqSTR", MapleDataTool.getInt("reqSTR", info, 0));
//        ret.put("reqDEX", MapleDataTool.getInt("reqDEX", info, 0));
//        ret.put("reqINT", MapleDataTool.getInt("reqINT", info, 0));
//        ret.put("reqLUK", MapleDataTool.getInt("reqLUK", info, 0));
//        ret.put("reqPOP", MapleDataTool.getInt("reqPOP", info, 0));
//        ret.put("cash", MapleDataTool.getInt("cash", info, 0));
//        ret.put("canLevel", info.getChildByPath("level") == null ? 0 : 1);
//        ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
//        ret.put("success", MapleDataTool.getInt("success", info, 0));
//        ret.put("setItemID", MapleDataTool.getInt("setItemID", info, 0));
//        ret.put("equipTradeBlock", MapleDataTool.getInt("equipTradeBlock", info, 0));
//        ret.put("durability", MapleDataTool.getInt("durability", info, -1));
//
//        if (GameConstants.isMagicWeapon(itemId)) {
//            ret.put("elemDefault", MapleDataTool.getInt("elemDefault", info, 100));
//            ret.put("incRMAS", MapleDataTool.getInt("incRMAS", info, 100)); // Poison
//            ret.put("incRMAF", MapleDataTool.getInt("incRMAF", info, 100)); // Fire
//            ret.put("incRMAL", MapleDataTool.getInt("incRMAL", info, 100)); // Lightning
//            ret.put("incRMAI", MapleDataTool.getInt("incRMAI", info, 100)); // Ice
//        }
//
//        equipStatsCache.put(itemId, ret);
//        return ret;
    }

    public final boolean isCash(final int itemId) {
        if (getEquipStats(itemId) == null) {
            return GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH;
        }
        return GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || getEquipStats(itemId).get("cash") != null;
    }

    public final boolean canEquip(final Map<String, Integer> stats, final int itemid, final int level, final int job, final int fame, final int str, final int dex, final int luk, final int int_, final int supremacy) {
        if ((level + supremacy) >= getReqLevel(itemid) && str >= getReqStr(str) && dex >= getReqDex(itemid) && luk >= getReqLuk(itemid) && int_ >= getReqInt(itemid)) {
            final int fameReq = getReqFame(itemid);
            return !(fameReq != 0 && fame < fameReq);
        }
        return false;
    }

    public final int getReqFame(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqPOP")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqPOP");
    }

    public final int getReqDex(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqDex")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqDex");
    }

    public final int getReqLuk(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqLuk")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqLuk");
    }

    public final int getReqStr(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqStr")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqStr");
    }

    public final int getReqInt(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqInt")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqInt");
    }

    public final int getReqLevel(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqLevel")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqLevel");
    }

    public final int getSlots(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("tuc")) {
            return 0;
        }
        return getEquipStats(itemId).get("tuc");
    }

    public final int getSetItemID(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("setItemID")) {
            return 0;
        }
        return getEquipStats(itemId).get("setItemID");
    }

    public final StructSetItem getSetItem(final int setItemId) {
        return setItems.get((byte) setItemId);
    }

    public final List<Integer> getScrollReqs(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null || i.scrollReqs == null) {
            return new ArrayList<>();
        }
        return i.scrollReqs;
    }

    public final IItem scrollEquipWithId(final IItem equip, final IItem scrollId, final boolean ws, final MapleCharacter chr, final int vegas) {
        if (equip.getType() == 1) { // See IItem.java
            final Equip nEquip = (Equip) equip;
            final Map<String, Integer> stats = getEquipStats(scrollId.getItemId());
            final Map<String, Integer> eqstats = getEquipStats(equip.getItemId());
            final int succ = (GameConstants.isTablet(scrollId.getItemId()) ? GameConstants.getSuccessTablet(scrollId.getItemId(), nEquip.getLevel()) : ((GameConstants.isEquipScroll(scrollId.getItemId()) || GameConstants.isPotentialScroll(scrollId.getItemId()) ? 0 : !stats.containsKey("success") ? 0 : stats.get("success"))));
            final int curse = (GameConstants.isTablet(scrollId.getItemId()) ? GameConstants.getCurseTablet(scrollId.getItemId(), nEquip.getLevel()) : ((GameConstants.isEquipScroll(scrollId.getItemId()) || GameConstants.isPotentialScroll(scrollId.getItemId()) ? 0 : !stats.containsKey("cursed") ? 0 : stats.get("cursed"))));
            int success = succ + (vegas == 5610000 && succ == 10 ? 20 : (vegas == 5610001 && succ == 60 ? 30 : 0));
            if (GameConstants.isPotentialScroll(scrollId.getItemId()) || GameConstants.isEquipScroll(scrollId.getItemId()) || Randomizer.nextInt(100) <= success) {
                switch (scrollId.getItemId()) {
                    case 2049000:
                    case 2049001:
                    case 2049002:
                    case 2049003:
                    case 2049004:
                    case 2049005: {
                        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc")) {
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                        }
                        break;
                    }
                    case 2049006:
                    case 2049007:
                    case 2049008: {
                        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc")) {
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 2));
                        }
                        break;
                    }
                    case 2040727: // Spikes on shoe, prevents slip
                    {
                        byte flag = nEquip.getFlag();
                        flag |= ItemFlag.SPIKES.getValue();
                        nEquip.setFlag(flag);
                        break;
                    }
                    case 2041058: // Cape for Cold protection
                    {
                        byte flag = nEquip.getFlag();
                        flag |= ItemFlag.COLD.getValue();
                        nEquip.setFlag(flag);
                        break;
                    }
                    default: {
                        if (GameConstants.isChaosScroll(scrollId.getItemId())) {
                            final int z = GameConstants.getChaosNumber(scrollId.getItemId());
                            if (nEquip.getStr() > 0) {
                                nEquip.setStr((short) (nEquip.getStr() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getDex() > 0) {
                                nEquip.setDex((short) (nEquip.getDex() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getInt() > 0) {
                                nEquip.setInt((short) (nEquip.getInt() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getLuk() > 0) {
                                nEquip.setLuk((short) (nEquip.getLuk() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getWatk() > 0) {
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getWdef() > 0) {
                                nEquip.setWdef((short) (nEquip.getWdef() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getMatk() > 0) {
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getMdef() > 0) {
                                nEquip.setMdef((short) (nEquip.getMdef() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getAcc() > 0) {
                                nEquip.setAcc((short) (nEquip.getAcc() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getAvoid() > 0) {
                                nEquip.setAvoid((short) (nEquip.getAvoid() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getSpeed() > 0) {
                                nEquip.setSpeed((short) (nEquip.getSpeed() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getJump() > 0) {
                                nEquip.setJump((short) (nEquip.getJump() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getHp() > 0) {
                                nEquip.setHp((short) (nEquip.getHp() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            if (nEquip.getMp() > 0) {
                                nEquip.setMp((short) (nEquip.getMp() + Randomizer.nextInt(z) * (Randomizer.nextBoolean() ? 1 : -1)));
                            }
                            break;
                        } else if (GameConstants.isEquipScroll(scrollId.getItemId())) {
                            final int chanc = Math.max((scrollId.getItemId() == 2049300 ? 100 : 80) - (nEquip.getEnhance() * 10), 10);
                            if (Randomizer.nextInt(100) > chanc) {
                                return null; //destroyed, nib
                            }
                            if (nEquip.getStr() > 0 || Randomizer.nextInt(50) == 1) { //1/50
                                nEquip.setStr((short) (nEquip.getStr() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getDex() > 0 || Randomizer.nextInt(50) == 1) { //1/50
                                nEquip.setDex((short) (nEquip.getDex() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getInt() > 0 || Randomizer.nextInt(50) == 1) { //1/50
                                nEquip.setInt((short) (nEquip.getInt() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getLuk() > 0 || Randomizer.nextInt(50) == 1) { //1/50
                                nEquip.setLuk((short) (nEquip.getLuk() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getWatk() > 0 && GameConstants.isWeapon(nEquip.getItemId())) {
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getWdef() > 0 || Randomizer.nextInt(40) == 1) { //1/40
                                nEquip.setWdef((short) (nEquip.getWdef() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getMatk() > 0 && GameConstants.isWeapon(nEquip.getItemId())) {
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getMdef() > 0 || Randomizer.nextInt(40) == 1) { //1/40
                                nEquip.setMdef((short) (nEquip.getMdef() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getAcc() > 0 || Randomizer.nextInt(20) == 1) { //1/20
                                nEquip.setAcc((short) (nEquip.getAcc() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getAvoid() > 0 || Randomizer.nextInt(20) == 1) { //1/20
                                nEquip.setAvoid((short) (nEquip.getAvoid() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getSpeed() > 0 || Randomizer.nextInt(10) == 1) { //1/10
                                nEquip.setSpeed((short) (nEquip.getSpeed() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getJump() > 0 || Randomizer.nextInt(10) == 1) { //1/10
                                nEquip.setJump((short) (nEquip.getJump() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getHp() > 0 || Randomizer.nextInt(5) == 1) { //1/5
                                nEquip.setHp((short) (nEquip.getHp() + Randomizer.nextInt(5)));
                            }
                            if (nEquip.getMp() > 0 || Randomizer.nextInt(5) == 1) { //1/5
                                nEquip.setMp((short) (nEquip.getMp() + Randomizer.nextInt(5)));
                            }
                            nEquip.setEnhance((byte) (nEquip.getEnhance() + 1));
                            break;
                        } else if (GameConstants.isPotentialScroll(scrollId.getItemId())) {
                            if (nEquip.getState() == 0) {
                                final int chanc = scrollId.getItemId() == 2049400 ? 90 : 70;
                                if (Randomizer.nextInt(100) > chanc) {
                                    return null; //destroyed, nib
                                }
                                nEquip.resetPotential();
                            }
                            break;
                        } else {
                            for (Entry<String, Integer> stat : stats.entrySet()) {
                                final String key = stat.getKey();

                                switch (key) {
                                    case "STR":
                                        nEquip.setStr((short) (nEquip.getStr() + stat.getValue()));
                                        break;
                                    case "DEX":
                                        nEquip.setDex((short) (nEquip.getDex() + stat.getValue()));
                                        break;
                                    case "INT":
                                        nEquip.setInt((short) (nEquip.getInt() + stat.getValue()));
                                        break;
                                    case "LUK":
                                        nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue()));
                                        break;
                                    case "PAD":
                                        nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue()));
                                        break;
                                    case "PDD":
                                        nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue()));
                                        break;
                                    case "MAD":
                                        nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue()));
                                        break;
                                    case "MDD":
                                        nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue()));
                                        break;
                                    case "ACC":
                                        nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue()));
                                        break;
                                    case "EVA":
                                        nEquip.setAvoid((short) (nEquip.getAvoid() + stat.getValue()));
                                        break;
                                    case "Speed":
                                        nEquip.setSpeed((short) (nEquip.getSpeed() + stat.getValue()));
                                        break;
                                    case "Jump":
                                        nEquip.setJump((short) (nEquip.getJump() + stat.getValue()));
                                        break;
                                    case "MHP":
                                        nEquip.setHp((short) (nEquip.getHp() + stat.getValue()));
                                        break;
                                    case "MMP":
                                        nEquip.setMp((short) (nEquip.getMp() + stat.getValue()));
                                        break;
                                    case "MHPr":
                                        nEquip.setHpR((short) (nEquip.getHpR() + stat.getValue()));
                                        break;
                                    case "MMPr":
                                        nEquip.setMpR((short) (nEquip.getMpR() + stat.getValue()));
                                        break;
                                }
                            }
                            break;
                        }
                    }
                }
                if (!GameConstants.isCleanSlate(scrollId.getItemId()) && !GameConstants.isSpecialScroll(scrollId.getItemId()) && !GameConstants.isEquipScroll(scrollId.getItemId()) && !GameConstants.isPotentialScroll(scrollId.getItemId())) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                }
            } else {
                if (!ws && !GameConstants.isCleanSlate(scrollId.getItemId()) && !GameConstants.isSpecialScroll(scrollId.getItemId()) && !GameConstants.isEquipScroll(scrollId.getItemId()) && !GameConstants.isPotentialScroll(scrollId.getItemId())) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                }
                if (Randomizer.nextInt(99) < curse) {
                    return null;
                }
            }
        }
        return equip;
    }

    public final IItem getEquipById(final int equipId) {
        return getEquipById(equipId, -1);
    }

    public final IItem getEquipById(final int equipId, final int ringId) {
        final Equip nEquip = new Equip(equipId, (byte) 0, ringId, (byte) 0);
        nEquip.setQuantity((short) 1);
        final Map<String, Integer> stats = getEquipStats(equipId);
        if (stats != null) {
            for (Entry<String, Integer> stat : stats.entrySet()) {
                final String key = stat.getKey();

                switch (key) {
                    case "STR":
                        nEquip.setStr(stat.getValue().shortValue());
                        break;
                    case "DEX":
                        nEquip.setDex(stat.getValue().shortValue());
                        break;
                    case "INT":
                        nEquip.setInt(stat.getValue().shortValue());
                        break;
                    case "LUK":
                        nEquip.setLuk(stat.getValue().shortValue());
                        break;
                    case "PAD":
                        nEquip.setWatk(stat.getValue().shortValue());
                        break;
                    case "PDD":
                        nEquip.setWdef(stat.getValue().shortValue());
                        break;
                    case "MAD":
                        nEquip.setMatk(stat.getValue().shortValue());
                        break;
                    case "MDD":
                        nEquip.setMdef(stat.getValue().shortValue());
                        break;
                    case "ACC":
                        nEquip.setAcc(stat.getValue().shortValue());
                        break;
                    case "EVA":
                        nEquip.setAvoid(stat.getValue().shortValue());
                        break;
                    case "Speed":
                        nEquip.setSpeed(stat.getValue().shortValue());
                        break;
                    case "Jump":
                        nEquip.setJump(stat.getValue().shortValue());
                        break;
                    case "MHP":
                        nEquip.setHp(stat.getValue().shortValue());
                        break;
                    case "MMP":
                        nEquip.setMp(stat.getValue().shortValue());
                        break;
                    case "MHPr":
                        nEquip.setHpR(stat.getValue().shortValue());
                        break;
                    case "MMPr":
                        nEquip.setMpR(stat.getValue().shortValue());
                        break;
                    case "tuc":
                        nEquip.setUpgradeSlots(stat.getValue().byteValue());
                        break;
                    case "Craft":
                        nEquip.setHands(stat.getValue().shortValue());
                        break;
                    case "durability":
                        nEquip.setDurability(stat.getValue());
//                } else if (key.equals("afterImage")) {
                        break;
                }
            }
        }
        equipCache.put(equipId, nEquip);
        return nEquip.copy();
    }

    private short getRandStat(final short defaultValue, final int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        // vary no more than ceil of 10% of stat
        final int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);

        return (short) ((defaultValue - lMaxRange) + Math.floor(Math.random() * (lMaxRange * 2 + 1)));
    }

    public final Equip randomizeStats(final Equip equip) {
        equip.setStr(getRandStat(equip.getStr(), 5));
        equip.setDex(getRandStat(equip.getDex(), 5));
        equip.setInt(getRandStat(equip.getInt(), 5));
        equip.setLuk(getRandStat(equip.getLuk(), 5));
        equip.setMatk(getRandStat(equip.getMatk(), 5));
        equip.setWatk(getRandStat(equip.getWatk(), 5));
        equip.setAcc(getRandStat(equip.getAcc(), 5));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5));
        equip.setJump(getRandStat(equip.getJump(), 5));
        equip.setHands(getRandStat(equip.getHands(), 5));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5));
        equip.setWdef(getRandStat(equip.getWdef(), 10));
        equip.setMdef(getRandStat(equip.getMdef(), 10));
        equip.setHp(getRandStat(equip.getHp(), 10));
        equip.setMp(getRandStat(equip.getMp(), 10));
        return equip;
    }

    public final MapleStatEffect getItemEffect(final int itemId) {
        MapleStatEffect ret = itemEffects.get(Integer.valueOf(itemId));
        if (ret == null) {
            final MapleData item = getItemData(itemId);
            if (item == null) {
                return null;
            }
            ret = MapleStatEffect.loadItemEffectFromData(item.getChildByPath("spec"), itemId);
            itemEffects.put(itemId, ret);
        }
        return ret;
    }

    public final List<Pair<Integer, Integer>> getSummonMobs(final int itemId) {
        if (summonMobCache.containsKey(itemId)) {
            return summonMobCache.get(itemId);
        }
        if (!GameConstants.isSummonSack(itemId)) {
            return null;
        }
        final MapleData data = getItemData(itemId).getChildByPath("mob");
        if (data == null) {
            return null;
        }
        final List<Pair<Integer, Integer>> mobPairs = new ArrayList<>();

        for (final MapleData child : data.getChildren()) {
            mobPairs.add(new Pair<>(
                    MapleDataTool.getIntConvert("id", child),
                    MapleDataTool.getIntConvert("prob", child)));
        }
        summonMobCache.put(itemId, mobPairs);
        return mobPairs;
    }

    public final int getCardMobId(final int id) {
        final ItemInformation i = getItemInformation(id);
        if (i == null) {
            return 0;
        }
        return i.monsterBook;
    }

    public final int getWatkForProjectile(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null || i.equipStats == null || i.equipStats.get("incPAD") == null) {
            return 0;
        }
        return i.equipStats.get("incPAD");
    }

    public final boolean canScroll(final int scrollid, final int itemid) {
        return (scrollid / 100) % 100 == (itemid / 10000) % 100;
    }

    public final String getName(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.name;
    }

    public final String getDesc(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.desc;
    }

    public final String getMsg(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.msg;
    }

    public final short getItemMakeLevel(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.itemMakeLevel;
    }

    public final byte isConsumeOnPickup(final int itemId) {
        // 0 = not, 1 = consume on pickup, 2 = consume + party
        if (consumeOnPickupCache.containsKey(itemId)) {
            return consumeOnPickupCache.get(itemId);
        }
        final MapleData data = getItemData(itemId);
        byte consume = (byte) MapleDataTool.getIntConvert("spec/consumeOnPickup", data, 0);
        if (consume == 0) {
            consume = (byte) MapleDataTool.getIntConvert("specEx/consumeOnPickup", data, 0);
        }
        if (consume == 1) {
            if (MapleDataTool.getIntConvert("spec/party", getItemData(itemId), 0) > 0) {
                consume = 2;
            }
        }
        consumeOnPickupCache.put(itemId, consume);
        return consume;
    }

    public final boolean isDropRestricted(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return ((i.flag & 0x200) != 0 || (i.flag & 0x400) != 0 || GameConstants.isDropRestricted(itemId));
    }

    public final boolean isPickupRestricted(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return ((i.flag & 0x80) != 0 || GameConstants.isPickupRestricted(itemId)) && itemId != 4001168 && itemId != 4031306 && itemId != 4031307;
    }

    public final boolean isAccountShared(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x100) != 0;
    }

    public final int getStateChangeItem(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.stateChange;
    }

    public final int getMeso(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.meso;
    }

    public final boolean isKarmaEnabled(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return i.karmaEnabled == 1;
    }

    public final boolean isPKarmaEnabled(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return i.karmaEnabled == 2;
    }

    public final boolean isPickupBlocked(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x40) != 0;
    }

    public final boolean isLogoutExpire(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x20) != 0;
    }

    public final boolean cantSell(final int itemId) { //true = cant sell, false = can sell
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x10) != 0;
    }

    public final Pair<Integer, List<StructRewardItem>> getRewardItem(final int itemid) {
        final ItemInformation i = getItemInformation(itemid);
        if (i == null) {
            return null;
        }
        return new Pair<>(i.totalprob, i.rewardItems);
    }

    public final Map<String, Integer> getSkillStats(final int itemId) {
        if (SkillStatsCache.containsKey(itemId)) {
            return SkillStatsCache.get(itemId);
        }
        if (!(itemId / 10000 == 228 || itemId / 10000 == 229 || itemId / 10000 == 562)) { // Skillbook and mastery book
            return null;
        }
        final MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        final MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        final Map<String, Integer> ret = new LinkedHashMap<>();
        for (final MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
        ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));

        final MapleData skill = info.getChildByPath("skill");

        for (int i = 0; i < skill.getChildren().size(); i++) { // List of allowed skillIds
            ret.put("skillid" + i, MapleDataTool.getInt(Integer.toString(i), skill, 0));
        }
        SkillStatsCache.put(itemId, ret);
        return ret;
    }

    public final List<Integer> petsCanConsume(final int itemId) {
        if (petsCanConsumeCache.get(itemId) != null) {
            return petsCanConsumeCache.get(itemId);
        }
        final List<Integer> ret = new ArrayList<>();
        final MapleData data = getItemData(itemId);
        if (data == null || data.getChildByPath("spec") == null) {
            return ret;
        }
        int curPetId;
        for (MapleData c : data.getChildByPath("spec")) {
            try {
                Integer.parseInt(c.getName());
            } catch (NumberFormatException e) {
                continue;
            }
            curPetId = MapleDataTool.getInt(c, 0);
            if (curPetId == 0) {
                break;
            }
            ret.add(curPetId);
        }
        petsCanConsumeCache.put(itemId, ret);
        return ret;
    }

    public final boolean isQuestItem(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x200) != 0 && itemId / 10000 != 301;
    }

    public final Pair<Integer, List<Integer>> questItemInfo(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return new Pair<>(i.questId, i.questItems);
    }

    public final boolean itemExists(final int itemId) {
        if (GameConstants.getInventoryType(itemId) == MapleInventoryType.UNDEFINED) {
            return false;
        }
        return getItemInformation(itemId) != null;
    }

    public int getPetLimitLife(int itemid) {
        if (this.petLimitLifeInfo.containsKey(itemid)) {
            return (this.petLimitLifeInfo.get(itemid));
        }
        if (!GameConstants.isPet(itemid)) {
            return 0;
        }
        MapleData item = getItemData(itemid);
        int limitLife = MapleDataTool.getIntConvert("info/limitedLife", item, 0);
        this.petLimitLifeInfo.put(itemid, limitLife);
        return limitLife;
    }

    public int getPetLife(int itemid) {
        if (this.petLifeInfo.containsKey(itemid)) {
            return (this.petLifeInfo.get(itemid));
        }
        if (!GameConstants.isPet(itemid)) {
            return 0;
        }
        MapleData item = getItemData(itemid);
        int life = MapleDataTool.getIntConvert("info/life", item, 0);
        this.petLifeInfo.put(itemid, life);
        return life;
    }

    public short getPetFlagInfo(int itemId) {
        if (this.petFlagInfo.containsKey(itemId)) {
            return (this.petFlagInfo.get(itemId));
        }
        short flag = 0;
        if (!GameConstants.isPet(itemId)) {
            return flag;
        }
        MapleData item = getItemData(itemId);
        if (item == null) {
            return flag;
        }
        if (MapleDataTool.getIntConvert("info/pickupItem", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.ITEM_PICKUP.getValue());
        }
        if (MapleDataTool.getIntConvert("info/longRange", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.EXPAND_PICKUP.getValue());
        }
        if (MapleDataTool.getIntConvert("info/pickupAll", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.AUTO_PICKUP.getValue());
        }
        if (MapleDataTool.getIntConvert("info/sweepForDrop", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.LEFTOVER_PICKUP.getValue());
        }
        if (MapleDataTool.getIntConvert("info/consumeHP", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.HP_CHARGE.getValue());
        }
        if (MapleDataTool.getIntConvert("info/consumeMP", item, 0) > 0) {
            flag = (short) (flag | MaplePet.PetFlag.MP_CHARGE.getValue());
        }
        this.petFlagInfo.put(itemId, flag);
        return flag;
    }

    public final boolean isOnlyTradeBlock(final int itemId) {
        final MapleData data = getItemData(itemId);
        boolean tradeblock = false;
        if (MapleDataTool.getIntConvert("info/tradeBlock", data, 0) == 1) {
            tradeblock = true;
        }
        return tradeblock;
    }

    public final ItemInformation getItemInformation(final int itemId) {
        if (itemId <= 0) {
            return null;
        }
        return dataCache.get(itemId);
    }
    private ItemInformation tmpInfo = null;

    public void initItemRewardData(ResultSet sqlRewardData) throws SQLException {
        final int itemID = sqlRewardData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemRewardData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.rewardItems == null) {
            tmpInfo.rewardItems = new ArrayList<>();
        }

        StructRewardItem add = new StructRewardItem();
        add.itemid = sqlRewardData.getInt("item");
        add.period = (add.itemid == 1122017 ? Math.max(sqlRewardData.getInt("period"), 7200) : sqlRewardData.getInt("period"));
        add.prob = sqlRewardData.getInt("prob");
        add.quantity = sqlRewardData.getShort("quantity");
        add.worldmsg = sqlRewardData.getString("worldMsg").length() <= 0 ? null : sqlRewardData.getString("worldMsg");
        add.effect = sqlRewardData.getString("effect");

        tmpInfo.rewardItems.add(add);
    }

    public void initItemAddData(ResultSet sqlAddData) throws SQLException {
        final int itemID = sqlAddData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemAddData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.equipAdditions == null) {
            tmpInfo.equipAdditions = new LinkedList<>();
        }

        while (sqlAddData.next()) {
            tmpInfo.equipAdditions.add(new Triple<>(sqlAddData.getString("key"), sqlAddData.getString("subKey"), sqlAddData.getString("value")));
        }
    }

    public void initItemEquipData(ResultSet sqlEquipData) throws SQLException {
        final int itemID = sqlEquipData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemEquipData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.equipStats == null) {
            tmpInfo.equipStats = new HashMap<>();
        }

        final int itemLevel = sqlEquipData.getInt("itemLevel");
        if (itemLevel == -1) {
            tmpInfo.equipStats.put(sqlEquipData.getString("key"), sqlEquipData.getInt("value"));
        } else {
            if (tmpInfo.equipIncs == null) {
                tmpInfo.equipIncs = new HashMap<>();
            }

            Map<String, Integer> toAdd = tmpInfo.equipIncs.get(itemLevel);
            if (toAdd == null) {
                toAdd = new HashMap<>();
                tmpInfo.equipIncs.put(itemLevel, toAdd);
            }
            toAdd.put(sqlEquipData.getString("key"), sqlEquipData.getInt("value"));
        }
    }

    public void finalizeEquipData(ItemInformation item) {
        int itemId = item.itemId;

        // Some equips do not have equip data. So we initialize it anyway if not initialized
        // already
        // Credits: Jay :)
        if (item.equipStats == null) {
            item.equipStats = new HashMap<>();
        }

        item.eq = new Equip(itemId, (byte) 0, -1, (byte) 0);
        short stats = GameConstants.getStat(itemId, 0);
        if (stats > 0) {
            item.eq.setStr(stats);
            item.eq.setDex(stats);
            item.eq.setInt(stats);
            item.eq.setLuk(stats);
        }
        stats = GameConstants.getATK(itemId, 0);
        if (stats > 0) {
            item.eq.setWatk(stats);
            item.eq.setMatk(stats);
        }
        stats = GameConstants.getHpMp(itemId, 0);
        if (stats > 0) {
            item.eq.setHp(stats);
            item.eq.setMp(stats);
        }
        stats = GameConstants.getDEF(itemId, 0);
        if (stats > 0) {
            item.eq.setWdef(stats);
            item.eq.setMdef(stats);
        }
        //讀取裝備屬性(新增屬性在這裡)
        if (item.equipStats.size() > 0) {
            for (Entry<String, Integer> stat : item.equipStats.entrySet()) {
                final String key = stat.getKey();
                switch (key) {
                    case "STR":
                        item.eq.setStr(GameConstants.getStat(itemId, stat.getValue()));
                        break;
                    case "DEX":
                        item.eq.setDex(GameConstants.getStat(itemId, stat.getValue()));
                        break;
                    case "INT":
                        item.eq.setInt(GameConstants.getStat(itemId, stat.getValue()));
                        break;
                    case "LUK":
                        item.eq.setLuk(GameConstants.getStat(itemId, stat.getValue()));
                        break;
                    case "PAD":
                        item.eq.setWatk(GameConstants.getATK(itemId, stat.getValue()));
                        break;
                    case "PDD":
                        item.eq.setWdef(GameConstants.getDEF(itemId, stat.getValue()));
                        break;
                    case "MAD":
                        item.eq.setMatk(GameConstants.getATK(itemId, stat.getValue()));
                        break;
                    case "MDD":
                        item.eq.setMdef(GameConstants.getDEF(itemId, stat.getValue()));
                        break;
                    case "ACC":
                        item.eq.setAcc((short) (int) stat.getValue());
                        break;
                    case "EVA":
                        item.eq.setAvoid((short) (int) stat.getValue());
                        break;
                    case "Speed":
                        item.eq.setSpeed((short) (int) stat.getValue());
                        break;
                    case "Jump":
                        item.eq.setJump((short) (int) stat.getValue());
                        break;
                    case "MHP":
                        item.eq.setHp(GameConstants.getHpMp(itemId, stat.getValue()));
                        break;
                    case "MMP":
                        item.eq.setMp(GameConstants.getHpMp(itemId, stat.getValue()));
                        break;
                    case "tuc":
                        item.eq.setUpgradeSlots(stat.getValue().byteValue());
                        break;
                    case "Craft":
                        item.eq.setHands(stat.getValue().shortValue());
                        break;
                    case "durability":
                        item.eq.setDurability(stat.getValue());
                        break;
                }
            }
            // 性向113版本沒有
//            if (item.equipStats.get("cash") != null && item.eq.getCharmEXP() <= 0) { //set the exp
//                short exp = 0;
//                int identifier = itemId / 10000;
//                if (ItemConstants.類型.武器(itemId) || identifier == 106) { //weapon overall
//                    exp = 60;
//                } else if (identifier == 100) { //hats
//                    exp = 50;
//                } else if (ItemConstants.類型.飾品(itemId) || identifier == 102 || identifier == 108 || identifier == 107) { //gloves shoes accessory
//                    exp = 40;
//                } else if (identifier == 104 || identifier == 105 || identifier == 110) { //top bottom cape
//                    exp = 30;
//                }
//                item.eq.setCharmEXP(exp);
//            }
        }
    }

    public void initItemInformation(ResultSet sqlItemData) throws SQLException {
        final ItemInformation ret = new ItemInformation();
        final int itemId = sqlItemData.getInt("itemid");
        ret.itemId = itemId;
        ret.slotMax = GameConstants.getSlotMax(itemId) > 0 ? GameConstants.getSlotMax(itemId) : sqlItemData.getShort("slotMax");
        ret.price = Double.parseDouble(sqlItemData.getString("price"));
        ret.wholePrice = sqlItemData.getInt("wholePrice");
        ret.stateChange = sqlItemData.getInt("stateChange");
        ret.name = sqlItemData.getString("name");
        ret.desc = sqlItemData.getString("desc");
        ret.msg = sqlItemData.getString("msg");

        ret.flag = sqlItemData.getInt("flags");

        ret.karmaEnabled = sqlItemData.getByte("karma");
        ret.meso = sqlItemData.getInt("meso");
        ret.monsterBook = sqlItemData.getInt("monsterBook");
        ret.itemMakeLevel = sqlItemData.getShort("itemMakeLevel");
        ret.questId = sqlItemData.getInt("questId");
        ret.create = sqlItemData.getInt("create");
        ret.replaceItem = sqlItemData.getInt("replaceId");
        ret.replaceMsg = sqlItemData.getString("replaceMsg");
        ret.afterImage = sqlItemData.getString("afterImage");
        ret.cardSet = 0;
        if (ret.monsterBook > 0 && itemId / 10000 == 238) {
            mobIds.put(ret.monsterBook, itemId);
            for (Entry<Integer, Triple<Integer, List<Integer>, List<Integer>>> set : monsterBookSets.entrySet()) {
                if (set.getValue().mid.contains(itemId)) {
                    ret.cardSet = set.getKey();
                    break;
                }
            }
        }

        final String scrollRq = sqlItemData.getString("scrollReqs");
        if (scrollRq.length() > 0) {
            ret.scrollReqs = new ArrayList<>();
            final String[] scroll = scrollRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.scrollReqs.add(Integer.parseInt(s));
                }
            }
        }
        final String consumeItem = sqlItemData.getString("consumeItem");
        if (consumeItem.length() > 0) {
            ret.questItems = new ArrayList<>();
            final String[] scroll = scrollRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.questItems.add(Integer.parseInt(s));
                }
            }
        }

        ret.totalprob = sqlItemData.getInt("totalprob");
        final String incRq = sqlItemData.getString("incSkill");
        if (incRq.length() > 0) {
            ret.incSkill = new ArrayList<>();
            final String[] scroll = incRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.incSkill.add(Integer.parseInt(s));
                }
            }
        }
        dataCache.put(itemId, ret);
    }
}