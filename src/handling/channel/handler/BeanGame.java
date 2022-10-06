package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import tools.data.LittleEndianAccessor;
import tools.packet.BeansPacket;

public class BeanGame {

    public static final void BeansGameAction(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        byte type = slea.readByte();
        switch (type) {
            case 0:// 調整小鋼珠力道
                break;
            case 1:// 開始打小鋼珠
                chr.setBeansStatus(true);
                break;
            case 2: // 暫停打小鋼珠
                chr.updateBeans();
                break;
            case 3:// 鋼珠進入蘑菇中獎區
                int beanNumber = slea.readInt(); // 中獎鋼珠序號
                slea.readInt();
                slea.readInt();
                if (chr.getBeansLight() < 7) {
                    chr.addBeansLight();
                }
                c.sendPacket(BeansPacket.setLightLevel(chr.getBeansLight()));
                break;
            case 4:
                // 亮燈區
                if (chr.getBeansLight() != 0) {
                    chr.setBeansLight(chr.getBeansLight() - 1);
                    c.sendPacket(BeansPacket.spinTest(chr.getBeansLight()));
                }
                break;
            case 5:
                // 上跑馬燈+獎勵
                if (chr.CanUseBeans()) {// Packet Edit
                    if (chr.canGainBeansReward()) {// Memory Editor
                        chr.setCanGainBeansReward(false);
                        chr.getMap().broadcastMessage(BeansPacket.marqueeMessage(chr.getName()));
                        c.sendPacket(BeansPacket.rewardBalls(2000));
                        chr.gainBeans(2000);
                    }
                }
                break;
            case 6:// 發射鋼珠訊息
                if (chr.CanUseBeans()) {// Packet Hack
                    slea.readByte();
                    int count = slea.readByte();

                    if (chr.getBeans() <= 0) {// Hack
                        chr.resetBeans();
                        c.sendPacket(BeansPacket.exitBeans());
                        return;
                    }

                    if (count >= 0) {// Hack
                        chr.gainBeans(-count);
                        chr.setCanGainBeansReward(true);
                        chr.dropMessage(-1, "目前小鋼珠數量: " + chr.getBeans());
                    }
                }
                break;
            case 7:
                if (chr.CanUseBeans()) {// Packet Hack
                    if (chr.canGainBeansReward()) {// Hack
                        slea.readInt();
                        int now = slea.readInt();
                        if (chr.getBeansStage() == 0) {
                            chr.setBeansTime(now);
                        }
                        if (now - chr.getBeansTime() > 10000) {
                            c.sendPacket(BeansPacket.rewardBalls(0, 5));
                            chr.setBeansStage(0);
                        } else if (now - chr.getBeansTime() > 5000) {
                            chr.setBeansStage(4);
                            c.sendPacket(BeansPacket.rewardBalls(100, 4));
                            chr.gainBeans(100);
                        } else {
                            chr.setBeansStage(1);
                            c.sendPacket(BeansPacket.rewardBalls(100, 1));
                            chr.gainBeans(100);
                        }
                    }
                }
                break;
            default: {
                System.out.println("未處理的類型【" + type + "】\n包" + slea.toString());
                break;
            }
        }
    }

    public static final void BeansUpdate(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        chr.resetBeans();
        c.sendPacket(BeansPacket.exitBeans());
    }
}
