package tools.packet;

import handling.SendPacketOpcode;
import tools.data.MaplePacketLittleEndianWriter;

public class BeansPacket {

    public static byte[] marqueeMessage(String playerName) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeansTips.getValue());
        int times = 1;
        mplew.writeInt(times);
        while (times > 0) {
            mplew.writeMapleAsciiString(playerName);
            times--;
        }
        return mplew.getPacket();
    }

    public static byte[] showBeans(int beansCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeanGameShow.getValue());
        mplew.writeInt(beansCount);
        return mplew.getPacket();
    }

    public static byte[] setLightLevel(int light) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeanGameShoot.getValue());
        mplew.write(3);
        mplew.write(light);
        return mplew.getPacket();
    }

    public static byte[] spinTest(int light) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeanGameShoot.getValue());
        mplew.write(4);
        mplew.write(light);
        mplew.write(1); // x
        mplew.write(2); // z
        mplew.write(3); // y
        mplew.write(1); // Helper
        mplew.write(0xFF);
        mplew.writeInt(1);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] rewardBalls(int ballsCount, int openStage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeanGameShoot.getValue());
        mplew.write(5);
        mplew.writeInt(ballsCount);
        mplew.write(openStage);
        return mplew.getPacket();
    }

    public static byte[] rewardBalls(int ballsCount) {
        return rewardBalls(ballsCount, 0);
    }

    public static byte[] exitBeans() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BeanGameShoot.getValue());
        mplew.write(6);
        return mplew.getPacket();
    }

    public static byte[] updateBalls(int cid, int beansCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_BEANS.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(beansCount);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

}
