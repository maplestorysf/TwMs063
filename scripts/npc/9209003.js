/*
 新年禮物 by梓條 v1
 */
var status = -1;


function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else if (mode == 0) {
        status--;
    } else {
        cm.dispose();
        return;
    }
    var Editing = true //false = 活動開始
    if (Editing) {
        cm.sendOk("等待2月8號吧!\r\n" +
                "今年的禮物是 ↓\r\n" +
                "#i1112585# + 666楓葉點數 #r或\r\n" +
                "#k#i1113021# + 666楓葉點數 ");
        cm.dispose();
        return;
    }
    if (status == 0) {
        cm.sendSimple("哈囉~我是新年活動NPC,今年有#r2個禮物跟楓葉點數666點#k但只能選#r其中一個戒指#r!!\r\n" +
                "#L0##d我想領 #i1112585# + 666楓葉點數\r\n" +
                "#L1##d我想領 #i1113021# + 666楓葉點數\r\n" +
                "#L2##d我不想要(點了就真的不能領囉)");
        //cm.dispose();
    } else if (status == 1) {
        if (selection == 2) {
            if (cm.getPlayer().getPrizeLog('新年活動') < 1) {
                cm.getPlayer().setPrizeLog('新年活動')
                cm.sendOk("不想領那就祝您新年快樂囉!");
                cm.worldMessage(6, "[訊息] " + "玩家" + cm.getChar().getName() + " 祝您新年快樂");
            } else {
                cm.sendOk("#d一個帳號只能領一次唷");
            }
        } else if (selection == 0) {
            if (cm.getPlayer().getPrizeLog('新年活動') < 1) {
                cm.getPlayer().modifyCSPoints(2, 666, true);
                cm.gainItem(1112585, 1);
                cm.getPlayer().setPrizeLog('新年活動');
                cm.sendOk("你領了 天使祝福戒指*1 + 666楓葉點數");
                cm.worldMessage(6, "[訊息] " + "玩家" + cm.getChar().getName() + " 領取了新年禮物");
            } else {
                cm.sendOk("#d一個帳號只能領一次唷");
            }
        } else if (selection == 1) {
            if (cm.getPlayer().getPrizeLog('新年活動') < 1) {
                cm.getPlayer().modifyCSPoints(2, 666, true);
                cm.gainItem(1113021, 1);
                cm.getPlayer().setPrizeLog('新年活動');
                cm.sendOk("你領了 愛情加速器*1 + 666楓葉點數");
                cm.worldMessage(6, "[訊息] " + "玩家" + cm.getChar().getName() + " 領取了新年禮物");
            } else {
                cm.sendOk("#d一個帳號只能領一次唷");
            }
        }
        cm.dispose();
    } else {
        cm.dispose();
    }
}