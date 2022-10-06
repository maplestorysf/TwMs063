/* global cm */

var status = -1;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else {
        status--;
    }

    if (status === -1) {
        cm.dispose();
    } else if (status === 0) {
        if (cm.haveItem(5220040)) {
            cm.sendYesNo("確定要使用 #b楓葉轉蛋券#k嗎？\r\n祝你好運哦!");
        } else {
            cm.sendOk("你沒有轉蛋券哦!.");
            cm.dispose();
        }
    } else if (status === 1) {
        var result = cm.gachapon(0);
        if (result !== -1) {
            cm.gainItem(5220040, -1);
            cm.sendOk("您已獲得 #b#t" + result + "##k.");
        } else {
            cm.sendOk("檢查一下背包是否已滿,或者所有設置的獎品已經發放完畢。");
        }
        cm.dispose();
    }
}