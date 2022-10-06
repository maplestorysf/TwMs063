package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import database.DatabaseConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.data.LittleEndianAccessor;

public class MapleMapRank {

    public static void FishRank(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `total_points` FROM fishrankings ORDER BY `total_points` ASC LIMIT 10");
            ResultSet rs = ps.executeQuery();
            //c.sendPacket(MaplePacketCreator.FishRank(rs));
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.out.println("讀取不夜城抓魚排行榜出錯 " + e);
        }
    }

    public static void PVPRank(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `winner` FROM pvprankings ORDER BY `winner` ASC LIMIT 10");
            ResultSet rs = ps.executeQuery();
            //c.sendPacket(MaplePacketCreator.PvpRank(rs));
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.out.println("讀取上海灘PVP排行榜出錯 " + e);
        }
    }
}
